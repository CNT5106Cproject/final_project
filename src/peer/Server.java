package peer;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import utils.CustomExceptions;
import utils.ErrorCode;
import utils.LogHandler;
import utils.Tools;

public class Server extends Thread{

	private Peer hostPeer;
	private static SystemInfo sysInfo = SystemInfo.getSingletonObj();
	private static FileManager fm = FileManager.getInstance();
	private static LogHandler logging = new LogHandler();

	public Server() {
		this.hostPeer = sysInfo.getHostPeer();
	}

	public void run() {
		logging.logStartServer();
		try {
			sysInfo.initServerListener();
			ServerSocket listener = sysInfo.getServerListener();
			try {
				/**
				 * Set up the unckoking & opt peer selection mechanism
				 */
				logging.writeLog("(server thread) Establishing Timer for PreferSelect with interval: " + sysInfo.getUnChokingInr() + "(sec)");
				sysInfo.initChokingMap();
				PreferSelect taskPrefSelect = new PreferSelect();
				sysInfo.getPreferSelectTimer().schedule(taskPrefSelect, 0, sysInfo.getUnChokingInr()*1000);
				
				logging.writeLog("(server thread) Establishing Timer for OptSelect with interval: " + sysInfo.getOptUnChokingInr() + "(sec)");
				OptSelect taskOptSelect = new OptSelect();
				sysInfo.getOptSelectTimer().schedule(taskOptSelect, 0, sysInfo.getOptUnChokingInr()*1000);

				int clientNum = 0;
				while(true) {
					new Handler(
						listener.accept(), 
						clientNum,
						this.hostPeer
					).start();
					// System.out.println("Client "  + clientNum + " is connected!");
					
					logging.writeLog(String.format(
						"(server thread) # %s client is connected",
						clientNum
					));
					clientNum++;
				}
			} 
			finally {
				listener.close();
			}
		}
		catch(IOException e) {
			if(sysInfo.getIsSystemComplete()) {
				logging.logSystemIsComplete();
			}
			else {
				String trace = Tools.getStackTrace(e);
				logging.writeLog("severe", "(server thread) ServerSocket IOException: " + trace);
			}
		}
		return;
	}
	/** 
	 *  This class will set an interval timer to make the unchoking-choking decision.
	*/
	public static class PreferSelect extends TimerTask {
		/**
    * UnchokingInterval p
    * - NumberOfPreferredNeighbors k
    */
		private final static int preferN = sysInfo.getPreferN();
		private ConcurrentHashMap<String, Socket> serverConnMap = new ConcurrentHashMap<String, Socket>();
		private ConcurrentHashMap<String, ActualMsg> actMsgMap = new ConcurrentHashMap<String, ActualMsg>();
		private ConcurrentHashMap<String, ObjectOutputStream> serverOpStream = new ConcurrentHashMap<String, ObjectOutputStream>();

		/**
		 * Select k neighbors from the interesting list
		 */
		private HashMap<String, Peer> neighborMap = sysInfo.getNeighborMap();;
		private HashMap<String, Peer> interestMap = sysInfo.getInterestMap();
		private HashMap<String, Peer> unChokingMap = sysInfo.getUnChokingMap();
		private HashMap<String, Peer> chokingMap = sysInfo.getChokingMap();

		private boolean randomlySelect = false;
		Random R = new Random();

		/**
		 * Construct select timer
		 */
		PreferSelect() {
			this.serverConnMap = sysInfo.getServerConnMap();
			this.actMsgMap = sysInfo.getActMsgMap();
			this.serverOpStream = sysInfo.getServerOpStream();
		}

		public void run() {
			try {
				if(isAllNeighborFinish()) {
					processClosingServer();
					return;
				}
				selectNodes();
				sendNewObtainList();
			}
			catch(CustomExceptions e) {
				String trace = Tools.getStackTrace(e);
				logging.writeLog("severe", trace);
			}
			catch(IOException e) {
				String trace = Tools.getStackTrace(e);
				logging.writeLog("severe", "Server PreferSelect Timer sending msg failed:" + trace);
			}
		}

