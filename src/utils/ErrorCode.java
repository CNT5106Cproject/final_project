package utils;

public class ErrorCode {
  /**
  * ErrorCode List
  */
  public ErrorCode() { }

  /**
  * Invalidation
  */
  public static int invalidArgumentLength = 3000;
  public static int invalidPeerID = 3001;

  /**
  * Missing Important Objects
  * - Singleton object error occurs
  */
  public static int missSystemInfo = 4000;
  public static int missLogHandler = 4001;
  public static int missFileManager = 4002;
  public static int missServerConn = 4003;
  public static int missServerOpStream = 4004;
  public static int missClientConn = 4005;
  public static int missClientOpStream = 4006;
  public static int missActMsgObj = 4007;

  /**
  * System error
  */
  public static int failParsePeerInfo = 5000;
  public static int failHandshake = 5001;
  public static int invalidHandshakeHeader = 5002;
  public static int invalidNeighbor = 5003;
}
