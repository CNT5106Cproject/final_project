package peer;
import java.util.concurrent.locks.ReentrantLock;
import java.io.*;
public class FileManager {
	final String fileName;
	final int fileLength;
	final int blockSize;
	final int blockNum;
	final int lastBlockSize;
	private final ReentrantLock lock = new ReentrantLock();
	private RandomAccessFile file;
	public FileManager(String fileName, int fileLength, int blockSize){
		// setup basic file info
		this.fileName = fileName;
		this.fileLength = fileLength;
		this.blockSize = blockSize;
		int remainder = fileLength % blockSize;
		this.blockNum = fileLength/blockSize + ((remainder > 0)?1:0);
		this.lastBlockSize = (remainder > 0)?remainder:blockSize;
		// open target file object
		try{
			file = new RandomAccessFile(this.fileName, "rw");
			// if file already exist, replace it with a blank file
			file.setLength(0);
			file.setLength(this.fileLength);
			// file.close();
		}
		catch(IOException e){
			System.err.println("FileManager init error");
		}
	}
	public boolean isFinish(){
		return false;
	}
	public int pickFileBlock(){
		return 0;
	}
	public int write(byte[] b, int off, int len){
		this.lock.lock();
		int ret = len;
		try{
			this.file.seek(off);
			this.file.write(b, 0, len);
		}	
		catch(IOException | NullPointerException | IndexOutOfBoundsException e){
			System.err.println("FileManager write failed");
			ret = -1;
		}
		finally{
			this.lock.unlock();
		}
		return ret;
	}
	public static void main(String args[])
	{
		FileManager client = new FileManager("XZY",124,10);

		client.write("21".getBytes(),16,2);
		System.out.println(client.blockNum);
		System.out.println(client.lastBlockSize);
	}
}