package peer;

import java.util.ArrayList;
import java.util.List;

/**
 * Create a singleton for System Parameters
 */
public final class SystemInfo {
  
  private static SystemInfo singletonObj = null;

  private static int retryLimit = 100;
  private static int retryInr = 3; // retry interval 

  /**
   * Host peer infos
   * - host 
   * - neighborList
   */
  private Peer host;
  private List<Peer> neighborList = new ArrayList<Peer>();

  /**
   * System Parameters from config
   */
  private int preferN;
  private int unChokingInr;
  private int optUnchokingInr;
  private String targetFileName;
  private int targetFileSize;
  private int filePieceSize;
  
  /**
   * Initialize peer's System infos
   */
  public SystemInfo() {}
  
  public SystemInfo(Peer host, List<Peer> neighborList) {
    singletonObj = new SystemInfo();
    singletonObj.setHostPeer(host);
    singletonObj.setPeerList(neighborList);
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
    if (singletonObj == null) {
      synchronized (SystemInfo.class) {
        if (singletonObj == null) {
          singletonObj = new SystemInfo();
        }
      }
    }
    return singletonObj;
  }

  public void setHostPeer(Peer host) {
    this.host = host;
  }

  public void setPeerList(List<Peer> neighborList) {
    this.neighborList = neighborList;
  }

  public void setSystemParam(List<String> SystemInfoList) {
    try {
      this.preferN = Integer.parseInt(SystemInfoList.get(0));
      this.unChokingInr = Integer.parseInt(SystemInfoList.get(1));
      this.optUnchokingInr = Integer.parseInt(SystemInfoList.get(2));
      this.targetFileName = SystemInfoList.get(3);
      this.targetFileSize = Integer.parseInt(SystemInfoList.get(4));
      this.filePieceSize = Integer.parseInt(SystemInfoList.get(5));
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }

  /**
  * Get peer infos
  */
  public Peer getHostPeer() {
    return this.host;
  }

  public List<Peer> getPeerList() {
    return this.neighborList;
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

  public String getTargetFileName() {
    return this.targetFileName;
  }

  public int getTargetFileSize() {
    return this.targetFileSize;
  }

  public int getFilePieceSize() {
    return this.filePieceSize;
  }

  public int getRetryLimit() {
    return SystemInfo.retryLimit;
  }

  public int getRetryInterval() {
    return SystemInfo.retryInr;
  }
}
