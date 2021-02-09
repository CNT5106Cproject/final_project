import utils.CustomExceptions;
import utils.ErrorCode;

public class PeerProcess {
	/**
  * Main Process of the Peer
  */
  public static void main(String[] args) {
    String peerID;
		ErrorCode code = new ErrorCode();
		boolean debug = false;
		try {
			PeerProcess process = new PeerProcess();
			if(args.length == 0) {
				throw new CustomExceptions("Missing peer id, please check the starting command", code.invalidPeerID);
			}

      peerID = args[0];
      System.out.println("ID: [" +peerID + "]" + " Establishing Peer"); 
      // TODO Starting Server
      // - Record the connections number with other clients

      // TODO build client socket with others
      // - check in interval - remain N-1 connections to other server
		}
		catch (Exception ex) {
			System.out.println(ex);
		}
	}
  
}