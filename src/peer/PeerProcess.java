package peer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Map.Entry;

import utils.CustomExceptions;
import utils.ErrorCode;
import utils.LogHandler;
public class PeerProcess {
	private static String peerInfoFN = "PeerInfo.cfg";
	private static String debugPeerInfoFN = "PeerInfo_debug.cfg";
  private static String SystemInfoFN = "Common.cfg";
	private static String debugSystemInfoFN = "Common_debug.cfg";
	private static String cfgDir = "../config/";

	/**
	 * Read Peer Info config
	 * 
	 * @debug = true -> read peer info's with host is local
	 */
	private static void readPeerInfo(String hostPeerId, boolean debug) {
		String fileName = peerInfoFN;

		Peer hostPeer = null;
		HashMap<String, Peer> createMap = new HashMap<String, Peer>();

		if (debug) {
			/**
			 * Use the testing config 
			 */
			fileName = debugPeerInfoFN;
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
					createMap.put(newPeer.getId(), newPeer);
				}
			}
			fileReader.close();
			
			/** Set up peer's host info and neighbor list */
			SystemInfo s = new SystemInfo(hostPeer, createMap);
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (CustomExceptions e) {
			System.out.println(e);
		}

	}

	private static void readCommon(String hostPeerId, boolean debug) {
		try {
			String fileName = SystemInfoFN;
			if (debug) {
				/**
				 * Use the testing config 
				 */
				fileName = debugSystemInfoFN;
			}
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
			
			/** Set up peer's system parameters */
			SystemInfo s = new SystemInfo(infoList);
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Main Process of the Peer
	 */
	public static void main(String[] args) {
		boolean debug = true;
		try {
			/* Must have at least peer ID */
			if (args.length < 1) {
				throw new CustomExceptions(ErrorCode.invalidArgumentLength, "Missing Peer Id");
			}
			/* Load peer infos */
			readPeerInfo(args[0], debug);
			readCommon(args[0], debug);

			/** Get peer's system parameter */
			SystemInfo sysInfo = SystemInfo.getSingletonObj();
			
			/** Set up peer's logger */
			LogHandler logging = new LogHandler();
			logging.logSystemParam();
			logging.logEstablishPeer();

			/** Set up peer's file manager */

			// TODO 
			// 1. Check file exist and hasFile flag
			String peerFileDir = cfgDir + sysInfo.getHostPeer().getId() + '/' + sysInfo.getFileName();
			String mode = sysInfo.getHostPeer().getHasFile() ? "r" : "rw";
			FileManager fm = FileManager.getInstance(
				peerFileDir,
				mode,
				sysInfo.getFileSize(),
				sysInfo.getPieceSize()
			);

      /* Start peer server thread -> inside we create Handler to handle sockets */
			Server server = new Server();
			server.start();

			/* Start building client threads for other target hosts */
			if(!sysInfo.getHostPeer().getHasFile()) {
				logging.writeLog(
					String.format("(peer process) start %s client connections with %s neighbors",
					sysInfo.getNeighborMap().size(),
					sysInfo.getNeighborMap().size()
				));

				for(Entry<String, Peer> n: sysInfo.getNeighborMap().entrySet()) {
					Client client = new Client(n.getValue());
					Thread t = new Thread(client);
					t.start();
				}
			}
			else {
				logging.writeLog("(peer process) Peer hasFile is true, no need start client threads to receive from others");
			}

			logging.writeLog("(peer process) Number of thread create by peer: " + java.lang.Thread.activeCount());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
  
}