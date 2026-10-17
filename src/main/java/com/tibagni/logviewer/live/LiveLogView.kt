package com.tibagni.logviewer.live

import com.tibagni.logviewer.MainView
import com.tibagni.logviewer.MainViewTab
import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.View
import com.tibagni.logviewer.adb.AdbDevice
import com.tibagni.logviewer.adb.DeviceState
import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.log.LogCellRenderer
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogListTableModel
import com.tibagni.logviewer.log.LogStream
import com.tibagni.logviewer.log.setupLogTableActions
import com.tibagni.logviewer.preferences.LogViewerPreferences
import com.tibagni.logviewer.preferences.LogViewerPreferencesDialog
import com.tibagni.logviewer.util.LogFontManager
import com.tibagni.logviewer.util.StringUtils
import com.tibagni.logviewer.util.SwingUtils
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.layout.WrapLayout
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.filter.Filter
import com.tibagni.logviewer.filter.FiltersPanel
import com.tibagni.logviewer.filter.FiltersPanelListener
import com.tibagni.logviewer.view.CommonIcons
import com.tibagni.logviewer.view.SearchableTable
import com.tibagni.logviewer.view.SideButtonsPanel
import com.tibagni.logviewer.view.SidePanelEntry
import com.tibagni.logviewer.view.Toast
import com.tibagni.logviewer.view.TriStateCheckbox
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.AdjustmentListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelListener
import javax.swing.*
import javax.swing.border.EmptyBorder

interface LiveLogView : MainViewTab {
  val isStreaming: Boolean
  fun startStreaming(device: AdbDevice)
  fun stopStreaming()
  fun buildStreamsMenu(): JMenu?
}

