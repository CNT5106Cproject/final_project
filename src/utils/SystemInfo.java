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
  private int unChokingInterval;
  private String targetFileName;
  private String targetFileSize;
  private int filePieceSize;
  
  public SystemInfo() {}
  
  public SystemInfo(Peer host, List<Peer> peerList) {
    instance = new SystemInfo();
    instance.setHostPeer(host);
    instance.setPeerList(peerList);
  }

  public SystemInfo(String[] SystemInfoList) {
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

    try {
      this.preferN = Integer.parseInt(SystemInfoList[0]);
      this.unChokingInterval = Integer.parseInt(SystemInfoList[1]);
      this.targetFileName = SystemInfoList[2];
      this.targetFileSize = SystemInfoList[3];
      this.filePieceSize = Integer.parseInt(SystemInfoList[4]);
    }
    catch(Exception e) {
      e.printStackTrace();
    }
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

  public Peer getHostPeer() {
    return this.host;
  }

  public List<Peer> getPeerList() {
    return this.peerList;
  }
}
