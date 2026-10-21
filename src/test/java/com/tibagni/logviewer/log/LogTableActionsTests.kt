package com.tibagni.logviewer.log

import com.tibagni.logviewer.filter.FiltersPanel
import com.tibagni.logviewer.i18n.I18n
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JSeparator
import javax.swing.JTable

class LogTableActionsTests {

  @Test
  fun testDoubleClickTriggersCallback() {
    val model = LogListTableModel("Test")
    val entry0 = LogEntry("Line 0", LogLevel.INFO, null, LogStream.MAIN)
    val entry1 = LogEntry("Line 1", LogLevel.DEBUG, null, LogStream.MAIN)
    model.setLogs(listOf(entry0, entry1))

    val table = JTable(model)
    var doubleClickedIndex = -1
    var doubleClickedEntry: LogEntry? = null

    table.setupLogTableActions(
      model = model,
      onDoubleClick = { index, entry ->
        doubleClickedIndex = index
        doubleClickedEntry = entry
      }
    )

    table.setRowSelectionInterval(1, 1)
    val listener = table.mouseListeners.last()
    val event = MouseEvent(
      table,
      MouseEvent.MOUSE_CLICKED,
      System.currentTimeMillis(),
      0,
      10,
      10,
      2,
      false,
      MouseEvent.BUTTON1
    )
    listener.mouseClicked(event)

    assertEquals(1, doubleClickedIndex)
    assertEquals(entry1, doubleClickedEntry)
  }

  @Test
  fun testRightClickAutoSelectsUnselectedRow() {
    val model = LogListTableModel("Test")
    val entries = (0..4).map { LogEntry("Line $it", LogLevel.INFO, null, LogStream.MAIN) }
    model.setLogs(entries)

    val table = JTable(model)
    table.rowHeight = 20
    table.setSize(500, 100)

    table.setupLogTableActions(model = model)

    table.setRowSelectionInterval(0, 0)
    assertEquals(0, table.selectedRow)

    val listener = table.mouseListeners.last()
    val rightClickRow2 = MouseEvent(
      table,
      MouseEvent.MOUSE_CLICKED,
      System.currentTimeMillis(),
      0,
      10,
      45, // y coordinate in row 2 (since rowHeight is 20)
      1,
      false,
      MouseEvent.BUTTON3
    )
    listener.mouseClicked(rightClickRow2)

    assertEquals(2, table.selectedRow)
  }

  @Test
  fun testRightClickOnAlreadySelectedRowKeepsMultiSelection() {
    val model = LogListTableModel("Test")
    val entries = (0..4).map { LogEntry("Line $it", LogLevel.INFO, null, LogStream.MAIN) }
    model.setLogs(entries)

    val table = JTable(model)
    table.rowHeight = 20
    table.setSize(500, 100)

    table.setupLogTableActions(model = model)

    table.setRowSelectionInterval(1, 3)
    assertArrayEquals(intArrayOf(1, 2, 3), table.selectedRows)

    val listener = table.mouseListeners.last()
    val rightClickRow2 = MouseEvent(
      table,
      MouseEvent.MOUSE_CLICKED,
      System.currentTimeMillis(),
      0,
      10,
      45, // in row 2
      1,
      false,
      MouseEvent.BUTTON3
    )
    listener.mouseClicked(rightClickRow2)

    assertArrayEquals(intArrayOf(1, 2, 3), table.selectedRows)
  }

