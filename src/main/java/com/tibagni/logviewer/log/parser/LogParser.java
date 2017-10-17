package com.tibagni.logviewer.log.parser;

import com.tibagni.logviewer.log.*;
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
  private ParsingProgress parsingProgress;

  public LogParser(LogReader logReader, ParsingProgress parsingProgress) {
    this.logReader = logReader;
    this.parsingProgress = parsingProgress;
    this.logEntries = new ArrayList<>();
  }

  public LogEntry[] parseLogs() throws LogReaderException {
    logReader.readLogs();
    Set<String> availableLogs = logReader.getAvailableLogsNames();
    int logsRead = 0;
    for (String log : availableLogs) {
      int progress = logsRead++ * 60 / availableLogs.size();
      parsingProgress.onProgress(progress, "Parsing " + log);
      logEntries.addAll(getLogEntries(logReader.get(log)));
    }

    if (availableLogs.size() > 1) {
      parsingProgress.onProgress(80, "Organizing all logs...");
      Collections.sort(logEntries);
    }

    parsingProgress.onProgress(95, "Organizing all logs...");
    int index = 0;
    for (LogEntry entry : logEntries) {
      entry.setIndex(index++);
    }

    parsingProgress.onProgress(100, "Completed");
    return logEntries.toArray(new LogEntry[0]);
  }

  private List<LogEntry> getLogEntries(String logText) {
    String[] lines = logText.split(StringUtils.LINE_SEPARATOR);
    List<LogEntry> logLines = new ArrayList<>(lines.length);

    for (String line : lines) {
      if (isLogLine(line)) {
        LogEntry entry = new LogEntry(line, findLogLevel(line), findTimestamp(line));
        logLines.add(entry);
      } else if (!shouldIgnoreLine(line)) {
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
    }

    return timestamp;
  }

  private boolean isLogLine(String line) {
    return line.matches(LOG_START_PATTERN);
  }

  private boolean shouldIgnoreLine(String line) {
    return line.startsWith("--------- beginning of");
  }

  public interface ParsingProgress {
    void onProgress(int progress, String description);
  }
}
