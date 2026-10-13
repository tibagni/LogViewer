package com.tibagni.logviewer.live

import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.View
import com.tibagni.logviewer.adb.AdbDevice
import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.log.LogCellRenderer
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogListTableModel
import com.tibagni.logviewer.preferences.LogViewerPreferences
import com.tibagni.logviewer.util.SwingUtils
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.SearchableTable
import com.tibagni.logviewer.filter.Filter
import com.tibagni.logviewer.filter.FiltersPanel
import com.tibagni.logviewer.filter.FiltersPanelListener
import com.tibagni.logviewer.util.StringUtils
import com.tibagni.logviewer.view.Toast
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Point
import java.awt.event.AdjustmentListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelListener
import javax.swing.*

interface LiveLogView : View {
  val contentPane: JPanel
  val isStreaming: Boolean
  fun startStreaming(device: AdbDevice)
  fun stopStreaming()
  fun attachFiltersPanel(panel: FiltersPanel)
  fun onThemeChanged()
}

class LiveLogViewImpl(
  // Allows tests to inject a mock presenter
  @VisibleForTesting
  presenterFactory: ((LiveLogPresenterView) -> LiveLogPresenter)? = null
) : LiveLogView, LiveLogPresenterView {

  override val contentPane: JPanel = JPanel()

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
  @VisibleForTesting internal lateinit var autoScrollCb: JCheckBox
  @VisibleForTesting internal lateinit var clearBtn: JButton
  @VisibleForTesting internal lateinit var stopBtn: JButton

  @VisibleForTesting internal lateinit var logList: SearchableTable
  @VisibleForTesting internal lateinit var filteredLogList: SearchableTable
  @VisibleForTesting internal lateinit var logListTableModel: LogListTableModel
  @VisibleForTesting internal lateinit var filteredLogListTableModel: LogListTableModel
  private val logRenderer: LogCellRenderer = LogCellRenderer()

  private val userPrefs: LogViewerPreferences = ServiceLocator.logViewerPrefs

  private val filtersPanelListener: FiltersPanelListener

  override fun attachFiltersPanel(panel: FiltersPanel) {
    this.filtersPanel = panel
    val currentDivider = mainSplitPane.dividerLocation
    mainSplitPane.leftComponent = panel
    if (currentDivider > 0) {
      mainSplitPane.dividerLocation = currentDivider
    }
    panel.listener = filtersPanelListener
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
    buildUi()
    presenter = presenterFactory?.invoke(this) ?: LiveLogPresenterImpl(
      view = this,
      liveLogsRepository = ServiceLocator.liveLogsRepository,
      adbLogStreamer = ServiceLocator.adbLogStreamer
    )
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
    contentPane.layout = BorderLayout()

    // Toolbar at top
    val toolbar = JPanel(GridBagLayout())
    toolbar.border = BorderFactory.createEmptyBorder(
      UIScaleUtils.dip(4),
      UIScaleUtils.dip(8),
      UIScaleUtils.dip(4),
      UIScaleUtils.dip(8)
    )

    deviceLabel = JLabel(I18n.format(I18n.LIVE_LOGS_LABEL_DEVICE, "-"))
    statusLabel = JLabel(I18n.format(I18n.LIVE_LOGS_LABEL_STATUS, "-"))
    statsLabel = JLabel(I18n.format(I18n.LIVE_LOGS_STATUS_SUMMARY, 0, 0))

    autoScrollCb = JCheckBox(I18n.get(I18n.LIVE_LOGS_AUTOSCROLL), true)

    clearBtn = JButton(I18n.get(I18n.LIVE_LOGS_CLEAR))
    clearBtn.addActionListener { presenter.clearLogs() }

    stopBtn = JButton(I18n.get(I18n.LIVE_LOGS_STOP))
    stopBtn.isEnabled = false
    stopBtn.addActionListener { presenter.stopStreaming() }

    val leftGroup = JPanel(FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(8), 0))
    leftGroup.add(deviceLabel)
    leftGroup.add(JSeparator(SwingConstants.VERTICAL).apply { preferredSize = Dimension(1, UIScaleUtils.dip(16)) })
    leftGroup.add(statusLabel)
    leftGroup.add(JSeparator(SwingConstants.VERTICAL).apply { preferredSize = Dimension(1, UIScaleUtils.dip(16)) })
    leftGroup.add(statsLabel)

    val rightGroup = JPanel(FlowLayout(FlowLayout.RIGHT, UIScaleUtils.dip(8), 0))
    rightGroup.add(autoScrollCb)
    rightGroup.add(clearBtn)
    rightGroup.add(stopBtn)

    toolbar.add(
      leftGroup,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(0)
        .withWeightx(1.0)
        .withFill(GridBagConstraints.HORIZONTAL)
        .build()
    )
    toolbar.add(
      rightGroup,
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(0)
        .withWeightx(0.0)
        .withAnchor(GridBagConstraints.EAST)
        .build()
    )

    contentPane.add(toolbar, BorderLayout.NORTH)

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

    // Double-click navigation from filtered log list to all logs list
    filteredLogList.table.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (e.clickCount == 2) {
          autoScrollCb.isSelected = false
          val selectedRow = filteredLogList.table.selectedRow
          val entry = filteredLogListTableModel.getEntry(selectedRow)
          if (entry != null) {
            val targetIndex = entry.index - presenter.visibleLogsOffset
            if (targetIndex in 0 until logListTableModel.rowCount) {
              logList.table.setRowSelectionInterval(targetIndex, targetIndex)
              SwingUtils.scrollToVisible(logList.table, targetIndex)
            }
          }
        }
      }
    })

    val mainSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
    mainSplitPane.dividerSize = UIScaleUtils.dip(5)
    mainSplitPane.isOneTouchExpandable = true
    mainSplitPane.resizeWeight = 0.15
    this.mainSplitPane = mainSplitPane

    mainSplitPane.rightComponent = logsSplitPane
    contentPane.add(mainSplitPane, BorderLayout.CENTER)
    setupAutoScrollInteractions()
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
    stopBtn.isEnabled = isStreaming
    val device = presenter.connectedDevice
    deviceLabel.text = I18n.format(I18n.LIVE_LOGS_LABEL_DEVICE, device?.displayName ?: "-")
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
      if (!autoScrollCb.isSelected) {
        val newY = maxOf(0, viewPositionBeforeEviction.y - evictedHeight)
        logList.scrollPane.viewport.viewPosition = Point(viewPositionBeforeEviction.x, newY)
      }
    }
    if (batch.newAllLogs.isNotEmpty()) {
      logListTableModel.addLogs(batch.newAllLogs)
      if (autoScrollCb.isSelected && logListTableModel.rowCount > 0) {
        val lastRow = logListTableModel.rowCount - 1
        logList.table.scrollRectToVisible(logList.table.getCellRect(lastRow, 0, true))
      }
    }

    if (batch.evictedFilteredCount > 0) {
      val viewPositionBeforeEviction = filteredLogList.scrollPane.viewport.viewPosition
      val evictedHeight = batch.evictedFilteredCount * filteredLogList.table.rowHeight
      filteredLogListTableModel.removeFirstLogs(batch.evictedFilteredCount)

      // Same compensation for filtered logs when oldest matching entries are evicted
      if (!autoScrollCb.isSelected) {
        val newY = maxOf(0, viewPositionBeforeEviction.y - evictedHeight)
        filteredLogList.scrollPane.viewport.viewPosition = Point(viewPositionBeforeEviction.x, newY)
      }
    }
    if (batch.newFilteredLogs.isNotEmpty()) {
      filteredLogListTableModel.addLogs(batch.newFilteredLogs)
      if (autoScrollCb.isSelected && filteredLogListTableModel.rowCount > 0) {
        val lastRow = filteredLogListTableModel.rowCount - 1
        filteredLogList.table.scrollRectToVisible(filteredLogList.table.getCellRect(lastRow, 0, true))
      }
    }

    logListTableModel.lastEntry?.let { logRenderer.recalculateLineNumberPreferredSize(it.index) }
  }

  override fun showAllLogs(allLogs: List<LogEntry>, filteredLogs: List<LogEntry>) {
    logListTableModel.setLogs(allLogs)
    filteredLogListTableModel.setLogs(filteredLogs)
    allLogs.lastOrNull()?.let { logRenderer.recalculateLineNumberPreferredSize(it.index) }
    if (autoScrollCb.isSelected) {
      scrollToBottom()
    }
  }

  private fun setupAutoScrollInteractions() {
    autoScrollCb.addActionListener {
      if (autoScrollCb.isSelected) {
        scrollToBottom()
      }
    }

    // Disable auto-scroll when user scrolls UP with mouse wheel or trackpad
    val mouseWheelListener = MouseWheelListener { e ->
      if ((e.wheelRotation < 0 || e.preciseWheelRotation < 0.0) && autoScrollCb.isSelected) {
        autoScrollCb.isSelected = false
      }
    }
    logList.scrollPane.addMouseWheelListener(mouseWheelListener)
    filteredLogList.scrollPane.addMouseWheelListener(mouseWheelListener)

    // Disable auto-scroll when user navigates with keyboard
    val keyListener = object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        if (autoScrollCb.isSelected) {
          when (e.keyCode) {
            KeyEvent.VK_PAGE_UP, KeyEvent.VK_PAGE_DOWN,
            KeyEvent.VK_UP, KeyEvent.VK_DOWN,
            KeyEvent.VK_HOME, KeyEvent.VK_END -> {
              autoScrollCb.isSelected = false
            }
          }
        }
      }
    }
    logList.table.addKeyListener(keyListener)
    filteredLogList.table.addKeyListener(keyListener)

    // Disable auto-scroll when user interacts with the vertical scrollbar
    val adjustmentListener = AdjustmentListener { e ->
      if (e.valueIsAdjusting && autoScrollCb.isSelected) {
        autoScrollCb.isSelected = false
      }
    }
    logList.scrollPane.verticalScrollBar.addAdjustmentListener(adjustmentListener)
    filteredLogList.scrollPane.verticalScrollBar.addAdjustmentListener(adjustmentListener)

    val scrollBarMouseListener = object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        if (autoScrollCb.isSelected) {
          autoScrollCb.isSelected = false
        }
      }
    }
    logList.scrollPane.verticalScrollBar.addMouseListener(scrollBarMouseListener)
    filteredLogList.scrollPane.verticalScrollBar.addMouseListener(scrollBarMouseListener)

    // Disable auto-scroll when user clicks on a log entry or anywhere in the table/viewport to inspect it
    val tableMouseListener = object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        if (autoScrollCb.isSelected) {
          autoScrollCb.isSelected = false
        }
      }
    }
    logList.table.addMouseListener(tableMouseListener)
    filteredLogList.table.addMouseListener(tableMouseListener)
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
    SwingUtilities.updateComponentTreeUI(contentPane)
  }

  override fun requestFinish(doFinish: () -> Unit) {
    presenter.finishing()
    doFinish()
  }
}
