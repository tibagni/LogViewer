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
import com.tibagni.logviewer.adb.AdbDevice
import com.tibagni.logviewer.adb.DeviceState
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.JSplitPane

class LiveLogViewTests {

  private val testDevice = AdbDevice(
    serialNumber = "device_123",
    state = DeviceState.ONLINE,
    product = "pixel",
    model = "Pixel_7",
    device = "panther",
    transportId = "1"
  )

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
    assertTrue(SwingUtilities.isDescendingFrom(filtersPanel, liveLogView.contentPane))
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
    assertTrue(liveLogView.isAutoScrollActive)

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

    assertFalse(liveLogView.isAutoScrollActive)
  }

  @Test
  fun testAutoScrollDisabledOnMouseWheel() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    assertTrue(liveLogView.isAutoScrollActive)

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
    assertTrue(liveLogView.isAutoScrollActive)

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
    assertFalse(liveLogView.isAutoScrollActive)
  }

  @Test
  fun testAutoScrollDisabledOnKeyboardNavigation() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    assertTrue(liveLogView.isAutoScrollActive)

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

    assertFalse(liveLogView.isAutoScrollActive)
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
    liveLogView.isAutoScrollActive = false
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
    liveLogView.isAutoScrollActive = true

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

    assertFalse("Auto scroll should be disabled on double click", liveLogView.isAutoScrollActive)
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

  @Test
  fun testOnTabSelectedLoadsDevicesWhenNotStreaming() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    `when`(mockPresenter.isStreaming).thenReturn(false)

    liveLogView.onTabSelected()

    verify(mockPresenter).loadDevices()
  }

  @Test
  fun testOnTabSelectedDoesNotLoadDevicesWhenStreaming() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    `when`(mockPresenter.isStreaming).thenReturn(true)

    liveLogView.onTabSelected()

    verify(mockPresenter, never()).loadDevices()
  }

  @Test
  fun testShowDeviceScanningShowsScanningPanel() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    liveLogView.showDeviceScanning()

    assertNotNull(liveLogView.contentPane)
  }

  @Test
  fun testShowNoDevicesShowsEmptyState() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    liveLogView.showNoDevices()

    assertNotNull(liveLogView.contentPane)
  }

  @Test
  fun testShowAdbNotAvailableShowsCard() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    liveLogView.showAdbNotAvailable()

    assertNotNull(liveLogView.contentPane)
  }

  @Test
  fun testShowDeviceListPopulatesDevices() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    liveLogView.showDeviceList(listOf(testDevice))

    assertNotNull(liveLogView.contentPane)
    assertEquals(1, liveLogView.deviceItemsContainer.componentCount)

    // Verify width is bounded to 700dp
    val prefSize = liveLogView.deviceItemsContainer.preferredSize
    assertEquals(UIScaleUtils.dip(700), prefSize.width)

    val itemCard = liveLogView.deviceItemsContainer.getComponent(0) as JPanel
    val allButtons = (itemCard as java.awt.Container).components
      .flatMap { (it as? java.awt.Container)?.components?.toList() ?: listOf(it) }
      .filterIsInstance<JButton>()

    val startBtn = allButtons.first { it.text == "Start" }
    val termBtn = allButtons.first { it.text == "Terminal" }

    startBtn.doClick()
    verify(mockPresenter).startStreaming(testDevice)

    termBtn.doClick()
    verify(mockPresenter).openDeviceTerminal(testDevice)

    // Double-click on card triggers startStreaming
    val doubleClickEvent = MouseEvent(
      itemCard,
      MouseEvent.MOUSE_CLICKED,
      System.currentTimeMillis(),
      0,
      10, 10,
      2, false,
      MouseEvent.BUTTON1
    )
    for (listener in itemCard.mouseListeners) {
      listener.mouseClicked(doubleClickEvent)
    }
    verify(mockPresenter, times(2)).startStreaming(testDevice)

    // Verify hover border transitions
    val enterEvent = MouseEvent(itemCard, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 10, 10, 0, false)
    for (listener in itemCard.mouseListeners) {
      listener.mouseEntered(enterEvent)
    }

    val exitEvent = MouseEvent(itemCard, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, -10, -10, 0, false)
    for (listener in itemCard.mouseListeners) {
      listener.mouseExited(exitEvent)
    }

    // Verify onThemeChanged refreshes device list
    liveLogView.onThemeChanged()
    assertEquals(1, liveLogView.deviceItemsContainer.componentCount)
  }

  @Test
  fun testShowLogsViewAndDeviceScanning() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    liveLogView.showLogsView(testDevice)
    liveLogView.showDeviceScanning()

    assertNotNull(liveLogView.contentPane)
  }

  @Test
  fun testSidePanelButtonsWiredToPresenter() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    `when`(mockPresenter.connectedDevice).thenReturn(testDevice)

    // Stop/Resume button when streaming
    `when`(mockPresenter.isStreaming).thenReturn(true)
    liveLogView.showStreamingStatus("Streaming", true)
    assertTrue(liveLogView.sidePanel.isItemEnabled(LiveLogViewImpl.ID_STOP_RESUME))
    liveLogView.sidePanel.performAction(LiveLogViewImpl.ID_STOP_RESUME)
    verify(mockPresenter).stopStreaming()

    // Stop/Resume button when stopped
    `when`(mockPresenter.isStreaming).thenReturn(false)
    liveLogView.showStreamingStatus("Stopped", false)
    assertTrue(liveLogView.sidePanel.isItemEnabled(LiveLogViewImpl.ID_STOP_RESUME))
    liveLogView.sidePanel.performAction(LiveLogViewImpl.ID_STOP_RESUME)
    verify(mockPresenter).startStreaming(testDevice)

    // Clear logs button
    liveLogView.sidePanel.performAction(LiveLogViewImpl.ID_CLEAR)
    verify(mockPresenter).clearLogs()

    // Terminal button
    liveLogView.sidePanel.performAction(LiveLogViewImpl.ID_TERMINAL)
    verify(mockPresenter).openDeviceTerminal(testDevice)

    // Change device button
    liveLogView.sidePanel.performAction(LiveLogViewImpl.ID_CHANGE_DEVICE)
    verify(mockPresenter).disconnectDevice()
  }

  @Test
  fun testAutoScrollButtonTogglesState() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    assertTrue(liveLogView.isAutoScrollActive)

    // Toggle off
    liveLogView.sidePanel.performAction(LiveLogViewImpl.ID_AUTOSCROLL)
    assertFalse(liveLogView.isAutoScrollActive)

    // Toggle back on
    liveLogView.sidePanel.performAction(LiveLogViewImpl.ID_AUTOSCROLL)
    assertTrue(liveLogView.isAutoScrollActive)
  }

  @Test
  fun testSidePanelExpandCollapseToggle() {
    val liveLogView = LiveLogViewImpl { mockPresenter }

    assertFalse(liveLogView.isSidePanelExpanded)

    // Expand
    liveLogView.sidePanel.toggleExpandBtn?.doClick()
    assertTrue(liveLogView.isSidePanelExpanded)

    // Collapse
    liveLogView.sidePanel.toggleExpandBtn?.doClick()
    assertFalse(liveLogView.isSidePanelExpanded)
  }
}