		/**
		 * After receiving every neighbors complete msg
		 * 1. check and close all handler
		 * 2. close server listener
		 * 3. close all timer
		 * @throws IOException
		 */
		public void processClosingServer() throws IOException{
			sysInfo.setIsSystemComplete();
			Tools.timeSleep(250);
			logging.writeLog("All nodes are 'complete', close all server handlers, # server handlers left " + sysInfo.getServerConnMap().size());
			for(Entry<String, Socket> sConn: sysInfo.getServerConnMap().entrySet()) {
				Socket handlerSock = sConn.getValue();
				handlerSock.close();
			}
			Tools.timeSleep(500);
			logging.writeLog("All nodes are 'complete', close server listener");
			sysInfo.getServerListener().close();
			Tools.timeSleep(500);
			logging.writeLog("All nodes are 'complete', cancel OptSelectTimer");
			sysInfo.getOptSelectTimer().cancel();
			logging.writeLog("All nodes are 'complete', cancel PreferSelectTimer");
			sysInfo.getPreferSelectTimer().cancel();
		}

		private boolean isAllNeighborFinish() {
			logging.writeLog("check if all neighbor isComplete or not");
			for(Entry<String, Peer> n: sysInfo.getNeighborMap().entrySet()) {
				Peer check = n.getValue();
				if(check.getHasFile()) continue;
				if(!check.getIsComplete()) return false;
			}
			logging.writeLog("all neighbor isComplete !!");
			return true;
		}

		/**
		 * Select Nodes to choke and unchoke
		 * 1. update neighbor map
		 * 2. update interest map 
		 * 3. random select | picked by download rate
		 * 4. depend on the situation to handle the selected nodes and unselected nodes
		 * 		a.unchoke -> unchoke => put key in unchokeMap (continue receiving 'request in the reading buffer')
		 * 		b.unchoke -> choke => send 'choke', remove from unchokeMap, put key in chokeMap
		 * 													but if the peer is optimistically unchoked, leave it alone
		 * 		c.choke -> unchoke => send 'unchoke', remove from chokeMap put key in unchokeMap
		 * 		d.choke -> choke => put key in chokeMap
		 */
		private int selectNodes() throws IOException, CustomExceptions {
			logging.writeLog("PreferSelect - start selecting the nodes to pass file pieces");
			
			interestMap.clear();
			for(Entry<String, Peer> n: neighborMap.entrySet()) {
				if(n.getValue().getHasFile()) continue;
				if(n.getValue().getIsComplete()) continue;
				if(n.getValue().getIsInterested()) {
					interestMap.put(n.getKey(), n.getValue());
				}
			}

			if(interestMap.isEmpty()) return 0;
			
			if(sysInfo.getHostPeer().getHasFile() || (fm != null && fm.isComplete())) {
				randomlySelect = true;
			}
			
			// testRandomDownLoadRate();
			
			List<Entry<String, Peer>> interestList = new ArrayList<Entry<String, Peer>>(interestMap.entrySet());
			if(interestMap.size() > preferN) {
				/**
				 * 3. random select | picked by download rate
				 */
				if(randomlySelect) {
					interestList = shuffleByRandom(interestList);
				}
				else {
					interestList = sortByDownload(interestList);
				}
			}
			
			for(Entry<String, Peer> i: interestList) {
				logging.writeLog(String.format("PreferSelect %s download rate: %s", i.getKey(), i.getValue().getDownloadRate()));
			}
			/**
			 * 4. depend on the situation to handle the selected nodes and unselected nodes
			 */
			int count = 1;
			for(Entry<String, Peer> i: interestList) {
				if(count > preferN) {
					// Not been picked, choke them
					if(i.getKey() == sysInfo.getOptUnchokingPeer().getId()) continue;
					if(unChokingMap.get(i.getKey()) != null) {
						// b.unchoke -> choke => send 'choke', remove from unchokeMap, put key in chokeMap
						String key = i.getKey();
						if(actMsgMap.get(key) == null) { 
							throw new CustomExceptions(ErrorCode.missActMsgObj, "miss peerId: " + key);
						}
						if(serverOpStream.get(key) == null) {
							throw new CustomExceptions(ErrorCode.missServerOpStream, "miss peerId: " + key);
						}
						actMsgMap.get(key).send(serverOpStream.get(key), ActualMsg.CHOKE, 0);
						unChokingMap.remove(key);
					}
					// b+d  put key in chokeMap
					chokingMap.put(i.getKey(), i.getValue());
				}
				else {
					// Been picked, unchoke them, initially every interest neighbors will be choked
					if(chokingMap.get(i.getKey()) != null) {
						// c.choke -> unchoke => send 'unchoke', remove from chokeMap put key in unchokeMap  
						String key = i.getKey();
						if(actMsgMap.get(key) == null) { 
							throw new CustomExceptions(ErrorCode.missActMsgObj, "miss peerId: " + key);
						}
						if(serverOpStream.get(key) == null) {
							throw new CustomExceptions(ErrorCode.missServerOpStream, "miss peerId: " + key);
						}
						actMsgMap.get(key).send(serverOpStream.get(key), ActualMsg.UNCHOKE, 0);
						chokingMap.remove(key);
					}
					// b+d  put key in unchokeMap
					unChokingMap.put(i.getKey(), i.getValue());
				}
				count ++;
			}
			logging.logChangePrefersPeers();
			return 0;
		}

