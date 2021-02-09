package utils;
import utils.ErrorCode;

public class CustomExceptions extends Exception {
  /**
  * Create a custom exception message wrapper
  */
  private static final long serialVersionUID = 1L;

  public CustomExceptions(String message, String code) {
    super(errorResponse(message, code));
  }

  private static String errorResponse(String message, String code) {
    return "Error Msg: " + message + ", Error Code: " + code;
  }
}
