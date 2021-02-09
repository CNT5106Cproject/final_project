package utils;
import utils.ErrorCode;

public class CustomExceptions extends Exception {
  /**
  * Create a custom exception message wrapper
  */
  private static final long serialVersionUID = 1L;
  private static boolean debug = true;

  public static String errorResponse(int code, String message) {
    return "Error Code: " + code + ", Error Msg: " + message;
  }

  public CustomExceptions(int code, String message) {
    super(errorResponse(code, message));
  }
}
