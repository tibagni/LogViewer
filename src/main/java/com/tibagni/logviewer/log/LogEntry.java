package com.tibagni.logviewer.log;

import com.tibagni.logviewer.filter.Filter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LogEntry implements Comparable<LogEntry> {

  private int index;
  public final LogTimestamp timestamp;
  public final String logText;
  public final LogLevel logLevel;
  public final LogStream logStream;

  private Filter appliedFilter;
  @Nullable
  private Filter searchFilter;

  public LogEntry(String logText, LogLevel logLevel, LogTimestamp timestamp) {
    this(logText, logLevel, timestamp, "");
  }

  public LogEntry(String logText, LogLevel logLevel, LogTimestamp timestamp, String logName) {
    this.logText = logText;
    this.logLevel = logLevel;
    this.timestamp = timestamp;
    this.logStream = LogStream.inferLogStreamFromName(logName);
  }

  public String getLogText() {
    return logText;
  }

  public LogLevel getLogLevel() {
    return logLevel;
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

  public LogStream getStream() {
    return logStream;
  }

  public int getLength() {
    return logText.length();
  }

  @Nullable
  public Filter getSearchFilter() {
    return searchFilter;
  }

  public LogEntry setSearchFilter(@Nullable Filter searchFilter) {
    this.searchFilter = searchFilter;
    return this;
  }

  @Override
  public String toString() {
    return getLogText();
  }

  @Override
  public int compareTo(@NotNull LogEntry o) {
    if (timestamp == null && o.timestamp == null) return 0;
    if (timestamp == null) return -1;
    if (o.timestamp == null) return 1;

    return timestamp.compareTo(o.timestamp);
  }
}
