package com.tibagni.logviewer

import com.tibagni.logviewer.about.AboutDialog
import com.tibagni.logviewer.bugreport.BugReportView
import com.tibagni.logviewer.bugreport.BugReportViewImpl
import com.tibagni.logviewer.filter.Filter
import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.preferences.LogViewerPreferences
import com.tibagni.logviewer.preferences.LogViewerPreferencesDialog
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.JFileChooserExt
import com.tibagni.logviewer.view.ProgressDialog
import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import java.net.URL
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

interface View {
  /**
   * This method is called when the application wants to finish.
   * This is to allow all the views to perform cleanup first.
   *
   * doFinish must be called when the view is ready and the app can be closed
   * if a view does not call doFinish, the application will not be closed.
   * This can happen if  some view displays a dialog to the user for example
   * and the user decides to not leave the application
   */
  fun requestFinish(doFinish: () -> Unit)
}

interface MainView {
  val parent: JFrame

  fun showOpenMultipleLogsFileChooser(): Array<File>?
  fun showOpenSingleLogFileChooser(): File?
  fun showSaveLogFileChooser(): File?
  fun showSaveFilterFileChooser(suggestedFileName: String? = null): File?
  fun showOpenMultipleFiltersFileChooser(): Array<File>

  fun showStartLoading(tag: String)
  fun showLoadingProgress(tag:String, progress: Int, note: String?)
  fun finishLoading(tag: String)

  fun enableSaveFilteredLogsMenu(enabled: Boolean)
  fun refreshMenuBar()

  fun onBugReportLoaded(bugreportPath: String, bugreportText: String)
  fun onBugReportClosed()
}

