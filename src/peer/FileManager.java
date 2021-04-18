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
import java.util.BitSet;
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
	private final HashMap<String, HashSet<Integer>> otherPeerHave = new HashMap<String, HashSet<Integer>>();
	private byte[] ownBitfield;

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
		// init bitfield
		int remainderBits = this.blockNum % 8;
		int bitfieldBytesNum = this.blockNum/8 + ((remainderBits == 0)?0:1);
		this.ownBitfield = new byte[bitfieldBytesNum];
		if(mode != "rw" && mode != "r"){
			logging.writeLog("severe", "FileManager init: unknown mode");
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
			if(mode == "r"){
				buildOwnBitfield(remainderBits);
			}
		}
		catch(IOException e){
			logging.writeLog("severe", "FileManager init: error");
		}
	}
	private void buildOwnBitfield(int remainderBits){
		byte full = (byte)0b11111111;
		int bitfieldBytesNum = this.ownBitfield.length;
		for(int i = 0; i < bitfieldBytesNum-1; i++){
			this.ownBitfield[i] |= full;
		}
		if(remainderBits == 0){
			this.ownBitfield[bitfieldBytesNum-1] |= full;
		}
		else{
			byte lastByte = 0;
			for(int i = 0; i < remainderBits; i++){
				lastByte |= FileManager.bitFlag[i];
			}
			this.ownBitfield[bitfieldBytesNum-1] = lastByte;
		}
		// printByteArray(this.ownBitfield);
	}
	private void updateOwnBitfield(int blockIdx){
		this.ownBitfield[blockIdx/8] |= FileManager.bitFlag[blockIdx%8];
		// printByteArray(this.ownBitfield);
	}

	public boolean isOwnBitfieldContain(int blockIdx){
		return ((byte)(this.ownBitfield[blockIdx/8] & FileManager.bitFlag[blockIdx%8]) != 0);
	}
	
	public int getOwnBitfieldSize(){
		return BitSet.valueOf(this.ownBitfield).cardinality();
	}

	private void printByteArray(byte[] bytes){
		for (byte b : bytes) {
			logging.writeLog(Integer.toBinaryString(b & 255 | 256).substring(1));
			//System.out.println(Integer.toBinaryString(b & 255 | 256).substring(1));
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
	/**
	 * Gets the own bitfield.
	 *
	 * @return     The own bitfield.
	 */
	public byte[] getOwnBitfield(){
		// synchronize with write(), make sure we return correct bitfield
		this.lock.lock();
		try{
			return this.ownBitfield;
		}
		finally{
			this.lock.unlock();
		}
	}
	/**
	 * insert block information(bitfield) of peerId
	 *
	 * @param      peerId  The peer id
	 * @param      b       bitfield
	 * @param      len     length of b
	 */
	public void insertBitfield(String peerId, byte[] b, int len){
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
	public void updateHave(String peerId, int blockIdx){
		HashSet<Integer> have = this.otherPeerHave.get(peerId);
		if(have == null) {
			logging.writeLog("severe", "FileManager updateHave: no such peerId");
			return;
		}
		have.add(blockIdx);
	}

	/**
	 * check if others is finished by checking the have HashSet
	 *
	 * @param      peerId    The peer id
	 */
	public boolean isOthersFinish(String peerId){
		HashSet<Integer> have = this.otherPeerHave.get(peerId);
		if(have == null) {
			logging.writeLog("severe", "FileManager updateHave: no such peerId");
			return false;
		}
		if(have.size() == blockNum) return true;
		return false;
	}

	/**
	 * Determines if file download is completed.
	 *
	 * @return     True if complete, False otherwise.
	 */
	public boolean isComplete(){
		logging.writeLog(
			"peer still interested size " + this.interested.size() + ", peer still downloading size " + this.downloading.size()
		);
		return (this.downloading.size() + this.interested.size() == 0);
	}
	/**
	 * Determines whether the specified peer identifier is interested.
	 *
	 * @param      peerId  The peer identifier
	 *
	 * @return     True if the specified peer identifier is interested, False otherwise.
	 */
	public synchronized boolean isInterested(String peerId){
		if(this.interested.size() == 0) return false;
		ArrayList<Integer> interested = new ArrayList<Integer>(this.interested);

		logging.writeLog("check interested in " + interested.size() + " # of blocks from peer " + peerId);
		// get intersection of interested and have
		logging.writeLog(String.format("PeerId: %s retain %s of blocks", peerId, this.otherPeerHave.get(peerId).size()));
		interested.retainAll(this.otherPeerHave.get(peerId));
		return (interested.size() != 0);
	}
	/**
	 * select interested file block from peerId "have" set and move selected 
	 * block index from "interested" to "downloading" set
	 *
	 * @param      peerId  The peer identifier
	 *
	 * @return     -1 when not interested, otherwise return interested block index.
	 */
	public synchronized int pickInterestedFileBlock(String peerId){
		Random rd = new Random();
		int blockIdx = -1;
		if(this.interested.size() == 0) {
			ArrayList<Integer> downloading = new ArrayList<Integer>(this.downloading);
			if(downloading.size() == 0) {
				return blockIdx;
			}
			blockIdx = downloading.get(rd.nextInt(downloading.size()));
			return blockIdx;
		}
		ArrayList<Integer> interested = new ArrayList<Integer>(this.interested);
		// get intersection of interested and have
		interested.retainAll(this.otherPeerHave.get(peerId));
		if(interested.size() == 0) return -1;
		blockIdx = interested.get(rd.nextInt(interested.size()));
		this.downloading.add(blockIdx);
		this.interested.remove(blockIdx);
		return blockIdx;
	}
	/**
	 * Gets the block size.
	 *
	 * @param      blockIdx  The block index
	 *
	 * @return     The block size.
	 */
	public int getBlockSize(int blockIdx){
		if(blockIdx < 0 || blockIdx >= this.blockNum) return -1;
		return (blockIdx == this.blockNum-1)? this.lastBlockSize: this.blockSize;
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
			logging.writeLog("severe", "FileManager read: null buffer");
			return -1;
		}
		if(blockIdx < 0 || blockIdx >= blockNum){
			logging.writeLog("severe", "FileManager read: out of range");
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
			logging.writeLog("severe", "FileManager read: read failed");
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
			logging.writeLog("severe", "FileManager write: null buffer");
			return -1;
		}
		if(this.mode == "r"){
			logging.writeLog("severe", "FileManager write: read mode instance");
			return -1;
		}
		if(blockIdx >= this.blockNum || blockIdx < 0 ||
			getBlockSize(blockIdx) != len){
			logging.writeLog("severe", "FileManager write: erroneous parameter");
			return -1;
		}
		this.lock.lock();
		int byteWrite = len;
		try{
			this.file.seek(blockIdx*this.blockSize);
			this.file.write(b, 0, len);
			this.downloading.remove(blockIdx);
			updateOwnBitfield(blockIdx);
		}	
		catch(IOException | NullPointerException | IndexOutOfBoundsException e){
			logging.writeLog("severe", "FileManager write: write failed");
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
			logging.writeLog("severe", "FileManager close: failed");
		}
	}
	public static void main(String args[])
	{
		// FileManager client =  FileManager.getInstance("XZY","rw",161,10);
		// // System.out.println(client.blockNum);
		// FileManager b = FileManager.getInstance();
		// if(b == client) System.out.println("same");
		// // byte[] b = {(byte)0b10101010,(byte)0b11111111};
		// // client.insertBitfield(0,b,2);
		// // client.updateHave(0,1);
		// // client.write(127,"12345678".getBytes(),8);
		// // client.write(15,"abcdefgz".getBytes(),8);
		// // System.out.println(client.blockNum);
		// // System.out.println(client.lastBlockSize);
		// // client.close();
	}
}