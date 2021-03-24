package peer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import utils.CustomExceptions;
import utils.ErrorCode;
import utils.LogHandler;

public class HandShake implements Serializable {

	private static final long serialVersionUID = -1L;
	private static final String Header = "P2PFILESHARINGPROJ"; 
	private byte[] zeroBits = new byte[10];
	private final String peerMsgHeader;
	private String peerID; // self-peer ID 
	private String targetPeerID; // the receiver of the msg
  private boolean success; // success flag for handshake

	private static LogHandler logging = new LogHandler();
	private static SystemInfo sysInfo = SystemInfo.getSingletonObj();
	
	/**
	 * Client's handShake constructor
	 * @param targetPeer - handshake msg receiver (Peer object)
	 */
	public HandShake(Peer targetPeer) {
		super();
		// Get self-peer info from SystemInfo Singleton -> self-peer Id & peer's neighborList;
		this.peerID = sysInfo.getHostPeer().getId(); 
		this.targetPeerID = targetPeer.getId();
		this.peerMsgHeader = getHeader();
		this.success = false;
	}

	/**
	 * Server's handShake constructor
	 * 1. Waiting client to send handshake
	 * 2. Set targetPeerId by receiving msg
	 */
	public HandShake() {
		super();
		this.peerID = sysInfo.getHostPeer().getId(); 
		this.peerMsgHeader = getHeader();
		this.targetPeerID = null;
		this.success = false;
	}

	public byte[] getZeroBits() {
		return zeroBits;
	}

	public void setZeroBits(byte[] zeroBits) {
		this.zeroBits = zeroBits;
	}
	public String getPeerID() {
		return peerID;
	}
	public void setPeerID(String peerID) {
		this.peerID = peerID;
	}
	
	public String getPeerMsgHeader() {
		return peerMsgHeader;
	}
	
	public static String getHeader() {
		return Header;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		/** Change the header to invalid one*/
		return sb.append("[Header :").append("ABCDE").append("]\n").append("[Zero Bits :").append(getZeroBits()).append("[Peer ID: ").append(this.peerID).append("]")
				.toString();
	}

	public void SendHandShake(OutputStream out) throws IOException {
		ObjectOutputStream opStream = new ObjectOutputStream(out);
		opStream.writeObject(this);
		opStream.flush();
		logging.logSendHandShakeMsg(this.targetPeerID);
	}

	public String ReceiveHandShake(InputStream in) throws IOException, CustomExceptions{
		try {
			ObjectInputStream ipStream = new ObjectInputStream(in);
			HandShake Response = (HandShake) ipStream.readObject();
			logging.writeLog(
				String.format(
				"Peer [%s] receive handshake msg [%s]",
				this.peerID,
				Response.toString()
			));
			checkHeader(Response.peerMsgHeader, Response.peerID);
			isNeighbor(Response.peerID);
			setSuccess();
			return Response != null ? Response.peerID : null;
		}
		catch(ClassNotFoundException e){
			logging.writeLog("severe", "read input stream exception, ex:" + e);
		}
		return null;
	}
	
	/**
	 *
	 * @param checkId
	 * @return
	 */
	private boolean isNeighbor(String checkId) throws CustomExceptions {
		logging.writeLog(
			String.format(
				"Peer [%s] check if [%s] is neighbor", 
				this.peerID,
				checkId
			)
		);

		for (Peer p: sysInfo.getPeerList()) {
			if(p.getId().equals(checkId)) {
				return true;
			}
		}
		
		throw new CustomExceptions(
			ErrorCode.failHandshake, 
			String.format(
				"Peer [%s] Handshake Failed, Peer [%s] is not neighbor", 
				this.peerID, 
				checkId
			)
		);
		// return sysInfo.getPeerList().contains(checkId);
	}

	private boolean checkHeader(String receiveHeader, String senderId) throws CustomExceptions {
		logging.writeLog(
			String.format(
				"Peer [%s] receive Handshake header %s from [%s]", 
				this.peerID,
				receiveHeader,
				senderId
			));
		
		/**
		 * TODO 
		 * check header, the receiveHeader should not be set static variable in Response 
		 * it should be deserialize from input stream
		 */
		/** this code is incorrect */
		if(receiveHeader.equals("P2PFILESHARINGPROJ")) {
			logging.writeLog(
				String.format(
					"Peer [%s] receive Handshake success from [%s]", 
					this.peerID,
					senderId
				));
			return true;
		}

		throw new CustomExceptions(
			ErrorCode.failHandshake, 
			String.format(
				"Peer [%s] Handshake Failed, Peer [%s] is not neighbor",
				this.peerID, 
				senderId
			)
		);
	}

	private void setSuccess() {
		this.success = true;
	}

	public boolean isSuccess() {
		return this.success;
	}

}
