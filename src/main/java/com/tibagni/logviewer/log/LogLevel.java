package com.tibagni.logviewer.log;

public enum LogLevel {
  VERBOSE,
  DEBUG,
  INFO,
  WARNING,
  ERROR;

  public static LogLevel createFromStringLevel(String level) {
    switch (level) {
      case "V":
        return LogLevel.VERBOSE;
      case "D":
        return LogLevel.DEBUG;
      case "I":
        return LogLevel.INFO;
      case "W":
        return LogLevel.WARNING;
      case "E":
        return LogLevel.ERROR;
    }

    return LogLevel.DEBUG;
  }
}
