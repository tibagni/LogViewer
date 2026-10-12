package com.tibagni.logviewer.log

import org.junit.Assert.*
import org.junit.Test
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener

class LogListTableModelTests {

  @Test
  fun testAddLogsAppendsAndFiresEvent() {
    val model = LogListTableModel("Test")
    val events = mutableListOf<TableModelEvent>()
    model.addTableModelListener { events.add(it) }

    val entry1 = LogEntry("Line 1", LogLevel.INFO, null, LogStream.MAIN)
    val entry2 = LogEntry("Line 2", LogLevel.DEBUG, null, LogStream.MAIN)
    model.addLogs(listOf(entry1, entry2))

    assertEquals(2, model.rowCount)
    assertEquals(entry1, model.getValueAt(0, 0))
    assertEquals(entry2, model.getValueAt(1, 0))
    assertEquals(1, events.size)
    assertEquals(TableModelEvent.INSERT, events[0].type)
    assertEquals(0, events[0].firstRow)
    assertEquals(1, events[0].lastRow)

    val entry3 = LogEntry("Line 3", LogLevel.ERROR, null, LogStream.MAIN)
    model.addLogs(listOf(entry3))

    assertEquals(3, model.rowCount)
    assertEquals(entry3, model.getValueAt(2, 0))
    assertEquals(2, events.size)
    assertEquals(TableModelEvent.INSERT, events[1].type)
    assertEquals(2, events[1].firstRow)
    assertEquals(2, events[1].lastRow)
  }

  @Test
  fun testRemoveFirstLogsEvictsAndFiresEvent() {
    val model = LogListTableModel("Test")
    val entries = (1..5).map { LogEntry("Line $it", LogLevel.INFO, null, LogStream.MAIN) }
    model.setLogs(entries)

    val events = mutableListOf<TableModelEvent>()
    model.addTableModelListener { events.add(it) }

    model.removeFirstLogs(2)

    assertEquals(3, model.rowCount)
    assertEquals("Line 3", (model.getValueAt(0, 0) as LogEntry).logText)
    assertEquals(1, events.size)
    assertEquals(TableModelEvent.DELETE, events[0].type)
    assertEquals(0, events[0].firstRow)
    assertEquals(1, events[0].lastRow)
  }

  @Test
  fun testRemoveFirstLogsMoreThanSizeClamps() {
    val model = LogListTableModel("Test")
    val entries = listOf(LogEntry("Line 1", LogLevel.INFO, null, LogStream.MAIN))
    model.setLogs(entries)

    model.removeFirstLogs(10)
    assertEquals(0, model.rowCount)
  }

  @Test
  fun testClearResetsAllRows() {
    val model = LogListTableModel("Test")
    model.setLogs(listOf(LogEntry("Line 1", LogLevel.INFO, null, LogStream.MAIN)))

    model.clear()
    assertEquals(0, model.rowCount)
    assertNull(model.lastEntry)
  }
}
