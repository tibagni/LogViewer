package com.tibagni.logviewer.logger;

import com.tibagni.logviewer.rc.LogLevelConfig;

import java.io.PrintStream;

public class Logger {
  private static LogLevelConfig.Level logLevel = LogLevelConfig.DEFAULT_LEVEL;

  private Logger() {}

  private static PrintStream debugStream = System.out;
  private static PrintStream errorStream = System.err;

  public static PrintStream getDebugStream() {
    return debugStream;
  }

  public static PrintStream getErrorStream() {
    return errorStream;
  }

  public static void initialize(LogLevelConfig config) {
    if (config != null) {
      logLevel = config.getConfigValue();
    }

    debugStream.println("Log Level set to " + logLevel.name());
  }

  public static void verbose(String message) {
    if (isLoggable(LogLevelConfig.Level.VERBOSE)) {
      debugStream.println(message);
    }
  }

  public static void debug(String message) {
    if (isLoggable(LogLevelConfig.Level.DEBUG)) {
      debugStream.println(message);
    }
  }

  public static void info(String message) {
    if (isLoggable(LogLevelConfig.Level.INFO)) {
      debugStream.println(message);
    }
  }

  public static void warning(String message) {
    if (isLoggable(LogLevelConfig.Level.WARNING)) {
      debugStream.println(message);
    }
  }

  public static void warning(String message, Throwable throwable) {
    if (isLoggable(LogLevelConfig.Level.WARNING)) {
      debugStream.println(message);
      throwable.printStackTrace(debugStream);
    }
  }

  public static void error(String message) {
    if (isLoggable(LogLevelConfig.Level.ERROR)) {
      errorStream.println(message);
    }
  }

  public static void error(String message, Throwable throwable) {
    if (isLoggable(LogLevelConfig.Level.ERROR)) {
      errorStream.println(message);
      throwable.printStackTrace(errorStream);
    }
  }

  private static boolean isLoggable(LogLevelConfig.Level level) {
    return level.compareTo(logLevel) >= 0;
  }
}
