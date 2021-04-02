package utils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.sound.midi.Receiver;

import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Filter;
import java.text.SimpleDateFormat;

import peer.Peer;
import peer.SystemInfo;

public final class LogHandler {

  private FileHandler logFH = null; // Project descriptions log - info level
  private FileHandler debugLogFH = null; // Debug logs
  private String logDir = "../log";

  private static Logger logger = null;
  private static SystemInfo sysInfo = SystemInfo.getSingletonObj();

  static {
    /*
    * Set java logging file handler msg format, java default is XML format
    */
    System.setProperty(
      "java.util.logging.SimpleFormatter.format",
      "[%1$tF %1$tT] [%4$s] [%3$s] %5$s %n"
    );
  }

  private static class logFilter implements Filter {
    private final Level upper;
    logFilter(final Level upper) {
      this.upper = upper;
    }
    
    @Override
    public boolean isLoggable(LogRecord record) {
        return record.getLevel().intValue() >= upper.intValue();
    }
  }

  /**
  * Create log files for peer
  */
  private void createLogFiles() {
    File isDir = new File(this.logDir);
    if (!isDir.exists()){
      isDir.mkdir();
    }
    
    /* 
    * Set file name with date
    */
    String dateString = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    String logFN = String.format("log_peer_[%s]_[%s].log", sysInfo.getHostPeer().getId(), dateString);
    String debugLogFN = String.format("debug_log_peer_[%s]_[%s].log", sysInfo.getHostPeer().getId(), dateString);

    try {
      this.debugLogFH = new FileHandler(this.logDir + "/" + debugLogFN, true);
      this.debugLogFH.setFormatter(new SimpleFormatter());
      
      this.logFH = new FileHandler(this.logDir + "/" + logFN, true);
      this.logFH.setFormatter(new SimpleFormatter());
      this.logFH.setFilter(new logFilter(Level.INFO));
    } catch (SecurityException e) {  
      e.printStackTrace();
    } catch (IOException e) {  
      e.printStackTrace();  
    }
  }

  public LogHandler() {
    /** 
     * Create logs, and adding log handlers
    */
    if(logger == null) {
      logger = Logger.getLogger(LogHandler.class.getName());
      logger.setLevel(Level.FINE);
      createLogFiles();
      logger.addHandler(this.logFH);
      logger.addHandler(this.debugLogFH);
    }
  }

  /**
  * Custom Messages
  */
  public void writeLog(String msg) {
    logger.fine(String.format("Peer [%s] %s", sysInfo.getHostPeer().getId(), msg));
  }

  public void writeLog(String lvl, String msg) {
    String hostPeerId = sysInfo.getHostPeer().getId();
    if(lvl == "severe") {
      logger.severe(String.format("Peer [%s] %s", hostPeerId, msg));
    }
    else if(lvl == "warning") {
      logger.warning(String.format("Peer [%s] %s", hostPeerId, msg));
    }
    else if(lvl == "info") {
      logger.info(String.format("Peer [%s] %s", hostPeerId, msg));
    }
    else {
      logger.fine(String.format("Peer [%s] %s", hostPeerId, msg));
    }
  }
  
  /**
   * 
   *
   */
  public void logSystemParam() {
    String msg = String.format(
      "System Params: PreferN [%s], UnChokingInr [%s], OptUnChokingInr [%s], FileName [%s], FileSize [%s], PieceSize [%s]", 
      sysInfo.getPreferN(),
      sysInfo.getUnChokingInr(),
      sysInfo.getOptUnChokingInr(),
      sysInfo.getFileName(),
      sysInfo.getFileSize(),
      sysInfo.getPieceSize()
    );
    logger.fine(msg);
  }

  /**
  * System Actions
  */
  public void logEstablishPeer() {
    String msg = String.format("Peer [%s] Start establishing host peer", sysInfo.getHostPeer().getId());
    logger.fine(msg);
  }

  public void logStartServer() {
    String msg = String.format("Peer [%s] Start server thread", sysInfo.getHostPeer().getId());
    logger.fine(msg);
  }

  public void logStartClient(Peer targetHost) {
    String msg = String.format(
      "Peer [%s] Start client thread, connecting to [%s]", sysInfo.getHostPeer().getId(), targetHost.getId()
    );
    logger.fine(msg);
  }

  public void logHandShakeSuccess(Peer sender, Peer recv) {
    String msg = String.format(
      "Peer [%s] set up handshake with [%s] SUCCESS", 
      sender.getId(), 
      recv.getId()
    );
    logger.fine(msg);
  }
  
