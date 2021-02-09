package peer;

import utils.CustomExceptions;
import utils.ErrorCode;
import peer.Server;
import peer.Peer;

public class PeerProcess {
	/**
  * Main Process of the Peer
  */
  public static void main(String[] args) {
		ErrorCode code = new ErrorCode();
		boolean debug = false;
		try {
			PeerProcess process = new PeerProcess();
			// 0 - ID, 1 - hostname, 2 - port
			if(args.length < 4) {
				throw new CustomExceptions(code.invalidArgumentLength, "Missing Peer Info Arguments");
			}
			Peer peer = new Peer(args[0], args[1], args[2], args[3]);
      System.out.println("peerId: [" + peer.getId() + "]" + ", Start Establishing Peer");  
      // TODO Starting Server
      // - Record the connections number with other clients
			Server server = new Server();
			server.setPort(peer.getPort());
			server.start();
      // TODO build client socket with others
      // - check in interval - remain N-1 connections to other server
		}
		catch (Exception ex) {
			System.out.println(ex);
		}
	}
  
}