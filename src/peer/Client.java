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
	private ObjectOutputStream out; // stream write to the socket
	private ObjectInputStream in; // stream read from the socket
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
					if (tryToConnect) {
						requestSocket = new Socket(targetHostPeer.getHostName(), targetHostPeer.getPort());
						logging.logStartConn(clientPeer, targetHostPeer);
					}
					/** 
					 * Disable tryToConnect
					 * Initialize inputStream and outputStream
					*/
					tryToConnect = false;
					out = new ObjectOutputStream(requestSocket.getOutputStream());
					out.flush();
					in = new ObjectInputStream(requestSocket.getInputStream());
					
					/**
					 * this.handShake is null for not been handshake before
					 * - Send handshake MSG
					 * - Validate handShake response
					*/
					if(this.handShake == null) {
						this.handShake = new HandShake(targetHostPeer);
					}
					
					int retryHandShake = 0;
					while(retryHandShake < sysInfo.getRetryLimit()) {
						this.handShake.SendHandShake(out);
						this.handShake.ReceiveHandShake(in);
						
						if(this.handShake.isSuccess()) {
							break;
						}
						logging.writeLog(
							"warning", 
							String.format("Handshake with [%s] failed, start retry %s", targetHostPeer.getId(), retryHandShake)
						);
						Tools.timeSleep(sysInfo.getRetryInterval());
						retryHandShake++;
					}

					if(!this.handShake.isSuccess()) {
						throw new CustomExceptions(
							ErrorCode.failHandshake, 
							String.format("Handshake Failed between Peer [%s] to Peer [%s] ", sysInfo.getHostPeer(), this.targetHostPeer)
						);
					}
					
					logging.writeLog("Handshake Success between Peer [%s] to Peer [%s], receiving file process start");
					while(true){
						// Receive actual msg from server
						actMsg.recv(in);
					}
				}
				catch (ConnectException e) {
					logging.logConnError(clientPeer, targetHostPeer);
					// recreate socket after time delay
					Tools.timeSleep(sysInfo.getRetryInterval());
					tryToConnect = true;
					this.handShake = null;
				}
			}
		}
		catch(Exception e){
			String msg = String.format(
				"Exception occurs in connection with [%s], ex: [%s]", 
				targetHostPeer.getId(), 
				e.getMessage()
			);
			logging.writeLog("severe", msg);
		}
		finally{
			// Close connections
			try{
				in.close();
				out.close();
				requestSocket.close();
			}
			catch(IOException e){
				logging.writeLog("severe", "Client close connection failed, ex:" + e);
			}
		}
	}

	//send a message to the output stream
	void sendMessage(String msg) {
		try {
			//stream write the message
			out.writeObject(msg);
			out.flush();
		}
		catch(IOException ioException){
			logging.writeLog("severe", "client send message failed, ex:" + ioException);
		}
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String args[]) {

	}
}
