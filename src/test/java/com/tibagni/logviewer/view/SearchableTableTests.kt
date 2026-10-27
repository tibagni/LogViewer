package com.tibagni.logviewer.view

import com.formdev.flatlaf.FlatClientProperties
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.log.LogCellRenderer
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.log.LogListTableModel
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.awt.Cursor
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.border.MatteBorder
import javax.swing.table.DefaultTableModel

class SearchableTableTests {

  @Test
  fun testFindNextMatchIndexSearchDown() {
    val table = SearchableTable(DefaultTableModel())
    val matches = listOf(5, 10, 20, 30)

    // When starting before the first element
    assertEquals(
      0,
      table.findNextMatchIndex(matches, lastPos = 0, searchDown = true)
    )
    assertEquals(
      0,
      table.findNextMatchIndex(matches, lastPos = -1, searchDown = true)
    )

    // When exactly on an element
    assertEquals(
      1,
      table.findNextMatchIndex(matches, lastPos = 5, searchDown = true)
    )
    assertEquals(
      2,
      table.findNextMatchIndex(matches, lastPos = 10, searchDown = true)
    )
    assertEquals(
      3,
      table.findNextMatchIndex(matches, lastPos = 20, searchDown = true)
    )

    // When between elements
    assertEquals(
      1,
      table.findNextMatchIndex(matches, lastPos = 7, searchDown = true)
    )
    assertEquals(
      2,
      table.findNextMatchIndex(matches, lastPos = 15, searchDown = true)
    )
    assertEquals(
      3,
      table.findNextMatchIndex(matches, lastPos = 25, searchDown = true)
    )

    // When on the last element or after the last element (wraps to 0)
    assertEquals(
      0,
      table.findNextMatchIndex(matches, lastPos = 30, searchDown = true)
    )
    assertEquals(
      0,
      table.findNextMatchIndex(matches, lastPos = 35, searchDown = true)
    )
  }

  @Test
  fun testFindNextMatchIndexSearchUp() {
    val table = SearchableTable(DefaultTableModel())
    val matches = listOf(5, 10, 20, 30)

    // When after the last element
    assertEquals(
      3,
      table.findNextMatchIndex(matches, lastPos = 35, searchDown = false)
    )

    // When exactly on an element
    assertEquals(
      2,
      table.findNextMatchIndex(matches, lastPos = 30, searchDown = false)
    )
    assertEquals(
      1,
      table.findNextMatchIndex(matches, lastPos = 20, searchDown = false)
    )
    assertEquals(
      0,
      table.findNextMatchIndex(matches, lastPos = 10, searchDown = false)
    )

    // When between elements
    assertEquals(
      2,
      table.findNextMatchIndex(matches, lastPos = 25, searchDown = false)
    )
    assertEquals(
      1,
      table.findNextMatchIndex(matches, lastPos = 15, searchDown = false)
    )
    assertEquals(
      0,
      table.findNextMatchIndex(matches, lastPos = 7, searchDown = false)
    )

    // When on first element or before first element (wraps to last)
    assertEquals(
      3,
      table.findNextMatchIndex(matches, lastPos = 5, searchDown = false)
    )
    assertEquals(
      3,
      table.findNextMatchIndex(matches, lastPos = 0, searchDown = false)
    )
    assertEquals(
      3,
      table.findNextMatchIndex(matches, lastPos = -1, searchDown = false)
    )
  }

  @Test
  fun testFindNextMatchIndexSingleElement() {
    val table = SearchableTable(DefaultTableModel())
    val matches = listOf(10)

    // Search down
    assertEquals(
      0,
      table.findNextMatchIndex(matches, lastPos = -1, searchDown = true)
    )
    assertEquals(
      0,
      table.findNextMatchIndex(matches, lastPos = 5, searchDown = true)
    )
    assertEquals(
      0,
      table.findNextMatchIndex(matches, lastPos = 10, searchDown = true)
    )
    assertEquals(
      0,
      table.findNextMatchIndex(matches, lastPos = 15, searchDown = true)
    )

    // Search up
    assertEquals(
      0,
      table.findNextMatchIndex(matches, lastPos = -1, searchDown = false)
    )
    assertEquals(
      0,
      table.findNextMatchIndex(matches, lastPos = 5, searchDown = false)
    )
    assertEquals(
      0,
      table.findNextMatchIndex(matches, lastPos = 10, searchDown = false)
    )
    assertEquals(
      0,
      table.findNextMatchIndex(matches, lastPos = 15, searchDown = false)
    )
  }

