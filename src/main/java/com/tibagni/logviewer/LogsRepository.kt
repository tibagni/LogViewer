package com.tibagni.logviewer

import com.tibagni.logviewer.log.FileLogReader
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogReaderException
import com.tibagni.logviewer.log.LogStream
import com.tibagni.logviewer.log.parser.LogParser
import com.tibagni.logviewer.logger.wrapProfiler
import java.io.File

class OpenLogsException(message: String?, cause: Throwable) : java.lang.Exception(message, cause)

interface LogsRepository {
  val currentlyOpenedLogFiles: List<File>
  val currentlyOpenedLogs: List<LogEntry>
  val availableStreams: Set<LogStream>
  val lastSkippedLogFiles: List<String>
  val potentialBugReports: Map<String, String>

  @Throws(OpenLogsException::class)
  fun openLogFiles(files: Array<File>, progressReporter: ProgressReporter)
}

class LogsRepositoryImpl : LogsRepository {
  private val _currentlyOpenedLogFiles = mutableListOf<File>()
  override val currentlyOpenedLogFiles: List<File>
    get() = _currentlyOpenedLogFiles

  private val _currentlyOpenedLogs = mutableListOf<LogEntry>()
  override val currentlyOpenedLogs: List<LogEntry>
    get() = _currentlyOpenedLogs

  private val _availableStreams = hashSetOf<LogStream>()
  override val availableStreams: Set<LogStream>
    get() = _availableStreams

  private val _lastSkippedLogFiles = mutableListOf<String>()
  override val lastSkippedLogFiles: List<String>
    get() = _lastSkippedLogFiles

  private val _potentialBugReports = mutableMapOf<String, String>()
  override val potentialBugReports: Map<String, String>
    get() = _potentialBugReports


  @Throws(OpenLogsException::class)
  override fun openLogFiles(files: Array<File>, progressReporter: ProgressReporter) {
    try {
      val logParser = LogParser(FileLogReader(files), progressReporter)
      val parsedLogs = wrapProfiler("ParseLogs") { logParser.parseLogs() }

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
      when (e) {
        is LogReaderException -> {
          throw OpenLogsException(e.message, e)
        }
        else -> throw e
      }
    }
  }
}