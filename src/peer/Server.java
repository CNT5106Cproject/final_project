package peer;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;

import utils.CustomExceptions;
import utils.LogHandler;
import utils.Tools;

public class Server extends Thread{

	private Peer hostPeer;
	private static SystemInfo sysInfo = SystemInfo.getSingletonObj();
	private static FileManager fm = FileManager.getInstance();
	private static LogHandler logging = new LogHandler();

	public Server() {
		Peer hostPeer = sysInfo.getHostPeer();
		this.hostPeer = hostPeer;
	}

	public Server(Peer peer) {
		this.hostPeer = peer;
	}

	public void run() {
		logging.logStartServer();
		
		try {
			ServerSocket listener = new ServerSocket(hostPeer.getPort());
			try {
				int clientNum = 1;
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
	 *  This class will set an interval timer to make the unchoking decision.
	*/
	private static class ChokingMechanism extends Thread {
		
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
		public boolean reactions(byte msg_type) throws IOException{
			if(msg_type == ActualMsg.INTERESTED) {
				logging.logReceiveInterestMsg(this.client);
			}
			else if(msg_type == ActualMsg.NOTINTERESTED) {

			}
			else if(msg_type == ActualMsg.REQUEST) {

			}
			return false;
		}	
  }
}
