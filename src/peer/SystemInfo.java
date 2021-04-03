package peer;

import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

import utils.CustomExceptions;
import utils.ErrorCode;

/**
 * Create a singleton for System Parameters
 */
public final class SystemInfo {
  
  private final ReentrantLock lock = new ReentrantLock();
  private static SystemInfo singletonObj = null;

  private static int retryLimit = 10;
  private static long retryInr = 3000; // retry interval (ms)
  private static long clientRequestPieceInr = 1000; // (ms)

  /**
   * Host peer infos
   * - host 
   * - neighborMap
   * - chokingMap 
   *    The choking-unChoking interval will assigns new value to this object,
   *    The opt unChoking interval will use this object to pick opt node.
   */
  private Peer host;
  private HashMap<String, Peer> neighborMap = new HashMap<String, Peer>();
  private HashMap<String, Peer> interestMap = new HashMap<String, Peer>();
  private HashMap<String, Peer> unChokingMap = new HashMap<String, Peer>();
  private HashMap<String, Peer> chokingMap = new HashMap<String, Peer>();
  private HashMap<String, Socket> connectionMap = new HashMap<String, Socket>();
	private HashMap<String, ActualMsg> actMsgMap = new HashMap<String, ActualMsg>();
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
    singletonObj.setHostPeer(host);
    singletonObj.setPeerList(neighborMap);
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
    singletonObj.setSystemParam(SystemInfoList);
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

  public void setHostPeer(Peer host) {
    this.host = host;
  }

  public void setPeerList(HashMap<String, Peer> neighborMap) {
    this.neighborMap = neighborMap;
  }

  public void setSystemParam(List<String> SystemInfoList) {
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

  public Peer getHostPeer() {
    return this.host;
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

  public void clearInterestMap() {
    this.interestMap.clear();
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

  public void clearUnChokingMap() {
    this.unChokingMap.clear();
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

  public void clearChokingMap() {
    this.chokingMap.clear();
  }
  
  public HashMap<String, Socket> getConnectionMap() {
    return this.connectionMap;
  }

  public HashMap<String, ActualMsg> getActMsgMap() {
    return this.actMsgMap;
  }

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
}
