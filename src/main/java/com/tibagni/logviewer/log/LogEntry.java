package com.tibagni.logviewer.log;

import java.awt.*;

public class LogEntry implements Comparable<LogEntry> {

  private int index;
  private LogTimestamp timestamp;
  private StringBuilder logText;
  private LogLevel logLevel;
  private Color filterColor;

  public LogEntry(String logText, LogLevel logLevel, LogTimestamp timestamp) {
    this.logText = new StringBuilder(logText);
    this.logLevel = logLevel;
    this.timestamp = timestamp;
  }

  public String getLogText() {
    return logText.toString();
  }

  public LogLevel getLogLevel() {
    return logLevel;
  }

  public void appendText(String text) {
    logText.append(text);
  }

  public Color getFilterColor() {
    return filterColor;
  }

  public void setFilterColor(Color filterColor) {
    this.filterColor = filterColor;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public void setTimestamp(LogTimestamp timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public String toString() {
    return getLogText();
  }

  @Override
  public int compareTo(LogEntry o) {
    if (timestamp == null && o.timestamp == null) return 0;
    if (timestamp == null) return -1;
    if (o.timestamp == null) return 1;

    return timestamp.compareTo(o.timestamp);
  }
}
