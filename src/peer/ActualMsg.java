package peer;

import java.io.*;

public class ActualMsg{
	// pretend to be enum
	public static byte CHOKE = 0;
	public static byte UNCHOKE = 1;
	public static byte INTERESTED = 2;
	public static byte NOTINTERESTED = 3;
	public static byte HAVE = 4;
	public static byte BITFIELD = 5;
	public static byte REQUEST = 6;
	public static byte PIECE = 7;
	// 3 msg type
	private final NoPayloadMsg noPayloadMsg = new NoPayloadMsg();
	private final ShortPayloadMsg shortMsg = new ShortPayloadMsg();
	private final BitfieldMsg bitfieldMsg = new BitfieldMsg();
	private final PieceMsg pieceMsg = new PieceMsg();
	// choke, unchoke, interested, notinterested have no payload
	// have, request, piece have 4 bytes payload
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
		}
	}
	public void send(OutputStream out, byte type, byte[] bitfield) throws IOException{
		ObjectOutputStream opStream = new ObjectOutputStream(out);
		if(type != BITFIELD) {
			System.err.println("ActualMsg send: wrong type");
			return;
		}
		this.bitfieldMsg.setData(1+bitfield.length,type, bitfield);
		opStream.writeObject(this.bitfieldMsg);
	}
	public void send(OutputStream out, byte type, int blockIdx, byte[] data) throws IOException{
		ObjectOutputStream opStream = new ObjectOutputStream(out);
		if(type != PIECE) {
			System.err.println("ActualMsg send: wrong type");
			return;
		}
		this.pieceMsg.setData(5+data.length, type, blockIdx, data);
		opStream.writeObject(this.pieceMsg);
	}
	public byte recv(InputStream in) throws IOException{
		ObjectInputStream ipStream = new ObjectInputStream(in);
		try{
			NoPayloadMsg msg = (NoPayloadMsg)ipStream.readObject();
			byte type = msg.getMsgType();
			if(type <= NOTINTERESTED){
				System.out.println(msg.getMsgLen());
				System.out.println(msg.getMsgType());
			}
			else if(type != BITFIELD){
				ShortPayloadMsg shortMsg = (ShortPayloadMsg) msg;
				System.out.println(shortMsg.getMsgLen());
				System.out.println(shortMsg.getMsgType());
				System.out.println(shortMsg.getBlockIdx());
			}
			else{
				BitfieldMsg bitfieldMsg = (BitfieldMsg) msg;
				System.out.println(bitfieldMsg.getMsgLen());
				System.out.println(bitfieldMsg.getMsgType());
				printByteArray(bitfieldMsg.getBitfield());
			}
			return type;
		}
		catch(ClassNotFoundException e){
			System.out.println("class not found");
		}
		return -1;
	}
	public void printByteArray(byte[] bytes){
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
		sender.send(out,ActualMsg.BITFIELD,b);
		recver.recv(new ByteArrayInputStream(out.toByteArray()));
		System.out.println("Type");
		System.out.println(recver);
	}
}
