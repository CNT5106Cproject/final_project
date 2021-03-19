package peer;

import java.io.*;

import utils.LogHandler;
// ActualMsg always have 4 objects for 4 types of message
// It will not be the object it created at the begining after recv()
// but it doesn't matter.
// 
// This class is not thread-safe
public class ActualMsg{
	Peer targetPeer; // communicate target peer
	// pretend to be enum
	public static byte CHOKE = 0;
	public static byte UNCHOKE = 1;
	public static byte INTERESTED = 2;
	public static byte NOTINTERESTED = 3;
	public static byte HAVE = 4;
	public static byte BITFIELD = 5;
	public static byte REQUEST = 6;
	public static byte PIECE = 7;
	// 4 msg type
	// choke, unchoke, interested, notinterested have no payload
	// only msg type and msg length
	// noPayloadMsg is actually the header
	private NoPayloadMsg noPayloadMsg = new NoPayloadMsg();
	// have, request have 4 bytes payload
	private ShortPayloadMsg shortMsg = new ShortPayloadMsg();
	// bitfield have a variable length bitfield
	private BitfieldMsg bitfieldMsg = new BitfieldMsg();
	// piece have a 4 bytes blockIdx and variable length of data
	private PieceMsg pieceMsg = new PieceMsg();

	private static LogHandler logging = new LogHandler();
	
	ActualMsg(Peer targetPeer) {
		this.targetPeer = targetPeer;
	}

	/**
	 * send() for 
	 * (1) CHOKE UNCHOKE INTERESTED NOTINTERESTED
	 * (2) HAVE REQUEST
	 * @param      out          The out
	 * @param      type         The type
	 * @param      blockIdx     The block index ( for (1) command this can be any number)
	 *
	 * @throws     IOException  exception, sth is wrong
	 */
	public void send(OutputStream out, byte type, int blockIdx) throws IOException{
		ObjectOutputStream opStream = new ObjectOutputStream(out);
		if(type <= NOTINTERESTED){
			this.noPayloadMsg.setData(1,type);
			opStream.writeObject(this.noPayloadMsg);
		}
		else if (type != BITFIELD && type < PIECE) {
			this.shortMsg.setData(5,type,blockIdx);
			opStream.writeObject(this.shortMsg);
		}
		else{
			System.err.println("ActualMsg send: wrong type");
			return;
		}
		opStream.flush();
	}
	/**
	 * send() for BITFIELD
	 *
	 * @param      out          The out
	 * @param      type         The type
	 * @param      bitfield     The bitfield
	 *
	 * @throws     IOException  exception, sth is wrong
	 */
	public void send(OutputStream out, byte type, byte[] bitfield) throws IOException{
		ObjectOutputStream opStream = new ObjectOutputStream(out);
		if(type != BITFIELD) {
			System.err.println("ActualMsg send: wrong type");
			return;
		}
		this.bitfieldMsg.setData(1+bitfield.length,type, bitfield);
		opStream.writeObject(this.bitfieldMsg);
		opStream.flush();
	}
	/**
	 * send() for PIECE
	 *
	 * @param      out          The out
	 * @param      type         The type
	 * @param      blockIdx     The block index
	 * @param      data         The piece data
	 *
	 * @throws     IOException  exception, sth is wrong
	 */
	public void send(OutputStream out, byte type, int blockIdx, byte[] data) throws IOException{
		ObjectOutputStream opStream = new ObjectOutputStream(out);
		if(type != PIECE) {
			System.err.println("ActualMsg send: wrong type");
			return;
		}
		this.pieceMsg.setData(5+data.length, type, blockIdx, data);
		opStream.writeObject(this.pieceMsg);
		opStream.flush();
	}
	/**
	 * recieve from send
	 *
	 * @param      in           inputstream usually object input stream
	 *
	 * @return     -1 when error happens, otherwise return the type of incoming msg
	 *
	 * @throws     IOException  
	 */
	public byte recv(InputStream in) throws IOException{
		ObjectInputStream ipStream = new ObjectInputStream(in);
		try{
			NoPayloadMsg msg = (NoPayloadMsg)ipStream.readObject();
			byte type = msg.getMsgType();
			if(type <= NOTINTERESTED){
				this.noPayloadMsg = msg;
			}
			else if(type < PIECE && type != BITFIELD){
				ShortPayloadMsg shortMsg = (ShortPayloadMsg) msg;
				this.shortMsg = shortMsg;
			}
			else if(type == BITFIELD){
				BitfieldMsg bitfieldMsg = (BitfieldMsg) msg;
				this.bitfieldMsg = bitfieldMsg;
			}
			else{
				PieceMsg pieceMsg = (PieceMsg) msg;
				this.pieceMsg = pieceMsg;
			}
			logging.writeLog(
				String.format("Receive msg from peer [%s], type: [%s]", this.targetPeer.getId(),type),
			);
			return type;
		}
		catch(ClassNotFoundException e){
			logging.writeLog("severe", "read input stream exception, ex:" + e);
		}
		return -1;
	}
	/**
	 * Prints a byte array.
	 *(May be moved to utility)
	 * @param      bytes  The bytes
	 */
	public static void printByteArray(byte[] bytes){
		for (byte b : bytes) {
			System.out.println(Integer.toBinaryString(b & 255 | 256).substring(1));
		}
	}
	public static void main(String args[]) throws IOException{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ActualMsg sender = new ActualMsg();
		ActualMsg recver = new ActualMsg();
		byte[] b = {
			(byte)0b10101010,
			(byte)0b01010101
		};
		sender.send(out,ActualMsg.REQUEST, 2);
		// sender.send(out,ActualMsg.PIECE, 2, b);
		int type = recver.recv(new ByteArrayInputStream(out.toByteArray()));

		if(type <= ActualMsg.NOTINTERESTED){
				System.out.println(recver.noPayloadMsg.getMsgType());
			}
			else if(type < ActualMsg.PIECE && type != ActualMsg.BITFIELD){
				System.out.println(recver.shortMsg.getMsgType());
				System.out.println(recver.shortMsg.getBlockIdx());
			}
			else if(type == ActualMsg.BITFIELD){
				System.out.println(recver.bitfieldMsg.getMsgType());
				ActualMsg.printByteArray(recver.bitfieldMsg.getBitfield());
			}
			else{
				System.out.println(recver.pieceMsg.getMsgType());
				System.out.println(recver.pieceMsg.getMsgLen());
				ActualMsg.printByteArray(recver.pieceMsg.getData());
			}

	}
}