class LiveLogViewImpl(
  private val mainView: MainView? = null,
  // Allows tests to inject a mock presenter
  @VisibleForTesting
  presenterFactory: ((LiveLogPresenterView) -> LiveLogPresenter)? = null
) : LiveLogView, LiveLogPresenterView {

  @VisibleForTesting
  constructor(presenterFactory: (LiveLogPresenterView) -> LiveLogPresenter) : this(null, presenterFactory)

  private val rootCardLayout = CardLayout()
  override val contentPane: JPanel = JPanel(rootCardLayout)

  @VisibleForTesting
  internal val presenter: LiveLogPresenter

  override val isStreaming: Boolean get() = presenter.isStreaming
  override fun startStreaming(device: AdbDevice) = presenter.startStreaming(device)
  override fun stopStreaming() = presenter.stopStreaming()

  override fun getMenus(): List<JMenu> = listOfNotNull(buildStreamsMenu())

  override fun buildStreamsMenu(): JMenu? {
    val streams = presenter.availableStreams
    if (streams.size <= 1) return null

    val streamsMenu = JMenu(I18n.get(I18n.MENU_STREAMS))
    for (stream in streams) {
      val item = JCheckBoxMenuItem(stream.toString()).apply {
        state = presenter.isStreamAllowed(stream)
        addItemListener { presenter.setStreamAllowed(stream, isSelected) }
      }
      streamsMenu.add(item)
    }
    return streamsMenu
  }

  private lateinit var mainSplitPane: JSplitPane
  private lateinit var devicesPanel: LiveLogDevicesPanel
  private var filtersPanel: FiltersPanel? = null
  @VisibleForTesting internal lateinit var deviceLabel: JLabel
  @VisibleForTesting internal lateinit var statusLabel: JLabel
  @VisibleForTesting internal lateinit var statsLabel: JLabel
  @VisibleForTesting internal lateinit var sidePanel: SideButtonsPanel
  @VisibleForTesting internal lateinit var filteringProgressBar: JProgressBar

  @VisibleForTesting
  internal var isAutoScrollActive: Boolean
    get() = sidePanel.isItemActive(ID_AUTOSCROLL)
    set(value) { sidePanel.setItemActive(ID_AUTOSCROLL, value) }

  @VisibleForTesting
  internal val isSidePanelExpanded: Boolean
    get() = sidePanel.isExpanded

  @VisibleForTesting internal lateinit var refreshDevicesBtn: JButton
  @VisibleForTesting internal lateinit var restartServerBtn: JButton
  @VisibleForTesting internal lateinit var buffersCard: JPanel
  @VisibleForTesting internal lateinit var selectAllCb: TriStateCheckbox
  @VisibleForTesting internal val bufferCheckboxes = mutableMapOf<String, JCheckBox>()
  internal val activeStartButtons: MutableList<JButton>
    get() = if (::devicesPanel.isInitialized) devicesPanel.activeStartButtons else mutableListOf()
  private lateinit var deviceSubCardLayout: CardLayout
  @VisibleForTesting internal lateinit var deviceCardsContainer: JPanel
  @VisibleForTesting internal lateinit var deviceItemsContainer: JPanel

  @VisibleForTesting internal lateinit var logList: SearchableTable
  @VisibleForTesting internal lateinit var filteredLogList: SearchableTable
  @VisibleForTesting internal lateinit var logListTableModel: LogListTableModel
  @VisibleForTesting internal lateinit var filteredLogListTableModel: LogListTableModel
  @VisibleForTesting internal val logRenderer: LogCellRenderer = LogCellRenderer()

  private val userPrefs: LogViewerPreferences = ServiceLocator.logViewerPrefs

  private val filtersPanelListener: FiltersPanelListener

  companion object {
    private const val CARD_DEVICES = "card_devices"
    private const val CARD_LOGS = "card_logs"

    internal const val SUB_CARD_SCANNING = LiveLogDevicesPanel.SUB_CARD_SCANNING
    internal const val SUB_CARD_ADB_NOT_FOUND = LiveLogDevicesPanel.SUB_CARD_ADB_NOT_FOUND
    internal const val SUB_CARD_NO_DEVICES = LiveLogDevicesPanel.SUB_CARD_NO_DEVICES
    internal const val SUB_CARD_DEVICE_LIST = LiveLogDevicesPanel.SUB_CARD_DEVICE_LIST

    @VisibleForTesting internal const val ID_STOP_RESUME = LiveLogsPanel.ID_STOP_RESUME
    @VisibleForTesting internal const val ID_CLEAR = LiveLogsPanel.ID_CLEAR
    @VisibleForTesting internal const val ID_AUTOSCROLL = LiveLogsPanel.ID_AUTOSCROLL
    @VisibleForTesting internal const val ID_TERMINAL = LiveLogsPanel.ID_TERMINAL
    @VisibleForTesting internal const val ID_CHANGE_DEVICE = LiveLogsPanel.ID_CHANGE_DEVICE
  }

  override fun attachFiltersPanel(panel: FiltersPanel) {
    this.filtersPanel = panel
    val currentDivider = mainSplitPane.dividerLocation
    mainSplitPane.leftComponent = panel
    if (currentDivider > 0) {
      mainSplitPane.dividerLocation = currentDivider
    }
    panel.listener = filtersPanelListener
    panel.activeFilterContextTag = presenter.filterContextTag
  }

  private fun navigateFilteredLog(filter: Filter, forward: Boolean) {
    val rowCount = filteredLogListTableModel.rowCount
    if (rowCount == 0) return

    val currentRow = filteredLogList.table.selectedRow
    val start = if (currentRow == -1) {
      if (forward) 0 else rowCount - 1
    } else {
      if (forward) currentRow + 1 else currentRow - 1
    }

    val range = if (forward) {
      (start until rowCount)
    } else {
      (start downTo 0)
    }

    for (i in range) {
      val entry = filteredLogListTableModel.getEntry(i)
      if (entry?.appliedFilter == filter) {
        filteredLogList.table.setRowSelectionInterval(i, i)
        SwingUtils.scrollToVisible(filteredLogList.table, i)
        return
      }
    }

    val parentWindow = SwingUtilities.getWindowAncestor(contentPane) ?: contentPane
    if (forward) {
      Toast.showToast(parentWindow, StringUtils.LEFT_ARROW_WITH_HOOK, Toast.LENGTH_SHORT)
    } else {
      Toast.showToast(parentWindow, StringUtils.RIGHT_ARROW_WITH_HOOK, Toast.LENGTH_SHORT)
    }
  }

  init {
    presenter = presenterFactory?.invoke(this) ?: LiveLogPresenterImpl(
      view = this,
      liveLogsRepository = ServiceLocator.liveLogsRepository,
      adbLogStreamer = ServiceLocator.adbLogStreamer
    )
    buildUi()
    presenter.init()

    filtersPanelListener = object : FiltersPanelListener {
      override fun onNavigateNextFilteredLog(filter: Filter) {
        navigateFilteredLog(filter, forward = true)
      }

      override fun onNavigatePrevFilteredLog(filter: Filter) {
        navigateFilteredLog(filter, forward = false)
      }
    }

    userPrefs.addPreferenceListener(object : LogViewerPreferences.Adapter() {
      override fun onShowLineNumbersChanged() {
        logRenderer.showLineNumbers(userPrefs.showLineNumbers)
        logList.table.repaint()
        filteredLogList.table.repaint()
      }

      override fun onLogFontSizeChanged() {
        updateLogFont()
      }

      override fun onLogFontBoldChanged() {
        updateLogFont()
      }

      override fun onLogFontFamilyChanged() {
        updateLogFont()
      }
    })

    logRenderer.showLineNumbers(userPrefs.showLineNumbers)
    updateLogFont()
  }

  private fun buildUi() {
    val devicesPanel = buildDevicesPanel()
    val logsPanel = buildLogsPanel()

    contentPane.add(devicesPanel, CARD_DEVICES)
    contentPane.add(logsPanel, CARD_LOGS)
    rootCardLayout.show(contentPane, CARD_DEVICES)
  }

  private fun buildDevicesPanel(): JPanel {
    devicesPanel = LiveLogDevicesPanel(presenter)
    refreshDevicesBtn = devicesPanel.refreshDevicesBtn
    restartServerBtn = devicesPanel.restartServerBtn
    buffersCard = devicesPanel.buffersCard
    selectAllCb = devicesPanel.selectAllCb
    bufferCheckboxes.clear()
    bufferCheckboxes.putAll(devicesPanel.bufferCheckboxes)
    devicesPanel.selectAllCb.addSelectionChangedListener(object : TriStateCheckbox.SelectionChangedListener {
      override fun onSelectionChanged(newSelectionState: TriStateCheckbox.SelectionState) {
        val selectAll = newSelectionState == TriStateCheckbox.SelectionState.SELECTED
        devicesPanel.updateStartButtonsState(selectAll)
      }
    })
    for (cb in devicesPanel.bufferCheckboxes.values) {
      cb.addActionListener {
        devicesPanel.updateStartButtonsState(getSelectedBuffers()?.isNotEmpty() == true)
      }
    }
    deviceSubCardLayout = devicesPanel.deviceSubCardLayout
    deviceCardsContainer = devicesPanel.deviceCardsContainer
    deviceItemsContainer = devicesPanel.deviceItemsContainer
    return devicesPanel
  }

  private fun createEmptyStatePanel(
    title: String,
    message: String,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null
  ): JPanel {
    val panel = JPanel(GridBagLayout())
    val content = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      isOpaque = false
    }

    val titleLabel = JLabel(title).apply {
      font = font.deriveFont(Font.BOLD, UIScaleUtils.dip(15).toFloat())
      alignmentX = Component.CENTER_ALIGNMENT
    }
    content.add(titleLabel)

    if (message.isNotEmpty()) {
      content.add(Box.createVerticalStrut(UIScaleUtils.dip(8)))
      val msgLabel = JLabel("<html><center style='width: 380px;'>$message</center></html>").apply { // i18n:ignore
        foreground = UIManager.getColor("Label.disabledForeground") ?: Color.GRAY
        alignmentX = Component.CENTER_ALIGNMENT
      }
      content.add(msgLabel)
    }

    if (buttonText != null && onButtonClick != null) {
      content.add(Box.createVerticalStrut(UIScaleUtils.dip(16)))
      val btn = JButton(buttonText).apply {
        alignmentX = Component.CENTER_ALIGNMENT
        addActionListener { onButtonClick() }
      }
      content.add(btn)
    }

    panel.add(content)
    return panel
  }

  private fun buildLogsPanel(): JPanel {
    val logsPanel = LiveLogsPanel(
      presenter = presenter,
      logRenderer = logRenderer,
      selectedBuffersProvider = { getSelectedBuffers() },
      onAutoScrollToggled = { active ->
        if (active) {
          scrollToBottom()
        }
      }
    )

    deviceLabel = logsPanel.deviceLabel
    statusLabel = logsPanel.statusLabel
    statsLabel = logsPanel.statsLabel
    filteringProgressBar = logsPanel.filteringProgressBar

    logListTableModel = logsPanel.logListTableModel
    logList = logsPanel.logList
    filteredLogListTableModel = logsPanel.filteredLogListTableModel
    filteredLogList = logsPanel.filteredLogList

    mainSplitPane = logsPanel.mainSplitPane
    sidePanel = logsPanel.sidePanel

    setupLogsContextActions()
    setupAutoScrollInteractions()
    return logsPanel
  }

  fun setSidePanelExpanded(expanded: Boolean) {
    sidePanel.isExpanded = expanded
  }

  private fun updateLogFont() {
    val fontSize = userPrefs.logFontSize
    val isBold = userPrefs.logFontBold
    val fontFamily = userPrefs.logFontFamily
    logRenderer.updateFont(fontSize, isBold, fontFamily)

    val font = LogFontManager.getFont(fontFamily, if (isBold) Font.BOLD else Font.PLAIN, fontSize)
    val fontMetrics = logList.table.getFontMetrics(font)
    val rowHeight = maxOf(UIScaleUtils.dip(22), fontMetrics.height + UIScaleUtils.dip(6))

    logList.table.rowHeight = rowHeight
    filteredLogList.table.rowHeight = rowHeight

    logListTableModel.lastEntry?.let { logRenderer.recalculateLineNumberPreferredSize(it.index) }
    logList.table.revalidate()
    filteredLogList.table.revalidate()
    logList.table.repaint()
    filteredLogList.table.repaint()
  }

  override fun showStreamingStatus(statusText: String, isStreaming: Boolean) {
    statusLabel.text = I18n.format(I18n.LIVE_LOGS_LABEL_STATUS, statusText)
    if (isStreaming) {
      sidePanel.updateItem(
        ID_STOP_RESUME,
        CommonIcons.stopIcon(),
        I18n.get(I18n.LIVE_LOGS_STOP)
      )
      sidePanel.setItemEnabled(ID_STOP_RESUME, true)
    } else {
      sidePanel.updateItem(
        ID_STOP_RESUME,
        CommonIcons.playIcon(),
        I18n.get(I18n.LIVE_LOGS_START)
      )
      sidePanel.setItemEnabled(ID_STOP_RESUME, presenter.connectedDevice != null)
    }
    val device = presenter.connectedDevice
    deviceLabel.text = I18n.format(I18n.LIVE_LOGS_LABEL_DEVICE, device?.displayName ?: "-")
    sidePanel.setItemEnabled(ID_TERMINAL, device?.isOnline == true)
  }

  private fun showDeviceSelectionView() {
    rootCardLayout.show(contentPane, CARD_DEVICES)
  }

  override fun showLogsView(device: AdbDevice) {
    deviceLabel.text = I18n.format(I18n.LIVE_LOGS_LABEL_DEVICE, device.displayName)
    sidePanel.setItemEnabled(ID_TERMINAL, device.isOnline)
    sidePanel.setItemEnabled(ID_STOP_RESUME, true)
    rootCardLayout.show(contentPane, CARD_LOGS)
  }

  override fun showDeviceScanning() {
    showDeviceSelectionView()
    deviceSubCardLayout.show(deviceCardsContainer, LiveLogDevicesPanel.SUB_CARD_SCANNING)
  }

  override fun showNoDevices() {
    showDeviceSelectionView()
    deviceSubCardLayout.show(deviceCardsContainer, LiveLogDevicesPanel.SUB_CARD_NO_DEVICES)
  }

  override fun showDeviceList(devices: List<AdbDevice>) {
    showDeviceSelectionView()
    populateDeviceList(devices)
    deviceSubCardLayout.show(deviceCardsContainer, LiveLogDevicesPanel.SUB_CARD_DEVICE_LIST)
  }

  override fun showAdbNotAvailable() {
    showDeviceSelectionView()
    deviceSubCardLayout.show(deviceCardsContainer, LiveLogDevicesPanel.SUB_CARD_ADB_NOT_FOUND)
  }

  override fun showRestartingServer(isRestarting: Boolean) {
    restartServerBtn.isEnabled = !isRestarting
    refreshDevicesBtn.isEnabled = !isRestarting
    if (isRestarting) {
      val window = SwingUtilities.getWindowAncestor(contentPane) ?: contentPane
      Toast.showToast(window, I18n.get(I18n.LIVE_LOGS_RESTARTING_SERVER), Toast.LENGTH_SHORT)
    }
  }

  override fun showRestartServerResult(success: Boolean) {
    val window = SwingUtilities.getWindowAncestor(contentPane) ?: contentPane
    if (success) {
      Toast.showToast(window, I18n.get(I18n.ADB_DEVICES_RESTART_SUCCESS), Toast.LENGTH_SHORT)
    } else {
      JOptionPane.showMessageDialog(
        window,
        I18n.get(I18n.ADB_DEVICES_RESTART_FAILED),
        I18n.get(I18n.ADB_ERROR_TITLE),
        JOptionPane.ERROR_MESSAGE
      )
    }
  }

  override fun showOpenTerminalFailed() {
    val window = SwingUtilities.getWindowAncestor(contentPane) ?: contentPane
    JOptionPane.showMessageDialog(
      window,
      I18n.get(I18n.ADB_SHELL_TERMINAL_FAILED),
      I18n.get(I18n.ADB_ERROR_TITLE),
      JOptionPane.WARNING_MESSAGE
    )
  }

  override fun onTabSelected() {
    if (!presenter.isStreaming && presenter.connectedDevice == null) {
      presenter.loadDevices()
    }
  }

  private var currentDevices: List<AdbDevice> = emptyList()

  private fun populateDeviceList(devices: List<AdbDevice>) {
    currentDevices = devices
    deviceItemsContainer.removeAll()
    if (::devicesPanel.isInitialized) {
      devicesPanel.activeStartButtons.clear()
    }

    val borderColor = UIManager.getColor("Component.borderColor")
      ?: UIManager.getColor("Separator.foreground")
      ?: Color(200, 200, 200)
    val hoverBorderColor = UIManager.getColor("Component.focusedBorderColor")
      ?: UIManager.getColor("Tree.selectionBackground")
      ?: Color(0, 120, 215)
    val cardBg = UIManager.getColor("Table.background") ?: UIManager.getColor("Panel.background")

    val normalBorder = BorderFactory.createCompoundBorder(
      BorderFactory.createLineBorder(borderColor, 1),
      EmptyBorder(UIScaleUtils.dip(10), UIScaleUtils.dip(16), UIScaleUtils.dip(10), UIScaleUtils.dip(16))
    )
    val hoverBorder = BorderFactory.createCompoundBorder(
      BorderFactory.createLineBorder(hoverBorderColor, 1),
      EmptyBorder(UIScaleUtils.dip(10), UIScaleUtils.dip(16), UIScaleUtils.dip(10), UIScaleUtils.dip(16))
    )

    for (device in devices) {
      val itemPanel = JPanel(BorderLayout()).apply {
        border = normalBorder
        background = cardBg
        isOpaque = true
        alignmentX = Component.CENTER_ALIGNMENT
        val itemHeight = if (device.state == DeviceState.UNAUTHORIZED) 80 else 64
        maximumSize = Dimension(Int.MAX_VALUE, UIScaleUtils.dip(itemHeight))
      }

      val infoPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
      }

      val topRow = JPanel(FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(8), 0)).apply {
        isOpaque = false
      }

      val (statusText, statusColor) = when (device.state) {
        DeviceState.ONLINE -> "● Online" to Color(46, 139, 87)
        DeviceState.UNAUTHORIZED -> "● Unauthorized" to Color(204, 120, 0)
        DeviceState.OFFLINE -> "● Offline" to Color(178, 34, 34)
        else -> "● ${device.state.name.lowercase()}" to Color.GRAY
      }
      val statusBadge = JLabel(statusText).apply {
        foreground = statusColor
        font = font.deriveFont(Font.BOLD)
      }
      val readableName = (device.model ?: device.product ?: device.device ?: "Android Device").replace('_', ' ')
      val nameLabel = JLabel(readableName).apply {
        font = font.deriveFont(Font.BOLD, UIScaleUtils.dip(13).toFloat())
      }
      val serialLabel = JLabel("(${device.serialNumber})").apply { // i18n:ignore
        foreground = UIManager.getColor("Label.disabledForeground") ?: Color.GRAY
      }

      topRow.add(statusBadge)
      topRow.add(nameLabel)
      topRow.add(serialLabel)
      infoPanel.add(topRow)

      if (device.state == DeviceState.UNAUTHORIZED) {
        val warningRow = JPanel(FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(8), 0)).apply {
          isOpaque = false
        }
        val warningLabel = JLabel(I18n.get(I18n.LIVE_LOGS_DEVICE_UNAUTHORIZED)).apply {
          foreground = Color(204, 120, 0)
          font = font.deriveFont(Font.ITALIC)
        }
        warningRow.add(warningLabel)
        infoPanel.add(warningRow)
      }

      val actionsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, UIScaleUtils.dip(8), 0)).apply {
        isOpaque = false
      }
      if (device.isOnline) {
        val hasSelection = getSelectedBuffers()?.isNotEmpty() == true
        val startBtn = JButton(I18n.get(I18n.LIVE_LOGS_START)).apply {
          cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
          isEnabled = hasSelection
          toolTipText = if (hasSelection) null else I18n.get(I18n.LIVE_LOGS_NO_BUFFERS_SELECTED_TOOLTIP)
          addActionListener {
            val selected = getSelectedBuffers()
            if (selected?.isNotEmpty() == true) {
              presenter.startStreaming(device, selected)
            } else {
              presenter.startStreaming(device)
            }
          }
        }
        activeStartButtons.add(startBtn)

        val termBtn = JButton(I18n.get(I18n.LIVE_LOGS_TERMINAL)).apply {
          cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
          addActionListener { presenter.openDeviceTerminal(device) }
        }
        actionsPanel.add(startBtn)
        actionsPanel.add(termBtn)

        itemPanel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        itemPanel.addMouseListener(object : MouseAdapter() {
          override fun mouseEntered(e: MouseEvent) {
            itemPanel.border = hoverBorder
            itemPanel.repaint()
          }

          override fun mouseExited(e: MouseEvent) {
            if (e.x !in 0 until itemPanel.width || e.y !in 0 until itemPanel.height) {
              itemPanel.border = normalBorder
              itemPanel.repaint()
            }
          }

          override fun mouseClicked(e: MouseEvent) {
            if (e.clickCount == 2 && SwingUtilities.isLeftMouseButton(e)) {
              val selected = getSelectedBuffers()
              if (selected?.isNotEmpty() == true) {
                presenter.startStreaming(device, selected)
              } else {
                presenter.startStreaming(device)
              }
            }
          }
        })
      }

      itemPanel.add(infoPanel, BorderLayout.WEST)
      itemPanel.add(actionsPanel, BorderLayout.EAST)
      deviceItemsContainer.add(itemPanel)
      if (device != devices.last()) {
        deviceItemsContainer.add(Box.createVerticalStrut(UIScaleUtils.dip(8)))
      }
    }

    deviceItemsContainer.revalidate()
    deviceItemsContainer.repaint()
  }

  override fun showStats(totalReceived: Long, droppedCount: Long, allCount: Int, filteredCount: Int) {
    statsLabel.text = I18n.format(I18n.LIVE_LOGS_STATUS_SUMMARY, allCount, droppedCount)
  }

  override fun showLogsBatch(batch: LiveLogBatch) {
    if (batch.evictedAllCount > 0) {
      val viewPositionBeforeEviction = logList.scrollPane.viewport.viewPosition
      val evictedHeight = batch.evictedAllCount * logList.table.rowHeight
      logListTableModel.removeFirstLogs(batch.evictedAllCount)

      // When rows are removed from the top, all remaining rows shift up by
      // `evictedHeight` pixels in the table coordinate space. If auto-scroll is disabled,
      // JViewport.viewPosition remains unchanged by default, causing visible rows to jump
      // upward. We need to compensate by adjusting viewPosition.y upward by the
      // same amount so the currently visible log lines remain visually in the same place.
      if (!isAutoScrollActive) {
        val newY = maxOf(0, viewPositionBeforeEviction.y - evictedHeight)
        logList.scrollPane.viewport.viewPosition = Point(viewPositionBeforeEviction.x, newY)
      }
    }
    if (batch.newAllLogs.isNotEmpty()) {
      logListTableModel.addLogs(batch.newAllLogs)
      if (isAutoScrollActive && logListTableModel.rowCount > 0) {
        val lastRow = logListTableModel.rowCount - 1
        logList.table.scrollRectToVisible(logList.table.getCellRect(lastRow, 0, true))
      }
    }

    if (batch.evictedFilteredCount > 0) {
      val viewPositionBeforeEviction = filteredLogList.scrollPane.viewport.viewPosition
      val evictedHeight = batch.evictedFilteredCount * filteredLogList.table.rowHeight
      filteredLogListTableModel.removeFirstLogs(batch.evictedFilteredCount)

      // Same compensation for filtered logs when oldest matching entries are evicted
      if (!isAutoScrollActive) {
        val newY = maxOf(0, viewPositionBeforeEviction.y - evictedHeight)
        filteredLogList.scrollPane.viewport.viewPosition = Point(viewPositionBeforeEviction.x, newY)
      }
    }
    if (batch.newFilteredLogs.isNotEmpty()) {
      filteredLogListTableModel.addLogs(batch.newFilteredLogs)
      if (isAutoScrollActive && filteredLogListTableModel.rowCount > 0) {
        val lastRow = filteredLogListTableModel.rowCount - 1
        filteredLogList.table.scrollRectToVisible(filteredLogList.table.getCellRect(lastRow, 0, true))
      }
    }

    if ((batch.newFilteredLogs.isNotEmpty() || batch.evictedFilteredCount > 0) &&
      filtersPanel?.activeFilterContextTag == presenter.filterContextTag
    ) {
      filtersPanel?.updateFiltersUI()
    }

    logListTableModel.lastEntry?.let { logRenderer.recalculateLineNumberPreferredSize(it.index) }
  }

  override fun showAllLogs(allLogs: List<LogEntry>, filteredLogs: List<LogEntry>) {
    logListTableModel.setLogs(allLogs)
    filteredLogListTableModel.setLogs(filteredLogs)
    allLogs.lastOrNull()?.let { logRenderer.recalculateLineNumberPreferredSize(it.index) }
    if (isAutoScrollActive) {
      scrollToBottom()
    }
    if (filtersPanel?.activeFilterContextTag == presenter.filterContextTag) {
      filtersPanel?.updateFiltersUI()
    }
  }

  override fun showFilteredLogs(filteredLogs: List<LogEntry>) {
    filteredLogListTableModel.setLogs(filteredLogs)
    if (isAutoScrollActive) {
      scrollToBottom()
    }
    if (filtersPanel?.activeFilterContextTag == presenter.filterContextTag) {
      filtersPanel?.updateFiltersUI()
    }
  }

  override fun showAvailableLogStreams(streams: Set<LogStream>) {
    val showStreams = streams.size > 1
    logRenderer.showStreams(showStreams)
    logList.table.repaint()
    filteredLogList.table.repaint()
    mainView?.refreshMenuBar()
  }

  internal fun getSelectedBuffers(): List<String>? {
    if (::devicesPanel.isInitialized) {
      return devicesPanel.getSelectedBuffers()
    }
    return null
  }

  override fun showFilteringProgress(progress: Int) {
    if (!filteringProgressBar.isVisible) {
      filteringProgressBar.isVisible = true
    }
    if (progress <= 0) {
      filteringProgressBar.isIndeterminate = true
    } else {
      if (filteringProgressBar.isIndeterminate) {
        filteringProgressBar.isIndeterminate = false
      }
      filteringProgressBar.value = progress
    }
  }

  override fun finishFilteringProgress() {
    filteringProgressBar.isVisible = false
    filteringProgressBar.isIndeterminate = false
    filteringProgressBar.value = 0
  }

  private fun setupAutoScrollInteractions() {
    // Disable auto-scroll when user scrolls UP with mouse wheel or trackpad
    val mouseWheelListener = MouseWheelListener { e ->
      if ((e.wheelRotation < 0 || e.preciseWheelRotation < 0.0) && isAutoScrollActive) {
        isAutoScrollActive = false
      }
    }
    logList.scrollPane.addMouseWheelListener(mouseWheelListener)
    filteredLogList.scrollPane.addMouseWheelListener(mouseWheelListener)

    // Disable auto-scroll when user navigates with keyboard
    val keyListener = object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        if (isAutoScrollActive) {
          when (e.keyCode) {
            KeyEvent.VK_PAGE_UP, KeyEvent.VK_PAGE_DOWN,
            KeyEvent.VK_UP, KeyEvent.VK_DOWN,
            KeyEvent.VK_HOME, KeyEvent.VK_END -> {
              isAutoScrollActive = false
            }
          }
        }
      }
    }
    logList.table.addKeyListener(keyListener)
    filteredLogList.table.addKeyListener(keyListener)

    // Disable auto-scroll when user interacts with the vertical scrollbar
    val adjustmentListener = AdjustmentListener { e ->
      if (e.valueIsAdjusting && isAutoScrollActive) {
        isAutoScrollActive = false
      }
    }
    logList.scrollPane.verticalScrollBar.addAdjustmentListener(adjustmentListener)
    filteredLogList.scrollPane.verticalScrollBar.addAdjustmentListener(adjustmentListener)

    val scrollBarMouseListener = object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        if (isAutoScrollActive) {
          isAutoScrollActive = false
        }
      }
    }
    logList.scrollPane.verticalScrollBar.addMouseListener(scrollBarMouseListener)
    filteredLogList.scrollPane.verticalScrollBar.addMouseListener(scrollBarMouseListener)

    // Disable auto-scroll when user clicks on a log entry or anywhere in the table/viewport to inspect it
    val tableMouseListener = object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        if (isAutoScrollActive) {
          isAutoScrollActive = false
        }
      }
    }
    logList.table.addMouseListener(tableMouseListener)
    filteredLogList.table.addMouseListener(tableMouseListener)
  }

  private fun setupLogsContextActions() {
    logList.table.setupLogTableActions(
      model = logListTableModel,
      filtersPanelProvider = { filtersPanel },
      onDoubleClick = { _, clickedEntry ->
        if (clickedEntry.appliedFilter != null) {
          for (index in 0 until filteredLogList.table.rowCount) {
            if (filteredLogListTableModel.getEntry(index) == clickedEntry) {
              SwingUtils.scrollToVisible(filteredLogList.table, index)
              filteredLogList.table.setRowSelectionInterval(index, index)
              break
            }
          }
        }
      }
    )

    filteredLogList.table.setupLogTableActions(
      model = filteredLogListTableModel,
      filtersPanelProvider = { filtersPanel },
      onDoubleClick = { _, clickedEntry ->
        isAutoScrollActive = false
        val targetIndex = clickedEntry.index - presenter.visibleLogsOffset
        if (targetIndex in 0 until logListTableModel.rowCount) {
          logList.table.setRowSelectionInterval(targetIndex, targetIndex)
          SwingUtils.scrollToVisible(logList.table, targetIndex)
        }
      }
    )
  }

  private fun scrollToBottom() {
    if (logListTableModel.rowCount > 0) {
      val lastRow = logListTableModel.rowCount - 1
      logList.table.scrollRectToVisible(logList.table.getCellRect(lastRow, 0, true))
    }
    if (filteredLogListTableModel.rowCount > 0) {
      val lastRow = filteredLogListTableModel.rowCount - 1
      filteredLogList.table.scrollRectToVisible(filteredLogList.table.getCellRect(lastRow, 0, true))
    }
  }

  override fun onThemeChanged() {
    updateLogFont()
    filtersPanel?.onThemeChanged()
    devicesPanel.onThemeChanged()
    if (currentDevices.isNotEmpty()) {
      populateDeviceList(currentDevices)
    }
    SwingUtilities.updateComponentTreeUI(contentPane)
  }

  override fun requestFinish(doFinish: () -> Unit) {
    presenter.finishing()
    doFinish()
  }
}

