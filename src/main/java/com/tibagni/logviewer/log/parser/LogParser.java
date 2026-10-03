package com.tibagni.logviewer.log.parser;

import com.tibagni.logviewer.ProgressReporter;
import com.tibagni.logviewer.log.*;
import com.tibagni.logviewer.logger.Logger;
import com.tibagni.logviewer.util.StringUtils;
import org.jetbrains.annotations.NotNull;

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

  private static final Pattern LOG_LEVEL_PATTERN =
      Pattern.compile("^\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}.*?([VDIWE])");
  private static final Pattern LOG_START_PATTERN =
      Pattern.compile("^\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
  private static final Pattern LOG_TIMESTAMP_PATTERN =
      Pattern.compile("^(\\d{1,2})-(\\d{1,2})\\s(\\d{1,2}):(\\d{1,2}):(\\d{1,2}).(\\d{3,})");

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

  private List<LogEntry> getLogEntries(String logText, String logPath) {
    LogStream logStream = LogStream.inferLogStreamFromName(logPath);
    List<LogEntry> logLines = new ArrayList<>();

    String currentLogLine = null;
    StringBuilder continuationBuilder = null;

    int textLen = logText.length();
    int start = 0;
    while (start < textLen) {
      int end = logText.indexOf('\n', start);
      if (end < 0) {
        end = textLen;
      }
      int lineEnd = end;
      if (lineEnd > start && logText.charAt(lineEnd - 1) == '\r') {
        lineEnd--;
      }
      String line = logText.substring(start, lineEnd);
      start = end + 1;

      // Sometimes a line can contain a lot of NULL chars at the end, making it fail when trying to open the log
      // (as these NULL chars will make the line length too long). So check here if the line has NULL chars
      // and remove them to avoid failing to open valid log files
      if (!line.isEmpty() && line.charAt(line.length() - 1) == '\u0000') {
        line = line.replaceAll("\\u0000", "");
      }

      if (isLogLine(line)) {
        if (continuationBuilder != null) {
          logLines.add(createLogEntry(continuationBuilder.toString(), logStream));
          continuationBuilder = null;
        } else if (currentLogLine != null) {
          logLines.add(createLogEntry(currentLogLine, logStream));
        }

        currentLogLine = line;
      } else if (!shouldIgnoreLine(line) && currentLogLine != null) {
        // This is probably a continuation of a already started log line. Append to it
        if (continuationBuilder == null) {
          continuationBuilder = new StringBuilder(currentLogLine);
        }

        if (continuationBuilder.length() >= MAX_LOG_LINE_ALLOWED) {
          continuationBuilder.delete(MAX_LOG_LINE_ALLOWED, continuationBuilder.length());

          // First check if we have already considered this as a potential bugreport. If so,
          // don't waste any more time here
          if (!potentialBugReports.containsKey(logPath)) {
            String incorrectLinePreview = continuationBuilder.substring(0, 100) + "...";
            Logger.warning(
                "Incorrect format on following line (too long - " + continuationBuilder.length() + " bytes):\n" +
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
          logLines.add(createLogEntry(continuationBuilder.toString(), logStream));
          continuationBuilder = null;
          currentLogLine = null;

          // This could simply be a malformed line, just continue parsing other lines
          continue;
        }
        continuationBuilder.append(StringUtils.LINE_SEPARATOR).append(line);
      }
    }

    // Make sure to add the last log line as well
    if (continuationBuilder != null) {
      logLines.add(createLogEntry(continuationBuilder.toString(), logStream));
    } else if (currentLogLine != null) {
      logLines.add(createLogEntry(currentLogLine, logStream));
    }

    return logLines;
  }

  private LogEntry createLogEntry(String logLine, LogStream logStream) {
    return new LogEntry(logLine, findLogLevel(logLine), findTimestamp(logLine), logStream);
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
    // Fast path: standard Android log timestamp "MM-dd HH:mm:ss.SSS"
    if (logLine.length() >= 18 &&
        isDigit(logLine.charAt(0)) && isDigit(logLine.charAt(1)) && logLine.charAt(2) == '-' &&
        isDigit(logLine.charAt(3)) && isDigit(logLine.charAt(4)) &&
        (logLine.charAt(5) == ' ' || logLine.charAt(5) == '\t') &&
        isDigit(logLine.charAt(6)) && isDigit(logLine.charAt(7)) && logLine.charAt(8) == ':' &&
        isDigit(logLine.charAt(9)) && isDigit(logLine.charAt(10)) && logLine.charAt(11) == ':' &&
        isDigit(logLine.charAt(12)) && isDigit(logLine.charAt(13)) &&
        (logLine.charAt(14) == '.' || logLine.charAt(14) == ',') &&
        isDigit(logLine.charAt(15)) && isDigit(logLine.charAt(16)) && isDigit(logLine.charAt(17))) {

      int month = (logLine.charAt(0) - '0') * 10 + (logLine.charAt(1) - '0');
      int day = (logLine.charAt(3) - '0') * 10 + (logLine.charAt(4) - '0');
      int hour = (logLine.charAt(6) - '0') * 10 + (logLine.charAt(7) - '0');
      int minutes = (logLine.charAt(9) - '0') * 10 + (logLine.charAt(10) - '0');
      int seconds = (logLine.charAt(12) - '0') * 10 + (logLine.charAt(13) - '0');

      // Check subseconds digits (at least 3 digits, up to 9 digits to safely fit in 32-bit int)
      int subEnd = 18;
      while (subEnd < logLine.length() && isDigit(logLine.charAt(subEnd))) {
        subEnd++;
      }
      int subLen = subEnd - 15;
      if (subLen >= 3 && subLen <= 9) {
        int hundredth = 0;
        for (int i = 15; i < subEnd; i++) {
          hundredth = hundredth * 10 + (logLine.charAt(i) - '0');
        }
        return new LogTimestamp(month, day, hour, minutes, seconds, hundredth);
      }
    }

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

  boolean isLogLine(String line) {
    if (line.length() < 14) return false;
    // Fast path: direct char check (100% equivalent to ^\d{2}-\d{2} \d{2}:\d{2}:\d{2})
    if (isDigit(line.charAt(0)) &&
        isDigit(line.charAt(1)) &&
        line.charAt(2) == '-' &&
        isDigit(line.charAt(3)) &&
        isDigit(line.charAt(4)) &&
        line.charAt(5) == ' ' &&
        isDigit(line.charAt(6)) &&
        isDigit(line.charAt(7)) &&
        line.charAt(8) == ':' &&
        isDigit(line.charAt(9)) &&
        isDigit(line.charAt(10)) &&
        line.charAt(11) == ':' &&
        isDigit(line.charAt(12)) &&
        isDigit(line.charAt(13))) {
      return true;
    }

    // Safety fallback: if anything non-standard, use the precompiled pattern
    return LOG_START_PATTERN.matcher(line).lookingAt();
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private boolean shouldIgnoreLine(String line) {
    return line.startsWith("--------- beginning of");
  }

  private boolean isPotentialBugReport(String logText) {
    return logText.contains("Bugreport format version:");
  }
}