		/**
		 * send new obtain blocks to unfinished neighbors
		 * @throws IOException
		 * @throws CustomExceptions
		 */
		private void sendNewObtainList() throws IOException, CustomExceptions {
			List<Integer> obtainBlocks = sysInfo.getNewObtainBlocksCopy();
			logging.writeLog("PreferSelect new obtain block size: " + obtainBlocks.size());
			if(obtainBlocks.size() != 0) {
				logging.writeLog("PreferSelect start sending new obtain blocks");
				for(Entry<String, Peer> n: sysInfo.getNeighborMap().entrySet()) {
					String key = n.getKey();
					if(n.getValue().getHasFile()) continue;
					if(n.getValue().getIsComplete()) continue;
					if(actMsgMap.get(key) != null) { 
						for(Integer blockIdx: obtainBlocks) {
							actMsgMap.get(key).send(serverOpStream.get(key), ActualMsg.HAVE, blockIdx);
						}
					}
					else {
						throw new CustomExceptions(ErrorCode.missActMsgObj, "miss key: " + key);
					}
				}
			}
		}
		
		private List<Entry<String, Peer>> shuffleByRandom(List<Entry<String, Peer>> interestList) {
			logging.writeLog("PreferSelect shuffleByRandom with interestList size: " + interestList.size());
			Collections.shuffle(interestList, R);
			return interestList;
		}

		private List<Entry<String, Peer>> sortByDownload(List<Entry<String, Peer>> interestList) {
			logging.writeLog("PreferSelect sortByDownload with interestList size: " + interestList.size());
			if(interestList.size() > 0) {
				/**
				 * Compare the peer objects downloading rate
				 */
				Comparator <Entry<String, Peer>> rate = new Comparator<Entry<String, Peer>>() {
					@Override
					public int compare(Entry<String, Peer> p1, Entry<String, Peer> p2) {
						double r1 = p1.getValue().getDownloadRate();
						double r2 = p2.getValue().getDownloadRate();
						/**
						 * In descending order.
						 */
						if(r1 < r2) return 1;
						if(r1 > r2) return -1;
						return 0;
					}
				};
				
				Collections.sort(interestList, rate);
				return interestList;
			}
			return null;
		}
		/**
		 * Assign Random download rate to nodes
		 */
		private void testRandomDownLoadRate() {	
			// TODO set a real downloading rate
			for(Entry<String, Peer> n: interestMap.entrySet()) {
				n.getValue().setDownloadRate(R.nextDouble());
			}
		}
	}

	/** 
	*  This class will set an interval timer to make the opt unchoking decision.
	*/
	private static class OptSelect extends TimerTask {
		
