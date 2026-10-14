package com.tibagni.logviewer.live

import com.tibagni.logviewer.ProgressReporter
import com.tibagni.logviewer.filter.Filter
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogStream
import com.tibagni.logviewer.log.LogTimestamp
import org.junit.Assert.*
import org.junit.Test
import java.awt.Color

class LiveLogsRepositoryTests {

  private fun createEntry(text: String, level: LogLevel = LogLevel.DEBUG, stream: LogStream = LogStream.MAIN): LogEntry {
    return LogEntry(text, level, LogTimestamp(5, 12, 13, 45, 26, 123), stream)
  }

  private fun createFilter(name: String, pattern: String, level: LogLevel = LogLevel.VERBOSE): Filter {
    return Filter(name, pattern, Color.RED, level, false)
  }

  @Test
  fun testAddLogEntryIncrementsCountsAndAssignsIndices() {
    val repo = LiveLogsRepositoryImpl(maxCapacity = 100)

    val entry1 = createEntry("First log", stream = LogStream.MAIN)
    val entry2 = createEntry("Second log", stream = LogStream.SYSTEM)

    val batch1 = repo.addLogEntry(entry1)
    val batch2 = repo.addLogEntry(entry2)

    assertEquals(0, entry1.index)
    assertEquals(1, entry2.index)
    assertEquals(2, repo.totalLogsReceived)
    assertEquals(0, repo.droppedLogsCount)
    assertEquals(2, repo.allLogsCount)
    assertEquals(0, repo.filteredLogsCount)
    assertTrue(repo.availableStreams.contains(LogStream.MAIN))
    assertTrue(repo.availableStreams.contains(LogStream.SYSTEM))

    assertEquals(1, batch1.newAllLogs.size)
    assertEquals(0, batch1.newFilteredLogs.size)
    assertEquals(1, batch2.newAllLogs.size)
  }

  @Test
  fun testIncrementalFilterEvaluation() {
    val repo = LiveLogsRepositoryImpl(maxCapacity = 100)
    val filter = createFilter("ErrorFilter", "FATAL")
    repo.setLiveFilters(listOf(filter))

    val entry1 = createEntry("Normal line")
    val entry2 = createEntry("FATAL EXCEPTION occurred")

    repo.addLogEntry(entry1)
    repo.addLogEntry(entry2)

    assertEquals(2, repo.allLogsCount)
    assertEquals(1, repo.filteredLogsCount)
    assertNull(entry1.appliedFilter)
    assertSame(filter, entry2.appliedFilter)
    assertEquals(1, filter.temporaryInfo.totalLinesFound)
    assertEquals(entry2, repo.filteredLogs[0])
  }

  @Test
  fun testMultipleFiltersFirstMatchWinsForStyle() {
    val repo = LiveLogsRepositoryImpl(maxCapacity = 100)
    val filter1 = createFilter("Filter1", "Network")
    val filter2 = createFilter("Filter2", "Error")
    repo.setLiveFilters(listOf(filter1, filter2))

    val entry = createEntry("Network Error occurred")
    repo.addLogEntry(entry)

    assertSame(filter1, entry.appliedFilter)
    assertEquals(1, filter1.temporaryInfo.totalLinesFound)
    assertEquals(1, filter2.temporaryInfo.totalLinesFound)
    assertEquals(1, repo.filteredLogsCount)
  }

  @Test
  fun testBufferEvictionWhenCapacityExceeded() {
    val repo = LiveLogsRepositoryImpl(maxCapacity = 3)

    val e1 = createEntry("Line 1")
    val e2 = createEntry("Line 2")
    val e3 = createEntry("Line 3")
    val e4 = createEntry("Line 4")

    repo.addLogEntry(e1)
    repo.addLogEntry(e2)
    repo.addLogEntry(e3)
    val batch4 = repo.addLogEntry(e4)

    assertEquals(3, repo.allLogsCount)
    assertEquals(4, repo.totalLogsReceived)
    assertEquals(1, repo.droppedLogsCount)
    assertEquals(1, repo.visibleLogsOffset)
    assertEquals(1, batch4.evictedAllCount)

    // Oldest entry (e1 with index 0) was evicted
    assertNull(repo.getEntryByAbsoluteIndex(0))
    assertEquals(e2, repo.getEntryByAbsoluteIndex(1))
    assertEquals(e3, repo.getEntryByAbsoluteIndex(2))
    assertEquals(e4, repo.getEntryByAbsoluteIndex(3))

    assertEquals(e2, repo.getEntryAt(0))
    assertEquals(e3, repo.getEntryAt(1))
    assertEquals(e4, repo.getEntryAt(2))
    assertNull(repo.getEntryAt(3))
  }

