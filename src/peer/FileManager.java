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
	/**
	 * Constructs a new instance.
	 *
	 * @param      fileName    The file name
	 * @param      fileLength  The file length
	 * @param      blockSize   The block size
	 */
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
			// if file already exist, replace it with a empty file
			file.setLength(0);
			file.setLength(this.fileLength);
			// file.close();
		}
		catch(IOException e){
			System.err.println("FileManager init error");
		}
	}
	/**
	 * Determines if file download is completed.
	 *
	 * @return     True if complete, False otherwise.
	 */
	public boolean isComplete(){
		return false;
	}
	public int pickFileBlock(){
		return 0;
	}
	/**
	 * write len bytes to blockIdx block
	 *
	 * @param      blockIdx  The block index
	 * @param      b         The byte array
	 * @param      len       The length
	 *
	 * @return     -1 when there is an error, otherwise the bytes write to file
	 */			
	public int write(int blockIdx, byte[] b, int len){
		if(blockIdx >= this.blockNum || blockNum < 0 ||
			(blockIdx != this.blockNum -1 && len != this.blockSize) || 
			(blockIdx == this.blockNum -1 && len != this.lastBlockSize)){
			System.err.println("FileManager write failed: erroneous len");
			return -1;
		}
		this.lock.lock();
		int ret = len;
		try{
			this.file.seek(blockIdx*this.blockSize);
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
	/**
	 * close file, for termination
	 */
	public void close(){
		try{
			this.file.close();
		}
		catch(IOException e){
			System.err.println("FileManager close failed");
		}
	}
	public static void main(String args[])
	{
		FileManager client = new FileManager("XZY",1024,8);

		client.write(127,"12345678".getBytes(),8);
		client.write(15,"abcdefgz".getBytes(),8);
		System.out.println(client.blockNum);
		System.out.println(client.lastBlockSize);
		client.close();
	}
}