  /**
  * Peer action errors
  */
  public void logConnError(Peer client, Peer targetHost) {
    String msg = String.format("Peer [%s] (client) occurs connection ERROR with Peer [%s], Start retry in [%s] sec", 
      client.getId(), 
      targetHost.getId(),
      sysInfo.getRetryInterval()
    );
    logger.severe(msg);
  }

  /**
   * [Important] 
   * Peer actions - Using Level.INFO to log the action
   * Do not forget to follow the project description format.
  */
  // 1. TCP connection
  public void logStartConn(Peer client, Peer targetHost) {
    String msg = String.format("Peer [%s] (client) makes a connection to Peer [%s]", client.getId(), targetHost.getId());
    logger.info(msg);
  }
  
  // 2. change of preferred neighbors
  public void logChangePrefersPeers() {

  }

  // 3. change of optimistically unchoked neighbor
  public void logChangeUnchokedPeer() {

  }
  
  // 4. unchoking
  public void logUnchoking(Peer sender) {
    String msg = String.format("Peer [%s] received the ‘unchoke’ message from [%s]", sysInfo.getHostPeer().getId(), sender.getId());
    logger.info(msg);
  }

  // 5. choking
  public void logChoking(Peer sender) {
    String msg = String.format("Peer [%s] received the ‘choke’ message from [%s]", sysInfo.getHostPeer().getId(), sender.getId());
    logger.info(msg);
  }

  // 6. receiving ‘have’ message
  public void logReceiveHaveMsg(Peer sender) {
    String msg = String.format("Peer [%s] received the ‘have’ message from [%s]", sysInfo.getHostPeer().getId(), sender.getId());
    logger.info(msg);
  }

  // 7. receiving ‘interested’ message
  public void logReceiveInterestMsg(Peer sender) {
    String msg = String.format("Peer [%s] received the ‘interested’ message from [%s]", sysInfo.getHostPeer().getId(), sender.getId());
    logger.info(msg);
  }

  // 8. receiving ‘not interested’ message
  public void logReceiveNotInterestMsg(Peer sender) {
    String msg = String.format("Peer [%s] received the ‘not interested’ message from [%s]", sysInfo.getHostPeer().getId(), sender.getId());
    logger.info(msg);
  }

  // 9. downloading a piece
  public void logDownload(Peer sender, int blockIdx, int numBlocks) {
    String msg = String.format(
      "Peer [%s] has downloaded the piece [%s] from [%s]. Now the number of pieces it has is [%s]", 
      sysInfo.getHostPeer().getId(), 
      blockIdx,
      sender.getId(),
      numBlocks
    );
    logger.info(msg);
  }

  // 10. completion of download
  public void logCompleteFile() {
    String msg = String.format("Peer [%s] has downloaded the complete file.  ", sysInfo.getHostPeer().getId());
    logger.info(msg);
  }
  
  public void logCloseConn(Peer targetPeer) {
    String msg = String.format("Peer [%s] close connection with Peer [%s]", 
      sysInfo.getHostPeer().getId(), 
      targetPeer.getId()
    );
    logger.info(msg);
  }

  public void logSendHandShakeMsg(String targetPeerID, String threadType) {
    logger.info(String.format("Peer [%s] (%s) sending handshake message to peer [%s]", sysInfo.getHostPeer().getId(), threadType, targetPeerID));
  }

  public void logReceiveHandShakeMsg(String senderId) {
    String msg = String.format("Peer [%s] received the ‘handshake’ message from [%s]", sysInfo.getHostPeer().getId(), senderId);
    logger.info(msg);
  }

  public void logSendBitFieldMsg(Peer recv) {
    logger.info(String.format("Peer [%s] (server) sending bitfield message to peer [%s]", sysInfo.getHostPeer().getId(), recv.getId()));
  }

  public void logBitFieldMsg(Peer sender) {
    String msg = String.format("Peer [%s] (client) received the ‘bitfield’ message from [%s]", sysInfo.getHostPeer().getId(), sender.getId());
    logger.info(msg);
  }

  public void logReceiveRequestMsg(Peer sender) {
    String msg = String.format("Peer [%s] (server) received the ‘request’ message from [%s]", sysInfo.getHostPeer().getId(), sender.getId());
    logger.info(msg);
  }

  public void logReceivePieceMsg(Peer sender) {
    String msg = String.format("Peer [%s] (client) received the ‘piece’ message from [%s]", sysInfo.getHostPeer().getId(), sender.getId());
    logger.info(msg);
  }
}