  @Test
  fun testBufferEvictionRemovesFromFilteredLogsAndDecrementsCount() {
    val repo = LiveLogsRepositoryImpl(maxCapacity = 2)
    val filter = createFilter("ErrorFilter", "Error")
    repo.setLiveFilters(listOf(filter))

    val e1 = createEntry("Error: first")
    val e2 = createEntry("Info: second")
    val e3 = createEntry("Info: third")

    repo.addLogEntry(e1)
    repo.addLogEntry(e2)

    assertEquals(2, repo.allLogsCount)
    assertEquals(1, repo.filteredLogsCount)
    assertEquals(1, filter.temporaryInfo.totalLinesFound)

    // e3 causes e1 to be evicted from allLogs AND filteredLogs
    val batch3 = repo.addLogEntry(e3)

    assertEquals(2, repo.allLogsCount)
    assertEquals(0, repo.filteredLogsCount)
    assertEquals(0, filter.temporaryInfo.totalLinesFound)
    assertEquals(1, batch3.evictedAllCount)
    assertEquals(1, batch3.evictedFilteredCount)
  }

  @Test
  fun testSetAppliedFiltersReEvaluatesExistingBuffer() {
    val repo = LiveLogsRepositoryImpl(maxCapacity = 100)

    val e1 = createEntry("Connecting to server")
    val e2 = createEntry("Failed to connect: Error 500")
    val e3 = createEntry("Retry connected")

    repo.addLogEntries(listOf(e1, e2, e3))
    assertEquals(0, repo.filteredLogsCount)

    val filter = createFilter("ConnectError", "Error")
    repo.setLiveFilters(listOf(filter))

    assertEquals(1, repo.filteredLogsCount)
    assertEquals(e2, repo.filteredLogs[0])
    assertSame(filter, e2.appliedFilter)
    assertNull(e1.appliedFilter)
    assertNull(e3.appliedFilter)
    assertEquals(1, filter.temporaryInfo.totalLinesFound)
  }

  @Test
  fun testClearResetsState() {
    val repo = LiveLogsRepositoryImpl(maxCapacity = 2)
    val filter = createFilter("ErrorFilter", "Error")
    repo.setLiveFilters(listOf(filter))

    repo.addLogEntry(createEntry("Error 1"))
    repo.addLogEntry(createEntry("Error 2"))
    repo.addLogEntry(createEntry("Error 3")) // Causes 1 eviction

    assertEquals(2, repo.allLogsCount)
    assertEquals(2, repo.filteredLogsCount)
    assertEquals(3, repo.totalLogsReceived)
    assertEquals(1, repo.droppedLogsCount)

    repo.clear()

    assertEquals(0, repo.allLogsCount)
    assertEquals(0, repo.filteredLogsCount)
    assertEquals(0, repo.totalLogsReceived)
    assertEquals(0, repo.droppedLogsCount)
    assertEquals(0, repo.visibleLogsOffset)
    assertTrue(repo.availableStreams.isEmpty())
    assertEquals(0, filter.temporaryInfo.totalLinesFound)
  }

  @Test
  fun testBatchAdditionPerformanceAndAccounting() {
    val repo = LiveLogsRepositoryImpl(maxCapacity = 100)
    val filter = createFilter("EvenFilter", "Line 2")
    repo.setLiveFilters(listOf(filter))

    val entries = (1..150).map { createEntry("Line $it") }
    val batch = repo.addLogEntries(entries)

    assertEquals(100, repo.allLogsCount)
    assertEquals(150, repo.totalLogsReceived)
    assertEquals(50, repo.droppedLogsCount)
    assertEquals(150, batch.newAllLogs.size)
    assertEquals(50, batch.evictedAllCount)
  }

  @Test
  fun testSetLiveFiltersWithProgressReporter() {
    val repo = LiveLogsRepositoryImpl(maxCapacity = 100)
    val entries = (1..20).map { createEntry("Log entry $it") }
    repo.addLogEntries(entries)

    val filter = createFilter("EntryFilter", "entry 1")
    val progressUpdates = mutableListOf<Int>()
    val progressReporter = ProgressReporter { progress, _ ->
      progressUpdates.add(progress)
    }

    repo.setLiveFilters(listOf(filter), progressReporter)

    assertTrue(progressUpdates.isNotEmpty())
    assertEquals(100, progressUpdates.last())
    assertEquals(11, repo.filteredLogsCount)
  }

  @Test
  fun testSetLiveFiltersEmptyClearsAppliedFilters() {
    val repo = LiveLogsRepositoryImpl(maxCapacity = 100)
    val e1 = createEntry("Line with error")
    val filter = createFilter("ErrorFilter", "error")
    repo.setLiveFilters(listOf(filter))
    repo.addLogEntry(e1)

    assertSame(filter, e1.appliedFilter)
    assertEquals(1, repo.filteredLogsCount)

    repo.setLiveFilters(emptyList())

    assertNull(e1.appliedFilter)
    assertEquals(0, repo.filteredLogsCount)
  }
}
