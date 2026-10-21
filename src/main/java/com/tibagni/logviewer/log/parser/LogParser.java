package com.tibagni.logviewer.log.parser;

import com.tibagni.logviewer.ProgressReporter;
import com.tibagni.logviewer.log.*;
import com.tibagni.logviewer.logger.Logger;
import com.tibagni.logviewer.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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

  private LogReader logReader;
  private List<LogEntry> logEntries;
  private ProgressReporter progressReporter;
  private final List<String> logsSkipped;
  private final Map<String, String> potentialBugReports;
  private final List<TimeDiscontinuity> timeDiscontinuities;

  public LogParser(LogReader logReader, ProgressReporter progressReporter) {
    this.logReader = logReader;
    this.progressReporter = progressReporter;
    this.logEntries = new ArrayList<>();
    this.logsSkipped = new ArrayList<>();
    this.potentialBugReports = new HashMap<>();
    this.timeDiscontinuities = new ArrayList<>();
  }

  public LogEntry[] parseLogs(Charset charset) throws LogReaderException {
    ensureState();

    logReader.readLogs(charset);
    Set<String> availableLogs = logReader.getAvailableLogPaths();

    int logsRead = 0;
    for (String log : availableLogs) {
      try {
        int progress = logsRead++ * 90 / availableLogs.size();
        progressReporter.onProgress(progress, "Reading " + log + "...");
        String logText = logReader.get(log);
        File logFile = logReader.getFile(log);
        List<LogEntry> logEntriesFromFile = getLogEntries(logText, log, logFile);

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

    LogEntry[] result = logEntries.toArray(new LogEntry[0]);

    if (availableLogs.size() > 1 || !isSorted(result)) {
      progressReporter.onProgress(91, "Sorting...");
      Arrays.parallelSort(result);
    }

    progressReporter.onProgress(95, "Setting index...");
    for (int i = 0; i < result.length; i++) {
      result[i].setIndex(i);
    }

    progressReporter.onProgress(100, "Completed");
    return result;
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
  public List<TimeDiscontinuity> getTimeDiscontinuities() {
    return timeDiscontinuities;
  }

  @NotNull
  public Set<LogStream> getAvailableStreams() {
    ensureState();

    Set<LogStream> availableStreams = new HashSet<>();
    Set<String> availableLogsNames = logReader.getAvailableLogPaths();
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

  private List<LogEntry> getLogEntries(String logText, String logPath, File logFile) {
    LogStream logStream = LogStream.inferLogStreamFromName(logPath);
    List<LogEntry> logLines = new ArrayList<>();

    String currentLogLine = null;
    int currentLineNumber = 0;
    StringBuilder continuationBuilder = null;
    LogTimestamp lastTimestamp = null;

    int textLen = logText.length();
    int start = 0;
    int rawLineNumber = 0;
    while (start < textLen) {
      rawLineNumber++;
      int end = logText.indexOf('\n', start);
      if (end < 0) {
        end = textLen;
      }
      int lineEnd = end;
      if (lineEnd > start && logText.charAt(lineEnd - 1) == '\r') {
        lineEnd--;
      }
      String line = sanitizeLine(logText.substring(start, lineEnd));
      start = end + 1;

      if (line.startsWith("--------- beginning of") || line.startsWith("------ ")) {
        lastTimestamp = null;
      }

      if (isLogLine(line)) {
        LogTimestamp currentTimestamp = findTimestamp(line);
        if (lastTimestamp != null && currentTimestamp != null) {
          if (isBackwardTimeDiscontinuity(lastTimestamp, currentTimestamp)) {
            timeDiscontinuities.add(new TimeDiscontinuity(
                logFile,
                logPath,
                rawLineNumber,
                lastTimestamp,
                currentTimestamp
            ));
          }
        }
        if (currentTimestamp != null) {
          lastTimestamp = currentTimestamp;
        }

        flushPendingLogEntry(logLines, continuationBuilder, currentLogLine, logStream, logFile, currentLineNumber);
        continuationBuilder = null;

        currentLogLine = line;
        currentLineNumber = rawLineNumber;
      } else if (!shouldIgnoreLine(line) && currentLogLine != null) {
        // This is probably a continuation of an already started log line. Append to it
        if (continuationBuilder == null) {
          continuationBuilder = new StringBuilder(currentLogLine);
        }

        if (continuationBuilder.length() >= MAX_LOG_LINE_ALLOWED) {
          handleOversizedContinuation(continuationBuilder, logPath, logText);

          // We are done with this line, add it to the list and clear currentLogLine to avoid
          // executing this same code over and over for invalid lines
          logLines.add(createLogEntry(continuationBuilder.toString(), logStream, logFile, currentLineNumber));
          continuationBuilder = null;
          currentLogLine = null;

          // This could simply be a malformed line, just continue parsing other lines
          continue;
        }
        continuationBuilder.append(StringUtils.LINE_SEPARATOR).append(line);
      }
    }

    // Make sure to add the last log line as well
    flushPendingLogEntry(logLines, continuationBuilder, currentLogLine, logStream, logFile, currentLineNumber);

    return logLines;
  }

  /**
   * Strips trailing null characters if present to avoid false payload length errors.
   *
   * @param line the raw line text extracted from the log.
   * @return the sanitized line without null characters.
   */
  private String sanitizeLine(String line) {
    if (!line.isEmpty() && line.charAt(line.length() - 1) == '\u0000') {
      return line.replace("\u0000", "");
    }
    return line;
  }

  /**
   * Flushes the active log line or continuation buffer into the collected log entries list.
   *
   * @param logLines the list collecting parsed [LogEntry] instances.
   * @param continuationBuilder the builder holding multiline continuation text, or null.
   * @param currentLogLine the last single log line text, or null.
   * @param logStream the [LogStream] associated with this log.
   * @param logFile the source [File] being parsed, or null.
   * @param lineNumber the line number in the source file.
   */
  private void flushPendingLogEntry(
      List<LogEntry> logLines,
      StringBuilder continuationBuilder,
      String currentLogLine,
      LogStream logStream,
      File logFile,
      int lineNumber) {
    if (continuationBuilder != null) {
      logLines.add(createLogEntry(continuationBuilder.toString(), logStream, logFile, lineNumber));
    } else if (currentLogLine != null) {
      logLines.add(createLogEntry(currentLogLine, logStream, logFile, lineNumber));
    }
  }

  /**
   * Inspects and logs payload overflow for oversized continuation lines, detecting potential bug reports.
   *
   * @param continuationBuilder the continuation buffer exceeding allowable payload length.
   * @param logPath the path of the log file being parsed.
   * @param logText the full raw text of the log file.
   */
  private void handleOversizedContinuation(
      StringBuilder continuationBuilder,
      String logPath,
      String logText) {
    continuationBuilder.delete(MAX_LOG_LINE_ALLOWED, continuationBuilder.length());

    // First check if we have already considered this as a potential bugreport. If so,
    // don't waste any more time here
    if (!potentialBugReports.containsKey(logPath)) {
      // This could be a bugreport. If this is the case, keep track of it
      if (isPotentialBugReport(logText)) {
        Logger.info("Found a potential bugreport: " + logPath);

        // Make sure to remove all '\r' so it does not get in the way of the parsers
        String bugReportText = logText.replace("\r", "");
        potentialBugReports.put(logPath, bugReportText);
      } else {
        String incorrectLinePreview = continuationBuilder.substring(0, 100) + "...";
        Logger.warning(
            "Incorrect format on following line (too long - " + continuationBuilder.length() + " bytes):\n" +
                "\"" + incorrectLinePreview + "\"\n\n" +
                "Maximum logcat line should be " + LOGGER_ENTRY_MAX_PAYLOAD + " bytes");
      }
    }
  }

  private LogEntry createLogEntry(String logLine, LogStream logStream, File sourceFile, int lineNumber) {
    return LogLineParser.createLogEntry(logLine, lineNumber, logStream, sourceFile);
  }

  LogLevel findLogLevel(String logLine) {
    return LogLineParser.findLogLevel(logLine);
  }

  LogTimestamp findTimestamp(String logLine) {
    return LogLineParser.findTimestamp(logLine);
  }

  boolean isLogLine(String line) {
    return LogLineParser.isLogLine(line);
  }

  private boolean shouldIgnoreLine(String line) {
    return LogLineParser.shouldIgnoreLine(line);
  }

  private boolean isPotentialBugReport(String logText) {
    return logText.contains("Bugreport format version:");
  }

  boolean isSorted(LogEntry[] entries) {
    for (int i = 1; i < entries.length; i++) {
      if (entries[i - 1].compareTo(entries[i]) > 0) {
        return false;
      }
    }
    return true;
  }

  boolean isBackwardTimeDiscontinuity(LogTimestamp prev, LogTimestamp curr) {
    if (prev.compareTo(curr) <= 0) {
      return false;
    }
    return prev.month != curr.month ||
           prev.day != curr.day ||
           prev.hour != curr.hour ||
           prev.minutes != curr.minutes ||
           prev.seconds != curr.seconds;
  }
}