  @Test
  fun testSearchContentSequential() = runBlocking {
    val model = LogListTableModel("Test")
    val entries = (0 until 100).map { i ->
      val text = if (i % 10 == 0) "Match line $i" else "Normal line $i"
      LogEntry(text, LogLevel.VERBOSE, null)
    }
    model.setLogs(entries)
    val table = SearchableTable(model)

    val req = SearchableTable.SearchRequest("Match")
    val matches = table.searchContent(req).await()

    assertEquals(10, matches.size)
    assertEquals((0 until 100 step 10).toList(), matches)

    val filter = LogCellRenderer.getSearchFilter(table.table)
    assertNotNull("Table should have search filter", filter)
    assertEquals("Match", filter?.patternString)
  }

  @Test
  fun testSearchContentLargeList() = runBlocking {
    val model = LogListTableModel("Test")
    val count = 25_000
    val entries = (0 until count).map { i ->
      val text = if (i % 2500 == 0) "Target line $i" else "Ignored line $i"
      LogEntry(text, LogLevel.VERBOSE, null)
    }
    model.setLogs(entries)
    val table = SearchableTable(model)

    val req = SearchableTable.SearchRequest("Target")
    val matches = table.searchContent(req).await()

    val expected = (0 until count step 2500).toList()
    assertEquals(expected.size, matches.size)
    assertEquals(expected, matches)

    for (i in 0 until matches.size - 1) {
      assertTrue(matches[i] < matches[i + 1])
    }
  }

  @Test
  fun testSearchContentClear() = runBlocking {
    val model = LogListTableModel("Test")
    val entries = (0 until 100).map { i ->
      val text = if (i % 10 == 0) "Match line $i" else "Normal line $i"
      LogEntry(text, LogLevel.VERBOSE, null)
    }
    model.setLogs(entries)
    val table = SearchableTable(model)

    table.searchContent(SearchableTable.SearchRequest("Match")).await()
    assertNotNull(LogCellRenderer.getSearchFilter(table.table))

    val matches = table.searchContent(
      SearchableTable.SearchRequest("")
    ).await()
    assertTrue(matches.isEmpty())

    assertNull(
      "Search filter should be null on table after clear",
      LogCellRenderer.getSearchFilter(table.table)
    )
  }

  @Test
  fun testMultipleTablesSharingSameRendererDoNotCrossContaminate() =
    runBlocking {
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

      val sharedRenderer = LogCellRenderer()
      table1.table.setDefaultRenderer(LogEntry::class.java, sharedRenderer)
      table2.table.setDefaultRenderer(LogEntry::class.java, sharedRenderer)

      table1.searchContent(SearchableTable.SearchRequest("foo")).await()

      val filter1 = LogCellRenderer.getSearchFilter(table1.table)
      val filter2 = LogCellRenderer.getSearchFilter(table2.table)

      assertNotNull("Table 1 should have searchFilter", filter1)
      assertEquals("foo", filter1?.patternString)
      assertNull("Table 2 should NOT have searchFilter", filter2)
    }

