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

  public LogParser(LogReader logReader, ProgressReporter progressReporter) {
    this.logReader = logReader;
    this.progressReporter = progressReporter;
    this.logEntries = new ArrayList<>();
    this.logsSkipped = new ArrayList<>();
    this.potentialBugReports = new HashMap<>();
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

  List<LogEntry> getLogEntries(String logText, String logPath, File logFile) {
    LogStream logStream = LogStream.inferLogStreamFromName(logPath);
    boolean isBugReport = registerBugReportIfNeeded(logText, logPath);
    EntryCollector collector = new EntryCollector(logStream, logFile, isBugReport);

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

      // In bugreports, section boundaries (e.g. '------ SYSTEM LOG ... ------') and log buffer
      // banners (shouldIgnoreLine) delimit independent log blocks and reset continuation state.
      if (isDelimiter(line, isBugReport)) {
        collector.flushAndReset();
      } else if (isLogLine(line, isBugReport)) {
        collector.startNewEntry(line, rawLineNumber);
      } else {
        collector.appendContinuation(line);
      }
    }

    return collector.finish();
  }

  /**
   * Registers a bugreport file upfront to enable indented service dumpsys parsing
   * without sacrificing strict column-0 matching for standard log files.
   */
  private boolean registerBugReportIfNeeded(String logText, String logPath) {
    boolean isBugReport = isPotentialBugReport(logText);
    if (isBugReport && !potentialBugReports.containsKey(logPath)) {
      Logger.info("Found a potential bugreport: " + logPath);
      potentialBugReports.put(logPath, logText.replace("\r", ""));
    }
    return isBugReport;
  }

  /**
   * Checks whether a line is a section delimiter or should-ignore buffer header.
   */
  private boolean isDelimiter(String line, boolean isBugReport) {
    return (isBugReport && isSectionBoundary(line)) || shouldIgnoreLine(line);
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
   * Encapsulates mutable log accumulation state (current entry, multiline continuations,
   * line numbers, and payload thresholds) during log file parsing.
   */
  private static class EntryCollector {
    private final List<LogEntry> entries = new ArrayList<>();
    private final LogStream logStream;
    private final File logFile;
    private final boolean isBugReport;

    private String currentLogLine = null;
    private int currentLineNumber = 0;
    private StringBuilder continuationBuilder = null;

    EntryCollector(
        LogStream logStream,
        File logFile,
        boolean isBugReport) {
      this.logStream = logStream;
      this.logFile = logFile;
      this.isBugReport = isBugReport;
    }

    void startNewEntry(String line, int lineNumber) {
      flush();
      continuationBuilder = null;
      currentLogLine = line;
      currentLineNumber = lineNumber;
    }

    void appendContinuation(String line) {
      if (currentLogLine == null) {
        return;
      }

      if (continuationBuilder == null) {
        continuationBuilder = new StringBuilder(currentLogLine);
      }

      if (continuationBuilder.length() >= MAX_LOG_LINE_ALLOWED) {
        handleOversizedContinuation();
        entries.add(createLogEntry(continuationBuilder.toString(), logStream, logFile, currentLineNumber));
        continuationBuilder = null;
        currentLogLine = null;
        return;
      }

      continuationBuilder.append(StringUtils.LINE_SEPARATOR).append(line);
    }

    void flushAndReset() {
      flush();
      continuationBuilder = null;
      currentLogLine = null;
    }

    List<LogEntry> finish() {
      flushAndReset();
      return entries;
    }

    private void flush() {
      if (continuationBuilder != null) {
        entries.add(createLogEntry(continuationBuilder.toString(), logStream, logFile, currentLineNumber));
      } else if (currentLogLine != null) {
        entries.add(createLogEntry(currentLogLine, logStream, logFile, currentLineNumber));
      }
    }

    private void handleOversizedContinuation() {
      continuationBuilder.delete(MAX_LOG_LINE_ALLOWED, continuationBuilder.length());

      if (!isBugReport) {
        String incorrectLinePreview = continuationBuilder.substring(0, 100) + "...";
        Logger.warning(
            "Incorrect format on following line (too long - " + continuationBuilder.length() + " bytes):\n" +
                "\"" + incorrectLinePreview + "\"\n\n" +
                "Maximum logcat line should be " + LOGGER_ENTRY_MAX_PAYLOAD + " bytes");
      }
    }
  }

  private static LogEntry createLogEntry(String logLine, LogStream logStream, File sourceFile, int lineNumber) {
    return LogLineParser.createLogEntry(logLine, lineNumber, logStream, sourceFile);
  }

  LogLevel findLogLevel(String logLine) {
    return LogLineParser.findLogLevel(logLine);
  }

  LogTimestamp findTimestamp(String logLine) {
    return LogLineParser.findTimestamp(logLine);
  }

  boolean isLogLine(String line) {
    return isLogLine(line, false);
  }

  boolean isLogLine(String line, boolean allowLeadingWhitespace) {
    return LogLineParser.isLogLine(line, allowLeadingWhitespace);
  }

  private boolean shouldIgnoreLine(String line) {
    return LogLineParser.shouldIgnoreLine(line);
  }

  /**
   * Recognizes Android bugreport dumpstate section boundaries, such as:
   * '------ SYSTEM LOG (logcat -v threadtime -d *:v) ------' or
   * '------ 0.120s was the duration of 'SYSTEM LOG' ------'.
   */
  private static boolean isSectionBoundary(String line) {
    return line.length() >= 14 && line.startsWith("------ ") && line.endsWith(" ------");
  }

  /**
   * Fast check to identify bugreport files by inspecting the format version banner
   * within the first 200KB of the file.
   */
  private static boolean isPotentialBugReport(String logText) {
    if (logText == null) {
      return false;
    }
    int limit = Math.min(logText.length(), 200_000);
    return logText.substring(0, limit).contains("Bugreport format version:");
  }

  boolean isSorted(LogEntry[] entries) {
    for (int i = 1; i < entries.length; i++) {
      if (entries[i - 1].compareTo(entries[i]) > 0) {
        return false;
      }
    }
    return true;
  }
}
