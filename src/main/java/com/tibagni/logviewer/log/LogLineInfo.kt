package com.tibagni.logviewer.log

import com.tibagni.logviewer.filter.Filter
import java.io.File

data class LogLineInfo(
  val entry: LogEntry,
  val sourceFile: File?,
  val lineNumber: Int,
  val logStream: LogStream,
  val logLevel: LogLevel,
  val timestamp: LogTimestamp?,
  val pid: Int?,
  val tid: Int?,
  val tag: String?,
  val message: String,
  val appliedFilter: Filter?,
  val index: Int
) {
  val fileName: String
    get() = sourceFile?.name ?: "-"

  val filePath: String
    get() = sourceFile?.absolutePath ?: "-"

  val hasFile: Boolean
    get() = sourceFile != null && sourceFile.exists()

  val formattedLineNumber: String
    get() = if (lineNumber > 0) lineNumber.toString() else "-"

  val formattedIndex: String
    get() = if (index >= 0) (index + 1).toString() else "-"

  val formattedPid: String
    get() = pid?.toString() ?: "-"

  val formattedTid: String
    get() = tid?.toString() ?: "-"

  val formattedTimestamp: String
    get() {
      val ts = timestamp ?: return "-"
      return String.format("%02d-%02d %02d:%02d:%02d.%03d", ts.month, ts.day, ts.hour, ts.minutes, ts.seconds, ts.hundredth)
    }

  val formattedTag: String
    get() = tag ?: "-"
}
