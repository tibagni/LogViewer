package com.tibagni.logviewer.view

import com.tibagni.logviewer.log.LogCellRenderer
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.log.LogListTableModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.awt.event.KeyEvent
import javax.swing.table.DefaultTableModel

class SearchableTableTests {

  @Test
  fun testFindNextMatchIndexSearchDown() {
    val table = SearchableTable(DefaultTableModel())
    val matches = listOf(5, 10, 20, 30)

    // When starting before the first element
    assertEquals(0, table.findNextMatchIndex(matches, lastPos = 0, searchDown = true))
    assertEquals(0, table.findNextMatchIndex(matches, lastPos = -1, searchDown = true))

    // When exactly on an element
    assertEquals(1, table.findNextMatchIndex(matches, lastPos = 5, searchDown = true))
    assertEquals(2, table.findNextMatchIndex(matches, lastPos = 10, searchDown = true))
    assertEquals(3, table.findNextMatchIndex(matches, lastPos = 20, searchDown = true))

    // When between elements
    assertEquals(1, table.findNextMatchIndex(matches, lastPos = 7, searchDown = true))
    assertEquals(2, table.findNextMatchIndex(matches, lastPos = 15, searchDown = true))
    assertEquals(3, table.findNextMatchIndex(matches, lastPos = 25, searchDown = true))

    // When on the last element or after the last element (wraps around to 0)
    assertEquals(0, table.findNextMatchIndex(matches, lastPos = 30, searchDown = true))
    assertEquals(0, table.findNextMatchIndex(matches, lastPos = 35, searchDown = true))
  }

  @Test
  fun testFindNextMatchIndexSearchUp() {
    val table = SearchableTable(DefaultTableModel())
    val matches = listOf(5, 10, 20, 30)

    // When after the last element
    assertEquals(3, table.findNextMatchIndex(matches, lastPos = 35, searchDown = false))

    // When exactly on an element
    assertEquals(2, table.findNextMatchIndex(matches, lastPos = 30, searchDown = false))
    assertEquals(1, table.findNextMatchIndex(matches, lastPos = 20, searchDown = false))
    assertEquals(0, table.findNextMatchIndex(matches, lastPos = 10, searchDown = false))

    // When between elements
    assertEquals(2, table.findNextMatchIndex(matches, lastPos = 25, searchDown = false))
    assertEquals(1, table.findNextMatchIndex(matches, lastPos = 15, searchDown = false))
    assertEquals(0, table.findNextMatchIndex(matches, lastPos = 7, searchDown = false))

    // When on the first element or before first element (wraps around to last index)
    assertEquals(3, table.findNextMatchIndex(matches, lastPos = 5, searchDown = false))
    assertEquals(3, table.findNextMatchIndex(matches, lastPos = 0, searchDown = false))
    assertEquals(3, table.findNextMatchIndex(matches, lastPos = -1, searchDown = false))
  }

  @Test
  fun testFindNextMatchIndexSingleElement() {
    val table = SearchableTable(DefaultTableModel())
    val matches = listOf(10)

    // Search down
    assertEquals(0, table.findNextMatchIndex(matches, lastPos = -1, searchDown = true))
    assertEquals(0, table.findNextMatchIndex(matches, lastPos = 5, searchDown = true))
    assertEquals(0, table.findNextMatchIndex(matches, lastPos = 10, searchDown = true))
    assertEquals(0, table.findNextMatchIndex(matches, lastPos = 15, searchDown = true))

    // Search up
    assertEquals(0, table.findNextMatchIndex(matches, lastPos = -1, searchDown = false))
    assertEquals(0, table.findNextMatchIndex(matches, lastPos = 5, searchDown = false))
    assertEquals(0, table.findNextMatchIndex(matches, lastPos = 10, searchDown = false))
    assertEquals(0, table.findNextMatchIndex(matches, lastPos = 15, searchDown = false))
  }

  @Test
  fun testSearchContentSequential() = runBlocking {
    val model = LogListTableModel("Test")
    val entries = (0 until 100).map { i ->
      LogEntry(if (i % 10 == 0) "Match line $i" else "Normal line $i", LogLevel.VERBOSE, null)
    }
    model.setLogs(entries)
    val table = SearchableTable(model)

    val matches = table.searchContent(SearchableTable.SearchRequest("Match")).await()

    assertEquals(10, matches.size)
    assertEquals((0 until 100 step 10).toList(), matches)

    // Verify searchFilter is set on the table, not on LogEntry
    val filter = LogCellRenderer.getSearchFilter(table.table)
    assertNotNull("Table should have search filter", filter)
    assertEquals("Match", filter?.patternString)
  }

  @Test
  fun testSearchContentLargeList() = runBlocking {
    val model = LogListTableModel("Test")
    val count = 25_000
    val entries = (0 until count).map { i ->
      LogEntry(if (i % 2500 == 0) "Target line $i" else "Ignored line $i", LogLevel.VERBOSE, null)
    }
    model.setLogs(entries)
    val table = SearchableTable(model)

    val matches = table.searchContent(SearchableTable.SearchRequest("Target")).await()

    val expected = (0 until count step 2500).toList()
    assertEquals(expected.size, matches.size)
    assertEquals(expected, matches)

    // Verify all match indices are strictly sorted
    for (i in 0 until matches.size - 1) {
      assertTrue(matches[i] < matches[i + 1])
    }
  }

