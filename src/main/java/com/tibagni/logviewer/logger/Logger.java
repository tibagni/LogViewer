package com.tibagni.logviewer.logger;

import java.io.PrintStream;

public class Logger {
  private Logger() {}

  private static PrintStream debugStream = System.out;
  private static PrintStream errorStream = System.err;

  public static PrintStream getDebugStream() {
    return debugStream;
  }

  public static PrintStream getErrorStream() {
    return errorStream;
  }

  public static void debug(String message) {
    debugStream.println(message);
  }

  public static void error(String message) {
    errorStream.println(message);
  }

  public static void error(String message, Throwable throwable) {
    errorStream.println(message);
    throwable.printStackTrace(errorStream);
  }
}
