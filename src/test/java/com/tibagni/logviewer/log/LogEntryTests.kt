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
}