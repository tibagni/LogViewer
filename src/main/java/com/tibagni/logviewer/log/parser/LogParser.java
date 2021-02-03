package com.tibagni.logviewer.log.parser;

import com.tibagni.logviewer.ProgressReporter;
import com.tibagni.logviewer.log.*;
import com.tibagni.logviewer.logger.Logger;
import com.tibagni.logviewer.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogParser {
  // This is the maximum size of a payload log from Android
  private static final int LOGGER_ENTRY_MAX_PAYLOAD = 4068;
  // Even though Android limits its buffer for log payload to LOGGER_ENTRY_MAX_PAYLOAD
  // There are other parts of the log, like TAG, timestamp, pid, tid...
  // So, to be absolute sure we will not discard a valid log file because
  // of size restriction, set our maximum to twice the Android's payload size.
  public static final int MAX_LOG_LINE_ALLOWED = LOGGER_ENTRY_MAX_PAYLOAD * 2;

  private static final Pattern LOG_LEVEL_PATTERN =
      Pattern.compile("^\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}.*?([VDIWE])");
  private static final String LOG_START_PATTERN = "^\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.*";
  private static final Pattern LOG_TIMESTAMP_PATTERN =
      Pattern.compile("^(\\d{2})-(\\d{2})\\s(\\d{2}):(\\d{2}):(\\d{2}).(\\d{3})");

  private LogReader logReader;
  private List<LogEntry> logEntries;
  private ProgressReporter progressReporter;
  private final List<String> logsSkipped;
  private final Map<String, String> potentialBugReports;

  public LogParser(LogReader logReader, ProgressReporter progressReporter) {
    this.logReader = logReader;
    this.progressReporter = progressReporter;
    this.logEntries = new ArrayList<>();
    this.logsSkipped = new ArrayList<>();
    this.potentialBugReports = new HashMap<>();
  }

  public LogEntry[] parseLogs() throws LogReaderException {
    ensureState();

    logReader.readLogs();
    Set<String> availableLogs = logReader.getAvailableLogsNames();

    int logsRead = 0;
    for (String log : availableLogs) {
      try {
        int progress = logsRead++ * 90 / availableLogs.size();
        progressReporter.onProgress(progress, "Reading " + log + "...");
        String logText = logReader.get(log);
        List<LogEntry> logEntriesFromFile = getLogEntries(logText, log);

        if (!logEntriesFromFile.isEmpty()) {
          logEntries.addAll(logEntriesFromFile);
        } else {
          Logger.warning("Skipping " + log + " because it was empty");
          logsSkipped.add(log);
        }
      } catch(Exception e) {
        Logger.warning("Skipping " + log + " because it failed to parse", e);
        logsSkipped.add(log);
      }
    }

    if (availableLogs.size() > 1) {
      progressReporter.onProgress(91, "Sorting...");
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

  @NotNull
  public List<String> getLogsSkipped() {
    return logsSkipped;
  }

  @NotNull
  public Map<String, String> getPotentialBugReports() {
    return potentialBugReports;
  }

  @NotNull
  public Set<LogStream> getAvailableStreams() {
    ensureState();

    Set<LogStream> availableStreams = new HashSet<>();
    Set<String> availableLogsNames = logReader.getAvailableLogsNames();
    for (String logName : availableLogsNames) {
      availableStreams.add(LogStream.inferLogStreamFromName(logName));
    }

    return availableStreams;
  }

  private void ensureState() {
    if (logReader == null || logEntries == null || progressReporter == null) {
      throw new IllegalStateException("LogParser was already released. Cannot use it...");
    }
  }

  public void release() {
    logEntries.clear();
    logEntries = null;

    progressReporter = null;

    logReader.close();
    logReader = null;
  }

  private List<LogEntry> getLogEntries(String logText, String logName) {
    String[] lines = logText.split(StringUtils.LINE_SEPARATOR);
    List<LogEntry> logLines = new ArrayList<>(lines.length);

    StringBuilder currentLogLine = null;
    for (String line : lines) {
      // Sometimes a line can contain a lot of NULL chars at the end, making it fail when trying to open the log
      // (as these NULL chars will make the line length too long). So check here if the line has NULL chars
      // and remove them to avoid failing to open valid log files
      if (!line.isEmpty() && line.charAt(line.length() - 1) == '\u0000') {
        line = line.replaceAll("\\u0000", "");
      }

      if (isLogLine(line)) {
        if (currentLogLine != null) {
          logLines.add(createLogEntry(currentLogLine.toString(), logName));
        }

        currentLogLine = new StringBuilder(line);
      } else if (!shouldIgnoreLine(line) && currentLogLine != null) {
        // This is probably a continuation of a already started log line. Append to it
        if (currentLogLine.length() >= MAX_LOG_LINE_ALLOWED) {
          currentLogLine.delete(MAX_LOG_LINE_ALLOWED, currentLogLine.length());

          // First check if we have already considered this as a potential bugreport. If so,
          // don't waste any more time here
          if (!potentialBugReports.containsKey(logName)) {
            String incorrectLinePreview = currentLogLine.substring(0, 100) + "...";
            Logger.warning(
                "Incorrect format on following line (too long - " + currentLogLine.length() + " bytes):\n" +
                    "\"" + incorrectLinePreview + "\"\n\n" +
                    "Maximum logcat line should be " + LOGGER_ENTRY_MAX_PAYLOAD + " bytes");

            // This could be a bugreport. If this is the case, keep track of it
            if (isPotentialBugReport(logText)) {
              Logger.info("Found a potential bugreport: " + logName);
              potentialBugReports.put(logName, logText);
            }
          }

          // We are done with this line, add it to the list and clear currentLogLine to avoid
          // executing this same code over and over for invalid lines
          logLines.add(createLogEntry(currentLogLine.toString(), logName));
          currentLogLine = null;

          // This could simply be a malformed line, just continue parsing other lines
          continue;
        }
        currentLogLine.append(StringUtils.LINE_SEPARATOR).append(line);
      }
    }

    // Make sure to add the last log line as well
    if (currentLogLine != null) {
      logLines.add(createLogEntry(currentLogLine.toString(), logName));
    }

    return logLines;
  }

  private LogEntry createLogEntry(String logLine, String logName) {
    return new LogEntry(logLine, findLogLevel(logLine), findTimestamp(logLine), logName);
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
    } catch (Exception e) {
      // Don't add a timestamp if we couldn't parse it
      // This should never happen anyway
      Logger.error("Failed to parse timestamp for: " + logLine, e);
    }

    return timestamp;
  }

  private boolean isLogLine(String line) {
    return line.matches(LOG_START_PATTERN);
  }

  private boolean shouldIgnoreLine(String line) {
    return line.startsWith("--------- beginning of");
  }

  private boolean isPotentialBugReport(String logText) {
    return logText.contains("Bugreport format version:");
  }
}
