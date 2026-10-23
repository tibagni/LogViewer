package com.tibagni.logviewer

import com.formdev.flatlaf.FlatClientProperties
import com.tibagni.logviewer.about.AboutDialog
import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.bugreport.BugReportView
import com.tibagni.logviewer.bugreport.BugReportViewImpl
import com.tibagni.logviewer.filter.Filter
import com.tibagni.logviewer.filter.FiltersPanel
import com.tibagni.logviewer.filter.FiltersPanelHost
import com.tibagni.logviewer.live.LiveLogView
import com.tibagni.logviewer.live.LiveLogViewImpl
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.preferences.KeymapPreferencePanel
import com.tibagni.logviewer.preferences.LogViewerPreferences
import com.tibagni.logviewer.preferences.LogViewerPreferencesDialog
import com.tibagni.logviewer.shortcuts.ShortcutAction
import com.tibagni.logviewer.shortcuts.ShortcutManager
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

interface MainViewTab : View {
  val contentPane: JPanel
  fun attachFiltersPanel(panel: FiltersPanel) {}
  fun onTabSelected() {}
  fun onTabDeselected() {}
  fun onThemeChanged() {}
  fun getMenus(): List<JMenu> = emptyList()
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
) : MainView, FiltersPanelHost {
  override val parentWindow: Frame
    get() = parent

  private lateinit var mainPanel: JPanel

  private var logSaveFileChooser: JFileChooserExt
  private var logOpenFileChooser: JFileChooserExt
  private var filterSaveFileChooser: JFileChooserExt
  private var filterOpenFileChooser: JFileChooserExt
  private val progressDialogs = mutableMapOf<String, ProgressDialog>()

  private val filtersPanel: FiltersPanel
  private val logViewerView: LogViewerView
  private val bugReportView: BugReportView
  private val liveLogView: LiveLogView
  private lateinit var tabbedPane: JTabbedPane

  /**
   * The underlying [JTabbedPane] managing the primary view tabs (Logs, Bug Report, Live Logs).
   *
   * Exposed internally for unit and integration testing to inspect tab configuration,
   * client properties, and visual styles.
   */
  @VisibleForTesting
  internal val mainTabbedPane: JTabbedPane
    get() = tabbedPane

  // Dynamic Menu items
  private var saveFilteredLogs: JMenuItem? = null
  private val shortcutManager = ShortcutManager(userPrefs)
  private val registeredTabStrokes = mutableListOf<KeyStroke>()

  private val tabs: List<MainViewTab>
  private val finishChain: List<View>
    get() = listOf(filtersPanel) + tabs
  private var finishChainPosition = 0

  private val activeTab: MainViewTab?
    get() = if (::tabbedPane.isInitialized) {
      tabs.firstOrNull { it.contentPane === tabbedPane.selectedComponent }
    } else {
      logViewerView
    }

  val contentPane: JPanel
    get() = mainPanel

  init {
    liveLogView = LiveLogViewImpl(this)
    filtersPanel = FiltersPanel(this)
    logViewerView = LogViewerViewImpl(this, filtersPanel, initialLogFiles)
    bugReportView = BugReportViewImpl(this)
    tabs = listOf(logViewerView, bugReportView, liveLogView)

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

      override fun onShortcutsChanged() {
        refreshMenuBar()
        configureTabShortcuts()
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
    tabs.forEach { it.onThemeChanged() }
  }

  private fun recreateFileChoosers() {
    logSaveFileChooser = JFileChooserExt(userPrefs.defaultLogsPath)
    logOpenFileChooser = JFileChooserExt(userPrefs.defaultLogsPath)
    filterSaveFileChooser = JFileChooserExt(userPrefs.defaultFiltersPath)
    filterOpenFileChooser = JFileChooserExt(userPrefs.defaultFiltersPath)
  }

  private fun handleClose() {
    if (finishChainPosition > finishChain.lastIndex) {
      // We went through all views, we can close the app now
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

    val fileMenu = JMenu(I18n.get(I18n.MENU_FILE))
    fileMenu.setMnemonic('F')
    val settingsItem = JMenuItem(I18n.get(I18n.MENU_ITEM_SETTINGS))
    shortcutManager.bindToMenuItem(settingsItem, ShortcutAction.FILE_SETTINGS)
    settingsItem.addActionListener { openUserPreferences() }
    fileMenu.add(settingsItem)

    val logsMenu = JMenu(I18n.get(I18n.MENU_LOGS))
    val openLogsItem = JMenuItem(I18n.get(I18n.MENU_ITEM_OPEN_LOGS))
    shortcutManager.bindToMenuItem(openLogsItem, ShortcutAction.LOGS_OPEN)
    openLogsItem.addActionListener { logViewerView.handleOpenLogsMenu() }
    logsMenu.add(openLogsItem)
    val refreshLogsItem = JMenuItem(I18n.get(I18n.MENU_ITEM_REFRESH))
    shortcutManager.bindToMenuItem(refreshLogsItem, ShortcutAction.LOGS_REFRESH)
    refreshLogsItem.addActionListener { logViewerView.handleRefreshLogsMenu() }
    logsMenu.add(refreshLogsItem)
    val closeLogsItem = JMenuItem(I18n.get(I18n.MENU_ITEM_CLOSE_LOGS))
    shortcutManager.bindToMenuItem(closeLogsItem, ShortcutAction.LOGS_CLOSE)
    closeLogsItem.addActionListener { logViewerView.handleCloseLogsMenu() }
    logsMenu.add(closeLogsItem)

    val changeEncodingMenu = JMenu(I18n.get(I18n.MENU_ENCODING))
    configureCharsetsMenu(changeEncodingMenu)
    logsMenu.add(changeEncodingMenu)

    saveFilteredLogs = JMenuItem(I18n.get(I18n.MENU_ITEM_SAVE_FILTERED_LOGS))
    saveFilteredLogs?.addActionListener { logViewerView.handleSaveFilteredLogsMenu() }
    logsMenu.add(saveFilteredLogs)
    logsMenu.addSeparator()
    val goToTimestampItem = JMenuItem(I18n.get(I18n.MENU_ITEM_GO_TO_TIMESTAMP))
    shortcutManager.bindToMenuItem(goToTimestampItem, ShortcutAction.LOGS_GO_TO_TIMESTAMP)
    goToTimestampItem.addActionListener { logViewerView.handleGoToTimestampMenu() }
    logsMenu.add(goToTimestampItem)
    logsMenu.addSeparator()
    val configureVisibleLogs = JMenuItem(I18n.get(I18n.MENU_ITEM_VISIBLE_LOGS))
    configureVisibleLogs.addActionListener { logViewerView.handleConfigureIgnoredLogs() }
    logsMenu.add(configureVisibleLogs)


    val filtersMenu = JMenu(I18n.get(I18n.MENU_FILTERS))
    val openFilterItem = JMenuItem(I18n.get(I18n.MENU_ITEM_OPEN_FILTERS))
    shortcutManager.bindToMenuItem(openFilterItem, ShortcutAction.FILTERS_OPEN)
    openFilterItem.addActionListener { filtersPanel.handleOpenFiltersMenu() }
    filtersMenu.add(openFilterItem)
    val findFilterItem = JMenuItem(I18n.get(I18n.MENU_ITEM_FIND_FILTERS))
    shortcutManager.bindToMenuItem(findFilterItem, ShortcutAction.FILTERS_FIND_GLOBAL)
    findFilterItem.addActionListener { filtersPanel.showSearchFiltersDialog() }
    filtersMenu.add(findFilterItem)
    val cleanDuplicatesItem = JMenuItem(I18n.get(I18n.MENU_ITEM_CLEAN_DUPLICATE_FILTERS))
    cleanDuplicatesItem.addActionListener { filtersPanel.handleCleanDuplicateFilters() }
    filtersMenu.add(cleanDuplicatesItem)

    val helpMenu = JMenu(I18n.get(I18n.MENU_HELP))
    val shortcutsItem = JMenuItem(I18n.get(I18n.MENU_ITEM_KEYBOARD_SHORTCUTS))
    val aboutItem = JMenuItem(I18n.get(I18n.MENU_ITEM_ABOUT))
    val onlineHelpItem = JMenuItem(I18n.get(I18n.MENU_ITEM_USER_GUIDE))
    shortcutsItem.addActionListener { openKeyboardShortcuts() }
    aboutItem.addActionListener { AboutDialog.showAboutDialog(parent) }
    onlineHelpItem.addActionListener { openUserGuide() }
    helpMenu.add(shortcutsItem)
    helpMenu.add(aboutItem)
    helpMenu.add(onlineHelpItem)

    // Add all menus in order
    menuBar.add(fileMenu)
    menuBar.add(logsMenu)
    menuBar.add(filtersMenu)
    activeTab?.getMenus()?.forEach { menuBar.add(it) }
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

  private fun openKeyboardShortcuts() {
    LogViewerPreferencesDialog.showPreferencesDialog(parent, KeymapPreferencePanel.ID)
  }

  private fun configureTabShortcuts() {
    if (!::mainPanel.isInitialized || !::tabbedPane.isInitialized) return
    registeredTabStrokes.forEach { mainPanel.unregisterKeyboardAction(it) }
    registeredTabStrokes.clear()

    val tabActions = listOf(
      ShortcutAction.NAV_TAB_LOGS to 0,
      ShortcutAction.NAV_TAB_BUGREPORT to 1,
      ShortcutAction.NAV_TAB_LIVE_LOGS to 2
    )
    for ((action, tabIdx) in tabActions) {
      val ks = shortcutManager.getKeyStroke(action)
      mainPanel.registerKeyboardAction(
        {
          if (tabIdx in 0 until tabbedPane.tabCount) {
            tabbedPane.selectedIndex = tabIdx
          }
        },
        ks,
        JComponent.WHEN_IN_FOCUSED_WINDOW
      )
      registeredTabStrokes.add(ks)
    }
  }

  /**
   * Constructs and configures the main user interface hierarchy.
   *
   * Initializes [mainPanel] and [tabbedPane], applying FlatLaf modern tab styling properties
   * including underlined tab type ([FlatClientProperties.TABBED_PANE_TAB_TYPE_UNDERLINED]),
   * tab separators ([FlatClientProperties.TABBED_PANE_SHOW_TAB_SEPARATORS]), and a borderless
   * layout ([FlatClientProperties.TABBED_PANE_HAS_FULL_BORDER]).
   *
   * Mounts the primary application tabs (Logs, Bug Report, and Live Logs), attaches tab selection
   * change listeners to properly route active tab state and menu bar refreshes, and places the
   * tabbed pane within the main layout.
   */
  private fun buildUi() {
    mainPanel = JPanel()
    mainPanel.layout = GridBagLayout()
    mainPanel.preferredSize = Dimension(UIScaleUtils.dip(1000), UIScaleUtils.dip(500))
    tabbedPane = JTabbedPane()
    tabbedPane.putClientProperty(
      FlatClientProperties.TABBED_PANE_TAB_TYPE,
      FlatClientProperties.TABBED_PANE_TAB_TYPE_UNDERLINED
    )
    tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_SHOW_TAB_SEPARATORS, true)
    tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_HAS_FULL_BORDER, false)

    tabbedPane.addTab(I18n.get(I18n.MAIN_TAB_LOGS), logViewerView.contentPane)
    tabbedPane.addTab(I18n.get(I18n.MAIN_TAB_BUG_REPORT), bugReportView.contentPane)
    tabbedPane.addTab(I18n.get(I18n.MAIN_TAB_LIVE_LOGS), liveLogView.contentPane)

    // Ensure the initial tab (Logs) has the filters panel properly attached
    logViewerView.attachFiltersPanel(filtersPanel)

    var lastActiveTab: MainViewTab? = logViewerView
    tabbedPane.addChangeListener {
      lastActiveTab?.onTabDeselected()
      val currentTab = activeTab
      lastActiveTab = currentTab
      currentTab?.let {
        it.attachFiltersPanel(filtersPanel)
        it.onTabSelected()
      }
      refreshMenuBar()
    }

    mainPanel.add(
      tabbedPane, GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(1)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )

    configureTabShortcuts()
  }
}