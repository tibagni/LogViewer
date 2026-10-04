package com.tibagni.logviewer.log

import com.tibagni.logviewer.filter.Filter
import org.junit.Assert.*
import org.junit.Test
import java.awt.Color
import java.awt.GraphicsEnvironment
import java.io.File

class LogLineDetailsDialogTests {

  @Test
  fun testLogLineInfoProperties() {
    val tempFile = File.createTempFile("testLog", ".txt")
    tempFile.deleteOnExit()
    val entry = LogEntry("some log line", LogLevel.INFO, LogTimestamp(1, 2, 3, 4, 5, 6), LogStream.SYSTEM, tempFile, 50)
    val filter = Filter("TestFilter", "pattern", Color.RED, LogLevel.INFO)
    val info = LogLineInfo(
      entry = entry,
      sourceFile = tempFile,
      lineNumber = 50,
      logStream = LogStream.SYSTEM,
      logLevel = LogLevel.INFO,
      timestamp = LogTimestamp(1, 2, 3, 4, 5, 6),
      pid = 1234,
      tid = 5678,
      tag = "TestTag",
      message = "Hello world",
      appliedFilter = filter,
      index = 10
    )

    assertEquals(tempFile, info.sourceFile)
    assertEquals(tempFile.name, info.fileName)
    assertEquals(tempFile.absolutePath, info.filePath)
    assertEquals(50, info.lineNumber)
    assertEquals("50", info.formattedLineNumber)
    assertTrue(info.hasFile)
    assertEquals(LogStream.SYSTEM, info.logStream)
    assertEquals(LogLevel.INFO, info.logLevel)
    assertEquals(1234, info.pid)
    assertEquals("1234", info.formattedPid)
    assertEquals(5678, info.tid)
    assertEquals("5678", info.formattedTid)
    assertEquals("TestTag", info.tag)
    assertEquals("TestTag", info.formattedTag)
    assertEquals("Hello world", info.message)
    assertEquals(filter, info.appliedFilter)
    assertEquals(10, info.index)
    assertEquals("11", info.formattedIndex)
    assertEquals("01-02 03:04:05.006", info.formattedTimestamp)
  }

  @Test
  fun testLogLineInfoDefaultsWhenFieldsNull() {
    val entry = LogEntry("some log line", LogLevel.DEBUG, null)
    val info = LogLineInfo(
      entry = entry,
      sourceFile = null,
      lineNumber = -1,
      logStream = LogStream.UNKNOWN,
      logLevel = LogLevel.DEBUG,
      timestamp = null,
      pid = null,
      tid = null,
      tag = null,
      message = "some log line",
      appliedFilter = null,
      index = -1
    )

    assertNull(info.sourceFile)
    assertEquals("-", info.fileName)
    assertEquals("-", info.filePath)
    assertEquals(-1, info.lineNumber)
    assertEquals("-", info.formattedLineNumber)
    assertFalse(info.hasFile)
    assertNull(info.pid)
    assertEquals("-", info.formattedPid)
    assertNull(info.tid)
    assertEquals("-", info.formattedTid)
    assertNull(info.tag)
    assertEquals("-", info.formattedTag)
    assertEquals("-", info.formattedIndex)
    assertEquals("-", info.formattedTimestamp)
  }

  @Test
  fun testDialogInstantiationInHeadlessMode() {
    if (GraphicsEnvironment.isHeadless()) {
      return
    }

    val file = File("/path/to/log.txt")
    val entry = LogEntry("10-12 22:33:46.839 3172 3172 V Tag: message", LogLevel.VERBOSE, null, LogStream.MAIN, file, 100)
    val info = LogLineParser.parseLineInfo(entry)

    var filteredPid: Int? = null
    var filteredTag: String? = null
    var openedFile: File? = null
    var openedLine: Int? = null

    val dialog = LogLineDetailsDialog(
      owner = null,
      info = info!!,
      onFilterByPid = { filteredPid = it },
      onFilterByTag = { filteredTag = it },
      onOpenInEditor = { f, l ->
        openedFile = f
        openedLine = l
      }
    )

    assertNotNull(dialog)
    dialog.dispose()
  }
}
