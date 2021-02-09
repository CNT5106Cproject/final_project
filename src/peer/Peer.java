package peer;

import utils.CustomExceptions;
import utils.ErrorCode;

public class Peer {
  /**
   * Peer Object with all infos
   */
  private static int peerId;
	private static String hostName;
	private static int port;
  private static boolean hasFile;

  public Peer() {
    
	}

  /**
   * @param peerId
   * @param hostName
   * @param port
   * @param hasFile
   */
	public Peer(String peerId, String hostName, String port, String hasFile) {
    super();
    try {
      this.peerId = Integer.parseInt(peerId);
      this.hostName = hostName;
      this.port = Integer.parseInt(port);
      this.hasFile = Short.parseShort(hasFile) == 0 ? false : true;
    }
    catch (Exception ex) {
      String errorStr = CustomExceptions.errorResponse(ErrorCode.failParsePeerInfo, "failed to parse peerinfo");
      System.out.println(errorStr + ", ex:" + ex);
      
		}
	}

  public int getId() {
		return this.peerId;
	}

  public String getHostName() {
		return this.hostName;
	}

  public int getPort() {
		return this.port;
	}
}
