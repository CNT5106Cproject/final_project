package utils;

import java.util.concurrent.TimeUnit;

public class Tools {
  public static void timeSleep(int sec) {
    try {
      TimeUnit.SECONDS.sleep(sec);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
