package peer;
public class BitfieldMsg extends NoPayloadMsg{
	private static final long serialVersionUID = -7686813251151539176L;
	public byte[] bitfield;
	public void setData(int msgLen, byte msgType, byte[] b){
		this.msgLen = msgLen;
		this.msgType = msgType;
		this.bitfield = b;
	}
	public byte[] getBitfield(){
		return this.bitfield;
	}
}