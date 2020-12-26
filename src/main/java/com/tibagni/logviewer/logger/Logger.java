package com.tibagni.logviewer.logger;

import com.tibagni.logviewer.rc.LogLevelConfig;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
  private static LogLevelConfig.Level logLevel = LogLevelConfig.DEFAULT_LEVEL;

  private Logger() {}

  private static final PrintStream debugStream = System.out;
  private static final PrintStream errorStream = System.err;

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

    log(LogLevelConfig.Level.INFO, "Log Level set to " + logLevel.name());
  }

  public static void verbose(String message) {
    if (isLoggable(LogLevelConfig.Level.VERBOSE)) {
      log(LogLevelConfig.Level.VERBOSE, message);
    }
  }

  public static void debug(String message) {
    if (isLoggable(LogLevelConfig.Level.DEBUG)) {
      log(LogLevelConfig.Level.DEBUG, message);
    }
  }

  public static void info(String message) {
    if (isLoggable(LogLevelConfig.Level.INFO)) {
      log(LogLevelConfig.Level.INFO, message);
    }
  }

  public static void warning(String message) {
    if (isLoggable(LogLevelConfig.Level.WARNING)) {
      log(LogLevelConfig.Level.WARNING, message);
    }
  }

  public static void warning(String message, Throwable throwable) {
    if (isLoggable(LogLevelConfig.Level.WARNING)) {
      log(LogLevelConfig.Level.WARNING, message);
      throwable.printStackTrace(debugStream);
    }
  }

  public static void error(String message) {
    if (isLoggable(LogLevelConfig.Level.ERROR)) {
      log(LogLevelConfig.Level.ERROR, message);
    }
  }

  public static void error(String message, Throwable throwable) {
    if (isLoggable(LogLevelConfig.Level.ERROR)) {
      log(LogLevelConfig.Level.ERROR, message);
      throwable.printStackTrace(errorStream);
    }
  }

  private static void log(LogLevelConfig.Level level, String message) {
    // 04-02 13:16:34.662  message
    String pattern = "dd-MM HH:mm:ss.S";
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
    String date = simpleDateFormat.format(new Date());

    String levelIndicator = level.name().substring(0,1).toUpperCase();

    String logMessage = date + " " + levelIndicator + " " + message;
    if (level == LogLevelConfig.Level.WARNING || level == LogLevelConfig.Level.ERROR) {
      errorStream.println(logMessage);
    } else {
      debugStream.println(logMessage);
    }
  }

  private static boolean isLoggable(LogLevelConfig.Level level) {
    return level.compareTo(logLevel) >= 0;
  }
}