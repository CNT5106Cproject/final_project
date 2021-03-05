package peer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import utils.CustomExceptions;
import utils.ErrorCode;
import utils.LogHandler;
public class PeerProcess {
	private static String peerInfoFN = "PeerInfo.cfg";
  private static String localPeerInfoFN = "PeerInfo_local.cfg";
  private static String SystemInfoFN = "Common.cfg";
	private static String cfgDir = "../config/";

	/**
	 * Read Peer Info config
	 * 
	 * @debug = true -> read peer info's with host is local
	 */
	private static void readPeerInfo(String hostPeerId, boolean debug) {
		String fileName = peerInfoFN;

		Peer hostPeer = null;
		List<Peer> neighborList = new ArrayList<Peer>();

		if (debug) {
			fileName = localPeerInfoFN;
		}
		try {
			System.out.println(String.format("[%s] Start reading PeerInfo from %s", hostPeerId, cfgDir + fileName));
			File cfgFile = new File(cfgDir + fileName);
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
				else {
					neighborList.add(newPeer);
				}
			}
			fileReader.close();

			SystemInfo s = new SystemInfo(hostPeer, neighborList);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private static void readCommon(String hostPeerId) {
		try {
			String fileName = SystemInfoFN;
			File cfgFile = new File(cfgDir + fileName);
			Scanner fileReader = new Scanner(cfgFile);
			
			ArrayList<String> infoList = new ArrayList<String>();
			System.out.println(String.format("[%s] Start reading Common from %s", hostPeerId, cfgDir + fileName));
			while (fileReader.hasNextLine()) {
				String infoLine = fileReader.nextLine();
				String[] infos = infoLine.split("\\s+");
				infoList.add(infos[1]);
			}
			fileReader.close();
			SystemInfo s = new SystemInfo(infoList);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
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
			readCommon(args[0]);

			SystemInfo s = SystemInfo.getSingletonObj();
			Peer hostPeer = s.getHostPeer();
			/* Set peer logger */
			LogHandler logging = new LogHandler();
			logging.logSystemParam();
			logging.logEstablishPeer();

      /* Start peer server thread -> inside we create Handler to handle sockets */
			Server server = new Server();
			server.start();

			/* Start building client threads for other target hosts */
			logging.writeLog("Start client connections");
			List<Peer> neighborList = s.getPeerList();
			for(int i=0; i < neighborList.size(); i++) {
				Client client = new Client(neighborList.get(i));
				Thread t = new Thread(client);
				t.start();
			}

			logging.writeLog("Number of thread: " + java.lang.Thread.activeCount());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
  
}