		Random R = new Random();
		private ConcurrentHashMap<String, ObjectOutputStream> serverOpStream = new ConcurrentHashMap<String, ObjectOutputStream>();
		private ConcurrentHashMap<String, ActualMsg> actMsgMap = new ConcurrentHashMap<String, ActualMsg>();
		private HashMap<String, Peer> neighborMap = sysInfo.getNeighborMap(); 
		private HashMap<String, Peer> unChokingMap = sysInfo.getUnChokingMap();
		private HashMap<String, Peer> chokingMap = sysInfo.getChokingMap();
		List<Peer> interestList = new ArrayList<Peer>();
		
		/**
		* OptimisticUnchokingInterval 10
		* - 1 optimistically unchoked neighbor
    */
		OptSelect() {
			this.serverOpStream = sysInfo.getServerOpStream();
			this.actMsgMap = sysInfo.getActMsgMap();
		}

		public void run() {
			try {
				logging.writeLog("start OptSelect - selecting optimistically unchoked node with every: " + sysInfo.getOptUnChokingInr() + " sec");
				selectNodes();
			}
			catch(CustomExceptions e) {
				String trace = Tools.getStackTrace(e);
				logging.writeLog("severe", trace);
			}
			catch(IOException e) {
				String trace = Tools.getStackTrace(e);
				logging.writeLog("severe", "Server OptSelect Timer IOException:" + trace);
			}
		}

		/**
		 * 1. select interest candidates
		 * 2. random pick opt unchoking peer
		 * 3. check if pick the same as previous -> return
		 * 
		 * 4. check the previous node -> if it's in unchoke map -> set it to choke 
		 * 5. check the new node -> if it's in choke map -> set it to unchoke
		 * @return
		 * @throws IOException
		 * @throws CustomExceptions
		 */
		private int selectNodes() throws IOException, CustomExceptions{
			interestList.clear();
			for(Entry<String, Peer> n: neighborMap.entrySet()) {
				if(n.getValue().getHasFile()) continue;
				if(n.getValue().getIsComplete()) continue;
				if(n.getValue().getIsInterested()) {
					interestList.add(n.getValue());
				}
			}
			
			logging.writeLog("OptSelect interestList size " + interestList.size());
			if(interestList.isEmpty()) return 0;

			Peer previousPeer = sysInfo.getOptUnchokingPeer();
			Peer newPeer = interestList.get(R.nextInt(interestList.size()));
			sysInfo.setOptUnchokingPeer(newPeer);
			logging.writeLog("OptSelect previous node " + previousPeer.getId() + ", new node " + newPeer.getId());
			logging.logChangeOptUnchokedPeer();
			
			// // 3. check if pick the same as previous -> return
			if(newPeer.getId().equals(previousPeer.getId())) return 0;
			
			// 4. check the previous node -> if it's in unchoke map -> set it to choke 
			if(unChokingMap.get(previousPeer.getId()) != null){
				if(actMsgMap.get(previousPeer.getId()) == null) { 
					throw new CustomExceptions(ErrorCode.missActMsgObj, "miss peerID: " + previousPeer.getId());
				}
				if(serverOpStream.get(previousPeer.getId()) == null) {
					throw new CustomExceptions(ErrorCode.missServerOpStream, "miss peerID: " + previousPeer.getId());
				}
				actMsgMap.get(previousPeer.getId()).send(serverOpStream.get(previousPeer.getId()), ActualMsg.CHOKE, 0);
				unChokingMap.remove(previousPeer.getId());
				chokingMap.put(previousPeer.getId(), previousPeer);
			}

			if(chokingMap.get(newPeer.getId()) != null){
				if(actMsgMap.get(newPeer.getId()) == null) { 
					throw new CustomExceptions(ErrorCode.missActMsgObj, "miss peerID: " + newPeer.getId());
				}
				if(serverOpStream.get(newPeer.getId()) == null) {
					throw new CustomExceptions(ErrorCode.missServerOpStream, "miss peerID: " + newPeer.getId());
				}
				actMsgMap.get(previousPeer.getId()).send(serverOpStream.get(previousPeer.getId()), ActualMsg.UNCHOKE, 0);
				chokingMap.remove(newPeer.getId());
				unChokingMap.put(newPeer.getId(), newPeer);
			}
			return 0;
		}
	}