  @Test
  fun testCreatePopupMenuSingleEntryWithPidAndTag() {
    val model = LogListTableModel("Test")
    val entry = LogEntry(
      "10-12 22:33:46.839 3172 3172 V KeyguardStatusView: refresh statusview",
      LogLevel.VERBOSE,
      null,
      LogStream.MAIN
    )
    model.setLogs(listOf(entry))

    val table = JTable(model)
    table.setRowSelectionInterval(0, 0)

    val mockHost = mock(com.tibagni.logviewer.filter.FiltersPanelHost::class.java)
    val mockPresenter = mock(com.tibagni.logviewer.filter.FiltersPresenter::class.java)
    val filtersPanel = FiltersPanel(mockHost) { mockPresenter }

    val popup = createPopupMenu(
      table = table,
      model = model,
      filtersPanelProvider = { filtersPanel }
    )

    val menuTexts = (0 until popup.componentCount)
      .mapNotNull { (popup.getComponent(it) as? JMenuItem)?.text }

    assertTrue("Menu should contain Log Details", menuTexts.contains(I18n.get(I18n.LOGS_MENU_LOG_DETAILS)))
    val expectedPidText = I18n.format(I18n.LOGS_MENU_FILTER_BY_PID, "3172")
    assertTrue("Menu should contain Filter by PID", menuTexts.contains(expectedPidText))
    val expectedTagText = I18n.format(I18n.LOGS_MENU_FILTER_BY_TAG, "KeyguardStatusView")
    assertTrue("Menu should contain Filter by Tag", menuTexts.contains(expectedTagText))
    assertTrue("Menu should contain Create Filter", menuTexts.contains(I18n.get(I18n.LOGS_MENU_CREATE_FILTER)))

    // Test actions trigger filtersPanel methods
    val pidItem = (0 until popup.componentCount).mapNotNull { popup.getComponent(it) as? JMenuItem }
      .first { it.text == expectedPidText }
    pidItem.actionListeners.forEach { it.actionPerformed(mock(java.awt.event.ActionEvent::class.java)) }
    verify(mockPresenter).addFilterForPid(3172)

    val tagItem = (0 until popup.componentCount).mapNotNull { popup.getComponent(it) as? JMenuItem }
      .first { it.text == expectedTagText }
    tagItem.actionListeners.forEach { it.actionPerformed(mock(java.awt.event.ActionEvent::class.java)) }
    verify(mockPresenter).addFilterForTag("KeyguardStatusView")

    val createItem = (0 until popup.componentCount).mapNotNull { popup.getComponent(it) as? JMenuItem }
      .first { it.text == I18n.get(I18n.LOGS_MENU_CREATE_FILTER) }
    assertTrue(createItem.actionListeners.isNotEmpty())
  }

  @Test
  fun testCreatePopupMenuSingleEntryWithoutPidOrTag() {
    val model = LogListTableModel("Test")
    val entry = LogEntry(
      "Unformatted text without PID or tag",
      LogLevel.DEBUG,
      null,
      LogStream.MAIN
    )
    model.setLogs(listOf(entry))

    val table = JTable(model)
    table.setRowSelectionInterval(0, 0)

    val mockHost = mock(com.tibagni.logviewer.filter.FiltersPanelHost::class.java)
    val mockPresenter = mock(com.tibagni.logviewer.filter.FiltersPresenter::class.java)
    val filtersPanel = FiltersPanel(mockHost) { mockPresenter }

    val popup = createPopupMenu(
      table = table,
      model = model,
      filtersPanelProvider = { filtersPanel }
    )

    val menuTexts = (0 until popup.componentCount)
      .mapNotNull { (popup.getComponent(it) as? JMenuItem)?.text }

    assertTrue("Menu should contain Log Details", menuTexts.contains(I18n.get(I18n.LOGS_MENU_LOG_DETAILS)))
    assertTrue("Menu should contain Create Filter", menuTexts.contains(I18n.get(I18n.LOGS_MENU_CREATE_FILTER)))
    assertFalse("Menu should not contain Filter by PID", menuTexts.any { it.startsWith("Filter by PID") })
    assertFalse("Menu should not contain Filter by Tag", menuTexts.any { it.startsWith("Filter by Tag") })
  }