internal class BuffersCardContainer(layout: WrapLayout) : JPanel(layout) {
  init {
    isOpaque = true
    alignmentX = Component.CENTER_ALIGNMENT
  }

  override fun getPreferredSize(): Dimension {
    val maxAllowed = UIScaleUtils.dip(DeviceListContainer.MAX_WIDTH)
    val parentWidth = parent?.width ?: 0
    val available = if (parentWidth > 0) parentWidth - UIScaleUtils.dip(32) else maxAllowed
    val targetWidth = minOf(maxAllowed, maxOf(UIScaleUtils.dip(320), available))
    val base = super.getPreferredSize()
    return Dimension(targetWidth, base.height)
  }

  override fun getMinimumSize(): Dimension {
    val pref = preferredSize
    return Dimension(minOf(pref.width, UIScaleUtils.dip(320)), pref.height)
  }

  override fun getMaximumSize(): Dimension {
    val pref = preferredSize
    return Dimension(pref.width, pref.height)
  }
}

internal class CenteredScrollablePanel : JPanel(GridBagLayout()), Scrollable {
  init {
    isOpaque = false
  }

  override fun getScrollableTracksViewportWidth(): Boolean = true

  override fun getScrollableTracksViewportHeight(): Boolean {
    val parent = parent
    return if (parent is JViewport) {
      parent.height > preferredSize.height
    } else {
      false
    }
  }

