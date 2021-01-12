package com.tibagni.logviewer.bugreport

import com.tibagni.logviewer.AsyncPresenter
import com.tibagni.logviewer.MainView
import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.View
import com.tibagni.logviewer.bugreport.section.SectionPanel
import com.tibagni.logviewer.bugreport.section.SectionPanelFactory
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.PaddingListCellRenderer
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

interface BugReportView: View {
  val contentPane: JPanel
}

interface BugReportPresenterView: AsyncPresenter.AsyncPresenterView {
  fun showBugReport(bugReport: BugReport)
  fun showErrorMessage(message: String?)
}

class BugReportViewImpl(private val mainView: MainView) : BugReportView, BugReportPresenterView {
  override val contentPane: JPanel = JPanel()

  private lateinit var sectionsList: JList<String>
  private lateinit var sectionsListModel: DefaultListModel<String>
  private lateinit var openBrButton: JButton
  private lateinit var mainSplitPane: JSplitPane

  private val sectionPanels = mutableMapOf<String, SectionPanel>()
  private var bugReport: BugReport? = null
  private var currentlySelectedSection = -1

  private val presenter: BugReportPresenter

  init {
    buildUi()
    presenter = BugReportPresenterImpl(this, ServiceLocator.bugReportRepository)

    openBrButton.addActionListener {
      val file = mainView.showOpenSingleLogFileChooser()
      file?.let { presenter.loadBugReport(file) }
    }
    sectionsList.addListSelectionListener { onSectionSelected() }
  }

  override fun showBugReport(bugReport: BugReport) {
    sectionsListModel.clear()
    sectionsListModel.addAll(
      bugReport.sections.map { it.sectionName }
    )
    this.bugReport = bugReport
    sectionsList.selectedIndex = 0
  }

  override fun showStartLoading() = mainView.showStartLoading()

  override fun showLoadingProgress(progress: Int, note: String?) = mainView.showLoadingProgress(progress, note)

  override fun finishLoading() = mainView.finishLoading()

  override fun requestFinish(doFinish: () -> Unit) {
    presenter.finishing()
    doFinish()
  }

  private fun onSectionSelected() {
    val selected = sectionsList.selectedIndex

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
      mainSplitPane.rightComponent = sectionPanels[sectionName]
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
    mainSplitPane.isOneTouchExpandable = true
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

    val emptyPanel = JPanel()
    openBrButton = JButton("Open bug report...")
    emptyPanel.layout = GridBagLayout()
    emptyPanel.add(
      openBrButton, GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(1)
        .withIpadx(UIScaleUtils.dip(10))
        .withIpady(UIScaleUtils.dip(10))
        .build()
    )

    mainSplitPane.leftComponent = sectionsList
    mainSplitPane.rightComponent = emptyPanel
  }
}