  @Test
  fun testEnterAndShiftEnterNavigation() = runBlocking {
    val model = LogListTableModel("Test")
    val entries = (0 until 50).map { i ->
      val text = if (i % 10 == 0) "Target line $i" else "Line $i"
      LogEntry(text, LogLevel.VERBOSE, null)
    }
    model.setLogs(entries)
    val table = SearchableTable(model)

    table.searchContent(SearchableTable.SearchRequest("Target")).await()

    val enterEvent = KeyEvent(
      table.searchText,
      KeyEvent.KEY_PRESSED,
      System.currentTimeMillis(),
      0,
      KeyEvent.VK_ENTER,
      KeyEvent.CHAR_UNDEFINED
    )
    table.searchText.keyListeners.forEach { it.keyPressed(enterEvent) }
    delay(100)
    assertEquals(0, LogCellRenderer.getHighlightLine(table.table))

    table.searchText.keyListeners.forEach { it.keyPressed(enterEvent) }
    delay(100)
    assertEquals(10, LogCellRenderer.getHighlightLine(table.table))

    val shiftEnterEvent = KeyEvent(
      table.searchText,
      KeyEvent.KEY_PRESSED,
      System.currentTimeMillis(),
      KeyEvent.SHIFT_DOWN_MASK,
      KeyEvent.VK_ENTER,
      KeyEvent.CHAR_UNDEFINED
    )
    table.searchText.keyListeners.forEach { it.keyPressed(shiftEnterEvent) }
    delay(100)
    assertEquals(0, LogCellRenderer.getHighlightLine(table.table))

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

    val comp = myLogsRenderer.getTableCellRendererComponent(
      table.table, entry, false, false, 0, 0
    )
    val renderer = comp as LogCellRenderer
    val field = LogCellRenderer::class.java.getDeclaredField("textView")
    field.isAccessible = true
    val textView = field.get(renderer) as javax.swing.JTextArea
    val highlights = textView.highlighter.highlights
    assertTrue("Highlighter should contain highlights", highlights.isNotEmpty())
    assertEquals(6, highlights[0].startOffset)
    assertEquals(11, highlights[0].endOffset)
  }

  @Test
  fun testSearchButtonsInstantiatedAndTyped() {
    val table = SearchableTable(DefaultTableModel())
    assertEquals(FlatButton::class.java, table.searchLast.javaClass)
    assertEquals(
      ToolbarDivider::class.java,
      table.searchNavDivider.javaClass
    )
    assertEquals(FlatButton::class.java, table.searchNext.javaClass)
    assertEquals(FlatButton::class.java, table.close.javaClass)
    assertEquals(
      StatusBadge::class.java,
      table.searchResult.javaClass
    )

    // Verify icons are present and properly sized
    assertNotNull(table.searchLast.icon)
    assertEquals(16, table.searchLast.icon.iconWidth)
    assertEquals(16, table.searchLast.icon.iconHeight)

    assertNotNull(table.searchNext.icon)
    assertEquals(16, table.searchNext.icon.iconWidth)
    assertEquals(16, table.searchNext.icon.iconHeight)

    assertNotNull(table.close.icon)
    assertEquals(16, table.close.icon.iconWidth)
    assertEquals(16, table.close.icon.iconHeight)

    // Verify tooltips
    assertEquals(
      I18n.get(I18n.SEARCH_PREV_MATCH),
      table.searchLast.toolTipText
    )
    assertEquals(
      I18n.get(I18n.SEARCH_NEXT_MATCH),
      table.searchNext.toolTipText
    )
    assertEquals(
      I18n.get(I18n.SEARCH_TOOLTIP_HIDE),
      table.close.toolTipText
    )
    // Verify FlatLaf toolbar button client property
    val expectedType = FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON
    assertEquals(
      expectedType,
      table.searchLast.getClientProperty(FlatClientProperties.BUTTON_TYPE)
    )
    assertEquals(
      expectedType,
      table.searchNext.getClientProperty(FlatClientProperties.BUTTON_TYPE)
    )
    assertEquals(
      expectedType,
      table.close.getClientProperty(FlatClientProperties.BUTTON_TYPE)
    )
  }

