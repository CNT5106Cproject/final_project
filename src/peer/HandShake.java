package peer;

import java.io.*;

public class Handshake
{
	
	// Attributes
	//For Handshake message header
	private static final int MSG_LENGTH = 32;
	private static final int HANDSHAKE_HEADER_LENGTH = 18;
	private static final int HANDSHAKE_ZEROBITS_LENGTH = 10;
	private static final int HANDSHAKE_PEERID_LENGTH = 4;
	private static final String CHARSET_NAME = "UTF8";
	private static final String HANDSHAKE_HEADER = "P2PFILESHARINGPROJ";
	private byte[] msg_header = new byte[HANDSHAKE_HEADER_LENGTH];
	private byte[] peerID = new byte[HANDSHAKE_PEERID_LENGTH];
	private byte[] zeroBits = new byte[HANDSHAKE_ZEROBITS_LENGTH];
	private String messageHeader;
	private String messagePeerID;


	/* Class constructor
	 * 
	 * 
	 */
	public Handshake(String Header, String PeerId) {

		try {
			this.messageHeader = Header;
			this.msg_header = Header.getBytes(CHARSET_NAME);
			if (this.msg_header.length > HANDSHAKE_HEADER_LENGTH)
				throw new Exception("Header too long");

			this.messagePeerID = PeerId;
			this.peerID = PeerId.getBytes(CHARSET_NAME);
			if (this.peerID.length > HANDSHAKE_HEADER_LENGTH)
				throw new Exception("Peer ID too long");

			this.zeroBits = "0000000000".getBytes(CHARSET_NAME);
		} catch (Exception e) {
			//PeerProcess.showLog(e.toString());
		}

	}

	public Handshake(){
		
	}

	// return the handShakeHeader
	public byte[] getHeader() {
		return msg_header;
	}
	
	// return the peerID
	public byte[] getPeerID() {
		return peerID;
	}

	// Set the zeroBits 
	public void setZeroBits(byte[] zeroBits) {
		this.zeroBits = zeroBits;
	}

	// return the zeroBits
	public byte[] getZeroBits() {
		return zeroBits;
	}

	// return the messageHeader
	public String getHeaderString() {
		return messageHeader;
	}

	// return the messagePeerID
	public String getPeerIDString() {
		return messagePeerID;
	}

	// Return the toString method of the Object
	public String toString() {
		return ("[HandshakeMessage] : Peer Id - " + this.messagePeerID
				+ ", Header - " + this.messageHeader);
	}

	// Set the handShakeHeader
	public void setHeader(byte[] handShakeHeader) {
		try {
			this.messageHeader = (new String(handShakeHeader, CHARSET_NAME)).toString().trim();
			this.msg_header = this.messageHeader.getBytes();
		} catch (UnsupportedEncodingException e) {
			//PeerProcess.showLog(e.toString());
		}
	}


	// Set the messagePeerID
	public void setPeerID(String messagePeerID) {
		try {
			this.messagePeerID = messagePeerID;
			this.peerID = messagePeerID.getBytes(CHARSET_NAME);
		} catch (UnsupportedEncodingException e) {
			//PeerProcess.showLog(e.toString());
		}
	}

	// Set the peerID 
	public void setPeerID(byte[] peerID) {
		try {
			this.messagePeerID = (new String(peerID, CHARSET_NAME)).toString().trim();
			this.peerID = this.messagePeerID.getBytes();

		} catch (UnsupportedEncodingException e) {
			//PeerProcess.showLog(e.toString());
		}
	}
	
	// Set the messageHeader 
	public void setHeader(String messageHeader) {
		try {
			this.messageHeader = messageHeader;
			this.msg_header = messageHeader.getBytes(CHARSET_NAME);
		} catch (UnsupportedEncodingException e) {
			//PeerProcess.showLog(e.toString());
		}
	}
	
	// Decodes the byte array HandshakeMessage and loads to the object HandshakeMessage
	public static Handshake decodeMessage(byte[] receivedMessage) {

		Handshake handshakeMessage = null;
		byte[] msgHeader = null;
		byte[] msgPeerID = null;

		try {
			// Initial check
			if (receivedMessage.length != MSG_LENGTH)
				throw new Exception("Unmatched byte array length");

			// VAR initialization
			handshakeMessage = new Handshake();
			msgHeader = new byte[HANDSHAKE_HEADER_LENGTH];
			msgPeerID = new byte[HANDSHAKE_PEERID_LENGTH];

			// Decode the received message
			System.arraycopy(receivedMessage, 0, msgHeader, 0,
					HANDSHAKE_HEADER_LENGTH);
			System.arraycopy(receivedMessage, HANDSHAKE_HEADER_LENGTH
					+ HANDSHAKE_ZEROBITS_LENGTH, msgPeerID, 0,
					HANDSHAKE_PEERID_LENGTH);

			// Populate handshakeMessage entity
			handshakeMessage.setHeader(msgHeader);
			handshakeMessage.setPeerID(msgPeerID);

		} catch (Exception e) {
			//PeerProcess.showLog(e.toString());
			handshakeMessage = null;
		}
		return handshakeMessage;
	}

	// Encodes a given message in the format HandshakeMessage 
	public static byte[] encodeMessage(Handshake handshakeMessage) {

		byte[] sendMessage = new byte[MSG_LENGTH];

		try {
			// Encode header
			if (handshakeMessage.getHeader() == null) {
				throw new Exception("Header Invalid");
			}
			if (handshakeMessage.getHeader().length > HANDSHAKE_HEADER_LENGTH|| handshakeMessage.getHeader().length == 0)
			{
				throw new Exception("Header Invalid");
			} else {
				System.arraycopy(handshakeMessage.getHeader(), 0, sendMessage,
						0, handshakeMessage.getHeader().length);
			}

			// Encode zero bits
			if (handshakeMessage.getZeroBits() == null) {
				throw new Exception("Invalid zero bits field.");
			} 
			if (handshakeMessage.getZeroBits().length > HANDSHAKE_ZEROBITS_LENGTH
					|| handshakeMessage.getZeroBits().length == 0) {
				throw new Exception("Invalid zero bits field.");
			} else {
				System.arraycopy(handshakeMessage.getZeroBits(), 0,
						sendMessage, HANDSHAKE_HEADER_LENGTH,
						HANDSHAKE_ZEROBITS_LENGTH - 1);
			}

			// Encode peer id
			if (handshakeMessage.getPeerID() == null) 
			{
				throw new Exception("Invalid peerID");
			} 
			else if (handshakeMessage.getPeerID().length > HANDSHAKE_PEERID_LENGTH
					|| handshakeMessage.getPeerID().length == 0) 
			{
				throw new Exception("Invalid peerID");
			} 
			else 
			{
				System.arraycopy(handshakeMessage.getPeerID(), 0, sendMessage,
						HANDSHAKE_HEADER_LENGTH + HANDSHAKE_ZEROBITS_LENGTH,
						handshakeMessage.getPeerID().length);
			}

		} 
		catch (Exception e) 
		{
			//PeerProcess.showLog(e.toString());
			sendMessage = null;
		}

		return sendMessage;
	}
}
