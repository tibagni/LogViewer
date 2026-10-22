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
import com.tibagni.logviewer.filter.FiltersView
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
import com.tibagni.logviewer.view.FlatButton
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.JSplitPane

import com.tibagni.logviewer.filter.FilterContextTag

class LiveLogViewTests {

  companion object {
    private val testTag = FilterContextTag.register("test_live_log_view_tag")
    private val otherTag = FilterContextTag.register("test_other_tag")
  }

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
    `when`(mockPresenter.filterContextTag).thenReturn(testTag)
    `when`(mockPresenter.availableBuffers).thenReturn(listOf("main", "system", "events", "crash", "radio", "kernel"))
    `when`(mockPresenter.defaultBuffers).thenReturn(listOf("main", "system", "events", "crash"))
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
    verify(mockPresenter).startStreaming(testDevice, listOf("main", "system", "events", "crash"))

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
    verify(mockPresenter, times(2)).startStreaming(testDevice, listOf("main", "system", "events", "crash"))

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

  private class TestFiltersPanel(host: FiltersPanelHost, presenterFactory: (FiltersView) -> FiltersPresenter) :
    FiltersPanel(host, presenterFactory) {
    var updateCount = 0
    override fun updateFiltersUI() {
      updateCount++
    }
  }

  @Test
  fun testAttachFiltersPanelCallsUpdateFiltersUI() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    val testPanel = TestFiltersPanel(mockHost) { mockFiltersPresenter }

    liveLogView.attachFiltersPanel(testPanel)

