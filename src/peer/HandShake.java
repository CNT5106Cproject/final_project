package peer;

import java.io.*;

import utils.CustomExceptions;
import utils.ErrorCode;
import utils.LogHandler;
import utils.Tools;
public class HandShake
{
	
	// Attributes
	//For Handshake message header
	private static final int MSG_LENGTH = 32;
	private static final int HANDSHAKE_HEADER_LENGTH = 18;
	private static final int HANDSHAKE_ZEROBITS_LENGTH = 10;
	private static final int HANDSHAKE_PEERID_LENGTH = 4;
	private static final String CHARSET_NAME = "UTF8";
	public static final String HANDSHAKE_HEADER = "P2PFILESHARINGPROJ";
	private int[] msg_header = new int[HANDSHAKE_HEADER_LENGTH];
	private int[] peerID = new int[HANDSHAKE_PEERID_LENGTH];
	private int[] zeroBits = new int[HANDSHAKE_ZEROBITS_LENGTH];
	private String messageHeader;
	private String messagePeerID;

	private static LogHandler logging = new LogHandler();

	/* Class constructor
	 * 
	 * 
	 */
	public HandShake(String Header, String PeerId) {

		try {
			this.messageHeader = Header;
			this.msg_header = convertToInt(Header);
			if (this.msg_header.length != HANDSHAKE_HEADER_LENGTH)
				throw new Exception("Unmatched header length");

			this.messagePeerID = PeerId;
			this.peerID = convertToInt(PeerId);
			if (this.peerID.length > HANDSHAKE_PEERID_LENGTH)
				throw new Exception("Peer ID too long");

			this.zeroBits = convertToInt("0000000000");
		} catch (Exception e) {
			//PeerProcess.showLog(e.toString());
		}

	}

	private HandShake(){
		
	}

	public int[] convertToInt(String s) {
		int[] result = new int[s.length()];
		for (int i = 0; i < s.length(); i++) {
			result[i] = s.charAt(i) - '0';
		}

		return result;
	}

	// return the handShakeHeader
	public int[] getHeader() {
		return msg_header;
	}
	
	// return the peerID
	public int[] getPeerID() {
		return peerID;
	}

	// Set the zeroBits 
	public void setZeroBits(int[] zeroBits) {
		this.zeroBits = zeroBits;
	}