  /**
   * Tests that the search bar components follow the modern IDE order:
   * searchText, searchResult badge, searchLast, searchNext, divider,
   * matchCase, and close button, with no redundant clear button.
   */
  @Test
  fun testSearchBarComponentsOrder() {
    val table = SearchableTable(DefaultTableModel())
    val panel = table.searchOptionPanel
    val components = panel.components.toList()

    assertTrue(components.contains(table.searchText))
    assertTrue(components.contains(table.searchResult))
    assertTrue(components.contains(table.searchLast))
    assertTrue(components.contains(table.searchNext))
    assertTrue(components.contains(table.searchNavDivider))
    assertTrue(components.contains(table.close))

    val searchTextIdx = components.indexOf(table.searchText)
    val searchResultIdx = components.indexOf(table.searchResult)
    val searchLastIdx = components.indexOf(table.searchLast)
    val searchNextIdx = components.indexOf(table.searchNext)
    val searchDividerIdx = components.indexOf(table.searchNavDivider)
    val closeIdx = components.indexOf(table.close)

    assertTrue(searchTextIdx < searchResultIdx)
    assertTrue(searchResultIdx < searchLastIdx)
    assertTrue(searchLastIdx < searchNextIdx)
    assertTrue(searchNextIdx < searchDividerIdx)
    assertTrue(searchDividerIdx < closeIdx)
  }

  @Test
  fun testSearchResultBadgeStateTransitions() = runBlocking {
    val model = LogListTableModel("Test")
    val entries = (0 until 50).map { i ->
      val text = if (i % 10 == 0) "Target line $i" else "Line $i"
      LogEntry(text, LogLevel.VERBOSE, null)
    }
    model.setLogs(entries)
    val table = SearchableTable(model)

    // Initially hidden
    assertFalse(table.searchResult.isVisible)
    assertEquals("", table.searchResult.text)

    // 1. Matches found (5 matches)
    table.searchContent(SearchableTable.SearchRequest("Target")).await()
    assertTrue(table.searchResult.isVisible)
    assertEquals(
      I18n.format(I18n.SEARCH_RESULTS_COUNT, 5),
      table.searchResult.text
    )
    assertEquals(
      StatusBadge.Variant.NORMAL,
      table.searchResult.variant
    )

    // 2. Navigate match via Enter (1 of 5)
    val enterEvent = KeyEvent(
      table.searchText,
      KeyEvent.KEY_PRESSED,
      System.currentTimeMillis(),
      0,
      KeyEvent.VK_ENTER,
      KeyEvent.CHAR_UNDEFINED
    )
    table.searchText.keyListeners.forEach { it.keyPressed(enterEvent) }
    delay(100)
    assertTrue(table.searchResult.isVisible)
    assertEquals("1/5", table.searchResult.text)
    assertEquals(
      StatusBadge.Variant.NORMAL,
      table.searchResult.variant
    )

    // 3. 0 matches found
    table.searchContent(
      SearchableTable.SearchRequest("NonExistent")
    ).await()
    assertTrue(table.searchResult.isVisible)
    assertEquals(
      I18n.format(I18n.SEARCH_RESULTS_COUNT, 0),
      table.searchResult.text
    )
    assertEquals(
      StatusBadge.Variant.NO_RESULTS,
      table.searchResult.variant
    )

    // 4. Invalid regex pattern
    table.searchContent(SearchableTable.SearchRequest("[")).await()
    assertTrue(table.searchResult.isVisible)
    assertEquals(
      I18n.get(I18n.SEARCH_BAD_PATTERN),
      table.searchResult.text
    )
    assertEquals(
      StatusBadge.Variant.WARNING,
      table.searchResult.variant
    )

    // 5. Blank pattern clears badge
    table.searchContent(SearchableTable.SearchRequest("")).await()
    assertFalse(table.searchResult.isVisible)
    assertEquals("", table.searchResult.text)
  }

  @Test
  fun testOnThemeChangedUpdatesIconsAndBorders() {
    val table = SearchableTable(DefaultTableModel())
    table.onThemeChanged()

    assertNotNull(table.searchLast.icon)
    assertEquals(16, table.searchLast.icon.iconWidth)
    assertEquals(16, table.searchLast.icon.iconHeight)

    assertNotNull(table.searchNext.icon)
    assertEquals(16, table.searchNext.icon.iconWidth)
    assertEquals(16, table.searchNext.icon.iconHeight)

    assertNotNull(table.close.icon)
    assertEquals(16, table.close.icon.iconWidth)
    assertEquals(16, table.close.icon.iconHeight)

    assertTrue(table.searchOptionPanel.border is CompoundBorder)
  }

