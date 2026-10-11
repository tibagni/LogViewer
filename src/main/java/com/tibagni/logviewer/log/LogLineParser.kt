package com.tibagni.logviewer.log

import java.util.regex.Matcher
import java.util.regex.Pattern

object LogLineParser {
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
