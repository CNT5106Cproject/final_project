package peer;

import utils.CustomExceptions;
import utils.ErrorCode;

public class Peer {
  /**
   * Peer Object with all infos
   */
  private String peerId;
	private String hostName;
	private int port;
  private boolean hasFile;
  
  public Peer() {
    
	}

  /**
   * @param peerId
   * @param hostName
   * @param port
   * @param hasFile
   */
	public Peer(String peerId, String hostName, String port, String hasFile) {
    try {
      this.peerId = peerId;
      this.hostName = hostName;
      this.port = Integer.parseInt(port);
      this.hasFile = Short.parseShort(hasFile) == 0 ? false : true;
    }
    catch (Exception ex) {
      String errorStr = CustomExceptions.errorResponse(ErrorCode.failParsePeerInfo, "failed to parse peer info");
      System.out.println(errorStr + ", ex:" + ex);
		}
	}

  /**
   * Set up only peerId, use in Server thread
   * @param peerId
   */
  public Peer(String peerId) {
    try {
      this.peerId = peerId;
      this.hostName = null;
      this.port = -1;
      this.hasFile = false;
    }
    catch (Exception ex) {
      String errorStr = CustomExceptions.errorResponse(ErrorCode.failParsePeerInfo, "failed to parse peer id");
      System.out.println(errorStr + ", ex:" + ex);
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
}
