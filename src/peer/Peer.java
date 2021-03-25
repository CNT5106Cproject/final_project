package peer;

import utils.CustomExceptions;
import utils.ErrorCode;
import utils.LogHandler;
import utils.Tools;

public class Peer {
  /**
   * Peer with initial params
   */
  private String peerId;
	private String hostName;
	private int port;
  private boolean hasFile;
  
  /**
   * Peer showing the file transfer state status
   */
  private boolean isInterested = false;
  private boolean isChoking = false;
  private boolean isDownLoading = false;

  public Peer() {
    
	}

  /**
   * @param peerId
   * @param hostName
   * @param port
   * @param hasFile
   */
	public Peer(String peerId, String hostName, String port, String hasFile) throws CustomExceptions{
    try {
      this.peerId = peerId;
      this.hostName = hostName;
      this.port = Integer.parseInt(port);
      this.hasFile = Short.parseShort(hasFile) == 0 ? false : true;
    }
    catch (Exception e) {
      String trace = Tools.getStackTrace(e);
      throw new CustomExceptions(
        ErrorCode.failParsePeerInfo, 
				String.format("failed to parse peer id, ex:" + trace)
			);
		}
	}

  /**
   * Set up only peerId, use in Server thread
   * @param peerId
   */
  public Peer(String peerId) throws CustomExceptions{
    try {
      this.peerId = peerId;
      this.hostName = null;
      this.port = -1;
      this.hasFile = false;
    }
    catch (Exception e) {
      String trace = Tools.getStackTrace(e);
      throw new CustomExceptions(
        ErrorCode.failParsePeerInfo, 
				String.format("failed to parse peer id, ex:" + trace)
			);
		}
	}
  
  public String getId() {
		return this.peerId;
	}

  public String getHostName() {
		return this.hostName;
	}

  public int getPort() {
		return this.port;
	}

  public boolean getHasFile() {
    return this.hasFile;
  }

  /**
   * Down below are functions for server thread 
   * to maintain file transfer progress
   */
  public void setIsInterested(boolean status) {
		this.isInterested = status;
	}

  public boolean getIsInterested() {
		return this.isInterested;
	}

  public void setChoking() {
		this.isChoking = true;
	}

  public void setUnChoking() {
		this.isChoking = false;
	}

  public boolean getIsChoking() {
		return this.isChoking;
	}

  public void setIsDownloading(boolean status) {
		this.isDownLoading = status;
	}

  public boolean getIsDownloading() {
		return this.isDownLoading;
	}
}
