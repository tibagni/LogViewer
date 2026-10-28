package com.tibagni.logviewer.bugreport

import com.tibagni.logviewer.AsyncPresenter
import com.tibagni.logviewer.MainView
import com.tibagni.logviewer.MainViewTab
import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.bugreport.section.EmptyBugReportSection
import com.tibagni.logviewer.bugreport.section.ui.SectionPanel
import com.tibagni.logviewer.bugreport.section.ui.SectionPanelFactory
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.StringUtils
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.CardBorderUtils
import com.tibagni.logviewer.view.CommonIcons
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*
import javax.swing.border.EmptyBorder

interface BugReportView : MainViewTab {
  fun onBugReportLoaded(bugreportPath: String, bugreportText: String)
  fun onBugReportClosed()
}

interface BugReportPresenterView : AsyncPresenter.AsyncPresenterView {
  fun showBugReport(bugReport: BugReport)
  fun showErrorMessage(message: String?)
}

/**
 * Bugreport tab view showing a sidebar of parsed bugreport sections on the left
 * and detailed section panels on the right within a split pane.
 */
class BugReportViewImpl(private val mainView: MainView) : BugReportView, BugReportPresenterView {
  override val contentPane: JPanel = JPanel()

  @VisibleForTesting
  internal lateinit var sectionsList: JList<String>
  @VisibleForTesting
  internal lateinit var sectionsListModel: DefaultListModel<String>
  @VisibleForTesting
  internal lateinit var mainSplitPane: JSplitPane
  @VisibleForTesting
  internal lateinit var sidebarPanel: JPanel
  @VisibleForTesting
  internal lateinit var sidebarHeaderLabel: JLabel

  private val sectionPanels = mutableMapOf<String, SectionPanel>()
  private var bugReport: BugReport? = null
  private var currentlySelectedSection = -1

  private val presenter: BugReportPresenter

