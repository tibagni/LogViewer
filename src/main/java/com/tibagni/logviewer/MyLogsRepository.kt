package com.tibagni.logviewer

import com.tibagni.logviewer.log.LogEntry
import java.util.*
import kotlin.math.abs

interface MyLogsRepository {
  val logs: List<LogEntry>

  fun addLogEntries(entries: List<LogEntry>)
  fun removeLogEntries(entries: List<LogEntry>)
  fun reset(entries: List<LogEntry>)
}

class MyLogsRepositoryImpl : MyLogsRepository {
  private val _logs = mutableListOf<LogEntry>()
  override val logs: List<LogEntry>
    get() = _logs

  override fun addLogEntries(entries: List<LogEntry>) {
    // We need to add the logs in order
    for (entry in entries) {
      insertInOrder(entry)
    }
  }

  override fun removeLogEntries(entries: List<LogEntry>) {
    _logs.removeAll(entries)
  }

  override fun reset(entries: List<LogEntry>) {
    _logs.clear()
    _logs.addAll(entries)
  }

  private fun insertInOrder(entry: LogEntry) {
    // find the nearest time pos to insert the new entry
    val indexFound = Collections.binarySearch(_logs, entry)
    if (indexFound < 0) { // Element not found. Insert
      val targetIndex = abs(indexFound + 1)
      _logs.add(targetIndex, entry)
    }
  }
}