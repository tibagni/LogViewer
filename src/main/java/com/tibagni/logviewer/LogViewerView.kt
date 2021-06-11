package com.tibagni.logviewer

import com.tibagni.logviewer.LogViewerPresenter.UserSelection
import com.tibagni.logviewer.filter.EditFilterDialog
import com.tibagni.logviewer.filter.Filter
import com.tibagni.logviewer.filter.FiltersList
import com.tibagni.logviewer.filter.FiltersList.FiltersListener
import com.tibagni.logviewer.log.LogCellRenderer
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogListTableModel
import com.tibagni.logviewer.log.LogStream
import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.util.StringUtils
import com.tibagni.logviewer.util.SwingUtils
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.FileDrop
import com.tibagni.logviewer.view.FlatButton
import com.tibagni.logviewer.view.SingleChoiceDialog
import com.tibagni.logviewer.view.Toast
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.*
import java.io.File
import java.lang.StringBuilder
import javax.swing.*

// This is the interface known by other views (MainView)
interface LogViewerView : View {
  val contentPane: JPanel
  fun buildStreamsMenu(): JMenu?
  fun handleOpenLogsMenu()
  fun handleRefreshLogsMenu()
  fun handleSaveFilteredLogsMenu()
  fun handleOpenFiltersMenu()
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
  fun showAvailableLogStreams(logStreams: Set<LogStream>?)
  fun showUnsavedFilterIndication(group: String?)
  fun hideUnsavedFilterIndication(group: String?)
  fun showAskToSaveFilterDialog(group: String?): UserSelection
  fun showSaveFilters(group: String?): File?
  fun finish()
  fun showNavigationNextOver()
  fun showNavigationPrevOver()
  fun showOpenPotentialBugReport(bugreportText: String)
  fun closeCurrentlyOpenedBugReports()
}