  @VisibleForTesting
  internal val emptyPane = JPanel()

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
    sectionPanels.clear()
    this.bugReport = null
    currentlySelectedSection = -1
    mainSplitPane.rightComponent = emptyPane
  }

  override fun onThemeChanged() {
    // Force panels to be recreated since theme changed
    sectionPanels.clear()
    sidebarHeaderLabel.icon = CommonIcons.viewListIcon()
    sectionsList.cellRenderer = SectionListCellRenderer()
    contentPane.revalidate()
    contentPane.repaint()
  }

  override fun showBugReport(bugReport: BugReport) {
    sectionsListModel.clear()
    bugReport.sections.forEach { sectionsListModel.addElement(it.sectionName) }
    this.bugReport = bugReport
    sectionsList.selectedIndex = 0
  }

  override fun showStartLoading() = mainView.showStartLoading("BR")

  override fun showLoadingProgress(progress: Int, note: String?) =
    mainView.showLoadingProgress("BR", progress, note)

  override fun finishLoading() = mainView.finishLoading("BR")

  override fun requestFinish(doFinish: () -> Unit) {
    presenter.finishing()
    doFinish()
  }

  private fun onSectionSelected() {
    val selected = sectionsList.selectedIndex

    if (selected < 0) {
      // If there is nothing selected on sections list, show empty pane
      currentlySelectedSection = -1
      mainSplitPane.rightComponent = emptyPane
      return
    }

    if (selected != currentlySelectedSection) {
      currentlySelectedSection = selected
      val sectionName = sectionsListModel[selected]
      swapSectionPanel(sectionName)
    }
  }

  /**
   * Swaps the right component of [mainSplitPane] to the panel corresponding to
   * [sectionName], instantiating the [SectionPanel] lazily if needed.
   *
   * @param sectionName the name of the parsed bugreport section to display.
   */
  private fun swapSectionPanel(sectionName: String) {
    bugReport?.let {
      if (sectionName !in sectionPanels) {
        val section = it.sectionsMap[sectionName] ?: EmptyBugReportSection
        sectionPanels[sectionName] = SectionPanelFactory.createPanelFor(sectionName, section)
      }

      val dividerLocation = mainSplitPane.dividerLocation
      val isFirstSwap = (mainSplitPane.rightComponent == emptyPane)

      mainSplitPane.rightComponent = sectionPanels[sectionName]

      if (!isFirstSwap) {
        mainSplitPane.dividerLocation = dividerLocation
      }
    }
  }

  override fun showErrorMessage(message: String?) {
    JOptionPane.showMessageDialog(
      contentPane,
      message,
      I18n.get(I18n.COMMON_ERROR),
      JOptionPane.ERROR_MESSAGE
    )
  }

  private fun buildUi() {
    sectionsListModel = DefaultListModel()
    sectionsList = JList(sectionsListModel).apply {
      cellRenderer = SectionListCellRenderer()
      selectionMode = ListSelectionModel.SINGLE_SELECTION
    }

    val headerPanel = JPanel(BorderLayout()).apply {
      border = EmptyBorder(
        UIScaleUtils.dip(10),
        UIScaleUtils.dip(12),
        UIScaleUtils.dip(8),
        UIScaleUtils.dip(12)
      )
      sidebarHeaderLabel = JLabel(I18n.get(I18n.BUGREPORT_SECTIONS_TITLE)).apply {
        icon = CommonIcons.viewListIcon()
        font = font.deriveFont(Font.BOLD, UIScaleUtils.dip(13).toFloat())
      }
      add(sidebarHeaderLabel, BorderLayout.WEST)
    }

    val scrollPane = JScrollPane(sectionsList).apply {
      border = BorderFactory.createEmptyBorder()
      viewportBorder = null
      verticalScrollBar.unitIncrement = 16
    }

    sidebarPanel = JPanel(BorderLayout()).apply {
      add(headerPanel, BorderLayout.NORTH)
      add(scrollPane, BorderLayout.CENTER)
      minimumSize = Dimension(UIScaleUtils.dip(180), UIScaleUtils.dip(150))
      preferredSize = Dimension(UIScaleUtils.dip(220), UIScaleUtils.dip(400))
    }

    contentPane.layout = GridBagLayout()
    mainSplitPane = JSplitPane().apply {
      dividerSize = UIScaleUtils.dip(5)
      isOneTouchExpandable = true
      resizeWeight = 0.15
    }

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
    val emptyContainer = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      isOpaque = false
      val emptyIcon = JLabel(CommonIcons.fileDocumentIcon(UIScaleUtils.dip(40))).apply {
        alignmentX = Component.CENTER_ALIGNMENT
      }
      val emptyTitle = JLabel(I18n.get(I18n.BUGREPORT_NO_SECTION_SELECTED)).apply {
        font = font.deriveFont(Font.BOLD, UIScaleUtils.dip(14).toFloat())
        alignmentX = Component.CENTER_ALIGNMENT
        border = EmptyBorder(UIScaleUtils.dip(10), 0, UIScaleUtils.dip(6), 0)
      }
      val emptyHint = JLabel(StringUtils.wrapHtml(I18n.get(I18n.BUGREPORT_OPEN_HINT))).apply {
        foreground = CardBorderUtils.resolveSecondaryLabelColor()
        alignmentX = Component.CENTER_ALIGNMENT
      }
      add(emptyIcon)
      add(emptyTitle)
      add(emptyHint)
    }

    emptyPane.add(
      emptyContainer,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(0)
        .withAnchor(GridBagConstraints.CENTER)
        .build()
    )

    mainSplitPane.leftComponent = sidebarPanel
    mainSplitPane.rightComponent = emptyPane
  }
}

/**
 * Modern list cell renderer for bugreport sections with file document vector icons
 * and consistent spacing.
 */
private class SectionListCellRenderer : DefaultListCellRenderer() {
  private val fileIcon = CommonIcons.fileDocumentIcon(UIScaleUtils.dip(15))
  private val cellBorder = EmptyBorder(
    UIScaleUtils.dip(6),
    UIScaleUtils.dip(10),
    UIScaleUtils.dip(6),
    UIScaleUtils.dip(10)
  )

  override fun getListCellRendererComponent(
    list: JList<*>?,
    value: Any?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    val component = super.getListCellRendererComponent(
      list,
      value,
      index,
      isSelected,
      cellHasFocus
    )
    if (component is JLabel) {
      component.icon = fileIcon
      component.border = cellBorder
    }
    return component
  }
}
