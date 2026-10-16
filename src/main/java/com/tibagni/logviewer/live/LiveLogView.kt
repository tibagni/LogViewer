package com.tibagni.logviewer.live

import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.View
import com.tibagni.logviewer.adb.AdbDevice
import com.tibagni.logviewer.adb.DeviceState
import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.view.TriStateCheckbox
import com.tibagni.logviewer.log.LogCellRenderer
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogListTableModel
import com.tibagni.logviewer.log.setupLogTableActions
import com.tibagni.logviewer.preferences.LogViewerPreferences
import com.tibagni.logviewer.preferences.LogViewerPreferencesDialog
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

interface LiveLogView : View {
  val contentPane: JPanel
  val isStreaming: Boolean
  fun startStreaming(device: AdbDevice)
  fun stopStreaming()
  fun attachFiltersPanel(panel: FiltersPanel)
  fun onThemeChanged()
  fun onTabSelected()
}

class LiveLogViewImpl(
  // Allows tests to inject a mock presenter
  @VisibleForTesting
  presenterFactory: ((LiveLogPresenterView) -> LiveLogPresenter)? = null
) : LiveLogView, LiveLogPresenterView {

  private val rootCardLayout = CardLayout()
  override val contentPane: JPanel = JPanel(rootCardLayout)

  @VisibleForTesting
  internal val presenter: LiveLogPresenter

  override val isStreaming: Boolean get() = presenter.isStreaming
  override fun startStreaming(device: AdbDevice) = presenter.startStreaming(device)
  override fun stopStreaming() = presenter.stopStreaming()

  private lateinit var mainSplitPane: JSplitPane
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
  private val activeStartButtons = mutableListOf<JButton>()
  private val deviceSubCardLayout = CardLayout()
  @VisibleForTesting internal lateinit var deviceCardsContainer: JPanel
  @VisibleForTesting internal lateinit var deviceItemsContainer: JPanel

  @VisibleForTesting internal lateinit var logList: SearchableTable
  @VisibleForTesting internal lateinit var filteredLogList: SearchableTable
  @VisibleForTesting internal lateinit var logListTableModel: LogListTableModel
  @VisibleForTesting internal lateinit var filteredLogListTableModel: LogListTableModel
  private val logRenderer: LogCellRenderer = LogCellRenderer()

  private val userPrefs: LogViewerPreferences = ServiceLocator.logViewerPrefs

  private val filtersPanelListener: FiltersPanelListener

  companion object {
    private const val CARD_DEVICES = "card_devices"
    private const val CARD_LOGS = "card_logs"

    private const val SUB_CARD_SCANNING = "scanning"
    private const val SUB_CARD_ADB_NOT_FOUND = "adb_not_found"
    private const val SUB_CARD_NO_DEVICES = "no_devices"
    private const val SUB_CARD_DEVICE_LIST = "device_list"

    @VisibleForTesting internal const val ID_STOP_RESUME = "stop_resume"
    @VisibleForTesting internal const val ID_CLEAR = "clear"
    @VisibleForTesting internal const val ID_AUTOSCROLL = "autoscroll"
    @VisibleForTesting internal const val ID_TERMINAL = "terminal"
    @VisibleForTesting internal const val ID_CHANGE_DEVICE = "change_device"
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

  private fun createBuffersCard(): JPanel {
    val card = BuffersCardContainer(
      WrapLayout(FlowLayout.CENTER, UIScaleUtils.dip(8), UIScaleUtils.dip(4))
    )
    updateBuffersCardStyle(card)

    val label = JLabel(I18n.get(I18n.LIVE_LOGS_BUFFERS)).apply {
      font = font.deriveFont(Font.BOLD)
    }
    card.add(label)

    selectAllCb = TriStateCheckbox(I18n.get(I18n.COMMON_ALL)).apply {
      isOpaque = false
      toolTipText = I18n.get(I18n.COMMON_SELECT_UNSELECT_ALL)
      addSelectionChangedListener(object : TriStateCheckbox.SelectionChangedListener {
        override fun onSelectionChanged(newSelectionState: TriStateCheckbox.SelectionState) {
          val selectAll = newSelectionState == TriStateCheckbox.SelectionState.SELECTED
          bufferCheckboxes.values.forEach { it.isSelected = selectAll }
          updateStartButtonsState(selectAll)
        }
      })
    }
    card.add(selectAllCb)

    card.add(JSeparator(SwingConstants.VERTICAL).apply {
      preferredSize = Dimension(1, UIScaleUtils.dip(16))
    })

    val defaultSelected = presenter.defaultBuffers.toSet()
    for (buf in presenter.availableBuffers) {
      val checkbox = JCheckBox(buf).apply {
        isOpaque = false
        isSelected = defaultSelected.contains(buf)
        addActionListener {
          evaluateSelectAllCbState()
          updateStartButtonsState(getSelectedBuffers().isNotEmpty())
        }
      }
      bufferCheckboxes[buf] = checkbox
      card.add(checkbox)
    }

    evaluateSelectAllCbState()
    return card
  }

  private fun evaluateSelectAllCbState() {
    val total = bufferCheckboxes.size
    val selectedCount = bufferCheckboxes.values.count { it.isSelected }
    selectAllCb.selectionState = when (selectedCount) {
      total -> TriStateCheckbox.SelectionState.SELECTED
      0 -> TriStateCheckbox.SelectionState.NOT_SELECTED
      else -> TriStateCheckbox.SelectionState.PARTIALLY_SELECTED
    }
    selectAllCb.updateUI()
  }

  private fun updateBuffersCardStyle(card: JPanel = buffersCard) {
    val borderColor = UIManager.getColor("Component.borderColor")
      ?: UIManager.getColor("Separator.foreground")
      ?: Color(200, 200, 200)
    val cardBg = UIManager.getColor("Table.background") ?: UIManager.getColor("Panel.background")

    card.border = BorderFactory.createCompoundBorder(
      BorderFactory.createLineBorder(borderColor, 1),
      EmptyBorder(UIScaleUtils.dip(8), UIScaleUtils.dip(14), UIScaleUtils.dip(8), UIScaleUtils.dip(14))
    )
    card.background = cardBg
  }

  internal fun getSelectedBuffers(): List<String> {
    return presenter.availableBuffers.filter { bufferCheckboxes[it]?.isSelected == true }
  }

  private fun updateStartButtonsState(hasSelection: Boolean) {
    for (btn in activeStartButtons) {
      btn.isEnabled = hasSelection
      btn.toolTipText = if (hasSelection) null else I18n.get(I18n.LIVE_LOGS_NO_BUFFERS_SELECTED_TOOLTIP)
    }
  }

  private fun buildDevicesPanel(): JPanel {
    val panel = JPanel(BorderLayout())

    val toolbar = JPanel(GridBagLayout()).apply {
      border = BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground") ?: Color.LIGHT_GRAY),
        EmptyBorder(UIScaleUtils.dip(8), UIScaleUtils.dip(16), UIScaleUtils.dip(8), UIScaleUtils.dip(16))
      )
    }

    val titleLabel = JLabel(I18n.get(I18n.LIVE_LOGS_CONNECTED_DEVICES)).apply {
      font = font.deriveFont(Font.BOLD, UIScaleUtils.dip(14).toFloat())
    }

    refreshDevicesBtn = JButton(I18n.get(I18n.LIVE_LOGS_REFRESH)).apply {
      addActionListener { presenter.loadDevices() }
    }
    restartServerBtn = JButton(I18n.get(I18n.LIVE_LOGS_RESTART_SERVER)).apply {
      addActionListener { presenter.restartAdbServer() }
    }

    val buttonsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, UIScaleUtils.dip(8), 0)).apply {
      isOpaque = false
      add(refreshDevicesBtn)
      add(restartServerBtn)
    }

    toolbar.add(
      titleLabel,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(0)
        .withWeightx(1.0)
        .withAnchor(GridBagConstraints.WEST)
        .build()
    )
    toolbar.add(
      buttonsPanel,
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(0)
        .withWeightx(0.0)
        .withAnchor(GridBagConstraints.EAST)
        .build()
    )

    panel.add(toolbar, BorderLayout.NORTH)

    deviceCardsContainer = JPanel(deviceSubCardLayout)

    val scanningPanel = createEmptyStatePanel(
      title = I18n.get(I18n.LIVE_LOGS_SCANNING),
      message = ""
    )
    val noDevicesPanel = createEmptyStatePanel(
      title = I18n.get(I18n.LIVE_LOGS_NO_DEVICES_TITLE),
      message = I18n.get(I18n.LIVE_LOGS_NO_DEVICES_MSG),
      buttonText = I18n.get(I18n.LIVE_LOGS_REFRESH),
      onButtonClick = { presenter.loadDevices() }
    )
    val adbNotFoundPanel = createEmptyStatePanel(
      title = I18n.get(I18n.LIVE_LOGS_ADB_NOT_FOUND_TITLE),
      message = I18n.get(I18n.LIVE_LOGS_ADB_NOT_FOUND_MSG),
      buttonText = I18n.get(I18n.LIVE_LOGS_CONFIGURE_ADB),
      onButtonClick = {
        val frame = SwingUtilities.getWindowAncestor(contentPane) as? JFrame
        LogViewerPreferencesDialog.showPreferencesDialog(frame)
        presenter.loadDevices()
      }
    )

    buffersCard = createBuffersCard()
    deviceItemsContainer = DeviceListContainer()

    val centeringPanel = CenteredScrollablePanel().apply {
      val buffersGbc = GridBagConstraints().apply {
        gridx = 0
        gridy = 0
        weightx = 1.0
        weighty = 0.0
        anchor = GridBagConstraints.NORTH
        fill = GridBagConstraints.NONE
        insets = Insets(
          UIScaleUtils.dip(20),
          UIScaleUtils.dip(16),
          UIScaleUtils.dip(12),
          UIScaleUtils.dip(16)
        )
      }
      add(buffersCard, buffersGbc)

      val devicesGbc = GridBagConstraints().apply {
        gridx = 0
        gridy = 1
        weightx = 1.0
        weighty = 0.0
        anchor = GridBagConstraints.NORTH
        fill = GridBagConstraints.NONE
        insets = Insets(
          0,
          UIScaleUtils.dip(16),
          UIScaleUtils.dip(20),
          UIScaleUtils.dip(16)
        )
      }
      add(deviceItemsContainer, devicesGbc)

      val filler = JPanel().apply { isOpaque = false }
      val fillerGbc = GridBagConstraints().apply {
        gridx = 0
        gridy = 2
        weightx = 1.0
        weighty = 1.0
        fill = GridBagConstraints.BOTH
      }
      add(filler, fillerGbc)

      addComponentListener(object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent) {
          buffersCard.revalidate()
          deviceItemsContainer.revalidate()
        }
      })
    }

    val deviceListScroll = JScrollPane(centeringPanel).apply {
      border = BorderFactory.createEmptyBorder()
      verticalScrollBar.unitIncrement = UIScaleUtils.dip(16)
    }

    deviceCardsContainer.add(scanningPanel, SUB_CARD_SCANNING)
    deviceCardsContainer.add(noDevicesPanel, SUB_CARD_NO_DEVICES)
    deviceCardsContainer.add(adbNotFoundPanel, SUB_CARD_ADB_NOT_FOUND)
    deviceCardsContainer.add(deviceListScroll, SUB_CARD_DEVICE_LIST)

    panel.add(deviceCardsContainer, BorderLayout.CENTER)
    return panel
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
    val panel = JPanel(BorderLayout())

    // Top status/info header
    val toolbar = JPanel(FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(8), UIScaleUtils.dip(4)))
    toolbar.border = BorderFactory.createEmptyBorder(
      UIScaleUtils.dip(2),
      UIScaleUtils.dip(8),
      UIScaleUtils.dip(2),
      UIScaleUtils.dip(8)
    )

    deviceLabel = JLabel(I18n.format(I18n.LIVE_LOGS_LABEL_DEVICE, "-"))
    statusLabel = JLabel(I18n.format(I18n.LIVE_LOGS_LABEL_STATUS, "-"))
    statsLabel = JLabel(I18n.format(I18n.LIVE_LOGS_STATUS_SUMMARY, 0, 0))

    toolbar.add(deviceLabel)
    toolbar.add(JSeparator(SwingConstants.VERTICAL).apply { preferredSize = Dimension(1, UIScaleUtils.dip(16)) })
    toolbar.add(statusLabel)
    toolbar.add(JSeparator(SwingConstants.VERTICAL).apply { preferredSize = Dimension(1, UIScaleUtils.dip(16)) })
    toolbar.add(statsLabel)

    val headerPanel = JPanel(BorderLayout())
    headerPanel.add(toolbar, BorderLayout.CENTER)

    filteringProgressBar = JProgressBar(0, 100).apply {
      preferredSize = Dimension(preferredSize.width, UIScaleUtils.dip(4))
      isStringPainted = false
      isVisible = false
    }
    headerPanel.add(filteringProgressBar, BorderLayout.SOUTH)

    panel.add(headerPanel, BorderLayout.NORTH)

    // Center split pane with logs
    val logsSplitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
    logsSplitPane.dividerSize = UIScaleUtils.dip(5)
    logsSplitPane.resizeWeight = 0.5

    logListTableModel = LogListTableModel(I18n.get(I18n.LOGS_TAB_ALL))
    logList = SearchableTable(logListTableModel)
    logList.table.setDefaultRenderer(LogEntry::class.java, logRenderer)

    filteredLogListTableModel = LogListTableModel(I18n.get(I18n.LOGS_TAB_FILTERED))
    filteredLogList = SearchableTable(filteredLogListTableModel)
    filteredLogList.table.setDefaultRenderer(LogEntry::class.java, logRenderer)

    logsSplitPane.topComponent = logList
    logsSplitPane.bottomComponent = filteredLogList

    setupLogsContextActions()

    val mainSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
    mainSplitPane.dividerSize = UIScaleUtils.dip(5)
    mainSplitPane.isOneTouchExpandable = true
    mainSplitPane.resizeWeight = 0.15
    this.mainSplitPane = mainSplitPane

    mainSplitPane.rightComponent = logsSplitPane
    panel.add(mainSplitPane, BorderLayout.CENTER)

    // Right-side vertical action bar
    sidePanel = buildSidePanel()
    panel.add(sidePanel, BorderLayout.EAST)

    setupAutoScrollInteractions()
    return panel
  }

  private fun buildSidePanel(): SideButtonsPanel {
    return SideButtonsPanel(
      isExpandable = true,
      entries = listOf(
        SidePanelEntry.Action(
          id = ID_STOP_RESUME,
          icon = CommonIcons.stopIcon(),
          text = I18n.get(I18n.LIVE_LOGS_STOP),
          isEnabled = false
        ) {
          if (presenter.isStreaming) {
            presenter.stopStreaming()
          } else {
            presenter.connectedDevice?.let { presenter.startStreaming(it) }
          }
        },
        SidePanelEntry.Action(
          id = ID_CLEAR,
          icon = CommonIcons.clearIcon(),
          text = I18n.get(I18n.LIVE_LOGS_CLEAR)
        ) { presenter.clearLogs() },
        SidePanelEntry.Toggle(
          id = ID_AUTOSCROLL,
          icon = CommonIcons.autoScrollIcon(),
          text = I18n.get(I18n.LIVE_LOGS_AUTOSCROLL),
          initialActive = true
        ) { active ->
          if (active) {
            scrollToBottom()
          }
        },
        SidePanelEntry.Separator,
        SidePanelEntry.Action(
          id = ID_TERMINAL,
          icon = CommonIcons.terminalIcon(),
          text = I18n.get(I18n.LIVE_LOGS_TERMINAL),
          isEnabled = false
        ) {
          presenter.connectedDevice?.let { presenter.openDeviceTerminal(it) }
        },
        SidePanelEntry.Action(
          id = ID_CHANGE_DEVICE,
          icon = CommonIcons.changeDeviceIcon(),
          text = I18n.get(I18n.LIVE_LOGS_CHANGE_DEVICE)
        ) { presenter.disconnectDevice() }
      )
    )
  }

  fun setSidePanelExpanded(expanded: Boolean) {
    sidePanel.isExpanded = expanded
  }

  private fun updateLogFont() {
    val fontSize = userPrefs.logFontSize
    val isBold = userPrefs.logFontBold
    logRenderer.updateFont(fontSize, isBold)

    val font = Font(Font.MONOSPACED, if (isBold) Font.BOLD else Font.PLAIN, fontSize)
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
    deviceSubCardLayout.show(deviceCardsContainer, SUB_CARD_SCANNING)
  }

  override fun showNoDevices() {
    showDeviceSelectionView()
    deviceSubCardLayout.show(deviceCardsContainer, SUB_CARD_NO_DEVICES)
  }

  override fun showDeviceList(devices: List<AdbDevice>) {
    showDeviceSelectionView()
    populateDeviceList(devices)
    deviceSubCardLayout.show(deviceCardsContainer, SUB_CARD_DEVICE_LIST)
  }

  override fun showAdbNotAvailable() {
    showDeviceSelectionView()
    deviceSubCardLayout.show(deviceCardsContainer, SUB_CARD_ADB_NOT_FOUND)
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

    activeStartButtons.clear()
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
        val hasSelection = getSelectedBuffers().isNotEmpty()
        val startBtn = JButton(I18n.get(I18n.LIVE_LOGS_START)).apply {
          cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
          isEnabled = hasSelection
          toolTipText = if (hasSelection) null else I18n.get(I18n.LIVE_LOGS_NO_BUFFERS_SELECTED_TOOLTIP)
          addActionListener {
            val selected = getSelectedBuffers()
            if (selected.isNotEmpty()) {
              presenter.startStreaming(device, selected)
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
              if (selected.isNotEmpty()) {
                presenter.startStreaming(device, selected)
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
    updateBuffersCardStyle()
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

private class BuffersCardContainer(layout: WrapLayout) : JPanel(layout) {
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

private class CenteredScrollablePanel : JPanel(GridBagLayout()), Scrollable {
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

private class DeviceListContainer : JPanel() {
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

