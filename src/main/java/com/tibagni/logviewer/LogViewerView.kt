package com.tibagni.logviewer

import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.filter.EditFilterDialog
import com.tibagni.logviewer.filter.Filter
import com.tibagni.logviewer.filter.FiltersPanel
import com.tibagni.logviewer.filter.FiltersPanelListener
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.log.*
import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.preferences.LogViewerPreferences
import com.tibagni.logviewer.util.StringUtils
import com.tibagni.logviewer.util.SwingUtils
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.*
import com.tibagni.logviewer.util.LogFontManager
import java.awt.*
import java.awt.event.*
import java.io.File
import java.nio.charset.Charset
import javax.swing.*
import kotlin.collections.HashSet


// This is the interface known by other views (MainView)
interface LogViewerView : MainViewTab {
  val presenter: LogViewerPresenter
  fun buildStreamsMenu(): JMenu?
  fun handleOpenLogsMenu()
  fun handleRefreshLogsMenu()
  fun handleCloseLogsMenu()
  fun handleChangeCharsetMenu(charset: Charset)
  fun handleSaveFilteredLogsMenu()
  fun handleGoToTimestampMenu()
  fun handleConfigureIgnoredLogs()
}

// This is the interface known by the presenter
interface LogViewerPresenterView : AsyncPresenter.AsyncPresenterView {
  fun showErrorMessage(message: String?)
  fun showSkippedLogsMessage(skippedLogs: List<String>)
  fun showLogs(logEntries: List<LogEntry>?)
  fun showMyLogs(logEntries: List<LogEntry>?)
  fun showCurrentLogsLocation(logsPath: String?)
  fun showFilteredLogs(logEntries: List<LogEntry>?)
  fun showLogLocationAtSearchedTimestamp(allLogsPosition: Int, filteredLogsPosition: Int)
  fun showInvalidTimestampSearchError(failedInput: String?)
  fun showAvailableLogStreams(logStreams: Set<LogStream>?)
  fun finish()
  fun showNavigationNextOver()
  fun showNavigationPrevOver()
  fun showOpenPotentialBugReport(bugreportPath: String, bugreportText: String)
  fun closeCurrentlyOpenedBugReports()
}

