package utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

public class Tools {
  public static void timeSleep(long milleSec) {
    try {
      TimeUnit.MILLISECONDS.sleep(milleSec);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static String getStackTrace(Throwable throwable) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw, true);
    throwable.printStackTrace(pw);
    return sw.getBuffer().toString();
  }
}
