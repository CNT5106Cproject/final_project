package peer;
public class PieceMsg extends ShortPayloadMsg{
	/**
	 *
	 */
	private static final long serialVersionUID = 8683107767721716599L;
	private byte[] data;
	public void setData(int msgLen, byte msgType, int blockIdx, byte[] data){
		this.msgLen = msgLen;
		this.msgType = msgType;
		this.blockIdx = blockIdx;
		this.data = data;
	}
	public byte[] getData(){
		return this.data;
	}
}