  @Test
  fun testSearchContentClear() = runBlocking {
    val model = LogListTableModel("Test")
    val entries = (0 until 100).map { i ->
      LogEntry(if (i % 10 == 0) "Match line $i" else "Normal line $i", LogLevel.VERBOSE, null)
    }
    model.setLogs(entries)
    val table = SearchableTable(model)

    // First search
    table.searchContent(SearchableTable.SearchRequest("Match")).await()
    assertNotNull(LogCellRenderer.getSearchFilter(table.table))

    // Clear search
    val matches = table.searchContent(SearchableTable.SearchRequest("")).await()
    assertTrue(matches.isEmpty())

    // Highlights should now be cleared on the table
    assertNull("Search filter should be null on table after clear", LogCellRenderer.getSearchFilter(table.table))
  }

  @Test
  fun testMultipleTablesSharingSameRendererDoNotCrossContaminate() = runBlocking {
    val entries = listOf(
      LogEntry("Common log line with foo", LogLevel.VERBOSE, null),
      LogEntry("Another line with bar", LogLevel.VERBOSE, null)
    )

    val model1 = LogListTableModel("Table 1")
    model1.setLogs(entries)
    val table1 = SearchableTable(model1)

    val model2 = LogListTableModel("Table 2")
    model2.setLogs(entries)
    val table2 = SearchableTable(model2)

    // Both tables share the exact same LogCellRenderer instance
    val sharedRenderer = LogCellRenderer()
    table1.table.setDefaultRenderer(LogEntry::class.java, sharedRenderer)
    table2.table.setDefaultRenderer(LogEntry::class.java, sharedRenderer)

    // Search foo in table 1
    table1.searchContent(SearchableTable.SearchRequest("foo")).await()

    val filter1 = LogCellRenderer.getSearchFilter(table1.table)
    val filter2 = LogCellRenderer.getSearchFilter(table2.table)

    assertNotNull("Table 1 should have searchFilter", filter1)
    assertEquals("foo", filter1?.patternString)

    // Table 2 should have NO searchFilter, despite sharing the exact same renderer instance
    assertNull("Table 2 should NOT have searchFilter", filter2)
  }

  @Test
  fun testEnterAndShiftEnterNavigation() = runBlocking {
    val model = LogListTableModel("Test")
    val entries = (0 until 50).map { i ->
      LogEntry(if (i % 10 == 0) "Target line $i" else "Line $i", LogLevel.VERBOSE, null)
    }
    model.setLogs(entries)
    val table = SearchableTable(model)

    // Execute search for "Target" (matches at 0, 10, 20, 30, 40)
    table.searchContent(SearchableTable.SearchRequest("Target")).await()

    // Send Enter (search down/next)
    val enterEvent = KeyEvent(table.searchText, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED)
    table.searchText.keyListeners.forEach { it.keyPressed(enterEvent) }
    delay(100)
    assertEquals(0, LogCellRenderer.getHighlightLine(table.table))

    // Next Enter -> row 10
    table.searchText.keyListeners.forEach { it.keyPressed(enterEvent) }
    delay(100)
    assertEquals(10, LogCellRenderer.getHighlightLine(table.table))

    // Shift + Enter -> row 0
    val shiftEnterEvent = KeyEvent(table.searchText, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), KeyEvent.SHIFT_DOWN_MASK, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED)
    table.searchText.keyListeners.forEach { it.keyPressed(shiftEnterEvent) }
    delay(100)
    assertEquals(0, LogCellRenderer.getHighlightLine(table.table))

    // Another Shift + Enter -> wraps to row 40
    table.searchText.keyListeners.forEach { it.keyPressed(shiftEnterEvent) }
    delay(100)
    assertEquals(40, LogCellRenderer.getHighlightLine(table.table))
  }

  @Test
  fun testMyLogsTableSearchHighlight() = runBlocking {
    val model = LogListTableModel("My Logs")
    val entry = LogEntry("hello world from my logs", LogLevel.VERBOSE, null)
    model.setLogs(listOf(entry))
    val table = SearchableTable(model)
    val myLogsRenderer = LogCellRenderer()
    table.table.setDefaultRenderer(LogEntry::class.java, myLogsRenderer)

    table.searchContent(SearchableTable.SearchRequest("world")).await()

    val comp = myLogsRenderer.getTableCellRendererComponent(table.table, entry, false, false, 0, 0)
    val renderer = comp as LogCellRenderer
    val field = LogCellRenderer::class.java.getDeclaredField("textView")
    field.isAccessible = true
    val textView = field.get(renderer) as javax.swing.JTextArea
    val highlights = textView.highlighter.highlights
    assertTrue("Highlighter should contain highlights", highlights.isNotEmpty())
    assertEquals(6, highlights[0].startOffset)
    assertEquals(11, highlights[0].endOffset)
  }
}
