package peer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import utils.CustomExceptions;
import utils.ErrorCode;
import utils.LogHandler;
import utils.SystemInfo;
public class PeerProcess {
	private static String peerInfoFN = "PeerInfo.cfg";
  private static String localPeerInfoFN = "PeerInfo_local.cfg";
  private static String SystemInfo = "Common.cfg";

	/**
	 * Read Peer Info config
	 * 
	 * @debug = true -> read peer info's with host is local
	 */
	private static void readPeerInfo(String hostPeerId, boolean debug) {
		String fileName = peerInfoFN;
		String cfgDir = "../config/";

		Peer hostPeer = null;
		List<Peer> peerList = new ArrayList<Peer>();

		if (debug) {
			fileName = localPeerInfoFN;
		}
		try {
			cfgDir = cfgDir + fileName;
			System.out.println(String.format("[%s] Start reading PeerInfo from %s", hostPeerId, cfgDir));
			File cfgFile = new File(cfgDir);
			Scanner fileReader = new Scanner(cfgFile);
			
			while (fileReader.hasNextLine()) {
				String infoLine = fileReader.nextLine();
				String[] infos = infoLine.split("\\s+");
				Peer newPeer = new Peer(infos[0], infos[1], infos[2], infos[3]);
				if (hostPeerId.equals(newPeer.getId())) {
					// Store Peer as host
					System.out.println(String.format("[%s] Successfully set host peer info", hostPeerId));
					hostPeer = newPeer;
				}
				peerList.add(newPeer);
			}
			fileReader.close();

			SystemInfo s = new SystemInfo(hostPeer, peerList);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private static void readCommon() {

	}
	/**
	 * Main Process of the Peer
	 */
	public static void main(String[] args) {
		try {
			/* Must have at least peer ID */
			if (args.length < 1) {
				throw new CustomExceptions(ErrorCode.invalidArgumentLength, "Missing Peer Id");
			}
			/* Load peer infos */
			readPeerInfo(args[0], true);
			SystemInfo s = utils.SystemInfo.getInstance();
			Peer hostPeer = s.getHostPeer();
			/* Set peer logger */
			LogHandler logging = new LogHandler(hostPeer);
			logging.logEstablishPeer(hostPeer);

      /* Start peer server thread*/
      // - Record the connections number with other clients
			Server server = new Server(hostPeer);
			server.start();
      // TODO build client socket with others
      // - check in interval - remain N-1 connections to other server
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
  
}