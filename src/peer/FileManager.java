package peer;
import java.util.concurrent.locks.ReentrantLock;

import utils.CustomExceptions;
import utils.ErrorCode;
import utils.LogHandler;

import java.io.*;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Random;
import java.util.ArrayList;
public class FileManager {
	public final String fileName;
	public final int fileLength;
	public final int blockSize;
	public final int blockNum;
	public final int lastBlockSize;
	public final String mode;
	private final ReentrantLock lock = new ReentrantLock();
	private RandomAccessFile file;
	private final HashSet<Integer> interested = new HashSet<Integer>();
	private final HashSet<Integer> downloading = new HashSet<Integer>();
	private final HashMap<Integer, HashSet<Integer>> otherPeerHave = new HashMap<Integer, HashSet<Integer>>();

	private static FileManager instance = null;
	private static SystemInfo sysInfo = SystemInfo.getSingletonObj();
	private static LogHandler logging = new LogHandler();
	/**
	 * Constructs a new instance.
	 *
	 * @param      fileName    The file name
	 * @param      mode        The mode(r/rw) 
	 * 						   r: assume file exist and only read file
	 * 						   rw: create/overwrite a new empty file, can read/write
	 * @param      fileLength  The file length
	 * @param      blockSize   The block size
	 */
	private FileManager(String fileName, String mode, int fileLength, int blockSize){
		// setup basic file info
		this.mode = mode;
		this.fileName = fileName;
		this.fileLength = fileLength;
		this.blockSize = blockSize;
		int remainder = fileLength % blockSize;
		this.blockNum = fileLength/blockSize + ((remainder > 0)?1:0);
		this.lastBlockSize = (remainder > 0)?remainder:blockSize;
		if(mode != "rw" && mode != "r"){
			System.err.println("FileManager init: unknown mode");
			return;
		}
		// open target file object
		try{
			file = new RandomAccessFile(this.fileName, "rw");
			// if file already exist, replace it with a empty file
			if(mode == "rw"){
				file.setLength(0);
				file.setLength(this.fileLength);
				for(int i = 0; i < this.blockNum; i++){
					this.interested.add(i);
				}
			}
		}
		catch(IOException e){
			System.err.println("FileManager init: error");
		}
	}
	/**
	 * Initializes and gets the instance.
	 *
	 * @param      fileName    The file name
	 * @param      mode        The mode
	 * @param      fileLength  The file length
	 * @param      blockSize   The block size
	 *
	 * @return     The instance.
	 */
	public static FileManager getInstance(String fileName, String mode, int fileLength, int blockSize){
		if(FileManager.instance == null){
			FileManager.instance = new FileManager(fileName, mode, fileLength, blockSize);
		}
		return FileManager.instance;
	}
	/**
	 * Gets the instance.
	 *
	 * @return     The instance. May return null if not initialized.
	 */
	public static FileManager getInstance() {
		try {
			if(FileManager.instance == null){
				throw new CustomExceptions(
					ErrorCode.missSystemInfo, 
					String.format("Missing File Manager in Peer [%s]", sysInfo.getHostPeer())
				);
			}
		}
		catch(CustomExceptions e) {
			logging.writeLog("severe", e.getMessage());
		}
		return FileManager.instance;
	}
	public static byte[] bitFlag = {
		(byte)0b10000000,
		(byte)0b01000000,
		(byte)0b00100000,
		(byte)0b00010000,
		(byte)0b00001000,
		(byte)0b00000100,
		(byte)0b00000010,
		(byte)0b00000001
	};
	/**
	 * insert block information(bitfield) of peerId
	 *
	 * @param      peerId  The peer id
	 * @param      b       bitfield
	 * @param      len     length of b
	 */
	public void insertBitfield(int peerId, byte[] b, int len){
		int blockIdx = 0;
		HashSet<Integer> have = new HashSet<Integer>();
		for(int i = 0; i < len && blockIdx < this.blockNum; i++){
			byte curByte = b[i];
			for(int j = 0; j < 8 && blockIdx < this.blockNum; j++, blockIdx++){
				if((curByte & FileManager.bitFlag[j]) != 0){
					have.add(blockIdx);
				}
			}
		}
		// for(int t : have){
		// 	System.out.println(t);
		// }
		this.otherPeerHave.put(peerId, have);
	}
	/**
	 * update block information of peerId
	 *
	 * @param      peerId    The peer id
	 * @param      blockIdx  The block index peerId updated
	 */
	public void updateHave(int peerId, int blockIdx){
		HashSet<Integer> have = this.otherPeerHave.get(peerId);
		if(have == null) {
			System.err.println("FileManager updateHave: no such peerId");
			return;
		}
		have.add(blockIdx);
	}
	/**
	 * Determines if file download is completed.
	 *
	 * @return     True if complete, False otherwise.
	 */
	public boolean isComplete(){
		return (this.downloading.size() + this.interested.size() == 0);
	}
	/**
	 * select interested file block from peerId "have" set and move selected 
	 * block index from "interested" to "downloading" set
	 *
	 * @param      peerId  The peer identifier
	 *
	 * @return     -1 when not interested, otherwise return interested block index.
	 */
	public synchronized int pickInterestedFileBlock(int peerId){
		if(this.interested.size() == 0) return -1;
		ArrayList<Integer> interested = new ArrayList<Integer>(this.interested);
		// get intersection of interested and have
		interested.retainAll(this.otherPeerHave.get(peerId));
		if(interested.size() == 0) return -1;
		Random rd = new Random();
		int blockIdx = interested.get(rd.nextInt(interested.size()));
		this.downloading.add(blockIdx);
		this.interested.remove(blockIdx);
		return blockIdx;
	}
	/**
	 * read bytes in blockIdx block to b
	 *
	 * @param      blockIdx  The block index
	 * @param      b         output byte array
	 * @param      len       The length of b
	 *
	 * @return     -1 when there is an error, otherwise the bytes read from file
	 */
	public int read(int blockIdx, byte[] b, int len){
		if(b == null){
			System.err.println("FileManager read: null buffer");
			return -1;
		}
		if(blockIdx < 0 || blockIdx >= blockNum){
			System.err.println("FileManager read: out of range");
			return -1;
		}
		int byteRead = 0;
		if(blockIdx == this.blockNum-1 && len > this.lastBlockSize) byteRead = this.lastBlockSize;
		else byteRead = len;
		this.lock.lock();
		try{
			this.file.seek(blockIdx*this.blockSize);
			this.file.read(b, 0, byteRead);
		}	
		catch(IOException | NullPointerException | IndexOutOfBoundsException e){
			System.err.println("FileManager read: read failed");
			byteRead = -1;
		}
		finally{
			this.lock.unlock();
		}
		return byteRead;
	}
	/**
	 * write len bytes to blockIdx block
	 *
	 * @param      blockIdx  The block index
	 * @param      b         The byte array
	 * @param      len       The length of b
	 *
	 * @return     -1 when there is an error, otherwise the bytes write to file
	 */			
	public int write(int blockIdx, byte[] b, int len){
		if(b == null){
			System.err.println("FileManager write: null buffer");
			return -1;
		}
		if(this.mode == "r"){
			System.err.println("FileManager write: read mode instance");
			return -1;
		}
		if(blockIdx >= this.blockNum || blockIdx < 0 ||
			(blockIdx != this.blockNum-1 && len != this.blockSize) || 
			(blockIdx == this.blockNum-1 && len != this.lastBlockSize)){
			System.err.println("FileManager write: erroneous parameter");
			return -1;
		}
		this.lock.lock();
		int byteWrite = len;
		try{
			this.file.seek(blockIdx*this.blockSize);
			this.file.write(b, 0, len);
			this.downloading.remove(blockIdx);
		}	
		catch(IOException | NullPointerException | IndexOutOfBoundsException e){
			System.err.println("FileManager write: write failed");
			byteWrite = -1;
		}
		finally{
			this.lock.unlock();
		}
		return byteWrite;
	}
	/**
	 * close file, for termination
	 */
	public void close(){
		try{
			this.file.close();
		}
		catch(IOException e){
			System.err.println("FileManager close: failed");
		}
	}
	public static void main(String args[])
	{
		FileManager client =  FileManager.getInstance("XZY","rw",72,8);
		FileManager b = FileManager.getInstance();
		if(b == client) System.out.println("same");
		// byte[] b = {(byte)0b10101010,(byte)0b11111111};
		// client.insertBitfield(0,b,2);
		// client.updateHave(0,1);
		// client.write(127,"12345678".getBytes(),8);
		// client.write(15,"abcdefgz".getBytes(),8);
		// System.out.println(client.blockNum);
		// System.out.println(client.lastBlockSize);
		// client.close();
	}
}