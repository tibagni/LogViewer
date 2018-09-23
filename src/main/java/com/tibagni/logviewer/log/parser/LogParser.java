package com.tibagni.logviewer.log.parser;

import com.tibagni.logviewer.ProgressReporter;
import com.tibagni.logviewer.log.*;
import com.tibagni.logviewer.logger.Logger;
import com.tibagni.logviewer.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogParser {
  private static final Pattern LOG_LEVEL_PATTERN =
      Pattern.compile("^\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}.*?([VDIWE])");
  private static final String LOG_START_PATTERN = "^\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.*";
  private static final Pattern LOG_TIMESTAMP_PATTERN =
      Pattern.compile("^(\\d{2})-(\\d{2})\\s(\\d{2}):(\\d{2}):(\\d{2}).(\\d{3})");

  private LogReader logReader;
  private List<LogEntry> logEntries;
  private ProgressReporter progressReporter;

  public LogParser(LogReader logReader, ProgressReporter progressReporter) {
    this.logReader = logReader;
    this.progressReporter = progressReporter;
    this.logEntries = new ArrayList<>();
  }

  public LogEntry[] parseLogs() throws LogReaderException {
    if (logReader == null || logEntries == null || progressReporter == null) {
      throw new IllegalStateException("LogParser was already released. Cannot use it...");
    }

    logReader.readLogs();
    Set<String> availableLogs = logReader.getAvailableLogsNames();
    int logsRead = 0;
    for (String log : availableLogs) {
      int progress = logsRead++ * 60 / availableLogs.size();
      progressReporter.onProgress(progress, "Reading " + log + "...");
      logEntries.addAll(getLogEntries(logReader.get(log)));
    }

    if (availableLogs.size() > 1) {
      progressReporter.onProgress(80, "Sorting...");
      Collections.sort(logEntries);
    }

    progressReporter.onProgress(95, "Setting index...");

    int index = 0;
    for (LogEntry entry : logEntries) {
      entry.setIndex(index++);
    }

    progressReporter.onProgress(100, "Completed");
    return logEntries.toArray(new LogEntry[0]);
  }

  public void release() {
    logEntries.clear();
    logEntries = null;

    progressReporter = null;

    logReader.close();
    logReader = null;
  }

  private List<LogEntry> getLogEntries(String logText) {
    String[] lines = logText.split(StringUtils.LINE_SEPARATOR);
    List<LogEntry> logLines = new ArrayList<>(lines.length);

    for (String line : lines) {
      if (isLogLine(line)) {
        LogEntry entry = new LogEntry(line, findLogLevel(line), findTimestamp(line));
        logLines.add(entry);
      } else if (!shouldIgnoreLine(line) && logLines.size() > 0) {
        // This is probably a continuation of a already started log line. Append to it
        LogEntry currentLine = logLines.get(logLines.size() - 1);
        currentLine.appendText(StringUtils.LINE_SEPARATOR + line);
      }
    }

    return logLines;
  }

  LogLevel findLogLevel(String logLine) {
    LogLevel logLevel = LogLevel.DEBUG;

    Matcher matcher = LOG_LEVEL_PATTERN.matcher(logLine);
    if (matcher.find()) {
      logLevel = LogLevel.createFromStringLevel(matcher.group(1));
    }

    return logLevel;
  }

  LogTimestamp findTimestamp(String logLine) {
    LogTimestamp timestamp = null;

    try {
      Matcher matcher = LOG_TIMESTAMP_PATTERN.matcher(logLine);
      if (matcher.find()) {
        timestamp = new LogTimestamp(matcher.group(1),
            matcher.group(2),
            matcher.group(3),
            matcher.group(4),
            matcher.group(5),
            matcher.group(6));
      }
    } catch (Exception ignore) {
      // Don't add a timestamp if we couldn't parse it
      // This should never happen anyway
      Logger.error("Failed to parse timestamp for: " + logLine, ignore);
    }

    return timestamp;
  }

  private boolean isLogLine(String line) {
    return line.matches(LOG_START_PATTERN);
  }

  private boolean shouldIgnoreLine(String line) {
    return line.startsWith("--------- beginning of");
  }
}
