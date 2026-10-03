package com.tibagni.logviewer.view

import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.log.LogListTableModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
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

    // Verify highlights are set
    for (i in 0 until 100) {
      val entry = model.getValueAt(i, 0) as LogEntry
      if (i % 10 == 0) {
        assertNotNull("Entry $i should have search filter", entry.searchFilter)
      } else {
        assertNull("Entry $i should not have search filter", entry.searchFilter)
      }
    }
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
    assertNotNull((model.getValueAt(0, 0) as LogEntry).searchFilter)

    // Clear search
    val matches = table.searchContent(SearchableTable.SearchRequest("")).await()
    assertTrue(matches.isEmpty())

    // Highlights should now be cleared
    for (i in 0 until 100) {
      val entry = model.getValueAt(i, 0) as LogEntry
      assertNull("Entry $i highlight should be cleared", entry.searchFilter)
    }
  }
}
