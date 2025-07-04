package com.tibagni.logviewer

import com.tibagni.logviewer.log.*
import com.tibagni.logviewer.log.parser.LogParser
import com.tibagni.logviewer.logger.wrapProfiler
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

class OpenLogsException(message: String?, cause: Throwable) : java.lang.Exception(message, cause)

interface LogsRepository {
  val currentlyOpenedLogFiles: List<File>
  val currentlyOpenedLogs: List<LogEntry>
  val availableStreams: Set<LogStream>
  val lastSkippedLogFiles: List<String>
  val potentialBugReports: Map<String, String>

  var firstVisibleLogIndex: Int
  var lastVisibleLogIndex: Int

  /**
   * Indicates the size of all logs opened, including any ignored log (not only the visible logs)
   */
  val allLogsSize: Int

  @Throws(OpenLogsException::class)
  fun openLogFiles(files: Array<File>, charset: Charset, progressReporter: ProgressReporter)
  fun getMatchingLogEntry(entry: LogEntry): LogEntry?
}

class LogsRepositoryImpl : LogsRepository {
  private val _currentlyOpenedLogFiles = mutableListOf<File>()
  override val currentlyOpenedLogFiles: List<File>
    get() = _currentlyOpenedLogFiles

  private val _currentlyOpenedLogs = mutableListOf<LogEntry>()
  override val currentlyOpenedLogs: List<LogEntry>
    get() = _currentlyOpenedLogs.subList(_firstVisibleLogIndex, _lastVisibleLogIndex + 1)

  private val _availableStreams = hashSetOf<LogStream>()
  override val availableStreams: Set<LogStream>
    get() = _availableStreams

  private val _lastSkippedLogFiles = mutableListOf<String>()
  override val lastSkippedLogFiles: List<String>
    get() = _lastSkippedLogFiles

  private val _potentialBugReports = mutableMapOf<String, String>()
  override val potentialBugReports: Map<String, String>
    get() = _potentialBugReports

  private var _firstVisibleLogIndex = 0
  override var firstVisibleLogIndex: Int
    get() = _firstVisibleLogIndex
    set(value) {
      _firstVisibleLogIndex = if (value == -1) 0 else value
    }

  private var _lastVisibleLogIndex = -1
  override var lastVisibleLogIndex: Int
    get() = _lastVisibleLogIndex
    set(value) {
      _lastVisibleLogIndex = if (value == -1) _currentlyOpenedLogs.lastIndex else value
    }
  override val allLogsSize: Int
    get() = _currentlyOpenedLogs.size


  @Throws(OpenLogsException::class)
  override fun openLogFiles(files: Array<File>, charset: Charset, progressReporter: ProgressReporter) {
    try {
      val logParser = LogParser(FileLogReader(files), progressReporter)
      val parsedLogs = wrapProfiler("ParseLogs") { logParser.parseLogs(charset) }

      _firstVisibleLogIndex = 0
      _lastVisibleLogIndex = parsedLogs.lastIndex

      _currentlyOpenedLogs.reset(parsedLogs)
      _availableStreams.reset(logParser.availableStreams)
      _lastSkippedLogFiles.reset(logParser.logsSkipped)
      _potentialBugReports.reset(logParser.potentialBugReports)

      if (parsedLogs.isNotEmpty()) {
        _currentlyOpenedLogFiles.reset(files)
      } else {
        _currentlyOpenedLogFiles.clear()
      }

      logParser.release()
    } catch (e: Exception) {
      // End the progress if we failed to read the file
      progressReporter.failProgress()
      when (e) {
        is LogReaderException -> {
          throw OpenLogsException(e.message, e)
        }
        else -> throw e
      }
    }
  }

  override fun getMatchingLogEntry(entry: LogEntry): LogEntry? {
    // Here we want to check if the given log entry exists anywhere in the list, not necessarily in the same index,
    // And we also want to make sure the text is the same. So, use a different comparator here that only considers
    // the timestamp for comparison and also checks if the log text is the same
    val cmp = Comparator.comparing { o: LogEntry -> o.timestamp }

    val indexFound = Collections.binarySearch(currentlyOpenedLogs, entry, cmp)
    if (indexFound >= 0) {
      // We found one index for a possible entry. But there might be multiple log entries for the same timestamp
      // so, iterate until we find the exact line we are looking for.
      // First we want to find the first log in this timestamp
      var i = indexFound
      while (i >= 0 && currentlyOpenedLogs[i].timestamp == entry.timestamp) {
        i--
      }

      // Now that we are in the beginning of the timestamp, look for the entry
      while (currentlyOpenedLogs[i].logText != entry.logText &&
        currentlyOpenedLogs[i].timestamp <= entry.timestamp) {
        i++
      }

      // We either found or finished search. check which one
      if (currentlyOpenedLogs[i].logText == entry.logText) {
        return currentlyOpenedLogs[i]
      }
    }

    // Not found
    return null
  }
}