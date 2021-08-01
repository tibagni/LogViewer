package com.tibagni.logviewer.bugreport

import com.tibagni.logviewer.AsyncPresenter
import com.tibagni.logviewer.MainView
import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.View
import com.tibagni.logviewer.bugreport.section.EmptyBugReportSection
import com.tibagni.logviewer.bugreport.section.ui.SectionPanel
import com.tibagni.logviewer.bugreport.section.ui.SectionPanelFactory
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.PaddingListCellRenderer
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

interface BugReportView : View {
  val contentPane: JPanel

  fun onBugReportLoaded(bugreportPath: String, bugreportText: String)
  fun onBugReportClosed()
  fun onThemeChanged()
}

interface BugReportPresenterView : AsyncPresenter.AsyncPresenterView {
  fun showBugReport(bugReport: BugReport)
  fun showErrorMessage(message: String?)
}

class BugReportViewImpl(private val mainView: MainView) : BugReportView, BugReportPresenterView {
  override val contentPane: JPanel = JPanel()

  private lateinit var sectionsList: JList<String>
  private lateinit var sectionsListModel: DefaultListModel<String>
  private lateinit var mainSplitPane: JSplitPane

  private val sectionPanels = mutableMapOf<String, SectionPanel>()
  private var bugReport: BugReport? = null
  private var currentlySelectedSection = -1

  private val presenter: BugReportPresenter

  private val emptyPane = JPanel()

  init {
    buildUi()
    presenter = BugReportPresenterImpl(this, ServiceLocator.bugReportRepository)
    sectionsList.addListSelectionListener { onSectionSelected() }
  }

  override fun onBugReportLoaded(bugreportPath: String, bugreportText: String) {
    presenter.loadBugReport(bugreportPath, bugreportText)
  }

  override fun onBugReportClosed() {
    presenter.closeBugReport()
    sectionsListModel.clear()
    this.bugReport = null
  }

  override fun onThemeChanged() {
    // Force panels to be recreated since theme changed
    sectionPanels.clear()
  }

  override fun showBugReport(bugReport: BugReport) {
    sectionsListModel.clear()
    sectionsListModel.addAll(
      bugReport.sections.map { it.sectionName }
    )
    this.bugReport = bugReport
    sectionsList.selectedIndex = 0
  }

  override fun showStartLoading() = mainView.showStartLoading("BR")

  override fun showLoadingProgress(progress: Int, note: String?) = mainView.showLoadingProgress("BR", progress, note)

  override fun finishLoading() = mainView.finishLoading("BR")

  override fun requestFinish(doFinish: () -> Unit) {
    presenter.finishing()
    doFinish()
  }

  private fun onSectionSelected() {
    val selected = sectionsList.selectedIndex

    if (selected < 0) {
      // If there is nothing selected on sections list, show empty pane
      mainSplitPane.rightComponent = emptyPane
      return
    }

    // This callback will be called twice when the selection is changed:
    // once for the newly selected item
    // and again for the unselected item
    // To avoid executing the same thing twice here, keep track of the last selected section
    if (selected != currentlySelectedSection) {
      currentlySelectedSection = selected
      val sectionName = sectionsListModel[selected]
      swapSectionPanel(sectionName)
    }
  }

  private fun swapSectionPanel(sectionName: String) {
    bugReport?.let {
      if (sectionName !in sectionPanels) {
        val section = it.sectionsMap[sectionName] ?: EmptyBugReportSection
        sectionPanels[sectionName] = SectionPanelFactory.createPanelFor(sectionName, section)
      }

      // Here we remember the divider position before we set the right panel, and set it again once the panel is added
      // This is a hack so that the left pane does not keep resizing everytime the right panel changes
      // Also, we don't want to set the divider size on first swap as it is not the right value yet
      val dividerLocation = mainSplitPane.dividerLocation
      val isFirstSwap = (mainSplitPane.rightComponent == emptyPane)

      mainSplitPane.rightComponent = sectionPanels[sectionName]

      if (!isFirstSwap) {
        mainSplitPane.dividerLocation = dividerLocation
      }

    }
  }

  override fun showErrorMessage(message: String?) {
    JOptionPane.showMessageDialog(contentPane, message, "Error...", JOptionPane.ERROR_MESSAGE)
  }

  private fun buildUi() {
    sectionsListModel = DefaultListModel()
    sectionsList = JList(sectionsListModel)
    sectionsList.cellRenderer = PaddingListCellRenderer(UIScaleUtils.dip(5))
    sectionsList.selectionMode = ListSelectionModel.SINGLE_SELECTION

    contentPane.layout = GridBagLayout()
    mainSplitPane = JSplitPane()
    mainSplitPane.dividerSize = UIScaleUtils.dip(5)
    mainSplitPane.isOneTouchExpandable = true
    mainSplitPane.resizeWeight = 0.05
    contentPane.add(
      mainSplitPane,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(0)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )

    emptyPane.layout = GridBagLayout()
    emptyPane.add(
      JLabel("<html>To open a bugreport go to '<i><u><b>Logs > Open logs...</b></u></i>'</html>"),
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(1)
        .withIpadx(UIScaleUtils.dip(10))
        .withIpady(UIScaleUtils.dip(10))
        .build()
    )

    mainSplitPane.leftComponent = sectionsList
    mainSplitPane.rightComponent = emptyPane
  }
}