	// return the zeroBits
	public int[] getZeroBits() {
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
			this.msg_header = convertToInt(this.messageHeader);
		} catch (UnsupportedEncodingException e) {
			//PeerProcess.showLog(e.toString());
		}
	}


	// Set the messagePeerID
	public void setPeerID(String messagePeerID) {
		try {
			this.messagePeerID = messagePeerID;
			this.peerID = convertToInt(messagePeerID);
		} catch (Exception e) {
			//PeerProcess.showLog(e.toString());
		}
	}

	// Set the peerID 
	public void setPeerID(byte[] peerID) {
		try {
			this.messagePeerID = (new String(peerID, CHARSET_NAME)).toString().trim();
			this.peerID = convertToInt(this.messagePeerID);

		} catch (UnsupportedEncodingException e) {
			//PeerProcess.showLog(e.toString());
		}
	}
	
	// Set the messageHeader 
	public void setHeader(String messageHeader) {
		try {
			this.messageHeader = messageHeader;
			this.msg_header = convertToInt(messageHeader);
		} catch (Exception e) {
			//PeerProcess.showLog(e.toString());
		}
	}
	
	// Decodes the byte array HandshakeMessage and loads to the object HandshakeMessage
	public static HandShake decodeMessage(byte[] receivedMessage) throws CustomExceptions {

		HandShake handshakeMessage = null;
		byte[] msgHeader = null;
		byte[] msgPeerID = null;

		try {
			// Initial check
			if (receivedMessage.length != MSG_LENGTH)
				throw new CustomExceptions(ErrorCode.failHandshake, "Unmatched byte array length");

			// VAR initialization
			handshakeMessage = new HandShake();
			msgHeader = new byte[HANDSHAKE_HEADER_LENGTH];
			msgPeerID = new byte[HANDSHAKE_PEERID_LENGTH];

			// Decode the received message
			System.arraycopy(receivedMessage, 0, msgHeader, 0,
					HANDSHAKE_HEADER_LENGTH);
			System.arraycopy(receivedMessage, HANDSHAKE_HEADER_LENGTH
					+ HANDSHAKE_ZEROBITS_LENGTH, msgPeerID, 0,
					HANDSHAKE_PEERID_LENGTH);


			//change handshake_header to received header


			for (int i = 0; i < msgHeader.length; i++) {
				byte[] handshake_header_byte = HANDSHAKE_HEADER.getBytes();
				if (msgHeader[i] != handshake_header_byte[i]) {
					throw new CustomExceptions(ErrorCode.invalidHandshakeHeader, "Header Invalid");
				}
			}

			// Populate handshakeMessage entity
			handshakeMessage.setHeader(msgHeader);
			handshakeMessage.setPeerID(msgPeerID);

		} catch(CustomExceptions e) {
			String trace = Tools.getStackTrace(e);
			logging.writeLog(trace);
			handshakeMessage = null;
		}
		return handshakeMessage;
	}

	// Encodes a given message in the format HandshakeMessage 
	public static byte[] encodeMessage(HandShake handshakeMessage) {

		byte[] sendMessage = new byte[MSG_LENGTH];

		try {
			// Encode header
			if (handshakeMessage.getHeader() == null) {
				throw new CustomExceptions(ErrorCode.invalidHandshakeHeader, "Header Invalid");
			}
			if (handshakeMessage.getHeader().length > HANDSHAKE_HEADER_LENGTH|| handshakeMessage.getHeader().length == 0)
			{
				throw new CustomExceptions(ErrorCode.invalidHandshakeHeader, "Header Invalid");
			} else {
				System.arraycopy(handshakeMessage.getHeader(), 0, sendMessage,
						0, handshakeMessage.getHeader().length);
			}

			// Encode zero bits
			if (handshakeMessage.getZeroBits() == null) {
				throw new CustomExceptions(ErrorCode.failHandshake, "Invalid zero bits field.");
			} 
			if (handshakeMessage.getZeroBits().length > HANDSHAKE_ZEROBITS_LENGTH
					|| handshakeMessage.getZeroBits().length == 0) {
				throw new CustomExceptions(ErrorCode.failHandshake, "Invalid zero bits field.");
			} else {
				System.arraycopy(handshakeMessage.getZeroBits(), 0,
						sendMessage, HANDSHAKE_HEADER_LENGTH,
						HANDSHAKE_ZEROBITS_LENGTH);
			}

			// Encode peer id
			if (handshakeMessage.getPeerID() == null) 
			{
				throw new CustomExceptions(ErrorCode.failHandshake, "Invalid zero bits field.");
			} 
			else if (handshakeMessage.getPeerID().length > HANDSHAKE_PEERID_LENGTH
					|| handshakeMessage.getPeerID().length == 0) 
			{
				throw new CustomExceptions(ErrorCode.failHandshake, "Invalid zero bits field.");
			} 
			else 
			{
				System.arraycopy(handshakeMessage.getPeerID(), 0, sendMessage,
						HANDSHAKE_HEADER_LENGTH + HANDSHAKE_ZEROBITS_LENGTH,
						handshakeMessage.getPeerID().length);
			}

		} 
		catch(CustomExceptions e) {
			String trace = Tools.getStackTrace(e);
			logging.writeLog(trace);
			sendMessage = null;
		}

		return sendMessage;
	}

	public static boolean SendHandshake(OutputStream opStream, byte[] handshakeMsg) throws IOException
	{
		try
		{
			opStream.write(handshakeMsg);
		}
		catch (IOException e)
		{
			return false;
		}
		return true;
	}

	public static String ReceiveHandshake(InputStream inputStream) throws IOException, CustomExceptions
	{
		byte[] receivedHandshakeByte = new byte[32];
		try
		{
			inputStream.read(receivedHandshakeByte);
			HandShake receiveMsg = decodeMessage(receivedHandshakeByte);
			return receiveMsg.getPeerIDString();
		}
		catch (IOException e)
		{
			// peerProcess.showLog(this.ownPeerId + ” ReceiveHandshake : ” + e.getMessage());
		}
		return null;
	}
}
