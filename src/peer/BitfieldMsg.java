package peer;
import peer.NoPayloadMsg;
public class BitfieldMsg extends NoPayloadMsg{
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