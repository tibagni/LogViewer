package com.tibagni.logviewer.view

import org.junit.Assert.assertEquals
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
}
