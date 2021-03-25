package peer;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import utils.CustomExceptions;
import utils.ErrorCode;
import utils.LogHandler;
import utils.Tools;

public class Client extends Peer implements Runnable {

	private Peer clientPeer;
	private Peer targetHostPeer;
	private Socket requestSocket; // socket connect to the server
	private boolean tryToConnect;
	private String message; // message send to the server
	private String MESSAGE; // capitalized message read from the server

	private static LogHandler logging = new LogHandler();
	private static SystemInfo sysInfo = SystemInfo.getSingletonObj();
	private static FileManager fm = FileManager.getInstance();
	private HandShake handShake;
	private ActualMsg actMsg;

	/**
	 * Create connection to target host: targetPort.
	 * Which store in Peer object
	 * @param targetHostPeer
	 */
	public Client(Peer targetHostPeer) {
		this.clientPeer = sysInfo.getHostPeer();
		this.targetHostPeer = targetHostPeer;
		this.tryToConnect = true;
		this.handShake = null;
		this.actMsg = new ActualMsg(this.targetHostPeer);
		logging.writeLog("Create Client thread, target peer - " + this.targetHostPeer.getId());
	}

	public void run() {
		try {
			while(fm != null && !fm.isComplete()) {
				try {
					// create a socket to connect to the server
					if (this.tryToConnect) {
						requestSocket = new Socket(targetHostPeer.getHostName(), targetHostPeer.getPort());
						logging.logStartConn(clientPeer, targetHostPeer);
					}
					/** 
					 * Disable tryToConnect
					 * Initialize inputStream and outputStream
					*/
					this.tryToConnect = false;
					OutputStream outConn = requestSocket.getOutputStream();
					InputStream inConn = requestSocket.getInputStream();
					
					/**
					 * this.handShake is null for not been handshake before
					 * - Send handshake MSG
					 * - Validate handShake response
					*/
					if(this.handShake == null) {
						this.handShake = new HandShake(targetHostPeer);
											
						int retryHandShake = 0;
						while(retryHandShake < sysInfo.getRetryLimit()) {
							this.handShake.SendHandShake(outConn);
							
							logging.logSendHandShakeMsg(this.targetHostPeer.getId(), "client");
							this.handShake.ReceiveHandShake(inConn);
							
							if(this.handShake.isSuccess()) {
								logging.logHandShakeSuccess(this.clientPeer, this.targetHostPeer);
								break;
							}

							logging.writeLog(
								"warning", 
								String.format("Handshake with [%s] failed, start retry %s", targetHostPeer.getId(), retryHandShake)
							);
							Tools.timeSleep(sysInfo.getRetryInterval());
							retryHandShake++;
						}
					}
					
					byte msg_type = -1;
					while(true){
						// Receive actual msg from server
						msg_type = actMsg.recv(inConn);
						if(msg_type != -1) {
							reactions(msg_type, outConn);
						}
					}
				}
				catch (ConnectException e) {
					logging.logConnError(clientPeer, targetHostPeer);
					// recreate socket after time delay
					recreate_connection();
				}
				catch(CustomExceptions e){
					String trace = Tools.getStackTrace(e);
					logging.writeLog("severe", trace);

					recreate_connection();
				}
				catch(IOException e){
					String trace = Tools.getStackTrace(e);
					logging.writeLog("severe", "client thread IO exception, ex:" + trace);

					recreate_connection();
				}
			}

			// The file download has complete
			logging.logCompleteFile();
		}			
		finally{
			try{
				if(requestSocket != null) {
					logging.writeLog("close client connection");
					requestSocket.close();
				}
				logging.writeLog("close client thread");
			}
			catch(IOException e){
				logging.writeLog("severe", "Client close connection failed, ex:" + e);
			}
		}
	}

	/**
	 * Client thread - recreate socket connection with target host peer
	 */
	private void recreate_connection() {
		Tools.timeSleep(sysInfo.getRetryInterval());
		tryToConnect = true;
		this.handShake = null;
	}

	/**
	 * Reaction of client receiving the msg, base on the msg type 
	 * @param msg_type
	 * @param outConn
	 * @return
	 * @throws IOException
	 */
	private boolean reactions(byte msg_type, OutputStream outConn) throws IOException{
		if(msg_type == ActualMsg.BITFIELD) {
			logging.logBitFieldMsg(this.targetHostPeer);
			// update targetHostPeer's bitfield
			byte[] b = actMsg.bitfieldMsg.getBitfield();
			fm.insertBitfield(this.targetHostPeer.getId(), b, b.length);
			// send interest or not
			if(fm.isInterested(this.targetHostPeer.getId())) {
				actMsg.send(outConn, ActualMsg.INTERESTED, 0);
			}
			else {
				actMsg.send(outConn, ActualMsg.NOTINTERESTED, 0);
			}
			return true;
		}
		else if(msg_type == ActualMsg.HAVE) {

		}
		else if(msg_type == ActualMsg.CHOKE) {

		}
		else if(msg_type == ActualMsg.UNCHOKE) {

		}
		else if(msg_type == ActualMsg.PIECE) {

		}
		return false;
	}

	/**
	 * 
	 * @param args
	 */
	public static void main(String args[]) {

	}
}
