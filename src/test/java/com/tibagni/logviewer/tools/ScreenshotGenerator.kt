package com.tibagni.logviewer.tools

import com.tibagni.logviewer.*
import com.tibagni.logviewer.adb.AdbDevice
import com.tibagni.logviewer.adb.DeviceState
import com.tibagni.logviewer.bugreport.BugReport
import com.tibagni.logviewer.bugreport.BugReportViewImpl
import com.tibagni.logviewer.bugreport.content.AppPackage
import com.tibagni.logviewer.bugreport.section.BugReportSection
import com.tibagni.logviewer.bugreport.section.BugreportInfoSection
import com.tibagni.logviewer.bugreport.section.PackagesSection
import com.tibagni.logviewer.bugreport.section.SectionNames
import com.tibagni.logviewer.bugreport.section.ui.ListSectionPanel
import com.tibagni.logviewer.filter.*
import com.tibagni.logviewer.filter.regex.RegexEditorDialog
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.live.LiveLogViewImpl
import com.tibagni.logviewer.log.*
import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.preferences.LogViewerPreferencesImpl
import com.tibagni.logviewer.rc.LogLevelConfig
import com.tibagni.logviewer.rc.RuntimeConfiguration
import com.tibagni.logviewer.rc.UIScaleConfig
import com.tibagni.logviewer.util.LogFontManager
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.SearchableTable
import java.awt.*
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import java.io.File
import java.util.prefs.AbstractPreferences
import javax.imageio.ImageIO
import javax.swing.*

/**
 * Automated screenshot generator for LogViewer documentation (`README.md` and `gh-pages`).
 *
 * Usage:
 *   ./gradlew generateScreenshots [-PoutputDir=/path/to/output]
 */
object ScreenshotGenerator {

  private class InMemoryPreferences : AbstractPreferences(null, "") {
    private val map = mutableMapOf<String, String>()
    override fun putSpi(key: String, value: String) { map[key] = value }
    override fun getSpi(key: String): String? = map[key]
    override fun removeSpi(key: String) { map.remove(key) }
    override fun removeNodeSpi() { map.clear() }
    override fun keysSpi(): Array<String> = map.keys.toTypedArray()
    override fun childrenNamesSpi(): Array<String> = emptyArray()
    override fun childSpi(name: String): AbstractPreferences = InMemoryPreferences()
    override fun syncSpi() {}
    override fun flushSpi() {}
  }

  @JvmStatic
  fun main(args: Array<String>) {
    // Force 1x UI scaling so 1920x1080 screenshots are crisp and consistent across HiDPI / standard displays
    System.setProperty("flatlaf.uiScale", "1.0")
    System.setProperty("sun.java2d.uiScale", "1.0")

    val outputDir = File(args.firstOrNull() ?: "docs/images")
    outputDir.mkdirs()

    // Read the user's configured theme before switching to isolated in-memory preferences
    // (we keep the default 1x log font size so 1920x1080 screenshots aren't oversized on HiDPI setups)
    val realPrefs = ServiceLocator.logViewerPrefs
    val savedTheme = realPrefs.lookAndFeel.ifBlank { "Dark" }

    LogViewerPreferencesImpl.setMockPreferences(InMemoryPreferences())
    realPrefs.lookAndFeel = savedTheme

    LogFontManager.init()
    RuntimeConfiguration.initialize()
    UIScaleUtils.initialize(RuntimeConfiguration.getConfig(RuntimeConfiguration.UI_SCALE, UIScaleConfig::class.java))
    UIScaleUtils.updateScaleFactor(1)
    Logger.initialize(RuntimeConfiguration.getConfig(RuntimeConfiguration.LOG_LEVEL, LogLevelConfig::class.java))

    try {
      SwingUtilities.invokeAndWait {
        initLookAndFeel()
        UIScaleUtils.updateScaleFactor(1)

        println("Generating screenshots into: ${outputDir.absolutePath}")

        generateMainAndSubScreenshots(outputDir)
        generateLiveLogsScreenshot(outputDir)
        generateBugReportScreenshot(outputDir)
        generateDialogScreenshots(outputDir)

        println("Successfully generated all screenshots in ${outputDir.absolutePath}")
      }
    } catch (t: Throwable) {
      t.printStackTrace()
      System.exit(1)
    } finally {
      System.exit(0)
    }
  }

  private fun initLookAndFeel() {
    System.setProperty("flatlaf.useWindowDecorations", "true")
    System.setProperty("flatlaf.menuBarEmbedded", "true")
    JFrame.setDefaultLookAndFeelDecorated(true)
    JDialog.setDefaultLookAndFeelDecorated(true)

    val prefs = ServiceLocator.logViewerPrefs
    val themeManager = ServiceLocator.themeManager
    val targetTheme = prefs.lookAndFeel.ifBlank { "Dark" }
    themeManager.setAccentColor(prefs.accentColor)
    themeManager.currentTheme = targetTheme
  }

  private fun generateMainAndSubScreenshots(outputDir: File) {
    val frame = createMainFrame()
    val mainView = MainViewImpl(frame, ServiceLocator.logViewerPrefs, emptySet())
    frame.contentPane = mainView.contentPane
    frame.setSize(1920, 1080)
    realizeWindow(frame)

    val logViewerView = getPrivateField<LogViewerViewImpl>(mainView, "logViewerView")
    val filtersPanel = getPrivateField<FiltersPanel>(mainView, "filtersPanel")
    val mainSplitPane = getPrivateField<JSplitPane>(logViewerView, "mainSplitPane")
    val mainLogSplit = getPrivateField<JSplitPane>(logViewerView, "mainLogSplit")
    val logsPane = getPrivateField<JSplitPane>(logViewerView, "logsPane")
    val allLogsTable = getPrivateField<SearchableTable>(logViewerView, "logList")
    val filteredLogsTable = getPrivateField<SearchableTable>(logViewerView, "filteredLogList")

    // Populate filter groups
    val filterGroups = buildSampleFilterGroups()
    val contextTag = logViewerView.presenter.filterContextTag
    populateContextMatches(filterGroups, contextTag)

    filtersPanel.activeFilterContextTag = contextTag
    filtersPanel.filtersPane.setFilters(filterGroups)

    // Collapse most groups, keep "All", "Performance.filter", and "Telephony-RIL-SIM.filter" open for main_view
    setExpandedGroups(
      filtersPanel.filtersPane,
      setOf("All", "Performance.filter", "Telephony-RIL-SIM.filter")
    )

    // Populate logs, filtered logs, and My Logs
    val allLogs = buildSampleAllLogs()
    val filteredLogs = buildSampleFilteredLogs(filterGroups)
    val myLogs = buildSampleMyLogs(filterGroups)

    logViewerView.showCurrentLogsLocation("C:\\Users\\TBAGNI\\Downloads\\AndroidLogs\\Logs\\")
    val availableStreams = linkedSetOf(
      LogStream.KERNEL,
      LogStream.MAIN,
      LogStream.EVENTS,
      LogStream.SYSTEM,
      LogStream.RADIO,
      LogStream.UNKNOWN
    )
    val allowedStreamsMap = getPrivateField<MutableMap<LogStream, Boolean>>(logViewerView.presenter, "allowedStreamsMap")
    availableStreams.forEach { allowedStreamsMap[it] = true }
    logViewerView.showAvailableLogStreams(availableStreams)
    logViewerView.showLogs(allLogs)
    logViewerView.showFilteredLogs(filteredLogs)
    logViewerView.showMyLogs(myLogs)

    mainSplitPane.dividerLocation = 460
    layoutWindowTree(frame)
    mainLogSplit.setDividerLocation(0.66)
    logsPane.setDividerLocation(0.55)
    layoutWindowTree(frame)

    // 1. main_view.png
    saveWindowImage(frame, File(outputDir, "main_view.png"))

    // 2. filter-details.png (crop of Performance.filter + Settings-SIM.filter in FiltersPanel)
    setExpandedGroups(filtersPanel.filtersPane, setOf("Performance.filter"))
    // Mark all filters in Performance.filter as applied for filter-details.png
    filterGroups["Performance.filter"]?.forEach { it.isApplied = true }
    filtersPanel.updateFiltersUI()
    layoutWindowTree(frame)

    val fullMainForFilterCrop = renderWindow(frame)
    val perfGroupComp = getFilterUIGroup(filtersPanel.filtersPane, "Performance.filter")
    val settingsGroupComp = getFilterUIGroup(filtersPanel.filtersPane, "Settings-SIM.filter")
    if (perfGroupComp != null && settingsGroupComp != null) {
      val p1 = SwingUtilities.convertPoint(perfGroupComp, Point(0, 0), frame.rootPane)
      val p2 = SwingUtilities.convertPoint(settingsGroupComp, Point(0, settingsGroupComp.height), frame.rootPane)
      val cropW = minOf(perfGroupComp.width + 130, fullMainForFilterCrop.width - p1.x)
      val cropH = minOf(p2.y - p1.y, fullMainForFilterCrop.height - p1.y)
      val cropped = fullMainForFilterCrop.getSubimage(maxOf(0, p1.x), maxOf(0, p1.y), cropW, cropH)
      saveImage(cropped, File(outputDir, "filter-details.png"))
    }

    // 3. stream-indication.png (crop showing stream indicator strip and 's'/'m' letters)
    val streamLogs = buildStreamIndicationLogs()
    logViewerView.showMyLogs(emptyList())
    logViewerView.showLogs(streamLogs)
    layoutWindowTree(frame)
    val fullStreamImg = renderWindow(frame)
    val tablePt = SwingUtilities.convertPoint(allLogsTable.table, Point(0, 0), frame.rootPane)
    val sx = maxOf(0, tablePt.x - 100)
    val sy = maxOf(0, tablePt.y)
    val sw = minOf(frame.rootPane.width - sx, allLogsTable.table.width + 120)
    val sh = minOf(330, fullStreamImg.height - sy)
    saveImage(fullStreamImg.getSubimage(sx, sy, sw, sh), File(outputDir, "stream-indication.png"))

    // 4. selecting-streams.png (Streams menu open in menu bar)
    logViewerView.showLogs(allLogs)
    logViewerView.showFilteredLogs(filteredLogs)
    layoutWindowTree(frame)
    generateSelectingStreamsScreenshot(frame, outputDir)

    // 5. log-context-menu.png, visible-logs-menu.png & elapsed-time-menu.png
    generateContextMenuScreenshots(frame, logViewerView, filtersPanel, allLogsTable, filteredLogsTable, outputDir)

    // 6. search-bar.png (Search bars open in both All Logs and Filtered Logs)
    setExpandedGroups(filtersPanel.filtersPane, emptySet())
    logViewerView.showMyLogs(emptyList())
    val searchAllLogs = buildSearchAllLogs()
    val searchFilteredLogs = buildSearchFilteredLogs()
    logViewerView.showLogs(searchAllLogs)
    logViewerView.showFilteredLogs(searchFilteredLogs)
    allLogsTable.table.setRowSelectionInterval(5, 5)
    filteredLogsTable.table.setRowSelectionInterval(1, 1)

    allLogsTable.searchOptionPanel.isVisible = true
    allLogsTable.searchText.text = "google"
    LogCellRenderer.setSearchFilter(
      allLogsTable.table,
      Filter("search", "google", Color.RED, LogLevel.VERBOSE, false)
    )
    val allSearchResultLbl = getPrivateField<JLabel>(allLogsTable, "searchResult")
    allSearchResultLbl.text = "  60,149 results  "

    filteredLogsTable.searchOptionPanel.isVisible = true
    filteredLogsTable.searchText.text = "Carrier"
    LogCellRenderer.setSearchFilter(
      filteredLogsTable.table,
      Filter("search", "Carrier", Color.RED, LogLevel.VERBOSE, false)
    )
    val filteredSearchResultLbl = getPrivateField<JLabel>(filteredLogsTable, "searchResult")
    filteredSearchResultLbl.text = "  747 results  "

    layoutWindowTree(frame)
    saveWindowImage(frame, File(outputDir, "search-bar.png"))

    frame.dispose()
  }

