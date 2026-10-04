package com.tibagni.logviewer

import com.tibagni.logviewer.about.AboutDialog
import com.tibagni.logviewer.bugreport.BugReportView
import com.tibagni.logviewer.bugreport.BugReportViewImpl
import com.tibagni.logviewer.filter.Filter
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.preferences.LogViewerPreferences
import com.tibagni.logviewer.preferences.LogViewerPreferencesDialog
import com.tibagni.logviewer.util.SwingUtils
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
import java.nio.charset.StandardCharsets
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
    logViewerView.onThemeChanged()
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
    logOpenFileChooser.dialogTitle = I18n.get(I18n.MAIN_FILECHOOSER_OPEN_LOGS)

    val selectedOption = logOpenFileChooser.showOpenDialog(mainPanel)
    return if (selectedOption == JFileChooser.APPROVE_OPTION) {
      logOpenFileChooser.selectedFiles
    } else null
  }

  override fun showOpenSingleLogFileChooser(): File? {
    logOpenFileChooser.resetChoosableFileFilters()
    logOpenFileChooser.isMultiSelectionEnabled = false
    logOpenFileChooser.dialogTitle = I18n.get(I18n.MAIN_FILECHOOSER_OPEN_LOG)

    val selectedOption = logOpenFileChooser.showOpenDialog(mainPanel)
    return if (selectedOption == JFileChooser.APPROVE_OPTION) {
      logOpenFileChooser.selectedFile
    } else null
  }

  override fun showSaveLogFileChooser(): File? {
    logSaveFileChooser.resetChoosableFileFilters()
    logSaveFileChooser.isMultiSelectionEnabled = false
    logSaveFileChooser.dialogTitle = I18n.get(I18n.MAIN_FILECHOOSER_SAVE_FILTERED_LOGS)

    val selectedOption = logSaveFileChooser.showSaveDialog(mainPanel)
    return if (selectedOption == JFileChooser.APPROVE_OPTION) {
      logSaveFileChooser.selectedFile
    } else null
  }

  override fun showSaveFilterFileChooser(suggestedFileName: String?): File? {
    filterSaveFileChooser.resetChoosableFileFilters()
    filterSaveFileChooser.isMultiSelectionEnabled = false
    filterSaveFileChooser.dialogTitle = I18n.get(I18n.MAIN_FILECHOOSER_SAVE_FILTER)
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
    filterOpenFileChooser.fileFilter = FileNameExtensionFilter(I18n.get(I18n.MAIN_FILECHOOSER_FILTER_FILES), Filter.FILE_EXTENSION)
    filterOpenFileChooser.isMultiSelectionEnabled = true
    filterOpenFileChooser.dialogTitle = I18n.get(I18n.MAIN_FILECHOOSER_OPEN_FILTERS)

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

    val menuShortcutMask = SwingUtils.getMenuShortcutKeyMask()

    val fileMenu = JMenu(I18n.get(I18n.MENU_FILE))
    fileMenu.setMnemonic('F')
    val settingsItem = JMenuItem(I18n.get(I18n.MENU_ITEM_SETTINGS))
    settingsItem.accelerator = KeyStroke.getKeyStroke(
      KeyEvent.VK_COMMA, menuShortcutMask
    )
    settingsItem.addActionListener { openUserPreferences() }
    fileMenu.add(settingsItem)

    val logsMenu = JMenu(I18n.get(I18n.MENU_LOGS))
    val openLogsItem = JMenuItem(I18n.get(I18n.MENU_ITEM_OPEN_LOGS))
    openLogsItem.addActionListener { logViewerView.handleOpenLogsMenu() }
    logsMenu.add(openLogsItem)
    val refreshLogsItem = JMenuItem(I18n.get(I18n.MENU_ITEM_REFRESH))
    refreshLogsItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)
    refreshLogsItem.addActionListener { logViewerView.handleRefreshLogsMenu() }
    logsMenu.add(refreshLogsItem)

    val changeEncodingMenu = JMenu(I18n.get(I18n.MENU_ENCODING))
    configureCharsetsMenu(changeEncodingMenu)
    logsMenu.add(changeEncodingMenu)

    saveFilteredLogs = JMenuItem(I18n.get(I18n.MENU_ITEM_SAVE_FILTERED_LOGS))
    saveFilteredLogs?.addActionListener { logViewerView.handleSaveFilteredLogsMenu() }
    logsMenu.add(saveFilteredLogs)
    logsMenu.addSeparator()
    val goToTimestampItem = JMenuItem(I18n.get(I18n.MENU_ITEM_GO_TO_TIMESTAMP))
    goToTimestampItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK)
    goToTimestampItem.addActionListener { logViewerView.handleGoToTimestampMenu() }
    logsMenu.add(goToTimestampItem)
    logsMenu.addSeparator()
    val configureVisibleLogs = JMenuItem(I18n.get(I18n.MENU_ITEM_VISIBLE_LOGS))
    configureVisibleLogs.addActionListener { logViewerView.handleConfigureIgnoredLogs() }
    logsMenu.add(configureVisibleLogs)


    val filtersMenu = JMenu(I18n.get(I18n.MENU_FILTERS))
    val openFilterItem = JMenuItem(I18n.get(I18n.MENU_ITEM_OPEN_FILTERS))
    openFilterItem.addActionListener { logViewerView.handleOpenFiltersMenu() }
    filtersMenu.add(openFilterItem)
    val findFilterItem = JMenuItem(I18n.get(I18n.MENU_ITEM_FIND_FILTERS))
    findFilterItem.accelerator = KeyStroke.getKeyStroke(
      KeyEvent.VK_F,
      menuShortcutMask or InputEvent.SHIFT_DOWN_MASK
    )
    findFilterItem.addActionListener { logViewerView.handleFindFiltersMenu() }
    filtersMenu.add(findFilterItem)
    val cleanDuplicatesItem = JMenuItem(I18n.get(I18n.MENU_ITEM_CLEAN_DUPLICATE_FILTERS))
    cleanDuplicatesItem.addActionListener { logViewerView.handleCleanDuplicateFilters() }
    filtersMenu.add(cleanDuplicatesItem)

    val helpMenu = JMenu(I18n.get(I18n.MENU_HELP))
    val aboutItem = JMenuItem(I18n.get(I18n.MENU_ITEM_ABOUT))
    val onlineHelpItem = JMenuItem(I18n.get(I18n.MENU_ITEM_USER_GUIDE))
    aboutItem.addActionListener { AboutDialog.showAboutDialog(parent) }
    onlineHelpItem.addActionListener { openUserGuide() }
    helpMenu.add(aboutItem)
    helpMenu.add(onlineHelpItem)

    // Build menus specific to child views
    val streamsMenu = logViewerView.buildStreamsMenu()

    // Add all menus in order
    menuBar.add(fileMenu)
    menuBar.add(logsMenu)
    menuBar.add(filtersMenu)
    streamsMenu?.let { menuBar.add(it) }
    menuBar.add(helpMenu)

    parent.jMenuBar = menuBar
    menuBar.revalidate()
    menuBar.repaint()
  }

  private fun configureCharsetsMenu(menu: JMenu) {
    val asciiSubMenuItem = JMenuItem(I18n.get(I18n.MENU_ITEM_ENCODING_ASCII))
    asciiSubMenuItem.addActionListener { logViewerView.handleChangeCharsetMenu(StandardCharsets.US_ASCII) }
    val latinSubMenuItem = JMenuItem(I18n.get(I18n.MENU_ITEM_ENCODING_LATIN))
    latinSubMenuItem.addActionListener { logViewerView.handleChangeCharsetMenu(StandardCharsets.ISO_8859_1) }
    val utf8SubMenuItem = JMenuItem(I18n.get(I18n.MENU_ITEM_ENCODING_UTF8))
    utf8SubMenuItem.addActionListener { logViewerView.handleChangeCharsetMenu(StandardCharsets.UTF_8) }
    val utf16beSubMenuItem = JMenuItem(I18n.get(I18n.MENU_ITEM_ENCODING_UTF16_BE))
    utf16beSubMenuItem.addActionListener { logViewerView.handleChangeCharsetMenu(StandardCharsets.UTF_16BE) }
    val utf16leSubMenuItem = JMenuItem(I18n.get(I18n.MENU_ITEM_ENCODING_UTF16_LE))
    utf16leSubMenuItem.addActionListener { logViewerView.handleChangeCharsetMenu(StandardCharsets.UTF_16LE) }
    val utf16SubMenuItem = JMenuItem(I18n.get(I18n.MENU_ITEM_ENCODING_UTF16))
    utf16SubMenuItem.addActionListener { logViewerView.handleChangeCharsetMenu(StandardCharsets.UTF_16) }
    menu.add(asciiSubMenuItem)
    menu.add(latinSubMenuItem)
    menu.add(utf8SubMenuItem)
    menu.add(utf16beSubMenuItem)
    menu.add(utf16leSubMenuItem)
    menu.add(utf16SubMenuItem)
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

    tabbedPane.addTab(I18n.get(I18n.MAIN_TAB_LOGS), logViewerView.contentPane)
    tabbedPane.addTab(I18n.get(I18n.MAIN_TAB_BUG_REPORT), bugReportView.contentPane)
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