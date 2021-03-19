package peer;
import java.io.Serializable;
public class NoPayloadMsg implements Serializable{
	protected int msgLen;
	protected byte msgType;
	public void setData(int msgLen, byte msgType){
		this.msgLen = msgLen;
		this.msgType = msgType;
	}
	public int getMsgLen(){
		return this.msgLen;
	}
	public byte getMsgType(){
		return this.msgType;
	}
}