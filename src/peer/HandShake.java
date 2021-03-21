package peer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import utils.LogHandler;

public class HandShake implements Serializable {

	//private static final long serialVersionUID = -1482860868859618509L;
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
		return sb.append("[Header :").append(getHeader()).append("]\n").append("[Zero Bits :").append(getZeroBits()).append("[Peer ID: ").append(this.peerID).append("]")
				.toString();
	}


	public void SendHandShake(OutputStream out) throws IOException {
		ObjectOutputStream opStream = new ObjectOutputStream(out);
		opStream.writeObject(this);
		opStream.flush();
		logging.logSendHandShakeMsg(this.targetPeerID);
	}
	public String ReceiveHandShake(InputStream in) throws IOException {
		try {
			ObjectInputStream ipStream = new ObjectInputStream(in);
			HandShake Response = (HandShake) ipStream.readObject();
			
			setSuccess();
			return Response != null ? Response.peerID : null;
		} 
		catch(ClassNotFoundException e){
			logging.writeLog("severe", "read input stream exception, ex:" + e);
		}
		catch (Exception e) {
			logging.writeLog("severe", String.format("Error, Receive handshake from [%s]", this.targetPeerID));
			System.out.println(e);
		}
		return null;
	}
	public boolean checkHeader(SystemInfo s) {
		List<Peer> peers = s.getPeerList();

		for (Peer p: peers) {
			//if () {}      checking for header
			if(p.getId() != this.getPeerId()) {
				return false;
			}
		}
		this.setSuccess();
		return true;
	}

	private void setSuccess() {
		this.success = true;
	}

	
	public boolean isSuccess() {
		return this.success;
	}
}
