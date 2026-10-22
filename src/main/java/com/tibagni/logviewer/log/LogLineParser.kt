package com.tibagni.logviewer.log

import com.tibagni.logviewer.logger.Logger
import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern

object LogLineParser {
  private val LOG_LEVEL_PATTERN =
    Pattern.compile("^\\s*(?:\\d{4}-)?\\d{1,2}-\\d{1,2}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2}(?:[\\.,:]\\d+)?(?:\\s+(?:\\d+|\\d+-\\d+(?:/\\S*)?))*\\s+([VDIWEF])(?:[\\s/:]|$)")
  private val LOG_TIMESTAMP_PATTERN =
    Pattern.compile("^\\s*(?:\\d{4}-)?(\\d{1,2})-(\\d{1,2})[ \t]+(\\d{1,2}):(\\d{1,2}):(\\d{1,2})(?:[\\.,:](\\d{3,})|(?=\\s|$))")

  private val LOG_PID_PATTERN =
    Pattern.compile("^\\s*(?:\\d{4}-)?\\d{1,2}-\\d{1,2}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2}[\\.,:]\\d+\\s+(?:(?:\\d+\\s+)?(\\d+)\\s+\\d+|(\\d+)-\\d+(?:/\\S*)?)\\s+([VDIWEF])(?:[\\s/:]|$)")

  private val LINE_INFO_THREADTIME =
    Pattern.compile("^\\s*(?:\\d{4}-)?\\d{1,2}-\\d{1,2}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2}[\\.,:]\\d+\\s+(?:(?:\\d+\\s+)?(\\d+)\\s+(\\d+))\\s+([VDIWEF])\\s+([^:]*?)\\s*:\\s*(.*)$", Pattern.DOTALL)
  private val LINE_INFO_PID_TID_SLASH =
    Pattern.compile("^\\s*(?:\\d{4}-)?\\d{1,2}-\\d{1,2}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2}[\\.,:]\\d+\\s+(\\d+)-(\\d+)(?:/\\S*)?\\s+([VDIWEF])/([^:]*?)\\s*:\\s*(.*)$", Pattern.DOTALL)
  private val LINE_INFO_PID_TID_SPACE =
    Pattern.compile("^\\s*(?:\\d{4}-)?\\d{1,2}-\\d{1,2}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2}[\\.,:]\\d+\\s+(\\d+)-(\\d+)(?:/\\S*)?\\s+([VDIWEF])\\s+([^:]*?)\\s*:\\s*(.*)$", Pattern.DOTALL)

  /**
   * Determines whether the given text starts with an Android log line prefix (MM-DD HH:MM:SS or YYYY-MM-DD HH:MM:SS).
   *
   * @param line The raw text line to evaluate.
   * @param allowLeadingWhitespace When true, allows leading whitespace before timestamp (used in bugreports).
   * @return True if the line matches log format, false otherwise.
   */
  @JvmStatic
  @JvmOverloads
  fun isLogLine(line: String, allowLeadingWhitespace: Boolean = false): Boolean {
    var offset = 0
    if (allowLeadingWhitespace) {
      val len = line.length
      while (offset < len && (line[offset] == ' ' || line[offset] == '\t')) {
        offset++
      }
    }
    if (line.length - offset < 14) return false
    return hasDatePrefixAt(line, offset)
  }