	/**
	* A handler thread class.  Handlers are spawned from the listening
	* loop and are responsible for dealing with a single client's requests.
	*/
  private static class Handler extends Thread {
		private Socket connection;
		//The index number of the client
		private int no;		
		private HandShake handShake;
		private Peer server;
		private Peer client;
		private ActualMsg actMsg;
		private ConcurrentHashMap<String, Socket> serverConnMap = new ConcurrentHashMap<String, Socket>();
		private ConcurrentHashMap<String, ActualMsg> actMsgMap = new ConcurrentHashMap<String, ActualMsg>();
		private ConcurrentHashMap<String, ObjectOutputStream> serverOpStream = new ConcurrentHashMap<String, ObjectOutputStream>();
		private ObjectOutputStream opStream = null;
		private InputStream inConn = null;

    public Handler(
			Socket connection, 
			int no, 
			Peer hostPeer 
		) {
	    this.no = no;
			this.server = hostPeer;
			this.client = null;
			this.handShake = null;
			this.actMsg = null;
			this.connection = connection;
			this.serverConnMap = sysInfo.getServerConnMap();
			this.actMsgMap = sysInfo.getActMsgMap();
			this.serverOpStream = sysInfo.getServerOpStream();
    }

    public void run() {
			/**
			 * Server progress
			 * 1. Handshake Msg -> success -> goes 2
			 * 2. Send BitField Msg to client 
			 * 3. Build InterestingList by interest messages
			 */
 			try {
				opStream = new ObjectOutputStream(connection.getOutputStream());
				inConn = connection.getInputStream();

				if(this.handShake == null) {	
					this.handShake = new HandShake();
					String getClientId = null;
					while(true) {
						// waiting for hand shake message
						getClientId = this.handShake.ReceiveHandShake(inConn);
						if(this.handShake.isSuccess() && getClientId != null) {
							this.client = new Peer(getClientId);
							// set clientID
							this.handShake.setTargetPeerID(getClientId);
							this.handShake.SendHandShake(opStream);
							logging.logSendHandShakeMsg(getClientId, "server");
							logging.logHandShakeSuccess(this.server, this.client);
							break;
						}
					}
			 	}
				
				/**
				 * 1. connection
				 * 2. actual msb obj
				 * 
				 * Store them into map for select thread to use it
				 */
				this.actMsg = new ActualMsg(this.client);
				serverConnMap.put(this.client.getId(), this.connection);
				actMsgMap.put(this.client.getId(), this.actMsg);
				serverOpStream.put(this.client.getId(), opStream);

				if(serverConnMap.get(this.client.getId()) == null) {
					throw new CustomExceptions(ErrorCode.missServerConn, "missing connection object, recreate the socket");
				}

				if(serverOpStream.get(this.client.getId()) == null) {
					throw new CustomExceptions(ErrorCode.missServerConn, "missing opStream, recreate the socket");
				}

				if(actMsgMap.get(this.client.getId()) == null) {
					actMsgMap.put(this.client.getId(), this.actMsg);
				}

				/**
				 * this.ownBitfield is set up at FileManager constructor
				 */
				this.actMsg.send(opStream, ActualMsg.BITFIELD, fm.getOwnBitfield());
				logging.logSendBitFieldMsg(this.client);
				
				byte msg_type = -1;
				while(true) {
					msg_type = actMsg.recv(inConn);
					if(msg_type != -1) {
						boolean isComplete = reactions(msg_type);
						if(isComplete) {
							// jump to close connections with client
							break;
						}
					}
				}
			}
			catch(CustomExceptions e){
				String trace = Tools.getStackTrace(e);
				String peerId = this.client != null ? this.client.getId() : "";
				logging.writeLog("severe", "(Server handler thread) CustomExceptions with client: " + peerId + ", ex:" + trace);
			}
			catch(IOException e){
				String peerId = this.client != null ? this.client.getId() : "";
				if(sysInfo.getIsSystemComplete()) {
					logging.writeLog("(Server handler thread) system is complete, close handler with " + peerId);
				}
				else {
					String trace = Tools.getStackTrace(e);
					logging.writeLog("severe", "(Server handler thread) IOException with client " + peerId + ", ex:" + trace);
				}
			}
			finally {
				try{
					if(this.client != null) {
						logging.writeLog(
							"(Server handler thread) " + this.client.getId() + " connection closing, connection handler with client"
						);
						if(this.serverOpStream.get(this.client.getId()) != null) {
							this.serverOpStream.remove(this.client.getId());
						}
						if(this.serverConnMap.get(this.client.getId()) != null) {
							this.serverConnMap.remove(this.client.getId());
						}
						if(this.actMsgMap.get(this.client.getId()) != null) {
							this.actMsgMap.remove(this.client.getId());
						}
					}
					this.inConn.close();
					this.opStream.close();
					this.connection.close();
				}
				catch(IOException e){
					String trace = Tools.getStackTrace(e);
					String peerId = this.client != null ? this.client.getId() : "";
					logging.writeLog("severe", "(Server handler thread) close connection failed : " + peerId + ", ex:" + trace);
				}
			}
			return;
		}

