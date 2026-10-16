package com.tibagni.logviewer.log

import com.tibagni.logviewer.logger.Logger
import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern

object LogLineParser {
  private val LOG_LEVEL_PATTERN =
    Pattern.compile("^\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}.*?([VDIWE])")
  private val LOG_START_PATTERN =
    Pattern.compile("^\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
  private val LOG_TIMESTAMP_PATTERN =
    Pattern.compile("^(\\d{1,2})-(\\d{1,2})\\s(\\d{1,2}):(\\d{1,2}):(\\d{1,2}).(\\d{3,})")

  private val LOG_PID_PATTERN =
    Pattern.compile("^\\s*(?:\\d{4}-)?\\d{1,2}-\\d{1,2}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2}[\\.,]\\d+\\s+(?:(?:\\d+\\s+)?(\\d+)\\s+\\d+|(\\d+)-\\d+(?:/\\S*)?)\\s+([VDIWEF])(?:[\\s/:]|$)")

  private val LINE_INFO_THREADTIME =
    Pattern.compile("^\\s*(?:\\d{4}-)?\\d{1,2}-\\d{1,2}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2}[\\.,]\\d+\\s+(?:(?:\\d+\\s+)?(\\d+)\\s+(\\d+))\\s+([VDIWEF])\\s+([^:]*?)\\s*:\\s*(.*)$", Pattern.DOTALL)
  private val LINE_INFO_PID_TID_SLASH =
    Pattern.compile("^\\s*(?:\\d{4}-)?\\d{1,2}-\\d{1,2}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2}[\\.,]\\d+\\s+(\\d+)-(\\d+)(?:/\\S*)?\\s+([VDIWEF])/([^:]*?)\\s*:\\s*(.*)$", Pattern.DOTALL)
  private val LINE_INFO_PID_TID_SPACE =
    Pattern.compile("^\\s*(?:\\d{4}-)?\\d{1,2}-\\d{1,2}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2}[\\.,]\\d+\\s+(\\d+)-(\\d+)(?:/\\S*)?\\s+([VDIWEF])\\s+([^:]*?)\\s*:\\s*(.*)$", Pattern.DOTALL)

  /**
   * Finds the process ID (PID) from a log line string, or returns -1 if not found.
   *
   * @param logLine the raw log line text.
   * @return the integer process ID, or -1 if parsing fails or line is invalid.
   */
  @JvmStatic
  fun isLogLine(line: String): Boolean {
    if (line.length < 14) return false
    if (line[0] in '0'..'9' &&
      line[1] in '0'..'9' &&
      line[2] == '-' &&
      line[3] in '0'..'9' &&
      line[4] in '0'..'9' &&
      line[5] == ' ' &&
      line[6] in '0'..'9' &&
      line[7] in '0'..'9' &&
      line[8] == ':' &&
      line[9] in '0'..'9' &&
      line[10] in '0'..'9' &&
      line[11] == ':' &&
      line[12] in '0'..'9' &&
      line[13] in '0'..'9'
    ) {
      return true
    }
    return LOG_START_PATTERN.matcher(line).lookingAt()
  }

  @JvmStatic
  fun findLogLevel(logLine: String): LogLevel {
    val matcher = LOG_LEVEL_PATTERN.matcher(logLine)
    return if (matcher.find()) {
      LogLevel.createFromStringLevel(matcher.group(1))
    } else {
      LogLevel.DEBUG
    }
  }

  @JvmStatic
  fun findTimestamp(logLine: String): LogTimestamp? {
    if (logLine.length >= 18 &&
      logLine[0] in '0'..'9' && logLine[1] in '0'..'9' && logLine[2] == '-' &&
      logLine[3] in '0'..'9' && logLine[4] in '0'..'9' &&
      (logLine[5] == ' ' || logLine[5] == '\t') &&
      logLine[6] in '0'..'9' && logLine[7] in '0'..'9' && logLine[8] == ':' &&
      logLine[9] in '0'..'9' && logLine[10] in '0'..'9' && logLine[11] == ':' &&
      logLine[12] in '0'..'9' && logLine[13] in '0'..'9' &&
      (logLine[14] == '.' || logLine[14] == ',') &&
      logLine[15] in '0'..'9' && logLine[16] in '0'..'9' && logLine[17] in '0'..'9'
    ) {
      val month = (logLine[0] - '0') * 10 + (logLine[1] - '0')
      val day = (logLine[3] - '0') * 10 + (logLine[4] - '0')
      val hour = (logLine[6] - '0') * 10 + (logLine[7] - '0')
      val minutes = (logLine[9] - '0') * 10 + (logLine[10] - '0')
      val seconds = (logLine[12] - '0') * 10 + (logLine[13] - '0')

      var subEnd = 18
      while (subEnd < logLine.length && logLine[subEnd] in '0'..'9') {
        subEnd++
      }
      val subLen = subEnd - 15
      if (subLen in 3..9) {
        var hundredth = 0
        for (i in 15 until subEnd) {
          hundredth = hundredth * 10 + (logLine[i] - '0')
        }
        return LogTimestamp(month, day, hour, minutes, seconds, hundredth)
      }
    }

    try {
      val matcher = LOG_TIMESTAMP_PATTERN.matcher(logLine)
      if (matcher.find()) {
        return LogTimestamp(
          matcher.group(1),
          matcher.group(2),
          matcher.group(3),
          matcher.group(4),
          matcher.group(5),
          matcher.group(6)
        )
      }
    } catch (e: Exception) {
      Logger.error("Failed to parse timestamp for: $logLine", e)
    }

    return null
  }

  @JvmStatic
  fun shouldIgnoreLine(line: String): Boolean {
    return line.startsWith("--------- beginning of")
  }

  @JvmStatic
  fun extractLogStreamFromBeginningOfLine(line: String): LogStream? {
    if (!line.startsWith("--------- beginning of")) return null
    val target = line.removePrefix("--------- beginning of").trim().substringAfterLast('/').trimEnd(':')
    val stream = LogStream.fromBuffer(target)
    return if (stream != LogStream.UNKNOWN) stream else null
  }

  @JvmStatic
  @JvmOverloads
  fun createLogEntry(
    logLine: String,
    lineNumber: Int,
    logStream: LogStream = LogStream.UNKNOWN,
    sourceFile: File? = null
  ): LogEntry {
    return LogEntry(logLine, findLogLevel(logLine), findTimestamp(logLine), logStream, sourceFile, lineNumber)
  }

  @JvmStatic
  fun findPid(logLine: String?): Int {
    if (logLine.isNullOrEmpty()) return -1
    val m = LOG_PID_PATTERN.matcher(logLine)
    if (!m.find()) return -1

    val pidGroup = m.group(1) ?: m.group(2) ?: return -1
    return pidGroup.toIntOrNull() ?: -1
  }

  @JvmStatic
  fun getFilterPatternForPid(pid: Int): String {
    return "^\\s*\\S+\\s+\\S+\\s+(?:(?:\\d+\\s+)?$pid\\s+\\d+\\s+|$pid-)"
  }

  @JvmStatic
  fun getFilterPatternForTag(tag: String): String {
    return "^\\s*\\S+\\s+\\S+[^:]*?\\s[VDIWEF](?:/" + Pattern.quote(tag) + "|\\s+" + Pattern.quote(tag) + ")(?:\\s*\\([^)]*\\))?\\s*:"
  }

  /**
   * Parses complete information from a LogEntry object into a structured LogLineInfo.
   *
   * @param entry the raw LogEntry containing the text and metadata.
   * @return the structured LogLineInfo, or null if the input entry is null.
   */
  @JvmStatic
  fun parseLineInfo(entry: LogEntry?): LogLineInfo? {
    if (entry == null) return null

    val logText = entry.logText
    var pid: Int? = null
    var tid: Int? = null
    var tag: String? = null
    var message = logText ?: ""

    if (logText != null) {
      val m = findLineInfoMatcher(logText)
      if (m != null) {
        pid = m.group(1).toIntOrNull()
        tid = m.group(2).toIntOrNull()
        tag = m.group(4)?.trim()
        message = m.group(5) ?: ""
      } else {
        val foundPid = findPid(logText)
        if (foundPid > 0) pid = foundPid
      }
    }

    return LogLineInfo(
      entry = entry,
      sourceFile = entry.sourceFile,
      lineNumber = entry.lineNumber,
      logStream = entry.logStream,
      logLevel = entry.logLevel,
      timestamp = entry.timestamp,
      pid = pid,
      tid = tid,
      tag = tag,
      message = message,
      appliedFilter = entry.appliedFilter,
      index = entry.index
    )
  }

  private fun findLineInfoMatcher(logText: String): Matcher? {
    var m = LINE_INFO_THREADTIME.matcher(logText)
    if (m.find()) return m

    m = LINE_INFO_PID_TID_SLASH.matcher(logText)
    if (m.find()) return m

    m = LINE_INFO_PID_TID_SPACE.matcher(logText)
    if (m.find()) return m

    return null
  }
}