  private fun generateSelectingStreamsScreenshot(frame: JFrame, outputDir: File) {
    val menuBar = frame.jMenuBar
    var streamsMenu: JMenu? = null
    for (i in 0 until menuBar.menuCount) {
      val m = menuBar.getMenu(i)
      if (m != null && m.text == I18n.get(I18n.MENU_STREAMS)) {
        streamsMenu = m
        break
      }
    }
    if (streamsMenu == null) return

    streamsMenu.model.isSelected = true
    streamsMenu.model.isArmed = true
    val popup = streamsMenu.popupMenu
    popup.pack()
    realizeComponent(popup)

    val baseImg = renderWindow(frame)
    val g2 = baseImg.createGraphics()
    val menuPt = SwingUtilities.convertPoint(streamsMenu, Point(0, streamsMenu.height), frame.rootPane)
    g2.translate(menuPt.x, menuPt.y)
    popup.paint(g2)
    g2.dispose()

    val cropX = maxOf(0, menuPt.x - 50)
    val cropY = 0
    val cropW = minOf(360, baseImg.width - cropX)
    val cropH = minOf(menuPt.y + popup.height + 65, baseImg.height)
    saveImage(baseImg.getSubimage(cropX, cropY, cropW, cropH), File(outputDir, "selecting-streams.png"))
    streamsMenu.model.isSelected = false
    streamsMenu.model.isArmed = false
  }

  private fun generateContextMenuScreenshots(
    frame: JFrame,
    logViewerView: LogViewerViewImpl,
    filtersPanel: FiltersPanel,
    allLogsTable: SearchableTable,
    filteredLogsTable: SearchableTable,
    outputDir: File
  ) {
    val model = getPrivateField<LogListTableModel>(logViewerView, "logListTableModel")
    allLogsTable.table.setRowSelectionInterval(1, 1)
    layoutWindowTree(frame)

    val popup = createPopupMenu(
      table = allLogsTable.table,
      model = model,
      parentWindow = frame,
      filtersPanelProvider = { filtersPanel },
      addSelectionActions = { p, _ ->
        p.add(I18n.get(I18n.LOGS_MENU_ADD_TO_MY_LOGS))
      },
      addExtraSingleEntryActions = { p, _ ->
        p.add(JSeparator())
        p.add(I18n.get(I18n.LOGS_MENU_IGNORE_BEFORE))
        p.add(I18n.get(I18n.LOGS_MENU_IGNORE_AFTER))
      }
    )
    popup.pack()
    realizeComponent(popup)

    val baseImg = renderWindow(frame)
    val tablePt = SwingUtilities.convertPoint(allLogsTable.table, Point(0, 0), frame.rootPane)
    val rowRect = allLogsTable.table.getCellRect(1, 0, true)

    // Render popup onto a copy for log-context-menu.png
    val imgContext = copyImage(baseImg)
    val popX = tablePt.x + 170
    val popY = tablePt.y + rowRect.y + rowRect.height / 2
    val g2 = imgContext.createGraphics()
    g2.translate(popX, popY)
    popup.paint(g2)
    g2.dispose()

    val cropX = maxOf(0, popX - 95)
    val cropY = maxOf(0, popY - 45)
    val cropW = minOf(popup.width + 215, imgContext.width - cropX)
    val cropH = minOf(popup.height + 120, imgContext.height - cropY)
    saveImage(imgContext.getSubimage(cropX, cropY, cropW, cropH), File(outputDir, "log-context-menu.png"))

    // Also save wider view for visible-logs-menu.png
    val wideX = maxOf(0, tablePt.x + 40)
    val wideY = maxOf(0, tablePt.y - 30)
    val wideW = minOf(960, imgContext.width - wideX)
    val wideH = minOf(680, imgContext.height - wideY)
    saveImage(imgContext.getSubimage(wideX, wideY, wideW, wideH), File(outputDir, "visible-logs-menu.png"))

    // Elapsed time between 2 selected log lines (PR #132)
    val filteredModel = getPrivateField<LogListTableModel>(logViewerView, "filteredLogListTableModel")
    allLogsTable.table.clearSelection()
    filteredLogsTable.table.clearSelection()
    filteredLogsTable.table.addRowSelectionInterval(1, 1) // 10-12 15:55:26.830
    filteredLogsTable.table.addRowSelectionInterval(5, 5) // 10-12 15:55:32.190
    layoutWindowTree(frame)

    val elapsedPopup = createPopupMenu(
      table = filteredLogsTable.table,
      model = filteredModel,
      parentWindow = frame,
      filtersPanelProvider = { filtersPanel },
      addSelectionActions = { p, _ ->
        p.add(I18n.get(I18n.LOGS_MENU_ADD_TO_MY_LOGS))
      }
    )
    elapsedPopup.pack()
    realizeComponent(elapsedPopup)

    // Find and arm the "Elapsed time: ..." submenu so its copy actions are visible too
    val elapsedSubmenu = elapsedPopup.components.filterIsInstance<JMenu>().firstOrNull()
    elapsedSubmenu?.model?.isSelected = true
    elapsedSubmenu?.model?.isArmed = true
    val subPopup = elapsedSubmenu?.popupMenu
    if (subPopup != null) {
      subPopup.pack()
      realizeComponent(subPopup)
    }

    val elapsedBaseImg = renderWindow(frame)
    val fTablePt = SwingUtilities.convertPoint(filteredLogsTable.table, Point(0, 0), frame.rootPane)
    val secondRowRect = filteredLogsTable.table.getCellRect(5, 0, true)

    val ePopX = fTablePt.x + 190
    val ePopY = fTablePt.y + secondRowRect.y + secondRowRect.height / 2
    val eg = elapsedBaseImg.createGraphics()
    setupHighQualityGraphics(eg)
    eg.translate(ePopX, ePopY)
    elapsedPopup.paint(eg)
    if (elapsedSubmenu != null && subPopup != null) {
      val subX = elapsedPopup.width - 2
      val subY = elapsedSubmenu.y
      eg.translate(subX, subY)
      subPopup.paint(eg)
    }
    eg.dispose()

    val eCropX = maxOf(0, fTablePt.x)
    val eCropY = maxOf(0, fTablePt.y - 26)
    val eCropW = minOf(840, elapsedBaseImg.width - eCropX)
    val eCropH = minOf(340, elapsedBaseImg.height - eCropY)
    saveImage(elapsedBaseImg.getSubimage(eCropX, eCropY, eCropW, eCropH), File(outputDir, "elapsed-time-menu.png"))
    filteredLogsTable.table.clearSelection()
  }

