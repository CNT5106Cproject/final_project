package utils;

import java.util.ArrayList;
import java.util.List;

import peer.Peer;
/**
 * Create a singleton for System Parameters
 */
public final class SystemInfo {
  
  private static SystemInfo instance = null;
  private Peer host;
  private List<Peer> peerList = new ArrayList<Peer>();
  private int preferN;
  private int unChokingInr;
  private int optUnchokingInr;
  private String targetFileName;
  private int targetFileSize;
  private int filePieceSize;
  
  public SystemInfo() {}
  
  public SystemInfo(Peer host, List<Peer> peerList) {
    instance = new SystemInfo();
    instance.setHostPeer(host);
    instance.setPeerList(peerList);
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
    instance.setSystemParam(SystemInfoList);
  }

  public static SystemInfo getInstance() {
    if (instance == null) {
      synchronized (SystemInfo.class) {
        if (instance == null) {
          instance = new SystemInfo();
        }
      }
    }
    return instance;
  }

  public void setHostPeer(Peer host) {
    this.host = host;
  }

  public void setPeerList(List<Peer> peerList) {
    this.peerList = peerList;
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
    return this.peerList;
  }

  /**
  * Get System Infos
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
}
