package utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Filter;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

import peer.Peer;

public class LogHandler {

  private int peerId;
	private String hostName;
	private int port;
  private FileHandler logFH;
  private FileHandler errLogFH;
  private String logDir = "../log";

  private static Logger logger = null;
  static {
    /*
     * Set java logging file handler msg format, java default is XML format
     */
    System.setProperty(
      "java.util.logging.SimpleFormatter.format",
      "[%1$tF %1$tT] [%4$s] [%3$s] %5$s %n"
    );
    logger = Logger.getLogger(LogHandler.class.getName());
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
    String dateString = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
    String logFN = String.format("log_peer_[%s]_[%s].log", this.peerId, dateString);
    String errLogFN = String.format("error_log_peer_[%s]_[%s].log", this.peerId, dateString);
  
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

  }

  public LogHandler(Peer host) {
    this.peerId = host.getId();
    this.hostName = host.getHostName();
    this.port = host.getPort(); 
    createLogFiles();
    logger.addHandler(this.logFH);
    logger.addHandler(this.errLogFH);
  }

  /**
  * Custom msg
  */
  public void writeLog(String msg) {
    logger.info(msg);
  }
  public void writeLog(String msg, String lvl) {
    if(lvl == "severe") {
      logger.severe(msg);
    }
    else if(lvl == "warning") {
      logger.warning(msg);
    }
  }
  
  /**
  * System infos
  */
  public void establishPeer(Peer host) {
    String msg = String.format("Peer [%s] start establishing", host.getId());
    logger.info(msg);
  }

  public void startServer(Peer host) {
    String msg = String.format("Peer [%s] start server thread", host.getId());
    logger.info(msg);
  }

  /**
  * Peer actions
  */
  public void Connect(Peer host, Peer client) {
    // return String.format("Peer [peer_ID 1] makes a connection to Peer [peer_ID 2]");
  }
  
}