  private fun generateLiveLogsScreenshot(outputDir: File) {
    val frame = createMainFrame()
    val mainView = MainViewImpl(frame, ServiceLocator.logViewerPrefs, emptySet())
    frame.contentPane = mainView.contentPane
    frame.setSize(1920, 1080)
    realizeWindow(frame)

    val filtersPanel = getPrivateField<FiltersPanel>(mainView, "filtersPanel")
    val liveLogView = getPrivateField<LiveLogViewImpl>(mainView, "liveLogView")
    val filterGroups = buildSampleFilterGroups()
    val contextTag = liveLogView.presenter.filterContextTag
    populateContextMatches(filterGroups, contextTag)

    filtersPanel.activeFilterContextTag = contextTag
    filtersPanel.filtersPane.setFilters(filterGroups)
    setExpandedGroups(
      filtersPanel.filtersPane,
      setOf("Performance.filter", "system-events.filter")
    )

    // Switch to Live Logs tab (index 2)
    mainView.mainTabbedPane.selectedIndex = 2

    val device = AdbDevice(
      serialNumber = "N5ST130192",
      state = DeviceState.ONLINE,
      product = "eiger_gu",
      model = "motorola_razr_plus_2026",
      device = "eiger"
    )
    val liveAllLogs = buildLiveAllLogs(filterGroups)
    val liveFilteredLogs = buildLiveFilteredLogs(filterGroups)
    ServiceLocator.liveLogsRepository.addLogEntries(liveAllLogs)
    liveLogView.showLogsView(device)
    liveLogView.showStreamingStatus("Streaming", true)
    liveLogView.deviceLabel.text = I18n.format(I18n.LIVE_LOGS_LABEL_DEVICE, device.displayName)
    liveLogView.showStats(87112L, 0L, 87112, 458)
    liveLogView.showAvailableLogStreams(setOf(LogStream.MAIN, LogStream.SYSTEM, LogStream.EVENTS))

    // Expand side buttons panel on the right
    liveLogView.setSidePanelExpanded(true)

    liveLogView.showAllLogs(liveAllLogs, liveFilteredLogs)
    liveLogView.logList.table.setRowSelectionInterval(8, 8)

    val liveMainSplit = getPrivateField<JSplitPane>(liveLogView, "mainSplitPane")
    liveMainSplit.dividerLocation = 460
    layoutWindowTree(frame)

    val img = renderWindow(frame)

    // Blur the device serial number inside deviceLabel for privacy
    val devLbl = liveLogView.deviceLabel
    val lblPt = SwingUtilities.convertPoint(devLbl, Point(0, 0), frame.rootPane)
    val fm = devLbl.getFontMetrics(devLbl.font)
    val fullText = devLbl.text
    val openParenIdx = fullText.indexOf('(')
    val closeParenIdx = fullText.indexOf(')')
    if (openParenIdx >= 0 && closeParenIdx > openParenIdx) {
      val blurX = lblPt.x + fm.stringWidth(fullText.substring(0, openParenIdx + 1)) - 2
      val blurY = lblPt.y
      val blurW = fm.stringWidth(fullText.substring(openParenIdx + 1, closeParenIdx)) + 4
      val blurH = devLbl.height
      blurRegion(img, blurX, blurY, blurW, blurH)
    }

    saveImage(img, File(outputDir, "live_logs.png"))
    frame.dispose()
  }

  private fun generateBugReportScreenshot(outputDir: File) {
    val frame = createMainFrame()
    val mainView = MainViewImpl(frame, ServiceLocator.logViewerPrefs, emptySet())
    frame.contentPane = mainView.contentPane
    frame.setSize(1920, 1080)
    realizeWindow(frame)

    val bugReportView = getPrivateField<BugReportViewImpl>(mainView, "bugReportView")
    mainView.mainTabbedPane.selectedIndex = 1

    val packages = buildSampleAppPackages()
    fun emptySection(name: String) = object : BugReportSection(name) {}
    val sections = listOf(
      BugreportInfoSection("", "", "", "", ""),
      emptySection(SectionNames.SYSTEM_PROPERTIES),
      PackagesSection(SectionNames.APPLICATION_PKG, packages),
      emptySection(SectionNames.SYSTEM_HIDDEN_PKG),
      emptySection(SectionNames.CARRIER_CONFIG),
      emptySection(SectionNames.SUBSCRIPTIONS),
      emptySection(SectionNames.PLAIN_TEXT)
    )
    bugReportView.showBugReport(BugReport("Sample BugReport", sections))

    val sectionsList = getPrivateField<JList<String>>(bugReportView, "sectionsList")
    val mainSplitPane = getPrivateField<JSplitPane>(bugReportView, "mainSplitPane")
    sectionsList.selectedIndex = 2 // "Application Packages"
    mainSplitPane.dividerLocation = 220
    layoutWindowTree(frame)

    val pkgPanel = mainSplitPane.rightComponent as? ListSectionPanel
    if (pkgPanel != null) {
      val pkgList = getPrivateFieldInHierarchy<JList<String>>(pkgPanel, "list")
      val pkgSplit = getPrivateFieldInHierarchy<JSplitPane>(pkgPanel, "splitPane")
      pkgList.selectedIndex = 11 // com.android.bips | v17 (37)
      pkgSplit.setDividerLocation(0.58)
    }
    layoutWindowTree(frame)

    saveWindowImage(frame, File(outputDir, "bugreport_app_view.png"))
    frame.dispose()
  }

