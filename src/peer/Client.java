package peer;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import utils.LogHandler;

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

	/**
	 * Create connection to target host: targetPort
	 * 
	 * @param targetHostPeer
	 */
	public Client(Peer targetHostPeer) {
		this.clientPeer = sysInfo.getHostPeer();
		this.targetHostPeer = targetHostPeer;
		this.tryToConnect = true;
	}

	private void createSocket() {
		try {
			requestSocket = new Socket(targetHostPeer.getHostName(), targetHostPeer.getPort());
			logging.logStartConn(clientPeer, targetHostPeer);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			// create a socket to connect to the server
			if (tryToConnect) {
				createSocket();
			}

			/** 
			 * Handshake and disable tryToConnect
			*/
			tryToConnect = false;
			
			// initialize inputStream and outputStream
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());
			// TODO Handshake
			message = String.format("I am node: %s, nice to meet you!", targetHostPeer.getId());
			sendMessage(message);

			while (true) {
				// Receive the upperCase sentence from the server
				MESSAGE = (String) in.readObject();
				// show the message to the user
				logging.writeLog(String.format("Receive msg from target host [%s], msg: %s", 
					targetHostPeer.getId(),
					MESSAGE
				));
			}
		} catch (ConnectException e) {
			logging.logConnError(clientPeer, targetHostPeer);
			try {
				TimeUnit.SECONDS.sleep(sysInfo.getRetryInterval());
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			tryToConnect = true;
		} 
		catch (ClassNotFoundException e) {
			String msg = String.format(
				"Read Buffer occurs ClassNotFoundException in connection with [%s], ex: [%s]", 
				targetHostPeer.getId(),
				e
			);
      logging.writeLog("severe", msg);
    } 
		catch(UnknownHostException unknownHost){
			String msg = "Connecting to invalid host";
			logging.writeLog("severe", msg + ", ex: " + unknownHost);
		}
		catch(IOException e){
			String msg = String.format(
				"IOException occurs in connection with [%s], ex: [%s]", 
				targetHostPeer.getId(), 
				e.getMessage()
			);
			logging.writeLog("severe", msg);
			tryToConnect = true;
		}
		finally{
			// Close connections
			try{
				in.close();
				out.close();
				requestSocket.close();
			}
			catch(IOException ioException){
				ioException.printStackTrace();
				
			}
		}
	}

	//send a message to the output stream
	void sendMessage(String msg)
	{
		try{
			//stream write the message
			out.writeObject(msg);
			out.flush();
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String args[]) {

	}
}
