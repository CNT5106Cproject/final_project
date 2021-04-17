package peer;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import utils.CustomExceptions;
import utils.ErrorCode;

/**
 * Create a singleton for System Parameters
 */
public final class SystemInfo {
  private boolean isDebugMode = true;
  private final ReentrantLock lock = new ReentrantLock();
  private static SystemInfo singletonObj = null;

  private static int retryLimit = 10;
  private static long retryInr = 3000; // retry interval (ms)
  private static long clientRequestPieceInr = 1500; // (ms)
  /**
   * Host peer objects
   * - host 
   * - neighborMap
   * - chokingMap, unChokingMap
   *    The choking-unChoking interval will update these two map,
   *    To picked the sending candidate to send piece.
   * - serverConnMap, asgMsgMap 
   *    Use for communicating between Interval thread and main Server thread.
   */
  private Peer host;
  private boolean isNeighborsComplete;
  private ServerSocket serverListener;
  private Timer PreferSelectTimer;
  private Timer OptSelectTimer;
  private Timer isSystemCompleteTimer;
  private HashMap<String, Peer> neighborMap = new HashMap<String, Peer>();
  /**
   * Maps for Preferred Selection
   */
  private HashMap<String, Peer> interestMap = new HashMap<String, Peer>();
  private HashMap<String, Peer> unChokingMap = new HashMap<String, Peer>();
  private HashMap<String, Peer> chokingMap = new HashMap<String, Peer>();

  /**
   * Map for Optimistical Select
   */
  private Peer optUnchokingPeer = new Peer();

  // Multiple handlers will modify and get this object - use ConcurrentHashMap
  private ConcurrentHashMap<String, Socket> serverConnMap = new ConcurrentHashMap<String, Socket>();
  private ConcurrentHashMap<String, ObjectOutputStream> serverOpStream = new ConcurrentHashMap<String, ObjectOutputStream>();
	private ConcurrentHashMap<String, ActualMsg> serverActMsgMap = new ConcurrentHashMap<String, ActualMsg>();
  private List<Integer> blockList  = new ArrayList<Integer>();
  private List<Integer> newObtainBlocks = Collections.synchronizedList(blockList);

  // Multiple clients will modify and get this object - use ConcurrentHashMap
  private ConcurrentHashMap<String, Socket> clientConnMap = new ConcurrentHashMap<String, Socket>();
  private ConcurrentHashMap<String, ObjectOutputStream> clientOpStream = new ConcurrentHashMap<String, ObjectOutputStream>();
  private ConcurrentHashMap<String, ActualMsg> clientActMsgMap = new ConcurrentHashMap<String, ActualMsg>();
  private ConcurrentHashMap<String, Boolean> isClientCompleteMap = new ConcurrentHashMap<String, Boolean>();

  
  /**
   * System Parameters from config
   */
  private int preferN;
  private int unChokingInr;
  private int optUnchokingInr;
  private String fileName;
  private int fileSize;
  private int filePieceSize;
  
  /**
   * Initialize peer's System infos
   */
  public SystemInfo() {}
  
  public SystemInfo(Peer host, HashMap<String, Peer> neighborMap) {
    singletonObj = new SystemInfo();
    singletonObj.initHostPeer(host);
    singletonObj.initNeighborMap(neighborMap);
    singletonObj.isNeighborsComplete = false;
    singletonObj.PreferSelectTimer = new Timer();
    singletonObj.OptSelectTimer = new Timer();
    singletonObj.isSystemCompleteTimer = new Timer();
  }

  public SystemInfo(List<String> SystemInfoList) {
    /**
    * Read and store the system infos, written in Common.cfg by sequences.
    *
    * NumberOfPreferredNeighbors 3
    * UnchokingInterval 5
    * OptimisticUnchokingInterval 10
    * FileName thefile
    * FileSize 2167705
    * PieceSize 16384
    */
    singletonObj.initSystemParam(SystemInfoList);
  }

  public static SystemInfo getSingletonObj() {
    try {
      if (SystemInfo.singletonObj == null) {
        throw new CustomExceptions(
          ErrorCode.missSystemInfo, 
          String.format("Missing SystemInfo in Peer")
        );
      }
    } 
    catch(CustomExceptions e) {
			System.out.println(e);
		}
    return singletonObj;
  }
  

  /**
   * Init system peer and communication objects or params
   */
  public void initHostPeer(Peer host) {
    this.host = host;
  }

  public void initServerListener() throws IOException {
    this.serverListener = new ServerSocket(host.getPort());
  }

  public void initNeighborMap(HashMap<String, Peer> neighborMap) {
    this.neighborMap = neighborMap;
  }
  
  public void initDebugMode(boolean debug) {
    this.isDebugMode = debug;
  }

