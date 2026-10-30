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
   * Checks if characters starting at [startIndex] match the date/time structure
   * (MM-DD HH:MM:SS or YYYY-MM-DD HH:MM:SS).
   */
  private fun hasDatePrefixAt(line: String, startIndex: Int): Boolean {
    val cur = skipOptionalYear(line, startIndex)
    if (line.length - cur < 14) return false
    if (!isMonthDay(line, cur)) return false

    val timeStart = skipWhitespace(line, cur + 5)
    if (timeStart == cur + 5 || line.length - timeStart < 8) return false

    return isTimeFormat(line, timeStart)
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
   * Result of parsing the date prefix: month, day, and the index where time begins.
   *
   * @property month Parsed month (1-12).
   * @property day Parsed day (1-31).
   * @property nextIndex Character index in the raw line where time components begin.
   */
  internal data class DatePrefixResult(val month: Int, val day: Int, val nextIndex: Int)

  /**
   * Result of parsing time components: hour, minute, second, and subsecond separator index.
   *
   * @property hour Parsed hour (0-23).
   * @property minutes Parsed minutes (0-59).
   * @property seconds Parsed seconds (0-59).
   * @property subsecondIndex Character index of the subsecond separator ('.', ',', ':').
   */
  internal data class TimeResult(
    val hour: Int,
    val minutes: Int,
    val seconds: Int,
    val subsecondIndex: Int
  )

  /**
   * High-performance non-regex timestamp parser for Android timestamped lines.
   *
   * @param logLine The raw log line text.
   * @return A parsed [LogTimestamp], or null if the line does not match.
   */
  internal fun parseFastTimestamp(logLine: String): LogTimestamp? {
    val offset = skipWhitespace(logLine, 0)
    val date = parseDatePrefix(logLine, offset) ?: return null
    val time = parseTime(logLine, date.nextIndex) ?: return null
    val subseconds = parseSubseconds(logLine, time.subsecondIndex) ?: return null

    return LogTimestamp(date.month, date.day, time.hour, time.minutes, time.seconds, subseconds)
  }

  /**
   * Parses the date prefix (optional year and MM-DD followed by whitespace).
   *
   * @param logLine The raw log line text.
   * @param startIndex The index to start parsing from (after leading whitespace).
   * @return A [DatePrefixResult] with month, day, and index after date whitespace, or null.
   */
  internal fun parseDatePrefix(logLine: String, startIndex: Int): DatePrefixResult? {
    val cur = skipOptionalYear(logLine, startIndex)
    if (logLine.length - cur < 18) return null
    if (!isMonthDay(logLine, cur)) return null

    val month = parseTwoDigitInt(logLine, cur)
    val day = parseTwoDigitInt(logLine, cur + 3)

    val nextIndex = skipWhitespace(logLine, cur + 5)
    if (nextIndex == cur + 5) return null

    return DatePrefixResult(month, day, nextIndex)
  }

  /**
   * Parses time components (HH:MM:SS) starting at [startIndex].
   *
   * @param logLine The raw log line text.
   * @param startIndex The index where time components begin.
   * @return A [TimeResult] containing hour, minute, second, and subsecond index, or null.
   */
  internal fun parseTime(logLine: String, startIndex: Int): TimeResult? {
    if (logLine.length - startIndex < 12) return null
    if (!isTimeFormat(logLine, startIndex)) return null

    val hour = parseTwoDigitInt(logLine, startIndex)
    val minutes = parseTwoDigitInt(logLine, startIndex + 3)
    val seconds = parseTwoDigitInt(logLine, startIndex + 6)
    return TimeResult(hour, minutes, seconds, startIndex + 8)
  }

  /**
   * Parses subsecond digits (between 3 and 9 digits) following a separator at [startIndex].
   *
   * @param logLine The raw log line text.
   * @param startIndex The index of the subsecond separator ('.', ',', or ':').
   * @return The parsed subsecond integer value, or null if malformed.
   */
  internal fun parseSubseconds(logLine: String, startIndex: Int): Int? {
    if (startIndex < 0 || startIndex >= logLine.length) return null
    if (!hasValidSubsecondPrefix(logLine, startIndex)) return null

    val digitsStart = startIndex + 1
    val subEnd = findDigitsEnd(logLine, digitsStart + 3)
    val subLen = subEnd - digitsStart
    if (subLen > 9) return null

    return parseDigitsToInt(logLine, digitsStart, subEnd)
  }

  /**
   * Advances the index through consecutive spaces or tabs.
   *
   * @param s The string to scan.
   * @param startIndex The index to start scanning from.
   * @return The index of the first non-whitespace character, or the string length.
   */
  private fun skipWhitespace(s: String, startIndex: Int): Int {
    var i = startIndex
    val len = s.length
    while (i < len && (s[i] == ' ' || s[i] == '\t')) {
      i++
    }
    return i
  }

  /**
   * Skips an optional 4-digit year prefix (YYYY-) if present.
   *
   * @param s The string to inspect.
   * @param offset The starting index.
   * @return The new offset after the year prefix, or the original offset if not present.
   */
  private fun skipOptionalYear(s: String, offset: Int): Int {
    if (offset + 5 <= s.length && isFourDigits(s, offset) && s[offset + 4] == '-') {
      return offset + 5
    }
    return offset
  }

  /**
   * Checks whether the characters at [offset] form a valid MM-DD pattern.
   *
   * @param s The string to inspect.
   * @param offset The starting index.
   * @return True if [offset] begins with two digits, a hyphen, and two digits.
   */
  private fun isMonthDay(s: String, offset: Int): Boolean {
    return isTwoDigits(s, offset) && s[offset + 2] == '-' && isTwoDigits(s, offset + 3)
  }

  /**
   * Checks whether the characters at [startIndex] match the HH:MM:SS format.
   *
   * @param s The string to inspect.
   * @param startIndex The index where HH:MM:SS should start.
   * @return True if characters form valid HH:MM:SS.
   */
  private fun isTimeFormat(s: String, startIndex: Int): Boolean {
    return isTwoDigits(s, startIndex) &&
      s[startIndex + 2] == ':' &&
      isTwoDigits(s, startIndex + 3) &&
      s[startIndex + 5] == ':' &&
      isTwoDigits(s, startIndex + 6)
  }

  /**
   * Validates that [startIndex] points to a valid separator followed by at least three digits.
   *
   * @param s The string to inspect.
   * @param startIndex The index of the separator.
   * @return True if separator is '.', ',', or ':' and next three characters are digits.
   */
  private fun hasValidSubsecondPrefix(s: String, startIndex: Int): Boolean {
    val sep = s[startIndex]
    val isValidSep = sep == '.' || sep == ',' || sep == ':'
    return isValidSep && isThreeDigits(s, startIndex + 1)
  }

  /**
   * Finds the index of the first non-digit character starting at [startIndex].
   *
   * @param s The string to inspect.
   * @param startIndex The index to start scanning from.
   * @return The index of the first non-digit or string length if all remaining are digits.
   */
  private fun findDigitsEnd(s: String, startIndex: Int): Int {
    var end = startIndex
    val len = s.length
    while (end < len && s[end] in '0'..'9') {
      end++
    }
    return end
  }

  /**
   * Parses characters in the range [start, end) into an integer.
   *
   * @param s The string containing digits.
   * @param start The inclusive start index.
   * @param end The exclusive end index.
   * @return The parsed integer value.
   */
  private fun parseDigitsToInt(s: String, start: Int, end: Int): Int {
    var value = 0
    for (idx in start until end) {
      value = value * 10 + (s[idx] - '0')
    }
    return value
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
