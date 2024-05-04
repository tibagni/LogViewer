package com.tibagni.logviewer.logger;

import com.tibagni.logviewer.rc.LogLevelConfig;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;

public class Logger {
  private static final int MAX_LOGS_CACHE = 500;
  private static final ArrayDeque<String> logsCache = new ArrayDeque<>(MAX_LOGS_CACHE);
  private static final Object logsCacheLock = new Object();

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

  // Use same format as Android so we can use LogViewer to analyze its own logs
  private static void log(LogLevelConfig.Level level, String message) {
    // 04-02 13:16:34.662 <pid> <tid> <level> <message>
    String pattern = "dd-MM HH:mm:ss.SSS";
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
    String date = simpleDateFormat.format(new Date());

    String levelIndicator = level.name().substring(0, 1).toUpperCase();
    DecimalFormat tidFormat = new DecimalFormat("000");
    String tid = tidFormat.format(Thread.currentThread().getId());
    String pid = "001"; // TODO use the real PID one day if needed

    String logMessage = date + " " + pid + " " + tid + " " + levelIndicator + " " + getCallingClassName() + ": " + message;
    if (level == LogLevelConfig.Level.WARNING || level == LogLevelConfig.Level.ERROR) {
      errorStream.println(logMessage);
    } else {
      debugStream.println(logMessage);
    }
    addLogToCache(logMessage);
  }

  private static String getCallingClassName() {
    StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
    if (stackTraceElements.length > 4) {
      // Index 0 is getStackTrace(), 1 is getCallingClassName(), 2 is log(), 3 is the caller (which is inside
      // the Logger class), 4 is the actual caller
      StackTraceElement caller = stackTraceElements[4];
      String className = caller.getClassName();
      int lastDotIndex = className.lastIndexOf('.');
      if (lastDotIndex != -1 && lastDotIndex < className.length() - 1) {
        return className.substring(lastDotIndex + 1);
      }
      return className; // If there is no package name
    }
    return "Unknown";
  }

  private static void addLogToCache(String log) {
    synchronized (logsCacheLock) {
      if (logsCache.size() >= MAX_LOGS_CACHE) {
        // We reached the maximum size, drop the first log before inserting the new one
        logsCache.pollFirst();
      }

      logsCache.add(log);
    }
  }

  public static void dump(PrintWriter pw) {
    synchronized (logsCacheLock) {
      while (!logsCache.isEmpty()) {
        pw.println(logsCache.removeFirst());
      }
    }
  }

  static boolean isLoggable(LogLevelConfig.Level level) {
    return level.compareTo(logLevel) >= 0;
  }

  static boolean isDebugLevel() {
    return isLoggable(LogLevelConfig.Level.DEBUG);
  }
}