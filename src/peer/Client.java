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
					requestSocket.close();
				}
				logging.writeLog(
					"(Client thread) close client thread with: " + targetHostPeer.getId()
				);
			}
			catch(IOException e){
				logging.writeLog("severe", "Client close connection failed, ex:" + e);
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
			logging.logReceiveBitFieldMsg(this.targetHostPeer);
			// update targetHostPeer's bitfield
			byte[] b = actMsg.bitfieldMsg.getBitfield();
			fm.insertBitfield(this.targetHostPeer.getId(), b, b.length);
			// send interest or not
			if(fm.isOthersFinish(this.targetHostPeer.getId())) {
				logging.writeLog("(client) notify that peer " + this.targetHostPeer.getId() + " isComplete");
				this.targetHostPeer.setIsComplete();
				sysInfo.getNeighborMap().put(this.targetHostPeer.getId(), this.targetHostPeer);
			}
			if(fm.isInterested(this.targetHostPeer.getId())) {
				actMsg.send(outConn, ActualMsg.INTERESTED, 0);
			}
			else {
				actMsg.send(outConn, ActualMsg.NOTINTERESTED, 0);
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
			if(fm.isInterested(this.targetHostPeer.getId())) {
				actMsg.send(outConn, ActualMsg.INTERESTED, 0);
			}
			else {
				actMsg.send(outConn, ActualMsg.NOTINTERESTED, 0);
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
			/**
			 * 1.choke -> unchoke 
			 * 		=> set choke to unchoke -> start sending pieces msg until been choked
			 * 2. unchoke -> unchoke
			 * 		=> continue sending pieces message 
			 */
			if(this.clientPeer.getIsComplete()) return false;
			if(clientPeer.getIsChoking()) {
				clientPeer.setUnChoking();
			}
			requestingPiece(this.targetHostPeer, outConn);
		}
		else if(msg_type == ActualMsg.PIECE) {
			logging.logReceivePieceMsg(this.targetHostPeer);
			int blockIdx = this.actMsg.pieceMsg.blockIdx;
			int blockLen = fm.getBlockSize(blockIdx);
			int isError = fm.write(blockIdx, this.actMsg.pieceMsg.getData(), blockLen);
			if(isError == -1) {
				logging.writeLog("unable write block " + blockIdx + " from " + this.targetHostPeer.getId());
				return false;
			}
			logging.logDownload(this.targetHostPeer, blockIdx, fm.getOwnBitfieldSize());
			sysInfo.addNewObtainBlocks(blockIdx);
			
			if(isClientComplete() && !this.clientPeer.getIsComplete()) {
				// The file download has complete
				logging.logCompleteFile();
				this.clientPeer.setIsComplete();
				logging.writeLog("send END msg to all server, isComplete = true, close connection with: " + targetHostPeer.getId());
				// send end message to all server
				sendEndMessageToAll();
				return true;
			}

			// TODO add piece to new obtain list 
			if(clientPeer.getIsChoking()) {
				logging.writeLog("unable continue requesting, peer has been choked");
				return false;
			}
			Tools.timeSleep(500);
			requestingPiece(this.targetHostPeer, outConn);
		}
		return true;
	}

	/**
	 * 1. request piece
	 * @param sender
	 * @param outConn
	 * @return
	 * @throws IOException
	 */
	private int requestingPiece(Peer sender, OutputStream outConn) throws IOException {
		int requestBlockIdx = fm.pickInterestedFileBlock(sender.getId());
		if(requestBlockIdx == -1) {
			logging.writeLog("requestingPiece stop, no interested block"); 
			return -1;
		} 
		this.actMsg.send(outConn, ActualMsg.REQUEST, requestBlockIdx);
		return 0;
	}

	private void sendEndMessageToAll() throws IOException {
		logging.writeLog("send END msg to # " + sysInfo.getClientConnMap().size() + " servers");
		for(Entry<String, Socket> conn: sysInfo.getClientConnMap().entrySet()) {
			logging.logSendEndMsg(conn.getKey());
			OutputStream outConn = conn.getValue().getOutputStream();
			this.actMsg.send(outConn, ActualMsg.END, 0);
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