  private fun generateDialogScreenshots(outputDir: File) {
    val owner = createMainFrame()
    owner.setSize(1200, 800)
    realizeWindow(owner)

    // 1. filter-editor.png
    val editFilterConstructor = EditFilterDialog::class.java.getDeclaredConstructor(
      Frame::class.java,
      Filter::class.java
    )
    editFilterConstructor.isAccessible = true
    val editFilterDialog = editFilterConstructor.newInstance(owner, null)
    editFilterDialog.isModal = false
    val colorChooser = getPrivateField<JColorChooser>(editFilterDialog, "colorChooser")
    colorChooser.color = Color.YELLOW
    editFilterDialog.pack()
    realizeWindow(editFilterDialog)
    saveWindowImage(editFilterDialog, File(outputDir, "filter-editor.png"))

    // 2. regex-editor.png
    val regexConstructor = RegexEditorDialog::class.java.getDeclaredConstructor(Dialog::class.java)
    regexConstructor.isAccessible = true
    val regexDialog = regexConstructor.newInstance(editFilterDialog)
    regexDialog.isModal = false
    val regexEdit = getPrivateField<JTextArea>(regexDialog, "regexEdit")
    regexEdit.text = "\\w:"
    regexDialog.pack()
    realizeWindow(regexDialog)
    saveWindowImage(regexDialog, File(outputDir, "regex-editor.png"))
    regexDialog.dispose()
    editFilterDialog.dispose()

    // 3. filter-search.png
    val filtersRepo = ServiceLocator.filtersRepository
    filtersRepo.closeAllFilters()
    val sampleGroups = buildSearchDialogFilterGroups()
    sampleGroups.forEach { (group, list) ->
      filtersRepo.addGroup(group)
      list.forEach { filtersRepo.addFilter(group, it) }
    }
    val dummyHost = object : FiltersPanelHost {
      override val parentWindow: Frame get() = owner
      override fun showSaveFilterFileChooser(suggestedFileName: String?): File? = null
      override fun showOpenMultipleFiltersFileChooser(): Array<File> = emptyArray()
    }
    val dummyFiltersPanel = FiltersPanel(dummyHost)
    val searchFiltersDialog = SearchFiltersDialog(
      owner = owner,
      presenter = dummyFiltersPanel.presenter,
      filtersRepository = filtersRepo,
      onFilterSelected = { _, _ -> }
    )
    searchFiltersDialog.isModal = false
    val resultsTable = getPrivateField<JTable>(searchFiltersDialog, "resultsTable")
    if (resultsTable.rowCount > 0) {
      resultsTable.setRowSelectionInterval(0, 0)
    }
    val matchesLabel = getPrivateField<JLabel>(searchFiltersDialog, "matchesLabel")
    matchesLabel.text = "Showing all 355 filter(s)"
    realizeWindow(searchFiltersDialog)
    saveWindowImage(searchFiltersDialog, File(outputDir, "filter-search.png"))
    searchFiltersDialog.dispose()

    // 4. line-inspector.png
    val sampleFile = File("/Users/tbagni/Downloads/AndroidLogs/Logs/aplogd/Boot-0000000023_Set-0000000543_Stream-e.txt")
    val sampleFilter = Filter("dvm_lock_sample:", "dvm_lock_sample:", Color.YELLOW, LogLevel.VERBOSE, false)
    val rawLine = "10-10 14:15:09.930  2350  5964 I dvm_lock_sample: [system_server,1,binder:2350_14,5,PowerManagerService.java,2041,void com.android.server.power.PowerManagerService.acquireWakeLockInternal(android.os.IBinder, int, int, java.lang.String, java.lang.String, android.os.WorkSource, java.lang.String, int, int, android.os.IWakeLockCallback),-,2100,void com.android.server.power.PowerManagerService.acquireWakeLockInternal(android.os.IBinder, int, int, java.lang.String, java.lang.String, android.os.WorkSource, java.lang.String, int, int, android.os.IWakeLockCallback),1]"
    val entry = LogEntry(
      rawLine,
      LogLevel.INFO,
      LogTimestamp(10, 10, 14, 15, 9, 930),
      LogStream.EVENTS,
      sampleFile,
      698
    ).apply {
      index = 70542
      appliedFilter = sampleFilter
    }
    val lineInfo = LogLineInfo(
      entry = entry,
      sourceFile = sampleFile,
      lineNumber = 698,
      logStream = LogStream.EVENTS,
      logLevel = LogLevel.INFO,
      timestamp = entry.timestamp,
      pid = 2350,
      tid = 5964,
      tag = "dvm_lock_sample",
      message = "[system_server,1,binder:2350_14,5,PowerManagerService.java,2041,void com.android.server.power.PowerManagerService.acquireWakeLockInternal(android.os.IBinder, int, int, java.lang.String, java.lang.String, android.os.WorkSource, java.lang.String, int, int, android.os.IWakeLockCallback),-,2100,void com.android.server.power.PowerManagerService.acquireWakeLockInternal(android.os.IBinder, int, int, java.lang.String, java.lang.String, android.os.WorkSource, java.lang.String, int, int, android.os.IWakeLockCallback),1]",
      appliedFilter = sampleFilter,
      index = 70542
    )
    val lineDetailsDialog = LogLineDetailsDialog(
      owner = owner,
      info = lineInfo,
      onFilterByPid = {},
      onFilterByTag = {},
      onOpenInEditor = { _, _ -> }
    )
    lineDetailsDialog.isModal = false
    realizeWindow(lineDetailsDialog)
    saveWindowImage(lineDetailsDialog, File(outputDir, "line-inspector.png"))
    lineDetailsDialog.dispose()

    // 5. visible-logs-dialog.png
    val firstLog = LogEntry(
      "04-11 14:07:53.803  root   892 25252 D resolv  : doQuery: rcode=0, ancount=2",
      LogLevel.DEBUG,
      LogTimestamp(4, 11, 14, 7, 53, 803)
    )
    val lastLog = LogEntry(
      "04-11 14:09:17.201  1000  1648  5749 I KPI-6PA-WMS-2: 978586 Start mAnimation: 0 in WindowStateAnimator{1d4bf36 PopupWindow:e912870}",
      LogLevel.INFO,
      LogTimestamp(4, 11, 14, 9, 17, 201)
    )
    val visDialog = VisibleLogsConfigurationDialog(owner, VisibleLogConfiguration(firstLog, lastLog))
    visDialog.isModal = false
    visDialog.pack()
    realizeWindow(visDialog)
    saveWindowImage(visDialog, File(outputDir, "visible-logs-dialog.png"))
    visDialog.dispose()

    // 6. go-to-dialog.png
    val optionPane = JOptionPane(
      I18n.get(I18n.LOGS_GOTO_TIMESTAMP_MSG),
      JOptionPane.PLAIN_MESSAGE,
      JOptionPane.OK_CANCEL_OPTION,
      null,
      null,
      "{month}-{day} {hour}:{min}:{sec}:{hund}"
    ).apply {
      wantsInput = true
      initialSelectionValue = "{month}-{day} {hour}:{min}:{sec}:{hund}"
    }
    val goToDialog = optionPane.createDialog(owner, I18n.get(I18n.LOGS_GOTO_TIMESTAMP_TITLE))
    goToDialog.isModal = false
    goToDialog.pack()
    realizeWindow(goToDialog)
    saveWindowImage(goToDialog, File(outputDir, "go-to-dialog.png"))
    goToDialog.dispose()

    owner.dispose()
  }

  private fun createMainFrame(): JFrame {
    val title = "${AppInfo.APPLICATION_NAME} v${AppInfo.currentVersion}"
    return JFrame(title)
  }

  private fun realizeWindow(window: Window) {
    if (!window.isDisplayable) {
      window.addNotify()
    }
    layoutWindowTree(window)
  }

  private fun realizeComponent(component: JComponent) {
    if (!component.isDisplayable) {
      val tempWindow = JWindow()
      tempWindow.contentPane.add(component)
      tempWindow.pack()
      layoutWindowTree(tempWindow)
    } else {
      component.doLayout()
      component.validate()
    }
  }

  private fun layoutWindowTree(component: Component) {
    component.doLayout()
    if (component is Container) {
      component.validate()
      for (child in component.components) {
        layoutWindowTree(child)
      }
    }
  }

  private fun renderWindow(window: Window): BufferedImage {
    layoutWindowTree(window)
    val root = when (window) {
      is JFrame -> window.rootPane
      is JDialog -> window.rootPane
      else -> window
    }
    val w = maxOf(1, root.width)
    val h = maxOf(1, root.height)
    val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val g2 = image.createGraphics()
    setupHighQualityGraphics(g2)
    // Two warmup paint passes so JTable cell renderers (e.g. LogCellRenderer) first
    // validate wrapped JTextArea widths on pass 1, update dynamic table row heights
    // on pass 2, and render with finalized row heights on pass 3
    repeat(2) {
      root.paint(g2)
      layoutWindowTree(window)
    }
    root.paint(g2)
    g2.dispose()
    return image
  }

  private fun saveWindowImage(window: Window, file: File) {
    val img = renderWindow(window)
    saveImage(img, file)
  }

  private fun saveImage(image: BufferedImage, file: File) {
    ImageIO.write(image, "png", file)
    println("  -> Saved ${file.name} (${image.width}x${image.height})")
  }

  private fun copyImage(source: BufferedImage): BufferedImage {
    val copy = BufferedImage(source.width, source.height, source.type)
    val g = copy.createGraphics()
    g.drawImage(source, 0, 0, null)
    g.dispose()
    return copy
  }

  private fun setupHighQualityGraphics(g2: Graphics2D) {
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
  }

  private fun blurRegion(img: BufferedImage, x: Int, y: Int, w: Int, h: Int) {
    val rx = x.coerceIn(0, img.width - 1)
    val ry = y.coerceIn(0, img.height - 1)
    val rw = w.coerceAtMost(img.width - rx)
    val rh = h.coerceAtMost(img.height - ry)
    if (rw <= 2 || rh <= 2) return

    val radius = 7
    val size = radius * 2 + 1
    val weight = 1.0f / (size * size)
    val data = FloatArray(size * size) { weight }
    val kernel = Kernel(size, size, data)
    val op = ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null)

    // Pad subimage so edges blur cleanly, then copy blurred inner rect back
    val pad = radius * 2
    val px = (rx - pad).coerceAtLeast(0)
    val py = (ry - pad).coerceAtLeast(0)
    val pw = (rw + pad * 2).coerceAtMost(img.width - px)
    val ph = (rh + pad * 2).coerceAtMost(img.height - py)

    var sub = copyImage(img.getSubimage(px, py, pw, ph))
    repeat(3) {
      sub = op.filter(sub, null)
    }

