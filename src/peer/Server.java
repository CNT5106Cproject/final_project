package peer;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

import utils.CustomExceptions;
import utils.LogHandler;
import utils.Tools;

public class Server extends Thread{

	private Peer hostPeer;
	private static SystemInfo sysInfo = SystemInfo.getSingletonObj();
	private static FileManager fm = FileManager.getInstance();
	private static LogHandler logging = new LogHandler();
	private Timer PreferSelectTimer = new Timer();
	private Timer OptSelectTimer = new Timer();
	
	public Server() {
		this.hostPeer = sysInfo.getHostPeer();
	}

	public void run() {
		logging.logStartServer();
		
		try {
			ServerSocket listener = new ServerSocket(hostPeer.getPort());
			try {
				
				/**
				 * Set up the unckoking & opt peer selection mechanism
				 */
				logging.writeLog("Establishing Timer for PreferSelect with interval: " + sysInfo.getUnChokingInr()*1000 + "(ms)");
				PreferSelect taskPrefSelect = new PreferSelect();
				// OptSelect optSelect = new OptSelect();
				this.PreferSelectTimer.schedule(taskPrefSelect, 0, sysInfo.getUnChokingInr()*1000);
				// this.OptSelectTimer.schedule(optSelect, sysInfo.getOptUnChokingInr());

				int clientNum = 0;
				while(true) {
					new Handler(listener.accept(), clientNum, this.hostPeer).start();
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
		catch(IOException ex) {

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
		private final ReentrantLock lock = new ReentrantLock();
		private final static int preferN = sysInfo.getPreferN();
		ArrayList<Entry<String, Peer>> bestNeighbors;
		private ActualMsg actMsg;

		/**
		 * Select k neighbors from the interesting list
		 */
		private HashMap<String, Peer> neighborMap = sysInfo.getNeighborMap();;
		private HashMap<String, Peer> interestMap = sysInfo.getInterestMap();
		private HashMap<String, Peer> previousChokeMap = null;

		private boolean randomlySelect = false;
		Random R = new Random();

		PreferSelect() {
			this.actMsg = new ActualMsg();
		}

		/**
		 * 1. update neighbor map
		 * 2. update interest map 
		 * 3. random select | picked by download rate
		 * 4. update chokelist, send choke & unchoke message
		 */
		public void run() {
			logging.writeLog("Start PreferSelect - selecting the nodes to pass file pieces");
			if(sysInfo.getHostPeer().getHasFile() || (fm != null && fm.isComplete())) {
				randomlySelect = true;
			}
			
			testRandomDownLoadRate();

			for(Entry<String, Peer> n: neighborMap.entrySet()) {
				if(n.getValue().getIsInterested()) {
					interestMap.put(n.getKey(), n.getValue());
				}
				else if(!n.getValue().getIsInterested()) {
					interestMap.remove(n.getKey());
				}
			}

			/**
			 * Pick k interested neighbors
			 * 3. random select | picked by download rate
			 */
			if(randomlySelect) {
				logging.writeLog("selecting interested node randomly");
				if(!interestMap.isEmpty()) {
					List<Entry<String, Peer>> interestList = sortHashMapByDownload(interestMap);
					for(Entry<String, Peer> i: interestList) {
						logging.writeLog(String.format("%s download rate: %s", i.getKey(), i.getValue().getDownloadRate()));
					}
				}
			}
			else {
				logging.writeLog("selecting interested node by download rate");
				if(!interestMap.isEmpty()) {
					// Sort the interested node in order of the download rate 
					logging.writeLog("[IMPORTANT] set interestList in order by download rate");
					List<Entry<String, Peer>> interestList = sortHashMapByDownload(interestMap);
					for(Entry<String, Peer> i: interestList) {
						logging.writeLog(String.format("%s download rate: %s", i.getKey(), i.getValue().getDownloadRate()));
					}
					
					previousChokeMap = sysInfo.getChokingMap();
					sysInfo.clearChokingMap();
					int count = 1;
				
					for(Entry<String, Peer> i: interestList) {
						if(count > preferN) {
							// Not been picked, choke them
							previousChokeMap.put(i.getKey(), i.getValue());
						}
						else {
							// check if peer is choked in previous round;
							if(previousChokeMap.get(i.getKey()) == null) {
								// send unchoke;
								// this.actMsg.send(outConn, ActualMsg.BITFIELD, fm.getOwnBitfield());
							}
						}
						count ++;
					}
				}
			}
		}

		private List<Entry<String, Peer>> sortHashMapByDownload(HashMap<String, Peer> targetMap) {
			logging.writeLog("start sortHashMapByDownload with targetMap size: " + targetMap.size());
			if(targetMap.size() > 0) {
				List<Entry<String, Peer>> targetList = new ArrayList<Entry<String, Peer>>(targetMap.entrySet());
				
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
				
				Collections.sort(targetList, rate);
				return targetList;
			}
			return null;
		}
		
		/**
		 * Assign Random download rate to nodes
		 */
		private void testRandomDownLoadRate() {	
			for(Entry<String, Peer> n: neighborMap.entrySet()) {
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
		private int no;		//The index number of the client
		
		private HandShake handShake;
		private Peer server;
		private Peer client;
		private ActualMsg actMsg;

    public Handler(Socket connection, int no, Peer hostPeer) {
      this.connection = connection;
	    this.no = no;
			this.server = hostPeer;
			this.client = null;
			this.handShake = null;
			this.actMsg = null;
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
				 * this.ownBitfield is set up at FileManager constructor
				 */
				this.actMsg = new ActualMsg(this.client);
				this.actMsg.send(outConn, ActualMsg.BITFIELD, fm.getOwnBitfield());
				logging.logSendBitFieldMsg(this.client);
				
				byte msg_type = -1;
				while(true) {
					msg_type = actMsg.recv(inConn);
					if(msg_type != -1) {
						reactions(msg_type);
					}
				}
			}
			catch(CustomExceptions e){
				String trace = Tools.getStackTrace(e);
				logging.writeLog("severe", trace);
			}
			catch(IOException e){
				String trace = Tools.getStackTrace(e);
				logging.writeLog("severe", "Server thread IO exception, ex:" + trace);
			}
			finally {
			// Close connections
				try{
					connection.close();
				}
				catch(IOException e){
					logging.writeLog("severe", "Server close connection failed, ex:" + e);
				}
			}
		}

		/**
		 * Reaction of Server receiving the msg, base on the msg type 
		 * @param msg_type
		 * @return
		 * @throws IOException
		 */
		private boolean reactions(byte msg_type) throws IOException{
			if(msg_type == ActualMsg.INTERESTED) {
				logging.logReceiveInterestMsg(this.client);
				setNeighborIN(this.client.getId(), true);
			}
			else if(msg_type == ActualMsg.NOTINTERESTED) {
				logging.logReceiveNotInterestMsg(this.client);
				setNeighborIN(this.client.getId(), false);
			}
			else if(msg_type == ActualMsg.REQUEST) {

			}
			return false;
		}
		
		/**
		 * Change the neighbor peer's isInterested status in neighborMap.
		 * @param peerId
		 * @param status
		 */
		private synchronized void setNeighborIN(String peerId, boolean status) {
			Peer p = sysInfo.getNeighborMap().get(peerId);
			p.setIsInterested(status);
			sysInfo.getNeighborMap().put(peerId, p);

			Peer check = sysInfo.getNeighborMap().get(this.client.getId());
			logging.writeLog(
				"Check neighbor " + this.client.getId() + " isInterested status: " + check.getIsInterested());
		}
  }
}