		/**
		 * Reaction of Server receiving the msg, base on the msg type 
		 * @param msg_type
		 * @return
		 * @throws IOException
		 */
		private boolean reactions(byte msg_type) throws IOException {
			if(msg_type == ActualMsg.INTERESTED) {
				logging.logReceiveInterestMsg(this.client);
				setNeighborIntStatus(this.client.getId(), true);
			}
			else if(msg_type == ActualMsg.NOTINTERESTED) {
				logging.logReceiveNotInterestMsg(this.client);
				setNeighborIntStatus(this.client.getId(), false);
			}
			else if(msg_type == ActualMsg.REQUEST) {
				/**
				 * Send back the request piece, if peer is unchoke
				 * 1. read block
				 * 2. send 
				 */
				logging.logReceiveRequestMsg(this.client);
				if(sysInfo.getChokingMap().get(this.client.getId()) != null) {
					logging.writeLog(this.client.getId() + " is choked, unable to response to peace");
					return false;
				}
				
				int blockIdx = this.actMsg.shortMsg.getBlockIdx();
				int blockLen = fm.getBlockSize(blockIdx);
				byte[] data = new byte[blockLen];
				fm.read(blockIdx, data, blockLen);

				this.actMsg.send(
					opStream, 
					ActualMsg.PIECE, 
					blockIdx, 
					data
				);
			}
			else if(msg_type == ActualMsg.COMPLETE) {
				logging.logReceiveCompleteMsg(this.client);
				/**
				 * 1. remove client from all map
				 * 2. remove object from connection & actMsg map 
				 */
				logging.writeLog("(server handler) notify that peer " + this.client.getId() + " isComplete");
				this.client.setIsComplete();
				sysInfo.getNeighborMap().put(this.client.getId(), this.client);
				
				removePeerFromMap();
				// remove connection and
				serverConnMap.remove(this.client.getId());
				actMsgMap.remove(this.client.getId());
				return true;
			}
			return false;
		}
		
		/**
		 * Change the neighbor peer's isInterested status in neighborMap.
		 * @param peerId
		 * @param status
		 */
		private synchronized void setNeighborIntStatus(String peerId, boolean status) {
			Peer p = sysInfo.getNeighborMap().get(peerId);
			p.setIsInterested(status);
			sysInfo.getNeighborMap().put(peerId, p);

			Peer check = sysInfo.getNeighborMap().get(this.client.getId());
			logging.writeLog(
				"check neighbor " + this.client.getId() + ", isInterested status: " + check.getIsInterested());
		}

		private void removePeerFromMap() {
			if(sysInfo.getInterestMap().get(this.client.getId()) != null) {
				sysInfo.getInterestMap().remove(this.client.getId());
			}
			if(sysInfo.getChokingMap().get(this.client.getId()) != null) {
				sysInfo.getChokingMap().remove(this.client.getId());
			}
			if(sysInfo.getUnChokingMap().get(this.client.getId()) != null) {
				sysInfo.getUnChokingMap().remove(this.client.getId());
			}
		}
  }
}