  @Test
  fun testSearchOptionPanelHeadlessPainting() {
    val table = SearchableTable(DefaultTableModel())
    val panel = table.searchOptionPanel
    panel.setSize(300, 30)

    val image = BufferedImage(300, 30, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    try {
      panel.paint(g)
    } finally {
      g.dispose()
    }

    var hasPaintedPixel = false
    for (x in 0 until 300) {
      for (y in 0 until 30) {
        if (((image.getRGB(x, y) ushr 24) and 0xFF) > 0) {
          hasPaintedPixel = true
          break
        }
      }
      if (hasPaintedPixel) break
    }
    assertTrue("Search option panel should paint in headless", hasPaintedPixel)
  }

  /**
   * Tests that scrollPane has empty borders on both border and viewportBorder
   * to ensure an edge-to-edge presentation connecting cleanly to splitters.
   */
  @Test
  fun testScrollPaneBorderSetup() {
    val table = SearchableTable(DefaultTableModel())

    assertNotNull(table.scrollPane.border)
    assertNotNull(table.scrollPane.viewportBorder)

    val borderInsets = table.scrollPane.border.getBorderInsets(table.scrollPane)
    assertEquals(0, borderInsets.top)
    assertEquals(0, borderInsets.left)
    assertEquals(0, borderInsets.bottom)
    assertEquals(0, borderInsets.right)

    val viewportInsets =
      table.scrollPane.viewportBorder.getBorderInsets(table.scrollPane)
    assertEquals(0, viewportInsets.top)
    assertEquals(0, viewportInsets.left)
    assertEquals(0, viewportInsets.bottom)
    assertEquals(0, viewportInsets.right)
  }

  /**
   * Tests that searchOptionPanel is styled with a docked find bar border:
   * top matte border separating it from the content above, and empty padding.
   */
  @Test
  fun testSearchOptionPanelBorderSetup() {
    val table = SearchableTable(DefaultTableModel())
    val border = table.searchOptionPanel.border
    assertTrue(
      "Border should be a CompoundBorder",
      border is CompoundBorder
    )
    val compoundBorder = border as CompoundBorder

    assertTrue(
      "Outside border should be a MatteBorder",
      compoundBorder.outsideBorder is MatteBorder
    )
    val matteBorder = compoundBorder.outsideBorder as MatteBorder
    val matteInsets = matteBorder.getBorderInsets(table.searchOptionPanel)
    assertEquals(UIScaleUtils.dip(1), matteInsets.top)
    assertEquals(0, matteInsets.left)
    assertEquals(0, matteInsets.bottom)
    assertEquals(0, matteInsets.right)

    assertTrue(
      "Inside border should be an EmptyBorder",
      compoundBorder.insideBorder is EmptyBorder
    )
    val emptyInsets =
      compoundBorder.insideBorder.getBorderInsets(table.searchOptionPanel)
    assertEquals(UIScaleUtils.dip(4), emptyInsets.top)
    assertEquals(UIScaleUtils.dip(6), emptyInsets.left)
    assertEquals(UIScaleUtils.dip(4), emptyInsets.bottom)
    assertEquals(UIScaleUtils.dip(6), emptyInsets.right)
  }

  /**
   * Tests that the search bar input has the native clear button enabled
   * and action controls have a hand cursor configured.
   */
  @Test
  fun testSearchBarModernControls() {
    val table = SearchableTable(DefaultTableModel())

    val showClear = table.searchText.getClientProperty(
      FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON
    )
    assertEquals(true, showClear)

    val handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    assertEquals(handCursor, table.searchLast.cursor)
    assertEquals(handCursor, table.searchNext.cursor)
    assertEquals(handCursor, table.close.cursor)
    assertEquals(handCursor, table.matchCaseOption.cursor)
  }
}
