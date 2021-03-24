package peer;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;

import utils.CustomExceptions;
import utils.LogHandler;

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
						"# %s client is connected",
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
  	private String message;    // message received from the client
		private String MESSAGE;    // uppercase message send to the client
		private Socket connection;
		private ObjectInputStream in;	//stream read from the socket
    private ObjectOutputStream out;    //stream write to the socket
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
			 * 1. Handshake Msg-> success -> goes 2
			 * 2. Send BitField Msg to client 
			 */
 			try {
				
				// initialize Input and Output streams
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());
				
				if(this.handShake == null) {
					this.handShake = new HandShake();
				}
				
				String getClientId = null;
				while(!handShake.isSuccess()) {
					// waiting for hand shake message
					getClientId = this.handShake.ReceiveHandShake(in);
				}
				
				this.client = new Peer(getClientId, null, null, null);
				logging.logHandShakeSuccess(this.server, this.client);
				this.actMsg = new ActualMsg(this.client);
				
				this.actMsg.send(out, ActualMsg.BITFIELD, fm.getOwnBitfield());
				byte msg_type = -1;
				// start receiving message from client
				while(true) {
					msg_type = actMsg.recv(in);
					reactions(msg_type);
				}
			}
			catch(CustomExceptions e){
				logging.writeLog("severe", e.toString());
			}
			catch(IOException e){
				logging.writeLog("severe", "Server thread IO exception, ex:" + e);
			}
			finally {
			// Close connections
				try{
					in.close();
					out.close();
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

			}
			else if(msg_type == ActualMsg.NOTINTERESTED) {

			}
			else if(msg_type == ActualMsg.REQUEST) {

			}
			return false;
		}	
  }
}