class LogViewerViewImpl(
  private val mainView: MainView,
  private var filtersPanel: FiltersPanel,
  initialLogFiles: Set<File>
) : LogViewerView, LogViewerPresenterView {
  override val presenter: LogViewerPresenter

  private lateinit var mainSplitPane: JSplitPane
  private lateinit var mainLogSplit: JSplitPane
  private var myLogsLastDividerLocation = -1.0
  private lateinit var logList: SearchableTable
  private lateinit var filteredLogList: SearchableTable
  private lateinit var myLogsList: SearchableTable
  private lateinit var logsPane: JSplitPane
  @VisibleForTesting
  internal lateinit var headerPanel: JPanel
  @VisibleForTesting
  internal lateinit var currentLogsLbl: JLabel
  @VisibleForTesting
  internal lateinit var statusBadge: StatusBadge
  private var totalLogsCount = 0
  private var filteredLogsCount = 0
  private lateinit var sideButtonsPanel: SideButtonsPanel

  private lateinit var logListTableModel: LogListTableModel
  private lateinit var filteredLogListTableModel: LogListTableModel
  private lateinit var myLogsListTableModel: LogListTableModel
  private val logRenderer: LogCellRenderer
  private val myLogsRenderer: LogCellRenderer
  private val userPrefs: LogViewerPreferences = ServiceLocator.logViewerPrefs

  private lateinit var _contentPane: JPanel
  override val contentPane: JPanel
    get() = _contentPane

  private val logStreams: HashSet<LogStream> = HashSet()
  private var doFinish: (() -> Unit)? = null

  private val filtersPanelListener: FiltersPanelListener

  override fun attachFiltersPanel(panel: FiltersPanel) {
    this.filtersPanel = panel
    panel.activeFilterContextTag = presenter.filterContextTag
    val currentDivider = mainSplitPane.dividerLocation
    mainSplitPane.leftComponent = panel
    if (currentDivider > 0) {
      mainSplitPane.dividerLocation = currentDivider
    }
    panel.listener = filtersPanelListener
  }

  init {
    buildUi()

    presenter = LogViewerPresenterImpl(
      this,
      userPrefs,
      ServiceLocator.logsRepository,
      ServiceLocator.myLogsRepository,
      ServiceLocator.filtersRepository
    )
    presenter.init()

    filtersPanelListener = object : FiltersPanelListener {
      override fun onNavigateNextFilteredLog(filter: Filter) {
        val selectedFilteredLog = filteredLogList.table.selectedRow
        val filteredLogIdx = presenter.getNextFilteredLogForFilter(filter, selectedFilteredLog)
        if (filteredLogIdx != -1) {
          SwingUtils.scrollToVisible(filteredLogList.table, filteredLogIdx)
          filteredLogList.table.setRowSelectionInterval(filteredLogIdx, filteredLogIdx)
        }
      }

      override fun onNavigatePrevFilteredLog(filter: Filter) {
        val selectedFilteredLog = filteredLogList.table.selectedRow
        val filteredLogIdx = presenter.getPrevFilteredLogForFilter(filter, selectedFilteredLog)
        if (filteredLogIdx != -1) {
          SwingUtils.scrollToVisible(filteredLogList.table, filteredLogIdx)
          filteredLogList.table.setRowSelectionInterval(filteredLogIdx, filteredLogIdx)
        }
      }
    }
    filtersPanel.listener = filtersPanelListener

    logRenderer = LogCellRenderer()
    logRenderer.showLineNumbers(userPrefs.showLineNumbers)

    // not shared with the log/filtered log list
    // the 'my logs' panel was shorten that others, so it will
    // make the line wrap, it will also affect the log/filtered log list
    myLogsRenderer = LogCellRenderer()
    myLogsRenderer.showLineNumbers(userPrefs.showLineNumbers)

    val showDividers = userPrefs.showLogLineDividers
    logList.table.showHorizontalLines = showDividers
    filteredLogList.table.showHorizontalLines = showDividers
    myLogsList.table.showHorizontalLines = showDividers

    // Listen to user preferences changes
    userPrefs.addPreferenceListener(object : LogViewerPreferences.Adapter() {
      override fun onShowLineNumbersChanged() {
        logRenderer.showLineNumbers(userPrefs.showLineNumbers)
        myLogsRenderer.showLineNumbers(userPrefs.showLineNumbers)
        logList.table.revalidate()
        logList.table.repaint()
        filteredLogList.table.revalidate()
        filteredLogList.table.repaint()
        myLogsList.table.revalidate()
        myLogsList.table.repaint()
      }

      override fun onShowLogLineDividersChanged() {
        val show = userPrefs.showLogLineDividers
        logList.table.showHorizontalLines = show
        filteredLogList.table.showHorizontalLines = show
        myLogsList.table.showHorizontalLines = show
        logList.table.repaint()
        filteredLogList.table.repaint()
        myLogsList.table.repaint()
      }

      override fun onLogFontSizeChanged() {
        updateLogFont()
      }

      override fun onLogFontFamilyChanged() {
        updateLogFont()
      }

      override fun onLogFontBoldChanged() {
        updateLogFont()
      }
    })

    logList.table.setDefaultRenderer(LogEntry::class.java, logRenderer)
    filteredLogList.table.setDefaultRenderer(LogEntry::class.java, logRenderer)
    myLogsList.table.setDefaultRenderer(LogEntry::class.java, myLogsRenderer)
    setupLogsContextActions()
    setupFilteredLogsContextActions()
    setupMyLogsContextActions()

    updateLogFont()

    // Configure file drop
    FileDrop(Logger.getDebugStream(), logsPane) { presenter.loadLogs(it) }

    // Load initial log files if any when component is shown
    if (initialLogFiles.isNotEmpty()) {
      mainView.parent.addComponentListener(object : ComponentAdapter() {
        override fun componentShown(e: ComponentEvent) {
          Logger.debug("Will load initial log files")
          mainView.parent.removeComponentListener(this)
          presenter.loadLogs(initialLogFiles.toTypedArray())
        }
      })
    }
  }


  private fun setupLogsContextActions() {
    logList.table.setupLogTableActions(
      model = logListTableModel,
      parentWindow = mainView.parent,
      filtersPanelProvider = { filtersPanel },
      onDoubleClick = { _, clickedEntry ->
        // go back to the filter list pos
        if (clickedEntry.appliedFilter != null) {
          var logIndex = -1
          for (index in 0 until filteredLogList.table.rowCount) {
            if (filteredLogListTableModel.getValueAt(index, 0) == clickedEntry) {
              logIndex = index
              break
            }
          }
          if (logIndex != -1) {
            SwingUtils.scrollToVisible(filteredLogList.table, logIndex)
            filteredLogList.table.setRowSelectionInterval(logIndex, logIndex)
          }
        }
      },
      addSelectionActions = { popup, selectedEntries ->
        popup.add(I18n.get(I18n.LOGS_MENU_ADD_TO_MY_LOGS)).addActionListener {
          presenter.addLogEntriesToMyLogs(selectedEntries)
        }
      },
      addExtraSingleEntryActions = { popup, singleEntry ->
        popup.add(JSeparator())
        popup.add(I18n.get(I18n.LOGS_MENU_IGNORE_BEFORE)).addActionListener {
          presenter.ignoreLogsBefore(singleEntry.index)
        }
        popup.add(I18n.get(I18n.LOGS_MENU_IGNORE_AFTER)).addActionListener {
          presenter.ignoreLogsAfter(singleEntry.index)
        }
      }
    )
  }

  private fun setupFilteredLogsContextActions() {
    filteredLogList.table.setupLogTableActions(
      model = filteredLogListTableModel,
      parentWindow = mainView.parent,
      filtersPanelProvider = { filtersPanel },
      onDoubleClick = { _, clickedEntry ->
        val logIndex = (clickedEntry.index - presenter.visibleLogsOffset) // Map to the visible index
        SwingUtils.scrollToVisible(logList.table, logIndex)
        logList.table.setRowSelectionInterval(logIndex, logIndex)
      },
      addSelectionActions = { popup, selectedEntries ->
        popup.add(I18n.get(I18n.LOGS_MENU_ADD_TO_MY_LOGS)).addActionListener {
          presenter.addLogEntriesToMyLogs(selectedEntries)
        }
      }
    )
  }

  private fun setupMyLogsContextActions() {
    myLogsList.table.setupLogTableActions(
      model = myLogsListTableModel,
      parentWindow = mainView.parent,
      filtersPanelProvider = { filtersPanel },
      onDoubleClick = { _, clickedEntry ->
        // if it is filtered, first jump to the filtered log panel
        var targetTable = logList.table
        var targetIndex = -1
        if (clickedEntry.appliedFilter != null) {
          for (index in 0 until filteredLogList.table.rowCount) {
            if (filteredLogListTableModel.getValueAt(index, 0) == clickedEntry) {
              targetIndex = index
              break
            }
          }
          targetTable = filteredLogList.table
        }
        if (targetIndex == -1) {
          // if cannot go to the filtered list (not found or stream not shown), fallback to all logs
          targetIndex = clickedEntry.index - presenter.visibleLogsOffset // Map to the visible index
          targetTable = logList.table
        }
        if (targetIndex in 0 until targetTable.rowCount) {
          SwingUtils.scrollToVisible(targetTable, targetIndex)
          targetTable.setRowSelectionInterval(targetIndex, targetIndex)
        }
      },
      addSelectionActions = { popup, _ ->
        popup.add(I18n.get(I18n.COMMON_REMOVE)).addActionListener {
          presenter.removeFromMyLog(myLogsList.table.selectedRows)
        }
      }
    )

    myLogsList.table.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent?) {
        if (myLogsList.table.selectedRow != -1 && (e?.keyCode == KeyEvent.VK_DELETE)) {
          presenter.removeFromMyLog(myLogsList.table.selectedRows)
        }
      }
    })
  }

  override fun getMenus(): List<JMenu> = listOfNotNull(buildStreamsMenu())

  override fun buildStreamsMenu(): JMenu? {
    if (logStreams.isEmpty()) return null

    val streamsMenu = JMenu(I18n.get(I18n.MENU_STREAMS))
    for (stream in logStreams) {
      val item = JCheckBoxMenuItem(stream.toString())
      item.state = presenter.isStreamAllowed(stream)
      item.addItemListener { presenter.setStreamAllowed(stream, item.isSelected) }
      streamsMenu.add(item)
    }

    return streamsMenu
  }

  override fun handleOpenLogsMenu() {
    val openFiles = mainView.showOpenMultipleLogsFileChooser()
    if (!openFiles.isNullOrEmpty()) {
      presenter.loadLogs(openFiles)
    }
  }

  override fun handleRefreshLogsMenu() = presenter.refreshLogs()
  override fun handleCloseLogsMenu() = presenter.closeLogs()
  override fun handleChangeCharsetMenu(charset: Charset) = presenter.refreshLogsWithDifferentCharset(charset)

  override fun handleSaveFilteredLogsMenu() {
    val file = mainView.showSaveLogFileChooser()
    file?.let { presenter.saveFilteredLogs(it) }
  }

  override fun handleGoToTimestampMenu() {
    var hintText = "{month}-{day} {hour}:{min}:{sec}:{hund}"
    var ts: LogTimestamp? = null
    if (filteredLogList.table.hasFocus() && filteredLogList.table.selectedRow >= 0) {
      val selectedEntry =
        filteredLogList.table.model.getValueAt(filteredLogList.table.selectedRow, filteredLogList.table.selectedColumn) as LogEntry
      ts = selectedEntry.timestamp
    } else if (logList.table.hasFocus() && logList.table.selectedRow >= 0) {
      val selectedEntry =
        logList.table.model.getValueAt(logList.table.selectedRow, logList.table.selectedColumn) as LogEntry
      ts = selectedEntry.timestamp
    }

    ts?.let { hintText = "${it.month}-${it.day} ${it.hour}:${it.minutes}:${it.seconds}.${it.hundredth}" }

    val input =
      JOptionPane.showInputDialog(
        contentPane,
        I18n.get(I18n.LOGS_GOTO_TIMESTAMP_MSG),
        I18n.get(I18n.LOGS_GOTO_TIMESTAMP_TITLE),
        JOptionPane.PLAIN_MESSAGE,
        null,
        null,
        hintText
      ) as String?

    if (input.isNullOrEmpty()) {
      Logger.debug("timestamp is empty. Dialog was canceled or no timestamp provided. Aborting...")
      return
    }

    presenter.goToTimestamp(input)
  }

  override fun handleConfigureIgnoredLogs() {
    val config = VisibleLogConfiguration(presenter.firstVisibleLog, presenter.lastVisibleLog)
    val userConfig = VisibleLogsConfigurationDialog.showIgnoredLogsConfigurationDialog(mainView.parent, config)
      ?: return // userConfig null mean dialog was cancelled. Don't do anything in this case

    if (config.startingLog == null && config.endingLog == null) {
      // If there was no starting or ending points configured before the dialog
      // then don't do anything as the dialog is only to clear previously set ignore points
      return
    }

    if (config == userConfig) {
      // No changes. No need to do anything
      return
    }

    // Reset only what changed
    presenter.resetIgnoredLogs(
      config.startingLog != userConfig.startingLog,
      config.endingLog != userConfig.endingLog
    )
  }

  private fun updateLogFont() {
    val fontSize = userPrefs.logFontSize
    val isBold = userPrefs.logFontBold
    val fontFamilyStr = try { userPrefs.logFontFamily } catch (e: Exception) { "Monospaced" }
    logRenderer.updateFont(fontSize, isBold, fontFamilyStr)
    myLogsRenderer.updateFont(fontSize, isBold, fontFamilyStr)

    val font = LogFontManager.getFont(fontFamilyStr, if (isBold) Font.BOLD else Font.PLAIN, fontSize)
    val fontMetrics = logList.table.getFontMetrics(font)
    val rowHeight = maxOf(UIScaleUtils.dip(22), fontMetrics.height + UIScaleUtils.dip(6))

    logList.table.rowHeight = rowHeight
    filteredLogList.table.rowHeight = rowHeight
    myLogsList.table.rowHeight = rowHeight

    logListTableModel.lastEntry?.let { logRenderer.recalculateLineNumberPreferredSize(it.index) }
    myLogsListTableModel.lastEntry?.let { myLogsRenderer.recalculateLineNumberPreferredSize(it.index) }

    logList.table.revalidate()
    logList.table.repaint()
    filteredLogList.table.revalidate()
    filteredLogList.table.repaint()
    myLogsList.table.revalidate()
    myLogsList.table.repaint()
  }

  override fun onThemeChanged() {
    updateLogFont()
    filtersPanel.onThemeChanged()
    SwingUtilities.updateComponentTreeUI(sideButtonsPanel)
    if (currentLogsLbl.icon != null) {
      currentLogsLbl.icon = CommonIcons.fileDocumentIcon()
    }
    headerPanel.repaint()
  }

  override fun requestFinish(doFinish: () -> Unit) {
    this.doFinish = doFinish
    presenter.finishing()
  }

  override fun showErrorMessage(message: String?) {
    JOptionPane.showMessageDialog(contentPane, message, I18n.get(I18n.COMMON_ERROR), JOptionPane.ERROR_MESSAGE)
  }

  override fun showSkippedLogsMessage(skippedLogs: List<String>) {
    val message = StringBuilder(I18n.get(I18n.LOGS_SKIPPED_FILES_MSG))
    message.append("\n\n")
    message.append(skippedLogs.joinToString("\n") { "> $it" })
    JOptionPane.showMessageDialog(
      contentPane,
      message.toString(),
      I18n.get(I18n.LOGS_SKIPPED_FILES_TITLE),
      JOptionPane.WARNING_MESSAGE
    )
  }

  override fun showLogs(logEntries: List<LogEntry>?) {
    totalLogsCount = logEntries?.size ?: 0
    statusBadge.updateStats(totalLogsCount, filteredLogsCount)
    logListTableModel.setLogs(logEntries)
    // calc the line number view needed width
    logEntries?.lastOrNull()?.let { logRenderer.recalculateLineNumberPreferredSize(it.index) }
  }

  override fun showMyLogs(logEntries: List<LogEntry>?) {
    if (logEntries.isNullOrEmpty()) {
      myLogsListTableModel.clear()
      sideButtonsPanel.setItemActive(MY_LOGS_BUTTON_ID, false)
      showMyLogsPanel(false)
    } else {
      myLogsListTableModel.setLogs(logEntries)
      myLogsListTableModel.lastEntry?.let { myLogsRenderer.recalculateLineNumberPreferredSize(it.index) }
      sideButtonsPanel.setItemActive(MY_LOGS_BUTTON_ID, true)
      showMyLogsPanel(true)
    }
  }

  private fun showMyLogsPanel(show: Boolean) {
    if (show) {
      mainLogSplit.rightComponent.isVisible = true
      val dividerPos = if (myLogsLastDividerLocation < 0) 0.7 else myLogsLastDividerLocation
      mainLogSplit.setDividerLocation(dividerPos)
    } else {
      mainLogSplit.rightComponent.isVisible = false
    }
  }

  override fun showCurrentLogsLocation(logsPath: String?) {
    Logger.debug("showCurrentLogsLocation: $logsPath")
    if (logsPath == null) {
      totalLogsCount = 0
      filteredLogsCount = 0
      statusBadge.updateStats(0, 0)
      currentLogsLbl.text = null
      currentLogsLbl.icon = null
      headerPanel.isVisible = false
      return
    }

    currentLogsLbl.icon = CommonIcons.fileDocumentIcon()
    headerPanel.isVisible = true
    val availableWidth = if (contentPane.width > 0) contentPane.width else 1000
    val text = SwingUtils.truncateTextFor(
      currentLogsLbl,
      "",
      logsPath,
      availableWidth
    ).trim()

    currentLogsLbl.text = text
  }

  override fun showFilteredLogs(logEntries: List<LogEntry>?) {
    filteredLogsCount = logEntries?.size ?: 0
    statusBadge.updateStats(totalLogsCount, filteredLogsCount)
    logEntries?.let {
      filteredLogListTableModel.setLogs(it)
    }
    logList.updateUI()
    filtersPanel.updateFiltersUI()

    // Update the save menu option availability
    mainView.enableSaveFilteredLogsMenu(logEntries?.isNotEmpty() ?: false)
  }

  override fun showLogLocationAtSearchedTimestamp(allLogsPosition: Int, filteredLogsPosition: Int) {
    Logger.debug("showLogLocationAtSearchedTimestamp: ($allLogsPosition, $filteredLogsPosition)")
    if (allLogsPosition >= 0) {
      SwingUtils.scrollToVisible(logList.table, allLogsPosition)
      logList.table.setRowSelectionInterval(allLogsPosition, allLogsPosition)
    }

    if (filteredLogsPosition >= 0) {
      SwingUtils.scrollToVisible(filteredLogList.table, filteredLogsPosition)
      filteredLogList.table.setRowSelectionInterval(filteredLogsPosition, filteredLogsPosition)
    }
  }

  override fun showInvalidTimestampSearchError(failedInput: String?) {
    JOptionPane.showMessageDialog(
      contentPane,
      I18n.format(I18n.LOGS_TIMESTAMP_PARSE_ERROR_MSG, failedInput),
      I18n.get(I18n.LOGS_TIMESTAMP_PARSE_ERROR_TITLE),
      JOptionPane.ERROR_MESSAGE
    )
  }

  override fun showAvailableLogStreams(logStreams: Set<LogStream>?) {
    this.logStreams.reset(logStreams)

    // We don't need to show the streams menu if there is only one stream
    val showStreams = logStreams != null && logStreams.size > 1

    logRenderer.showStreams(showStreams)
    myLogsRenderer.showStreams(showStreams)

    // Refresh the menu bar to make sure the streams are shown
    mainView.refreshMenuBar()
  }

  override fun finish() {
    // We can now tell the application we can close
    doFinish?.invoke()
  }

  override fun showNavigationNextOver() =
    Toast.showToast(contentPane.parent, StringUtils.LEFT_ARROW_WITH_HOOK, Toast.LENGTH_SHORT)

  override fun showNavigationPrevOver() =
    Toast.showToast(contentPane.parent, StringUtils.RIGHT_ARROW_WITH_HOOK, Toast.LENGTH_SHORT)

  override fun showOpenPotentialBugReport(bugreportPath: String, bugreportText: String) {
    mainView.onBugReportLoaded(bugreportPath, bugreportText)
  }

  override fun closeCurrentlyOpenedBugReports() {
    mainView.onBugReportClosed()
  }

  override fun showStartLoading() = mainView.showStartLoading("Logs")

  override fun showLoadingProgress(progress: Int, note: String?) = mainView.showLoadingProgress("Logs", progress, note)

  override fun finishLoading() = mainView.finishLoading("Logs")

  private fun buildUi() {
    _contentPane = JPanel()
    _contentPane.layout = GridBagLayout()
    _contentPane.preferredSize = Dimension(UIScaleUtils.dip(1000), UIScaleUtils.dip(500))

    currentLogsLbl = JLabel()
    currentLogsLbl.autoscrolls = true
    currentLogsLbl.text = ""
    currentLogsLbl.icon = CommonIcons.fileDocumentIcon()
    currentLogsLbl.iconTextGap = UIScaleUtils.dip(6)

    statusBadge = StatusBadge()

    headerPanel = object : JPanel(BorderLayout()) {
      override fun paintComponent(g: Graphics) {
        val g2 = g.create() as? Graphics2D ?: return
        try {
          if (width > 0 && height > 0) {
            g2.setRenderingHint(
              RenderingHints.KEY_ANTIALIASING,
              RenderingHints.VALUE_ANTIALIAS_ON
            )
            val bg = UIManager.getColor("Panel.background")
              ?: background
              ?: Color(240, 240, 240)
            val isDark = isDarkTheme(bg)
            val tint = if (isDark) {
              Color(255, 255, 255, 8)
            } else {
              Color(0, 0, 0, 10)
            }
            g2.color = bg
            g2.fillRect(0, 0, width, height)
            g2.color = tint
            g2.fillRect(0, 0, width, height)
          }
        } finally {
          g2.dispose()
        }
        super.paintComponent(g)
      }
    }
    headerPanel.isOpaque = false
    headerPanel.isVisible = false
    headerPanel.border = BorderFactory.createCompoundBorder(
      BorderFactory.createMatteBorder(
        0,
        0,
        UIScaleUtils.dip(1),
        0,
        UIManager.getColor("Component.borderColor")
          ?: Color(128, 128, 128, 60)
      ),
      BorderFactory.createEmptyBorder(
        UIScaleUtils.dip(4),
        UIScaleUtils.dip(8),
        UIScaleUtils.dip(4),
        UIScaleUtils.dip(8)
      )
    )
    headerPanel.add(currentLogsLbl, BorderLayout.CENTER)
    headerPanel.add(statusBadge, BorderLayout.EAST)

    _contentPane.add(
      headerPanel,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(0)
        .withGridWidth(2)
        .withWeightx(1.0)
        .withFill(GridBagConstraints.HORIZONTAL)
        .withAnchor(GridBagConstraints.WEST)
        .build()
    )

    mainSplitPane = JSplitPane()
    mainSplitPane.dividerSize = UIScaleUtils.dip(4)
    mainSplitPane.isOneTouchExpandable = true
    mainSplitPane.resizeWeight = 0.15
    _contentPane.add(
      mainSplitPane,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(1)
        .withWeightx(0.999)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .withAnchor(GridBagConstraints.CENTER)
        .build()
    )

    mainLogSplit = JSplitPane()
    mainLogSplit.dividerSize = UIScaleUtils.dip(4)
    mainLogSplit.resizeWeight = 0.8
    mainLogSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY) { evt ->
      if (evt.propertyName == JSplitPane.DIVIDER_LOCATION_PROPERTY) {
        val newDividerLocation = evt.newValue as Int
        if (newDividerLocation < mainLogSplit.maximumDividerLocation) {
          myLogsLastDividerLocation = newDividerLocation.toDouble() / mainLogSplit.width
        }
      }
    }

    logsPane = JSplitPane()
    logsPane.dividerSize = UIScaleUtils.dip(4)
    logsPane.orientation = JSplitPane.VERTICAL_SPLIT
    logsPane.resizeWeight = 0.6

    logListTableModel = LogListTableModel(I18n.get(I18n.LOGS_TAB_ALL))
    logList = SearchableTable(logListTableModel)
    logsPane.leftComponent = logList // Left or above (above in this case)

    filteredLogListTableModel = LogListTableModel(I18n.get(I18n.LOGS_TAB_FILTERED))
    filteredLogList = SearchableTable(filteredLogListTableModel)
    logsPane.rightComponent = filteredLogList // Right or below (below in this case)

    myLogsListTableModel = LogListTableModel(I18n.get(I18n.LOGS_TAB_MY_LOGS))
    myLogsList = SearchableTable(myLogsListTableModel)

    mainLogSplit.leftComponent = logsPane
    mainLogSplit.rightComponent = myLogsList
    mainLogSplit.rightComponent.isVisible = false // Start collapsed

    mainSplitPane.rightComponent = mainLogSplit

    sideButtonsPanel = SideButtonsPanel(
      isExpandable = false,
      entries = listOf(
        SidePanelEntry.Toggle(
          id = MY_LOGS_BUTTON_ID,
          icon = CommonIcons.myLogsIcon(),
          text = I18n.get(I18n.LOGS_MY_LOGS_TOOLTIP)
        ) { showMyLogsPanel(it) }
      )
    )
    _contentPane.add(
      sideButtonsPanel,
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(1)
        .withWeightx(0.001)
        .withAnchor(GridBagConstraints.EAST)
        .withFill(GridBagConstraints.VERTICAL)
        .build()
    )

    mainSplitPane.leftComponent = filtersPanel
  }

  /**
   * Determines whether the active theme is a dark theme, using the theme
   * manager or falling back to relative luminance of [fallbackBg].
   */
  private fun isDarkTheme(fallbackBg: Color): Boolean {
    return try {
      ServiceLocator.themeManager.isDark
    } catch (_: Throwable) {
      val lum = (0.299 * fallbackBg.red +
        0.587 * fallbackBg.green +
        0.114 * fallbackBg.blue) / 255.0
      lum < 0.5
    }
  }

  companion object {
    private const val MY_LOGS_BUTTON_ID = "my_logs"
  }
}