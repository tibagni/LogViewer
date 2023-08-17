package com.tibagni.logviewer

import com.tibagni.logviewer.LogViewerPresenter.UserSelection
import com.tibagni.logviewer.filter.EditFilterDialog
import com.tibagni.logviewer.filter.Filter
import com.tibagni.logviewer.filter.FiltersList
import com.tibagni.logviewer.filter.FiltersList.FiltersListener
import com.tibagni.logviewer.log.*
import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.preferences.LogViewerPreferences
import com.tibagni.logviewer.util.StringUtils
import com.tibagni.logviewer.util.SwingUtils
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.*
import java.awt.*
import java.awt.event.*
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.io.File
import javax.swing.*
import kotlin.math.roundToInt

// This is the interface known by other views (MainView)
interface LogViewerView : View {
  val contentPane: JPanel
  fun buildStreamsMenu(): JMenu?
  fun handleOpenLogsMenu()
  fun handleRefreshLogsMenu()
  fun handleSaveFilteredLogsMenu()
  fun handleOpenFiltersMenu()
  fun handleGoToTimestampMenu()
  fun handleConfigureIgnoredLogs()
  fun onThemeChanged()
}

// This is the interface known by the presenter
interface LogViewerPresenterView : AsyncPresenter.AsyncPresenterView {
  fun configureFiltersList(filters: Map<String, List<Filter>>?)
  fun showErrorMessage(message: String?)
  fun showSkippedLogsMessage(skippedLogs: List<String>)
  fun showLogs(logEntries: List<LogEntry>?)
  fun showCurrentLogsLocation(logsPath: String?)
  fun showFilteredLogs(logEntries: List<LogEntry>?)
  fun showLogLocationAtSearchedTimestamp(allLogsPosition: Int, filteredLogsPosition: Int)
  fun showInvalidTimestampSearchError(failedInput: String?)
  fun onAppliedFiltersRemembered()
  fun showAvailableLogStreams(logStreams: Set<LogStream>?)
  fun showUnsavedFilterIndication(group: String?)
  fun hideUnsavedFilterIndication(group: String?)
  fun showAskToSaveFilterDialog(group: String?): UserSelection
  fun showAskToSaveMultipleFiltersDialog(groups: Array<String>): Array<Boolean>?
  fun showSaveFilters(group: String?): File?
  fun finish()
  fun showNavigationNextOver()
  fun showNavigationPrevOver()
  fun showOpenPotentialBugReport(bugreportPath: String, bugreportText: String)
  fun closeCurrentlyOpenedBugReports()
  fun collapseAllGroups()
}

