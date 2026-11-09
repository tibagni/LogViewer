package com.tibagni.logviewer

import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.filter.Filter
import com.tibagni.logviewer.filter.FilterContextTag
import com.tibagni.logviewer.filter.FiltersPanel
import com.tibagni.logviewer.filter.FiltersPanelListener
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.log.*
import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.preferences.LogViewerPreferences
import com.tibagni.logviewer.shortcuts.ShortcutAction
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
  initialLogFiles: Set<File> = emptySet()
) : LogViewerView, LogViewerPresenterView {
  override val presenter: LogViewerPresenter

  override val tabTitle: String
    get() = I18n.get(I18n.MAIN_TAB_LOGS)

  override val tabIcon: Icon
    get() = CommonIcons.logsTabIcon(UIScaleUtils.dip(16))

  override val filterContextTag: FilterContextTag
    get() = presenter.filterContextTag

  private var filtersPanel: FiltersPanel? = null

  private lateinit var mainSplitPane: JSplitPane
  private lateinit var mainLogSplit: JSplitPane
  private var myLogsLastDividerLocation = -1.0
  private lateinit var logList: SearchableTable
  private lateinit var filteredLogList: SearchableTable
  private lateinit var myLogsList: SearchableTable
  private lateinit var logsPane: JSplitPane
  private lateinit var emptyLogsCard: JPanel
  private lateinit var emptyLogsOpenBtn: FlatButton

  private val centerLogsCardLayout = CardLayout()
  private val centerLogsContainer = JPanel(centerLogsCardLayout)

  @VisibleForTesting
  internal val headerPanel: LogViewerHeaderPanel = LogViewerHeaderPanel()
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
          val logIndex = filteredLogListTableModel.indexOf(clickedEntry)
          if (logIndex != -1) {
            SwingUtils.scrollToVisible(filteredLogList.table, logIndex)
            filteredLogList.table.setRowSelectionInterval(logIndex, logIndex)
          }
        }
      },
      addSelectionActions = { popup, selectedEntries ->
        val addToMyLogsItem = JMenuItem(I18n.get(I18n.LOGS_MENU_ADD_TO_MY_LOGS)).apply {
          icon = CommonIcons.myLogsIcon(UIScaleUtils.dip(14))
        }
        addToMyLogsItem.addActionListener {
          presenter.addLogEntriesToMyLogs(selectedEntries)
        }
        popup.add(addToMyLogsItem)
      },
      addExtraSingleEntryActions = { popup, singleEntry ->
        popup.add(JSeparator())
        val ignoreBeforeItem = JMenuItem(I18n.get(I18n.LOGS_MENU_IGNORE_BEFORE)).apply {
          icon = CommonIcons.chevronLeftIcon(UIScaleUtils.dip(14))
        }
        ignoreBeforeItem.addActionListener {
          presenter.ignoreLogsBefore(singleEntry.index)
        }
        val ignoreAfterItem = JMenuItem(I18n.get(I18n.LOGS_MENU_IGNORE_AFTER)).apply {
          icon = CommonIcons.chevronRightIcon(UIScaleUtils.dip(14))
        }
        ignoreAfterItem.addActionListener {
          presenter.ignoreLogsAfter(singleEntry.index)
        }
        popup.add(ignoreBeforeItem)
        popup.add(ignoreAfterItem)
      }
    )
  }

  private fun setupFilteredLogsContextActions() {
    filteredLogList.table.setupLogTableActions(
      model = filteredLogListTableModel,
      parentWindow = mainView.parent,
      filtersPanelProvider = { filtersPanel },
      onDoubleClick = { _, clickedEntry ->
        // Map to the visible index
        val logIndex = clickedEntry.index - presenter.visibleLogsOffset
        SwingUtils.scrollToVisible(logList.table, logIndex)
        logList.table.setRowSelectionInterval(logIndex, logIndex)
      },
      addSelectionActions = { popup, selectedEntries ->
        val addToMyLogsItem = JMenuItem(I18n.get(I18n.LOGS_MENU_ADD_TO_MY_LOGS)).apply {
          icon = CommonIcons.myLogsIcon(UIScaleUtils.dip(14))
        }
        addToMyLogsItem.addActionListener {
          presenter.addLogEntriesToMyLogs(selectedEntries)
        }
        popup.add(addToMyLogsItem)
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
          targetIndex = filteredLogListTableModel.indexOf(clickedEntry)
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
        val removeItem = JMenuItem(I18n.get(I18n.COMMON_REMOVE)).apply {
          icon = CommonIcons.deleteIcon(UIScaleUtils.dip(14))
        }
        removeItem.addActionListener {
          presenter.removeFromMyLog(myLogsList.table.selectedRows)
        }
        popup.add(removeItem)
      }
    )

    myLogsList.table.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent?) {
        if (myLogsList.table.selectedRow != -1 && e != null &&
          ServiceLocator.shortcutManager.matches(e, ShortcutAction.LOGS_REMOVE_MY_LOG)
        ) {
          presenter.removeFromMyLog(myLogsList.table.selectedRows)
        }
      }
    })
  }

  override fun getMenus(): List<JMenu> = listOfNotNull(buildStreamsMenu())

  override fun buildStreamsMenu(): JMenu? {
    return LogStreamsMenuBuilder.buildStreamsMenu(
      availableStreams = logStreams,
      isStreamAllowed = { presenter.isStreamAllowed(it) },
      onStreamToggled = { stream, allowed -> presenter.setStreamAllowed(stream, allowed) }
    )
  }

  override fun handleOpenLogsMenu() {
    val openFiles = mainView.showOpenMultipleLogsFileChooser()
    if (!openFiles.isNullOrEmpty()) {
      presenter.loadLogs(openFiles)
    }
  }

  override fun handleRefreshLogsMenu() = presenter.refreshLogs()
  override fun handleCloseLogsMenu() = presenter.closeLogs()
  override fun handleChangeCharsetMenu(charset: Charset) {
    presenter.refreshLogsWithDifferentCharset(charset)
  }

  override fun handleSaveFilteredLogsMenu() {
    val file = mainView.showSaveLogFileChooser()
    file?.let { presenter.saveFilteredLogs(it) }
  }

  override fun handleGoToTimestampMenu() {
    var hintText = "{month}-{day} {hour}:{min}:{sec}:{hund}"
    var ts: LogTimestamp? = null
    if (filteredLogList.table.hasFocus() && filteredLogList.table.selectedRow >= 0) {
      val row = filteredLogList.table.selectedRow
      val col = filteredLogList.table.selectedColumn
      val selectedEntry = filteredLogList.table.model.getValueAt(row, col) as LogEntry
      ts = selectedEntry.timestamp
    } else if (logList.table.hasFocus() && logList.table.selectedRow >= 0) {
      val row = logList.table.selectedRow
      val col = logList.table.selectedColumn
      val selectedEntry = logList.table.model.getValueAt(row, col) as LogEntry
      ts = selectedEntry.timestamp
    }

    ts?.let {
      hintText = "${it.month}-${it.day} ${it.hour}:${it.minutes}:${it.seconds}.${it.hundredth}"
    }

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
    val userConfig =
      VisibleLogsConfigurationDialog.showIgnoredLogsConfigurationDialog(mainView.parent, config)
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

    val style = if (isBold) Font.BOLD else Font.PLAIN
    val font = LogFontManager.getFont(fontFamilyStr, style, fontSize)
    val fontMetrics = logList.table.getFontMetrics(font)
    val rowHeight = maxOf(UIScaleUtils.dip(22), fontMetrics.height + UIScaleUtils.dip(6))

    logList.table.rowHeight = rowHeight
    filteredLogList.table.rowHeight = rowHeight
    myLogsList.table.rowHeight = rowHeight

    logListTableModel.lastEntry?.let {
      logRenderer.recalculateLineNumberPreferredSize(it.index)
    }
    myLogsListTableModel.lastEntry?.let {
      myLogsRenderer.recalculateLineNumberPreferredSize(it.index)
    }

    logList.table.revalidate()
    logList.table.repaint()
    filteredLogList.table.revalidate()
    filteredLogList.table.repaint()
    myLogsList.table.revalidate()
    myLogsList.table.repaint()
  }

  override fun onThemeChanged() {
    updateLogFont()
    filtersPanel?.onThemeChanged()
    SwingUtilities.updateComponentTreeUI(sideButtonsPanel)
    headerPanel.onThemeChanged()
    if (::logList.isInitialized) {
      logList.onThemeChanged()
      filteredLogList.onThemeChanged()
      myLogsList.onThemeChanged()
    }
    if (::emptyLogsCard.isInitialized) {
      val padVert = UIScaleUtils.dip(24)
      val padHoriz = UIScaleUtils.dip(16)
      emptyLogsCard.border = CardBorderUtils.createCardBorder(
        insets = Insets(padVert, padHoriz, padVert, padHoriz)
      )
    }
    if (::emptyLogsOpenBtn.isInitialized) {
      emptyLogsOpenBtn.updateUI()
    }
  }

  override fun requestFinish(doFinish: () -> Unit) {
    this.doFinish = doFinish
    presenter.finishing()
  }

  override fun showErrorMessage(message: String?) {
    JOptionPane.showMessageDialog(
      contentPane,
      message,
      I18n.get(I18n.COMMON_ERROR),
      JOptionPane.ERROR_MESSAGE
    )
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

  /**
   * Updates the total log count and synchronizes the header panel badge.
   *
   * @param count The total number of loaded log lines.
   */
  private fun updateTotalLogsCount(count: Int) {
    totalLogsCount = count
    headerPanel.updateStats(totalLogsCount, filteredLogsCount)
    updateEmptyStateVisibility()
  }

  /**
   * Updates the filtered log count and synchronizes the header panel badge.
   *
   * @param count The number of filtered log lines matching active filters.
   */
  private fun updateFilteredLogsCount(count: Int) {
    filteredLogsCount = count
    headerPanel.updateStats(totalLogsCount, filteredLogsCount)
  }

  override fun showLogs(logEntries: List<LogEntry>?) {
    updateTotalLogsCount(logEntries?.size ?: 0)
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
      myLogsListTableModel.lastEntry?.let {
        myLogsRenderer.recalculateLineNumberPreferredSize(it.index)
      }
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
      headerPanel.reset()
      updateEmptyStateVisibility()
      return
    }

    val availableWidth = if (contentPane.width > 0) contentPane.width else 1000
    headerPanel.updatePath(logsPath, availableWidth)
  }

  override fun showFilteredLogs(logEntries: List<LogEntry>?) {
    updateFilteredLogsCount(logEntries?.size ?: 0)
    logEntries?.let {
      filteredLogListTableModel.setLogs(it)
    }
    logList.updateUI()
    filtersPanel?.updateFiltersUI()

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

  /**
   * Updates CardLayout visibility between the empty state card and the main logs pane
   * depending on whether log entries are currently loaded.
   */
  private fun updateEmptyStateVisibility() {
    if (totalLogsCount == 0) {
      centerLogsCardLayout.show(centerLogsContainer, CARD_EMPTY)
    } else {
      centerLogsCardLayout.show(centerLogsContainer, CARD_LOGS)
    }
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

  override fun showLoadingProgress(progress: Int, note: String?) {
    mainView.showLoadingProgress("Logs", progress, note)
  }

  override fun finishLoading() = mainView.finishLoading("Logs")

  private fun buildUi() {
    _contentPane = JPanel()
    _contentPane.layout = GridBagLayout()
    _contentPane.preferredSize = Dimension(UIScaleUtils.dip(1000), UIScaleUtils.dip(500))

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

    mainSplitPane = buildSplitPaneHierarchy()
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

    sideButtonsPanel = buildSideButtonsPanel()
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
  }

  /**
   * Builds and configures the split pane hierarchy containing the main logs,
   * filtered logs, and My Logs panels.
   *
   * @return the configured [mainSplitPane] instance.
   */
  private fun buildSplitPaneHierarchy(): JSplitPane {
    mainSplitPane = JSplitPane().apply {
      dividerSize = UIScaleUtils.dip(4)
      isOneTouchExpandable = true
      resizeWeight = 0.15
    }

    mainLogSplit = JSplitPane().apply {
      dividerSize = UIScaleUtils.dip(4)
      resizeWeight = 0.8
      addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY) { evt ->
        if (evt.propertyName == JSplitPane.DIVIDER_LOCATION_PROPERTY) {
          val newDividerLocation = evt.newValue as Int
          if (newDividerLocation < maximumDividerLocation) {
            myLogsLastDividerLocation = newDividerLocation.toDouble() / width
          }
        }
      }
    }

    logsPane = JSplitPane().apply {
      dividerSize = UIScaleUtils.dip(4)
      orientation = JSplitPane.VERTICAL_SPLIT
      resizeWeight = 0.6
    }

    logListTableModel = LogListTableModel(I18n.get(I18n.LOGS_TAB_ALL))
    logList = SearchableTable(logListTableModel)
    logsPane.leftComponent = logList

    filteredLogListTableModel = LogListTableModel(I18n.get(I18n.LOGS_TAB_FILTERED))
    filteredLogList = SearchableTable(filteredLogListTableModel)
    logsPane.rightComponent = filteredLogList

    myLogsListTableModel = LogListTableModel(I18n.get(I18n.LOGS_TAB_MY_LOGS))
    myLogsList = SearchableTable(myLogsListTableModel)

    emptyLogsOpenBtn = FlatButton().apply {
      text = I18n.get(I18n.MAIN_EMPTY_LOGS_BTN_OPEN)
      icon = CommonIcons.folderIcon(UIScaleUtils.dip(14))
      iconTextGap = UIScaleUtils.dip(6)
      addActionListener { handleOpenLogsMenu() }
    }

    emptyLogsCard = JPanel(GridBagLayout()).apply {
      isOpaque = false
      border = CardBorderUtils.createCardBorder(
        insets = Insets(
          UIScaleUtils.dip(24),
          UIScaleUtils.dip(16),
          UIScaleUtils.dip(24),
          UIScaleUtils.dip(16)
        )
      )

      val iconLabel = JLabel(CommonIcons.logsTabIcon(UIScaleUtils.dip(48)))
      val titleLabel = JLabel(I18n.get(I18n.MAIN_EMPTY_LOGS_TITLE)).apply {
        font = font.deriveFont(Font.BOLD, font.size2D + UIScaleUtils.dip(2))
        foreground = UIManager.getColor("Label.foreground")
      }
      val hintLabel = JLabel(I18n.get(I18n.MAIN_EMPTY_LOGS_HINT)).apply {
        foreground = CardBorderUtils.resolveSecondaryLabelColor()
      }

      add(
        iconLabel,
        GBConstraintsBuilder()
          .withGridx(0).withGridy(0)
          .withAnchor(GridBagConstraints.CENTER)
          .withInsets(Insets(0, 0, UIScaleUtils.dip(12), 0))
          .build()
      )
      add(
        titleLabel,
        GBConstraintsBuilder()
          .withGridx(0).withGridy(1)
          .withAnchor(GridBagConstraints.CENTER)
          .withInsets(Insets(0, 0, UIScaleUtils.dip(6), 0))
          .build()
      )
      add(
        hintLabel,
        GBConstraintsBuilder()
          .withGridx(0).withGridy(2)
          .withAnchor(GridBagConstraints.CENTER)
          .withInsets(Insets(0, 0, UIScaleUtils.dip(16), 0))
          .build()
      )
      add(
        emptyLogsOpenBtn,
        GBConstraintsBuilder()
          .withGridx(0).withGridy(3)
          .withAnchor(GridBagConstraints.CENTER)
          .build()
      )
    }

    if (!GraphicsEnvironment.isHeadless()) {
      FileDrop(Logger.getDebugStream(), emptyLogsCard) { files ->
        if (files != null && files.isNotEmpty()) {
          presenter.loadLogs(files)
        }
      }
    }

    centerLogsContainer.apply {
      isOpaque = false
      add(logsPane, CARD_LOGS)
      add(emptyLogsCard, CARD_EMPTY)
    }

    mainLogSplit.leftComponent = centerLogsContainer
    mainLogSplit.rightComponent = myLogsList
    mainLogSplit.rightComponent.isVisible = false

    mainSplitPane.rightComponent = mainLogSplit

    updateEmptyStateVisibility()
    return mainSplitPane
  }

  /**
   * Builds the side action buttons panel with tools for toggling the My Logs view.
   *
   * @return the configured [SideButtonsPanel] instance.
   */
  private fun buildSideButtonsPanel(): SideButtonsPanel {
    return SideButtonsPanel(
      isExpandable = false,
      entries = listOf(
        SidePanelEntry.Toggle(
          id = MY_LOGS_BUTTON_ID,
          icon = CommonIcons.myLogsIcon(),
          text = I18n.get(I18n.LOGS_MY_LOGS_TOOLTIP)
        ) { showMyLogsPanel(it) }
      )
    )
  }

  companion object {
    private const val CARD_LOGS = "logs"
    private const val CARD_EMPTY = "empty"
    private const val MY_LOGS_BUTTON_ID = "my_logs"
  }
}