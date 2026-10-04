package com.tibagni.logviewer.log;

import com.tibagni.logviewer.filter.Filter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;

public class LogEntry implements Comparable<LogEntry> {

  private int index;
  public final LogTimestamp timestamp;
  public final String logText;
  public final LogLevel logLevel;
  public final LogStream logStream;
  @Nullable
  private final File sourceFile;
  private final int lineNumber;

  private Filter appliedFilter;

  public LogEntry(String logText, LogLevel logLevel, LogTimestamp timestamp) {
    this(logText, logLevel, timestamp, "");
  }

  public LogEntry(String logText, LogLevel logLevel, LogTimestamp timestamp, String logName) {
    this(logText, logLevel, timestamp, LogStream.inferLogStreamFromName(logName), null, 0);
  }

  public LogEntry(String logText, LogLevel logLevel, LogTimestamp timestamp, LogStream logStream) {
    this(logText, logLevel, timestamp, logStream, null, 0);
  }

  public LogEntry(String logText, LogLevel logLevel, LogTimestamp timestamp, LogStream logStream,
                  @Nullable File sourceFile, int lineNumber) {
    this.logText = logText;
    this.logLevel = logLevel;
    this.timestamp = timestamp;
    this.logStream = logStream != null ? logStream : LogStream.UNKNOWN;
    this.sourceFile = sourceFile;
    this.lineNumber = lineNumber;
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

  @Nullable
  public File getSourceFile() {
    return sourceFile;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public int getLength() {
    return logText.length();
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

    int time = timestamp.compareTo(o.timestamp);
    // compare index if same time
    return time == 0 ? Integer.compare(index, o.index) : time;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LogEntry logEntry = (LogEntry) o;
    return index == logEntry.index &&
        lineNumber == logEntry.lineNumber &&
        Objects.equals(sourceFile, logEntry.sourceFile) &&
        Objects.equals(timestamp, logEntry.timestamp) &&
        Objects.equals(logText, logEntry.logText) &&
        logLevel == logEntry.logLevel &&
        logStream == logEntry.logStream;
  }

  @Override
  public int hashCode() {
    return Objects.hash(index, timestamp, logText, logLevel, logStream, sourceFile, lineNumber);
  }
}
