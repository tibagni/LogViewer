package com.tibagni.logviewer

import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.log.LogTimestamp
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MyLogsRepositoryTests {
  private lateinit var myLogsRepository: MyLogsRepository

  @Before
  fun setUp() {
    myLogsRepository = MyLogsRepositoryImpl()
  }

  @Test
  fun testAddLogEntries() {
    // Timestamps are important here ad it is what is used to order the logs
    val log1 = LogEntry("48012 09-04 13:34:22.530 Log line 1", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 22, 530))
    val log2 = LogEntry("48012 09-04 13:34:23.210 Log line 2", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 23, 210))
    val log3 = LogEntry("48012 09-04 13:34:24.120 Log line 3", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 24, 120))
    val log4 = LogEntry("48012 09-04 13:34:24.312 Log line 4", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 24, 312))
    val log5 = LogEntry("48012 09-04 13:34:25.003 Log line 5", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 25, 3))

    myLogsRepository.addLogEntries(listOf(log1, log4))
    myLogsRepository.addLogEntries(listOf(log2, log5))
    myLogsRepository.addLogEntries(listOf(log3))

    assertEquals(5, myLogsRepository.logs.size)
    assertEquals(listOf(log1, log2, log3, log4, log5), myLogsRepository.logs)
  }

  @Test
  fun testAddLogEntriesDuplicates() {
    // Timestamps are important here ad it is what is used to order the logs
    val log1 = LogEntry("48012 09-04 13:34:22.530 Log line 1", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 22, 530))
    val log2 = LogEntry("48012 09-04 13:34:23.210 Log line 2", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 23, 210))
    val log3 = LogEntry("48012 09-04 13:34:24.120 Log line 3", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 24, 120))
    val log4 = LogEntry("48012 09-04 13:34:24.312 Log line 4", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 24, 312))
    val log5 = LogEntry("48012 09-04 13:34:25.003 Log line 5", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 25, 3))

    myLogsRepository.addLogEntries(listOf(log1, log4))
    myLogsRepository.addLogEntries(listOf(log2, log5))
    myLogsRepository.addLogEntries(listOf(log3))
    myLogsRepository.addLogEntries(listOf(log3))
    myLogsRepository.addLogEntries(listOf(log2))
    myLogsRepository.addLogEntries(listOf(log5))

    // Make sure the duplicated entries are ignored
    assertEquals(5, myLogsRepository.logs.size)
    assertEquals(listOf(log1, log2, log3, log4, log5), myLogsRepository.logs)
  }

  @Test
  fun testResetLogs() {
    // Timestamps are important here ad it is what is used to order the logs
    val log1 = LogEntry("48012 09-04 13:34:22.530 Log line 1", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 22, 530))
    val log2 = LogEntry("48012 09-04 13:34:23.210 Log line 2", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 23, 210))
    val log3 = LogEntry("48012 09-04 13:34:24.120 Log line 3", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 24, 120))
    val log4 = LogEntry("48012 09-04 13:34:24.312 Log line 4", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 24, 312))
    val log5 = LogEntry("48012 09-04 13:34:25.003 Log line 5", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 25, 3))

    myLogsRepository.addLogEntries(listOf(log1, log2))
    assertEquals(2, myLogsRepository.logs.size)
    assertEquals(listOf(log1, log2), myLogsRepository.logs)

    myLogsRepository.reset(listOf(log3))
    assertEquals(1, myLogsRepository.logs.size)
    assertEquals(listOf(log3), myLogsRepository.logs)

    myLogsRepository.reset(listOf(log4, log5))
    assertEquals(2, myLogsRepository.logs.size)
    assertEquals(listOf(log4, log5), myLogsRepository.logs)
  }

  @Test
  fun testRemoveLogs() {
    // Timestamps are important here ad it is what is used to order the logs
    val log1 = LogEntry("48012 09-04 13:34:22.530 Log line 1", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 22, 530))
    val log2 = LogEntry("48012 09-04 13:34:23.210 Log line 2", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 23, 210))
    val log3 = LogEntry("48012 09-04 13:34:24.120 Log line 3", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 24, 120))
    val log4 = LogEntry("48012 09-04 13:34:24.312 Log line 4", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 24, 312))
    val log5 = LogEntry("48012 09-04 13:34:25.003 Log line 5", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 25, 3))

    myLogsRepository.addLogEntries(listOf(log1, log2, log3, log4, log5))
    assertEquals(5, myLogsRepository.logs.size)
    assertEquals(listOf(log1, log2, log3, log4, log5), myLogsRepository.logs)

    myLogsRepository.removeLogEntries(listOf(log1))
    assertEquals(4, myLogsRepository.logs.size)
    assertEquals(listOf(log2, log3, log4, log5), myLogsRepository.logs)

    myLogsRepository.removeLogEntries(listOf(log3, log4))
    assertEquals(2, myLogsRepository.logs.size)
    assertEquals(listOf(log2, log5), myLogsRepository.logs)
  }

  @Test
  fun testRemoveNonExistentLog() {
    // Timestamps are important here ad it is what is used to order the logs
    val log1 = LogEntry("48012 09-04 13:34:22.530 Log line 1", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 22, 530))
    val log2 = LogEntry("48012 09-04 13:34:23.210 Log line 2", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 23, 210))
    val log3 = LogEntry("48012 09-04 13:34:24.120 Log line 3", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 24, 120))
    val log4 = LogEntry("48012 09-04 13:34:24.312 Log line 4", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 24, 312))
    val log5 = LogEntry("48012 09-04 13:34:25.003 Log line 5", LogLevel.DEBUG, LogTimestamp(9, 4, 13, 34, 25, 3))

    myLogsRepository.addLogEntries(listOf(log1, log2, log3, log4))
    assertEquals(4, myLogsRepository.logs.size)
    assertEquals(listOf(log1, log2, log3, log4), myLogsRepository.logs)

    myLogsRepository.removeLogEntries(listOf(log5))
    assertEquals(4, myLogsRepository.logs.size)
    assertEquals(listOf(log1, log2, log3, log4), myLogsRepository.logs)
  }
}