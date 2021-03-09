package utils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Filter;
import java.text.SimpleDateFormat;

import peer.Peer;
import peer.SystemInfo;

public final class LogHandler {

  private FileHandler logFH = null;
  private FileHandler errLogFH = null;
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
    String errLogFN = String.format("error_log_peer_[%s]_[%s].log", sysInfo.getHostPeer().getId(), dateString);

    try {
      this.logFH = new FileHandler(this.logDir + "/" + logFN, true);
      this.logFH.setFormatter(new SimpleFormatter());

      this.errLogFH = new FileHandler(this.logDir + "/" + errLogFN, true);
      this.errLogFH.setFormatter(new SimpleFormatter());
      this.errLogFH.setFilter(new logFilter(Level.SEVERE));
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
      createLogFiles();
      logger.addHandler(this.logFH);
      logger.addHandler(this.errLogFH);
    }
  }

  /**
  * Custom Messages
  */
  public void writeLog(String msg) {
    logger.info(String.format("Peer [%s] %s", sysInfo.getHostPeer().getId(), msg));
  }

  public void writeLog(String lvl, String msg) {
    String hostPeerId = sysInfo.getHostPeer().getId();
    if(lvl == "severe") {
      logger.severe(String.format("Peer [%s] %s", hostPeerId, msg));
    }
    else if(lvl == "warning") {
      logger.warning(String.format("Peer [%s] %s", hostPeerId, msg));
    }
    else {
      logger.finest(String.format("Peer [%s] %s", hostPeerId, msg));
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
    logger.info(msg);
  }

  /**
  * System Actions
  */
  public void logEstablishPeer() {
    String msg = String.format("Peer [%s] start establishing host peer", sysInfo.getHostPeer().getId());
    logger.info(msg);
  }

  public void logStartServer() {
    String msg = String.format("Peer [%s] start server thread", sysInfo.getHostPeer().getId());
    logger.info(msg);
  }

  public void logStartClient(Peer targetHost) {
    String msg = String.format(
      "Peer [%s] start client thread, connecting to [%s]", sysInfo.getHostPeer().getId(), targetHost.getId()
    );
    logger.info(msg);
  }
  
  /**
  * Peer actions - [Important] 
  * Do not forget to follow the project description format.
  */
  // 1. TCP connection
  public void logStartConn(Peer client, Peer targetHost) {
    String msg = String.format("Peer [%s] makes a connection to Peer [%s]", client.getId(), targetHost.getId());
    logger.info(msg);
  }
  
  // 2. change of preferred neighbors
  // 3. change of optimistically unchoked neighbor
  // 4. unchoking
  // 5. choking
  // 6. receiving ‘have’ message
  // 7. receiving ‘interested’ message
  // 8. receiving ‘not interested’ message
  // 9. downloading a piece
  // 10. completion of download
  
  /**
  * Peer action errors
  */
  public void logConnError(Peer client, Peer targetHost) {
    String msg = String.format("Peer [%s] occurs connection error with Peer [%s], start retry in [%s] sec", 
      client.getId(), 
      targetHost.getId(),
      sysInfo.getRetryInterval()
    );
    logger.info(msg);
  }
  
}