  @Test
  fun testCreatePopupMenuMultipleEntries() {
    val model = LogListTableModel("Test")
    val entry0 = LogEntry("Line 0", LogLevel.INFO, null, LogStream.MAIN)
    val entry1 = LogEntry("Line 1", LogLevel.DEBUG, null, LogStream.MAIN)
    model.setLogs(listOf(entry0, entry1))

    val table = JTable(model)
    table.setRowSelectionInterval(0, 1)

    var selectionActionsInvokedWith: List<LogEntry>? = null

    val popup = createPopupMenu(
      table = table,
      model = model,
      addSelectionActions = { p, entries ->
        selectionActionsInvokedWith = entries
        p.add(JMenuItem("Custom Multi Action"))
      }
    )

    assertEquals(listOf(entry0, entry1), selectionActionsInvokedWith)

    val menuTexts = (0 until popup.componentCount)
      .mapNotNull { (popup.getComponent(it) as? JMenuItem)?.text }

    assertTrue(menuTexts.contains("Custom Multi Action"))
    assertFalse("Multi-selection should not have Log Details", menuTexts.contains(I18n.get(I18n.LOGS_MENU_LOG_DETAILS)))
    assertFalse("Multi-selection should not have Create Filter", menuTexts.contains(I18n.get(I18n.LOGS_MENU_CREATE_FILTER)))
  }

  @Test
  fun testExtraSingleEntryActionsAppended() {
    val model = LogListTableModel("Test")
    val entry = LogEntry("Line 0", LogLevel.INFO, null, LogStream.MAIN)
    model.setLogs(listOf(entry))

    val table = JTable(model)
    table.setRowSelectionInterval(0, 0)

    val popup = createPopupMenu(
      table = table,
      model = model,
      addExtraSingleEntryActions = { p, _ ->
        p.add(JMenuItem("Extra Single Action"))
      }
    )

    val menuTexts = (0 until popup.componentCount)
      .mapNotNull { (popup.getComponent(it) as? JMenuItem)?.text }

    assertTrue(menuTexts.contains("Extra Single Action"))
  }

  @Test
  fun testCreatePopupMenuTwoEntriesWithTimestampsAddsElapsedTimeMenu() {
    val model = LogListTableModel("Test")
    val t1 = LogTimestamp(5, 15, 14, 15, 0, 50)
    val t2 = LogTimestamp(5, 15, 14, 16, 23, 500)
    val entry0 = LogEntry("05-15 14:15:00.050 Line 0", LogLevel.INFO, t1, LogStream.MAIN)
    val entry1 = LogEntry("05-15 14:16:23.500 Line 1", LogLevel.DEBUG, t2, LogStream.MAIN)
    model.setLogs(listOf(entry0, entry1))

    val table = JTable(model)
    table.setRowSelectionInterval(0, 1)

    val popup = createPopupMenu(
      table = table,
      model = model
    )

    // Find the Elapsed Time menu (which is a JMenu)
    val elapsedMenu = (0 until popup.componentCount)
      .mapNotNull { popup.getComponent(it) as? javax.swing.JMenu }
      .firstOrNull { it.text.contains("1m 23s 450ms") }

    assertNotNull("Popup should contain Elapsed Time menu", elapsedMenu)
    assertEquals(2, elapsedMenu!!.itemCount)

    val copySnippetItem = elapsedMenu.getItem(0)
    assertEquals(I18n.get(I18n.LOGS_MENU_COPY_ELAPSED_TIME_SNIPPET), copySnippetItem.text)
    assertTrue(copySnippetItem.actionListeners.isNotEmpty())

    val copyDurationOnlyItem = elapsedMenu.getItem(1)
    assertEquals(I18n.get(I18n.LOGS_MENU_COPY_DURATION_ONLY), copyDurationOnlyItem.text)
    assertTrue(copyDurationOnlyItem.actionListeners.isNotEmpty())
  }

  @Test
  fun testCreatePopupMenuTwoEntriesWithoutTimestampsDoesNotAddElapsedTimeMenu() {
    val model = LogListTableModel("Test")
    val entry0 = LogEntry("Line 0", LogLevel.INFO, null, LogStream.MAIN)
    val entry1 = LogEntry("Line 1", LogLevel.DEBUG, null, LogStream.MAIN)
    model.setLogs(listOf(entry0, entry1))

    val table = JTable(model)
    table.setRowSelectionInterval(0, 1)

    val popup = createPopupMenu(
      table = table,
      model = model
    )

    val elapsedMenu = (0 until popup.componentCount)
      .mapNotNull { popup.getComponent(it) as? javax.swing.JMenu }
      .firstOrNull()

    assertNull("Popup should not contain Elapsed Time menu when timestamps are null", elapsedMenu)
  }
}