  override fun getPreferredScrollableViewportSize(): Dimension = preferredSize
  override fun getScrollableBlockIncrement(visibleRect: Rectangle, orientation: Int, direction: Int): Int = UIScaleUtils.dip(64)
  override fun getScrollableUnitIncrement(visibleRect: Rectangle, orientation: Int, direction: Int): Int = UIScaleUtils.dip(16)
}

internal class DeviceListContainer : JPanel() {
  init {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    isOpaque = false
  }

  companion object {
    const val MAX_WIDTH = 700
  }

  override fun getPreferredSize(): Dimension {
    val base = super.getPreferredSize()
    val maxAllowed = UIScaleUtils.dip(MAX_WIDTH)
    val parentWidth = parent?.width ?: 0
    val available = if (parentWidth > 0) parentWidth - UIScaleUtils.dip(32) else maxAllowed
    val targetWidth = minOf(maxAllowed, maxOf(UIScaleUtils.dip(320), available))
    return Dimension(targetWidth, base.height)
  }

  override fun getMinimumSize(): Dimension {
    val pref = preferredSize
    return Dimension(minOf(pref.width, UIScaleUtils.dip(320)), pref.height)
  }

  override fun getMaximumSize(): Dimension {
    val pref = preferredSize
    return Dimension(pref.width, Int.MAX_VALUE)
  }
}

