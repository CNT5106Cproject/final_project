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
import java.util.concurrent.locks.ReentrantLock;

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
				logging.writeLog("Establishing Timer for PreferSelect with interval: " + sysInfo.getUnChokingInr()*1000 + "(ms)");
				sysInfo.initChokingMap();
				PreferSelect taskPrefSelect = new PreferSelect();
				sysInfo.getPreferSelectTimer().schedule(taskPrefSelect, 0, sysInfo.getUnChokingInr()*1000);
				// this.OptSelectTimer.schedule(optSelect, sysInfo.getOptUnChokingInr());

				int clientNum = 0;
				while(true) {
					new Handler(
						listener.accept(), 
						clientNum,
						this.hostPeer
					).start();
					// System.out.println("Client "  + clientNum + " is connected!");
					
					logging.writeLog(String.format(
						"(server) # %s client is connected",
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
		}

		public void run() {
			try {
				if(isAllNeighborFinish()) {
					sysInfo.setIsSystemComplete();
					logging.writeLog("All nodes are ‘end’, close server listener");
					// sysInfo.getServerThread().stop();
					sysInfo.getServerListener().close();
					Tools.timeSleep(500);
					logging.writeLog("All nodes are ‘end’, cancel PreferSelectTimer");
					sysInfo.getPreferSelectTimer().cancel();
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
		 * 		c.choke -> unchoke => send 'unchoke', remove from chokeMap put key in unchokeMap
		 * 		d.choke -> choke => put key in chokeMap
		 */
		private int selectNodes() throws IOException, CustomExceptions {
			logging.writeLog("start PreferSelect - selecting the nodes to pass file pieces");
			logging.writeLog("selecting interested node");
			
			interestMap.clear();
			for(Entry<String, Peer> n: neighborMap.entrySet()) {
				if(n.getValue().getHasFile()) continue;
				if(n.getValue().getIsComplete()) {
					
				};
				if(n.getValue().getIsInterested()) {
					interestMap.put(n.getKey(), n.getValue());
				}
			}

			if(interestMap.isEmpty()) return 0;
			
			if(sysInfo.getHostPeer().getHasFile() || (fm != null && fm.isComplete())) {
				randomlySelect = true;
			}
			
			testRandomDownLoadRate();
			
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
				logging.writeLog(String.format("%s download rate: %s", i.getKey(), i.getValue().getDownloadRate()));
			}
			
			/**
			 * 4. depend on the situation to handle the selected nodes and unselected nodes
			 */
			int count = 1;
			for(Entry<String, Peer> i: interestList) {
				if(count > preferN) {
					// Not been picked, choke them
					if(unChokingMap.get(i.getKey()) != null) {
						// b.unchoke -> choke => send 'choke', remove from unchokeMap, put key in chokeMap
						String key = i.getKey();
						logging.writeLog("try to send choke to client " + key);
						if(actMsgMap.get(key) == null) { 
							throw new CustomExceptions(ErrorCode.missActMsgObj, "miss key: " + key);
						}
						if(serverConnMap.get(key) == null) {
							throw new CustomExceptions(ErrorCode.missServerConn, "miss key: " + key);
						}
						OutputStream outConn = serverConnMap.get(key).getOutputStream();
						actMsgMap.get(key).send(outConn, ActualMsg.CHOKE, 0);
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
						logging.writeLog("try to send unchoke to client " + key);
						if(actMsgMap.get(key) == null) { 
							throw new CustomExceptions(ErrorCode.missActMsgObj, "miss key: " + key);
						}
						if(serverConnMap.get(key) == null) {
							throw new CustomExceptions(ErrorCode.missServerConn, "miss key: " + key);
						}
						OutputStream outConn = serverConnMap.get(key).getOutputStream();
						actMsgMap.get(key).send(outConn, ActualMsg.UNCHOKE, 0);
						chokingMap.remove(key);
					}
					// b+d  put key in unchokeMap
					unChokingMap.put(i.getKey(), i.getValue());
				}
				count ++;
			}
			
			logging.writeLog("show chokingMap:");
			for(Entry<String, Peer> n: chokingMap.entrySet()) {
				logging.writeLog(n.getKey());
			}

			logging.writeLog("show unChokingMap:");
			for(Entry<String, Peer> n: unChokingMap.entrySet()) {
				logging.writeLog(n.getKey());
			}
			return 0;
		}

		/**
		 * send new obtain blocks to unfinished neighbors
		 * @throws IOException
		 * @throws CustomExceptions
		 */
		private void sendNewObtainList() throws IOException, CustomExceptions {
			List<Integer> obtainBlocks = sysInfo.getNewObtainBlocksCopy();
			logging.writeLog("new obtain block size: " + obtainBlocks.size());
			if(obtainBlocks.size() != 0) {
				logging.writeLog("start sending new obtain blocks");
				for(Entry<String, Peer> n: sysInfo.getNeighborMap().entrySet()) {
					String key = n.getKey();
					if(n.getValue().getHasFile()) continue;
					if(n.getValue().getIsComplete()) continue;
					if(actMsgMap.get(key) != null) { 
						OutputStream outConn = serverConnMap.get(key).getOutputStream();
						for(Integer blockIdx: obtainBlocks) {
							actMsgMap.get(key).send(outConn, ActualMsg.HAVE, blockIdx);
						}
					}
					else {
						throw new CustomExceptions(ErrorCode.missActMsgObj, "miss key: " + key);
					}
				}
			}
		}
		
		private List<Entry<String, Peer>> shuffleByRandom(List<Entry<String, Peer>> interestList) {
			logging.writeLog("start shuffleByRandom with interestList size: " + interestList.size());
			Collections.shuffle(interestList, R);
			return interestList;
		}

		private List<Entry<String, Peer>> sortByDownload(List<Entry<String, Peer>> interestList) {
			logging.writeLog("start sortByDownload with interestList size: " + interestList.size());
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
			for(Entry<String, Peer> n: interestMap.entrySet()) {
				n.getValue().setDownloadRate(R.nextDouble());
			}
		}
	}

	/** 
	*  This class will set an interval timer to make the opt unchoking decision.
	*/
	private static class OptSelect extends TimerTask {
		private final ReentrantLock lock = new ReentrantLock();
		// private final static int optUnchokingInr = sysInfo.getOptUnChokingInr();
  	private final static int optCount = 1;
		Random R = new Random();
		/**
		* OptimisticUnchokingInterval 10
		* - 1 optimistically unchoked neighbor
    */
		public void run() {

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
    }

    public void run() {
			/**
			 * Server progress
			 * 1. Handshake Msg -> success -> goes 2
			 * 2. Send BitField Msg to client 
			 * 3. Build InterestingList by interest messages
			 */
 			try {
				OutputStream outConn = connection.getOutputStream();
				InputStream inConn = connection.getInputStream();
				
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
							this.handShake.SendHandShake(outConn);
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

				if(serverConnMap.get(this.client.getId()) == null) {
					throw new CustomExceptions(ErrorCode.missServerConn, "missing connection object, recreate the socket");
				}

				if(actMsgMap.get(this.client.getId()) == null) {
					actMsgMap.put(this.client.getId(), this.actMsg);
				}

				/**
				 * this.ownBitfield is set up at FileManager constructor
				 */
				this.actMsg.send(outConn, ActualMsg.BITFIELD, fm.getOwnBitfield());
				logging.logSendBitFieldMsg(this.client);
				
				byte msg_type = -1;
				while(true) {
					msg_type = actMsg.recv(inConn);
					if(msg_type != -1) {
						boolean isEnd = reactions(msg_type);
						if(isEnd) {
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
				String trace = Tools.getStackTrace(e);
				String peerId = this.client != null ? this.client.getId() : "";
				logging.writeLog("severe", "(Server handler thread) IOException with client: " + peerId + ", ex:" + trace);
			}
			finally {
				try{
					if(this.client != null) {
						if(this.client.getIsComplete()) {
							logging.writeLog(
								"(Server handler thread) " + this.client.getId() + " client is finish, server closing connection handler with client"
							);
						}
						else {
							logging.writeLog(
								"(Server handler thread) " + this.client.getId() + " connection closing due to error, connection handler with client"
							);
						}
					}
					connection.close();
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
				 * Send back the request piece
				 * 1. read block
				 * 2. send 
				 */
				logging.logReceiveRequestMsg(this.client);
				OutputStream outConn = this.connection.getOutputStream();
				
				int blockIdx = this.actMsg.shortMsg.getBlockIdx();
				int blockLen = fm.getBlockSize(blockIdx);
				byte[] data = new byte[blockLen];
				fm.read(blockIdx, data, blockLen);

				this.actMsg.send(
					outConn, 
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
