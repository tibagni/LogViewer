package com.tibagni.logviewer.log

import org.junit.Assert.*
import org.junit.Test

class LogEntryTests {

  @Test
  fun testLogEntryEquals() {
    val entry1 = LogEntry("Text1", LogLevel.DEBUG, LogTimestamp(9, 1, 8, 0, 0, 0))
    val entry2 = LogEntry("Text1", LogLevel.DEBUG, LogTimestamp(9, 1, 8, 0, 0, 0))

    assertEquals(entry1, entry2)
    assertEquals(entry1.hashCode(), entry2.hashCode())
  }

  @Test
  fun testLogEntryTextNotEquals() {
    val entry1 = LogEntry("Text1", LogLevel.DEBUG, LogTimestamp(9, 1, 8, 0, 0, 0))
    val entry2 = LogEntry("Text2", LogLevel.DEBUG, LogTimestamp(9, 1, 8, 0, 0, 0))

    assertNotEquals(entry1, entry2)
    assertNotEquals(entry1.hashCode(), entry2.hashCode())
  }

  @Test
  fun testLogEntryLevelNotEquals() {
    val entry1 = LogEntry("Text1", LogLevel.DEBUG, LogTimestamp(9, 1, 8, 0, 0, 0))
    val entry2 = LogEntry("Text1", LogLevel.INFO, LogTimestamp(9, 1, 8, 0, 0, 0))

    assertNotEquals(entry1, entry2)
    assertNotEquals(entry1.hashCode(), entry2.hashCode())
  }

  @Test
  fun testLogEntryTimestampMonthNotEquals() {
    val entry1 = LogEntry("Text1", LogLevel.DEBUG, LogTimestamp(9, 1, 8, 0, 0, 0))
    val entry2 = LogEntry("Text1", LogLevel.DEBUG, LogTimestamp(8, 1, 8, 0, 0, 0))

    assertNotEquals(entry1, entry2)
    assertNotEquals(entry1.hashCode(), entry2.hashCode())
  }

  @Test
  fun testLogEntryTimestampDayNotEquals() {
    val entry1 = LogEntry("Text1", LogLevel.DEBUG, LogTimestamp(9, 1, 8, 0, 0, 0))
    val entry2 = LogEntry("Text1", LogLevel.DEBUG, LogTimestamp(9, 2, 8, 0, 0, 0))

    assertNotEquals(entry1, entry2)
    assertNotEquals(entry1.hashCode(), entry2.hashCode())
  }

  @Test
  fun testLogEntryTimestampHourNotEquals() {
    val entry1 = LogEntry("Text1", LogLevel.DEBUG, LogTimestamp(9, 1, 8, 0, 0, 0))
    val entry2 = LogEntry("Text1", LogLevel.DEBUG, LogTimestamp(9, 2, 3, 0, 0, 0))

    assertNotEquals(entry1, entry2)
    assertNotEquals(entry1.hashCode(), entry2.hashCode())
  }

  @Test
  fun testLogEntryTimestampMinutesNotEquals() {
    val entry1 = LogEntry("Text1", LogLevel.DEBUG, LogTimestamp(9, 1, 8, 0, 0, 0))
    val entry2 = LogEntry("Text1", LogLevel.DEBUG, LogTimestamp(9, 2, 8, 10, 0, 0))

    assertNotEquals(entry1, entry2)
    assertNotEquals(entry1.hashCode(), entry2.hashCode())
  }

  @Test
  fun testLogEntryTimestampSecondsNotEquals() {
    val entry1 = LogEntry("Text1", LogLevel.DEBUG, LogTimestamp(9, 1, 8, 0, 0, 0))
    val entry2 = LogEntry("Text1", LogLevel.DEBUG, LogTimestamp(9, 2, 8, 0, 30, 0))

    assertNotEquals(entry1, entry2)
    assertNotEquals(entry1.hashCode(), entry2.hashCode())
  }

  @Test
  fun testLogEntryTimestampHundredthNotEquals() {
    val entry1 = LogEntry("Text1", LogLevel.DEBUG, LogTimestamp(9, 1, 8, 0, 0, 0))
    val entry2 = LogEntry("Text1", LogLevel.DEBUG, LogTimestamp(9, 2, 8, 0, 0, 90))

    assertNotEquals(entry1, entry2)
    assertNotEquals(entry1.hashCode(), entry2.hashCode())
  }

  @Test
  fun testLogEntryWithLogStream() {
    val entry1 = LogEntry("Text1", LogLevel.DEBUG, LogTimestamp(9, 1, 8, 0, 0, 0), LogStream.MAIN)
    val entry2 = LogEntry("Text1", LogLevel.DEBUG, LogTimestamp(9, 1, 8, 0, 0, 0), "main.txt")
    val entryNullStream = LogEntry("Text1", LogLevel.DEBUG, LogTimestamp(9, 1, 8, 0, 0, 0), null as LogStream?)

    assertEquals(LogStream.MAIN, entry1.logStream)
    assertEquals(LogStream.MAIN, entry2.logStream)
    assertEquals(entry1, entry2)
    assertEquals(LogStream.UNKNOWN, entryNullStream.logStream)
  }

  @Test
  fun testLogEntryWithSourceFileAndLineNumber() {
    val file1 = java.io.File("/path/to/log1.txt")
    val file2 = java.io.File("/path/to/log2.txt")
    val entry1 = LogEntry("Text1", LogLevel.DEBUG, LogTimestamp(9, 1, 8, 0, 0, 0), LogStream.MAIN, file1, 42)
    val entry2 = LogEntry("Text1", LogLevel.DEBUG, LogTimestamp(9, 1, 8, 0, 0, 0), LogStream.MAIN, file1, 42)
    val entryDifferentLine = LogEntry("Text1", LogLevel.DEBUG, LogTimestamp(9, 1, 8, 0, 0, 0), LogStream.MAIN, file1, 99)
    val entryDifferentFile = LogEntry("Text1", LogLevel.DEBUG, LogTimestamp(9, 1, 8, 0, 0, 0), LogStream.MAIN, file2, 42)

    assertEquals(file1, entry1.sourceFile)
    assertEquals(42, entry1.lineNumber)
    assertEquals(entry1, entry2)
    assertEquals(entry1.hashCode(), entry2.hashCode())

    assertNotEquals(entry1, entryDifferentLine)
    assertNotEquals(entry1, entryDifferentFile)
  }
}