    assertEquals(1, testPanel.updateCount)
    assertEquals(testTag, testPanel.activeFilterContextTag)
  }

  @Test
  fun testShowLogsBatchUpdatesFiltersUIInLiveLogsContext() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    val testPanel = TestFiltersPanel(mockHost) { mockFiltersPresenter }
    liveLogView.attachFiltersPanel(testPanel)
    testPanel.updateCount = 0

    val entry = LogEntry("line 1", LogLevel.INFO, null)
    val batch = LiveLogBatch(
      newAllLogs = listOf(entry),
      newFilteredLogs = listOf(entry),
      evictedAllCount = 0,
      evictedFilteredCount = 0
    )

    liveLogView.showLogsBatch(batch)

    assertEquals(1, testPanel.updateCount)

    // When panel is switched to another context tag, should NOT update UI for live batches
    testPanel.activeFilterContextTag = otherTag
    testPanel.updateCount = 0

    liveLogView.showLogsBatch(batch)

    assertEquals(0, testPanel.updateCount)
  }

  @Test
  fun testShowAllLogsUpdatesFiltersUIInLiveLogsContext() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    val testPanel = TestFiltersPanel(mockHost) { mockFiltersPresenter }
    liveLogView.attachFiltersPanel(testPanel)
    testPanel.updateCount = 0

    liveLogView.showAllLogs(emptyList(), emptyList())
    assertEquals(1, testPanel.updateCount)

    testPanel.activeFilterContextTag = otherTag
    testPanel.updateCount = 0
    liveLogView.showAllLogs(emptyList(), emptyList())
    assertEquals(0, testPanel.updateCount)
  }

  @Test
  fun testBufferCheckboxesInitializedAndToggled() {
    val liveLogView = LiveLogViewImpl { mockPresenter }

    for (buf in listOf("main", "system", "events", "crash")) {
      assertTrue("Buffer $buf should be checked by default", liveLogView.bufferCheckboxes[buf]?.isSelected == true)
    }
    assertFalse(liveLogView.bufferCheckboxes["radio"]?.isSelected == true)
    assertFalse(liveLogView.bufferCheckboxes["kernel"]?.isSelected == true)
    assertEquals(com.tibagni.logviewer.view.TriStateCheckbox.SelectionState.PARTIALLY_SELECTED, liveLogView.selectAllCb.selectionState)

    liveLogView.showDeviceList(listOf(testDevice))

    // Locate start button on card
    val itemCard = liveLogView.deviceItemsContainer.components.first { it is JPanel }
    val allButtons = (itemCard as java.awt.Container).components
      .flatMap { (it as? java.awt.Container)?.components?.toList() ?: listOf(it) }
      .filterIsInstance<JButton>()
    val startBtn = allButtons.first { it.text == "Start" }
    assertTrue(startBtn.isEnabled)
    assertNull(startBtn.toolTipText)

    // Click selectAllCb from partially selected -> selects all
    liveLogView.selectAllCb.doClick()
    assertEquals(com.tibagni.logviewer.view.TriStateCheckbox.SelectionState.SELECTED, liveLogView.selectAllCb.selectionState)
    assertTrue(liveLogView.bufferCheckboxes.values.all { it.isSelected })
    assertTrue(startBtn.isEnabled)

    // Click selectAllCb again -> unselects all
    liveLogView.selectAllCb.doClick()
    assertEquals(com.tibagni.logviewer.view.TriStateCheckbox.SelectionState.NOT_SELECTED, liveLogView.selectAllCb.selectionState)
    assertTrue(liveLogView.bufferCheckboxes.values.all { !it.isSelected })
    assertFalse(startBtn.isEnabled)
    assertNotNull(startBtn.toolTipText)

    // Recheck one checkbox manually
    liveLogView.bufferCheckboxes["main"]?.doClick()
    assertEquals(com.tibagni.logviewer.view.TriStateCheckbox.SelectionState.PARTIALLY_SELECTED, liveLogView.selectAllCb.selectionState)
    assertTrue(startBtn.isEnabled)
    assertNull(startBtn.toolTipText)

    // Click Start button and verify presenter called with selected buffer
    startBtn.doClick()
    verify(mockPresenter).startStreaming(testDevice, listOf("main"))
  }

  @Test
  fun testShowAvailableLogStreamsTogglesRendererAndCallsRefreshMenuBar() {
    val mockMainView = mock(com.tibagni.logviewer.MainView::class.java)
    val liveLogView = LiveLogViewImpl(mockMainView) { mockPresenter }

    // With 1 stream, streams indicator should not be shown
    liveLogView.showAvailableLogStreams(setOf(LogStream.MAIN))
    val layout = liveLogView.logRenderer.layout as java.awt.BorderLayout
    assertNull(layout.getLayoutComponent(java.awt.BorderLayout.LINE_END))
    verify(mockMainView).refreshMenuBar()

    // With 2+ streams, streams indicator should be shown
    liveLogView.showAvailableLogStreams(setOf(LogStream.MAIN, LogStream.SYSTEM))
    assertNotNull(layout.getLayoutComponent(java.awt.BorderLayout.LINE_END))
    verify(mockMainView, times(2)).refreshMenuBar()
  }

  @Test
  fun testBuildStreamsMenu() {
    val liveLogView = LiveLogViewImpl { mockPresenter }

    // When 0 or 1 streams available, menu is null
    `when`(mockPresenter.availableStreams).thenReturn(setOf(LogStream.MAIN))
    assertNull(liveLogView.buildStreamsMenu())

    // When 2 streams available, menu is created
    `when`(mockPresenter.availableStreams).thenReturn(setOf(LogStream.MAIN, LogStream.SYSTEM))
    `when`(mockPresenter.isStreamAllowed(LogStream.MAIN)).thenReturn(true)
    `when`(mockPresenter.isStreamAllowed(LogStream.SYSTEM)).thenReturn(false)

    val menu = liveLogView.buildStreamsMenu()
    assertNotNull(menu)
    assertEquals(2, menu!!.itemCount)

    val item0 = menu.getItem(0) as javax.swing.JCheckBoxMenuItem
    val item1 = menu.getItem(1) as javax.swing.JCheckBoxMenuItem

    assertEquals(LogStream.MAIN.toString(), item0.text)
    assertTrue(item0.state)
    assertEquals(LogStream.SYSTEM.toString(), item1.text)
    assertFalse(item1.state)

    // Toggling item triggers presenter
    item0.doClick()
    verify(mockPresenter).setStreamAllowed(LogStream.MAIN, false)
  }

  @Test
  fun testShowFilteredLogsUpdatesTableModel() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    val entry1 = LogEntry("line 1", LogLevel.INFO, null, LogStream.MAIN)
    val entry2 = LogEntry("line 2", LogLevel.DEBUG, null, LogStream.SYSTEM)

    liveLogView.showFilteredLogs(listOf(entry1, entry2))
    assertEquals(2, liveLogView.filteredLogListTableModel.rowCount)
    assertEquals(entry1, liveLogView.filteredLogListTableModel.getEntry(0))
    assertEquals(entry2, liveLogView.filteredLogListTableModel.getEntry(1))
  }

  @Test
  fun testGetMenusIncludesStreamsMenu() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    `when`(mockPresenter.availableStreams).thenReturn(setOf(LogStream.MAIN, LogStream.SYSTEM))

    val menus = liveLogView.getMenus()
    assertEquals(1, menus.size)
    assertEquals(2, menus[0].itemCount)
  }
  @Test
  fun testHeaderActionButtonsInLiveLogViewAreFlatButtons() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    assertEquals(
      FlatButton::class.java,
      liveLogView.refreshDevicesBtn.javaClass
    )
    assertEquals(
      FlatButton::class.java,
      liveLogView.restartServerBtn.javaClass
    )
  }
}
