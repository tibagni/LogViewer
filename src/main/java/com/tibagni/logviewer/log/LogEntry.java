package com.tibagni.logviewer.log;

import com.tibagni.logviewer.filter.Filter;

import java.awt.*;

public class LogEntry implements Comparable<LogEntry> {

  private int index;
  private LogTimestamp timestamp;
  private final StringBuilder logText;
  private final LogLevel logLevel;
  private final String logName;
  private final LogStream logStream;

  private Filter appliedFilter;

  public LogEntry(String logText, LogLevel logLevel, LogTimestamp timestamp) {
    this(logText, logLevel, timestamp, "");
  }

  public LogEntry(String logText, LogLevel logLevel, LogTimestamp timestamp, String logName) {
    this.logText = new StringBuilder(logText);
    this.logLevel = logLevel;
    this.timestamp = timestamp;
    this.logName = logName;
    this.logStream = LogStream.inferLogStreamFromName(logName);
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

  public Filter getAppliedFilter() {
    return appliedFilter;
  }

  public void setAppliedFilter(Filter appliedFilter) {
    this.appliedFilter = appliedFilter;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public LogTimestamp getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(LogTimestamp timestamp) {
    this.timestamp = timestamp;
  }

  public LogStream getStream() {
    return logStream;
  }

  public int getLength() {
    return logText.length();
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
