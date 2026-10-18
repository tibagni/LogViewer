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
   * Determines whether the given text starts with a standard Android log line prefix (MM-DD HH:MM:SS).
   *
   * @param line The raw text line to evaluate.
   * @return True if the line matches standard log format, false otherwise.
   */
  @JvmStatic
  fun isLogLine(line: String): Boolean {
    if (line.length < 14) return false
    return hasStandardLogPrefix(line) || LOG_START_PATTERN.matcher(line).lookingAt()
  }

  /**
   * Fast-path check verifying if characters 0..13 match the standard `MM-DD HH:MM:SS` date prefix.
   */
  private fun hasStandardLogPrefix(line: String): Boolean {
    return isDatePrefix(line, '\t')
  }

  /**
   * Validates the 14-character date and time structure (MM-DD HH:MM:SS) with configurable space separator.
   */
  private fun isDatePrefix(line: String, allowedWhitespace: Char): Boolean {
    return isTwoDigits(line, 0) &&
      line[2] == '-' &&
      isTwoDigits(line, 3) &&
      (line[5] == ' ' || line[5] == allowedWhitespace) &&
      isTwoDigits(line, 6) &&
      line[8] == ':' &&
      isTwoDigits(line, 9) &&
      line[11] == ':' &&
      isTwoDigits(line, 12)
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

  /**
   * Parses the timestamp component from a log line, attempting a high-performance fast path first
   * before falling back to regex matching.
   *
   * @param logLine The raw log line text.
   * @return A parsed [LogTimestamp], or null if parsing fails.
   */
  @JvmStatic
  fun findTimestamp(logLine: String): LogTimestamp? {
    parseFastTimestamp(logLine)?.let { return it }

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

  /**
   * High-performance non-regex timestamp parser for standard Android `MM-DD HH:MM:SS.mmm` lines.
   */
  private fun parseFastTimestamp(logLine: String): LogTimestamp? {
    if (!hasStandardTimestampPrefix(logLine)) return null

    val month = parseTwoDigitInt(logLine, 0)
    val day = parseTwoDigitInt(logLine, 3)
    val hour = parseTwoDigitInt(logLine, 6)
    val minutes = parseTwoDigitInt(logLine, 9)
    val seconds = parseTwoDigitInt(logLine, 12)

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
    return null
  }

  /**
   * Validates if the line begins with the standard timestamp structure up to 3 subsecond digits.
   */
  private fun hasStandardTimestampPrefix(line: String): Boolean {
    if (line.length < 18) return false
    val sep14 = line[14]
    return isDatePrefix(line, '\t') &&
      (sep14 == '.' || sep14 == ',') &&
      isThreeDigits(line, 15)
  }

  private fun isTwoDigits(s: String, startIndex: Int): Boolean {
    return s[startIndex] in '0'..'9' && s[startIndex + 1] in '0'..'9'
  }

  private fun isThreeDigits(s: String, startIndex: Int): Boolean {
    return s[startIndex] in '0'..'9' && s[startIndex + 1] in '0'..'9' && s[startIndex + 2] in '0'..'9'
  }

  private fun parseTwoDigitInt(s: String, startIndex: Int): Int {
    return (s[startIndex] - '0') * 10 + (s[startIndex + 1] - '0')
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

  private val LINE_INFO_PATTERNS = arrayOf(
    LINE_INFO_THREADTIME,
    LINE_INFO_PID_TID_SLASH,
    LINE_INFO_PID_TID_SPACE
  )

  private fun findLineInfoMatcher(logText: String): Matcher? {
    for (pattern in LINE_INFO_PATTERNS) {
      val m = pattern.matcher(logText)
      if (m.find()) return m
    }
    return null
  }
}
