package com.tibagni.logviewer.log

import java.util.*
import kotlin.math.abs

class EditableLogListTableModel(title: String) : LogListTableModel(title) {

  fun addLogIfDoesNotExist(entry: LogEntry) {
    // find the nearest time pos to insert the new entry
    val indexFound = Collections.binarySearch(entries, entry)
    if (indexFound < 0) { // Element not found. Insert
      val targetIndex = abs(indexFound + 1)
      entries.add(targetIndex, entry)
      fireTableRowsInserted(targetIndex, targetIndex)
    }
  }

  fun removeLog(entry: LogEntry) {
    val index = entries.indexOf(entry)
    if (index != -1) {
      entries.removeAt(index)
      fireTableRowsDeleted(index, index)
    }
  }

  fun clear() {
    if (entries.isEmpty()) return
    val index = entries.size - 1
    entries.clear()
    fireTableRowsDeleted(0, index)
  }
}