package com.tibagni.logviewer.log;

import java.util.HashMap;
import java.util.Map;

public enum LogStream {
  MAIN,
  SYSTEM,
  RADIO,
  EVENTS,
  KERNEL,
  UNKNOWN;

  private static Map<LogStream, String[]> logNamesMap = new HashMap<>();
  static {
    String[] mainLogNames = {"main", "-m."};
    String[] systemLogNames = {"system", "-s."};
    String[] radioLogNames = {"radio", "-r."};
    String[] eventsLogNames = {"events", "-e."};
    String[] kernelLogNames = {"kernel", "-k."};
    logNamesMap.put(MAIN, mainLogNames);
    logNamesMap.put(SYSTEM, systemLogNames);
    logNamesMap.put(RADIO, radioLogNames);
    logNamesMap.put(EVENTS, eventsLogNames);
    logNamesMap.put(KERNEL, kernelLogNames);
  }

  public static LogStream inferLogStreamFromName(String logName) {
    for (LogStream stream : logNamesMap.keySet()) {
      String[] possibilities = logNamesMap.get(stream);
      if (matchesWith(possibilities, logName)) {
        return stream;
      }
    }

    return UNKNOWN;
  }

  private static boolean matchesWith(String[] possibilities, String actualName) {
    if (possibilities == null || possibilities.length == 0 ||
        actualName == null || actualName.isEmpty()) {
      return false;
    }

    for (String possibility : possibilities) {
      if (actualName.toUpperCase().contains(possibility.toUpperCase())) {
        return true;
      }
    }

    return false;
  }

  @Override
  public String toString() {
    String str = super.toString();
    return str.charAt(0) + str.substring(1).toLowerCase();
  }

  public String getSymbol() {
    return super.toString().substring(0, 1).toLowerCase();
  }
}
