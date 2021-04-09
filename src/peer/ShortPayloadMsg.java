package peer;
public class ShortPayloadMsg extends NoPayloadMsg{
	/**
	 *
	 */
	private static final long serialVersionUID = 3122280334566161027L;
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