    val g2 = img.createGraphics()
    val offsetX = rx - px
    val offsetY = ry - py
    g2.drawImage(sub.getSubimage(offsetX, offsetY, rw, rh), rx, ry, null)
    g2.dispose()
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T> getPrivateField(instance: Any, fieldName: String): T {
    val field = instance.javaClass.getDeclaredField(fieldName)
    field.isAccessible = true
    return field.get(instance) as T
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T> getPrivateFieldInHierarchy(instance: Any, fieldName: String): T {
    var clazz: Class<*>? = instance.javaClass
    while (clazz != null) {
      try {
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(instance) as T
      } catch (_: NoSuchFieldException) {
        clazz = clazz.superclass
      }
    }
    throw NoSuchFieldException(fieldName)
  }

  private fun setExpandedGroups(filtersList: FiltersList, expandedNames: Set<String>) {
    val map = getPrivateField<Map<String, Any>>(filtersList, "filterUIGroups")
    for ((name, groupObj) in map) {
      val method = groupObj.javaClass.getDeclaredMethod("forceGroupVisibilitySilently", Boolean::class.javaPrimitiveType)
      method.isAccessible = true
      method.invoke(groupObj, name in expandedNames)
    }
    filtersList.revalidate()
    filtersList.repaint()
  }

  private fun getFilterUIGroup(filtersList: FiltersList, name: String): JComponent? {
    val map = getPrivateField<Map<String, JComponent>>(filtersList, "filterUIGroups")
    return map[name]
  }

  private fun buildSampleFilterGroups(): Map<String, List<Filter>> {
    fun f(name: String, pattern: String, color: Color, applied: Boolean = false): Filter {
      return Filter(name, pattern, color, LogLevel.VERBOSE, false).apply { isApplied = applied }
    }

    return linkedMapOf(
      "All" to listOf(
        f("DcmoPlugin :", "DcmoPlugin :", Color(120, 175, 255)),
        f("DM_DmtPluginManager", "DM_DmtPluginManager", Color(83, 199, 246)),
        f("DishDMPasswordGenerator", "DishDMPasswordGenerator", Color(235, 118, 91)),
        f("DMHttpConnector", "DMHttpConnector", Color(172, 224, 249)),
        f("clientPW =", "clientPW =", Color(92, 168, 255))
      ),
      "Performance.filter" to listOf(
        f("slow dispatch", "Slow dispatch", Color(255, 102, 250), applied = true),
        f("CPU usage", "CPU usage", Color(222, 143, 143), applied = true),
        f("dvm_lock_sample:", "dvm_lock_sample:", Color(235, 235, 50), applied = false),
        f("IPCThreadState: Waiting for thread to be free", "IPCThreadState: Waiting for thread", Color(222, 143, 143), applied = false)
      ),
      "Settings-SIM.filter" to listOf(
        f("SubscriptionInfo", "SubscriptionInfo", Color(83, 199, 246))
      ),
      "Telephony-DCI.filter" to listOf(f("DCI", "DCI", Color.CYAN)),
      "Telephony-Data.filter" to listOf(f("DataNetwork", "DataNetwork", Color.CYAN)),
      "Telephony-EUICC.filter" to listOf(f("Euicc", "Euicc", Color.CYAN)),
      "Telephony-EchoLocate.filter" to listOf(f("EchoLocate", "EchoLocate", Color.CYAN)),
      "Telephony-FW.filter" to listOf(f("PhoneFactory", "PhoneFactory", Color.CYAN)),
      "Telephony-IMS.filter" to listOf(f("ImsPhone", "ImsPhone", Color.CYAN)),
      "Telephony-InCallUi.filter" to listOf(f("InCallPresenter", "InCallPresenter", Color.CYAN)),
      "Telephony-MNS.filter" to listOf(f("ManualNetwork", "ManualNetwork", Color.CYAN)),
      "Telephony-RIL-SIM.filter" to listOf(
        f("< UNSOL_RESPONSE_SIM_STATUS_CHANGED", "UNSOL_RESPONSE_SIM_STATUS_CHANGED", Color(180, 180, 40)),
        f("< SLOT_STATUS_CHANGED", "SLOT_STATUS_CHANGED", Color(180, 180, 40)),
        f("< UNSOL_SIM_REFRESH", "UNSOL_SIM_REFRESH", Color(255, 140, 0), applied = true),
        f("[<>] GET_SIM_STATUS", "GET_SIM_STATUS", Color(185, 185, 0), applied = true),
        f("[<>] GET_SLOT_STATUS", "GET_SLOT_STATUS", Color(185, 185, 0)),
        f("[<>] GET_PLMN_FROM_SE13", "GET_PLMN_FROM_SE13", Color(83, 199, 246)),
        f("[<>] RIL_REQUEST_ENABLE_UICC_APPLICATIONS", "RIL_REQUEST_ENABLE_UICC_APPLICATIONS", Color(235, 118, 91)),
        f("[<>] ENABLE_UICC_APPLICATIONS", "ENABLE_UICC_APPLICATIONS", Color(235, 118, 91)),
        f("< RIL_UNSOL_UICC_APPLICATIONS_ENABLEMENT_CHANGED", "RIL_UNSOL_UICC_APPLICATIONS_ENABLEMENT_CHANGED", Color(195, 185, 195)),
        f("< UNSOL_UICC_APPLICATIONS_ENABLEMENT_CHANGED", "UNSOL_UICC_APPLICATIONS_ENABLEMENT_CHANGED", Color(222, 143, 143)),
        f("[<>] GET_UICC_APPLICATIONS_ENABLEMENT", "GET_UICC_APPLICATIONS_ENABLEMENT", Color(190, 140, 235)),
        f("GET_SIM_PHONEBOOK_RECORDS", "GET_SIM_PHONEBOOK_RECORDS", Color(83, 199, 246)),
        f("UNSOL_RESPONSE_SIM_PHONEBOOK_RECORDS_RECEIVED", "UNSOL_RESPONSE_SIM_PHONEBOOK_RECORDS_RECEIVED", Color(195, 185, 195)),
        f("[<>] ENTER_SIM_PIN", "ENTER_SIM_PIN", Color(180, 220, 180)),
        f("[<>] ENTER_SIM_PUK", "ENTER_SIM_PUK", Color(180, 220, 180)),
        f("[<>] SET_LOGICAL_TO_PHYSICAL_SLOT_MAPPING", "SET_LOGICAL_TO_PHYSICAL_SLOT_MAPPING", Color(100, 230, 120)),
        f("[<>] CHANGE_SIM_PIN2", "CHANGE_SIM_PIN2", Color(180, 220, 180))
      ),
      "Telephony-RIL.filter" to listOf(f("RILJ", "RILJ", Color.CYAN)),
      "Telephony-SIM.filter" to listOf(
        f("UiccController", "UiccController", Color(235, 80, 235), applied = true),
        f("SMSVC", "SMSVC", Color(83, 199, 246), applied = true),
        f("UiccSlot", "UiccSlot", Color(235, 118, 91), applied = true),
        f("IsimUiccRecords", "IsimUiccRecords", Color(200, 90, 240), applied = true)
      ),
      "Telephony-TelecommSvc.filter" to listOf(f("TelecomService", "TelecomService", Color.CYAN)),
      "Telephony-TelephonySvc.filter" to listOf(f("TelephonyRegistry", "TelephonyRegistry", Color.CYAN)),
      "TelephonyProvider.filter" to listOf(f("TelephonyProvider", "TelephonyProvider", Color.CYAN, applied = true)),
      "system-events.filter" to listOf(
        f("key_back_press", "key_back_press", Color(100, 230, 100), applied = true),
        f("Back button event: ACTION_", "Back button event: ACTION_", Color(185, 185, 0), applied = true),
        f("am_proc_start:", "am_proc_start:", Color(100, 230, 100), applied = true),
        f("ActivityManagerTiming: FinishBooting", "FinishBooting", Color(235, 235, 50), applied = true),
        f("B2G.BugReportController: Started report collection", "Started report collection", Color(185, 185, 0), applied = true),
        f("Build fingerprint", "Build fingerprint", Color(222, 143, 143), applied = true),
        f("ShutdownThread", "ShutdownThread", Color(83, 199, 246), applied = true),
        f("WM boot postFinishBooting", "postFinishBooting", Color(83, 199, 246), applied = true),
        f("ActivityManager: Set .+: top-activity", "ActivityManager: Start", Color(100, 230, 100), applied = true),
        f("am_anr", "am_anr", Color(100, 230, 100), applied = true)
      )
    )
  }

  private fun populateContextMatches(groups: Map<String, List<Filter>>, tag: FilterContextTag) {
    fun setCount(group: String, filterName: String, mainCount: Int) {
      val f = groups[group]?.firstOrNull { it.name == filterName } ?: return
      val info = f.initContext(tag)
      info.setAllowedStreams(LogStream.values().toSet())
      repeat(mainCount) { info.incrementLineCount(LogStream.MAIN) }
    }
    setCount("Performance.filter", "slow dispatch", 90)
    setCount("Performance.filter", "CPU usage", 1)
    setCount("Performance.filter", "dvm_lock_sample:", 2)
    setCount("Performance.filter", "IPCThreadState: Waiting for thread to be free", 0)
    setCount("Telephony-RIL-SIM.filter", "< UNSOL_SIM_REFRESH", 4)
    setCount("Telephony-RIL-SIM.filter", "[<>] GET_SIM_STATUS", 68)
    setCount("system-events.filter", "key_back_press", 0)
    setCount("system-events.filter", "Back button event: ACTION_", 0)
    setCount("system-events.filter", "am_proc_start:", 127)
    setCount("system-events.filter", "ActivityManagerTiming: FinishBooting", 0)
    setCount("system-events.filter", "B2G.BugReportController: Started report collection", 0)
    setCount("system-events.filter", "Build fingerprint", 1)
    setCount("system-events.filter", "ShutdownThread", 0)
    setCount("system-events.filter", "WM boot postFinishBooting", 0)
    setCount("system-events.filter", "ActivityManager: Set .+: top-activity", 0)
    setCount("system-events.filter", "am_anr", 0)
  }

  private fun buildSampleAllLogs(): List<LogEntry> {
    val lines = listOf(
      "10-12 15:27:54.790  2350  3793 I framework_stats_log: [PROVIDER_ACQUISITION_EVENT_REPORTED, 10389, 10362, 1, com.google.android.apps.wellbeing, com.android.settings.intelligence, 19, 15, false, 0]",
      "10-12 15:27:54.791  2350  3793 I framework_stats_log: [PROVIDER_ACQUISITION_EVENT_REPORTED, 10519, 10362, 1, 1, com.google.android.cellbroadcastreceiver, com.android.settings.intelligence, 19, 15, false, 0]",
      "10-12 15:27:54.798  2350  3793 I framework_stats_log: [PROVIDER_ACQUISITION_EVENT_REPORTED, 10307, 10362, 1, 1, com.google.android.euicc, com.android.settings.intelligence, 19, 5, false, 0]",
      "10-12 15:27:54.799  2350  3793 I framework_stats_log: [PROVIDER_ACQUISITION_EVENT_REPORTED, 10534, 10362, 1, 1, com.google.android.gms, com.android.settings.intelligence, 19, 15, false, 0]",
      "10-12 15:27:54.800  2350  3793 I framework_stats_log: [PROVIDER_ACQUISITION_EVENT_REPORTED, 10534, 10362, 1, 1, com.google.android.gms, com.android.settings.intelligence, 19, 15, false, 0]",
      "10-12 15:27:54.802  2350  3793 I framework_stats_log: [SERVICE_STATE_CHANGED, 10534, com.google.android.gms, com.google.android.gms.chimera.UiIntentOperationService, 1]",
      "10-12 15:27:54.802  2350  3793 I framework_stats_log: [SERVICE_LAUNCH_REPORTED, 10534, com.google.android.gms, com.google.android.gms.chimera.UiIntentOperationService]",
      "10-12 15:27:54.802  2350  3793 I framework_stats_log: [SERVICE_REQUEST_EVENT_REPORTED, 10534, 10534, com.google.android.gms.GOOGLE_SETTINGS_OPERATION, 1, false, 1, .ui, .chimera.UiIntentOperationService, 1, com.google.android.gms, com.google.android.gms, 15, 15, false, 0]",
      "10-12 15:27:54.806  2350  3793 I framework_stats_log: [ALARM_SCHEDULED, 10534, false, false, false, false, false, 0, false, 1020, false]",
      "10-12 15:27:54.810  2350  3793 I framework_stats_log: [PROVIDER_ACQUISITION_EVENT_REPORTED, 10534, 10362, 1, 1, com.google.android.gms, com.android.settings.intelligence, 19, 15, false, 0]"
    )
    return lines.mapIndexed { idx, text ->
      LogLineParser.createLogEntry(text, idx + 79595, LogStream.EVENTS).apply { index = idx + 79594 }
    }
  }

  private fun buildSampleFilteredLogs(groups: Map<String, List<Filter>>): List<LogEntry> {
    val smsvcFilter = groups["Telephony-SIM.filter"]!!.first { it.name == "SMSVC" }
    val uiccSlotFilter = groups["Telephony-SIM.filter"]!!.first { it.name == "UiccSlot" }
    val uiccCtrlFilter = groups["Telephony-SIM.filter"]!!.first { it.name == "UiccController" }
    val getSimFilter = groups["Telephony-RIL-SIM.filter"]!!.first { it.name == "[<>] GET_SIM_STATUS" }

    val items = listOf(
      "10-12 15:55:26.828  5431  5431 D SMSVC   : setSubscriptionProperty: subId=6, columnName=wfc_ims_mdn, value=0, calling package=com.android.phone" to smsvcFilter,
      "10-12 15:55:26.830  5431  5431 D SMSVC   : setSubscriptionProperty: subId=6, columnName=wfc_ims_mdn, value=0, calling package=com.android.phone" to smsvcFilter,
      "10-12 15:55:32.126  2350  2350 I framework_stats_log: [BINDER_CALLS_REPORTED, 10290, 1001, com.android.internal.telephony.euicc.IEuiccController, isCarrierForceSingleSimAndPsimOnly, 50, 24247, 0, 0, 184664079, 5, 292, 19378, 0, 0, []]" to uiccSlotFilter,
      "10-12 15:55:32.188  5431  5431 D UiccController: handleMessage: Received ICC_STATUS_CHANGED for phoneId 0" to uiccCtrlFilter,
      "10-12 15:55:32.188  5431  5431 D UiccController: [0]Received EVENT_ICC_STATUS_CHANGED, calling getIccCardStatus" to uiccCtrlFilter,
      "10-12 15:55:32.190  5431  5431 D RILJ    : [2890]> GET_SIM_STATUS [PHONE0]" to getSimFilter,
      "10-12 15:55:32.222  5431  5431 D RILJ    : [2890]< GET_SIM_STATUS IccCardState {CARDSTATE_PRESENT,PINSTATE_UNKNOWN,num_apps=3,gsm_id=-1,cdma_id=-1,ims_id=-1,{APPTYPE_ISIM,APPSTATE_DETECTED,pin1=PINSTATE_UNKNOWN,pin2=PINSTATE_UNKNOWN},atr=3b9f96803f4cb38031e073f6a114574a434f5054656c34,iccid=8901260910775480995F,eid=89043051202509096226015876483333,SupportedMepMode=MEP_A1,SlotPortMapping={physicalSlotIndex=1, portIndex=1}} [PHONE0]" to getSimFilter
    )
    return items.mapIndexed { idx, (text, filter) ->
      LogLineParser.createLogEntry(text, idx + 70407, LogStream.RADIO).apply {
        index = idx + 70406
        appliedFilter = filter
      }
    }
  }

  private fun buildSampleMyLogs(groups: Map<String, List<Filter>>): List<LogEntry> {
    val smsvcFilter = groups["Telephony-SIM.filter"]!!.first { it.name == "SMSVC" }
    val isimFilter = groups["Telephony-SIM.filter"]!!.first { it.name == "IsimUiccRecords" }
    val getSimFilter = groups["Telephony-RIL-SIM.filter"]!!.first { it.name == "[<>] GET_SIM_STATUS" }

    val items = listOf(
      "10-12 15:55:32.190  5431  5431 D RILJ    : [2890]> GET_SIM_STATUS [PHONE0]" to getSimFilter,
      "10-12 15:55:32.222  5431  5431 D RILJ    : [2890]< GET_SIM_STATUS IccCardState {CARDSTATE_PRESENT,PINSTATE_UNKNOWN,num_apps=3,gsm_id=-1,cdma_id=-1,ims_id=1{APPTYPE_ISIM,APPSTATE_DETECTED,pin1=PINSTATE_UNKNOWN,pin2=PINSTATE_UNKNOWN},atr=3b9f96803f4cb38031e073f6a114574a434f5054656c34,iccid=8901260910775480995F,eid=89043051202509096226015876483333,SupportedMepMode=MEP_A1,SlotPortMapping={physicalSlotIndex=1, portIndex=1}} [PHONE0]" to getSimFilter,
      "10-12 15:55:32.356  5431  5431 D SMSVC   : markSubscriptionsInactive: slot 1" to smsvcFilter,
      "10-12 15:55:32.366  5431  5431 D SMSVC   : markSubscriptionsInactive: current mapping [slot 0: subId=6]" to smsvcFilter,
      "10-12 15:55:35.183  5431  5431 E IsimUiccRecords: [ISIM-0] IsimUiccRecords: handleMessage { when=0 what=100 obj=android.os.AsyncResult@7676d0c target=com.android.internal.telephony.uicc.IsimUiccRecords {884f98c} sendThreadId=5714 sendThreadName=UiccController-SIM-IO async=false heapIndex=-1 hashCode=d8b4655 }[100]" to isimFilter,
      "10-12 15:55:40.249  5431  5431 D RILJ    : [3387]< GET_SIM_STATUS IccCardState {CARDSTATE_PRESENT,PINSTATE_UNKNOWN,num_apps=3,gsm_id=0{APPTYPE_USIM,APPSTATE_DETECTED,pin1=PINSTATE_DISABLED,pin2=PINSTATE_ENABLED_NOT_VERIFIED},cdma_id=-1,ims_id=1{APPTYPE_ISIM,APPSTATE_READY,pin1=PINSTATE_DISABLED,pin2=PINSTATE_UNKNOWN},atr=3b9f96803f4cb38031e073f6a114574a434f5054656c34,iccid=8901260910775480995F,eid=89043051202509096226015876483333,SupportedMepMode=MEP_A1,SlotPortMapping={physicalSlotIndex=1, portIndex=1}} [PHONE0]" to getSimFilter,
      "10-12 15:55:40.250  5431  5431 D RILJ    : [3389]< GET_SIM_STATUS IccCardState {CARDSTATE_PRESENT,PINSTATE_UNKNOWN,num_apps=3,gsm_id=0{APPTYPE_USIM,APPSTATE_DETECTED,pin1=PINSTATE_DISABLED,pin2=PINSTATE_ENABLED_NOT_VERIFIED},cdma_id=-1,ims_id=1{APPTYPE_ISIM,APPSTATE_READY,pin1=PINSTATE_DISABLED,pin2=PINSTATE_UNKNOWN},atr=3b9f96803f4cb38031e073f6a114574a434f5054656c34,iccid=8901260910775480995F,eid=89043051202509096226015876483333,SupportedMepMode=MEP_A1,SlotPortMapping={physicalSlotIndex=1, portIndex=1}} [PHONE0]" to getSimFilter,
      "10-12 15:55:42.564  5431  5431 D SMSVC   : isSatelliteSpn: Read config_satellite_sim_spn_identifier from device config" to smsvcFilter,
      "10-12 15:55:42.564  5431  5431 D SMSVC   : isSatelliteSpn: overlaySpn=, spn=AT&T" to smsvcFilter,
      "10-12 15:55:42.564  5431  5431 D SMSVC   : isSatellitePlmn: Read config_satellite_sim_plmn_identifier from device config" to smsvcFilter
    )
    return items.mapIndexed { idx, (text, filter) ->
      LogLineParser.createLogEntry(text, idx + 70410, LogStream.RADIO).apply {
        index = idx + 70409
        appliedFilter = filter
      }
    }
  }

  private fun buildStreamIndicationLogs(): List<LogEntry> {
    val items = listOf(
      "10-09 13:56:20.984  2350  5964 D PowerManagerService: acquireWakeLockInternal: lock=180331598, flags=0x1, tag=\"GCoreFlp\", ws=WorkSource{10550 com.motorola.gpslogger.debug}, uid=10534, pid=10440, packageName=\"com.google.android.gms\", displayId=0" to LogStream.SYSTEM,
      "10-09 13:56:20.984  2350  5964 D PowerManagerService: nativeAcquireSuspendBlocker name:PowerManagerService.WakeLocks duration ms: 1" to LogStream.SYSTEM,
      "10-10 07:49:01.968  1940  2011 V LocSvc_ApiV02: gnssMeasurementCorrections.satCorrections:" to LogStream.MAIN,
      "10-10 07:49:01.968  1940  2011 V LocSvc_ApiV02: satCorrections[64].svType = 1 satCorrections[64].svId = 10 satCorrections[64].carrierFrequencyHz = 1176450048.00 satCorrections[64].probSatIsLos = 0.00 satCorrections[64].excessPathLengthMeters = 0.00 satCorrections[64].excessPathLengthUncertaintyMeters = 0.00 satCorrections[64].reflectingPlane.latitudeDegree = 0.00 satCorrections[64].reflectingPlane.longitudeDegrees = 0.00 satCorrections[64].reflectingPlane.altitudeMeters = 0.00 satCorrections[64].reflectingPlane.azimuthDegrees = 0.00" to LogStream.MAIN
    )
    return items.mapIndexed { idx, (text, stream) ->
      LogLineParser.createLogEntry(text, idx + 1, stream).apply { index = idx }
    }
  }

  private fun buildSearchAllLogs(): List<LogEntry> {
    return buildSampleAllLogs()
  }

  private fun buildSearchFilteredLogs(): List<LogEntry> {
    val euiccFilter = Filter("EuiccGoogle", "EuiccGoogle", Color(185, 185, 0), LogLevel.VERBOSE, false).apply { isApplied = true }
    val lines = listOf(
      "10-12 15:59:25.091 12215 12215 I EuiccGoogle: [116] CarrierInfoStore.initCarrierInfoStore: Carrier proto is empty.",
      "10-12 15:59:25.091 12215 12215 E EuiccGoogle: [116] CarrierInfoStore.getCarrierByConfigName: Unable to find carrier config name key: carrier_freedom",
      "10-12 15:59:25.091 12215 12215 E EuiccGoogle: [116] CarrierInfoStore.initCountryCarrierList: Unable to find carrier by config name: carrier_freedom",
      "10-12 15:59:25.091 12215 12215 I EuiccGoogle: [116] CarrierInfoStore.initCarrierInfoStore: initCarrierInfoStore",
      "10-12 15:59:25.091 12215 12215 I EuiccGoogle: [116] CarrierInfoStore.initCarrierInfoStore: Carrier proto is empty.",
      "10-12 15:59:25.091 12215 12215 E EuiccGoogle: [116] CarrierInfoStore.getCarrierByConfigName: Unable to find carrier config name key: carrier_koodo",
      "10-12 15:59:25.091 12215 12215 E EuiccGoogle: [116] CarrierInfoStore.initCountryCarrierList: Unable to find carrier by config name: carrier_koodo",
      "10-12 15:59:25.091 12215 12215 I EuiccGoogle: [116] CarrierInfoStore.initCarrierInfoStore: initCarrierInfoStore"
    )
    return lines.mapIndexed { idx, text ->
      LogLineParser.createLogEntry(text, idx + 1, LogStream.MAIN).apply {
        index = idx
        appliedFilter = euiccFilter
      }
    }
  }

  private fun buildLiveAllLogs(groups: Map<String, List<Filter>>): List<LogEntry> {
    val amStartFilter = groups["system-events.filter"]!!.first { it.name == "am_proc_start:" }
    val actStartFilter = groups["system-events.filter"]!!.first { it.name == "ActivityManager: Set .+: top-activity" }

    val items = listOf(
      "10-16 17:12:07.465  2635  6233 I framework_stats_log: [SCHEDULED_JOB_STATE_CHANGED, [10379], [null], com.motorola.batterycare/androidx.work.impl.background.systemjob.SystemJobService, 1, -1, 0, 11790, false, false, false, false, false, false, false, false, false, false, 0, false, 300, 300, 0, 0, false, false, false, false, false, false, false, false, 3, false, false, false, 0, -1, -1, 0, 1005, null, 0, 0, 0, 0, 0, true, true, false, 0, 0, null, [], 0, 2, false]" to null,
      "10-16 17:12:07.466  2635  3063 D PowerManagerService: handleSandman ignore 1" to null,
      "10-16 17:12:07.468  2203  2203 D Zygote  : Forked child process 18681" to null,
      "10-16 17:12:07.469  2635  2975 I framework_stats_log: [PROCESS_LIFE_CYCLE_STATE_CHANGED, 10310, com.google.android.apps.turbo, 1]" to null,
      "10-16 17:12:07.469  2635  2975 I am_proc_start: [0,18681,10310,com.google.android.apps.turbo,broadcast,{com.google.android.apps.turbo/com.google.android.apps.turbo.deadline.library.GGaugeUpdateReceiver}]" to amStartFilter,
      "10-16 17:12:07.469  2635  2975 I ActivityManager: Start proc 18681:com.google.android.apps.turbo/u0a310 for broadcast {com.google.android.apps.turbo/com.google.android.apps.turbo.deadline.library.GGaugeUpdateReceiver}" to actStartFilter,
      "10-16 17:12:07.475  8572 14364 I BatteryCare: [SmartBackground] [BatteryCareTaskService] Receive event processWork com.motorola.batterycare.services.ACTION_NOTIFY_EVENT" to null,
      "10-16 17:12:07.476  8572  9297 I WM-WorkerWrapper: Worker result SUCCESS for Work [ id=a2cc0c0d-5c8d-4c13-b935-8c0aae0137c3, tags={ com.motorola.batterycare.services.BatteryCareTaskWorker } ]" to null,
      "10-16 17:12:07.477  2635  6233 I framework_stats_log: [SCHEDULED_JOB_STATE_CHANGED, [10379], [null], com.motorola.batterycare/androidx.work.impl.background.systemjob.SystemJobService, 0, 10, 0, 11790, false, false, false, false, false, false, false, false, false, false, 0, false, 300, 300, 0, 0, false, false, false, false, false, false, false, false, 3, false, false, false, 0, -1, -1, 0, 1005, null, 0, 0, 0, 0, 0, true, true, false, 0, 0, null, [], 0, 2, false]" to null
    )
    return items.mapIndexed { idx, (text, filter) ->
      LogLineParser.createLogEntry(text, idx + 1, LogStream.MAIN).apply {
        index = idx
        appliedFilter = filter
      }
    }
  }

  private fun buildLiveFilteredLogs(groups: Map<String, List<Filter>>): List<LogEntry> {
    val slowFilter = groups["Performance.filter"]!!.first { it.name == "slow dispatch" }
    val amStartFilter = groups["system-events.filter"]!!.first { it.name == "am_proc_start:" }
    val actStartFilter = groups["system-events.filter"]!!.first { it.name == "ActivityManager: Set .+: top-activity" }

    val items = listOf(
      "10-16 17:11:53.532  2635  5517 D Binder  : Slow dispatch took 169 ms, thread: binder:2635_7 for { InterfaceDescriptor=android.os.IPowerManager code=1598311760 }" to slowFilter,
      "10-16 17:11:53.800  2635  5517 D Binder  : Slow dispatch took 204 ms, thread: binder:2635_7 for { InterfaceDescriptor=android.os.IPowerManager code=1598311760 }" to slowFilter,
      "10-16 17:12:07.469  2635  2975 I am_proc_start: [0,18681,10310,com.google.android.apps.turbo,broadcast,{com.google.android.apps.turbo/com.google.android.apps.turbo.deadline.library.GGaugeUpdateReceiver}]" to amStartFilter,
      "10-16 17:12:07.469  2635  2975 I ActivityManager: Start proc 18681:com.google.android.apps.turbo/u0a310 for broadcast {com.google.android.apps.turbo/com.google.android.apps.turbo.deadline.library.GGaugeUpdateReceiver}" to actStartFilter,
      "10-16 17:12:07.544  2635  2975 I am_proc_start: [10,18692,1010310,com.google.android.apps.turbo,broadcast,{com.google.android.apps.turbo/com.google.android.apps.turbo.deadline.library.GGaugeUpdateReceiver}]" to amStartFilter,
      "10-16 17:12:07.544  2635  2975 I ActivityManager: Start proc 18692:com.google.android.apps.turbo/u10a310 for broadcast {com.google.android.apps.turbo/com.google.android.apps.turbo.deadline.library.GGaugeUpdateReceiver}" to actStartFilter,
      "10-16 17:12:13.664  2635  5517 D Binder  : Slow dispatch took 201 ms, thread: binder:2635_7 for { InterfaceDescriptor=android.os.IPowerManager code=1598311760 }" to slowFilter,
      "10-16 17:12:13.890  2635  5517 D Binder  : Slow dispatch took 156 ms, thread: binder:2635_7 for { InterfaceDescriptor=android.os.IPowerManager code=1598311760 }" to slowFilter,
      "10-16 17:12:33.807  2635  6225 D Binder  : Slow dispatch took 200 ms, thread: binder:2635_16 for { InterfaceDescriptor=android.os.IPowerManager code=1598311760 }" to slowFilter
    )
    return items.mapIndexed { idx, (text, filter) ->
      LogLineParser.createLogEntry(text, idx + 1, LogStream.MAIN).apply {
        index = idx
        appliedFilter = filter
      }
    }
  }

  private fun buildSampleAppPackages(): List<AppPackage> {
    val bipsDetails = """
      Package [com.android.bips] (b63b4ce):
        appId=10210
        pkg=Package{58ea53f com.android.bips}
        codePath=/system/priv-app/BuiltInPrintService
        resourcePath=/system/priv-app/BuiltInPrintService
        legacyNativeLibraryDir=/system/priv-app/BuiltInPrintService/lib
        extractNativeLibs=true
        primaryCpuAbi=arm64-v8a
        secondaryCpuAbi=null
        cpuAbiOverride=null
        versionCode=37 minSdk=37 targetSdk=37
        minExtensionVersions=[]
        versionName=17
        hiddenApiEnforcementPolicy=0
        usesNonSdkApi=false
        splits=[base]
        apkSigningVersion=3
        flags=[ SYSTEM HAS_CODE ALLOW_CLEAR_USER_DATA ALLOW_BACKUP KILL_AFTER_RESTORE ]
        privateFlags=[ PRIVATE_FLAG_ACTIVITIES_RESIZE_MODE_RESIZEABLE_VIA_SDK_VERSION ALLOW_AUDIO_PLAYBACK_CAPTURE PRIVILEGED PRIVATE_FLAG_ALLOW_NATIVE_HEAP_POINTER_TAGGING ]
        forceQueryable=false
        pageSizeCompat=0
    """.trimIndent()

    val entries = listOf(
      Triple("android", "37", "17"),
      Triple("android.auto_generated_characteristics_rro", "37", "17"),
      Triple("android.auto_generated_rro_product__", "1", "1.0"),
      Triple("android.auto_generated_rro_vendor__", "1", "1.0"),
      Triple("android.autoinstalls.config.motorola.layout", "6006", "MAIN-MAKALU-COMCAST-6006"),
      Triple("cci.usage", "1739893769", "9.0.0"),
      Triple("com.amazon.appmanager", "140820", "1.1.36.0"),
      Triple("com.android.DeviceAsWebcam", "37", "17"),
      Triple("com.android.appsearch.aiseal.config", "37", "17"),
      Triple("com.android.avatarpicker", "37", "17"),
      Triple("com.android.backupconfirm", "37", "17"),
      Triple("com.android.bips", "37", "17"),
      Triple("com.android.bluetooth", "37", "17"),
      Triple("com.android.bluetoothmidiservice", "37", "17"),
      Triple("com.android.bookmarkprovider", "37", "17"),
      Triple("com.android.calllogbackup", "37", "17"),
      Triple("com.android.cameraextensions", "37", "17"),
      Triple("com.android.carrierdefaultapp", "37", "17"),
      Triple("com.android.cellbroadcast.overlay", "37", "17"),
      Triple("com.android.cellbroadcastreceiver", "300000000", "R-initial"),
      Triple("com.android.cellbroadcastservice.overlay", "37", "17"),
      Triple("com.android.certinstaller", "37", "17"),
      Triple("com.android.chrome", "805903904", "155.0.8059.39"),
      Triple("com.android.companiondevicemanager", "37", "17"),
      Triple("com.android.companiondevicemanager.auto_generated_characteristics_rro", "37", "17")
    )
    return entries.map { (pkg, code, ver) ->
      AppPackage(pkg, code, ver, "/data/user/0/$pkg", if (pkg == "com.android.bips") bipsDetails else "Package [$pkg]")
    }
  }

  private fun buildSearchDialogFilterGroups(): Map<String, List<Filter>> {
    fun f(name: String, pattern: String, color: Color, applied: Boolean = true): Filter {
      return Filter(name, pattern, color, LogLevel.VERBOSE, false).apply { isApplied = applied }
    }
    return linkedMapOf(
      "IMS-RIL.filter" to listOf(
        f("[<>] IMS_REGISTRATION_STATE", "[<>] IMS_REGISTRATION_STATE", Color(195, 185, 195), applied = false)
      ),
      "LPA.filter" to listOf(
        f("EuiccGoogle", "EuiccGoogle", Color(255, 90, 250)),
        f("Send request to SM-DP\\+:", "Send request to SM-DP\\+:", Color(80, 235, 80)),
        f("SM-DP\\+ response:", "SM-DP\\+ response:", Color(80, 235, 235)),
        f("ActivationActivity.startPreActivationFlow", "ActivationActivity.startPreActivationFlow", Color(83, 199, 246)),
        f("Successfully installed profile.", "Successfully installed profile.", Color(222, 143, 143)),
        f("Successfully download profile.", "Successfully download profile.", Color(222, 143, 143)),
        f("Error installing profile.", "Error installing profile.", Color(255, 90, 250)),
        f("DownloadFragment.downloadNextSubOrDeliverResults:", "DownloadFragment.downloadNextSubOrDeliverResults:", Color(172, 224, 249)),
        f("EuiccGoogle: .* Carrier.* is chosen.", "EuiccGoogle: .* Carrier.* is chosen.", Color(235, 235, 50)),
        f("EuiccServiceImpl.onSwitchToSubscriptionWithPort", "EuiccServiceImpl.onSwitchToSubscriptionWithPort", Color(172, 224, 249)),
        f("EuiccGoogle:.*disableProfile:", "EuiccGoogle:.*disableProfile:", Color(83, 199, 246)),
        f("EuiccServiceImpl.onDeleteSubscription:", "EuiccServiceImpl.onDeleteSubscription:", Color(80, 235, 80))
      )
    )
  }
}