  /**
   * Checks if characters starting at [startIndex] match the date/time structure (MM-DD HH:MM:SS or YYYY-MM-DD HH:MM:SS).
   */
  private fun hasDatePrefixAt(line: String, startIndex: Int): Boolean {
    var i = startIndex
    val len = line.length
    if (len - i < 14) return false

    // Optional 4-digit year: YYYY-
    if (i + 5 <= len && isFourDigits(line, i) && line[i + 4] == '-') {
      i += 5
    }

    if (len - i < 14) return false

    // MM-DD
    if (!isTwoDigits(line, i) || line[i + 2] != '-' || !isTwoDigits(line, i + 3)) {
      return false
    }
    i += 5

    // Separator between date and time: 1 or more spaces or tabs
    if (i >= len || (line[i] != ' ' && line[i] != '\t')) return false
    while (i < len && (line[i] == ' ' || line[i] == '\t')) {
      i++
    }

    // HH:MM:SS
    if (len - i < 8) return false
    return isTwoDigits(line, i) &&
      line[i + 2] == ':' &&
      isTwoDigits(line, i + 3) &&
      line[i + 5] == ':' &&
      isTwoDigits(line, i + 6)
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
        val hundredthStr = matcher.group(6) ?: "0"
        return LogTimestamp(
          matcher.group(1),
          matcher.group(2),
          matcher.group(3),
          matcher.group(4),
          matcher.group(5),
          hundredthStr
        )
      }
    } catch (e: Exception) {
      Logger.error("Failed to parse timestamp for: $logLine", e)
    }

    return null
  }

  /**
   * High-performance non-regex timestamp parser for Android timestamped lines.
   */
  private fun parseFastTimestamp(logLine: String): LogTimestamp? {
    var offset = 0
    val len = logLine.length
    while (offset < len && (logLine[offset] == ' ' || logLine[offset] == '\t')) {
      offset++
    }
    if (len - offset < 18) return null

    // Optional 4-digit year: YYYY-
    if (offset + 5 <= len && isFourDigits(logLine, offset) && logLine[offset + 4] == '-') {
      offset += 5
    }

    if (len - offset < 18) return null

    // MM-DD
    if (!isTwoDigits(logLine, offset) || logLine[offset + 2] != '-' || !isTwoDigits(logLine, offset + 3)) {
      return null
    }

    val month = parseTwoDigitInt(logLine, offset)
    val day = parseTwoDigitInt(logLine, offset + 3)

    var i = offset + 5
    if (i >= len || (logLine[i] != ' ' && logLine[i] != '\t')) return null
    while (i < len && (logLine[i] == ' ' || logLine[i] == '\t')) {
      i++
    }

    if (len - i < 12) return null // Need at least HH:MM:SS.mmm (8 + 1 + 3)

    if (!isTwoDigits(logLine, i) || logLine[i + 2] != ':' || !isTwoDigits(logLine, i + 3) ||
        logLine[i + 5] != ':' || !isTwoDigits(logLine, i + 6)) {
      return null
    }

    val hour = parseTwoDigitInt(logLine, i)
    val minutes = parseTwoDigitInt(logLine, i + 3)
    val seconds = parseTwoDigitInt(logLine, i + 6)

    val sep = logLine[i + 8]
    if (sep != '.' && sep != ',' && sep != ':') return null
    if (!isThreeDigits(logLine, i + 9)) return null

    var subEnd = i + 12
    while (subEnd < len && logLine[subEnd] in '0'..'9') {
      subEnd++
    }
    val subLen = subEnd - (i + 9)
    if (subLen in 3..9) {
      var hundredth = 0
      for (idx in (i + 9) until subEnd) {
        hundredth = hundredth * 10 + (logLine[idx] - '0')
      }
      return LogTimestamp(month, day, hour, minutes, seconds, hundredth)
    }
    return null
  }

  private fun isTwoDigits(s: String, startIndex: Int): Boolean {
    return startIndex + 1 < s.length && s[startIndex] in '0'..'9' && s[startIndex + 1] in '0'..'9'
  }

  private fun isThreeDigits(s: String, startIndex: Int): Boolean {
    return startIndex + 2 < s.length &&
      s[startIndex] in '0'..'9' && s[startIndex + 1] in '0'..'9' && s[startIndex + 2] in '0'..'9'
  }

  private fun isFourDigits(s: String, startIndex: Int): Boolean {
    return startIndex + 3 < s.length &&
      s[startIndex] in '0'..'9' &&
      s[startIndex + 1] in '0'..'9' &&
      s[startIndex + 2] in '0'..'9' &&
      s[startIndex + 3] in '0'..'9'
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
