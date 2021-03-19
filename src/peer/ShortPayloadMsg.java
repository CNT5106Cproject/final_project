package peer;
import peer.NoPayloadMsg;
public class ShortPayloadMsg extends NoPayloadMsg{
	protected int blockIdx;
	public void setData(int msgLen, byte msgType, int blockIdx){
		this.msgLen = msgLen;
		this.msgType = msgType;
		this.blockIdx = blockIdx;
	}
	public int getBlockIdx(){
		return this.blockIdx;
	}
}