class MainViewImpl(
  override val parent: JFrame,
  private val userPrefs: LogViewerPreferences,
  initialLogFiles: Set<File>
) : MainView {
  private lateinit var mainPanel: JPanel

  private var logSaveFileChooser: JFileChooserExt
  private var logOpenFileChooser: JFileChooserExt
  private var filterSaveFileChooser: JFileChooserExt
  private var filterOpenFileChooser: JFileChooserExt
  private val progressDialogs = mutableMapOf<String, ProgressDialog>()

  private val logViewerView: LogViewerView
  private val bugReportView: BugReportView

  // Dynamic Menu items
  private var saveFilteredLogs: JMenuItem? = null

  private val finishChain: List<View>
  private var finishChainPosition = 0

  val contentPane: JPanel
    get() = mainPanel

  init {
    logViewerView = LogViewerViewImpl(this, initialLogFiles)
    bugReportView = BugReportViewImpl(this)
    finishChain = listOf(logViewerView, bugReportView)

    buildUi()
    configureMenuBar()

    logSaveFileChooser = JFileChooserExt(userPrefs.defaultLogsPath)
    logOpenFileChooser = JFileChooserExt(userPrefs.defaultLogsPath)
    filterSaveFileChooser = JFileChooserExt(userPrefs.defaultFiltersPath)
    filterOpenFileChooser = JFileChooserExt(userPrefs.defaultFiltersPath)
    userPrefs.addPreferenceListener(object : LogViewerPreferences.Adapter() {
      override fun onDefaultFiltersPathChanged() {
        filterSaveFileChooser.currentDirectory = userPrefs.defaultFiltersPath
        filterOpenFileChooser.currentDirectory = userPrefs.defaultFiltersPath
      }

      override fun onDefaultLogsPathChanged() {
        logOpenFileChooser.currentDirectory = userPrefs.defaultLogsPath
      }
    })

    parent.defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE

    parent.addWindowListener(object : WindowAdapter() {
      override fun windowClosing(e: WindowEvent) {
        finishChainPosition = 0
        handleClose()
      }
    })
  }

  fun themeChanged() {
    recreateFileChoosers()
    bugReportView.onThemeChanged()
    bugReportView.onThemeChanged()
  }

  private fun recreateFileChoosers() {
    logSaveFileChooser = JFileChooserExt(userPrefs.defaultLogsPath)
    logOpenFileChooser = JFileChooserExt(userPrefs.defaultLogsPath)
    filterSaveFileChooser = JFileChooserExt(userPrefs.defaultFiltersPath)
    filterOpenFileChooser = JFileChooserExt(userPrefs.defaultFiltersPath)
  }

  private fun handleClose() {
    if (finishChainPosition > finishChain.lastIndex) {
      // We wen through all views, we can close the app now
      parent.dispose()
      mainScope.cancel()
      return
    }

    val viewToFinish = finishChain[finishChainPosition]
    viewToFinish.requestFinish { finishApplication() }
  }

  private fun finishApplication() {
    // continue on the finishChain
    finishChainPosition++
    handleClose()
  }

  override fun showOpenMultipleLogsFileChooser(): Array<File>? {
    logOpenFileChooser.resetChoosableFileFilters()
    logOpenFileChooser.isMultiSelectionEnabled = true
    logOpenFileChooser.dialogTitle = "Open Logs..."

    val selectedOption = logOpenFileChooser.showOpenDialog(mainPanel)
    return if (selectedOption == JFileChooser.APPROVE_OPTION) {
      logOpenFileChooser.selectedFiles
    } else null
  }

  override fun showOpenSingleLogFileChooser(): File? {
    logOpenFileChooser.resetChoosableFileFilters()
    logOpenFileChooser.isMultiSelectionEnabled = false
    logOpenFileChooser.dialogTitle = "Open Log..."

    val selectedOption = logOpenFileChooser.showOpenDialog(mainPanel)
    return if (selectedOption == JFileChooser.APPROVE_OPTION) {
      logOpenFileChooser.selectedFile
    } else null
  }

  override fun showSaveLogFileChooser(): File? {
    logSaveFileChooser.resetChoosableFileFilters()
    logSaveFileChooser.isMultiSelectionEnabled = false
    logSaveFileChooser.dialogTitle = "Save Filtered Logs..."

    val selectedOption = logSaveFileChooser.showSaveDialog(mainPanel)
    return if (selectedOption == JFileChooser.APPROVE_OPTION) {
      logSaveFileChooser.selectedFile
    } else null
  }

  override fun showSaveFilterFileChooser(suggestedFileName: String?): File? {
    filterSaveFileChooser.resetChoosableFileFilters()
    filterSaveFileChooser.isMultiSelectionEnabled = false
    filterSaveFileChooser.dialogTitle = "Save Filter..."
    filterSaveFileChooser.setSaveExtension(Filter.FILE_EXTENSION)
    if (!suggestedFileName.isNullOrEmpty()) {
      filterSaveFileChooser.selectedFile = File(suggestedFileName)
    } else {
      filterSaveFileChooser.selectedFile = null
    }

    val selectedOption = filterSaveFileChooser.showSaveDialog(mainPanel)
    return if (selectedOption == JFileChooser.APPROVE_OPTION) {
      filterSaveFileChooser.selectedFile
    } else null
  }

  override fun showOpenMultipleFiltersFileChooser(): Array<File> {
    filterOpenFileChooser.resetChoosableFileFilters()
    filterOpenFileChooser.fileFilter = FileNameExtensionFilter("Filter files", Filter.FILE_EXTENSION)
    filterOpenFileChooser.isMultiSelectionEnabled = true
    filterOpenFileChooser.dialogTitle = "Open Filters..."

    val selectedOption = filterOpenFileChooser.showOpenDialog(mainPanel)
    return if (selectedOption == JFileChooser.APPROVE_OPTION) {
      filterOpenFileChooser.selectedFiles
    } else arrayOf()
  }

  override fun showStartLoading(tag: String) {
    var progressDialog = progressDialogs[tag]
    if (progressDialog == null) {
      progressDialog = ProgressDialog.showProgressDialog(parent)
      progressDialogs[tag] = progressDialog
    }
  }

  override fun showLoadingProgress(tag: String, progress: Int, note: String?) {
    val progressDialog = progressDialogs[tag]
    progressDialog?.publishProgress(progress)
    progressDialog?.updateProgressText(note)
  }

  override fun finishLoading(tag: String) {
    progressDialogs[tag]?.finishProgress()
    progressDialogs.remove(tag)
  }

  override fun enableSaveFilteredLogsMenu(enabled: Boolean) {
    saveFilteredLogs?.isEnabled = enabled
  }

  override fun refreshMenuBar() {
    configureMenuBar()
  }

  override fun onBugReportLoaded(bugreportPath: String, bugreportText: String) {
    bugReportView.onBugReportLoaded(bugreportPath, bugreportText)
  }

  override fun onBugReportClosed() {
    bugReportView.onBugReportClosed()
  }

  private fun configureMenuBar() {
    val menuBar = JMenuBar()

    val fileMenu = JMenu("File")
    fileMenu.setMnemonic('F')
    val settingsItem = JMenuItem("Settings")
    settingsItem.accelerator = KeyStroke.getKeyStroke(
      KeyEvent.VK_COMMA, Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx
    )
    settingsItem.addActionListener { openUserPreferences() }
    fileMenu.add(settingsItem)

    val logsMenu = JMenu("Logs")
    val openLogsItem = JMenuItem("Open Logs...")
    openLogsItem.addActionListener { logViewerView.handleOpenLogsMenu() }
    logsMenu.add(openLogsItem)
    val refreshLogsItem = JMenuItem("Refresh...")
    refreshLogsItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)
    refreshLogsItem.addActionListener { logViewerView.handleRefreshLogsMenu() }
    logsMenu.add(refreshLogsItem)
    saveFilteredLogs = JMenuItem("Save Filtered Logs")
    saveFilteredLogs?.addActionListener { logViewerView.handleSaveFilteredLogsMenu() }
    logsMenu.add(saveFilteredLogs)
    logsMenu.addSeparator()
    val goToTimestampItem = JMenuItem("Go to timestamp")
    goToTimestampItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK)
    goToTimestampItem.addActionListener { logViewerView.handleGoToTimestampMenu() }
    logsMenu.add(goToTimestampItem)
    logsMenu.addSeparator()
    val configureVisibleLogs = JMenuItem("Visible logs")
    configureVisibleLogs.addActionListener { logViewerView.handleConfigureIgnoredLogs() }
    logsMenu.add(configureVisibleLogs)


    val filtersMenu = JMenu("Filters")
    val openFilterItem = JMenuItem("Open Filters...")
    openFilterItem.addActionListener { logViewerView.handleOpenFiltersMenu() }
    filtersMenu.add(openFilterItem)

    val helpMenu = JMenu("Help")
    val aboutItem = JMenuItem("About")
    val onlineHelpItem = JMenuItem("User Guide")
    aboutItem.addActionListener { AboutDialog.showAboutDialog(parent) }
    onlineHelpItem.addActionListener { openUserGuide() }
    helpMenu.add(aboutItem)
    helpMenu.add(onlineHelpItem)

    // Build menus specific to child views
    val streamsMenu = logViewerView.buildStreamsMenu()

    // Add all menus in order
    menuBar.add(fileMenu)
    menuBar.add(logsMenu)
    menuBar.add(logsMenu)
    menuBar.add(filtersMenu)
    streamsMenu?.let { menuBar.add(it) }
    menuBar.add(helpMenu)

    parent.jMenuBar = menuBar
    menuBar.revalidate()
    menuBar.repaint()
  }


  private fun openUserGuide() {
    try {
      Desktop.getDesktop().browse(URL(AppInfo.USER_GUIDE_URL).toURI())
    } catch (e: IOException) {
      Logger.error("Failed to open online help", e)
    } catch (e: URISyntaxException) {
      Logger.error("Failed to open online help", e)
    }
  }

  private fun openUserPreferences() {
    LogViewerPreferencesDialog.showPreferencesDialog(parent)
  }

  private fun buildUi() {
    mainPanel = JPanel()
    mainPanel.layout = GridBagLayout()
    mainPanel.preferredSize = Dimension(UIScaleUtils.dip(1000), UIScaleUtils.dip(500))
    val tabbedPane = JTabbedPane()

    tabbedPane.addTab("Logs", logViewerView.contentPane)
    tabbedPane.addTab("Bug Report", bugReportView.contentPane)
    mainPanel.add(
      tabbedPane, GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(1)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )
  }
}