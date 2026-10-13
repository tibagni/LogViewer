package com.tibagni.logviewer.live

import com.tibagni.logviewer.LogViewerPresenter
import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.filter.Filter
import com.tibagni.logviewer.filter.FiltersPanel
import com.tibagni.logviewer.filter.FiltersPanelHost
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.awt.Color
import com.tibagni.logviewer.filter.FiltersPresenter
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.log.LogStream
import java.awt.Frame
import java.awt.Point
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.io.File
import javax.swing.JFrame
import javax.swing.JSplitPane

class LiveLogViewTests {

  @Mock
  private lateinit var mockPresenter: LiveLogPresenter

  @Mock
  private lateinit var mockFiltersPresenter: FiltersPresenter

  @Mock
  private lateinit var mockHost: FiltersPanelHost

  @Mock
  private lateinit var mockFrame: Frame

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    `when`(mockHost.parentWindow).thenReturn(mockFrame)
  }

  @Test
  fun testAttachFiltersPanelReparentsPanel() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    val filtersPanel = FiltersPanel(mockHost) { mockFiltersPresenter }

    liveLogView.attachFiltersPanel(filtersPanel)

    // Verify panel parent is inside liveLogView contentPane
    assertSame(filtersPanel.parent?.parent, liveLogView.contentPane)
    assertNotNull(filtersPanel.listener)
  }

  @Test
  fun testLiveLogViewDelegatesStreaming() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    `when`(mockPresenter.isStreaming).thenReturn(true)
    assertTrue(liveLogView.isStreaming)

    val device = com.tibagni.logviewer.adb.AdbDevice("device123", com.tibagni.logviewer.adb.DeviceState.ONLINE)
    liveLogView.startStreaming(device)
    verify(mockPresenter).startStreaming(device)

    liveLogView.stopStreaming()
    verify(mockPresenter).stopStreaming()
  }

  @Test
  fun testAutoScrollDisabledOnMouseClick() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    assertTrue(liveLogView.autoScrollCb.isSelected)

    val mouseEvent = MouseEvent(
      liveLogView.logList.table,
      MouseEvent.MOUSE_PRESSED,
      System.currentTimeMillis(),
      0,
      10, 10,
      1, false
    )
    for (listener in liveLogView.logList.table.mouseListeners) {
      listener.mousePressed(mouseEvent)
    }

    assertFalse(liveLogView.autoScrollCb.isSelected)
  }

  @Test
  fun testAutoScrollDisabledOnMouseWheel() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    assertTrue(liveLogView.autoScrollCb.isSelected)

    val wheelDownEvent = MouseWheelEvent(
      liveLogView.logList.scrollPane,
      MouseEvent.MOUSE_WHEEL,
      System.currentTimeMillis(),
      0,
      10, 10,
      0, false,
      MouseWheelEvent.WHEEL_UNIT_SCROLL,
      3, 1
    )
    for (listener in liveLogView.logList.scrollPane.mouseWheelListeners) {
      listener.mouseWheelMoved(wheelDownEvent)
    }
    // Scrolling DOWN should NOT disable auto-scroll
    assertTrue(liveLogView.autoScrollCb.isSelected)

    val wheelUpEvent = MouseWheelEvent(
      liveLogView.logList.scrollPane,
      MouseEvent.MOUSE_WHEEL,
      System.currentTimeMillis(),
      0,
      10, 10,
      0, false,
      MouseWheelEvent.WHEEL_UNIT_SCROLL,
      3, -1
    )
    for (listener in liveLogView.logList.scrollPane.mouseWheelListeners) {
      listener.mouseWheelMoved(wheelUpEvent)
    }

    // Scrolling UP should disable auto-scroll
    assertFalse(liveLogView.autoScrollCb.isSelected)
  }

  @Test
  fun testAutoScrollDisabledOnKeyboardNavigation() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    assertTrue(liveLogView.autoScrollCb.isSelected)

    val keyEvent = KeyEvent(
      liveLogView.logList.table,
      KeyEvent.KEY_PRESSED,
      System.currentTimeMillis(),
      0,
      KeyEvent.VK_PAGE_UP,
      KeyEvent.CHAR_UNDEFINED
    )
    for (listener in liveLogView.logList.table.keyListeners) {
      listener.keyPressed(keyEvent)
    }

    assertFalse(liveLogView.autoScrollCb.isSelected)
  }

  @Test
  fun testViewportPositionCompensatedWhenLogsEvictedWithAutoScrollDisabled() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    val panel = liveLogView.contentPane
    panel.setSize(800, 600)
    panel.doLayout()
    liveLogView.logList.setSize(800, 300)
    liveLogView.logList.doLayout()
    liveLogView.logList.scrollPane.setSize(800, 300)
    liveLogView.logList.scrollPane.doLayout()

    val initialLogs = (1..50).map { LogEntry("Line $it", LogLevel.INFO, null, LogStream.MAIN) }
    liveLogView.showAllLogs(initialLogs, emptyList())

    // Turn auto-scroll OFF and position viewport at y = 200
    liveLogView.autoScrollCb.isSelected = false
    val viewport = liveLogView.logList.scrollPane.viewport
    viewport.viewPosition = Point(0, 200)

    // Evict 5 logs (evictedHeight = 5 * rowHeight)
    val rowHeight = liveLogView.logList.table.rowHeight
    val batchLogs = (51..55).map { LogEntry("Line $it", LogLevel.INFO, null, LogStream.MAIN) }
    liveLogView.showLogsBatch(LiveLogBatch(batchLogs, emptyList(), 5, 0))

    // Viewport position should be compensated: 200 - (5 * rowHeight)
    val expectedY = maxOf(0, 200 - (5 * rowHeight))
    assertEquals(expectedY, viewport.viewPosition.y)
  }

  @Test
  fun testFilteringProgressDisplay() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    assertFalse(liveLogView.filteringProgressBar.isVisible)

    // Initial progress <= 0 should make bar visible and indeterminate
    liveLogView.showFilteringProgress(0)
    assertTrue(liveLogView.filteringProgressBar.isVisible)
    assertTrue(liveLogView.filteringProgressBar.isIndeterminate)

    // Intermediate progress should set determinate value
    liveLogView.showFilteringProgress(45)
    assertTrue(liveLogView.filteringProgressBar.isVisible)
    assertFalse(liveLogView.filteringProgressBar.isIndeterminate)
    assertEquals(45, liveLogView.filteringProgressBar.value)

    // Finish progress should hide and reset bar
    liveLogView.finishFilteringProgress()
    assertFalse(liveLogView.filteringProgressBar.isVisible)
    assertFalse(liveLogView.filteringProgressBar.isIndeterminate)
    assertEquals(0, liveLogView.filteringProgressBar.value)
  }

  @Test
  fun testDoubleClickFilteredLogNavigatesToAllLogsAndDisablesAutoScroll() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    val entry0 = LogEntry("Line 0", LogLevel.INFO, null, LogStream.MAIN)
    val entry1 = LogEntry("Line 1", LogLevel.DEBUG, null, LogStream.MAIN)
    val entry2 = LogEntry("Line 2", LogLevel.ERROR, null, LogStream.MAIN)
    entry0.index = 0
    entry1.index = 1
    entry2.index = 2

    liveLogView.showAllLogs(listOf(entry0, entry1, entry2), listOf(entry1))
    liveLogView.autoScrollCb.isSelected = true

    // Double click the only entry in filteredLogList (index 0 which is entry1 with index 1)
    liveLogView.filteredLogList.table.setRowSelectionInterval(0, 0)
    val event = MouseEvent(
      liveLogView.filteredLogList.table,
      MouseEvent.MOUSE_CLICKED,
      System.currentTimeMillis(),
      0,
      10,
      10,
      2,
      false,
      MouseEvent.BUTTON1
    )
    liveLogView.filteredLogList.table.mouseListeners.forEach { it.mouseClicked(event) }

    assertFalse("Auto scroll should be disabled on double click", liveLogView.autoScrollCb.isSelected)
    assertEquals(1, liveLogView.logList.table.selectedRow)
  }

  @Test
  fun testDoubleClickLogWithFilterNavigatesToFilteredLog() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    val filter = Filter("Test", "Line 1", Color.RED, LogLevel.DEBUG)
    val entry0 = LogEntry("Line 0", LogLevel.INFO, null, LogStream.MAIN)
    val entry1 = LogEntry("Line 1", LogLevel.DEBUG, null, LogStream.MAIN)
    val entry2 = LogEntry("Line 2", LogLevel.ERROR, null, LogStream.MAIN)
    entry0.index = 0
    entry1.index = 1
    entry2.index = 2
    entry1.appliedFilter = filter

    liveLogView.showAllLogs(listOf(entry0, entry1, entry2), listOf(entry1))

    // Double click entry1 in logList
    liveLogView.logList.table.setRowSelectionInterval(1, 1)
    val event = MouseEvent(
      liveLogView.logList.table,
      MouseEvent.MOUSE_CLICKED,
      System.currentTimeMillis(),
      0,
      10,
      10,
      2,
      false,
      MouseEvent.BUTTON1
    )
    liveLogView.logList.table.mouseListeners.forEach { it.mouseClicked(event) }

    assertEquals(0, liveLogView.filteredLogList.table.selectedRow)
  }
}