class LogViewerViewImpl(private val mainView: MainView, initialLogFiles: Set<File>) : LogViewerView,
  LogViewerPresenterView {
  private val presenter: LogViewerPresenter

  private lateinit var logList: SearchableTable
  private lateinit var filteredLogList: SearchableTable
  private lateinit var pickedLogList: SearchableTable
  private lateinit var clearPickedLogButton: JButton
  private lateinit var addNewFilterGroupBtn: JButton
  private lateinit var moreFilterOptionsBtn: JButton
  private lateinit var collapseExpandAllGroupsBtn: JButton
  private lateinit var logsPane: JSplitPane
  private lateinit var currentLogsLbl: JLabel
  private lateinit var filtersPane: FiltersList

  private lateinit var logListTableModel: LogListTableModel
  private lateinit var filteredLogListTableModel: LogListTableModel
  private lateinit var pickedLogListTableModel: LogListTableModel
  private val logRenderer: LogCellRenderer
  private val pickledLogRenderer: LogCellRenderer

  private lateinit var _contentPane: JPanel
  override val contentPane: JPanel
    get() = _contentPane

  private val logStreams: HashSet<LogStream> = HashSet()
  private var doFinish: (() -> Unit)? = null

  private val frc = FontRenderContext(AffineTransform(), true, true)

  init {
    buildUi()
    val userPrefs = ServiceLocator.logViewerPrefs

    presenter = LogViewerPresenterImpl(
      this,
      userPrefs,
      ServiceLocator.logsRepository,
      ServiceLocator.filtersRepository
    )
    presenter.init()

    logRenderer = LogCellRenderer()
    logRenderer.showLineNumbers(userPrefs.showLineNumbers)

    userPrefs.addPreferenceListener(object : LogViewerPreferences.Adapter() {
      override fun onShowLineNumbersChanged() {
        logRenderer.showLineNumbers(userPrefs.showLineNumbers)
        logList.revalidate()
        logList.repaint()
        filteredLogList.table.revalidate()
        filteredLogList.table.repaint()
        pickedLogList.table.revalidate()
        pickedLogList.table.repaint()
      }
    })
    // not share with the log/filtered log list
    // the picked log panel was shorten that others, so it will
    // make the line wrap, it will also affect the log/filtered log list
    pickledLogRenderer = LogCellRenderer()
    pickledLogRenderer.showLineNumbers(userPrefs.showLineNumbers)

    addNewFilterGroupBtn.addActionListener { addGroup() }
    // Use Mouse event here to get the position on screen
    moreFilterOptionsBtn.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        showFilterOptionsMenu(e)
      }
    })
    collapseExpandAllGroupsBtn.addActionListener { filtersPane.toggleGroupsVisibility() }
    setupFiltersContextActions()

    logList.table.setDefaultRenderer(LogEntry::class.java, logRenderer)
    filteredLogList.table.setDefaultRenderer(LogEntry::class.java, logRenderer)
    pickedLogList.table.setDefaultRenderer(LogEntry::class.java, pickledLogRenderer)
    setupLogsContextActions()
    setupFilteredLogsContextActions()
    setupPickedLogsContextActions()

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

    updateCollapseExpandButtonState()
  }

  private fun showFilterOptionsMenu(e: MouseEvent) {
    val popup = JPopupMenu()
    val closeAllGroupsItem = popup.add("Close all groups")
    closeAllGroupsItem.toolTipText = "Close all currently open groups"

    val unApplyAllFilters = popup.add("\"Un-apply\" all filters")
    unApplyAllFilters.toolTipText = "\"Un-apply\" all filters from all groups"

    closeAllGroupsItem.addActionListener { closeAllGroups() }
    unApplyAllFilters.addActionListener { clearAllFiltersSelection() }

    // If there are no groups, there is no point in closing anything
    closeAllGroupsItem.isEnabled = presenter.groups.isNotEmpty()

    popup.add(closeAllGroupsItem)
    popup.add(unApplyAllFilters)
    popup.show(e.component, e.x, e.y)
  }

  private fun closeAllGroups() {
    val userChoice = JOptionPane.showConfirmDialog(
      mainView.parent,
      "Are you sure you want to close all groups?",
      "Are you sure?",
      JOptionPane.YES_NO_OPTION,
      JOptionPane.WARNING_MESSAGE
    )
    if (userChoice == JOptionPane.YES_NO_OPTION) {
      val openGroups = presenter.groups
      openGroups.forEach { presenter.removeGroup(it) }
    }
  }

  private fun clearAllFiltersSelection() {
    presenter.setAllFiltersApplied(false)
  }

  private fun addGroup(initializeFilter: Boolean = true): String? {
    var newGroupName = JOptionPane.showInputDialog(
      mainView.parent,
      "What is the name of your new Filters Group?",
      "New Filters Group",
      JOptionPane.PLAIN_MESSAGE
    )

    if (!StringUtils.isEmpty(newGroupName)) {
      // If this name is already taken, a number will be appended to the end of the name
      newGroupName = presenter.addGroup(newGroupName)
      if (initializeFilter) {
        addFilter(newGroupName)
      }
    }

    updateCollapseExpandButtonState()
    return newGroupName
  }

  private fun addFilter(group: String) {
    val newFilter = EditFilterDialog.showEditFilterDialog(mainView.parent)
    if (newFilter != null) {
      presenter.addFilter(group, newFilter)
    }
  }

  private fun setupFiltersContextActions() {
    filtersPane.setFiltersListener(object : FiltersListener {
      override fun onReordered(group: String, orig: Int, dest: Int) {
        presenter.reorderFilters(group, orig, dest)
      }

      override fun onFiltersApplied() {
        presenter.applyFilters()
      }

      override fun onEditFilter(filter: Filter) {
        // The filter is automatically updated by this dialog. We only check the result
        // to determine if the dialog was canceled or not
        val edited = EditFilterDialog.showEditFilterDialog(mainView.parent, filter)
        if (edited != null) {
          // Tell the presenter a filter was edited. It will not update the filters
          // as filters are updated by EditFilterDialog itself, it will only determine
          // if the filter was, in fact, updated and mark unsaved changes if necessary.
          presenter.filterEdited(filter)
        }
      }

      override fun onDuplicateFilter(group: String, filter: Filter) {
        val duplicatedFilter = Filter(filter)

        // It does not make much sense to apply a duplicated filter
        // 1 - If the original filter is already applied, applying the duplicate filter will bring no value
        // 2 - If the original filter is not applied, why user would want to apply the duplicate
        // 3 - Duplicating a filter is useful to create another filter from the original one,
        //     so it only makes sense to apply it after editing
        // 4 - Apply a filter is an expensive operation. If it does not bring any value, better not to do it
        duplicatedFilter.isApplied = false
        presenter.addFilter(group, duplicatedFilter, true)
      }

      override fun onDeleteFilters(group: String, indices: IntArray) {
        val userChoice = JOptionPane.showConfirmDialog(
          mainView.parent,
          "Are you sure you want to delete the selected filter(s)?",
          "Are you sure?",
          JOptionPane.YES_NO_OPTION,
          JOptionPane.WARNING_MESSAGE
        )
        if (userChoice != JOptionPane.YES_OPTION) return
        presenter.removeFilters(group, indices)
      }

      override fun onMoveFilters(group: String, indices: IntArray) {
        val groups = presenter.groups

        // Do not show the current group to the user. It does not make sense to move a filter to the same group
        val options = groups.filter { !StringUtils.areEquals(it, group) }.toTypedArray() + arrayOf("Create new")
        val dialog = SingleChoiceDialog(
          "Move ${indices.size} filter(s)",
          "Select the group to move the filters to",
          options,
          0
        )

        val choice = dialog.show(mainView.parent)
        if (choice == SingleChoiceDialog.DIALOG_CANCELLED) {
          return
        }

        val destGroup = if (choice == options.lastIndex) {
          addGroup(false)
        } else {
          options[choice]
        }

        if (StringUtils.isEmpty(destGroup)) {
          Logger.info("Do not move filter. Selected group is empty")
          return
        }

        presenter.moveFilters(group, destGroup, indices)
      }

      override fun onCloseGroup(group: String) {
        val userChoice = JOptionPane.showConfirmDialog(
          mainView.parent,
          "Are you sure you want to close this group?",
          "Are you sure?",
          JOptionPane.YES_NO_OPTION,
          JOptionPane.WARNING_MESSAGE
        )
        if (userChoice != JOptionPane.YES_NO_OPTION) return
        presenter.removeGroup(group)
      }

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

      override fun onAddFilter(group: String) {
        addFilter(group)
      }

      override fun onSaveFilters(group: String) {
        saveFilter(group)
      }

      override fun onGroupVisibilityChanged(group: String?) {
        updateCollapseExpandButtonState()
      }
    })
  }

  private fun updateCollapseExpandButtonState() {
    collapseExpandAllGroupsBtn.text = if (filtersPane.hasAtLeastOneGroupVisible()) {
      "${StringUtils.DOWN_ARROW_HEAD} All"
    } else {
      "${StringUtils.RIGHT_ARROW_HEAD} All"
    }
  }

  private fun saveFilter(filtersGroup: String) = presenter.saveFilters(filtersGroup)

  private fun setupLogsContextActions() {
    logList.table.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (SwingUtilities.isRightMouseButton(e) && logList.table.selectedRow != -1) {
          val popup = JPopupMenu()
          popup
            .add("Pick this log into the right window")
            .addActionListener {
              logList.table.selectedRows
                .map { logListTableModel.getValueAt(it, 0) as LogEntry }
                .forEach { pickedLogListTableModel.addLog(it) }
              onPickedLogListModelUpdate()
            }
          if (logList.table.selectedRowCount == 1) {
            popup.add(JSeparator())
            val createFilterItem = popup.add("Create Filter from this line...")
            popup.add(JSeparator())
            val ignorePrevLines = popup.add("Ignore all logs before this point")
            val ignoreNextLines = popup.add("Ignore all logs after this point")
            createFilterItem.addActionListener {
              val entry = logListTableModel.getValueAt(logList.table.selectedRow, 0) as LogEntry
              addFilterFromLogLine(entry.logText)
            }
            ignorePrevLines.addActionListener {
              val entry = logListTableModel.getValueAt(logList.table.selectedRow, 0) as LogEntry
              presenter.ignoreLogsBefore(entry.index)
            }
            ignoreNextLines.addActionListener {
              val entry = logListTableModel.getValueAt(logList.table.selectedRow, 0) as LogEntry
              presenter.ignoreLogsAfter(entry.index)
            }
          }
          popup.show(logList.table, e.x, e.y)
        }
      }
    })
  }

  private fun addFilterFromLogLine(logLine: String) {
    val filter = EditFilterDialog.showEditFilterDialogWithText(mainView.parent, logLine)
    if (filter != null) {
      val groups = presenter.groups
      var group = if (groups.size == 1) groups[0] else null
      if (StringUtils.isEmpty(group)) {
        val options = groups.toTypedArray() + arrayOf("Create new")
        val createNewOptionIdx = options.size - 1

        val dialog =
          SingleChoiceDialog(
            "Select Filter group",
            "Which group do you want to add this filter to?",
            options,
            createNewOptionIdx
          )

        val choice = dialog.show(mainView.parent)
        group = when (choice) {
          SingleChoiceDialog.DIALOG_CANCELLED -> null
          createNewOptionIdx -> JOptionPane.showInputDialog(
            mainView.parent,
            "What is the name of your new Filters Group?",
            "New Filters Group",
            JOptionPane.PLAIN_MESSAGE
          )
          else -> options[choice]
        }
      }
      if (!StringUtils.isEmpty(group)) {
        presenter.addFilter(group, filter)
      }
    }
  }

  private fun setupFilteredLogsContextActions() {
    filteredLogList.table.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (e.clickCount == 2) {
          val selectedIndex = filteredLogList.table.selectedRow
          val clickedEntry = filteredLogListTableModel.getValueAt(selectedIndex, 0) as LogEntry
          val logIndex = (clickedEntry.index - presenter.visibleLogsOffset) // Map to the visible index
          SwingUtils.scrollToVisible(logList.table, logIndex)
          logList.table.setRowSelectionInterval(logIndex, logIndex)
        } else if (SwingUtilities.isRightMouseButton(e) && filteredLogList.table.selectedRow != -1) {
          val popup = JPopupMenu()
          val pickItem = popup.add("Pick into the right window")
          pickItem.addActionListener {
            filteredLogList.table.selectedRows
              .map { filteredLogListTableModel.getValueAt(it, 0) as LogEntry }
              .forEach { pickedLogListTableModel.addLog(it) }
            onPickedLogListModelUpdate()
          }
          popup.show(filteredLogList.table, e.x, e.y)
        }
      }
    })
  }

  private fun setupPickedLogsContextActions() {
    pickedLogList.table.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (e.clickCount == 2) {
          val selectedIndex = pickedLogList.table.selectedRow
          val clickedEntry = pickedLogListTableModel.getValueAt(selectedIndex, 0) as LogEntry
          var logIndex = clickedEntry.index

          // if the picked log has the filter, first jump to the filtered log panel
          val targetTable = if (clickedEntry.appliedFilter != null) {
            for (index in 0 until filteredLogList.table.rowCount) {
              if (filteredLogListTableModel.getValueAt(index, 0) == clickedEntry) {
                logIndex = index
                break
              }
            }
            filteredLogList.table
          } else {
            logIndex -= presenter.visibleLogsOffset // Map to the visible index
            logList.table
          }
          SwingUtils.scrollToVisible(targetTable, logIndex)
          targetTable.setRowSelectionInterval(logIndex, logIndex)
        } else if (SwingUtilities.isRightMouseButton(e) && pickedLogList.table.selectedRow != -1) {
          val popup = JPopupMenu()
          val removeItem = popup.add("Remove")
          removeItem.addActionListener {
            pickedLogList.table.selectedRows
              .map { pickedLogListTableModel.getValueAt(it, 0) as LogEntry }
              .forEach { pickedLogListTableModel.removeLog(it) }
            onPickedLogListModelUpdate()
          }
          popup.show(pickedLogList.table, e.x, e.y)
        }
      }
    })
    clearPickedLogButton.addActionListener {
      pickedLogListTableModel.clearLog()
    }
  }

  private fun onPickedLogListModelUpdate() {
    var maxIndex = -1
    for (index in 0 until pickedLogListTableModel.rowCount) {
      val e = pickedLogListTableModel.getValueAt(index, 0) as LogEntry
      if (e.index > maxIndex) {
        maxIndex = e.index
      }
    }
    if (maxIndex != -1) {
        val line = "${(maxIndex + 1)}"
        val width = pickedLogList.font.getStringBounds(line, frc).width.roundToInt()
        pickledLogRenderer.updateLineNumberWidth(width)
    }
  }

  override fun buildStreamsMenu(): JMenu? {
    if (logStreams.isEmpty()) return null

    val streamsMenu = JMenu("Streams")
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
    if (openFiles != null && openFiles.isNotEmpty()) {
      presenter.loadLogs(openFiles)
    }
  }

  override fun handleRefreshLogsMenu() = presenter.refreshLogs()

  override fun handleSaveFilteredLogsMenu() {
    val file = mainView.showSaveLogFileChooser()
    file?.let { presenter.saveFilteredLogs(it) }
  }

  override fun handleOpenFiltersMenu() {
    val filterFiles = mainView.showOpenMultipleFiltersFileChooser()
    if (filterFiles.isNotEmpty()) {
      var keepCurrentFilters = false
      if (!filtersPane.isEmpty) {
        // Ask the user if we should keep the the existing filters
        val dialog = SingleChoiceDialog(
          "There are currently opened filters already.",
          "What do you want to do?",
          arrayOf(
            "Keep existing filters and add the new one(s)",
            "Open just the new filter(s) and close others"
          ),
          0
        )

        val choice = dialog.show(mainView.parent)
        if (choice == SingleChoiceDialog.DIALOG_CANCELLED) {
          return
        }
        keepCurrentFilters = choice == 0
      }

      presenter.loadFilters(filterFiles, keepCurrentFilters)
    }
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
        "If the exact timestamp is not found, it will go to the closest around it...",
        "Go to timestamp...",
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

  override fun onThemeChanged() {
    // Do nothing
  }

  override fun requestFinish(doFinish: () -> Unit) {
    this.doFinish = doFinish
    presenter.finishing()
  }

  override fun configureFiltersList(filters: Map<String, List<Filter>>?) {
    filtersPane.setFilters(HashMap(filters))
  }

  override fun showErrorMessage(message: String?) {
    JOptionPane.showMessageDialog(contentPane, message, "Error...", JOptionPane.ERROR_MESSAGE)
  }

  override fun showSkippedLogsMessage(skippedLogs: List<String>) {
    val message = StringBuilder("There was a problem parsing below files and they were not loaded")
    message.append("\n\n")
    message.append(skippedLogs.joinToString("\n") { "> $it" })
    JOptionPane.showMessageDialog(
      contentPane,
      message.toString(),
      "Some files were not opened",
      JOptionPane.WARNING_MESSAGE
    )
  }

  override fun showLogs(logEntries: List<LogEntry>?) {
    logListTableModel.setLogs(logEntries)
    // calc the line number view needed width
    logEntries?.maxByOrNull { it.index }?.index?.let {
      val line = "${(it + 1)}"
      val width = logList.font.getStringBounds(line, frc).width.roundToInt()
      logRenderer.updateLineNumberWidth(width)
    }
  }

  override fun showCurrentLogsLocation(logsPath: String?) {
    Logger.debug("showCurrentLogsLocation: $logsPath")
    val text = if (logsPath == null) null else SwingUtils.truncateTextFor(
      currentLogsLbl,
      "Logs path:",
      logsPath,
      contentPane.width
    )

    currentLogsLbl.text = text
  }

  override fun showFilteredLogs(logEntries: List<LogEntry>?) {
    logEntries?.let {
      filteredLogListTableModel.setLogs(it)
    }
    logList.updateUI()
    filtersPane.updateUI()

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
      "it was not possible to parse \"$failedInput\".\n" +
          "Please make sure your input is in the correct format\n",
      "Could not parse input",
      JOptionPane.ERROR_MESSAGE
    )
  }

  override fun onAppliedFiltersRemembered() {
    // Just make sure to keep the filters pane updated when they are loaded (if applied)
    filtersPane.updateUI()
  }

  override fun showAvailableLogStreams(logStreams: Set<LogStream>?) {
    this.logStreams.reset(logStreams)

    // We don't need to show the streams menu if there is only one stream
    val showStreams = logStreams != null && logStreams.size > 1

    logRenderer.showStreams(showStreams)
    pickledLogRenderer.showStreams(showStreams)

    // Refresh the menu bar to make sure the streams are shown
    mainView.refreshMenuBar()
  }

  override fun showUnsavedFilterIndication(group: String?) {
    filtersPane.showUnsavedIndication(group, true)
  }

  override fun hideUnsavedFilterIndication(group: String?) {
    filtersPane.showUnsavedIndication(group, false)
  }

  override fun showAskToSaveFilterDialog(group: String?): UserSelection {
    val userChoice = JOptionPane.showConfirmDialog(
      contentPane.parent,
      "$group has unsaved changes. Do you want to save it?",
      "Unsaved changes",
      JOptionPane.YES_NO_CANCEL_OPTION,
      JOptionPane.WARNING_MESSAGE
    )

    return convertFromSwing(userChoice)
  }

  override fun showAskToSaveMultipleFiltersDialog(groups: Array<String>): Array<Boolean>? {
    val dialog = MultipleChoiceDialog(
      "Save modified filter groups?",
      "Select which groups to save",
      groups,
      true
    )

    return dialog.show(mainView.parent)
  }

  override fun showSaveFilters(group: String?) = mainView.showSaveFilterFileChooser(group)

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

  override fun collapseAllGroups() {
    filtersPane.setAllGroupsVisibility(false)
  }

  override fun showStartLoading() = mainView.showStartLoading("Logs")

  override fun showLoadingProgress(progress: Int, note: String?) = mainView.showLoadingProgress("Logs", progress, note)

  override fun finishLoading() = mainView.finishLoading("Logs")

  private fun convertFromSwing(swingOption: Int): UserSelection {
    when (swingOption) {
      JOptionPane.YES_OPTION -> return UserSelection.CONFIRMED
      JOptionPane.NO_OPTION -> return UserSelection.REJECTED
      JOptionPane.CANCEL_OPTION, JOptionPane.CLOSED_OPTION -> return UserSelection.CANCELLED
    }
    throw IllegalArgumentException("Invalid option: $swingOption")
  }

  private fun buildUi() {
    _contentPane = JPanel()
    _contentPane.layout = GridBagLayout()
    _contentPane.preferredSize = Dimension(UIScaleUtils.dip(1000), UIScaleUtils.dip(500))

    currentLogsLbl = JLabel()
    currentLogsLbl.autoscrolls = true
    currentLogsLbl.text = ""
    currentLogsLbl.border = BorderFactory.createEmptyBorder(
      UIScaleUtils.dip(5),
      UIScaleUtils.dip(5),
      UIScaleUtils.dip(5),
      UIScaleUtils.dip(5)
    )
    _contentPane.add(
      currentLogsLbl,
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(0)
        .withAnchor(GridBagConstraints.WEST)
        .build()
    )

    val mainSplitPane = JSplitPane()
    mainSplitPane.dividerSize = UIScaleUtils.dip(5)
    mainSplitPane.isOneTouchExpandable = true
    mainSplitPane.resizeWeight = 0.05
    _contentPane.add(
      mainSplitPane,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(1)
        .withGridWidth(6)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .withAnchor(GridBagConstraints.CENTER)
        .build()
    )

    val mainLogSplit = JSplitPane()
    mainLogSplit.dividerSize = UIScaleUtils.dip(5)
    mainLogSplit.isOneTouchExpandable = true
    mainLogSplit.resizeWeight = 0.8

    logsPane = JSplitPane()
    logsPane.dividerSize = UIScaleUtils.dip(5)
    logsPane.orientation = JSplitPane.VERTICAL_SPLIT
    logsPane.resizeWeight = 0.6

    logListTableModel = LogListTableModel("All Logs")
    logList = SearchableTable(logListTableModel)
    logsPane.leftComponent = logList // Left or above (above in this case)

    filteredLogListTableModel = LogListTableModel("Filtered Logs")
    filteredLogList = SearchableTable(filteredLogListTableModel)
    logsPane.rightComponent = filteredLogList // Right or below (below in this case)

    pickedLogListTableModel = LogListTableModel("Picked Logs")
    pickedLogList = SearchableTable(pickedLogListTableModel)

    pickedLogList.add(JButton("Clear Picked Logs").also { clearPickedLogButton = it },
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(0)
        .withAnchor(GridBagConstraints.WEST)
        .build()
    )

    mainLogSplit.leftComponent = logsPane
    mainLogSplit.rightComponent = pickedLogList

    mainSplitPane.rightComponent = mainLogSplit

    val filtersMainPane = JPanel()
    filtersMainPane.layout = GridBagLayout()
    filtersMainPane.border = BorderFactory.createTitledBorder("Filters")
    val emptyPane = JPanel()
    emptyPane.layout = FlowLayout(FlowLayout.CENTER, 5, 5)
    filtersMainPane.add(
      emptyPane,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(2)
        .withWeightx(1.0)
        .withAnchor(GridBagConstraints.SOUTH)
        .build()
    )

    filtersPane = FiltersList()
    filtersMainPane.add(
      JScrollPane(filtersPane).also { it.verticalScrollBar.unitIncrement = 16 },
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(1)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )

    val filterButtonsPane = JPanel(BorderLayout())
    val filterActionButtonsPane = JPanel()
    collapseExpandAllGroupsBtn = FlatButton()
    addNewFilterGroupBtn = JButton()
    addNewFilterGroupBtn.actionCommand = "Add"
    addNewFilterGroupBtn.text = "New Group"
    moreFilterOptionsBtn = JButton()
    moreFilterOptionsBtn.toolTipText = "More options"
    moreFilterOptionsBtn.text = StringUtils.THREE_LINES

    filterActionButtonsPane.add(addNewFilterGroupBtn)
    filterActionButtonsPane.add(moreFilterOptionsBtn)

    filterButtonsPane.add(collapseExpandAllGroupsBtn, BorderLayout.WEST)
    filterButtonsPane.add(filterActionButtonsPane, BorderLayout.EAST)

    filtersMainPane.add(
      filterButtonsPane,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(0)
        .withInsets(
          Insets(
            UIScaleUtils.dip(0),
            UIScaleUtils.dip(0),
            UIScaleUtils.dip(10),
            UIScaleUtils.dip(5)
          )
        )
        .withFill(GridBagConstraints.BOTH)
        .build()
    )

    mainSplitPane.leftComponent = filtersMainPane
  }
}