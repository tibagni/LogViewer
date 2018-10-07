package com.tibagni.logviewer.util;

import com.tibagni.logviewer.logger.Logger;

public class CommonUtils {

  public static void sleepSilently(long duration) {
    try {
      Thread.sleep(duration);
    } catch (InterruptedException e) {
      Logger.error("Thread.sleep was interrupted", e);
    }
  }
}
