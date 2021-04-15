package peer;

import java.net.*;
import java.io.*;
import java.util.Map.Entry;
import utils.CustomExceptions;
import utils.LogHandler;
import utils.Tools;

public class Client extends Peer implements Runnable {

	private Peer clientPeer;
	private Peer targetHostPeer;
	private Socket requestSocket; // socket connect to the server
	private boolean tryToConnect;

	private static LogHandler logging = new LogHandler();
	private static SystemInfo sysInfo = SystemInfo.getSingletonObj();
	private static FileManager fm = FileManager.getInstance();
	private HandShake handShake;
	private ActualMsg actMsg;
	private ObjectOutputStream opStream = null;
	private ObjectInputStream inStream = null;
	
	/**
	 * Use to count download rate
	 */
	private double downloadRate = 0; // MegabytesPerSec
	private long startReadTime = 0;

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
			while(!this.clientPeer.getIsComplete()) {
				try {
					// create a socket to connect to the server
					if (this.tryToConnect) {
						requestSocket = new Socket(targetHostPeer.getHostName(), targetHostPeer.getPort());
						sysInfo.getClientConnMap().put(targetHostPeer.getId(), requestSocket);
						
						opStream = new ObjectOutputStream(requestSocket.getOutputStream());
						sysInfo.getClientOpStream().put(targetHostPeer.getId(), opStream);
						inStream = new ObjectInputStream(requestSocket.getInputStream());
						
						logging.logStartConn(clientPeer, targetHostPeer);
					}
					/** 
					 * Disable tryToConnect
					 * Initialize inputStream and outputStream
					*/
					this.tryToConnect = false;
					
					/**
					 * this.handShake is null for not been handshake before
					 * - Send handshake MSG
					 * - Validate handShake response
					*/
					if(this.handShake == null) {
						this.handShake = new HandShake(targetHostPeer);
											
						int retryHandShake = 0;
						while(retryHandShake < sysInfo.getRetryLimit()) {
							this.handShake.SendHandShake(this.opStream);
							
							logging.logSendHandShakeMsg(this.targetHostPeer.getId(), "client");
							this.handShake.ReceiveHandShake(inStream);
							
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
						// Receive msg from server
						startReadTime = System.nanoTime();
						msg_type = actMsg.recv(inStream);
						if(msg_type != -1) {
							boolean isComplete = reactions(msg_type);
							if(isComplete) {
								// jump to close connections with client
								break;
							}
						}
					}
				}
				catch (ConnectException e) {
					if(this.clientPeer.getIsComplete()) {
						logging.writeLog(
							"(Client thread) connection close by server " + targetHostPeer.getId() + ", client peer is completed"
						);
						return;
					}
					else {
						String trace = Tools.getStackTrace(e);
						logging.logConnError(clientPeer, targetHostPeer);
						logging.writeLog("warning", 
							"(Client thread) ConnectException with client: " + targetHostPeer.getId() + ", ex:" + trace
						);
						recreate_connection(targetHostPeer);
					}
				}
				catch(CustomExceptions e){
					String trace = Tools.getStackTrace(e);
					logging.writeLog("warning", 
						"(Client thread) CustomExceptions with client: " + targetHostPeer.getId() + ", ex:" + trace
					);
					recreate_connection(targetHostPeer);
				}
				catch(IOException e){
					if(this.clientPeer.getIsComplete()) {
						logging.writeLog(
							"(Client thread) connection close by server " + targetHostPeer.getId() + ", client peer is completed"
						);
						return;
					}
					else {
						String trace = Tools.getStackTrace(e);
						logging.writeLog("warning", 
							"(Client thread) IOException with client: " + targetHostPeer.getId() + ", ex:" + trace
						);
						recreate_connection(targetHostPeer);
					}
				}
			}
		}
		finally{
			try{
				if(requestSocket != null) {
					logging.writeLog(
						"(Client thread) close client connection with client: " + targetHostPeer.getId()
					);
					removeCommunicateObjects();

					requestSocket.close();
				}
				logging.writeLog(
					"(Client thread) close client thread with: " + targetHostPeer.getId()
				);
			}
			catch(IOException e){
				String trace = Tools.getStackTrace(e);
				logging.writeLog("severe", "Client close connection failed, ex:" + trace);
			}
		}
	}

	/**
	 * Client thread - recreate socket connection with target host peer
	 */
	private void recreate_connection(Peer targetHostPeer) {
		logging.writeLog("warning", "RECONNECTING Peer [" + targetHostPeer.getId() + "]" );
		Tools.timeSleep(sysInfo.getRetryInterval());
		tryToConnect = true;
		this.handShake = null;
		removeCommunicateObjects();
	}

	private void removeCommunicateObjects() {
		try {
			if(this.opStream != null) {
				this.opStream.close();
			}
			if(this.inStream != null) {
				this.inStream.close();
			}
		}
		catch (IOException e) {
			String trace = Tools.getStackTrace(e);
			logging.writeLog("severe", "removeCommunicateObjects failed, ex:" + trace);
		}
		if(sysInfo.getClientOpStream().get(targetHostPeer.getId()) != null) {
			sysInfo.getClientOpStream().remove(targetHostPeer.getId());
		}
		if(sysInfo.getClientConnMap().get(targetHostPeer.getId()) != null) {
			sysInfo.getClientConnMap().remove(targetHostPeer.getId());
		}
	}

	private synchronized void processClosingClient() throws IOException{
		this.clientPeer.setIsComplete();
		logging.writeLog("send COMPLETE msg to all server, isComplete = true, close connection with: " + targetHostPeer.getId());
		// send complete message to all server
		sendCompleteMessageToAll();
	}
	/**
	 * Reaction of client receiving the msg, base on the msg type 
	 * @param msg_type
	 * @param outConn
	 * @return true -> isEnd
	 * @throws IOException
	 */
	private boolean reactions(byte msg_type) throws IOException{

		if(msg_type == ActualMsg.BITFIELD) {
			logging.logReceiveBitFieldMsg(this.targetHostPeer);
			// update targetHostPeer's bitfield
			byte[] b = actMsg.bitfieldMsg.getBitfield();
			fm.insertBitfield(this.targetHostPeer.getId(), b, b.length);

			/**
			 * Notice other peer is complete after receiving bitfield
			 */
			if(fm.isOthersFinish(this.targetHostPeer.getId())) {
				logging.writeLog("(client) notify that peer " + this.targetHostPeer.getId() + " isComplete");
				this.targetHostPeer.setIsComplete();
				sysInfo.getNeighborMap().put(this.targetHostPeer.getId(), this.targetHostPeer);
			}

			// send interest or not
			if(fm.isInterested(this.targetHostPeer.getId())) {
				actMsg.send(opStream, ActualMsg.INTERESTED, 0);
			}
			else {
				actMsg.send(opStream, ActualMsg.NOTINTERESTED, 0);
			}
		}
		else if(msg_type == ActualMsg.HAVE) {
			logging.logReceiveHaveMsg(this.targetHostPeer);

			int blockIdx = this.actMsg.shortMsg.getBlockIdx();
			logging.writeLog(String.format("%s send have to notify obtaining new block: %s", 
				this.targetHostPeer.getId(),
				blockIdx
			));
			fm.updateHave(this.targetHostPeer.getId(), blockIdx);

			/**
			 * Notice other peer is complete after receiving have
			 */
			if(fm.isOthersFinish(this.targetHostPeer.getId())) {
				logging.writeLog("(client) notify that peer " + this.targetHostPeer.getId() + " isComplete");
				this.targetHostPeer.setIsComplete();
				sysInfo.getNeighborMap().put(this.targetHostPeer.getId(), this.targetHostPeer);
			}

			if(fm.isInterested(this.targetHostPeer.getId())) {
				actMsg.send(opStream, ActualMsg.INTERESTED, 0);
			}
			else {
				actMsg.send(opStream, ActualMsg.NOTINTERESTED, 0);
			}
		}
		else if(msg_type == ActualMsg.CHOKE) {
			logging.logChoking(this.targetHostPeer);
			if(!clientPeer.getIsChoking()) {
				clientPeer.setChoking();
			}
		}
		else if(msg_type == ActualMsg.UNCHOKE) {
			logging.logUnchoking(this.targetHostPeer);

			if(this.clientPeer.getIsComplete()) return false;
			if(isClientComplete() && !this.clientPeer.getIsComplete()) {
				processClosingClient();
				return true;
			}
			/**
			 * 1.choke -> unchoke 
			 * 		=> set choke to unchoke -> start sending pieces msg until been choked
			 * 2. unchoke -> unchoke
			 * 		=> continue sending pieces message 
			 */

			if(clientPeer.getIsChoking()) {
				clientPeer.setUnChoking();
			}
			requestingPiece(this.targetHostPeer);
		}
		else if(msg_type == ActualMsg.PIECE) {
			/**
			 * 1. Set download rate
			 * 2. write block into file
			 * 3. add block to new obtain blocks list
			 * 
			 * 4. check if complete 
			 * 		-> YES then start closing procsess
			 * 		-> NO, continue
			 * 5. if not choked -> continue requesting another piece
			 */

			logging.logReceivePieceMsg(this.targetHostPeer);
			double spendSec = (double)(System.nanoTime() - this.startReadTime) / 1_000_000_000.0;
			// MegabytesPerSec
			this.downloadRate = (this.actMsg.pieceMsg.getMsgLen() / spendSec) / 1_000_000.0;
			setDownloadRate();

			int blockIdx = this.actMsg.pieceMsg.blockIdx;
			int blockLen = fm.getBlockSize(blockIdx);
			int isError = fm.write(blockIdx, this.actMsg.pieceMsg.getData(), blockLen);
			if(isError == -1) {
				logging.writeLog("unable write block " + blockIdx + " from " + this.targetHostPeer.getId());
				return false;
			}
			logging.logDownload(this.targetHostPeer, blockIdx, fm.getOwnBitfieldSize());

			sysInfo.addNewObtainBlocks(blockIdx);
			
			if(isClientComplete()) {
				logging.logCompleteFile();
				processClosingClient();
				return true;
			}

			if(clientPeer.getIsChoking()) {
				logging.writeLog("unable continue requesting, peer has been choked");
				return false;
			}
			Tools.timeSleep(200);
			requestingPiece(this.targetHostPeer);
		}
		return false;
	}
	
	private void setDownloadRate() {
		Peer p = sysInfo.getNeighborMap().get(targetHostPeer.getId());
		p.setDownloadRate(this.downloadRate);
		this.downloadRate = 0;
	}
	/**
	 * 1. request piece
	 * @param sender
	 * @param outConn
	 * @return
	 * @throws IOException
	 */
	private int requestingPiece(Peer sender) throws IOException {
		int requestBlockIdx = fm.pickInterestedFileBlock(sender.getId());
		if(requestBlockIdx == -1) {
			logging.writeLog("requestingPiece stop, no interested block"); 
			return -1;
		} 
		this.actMsg.send(opStream, ActualMsg.REQUEST, requestBlockIdx);
		return 0;
	}

	private void sendCompleteMessageToAll() throws IOException {
		logging.writeLog("send COMPLETE msg to # " + sysInfo.getClientOpStream().size() + " servers");
		for(Entry<String, ObjectOutputStream> conn: sysInfo.getClientOpStream().entrySet()) {
			logging.logSendCompleteMsg(conn.getKey());
			this.actMsg.send(conn.getValue(), ActualMsg.COMPLETE, 0);
		}
	}
	
	boolean isClientComplete(){
		if(fm != null && fm.isComplete()) return true;
		return false;
	}
	/**
	 * 
	 * @param args
	 */
	public static void main(String args[]) {

	}
}
