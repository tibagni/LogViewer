package com.tibagni.logviewer.log.parser;

import com.tibagni.logviewer.ProgressReporter;
import com.tibagni.logviewer.log.*;
import com.tibagni.logviewer.logger.Logger;
import com.tibagni.logviewer.util.LargeFileReader;
import com.tibagni.logviewer.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
      Pattern.compile("^(\\d{1,2})-(\\d{1,2})\\s(\\d{1,2}):(\\d{1,2}):(\\d{1,2}).(\\d{3,})");
  // read max line to check if the reading file is a bugreport file
  private static final int MAX_LINE_TO_CHECK_POTENTIAL_BUGREPORT = 100;
  private static final boolean USE_MULTI_THREAD_TO_PARSE_LOG = true;

  private LogReader logReader;
  private List<LogEntry> logEntries;
  private ProgressReporter progressReporter;
  private final List<String> logsSkipped;
  private final Map<String, String> potentialBugReports;
  private final Lock logLock = new ReentrantLock();

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
    Set<String> availableLogs = logReader.getAvailableLogPaths();

    int logsRead = 0;
    for (String log : availableLogs) {
      try {
        int progress = logsRead++ * 90 / availableLogs.size();
        progressReporter.onProgress(progress, "Reading " + log + "...");
        File logFile = logReader.get(log);
        List<LogEntry> logEntriesFromFile = getLogEntries(logFile, log);

        if (!logEntriesFromFile.isEmpty()) {
          logEntries.addAll(logEntriesFromFile);
        } else {
          Logger.warning("Skipping " + log + " because it was empty");
          logsSkipped.add(log);
        }
      } catch (LogReaderException e) {
        throw new LogReaderException("Error reading: " + log, e);
      } catch (Exception e) {
        Logger.warning("Skipping " + log + " because it failed to parse", e);
        logsSkipped.add(log);
      }
    }

    if (!availableLogs.isEmpty()) {
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

  private List<LogEntry> getLogEntries(File logFile, String logPath) throws LogReaderException {
    // 300Mb file
    // single thread: ~13s
    // multi thread: ~2.5s
    // 1Gb file
    // single thread: ~50s
    // multi thread: ~9s
    if (!USE_MULTI_THREAD_TO_PARSE_LOG) {
      return getLogEntriesSingleThread(logFile, logPath);
    }
    return getEntriesMultiThread(logFile, logPath);
  }

  @NotNull
  private List<LogEntry> getEntriesMultiThread(File logFile, String logPath) throws LogReaderException {
    boolean maybeBugreport = checkPotentialBugReport(logFile, logPath);

    List<LogEntry> logLines = new ArrayList<>();
    Map<Integer, StringBuilder> bugreportLines = new HashMap<>();
    // todo: consider move to the coroutine way
    try {
      new LargeFileReader(logFile,
          null,
          2 * 1024 * 1024,
          Runtime.getRuntime().availableProcessors(),
          (sliceIndex, line) -> {
            LogEntry e = createLogEntryLocked(line, logPath);
            logLock.lock();
            try {
              if (e != null) {
                logLines.add(e);
              } else if (maybeBugreport) {
                // merge all non log line to bugreport content
                StringBuilder builder = bugreportLines.computeIfAbsent(sliceIndex, StringBuilder::new);
                builder.append(line).append(StringUtils.LINE_SEPARATOR);
              }
            } finally {
              logLock.unlock();
            }
          }).startAndWaitComplete();
    } catch (IOException e) {
      throw new LogReaderException("Error reading: " + logFile, e);
    }

    // merge bugreport slice content
    if (!bugreportLines.isEmpty()) {
      StringBuilder builder = new StringBuilder();
      bugreportLines.keySet()
          .stream()
          .sorted()
          .forEach(index -> builder
              .append(bugreportLines.get(index)));
      // Make sure to remove all '\r' so it does not get in the way of the parsers
      String bugReportText = builder.toString().replaceAll("\r", "");
      potentialBugReports.put(logPath, bugReportText);
    }
    return logLines;
  }

  private boolean checkPotentialBugReport(File logFile, String logPath) throws LogReaderException {
    // First check if we have already considered this as a potential bugreport. If so,
    // don't waste any more time here
    boolean potential = false;
    if (!potentialBugReports.containsKey(logPath)) {
      try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
        String line;
        int readLineNum = 0;
        while ((line = reader.readLine()) != null && readLineNum <= MAX_LINE_TO_CHECK_POTENTIAL_BUGREPORT) {
          // This could be a bugreport. If this is the case, keep track of it
          if (isPotentialBugReport(line)) {
            Logger.info("Found a potential bugreport: " + logPath);
            potential = true;
            break;
          }
          readLineNum++;
        }
      } catch (IOException e) {
        throw new LogReaderException("Error reading: " + logFile, e);
      }
    }
    return potential;
  }

  @Nullable
  private LogEntry createLogEntryLocked(String line, String logPath) {
    // Sometimes a line can contain a lot of NULL chars at the end, making it fail when trying to open the log
    // (as these NULL chars will make the line length too long). So check here if the line has NULL chars
    // and remove them to avoid failing to open valid log files
    if (!line.isEmpty() && line.charAt(line.length() - 1) == '\u0000') {
      line = line.replaceAll("\\u0000", "");
    }

    if (isLogLine(line)) {
      StringBuilder currentLogLine = new StringBuilder(line);
      // This is probably a continuation of a already started log line. Append to it
      if (currentLogLine.length() >= MAX_LOG_LINE_ALLOWED) {
        currentLogLine.delete(MAX_LOG_LINE_ALLOWED, currentLogLine.length());
        String incorrectLinePreview = currentLogLine.substring(0, 100) + "...";
        Logger.warning(
            "Incorrect format on following line (too long - " + currentLogLine.length() + " bytes):\n" +
                "\"" + incorrectLinePreview + "\"\n\n" +
                "Maximum logcat line should be " + LOGGER_ENTRY_MAX_PAYLOAD + " bytes");
      }
      return createLogEntry(currentLogLine.toString(), logPath);
    }
    return null;
  }

  private List<LogEntry> getLogEntriesSingleThread(File file, String logPath) throws LogReaderException {
    StringBuilder builder = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = reader.readLine()) != null) {
        builder.append(line);
        builder.append(StringUtils.LINE_SEPARATOR);
      }
    } catch (IOException e) {
      throw new LogReaderException("Error reading: " + file, e);
    }
    return getLogEntries2(builder.toString(), logPath);
  }

  private List<LogEntry> getLogEntries2(String logText, String logPath) {
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
          logLines.add(createLogEntry(currentLogLine.toString(), logPath));
        }

        currentLogLine = new StringBuilder(line);
      } else if (!shouldIgnoreLine(line) && currentLogLine != null) {
        // This is probably a continuation of a already started log line. Append to it
        if (currentLogLine.length() >= MAX_LOG_LINE_ALLOWED) {
          currentLogLine.delete(MAX_LOG_LINE_ALLOWED, currentLogLine.length());

          // First check if we have already considered this as a potential bugreport. If so,
          // don't waste any more time here
          if (!potentialBugReports.containsKey(logPath)) {
            String incorrectLinePreview = currentLogLine.substring(0, 100) + "...";
            Logger.warning(
                "Incorrect format on following line (too long - " + currentLogLine.length() + " bytes):\n" +
                    "\"" + incorrectLinePreview + "\"\n\n" +
                    "Maximum logcat line should be " + LOGGER_ENTRY_MAX_PAYLOAD + " bytes");

            // This could be a bugreport. If this is the case, keep track of it
            if (isPotentialBugReport(logText)) {
              Logger.info("Found a potential bugreport: " + logPath);

              // Make sure to remove all '\r' so it does not get in the way of the parsers
              String bugReportText = logText.replaceAll("\r", "");
              potentialBugReports.put(logPath, bugReportText);
            }
          }

          // We are done with this line, add it to the list and clear currentLogLine to avoid
          // executing this same code over and over for invalid lines
          logLines.add(createLogEntry(currentLogLine.toString(), logPath));
          currentLogLine = null;

          // This could simply be a malformed line, just continue parsing other lines
          continue;
        }
        currentLogLine.append(StringUtils.LINE_SEPARATOR).append(line);
      }
    }

    // Make sure to add the last log line as well
    if (currentLogLine != null) {
      logLines.add(createLogEntry(currentLogLine.toString(), logPath));
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
    return line.startsWith("--------- beginning of") || line.startsWith("=aplogcat=");
  }

  private boolean isPotentialBugReport(String logText) {
    return logText.contains("Bugreport format version:");
  }
}