class LogViewerViewImpl(private val mainView: MainView, initialLogFiles: Set<File>) : LogViewerView,
  LogViewerPresenterView {
  private val presenter: LogViewerPresenterImpl

  private lateinit var logList: JTable
  private lateinit var filteredLogList: JTable
  private lateinit var addNewFilterGroupBtn: JButton
  private lateinit var logsPane: JSplitPane
  private lateinit var currentLogsLbl: JLabel
  private lateinit var filtersPane: FiltersList

  private lateinit var logListTableModel: LogListTableModel
  private lateinit var filteredLogListTableModel: LogListTableModel
  private val logRenderer: LogCellRenderer

  private lateinit var _contentPane: JPanel
  override val contentPane: JPanel
    get() = _contentPane

  private val logStreams: HashSet<LogStream> = HashSet()
  private var doFinish: (() -> Unit)? = null

  init {
    buildUi()

    presenter = LogViewerPresenterImpl(
      this,
      ServiceLocator.logViewerPrefs,
      ServiceLocator.logsRepository,
      ServiceLocator.filtersRepository
    )
    presenter.init()

    logRenderer = LogCellRenderer()

    addNewFilterGroupBtn.addActionListener { addGroup() }
    setupFiltersContextActions()

    logList.setDefaultRenderer(LogEntry::class.java, logRenderer)
    filteredLogList.setDefaultRenderer(LogEntry::class.java, logRenderer)
    setupLogsContextActions()
    setupFilteredLogsContextActions()

    // Configure file drop

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

  private fun addGroup() {
    val newGroupName = JOptionPane.showInputDialog(
      mainView.parent,
      "What is the name of your new Filters Group?",
      "New Filters Group",
      JOptionPane.PLAIN_MESSAGE
    )
    if (!StringUtils.isEmpty(newGroupName)) {
      // If this name is already taken, a number will be appended to the end of the name
      val addedGroupName = presenter.addGroup(newGroupName)
      addFilter(addedGroupName)
    }
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

      override fun onDeleteGroup(group: String) {
        val userChoice = JOptionPane.showConfirmDialog(
          mainView.parent,
          "Are you sure you want to delete this whole group?",
          "Are you sure?",
          JOptionPane.YES_NO_OPTION,
          JOptionPane.WARNING_MESSAGE
        )
        if (userChoice != JOptionPane.YES_NO_OPTION) return
        presenter.removeGroup(group)
      }

      override fun onNavigateNextFilteredLog(filter: Filter) {
        val selectedFilteredLog = filteredLogList.selectedRow
        val filteredLogIdx = presenter.getNextFilteredLogForFilter(filter, selectedFilteredLog)
        if (filteredLogIdx != -1) {
          SwingUtils.scrollToVisible(filteredLogList, filteredLogIdx)
          filteredLogList.setRowSelectionInterval(filteredLogIdx, filteredLogIdx)
        }
      }

      override fun onNavigatePrevFilteredLog(filter: Filter) {
        val selectedFilteredLog = filteredLogList.selectedRow
        val filteredLogIdx = presenter.getPrevFilteredLogForFilter(filter, selectedFilteredLog)
        if (filteredLogIdx != -1) {
          SwingUtils.scrollToVisible(filteredLogList, filteredLogIdx)
          filteredLogList.setRowSelectionInterval(filteredLogIdx, filteredLogIdx)
        }
      }

      override fun onAddFilter(group: String) {
        addFilter(group)
      }

      override fun onSaveFilters(group: String) {
        saveFilter(group)
      }
    })
  }

  private fun saveFilter(filtersGroup: String) = presenter.saveFilters(filtersGroup)

  private fun setupLogsContextActions() {
    logList.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (SwingUtilities.isRightMouseButton(e) && logList.selectedRowCount == 1) {
          val popup = JPopupMenu()
          val createFilterItem = popup.add("Create Filter from this line...")
          createFilterItem.addActionListener {
            val entry = logListTableModel.getValueAt(logList.selectedRow, 0) as LogEntry
            addFilterFromLogLine(entry.logText)
          }
          popup.show(logList, e.x, e.y)
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
    filteredLogList.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (e.clickCount == 2) {
          val selectedIndex = filteredLogList.selectedRow
          val clickedEntry = filteredLogListTableModel.getValueAt(selectedIndex, 0) as LogEntry
          val logIndex = clickedEntry.index
          SwingUtils.scrollToVisible(logList, logIndex)
          logList.setRowSelectionInterval(logIndex, logIndex)
        }
      }
    })
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
    filteredLogListTableModel.setLogs(logEntries)
    logList.updateUI()
    filtersPane.updateUI()

    // Update the save menu option availability
    mainView.enableSaveFilteredLogsMenu(logEntries?.isNotEmpty() ?: false)
  }

  override fun showAvailableLogStreams(logStreams: Set<LogStream>?) {
    this.logStreams.reset(logStreams)

    // We don't need to show the streams menu if there is only one stream
    val showStreams = logStreams != null && logStreams.size > 1

    logRenderer.showStreams(showStreams)

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

  override fun showSaveFilters(group: String?) = mainView.showSaveFilterFileChooser()

  override fun finish() {
    // We can now tell the application we can close
    doFinish?.invoke()
  }

  override fun showNavigationNextOver() =
    Toast.showToast(contentPane.parent, StringUtils.LEFT_ARROW_WITH_HOOK, Toast.LENGTH_SHORT)

  override fun showNavigationPrevOver() =
    Toast.showToast(contentPane.parent, StringUtils.RIGHT_ARROW_WITH_HOOK, Toast.LENGTH_SHORT)

  override fun showOpenPotentialBugReport(bugreportText: String) {
    mainView.onBugReportLoaded(bugreportText)
  }

  override fun closeCurrentlyOpenedBugReports() {
    mainView.onBugReportClosed()
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
        .build()
    )

    logsPane = JSplitPane()
    logsPane.dividerSize = UIScaleUtils.dip(5)
    logsPane.orientation = JSplitPane.VERTICAL_SPLIT
    logsPane.resizeWeight = 0.6

    logListTableModel = LogListTableModel("All Logs")
    logList = JTable(logListTableModel)
    logsPane.leftComponent = JScrollPane(logList) // Left or above (above in this case)


    filteredLogListTableModel = LogListTableModel("Filtered Logs")
    filteredLogList = JTable(filteredLogListTableModel)
    logsPane.rightComponent = JScrollPane(filteredLogList) // Right or below (below in this case)


    mainSplitPane.rightComponent = logsPane

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

    val filterButtonsPane = JPanel()
    filterButtonsPane.layout = FlowLayout(FlowLayout.CENTER)
    addNewFilterGroupBtn = FlatButton()
    addNewFilterGroupBtn.actionCommand = "Add"
    addNewFilterGroupBtn.text = "New Group"
    filterButtonsPane.add(addNewFilterGroupBtn)

    filtersMainPane.add(
      filterButtonsPane,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(0)
        .withWeightx(1.0)
        .withFill(GridBagConstraints.VERTICAL)
        .build()
    )

    mainSplitPane.leftComponent = filtersMainPane
  }
}