  public void initSystemParam(List<String> SystemInfoList) {
    try {
      this.preferN = Integer.parseInt(SystemInfoList.get(0));
      this.unChokingInr = Integer.parseInt(SystemInfoList.get(1));
      this.optUnchokingInr = Integer.parseInt(SystemInfoList.get(2));
      this.fileName = SystemInfoList.get(3);
      this.fileSize = Integer.parseInt(SystemInfoList.get(4));
      this.filePieceSize = Integer.parseInt(SystemInfoList.get(5));
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }

  public void initChokingMap() {
    for(Entry<String, Peer> n: this.neighborMap.entrySet()) {
      if(!n.getValue().getHasFile()) {
        this.chokingMap.put(n.getKey(), n.getValue());
      }
    }
  }

  /**
   * Receive piece, add to new obtain blocks list
   * @param blockIdx
   */
  public void addNewObtainBlocks(int blockIdx) {
    this.newObtainBlocks.add(blockIdx);
  }

  public void setIsNeighborsComplete() {
    this.isNeighborsComplete = true;
  }

  /**
   * Assign optimistically unchoked peer
   * @param selected
   */
  public void setOptUnchokingPeer(Peer selected) {
    this.optUnchokingPeer = selected;
  }

  public boolean getIsDebugMode() {
    return this.isDebugMode;
  }

  /**
  * Get system peer and communication objects or params
  */
  public Peer getHostPeer() {
    return this.host;
  }

  public ServerSocket getServerListener() {
    return this.serverListener;
  }

  public boolean getIsNeighborsComplete() {
    return this.isNeighborsComplete;
  }

  public Timer getIsSystemCompleteTimer() {
    return this.isSystemCompleteTimer;
  }

  public Timer getPreferSelectTimer() {
    return this.PreferSelectTimer;
  }

  public Timer getOptSelectTimer() {
    return this.OptSelectTimer;
  }

  public HashMap<String, Peer> getNeighborMap() {
    this.lock.lock();
		try{
      return this.neighborMap;
		}
		finally{
			this.lock.unlock();
		}
  }

  public HashMap<String, Peer> getInterestMap() {
    this.lock.lock();
		try{
      return this.interestMap;
		}
		finally{
			this.lock.unlock();
		}
  }

  public HashMap<String, Peer> getUnChokingMap() {
    this.lock.lock();
		try{
      return this.unChokingMap;
		}
		finally{
			this.lock.unlock();
		}
  }

  public HashMap<String, Peer> getChokingMap() {
    this.lock.lock();
		try{
      return this.chokingMap;
		}
		finally{
			this.lock.unlock();
		}
  }
  
  public Peer getOptUnchokingPeer() {
    return this.optUnchokingPeer;
  }

  public ConcurrentHashMap<String, Socket> getServerConnMap() {
    return this.serverConnMap;
  }

  public ConcurrentHashMap<String, ObjectOutputStream> getServerOpStream() {
    return this.serverOpStream;
  }

  public ConcurrentHashMap<String, ActualMsg> getServerActMsgMap() {
    return this.serverActMsgMap;
  }

  public ConcurrentHashMap<String, Socket> getClientConnMap() {
    return this.clientConnMap;
  }

  public ConcurrentHashMap<String, ObjectOutputStream> getClientOpStream() {
    return this.clientOpStream;
  }

  public ConcurrentHashMap<String, ActualMsg> getClientActMsgMap() {
    return this.clientActMsgMap;
  }

  public ConcurrentHashMap<String, Boolean> getIsClientCompleteMap() {
    return this.isClientCompleteMap;
  }
  /**
   * 1. Copy newObtainBlocks with lock.
   * 2. Clear newObtainBlocks
   * @return copyList from newObtainBlocks
   */
  public List<Integer> getNewObtainBlocksCopy() {
    this.lock.lock();
		try{
      List<Integer> copyList = new ArrayList<Integer>();
      copyList.addAll(this.newObtainBlocks);
      this.newObtainBlocks.clear();
      return copyList;
		}
		finally{
			this.lock.unlock();
		}
  }

  /**
  * Get system params
  */
  public int getPreferN() {
    return this.preferN;
  }

  public int getUnChokingInr() {
    return this.unChokingInr;
  }

  public int getOptUnChokingInr() {
    return this.optUnchokingInr;
  }

  public String getFileName() {
    return this.fileName;
  }

  public int getFileSize() {
    return this.fileSize;
  }

  public int getPieceSize() {
    return this.filePieceSize;
  }

  public int getRetryLimit() {
    return SystemInfo.retryLimit;
  }

  public long getRetryInterval() {
    return SystemInfo.retryInr;
  }

  public long getClientRequestPieceInr() {
    return SystemInfo.clientRequestPieceInr;
  }
  
  /**
   * Test Function
   */
  public void printNeighborsInfo() {
    if(this.neighborMap != null && this.neighborMap.size() != 0) {
      String infos = String.format("[%s] Print out neighbor map \n", this.host.getId());
      for(Entry<String, Peer> n: this.neighborMap.entrySet()) {
        infos += String.format(
          "(%s) isInterested: (%s) isChoking (%s) isDownloading (%s)\n",
          n.getKey(),
          n.getValue().getIsInterested(),
          n.getValue().getIsChoking(),
          n.getValue().getIsDownloading()
        );
        System.out.println(infos);
      }
    }
  }
}
