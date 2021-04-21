package peer;

import java.net.*;
import java.io.*;
import java.util.Map.Entry;
import utils.CustomExceptions;
import utils.ErrorCode;
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
	private boolean isClientComplete = false;
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
		this.requestSocket = null;
		this.tryToConnect = true;
		this.handShake = null;
		this.actMsg = new ActualMsg(this.targetHostPeer);
		logging.writeLog("Create Client thread, target peer - " + this.targetHostPeer.getId());
	}

	public void run() {
		while(!isClientComplete) {
			try {
				// create a socket to connect to the server
				if (this.tryToConnect) {
					logging.logStartConn(clientPeer, targetHostPeer);
					this.requestSocket = new Socket(targetHostPeer.getHostName(), targetHostPeer.getPort());
					sysInfo.getClientConnMap().put(targetHostPeer.getId(), this.requestSocket);
					
					this.opStream = new ObjectOutputStream(requestSocket.getOutputStream());
					sysInfo.getClientOpStream().put(targetHostPeer.getId(), this.opStream);
					
					sysInfo.getClientActMsgMap().put(targetHostPeer.getId(), this.actMsg);
					this.inStream = new ObjectInputStream(requestSocket.getInputStream());
				}
			
				if(sysInfo.getClientConnMap().get(targetHostPeer.getId()) == null) {
					throw new CustomExceptions(ErrorCode.missClientConn, "missing client connection object, recreate the socket");
				}

				if(sysInfo.getClientOpStream().get(targetHostPeer.getId()) == null) {
					throw new CustomExceptions(ErrorCode.missClientOpStream, "missing client opStream, recreate the socket");
				}

				if(sysInfo.getClientActMsgMap().get(targetHostPeer.getId()) == null) {
					throw new CustomExceptions(ErrorCode.missActMsgObj, "missing actMsgMap, recreate the socket");
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
					this.handShake = new HandShake(HandShake.HANDSHAKE_HEADER, this.targetHostPeer.getId());
						
					while(true) {
						HandShake.SendHandshake(requestSocket.getOutputStream(), HandShake.encodeMessage(this.handShake));
						
						logging.logSendHandShakeMsg(this.targetHostPeer.getId(), "client");
						String serverPeerId = HandShake.ReceiveHandshake(inStream);
						
						if(serverPeerId != null) {
							logging.logHandShakeSuccess(this.clientPeer, this.targetHostPeer);
							break;
						}

						logging.writeLog("warning", String.format("Handshake with [%s] failed, start retry", targetHostPeer.getId()));
						Tools.timeSleep(sysInfo.getRetryInterval());
					}
				}
				
				byte msg_type = -1;
				while(!isClientComplete){
					// Receive msg from server
					startReadTime = System.nanoTime();
					msg_type = actMsg.recv(inStream);
					if(msg_type != -1) {
						reactions(msg_type);
					}
					if(isClientComplete) break;
				}
			}
			catch (ConnectException e) {
				String trace = Tools.getStackTrace(e);
				logging.logConnError(clientPeer, targetHostPeer);
				logging.writeLog("warning", 
					"(Client thread) ConnectException with client: " + targetHostPeer.getId() + ", ex:" + trace
				);
			}
			catch(CustomExceptions e){
				String trace = Tools.getStackTrace(e);
				logging.writeLog("warning", 
					"(Client thread) CustomExceptions with client: " + targetHostPeer.getId() + ", ex:" + trace
				);
			}
			catch(IOException e){
				String trace = Tools.getStackTrace(e);
				logging.writeLog("warning", 
					"(Client thread) IOException with client: " + targetHostPeer.getId() + ", ex:" + trace
				);
			}
			catch(Exception e) {
				String trace = Tools.getStackTrace(e);
				logging.writeLog("warning", 
					"(Client thread) Exception with client: " + targetHostPeer.getId() + ", ex:" + trace
				);
			}
			finally{
				try {
					logging.writeLog(
						"(Client thread) client final block: " + targetHostPeer.getId() + ", check close client flag: " + isClientComplete
					);
					if(requestSocket != null) {
						logging.writeLog(
							"(Client thread) close client connection with client: " + targetHostPeer.getId() + ", check close client flag: " + isClientComplete
						);
						requestSocket.close();
						requestSocket = null;
					}
				}
				catch(IOException e){
					String trace = Tools.getStackTrace(e);
					logging.writeLog("severe", "client close connection failed, with " + targetHostPeer.getId() + ",ex: "+ trace);
				}
			}
			if(!isClientComplete) {
				recreate_connection();
				removeCommunicateObjects();
			}
		}
		removeCommunicateObjects();
		return;
	}

	/**
	 * Client thread - recreate socket connection with target host peer
	 */
	private void recreate_connection() {
		logging.writeLog("warning", "RECONNECTING Peer [" + targetHostPeer.getId() + "]" );
		Tools.timeSleep(sysInfo.getRetryInterval());
		tryToConnect = true;
		this.requestSocket = null;
		this.handShake = null;
		this.opStream = null;
		this.inStream = null;
	}

	private void removeCommunicateObjects() {
		if(sysInfo.getClientConnMap().get(targetHostPeer.getId()) != null) {
			try {
				sysInfo.getClientConnMap().get(targetHostPeer.getId()).close();
			}
			catch (IOException e) {
				String trace = Tools.getStackTrace(e);
				logging.writeLog("removeCommunicateObjects getClientConnMap failed, ex:" + trace);
			}
			sysInfo.getClientConnMap().remove(targetHostPeer.getId());
		}

		if(sysInfo.getClientOpStream().get(targetHostPeer.getId()) != null) {
			try {
				sysInfo.getClientOpStream().get(targetHostPeer.getId()).close();
			}
			catch (IOException e) {
				String trace = Tools.getStackTrace(e);
				logging.writeLog("removeCommunicateObjects getClientOpStream failed, ex:" + trace);
			}	
			sysInfo.getClientOpStream().remove(targetHostPeer.getId());
		}

		if(sysInfo.getClientActMsgMap().get(targetHostPeer.getId()) != null) {
			sysInfo.getClientActMsgMap().remove(targetHostPeer.getId());
		}
		
		try {
			if(this.inStream != null) {
				this.inStream.close();
			}
		}
		catch (IOException e) {
			String trace = Tools.getStackTrace(e);
			logging.writeLog("removeCommunicateObjects inStream failed, ex:" + trace);
		}
	}
	/**
	 * Reaction of client receiving the msg, base on the msg type 
	 * @param msg_type
	 * @param outConn
	 * @return true -> isEnd
	 * @throws IOException
	 */
	private boolean reactions(byte msg_type) throws IOException{

		if(msg_type == ActualMsg.COMPLETE) {
			logging.logReceiveBackCompleteMsg(targetHostPeer);
			// close client
			isClientComplete = true;
			sysInfo.getIsClientCompleteMap().put(targetHostPeer.getId(), isClientComplete);
		}

		if(isDownloadComplete()) {
			logging.writeLog("peer already complete");
			return true;
		}

		if(msg_type == ActualMsg.BITFIELD) {
			logging.logReceiveBitFieldMsg(this.targetHostPeer);
			// update targetHostPeer's bitfield
			byte[] b = actMsg.bitfieldMsg.getBitfield();
			fm.insertBitfield(this.targetHostPeer.getId(), b, b.length);

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
			
			if(isDownloadComplete()) {
				logging.logCompleteFile();
				broadcastComplete t = new broadcastComplete();
				t.start();
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
	
	public void closeAllClientThread() throws IOException {
		logging.writeLog("(client thread) close all client thread # " + sysInfo.getClientConnMap().size());
		for(Entry<String, Socket> conn: sysInfo.getClientConnMap().entrySet()) {
			try {
				logging.writeLog("close client thread: " + conn.getKey());
				conn.getValue().close();
			}
			catch(IOException e) {
				String trace = Tools.getStackTrace(e);
				logging.writeLog("close client thread, ex: " + trace);
			}
		}
		sysInfo.getClientConnMap().clear();
	}

	private boolean isDownloadComplete(){
		if(fm != null && fm.isComplete()) return true;
		return false;
	}

	private static class broadcastComplete extends Thread {
		public void run() {
			logging.writeLog("start broadcastComplete");
			while(true) {
				if(isAllClientComplete()) {
					logging.writeLog("end broadcastComplete");
					break;
				}
				sendCompleteMessageToAll();
				Tools.timeSleep(sysInfo.getRetryInterval());
			}
		}

		private boolean isAllClientComplete() {
			logging.writeLog("check isAllClientComplete?");
			for(Entry<String, Peer> n: sysInfo.getNeighborMap().entrySet()) {
				String peerID = n.getKey();
				if(sysInfo.getIsClientCompleteMap().get(peerID) == null) return false;
			}
			return true;
		}

		private void sendCompleteMessageToAll(){
			logging.writeLog("send COMPLETE msg to # " + sysInfo.getClientOpStream().size() + " servers");
			for(Entry<String, ObjectOutputStream> conn: sysInfo.getClientOpStream().entrySet()) {
				String peerID = conn.getKey();
				logging.logSendCompleteMsg(peerID);
				try {
					if(sysInfo.getClientActMsgMap().get(peerID) != null) {
						sysInfo.getClientActMsgMap().get(peerID).send(conn.getValue(), ActualMsg.COMPLETE, 0);
					}
					else {
						logging.writeLog("missing ActMsg obj for " + peerID);
					}
				}
				catch(IOException e) {
				}
			}
		}
	}

	/**
	 * 
	 * @param args
	 */
	public static void main(String args[]) {

	}
}
