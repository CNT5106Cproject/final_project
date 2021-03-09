package peer;

import java.util.ArrayList;
import java.util.List;

import utils.CustomExceptions;
import utils.ErrorCode;

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
  private String fileName;
  private int fileSize;
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

  public void setPeerList(List<Peer> neighborList) {
    this.neighborList = neighborList;
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

  public int getRetryInterval() {
    return SystemInfo.retryInr;
  }
}
