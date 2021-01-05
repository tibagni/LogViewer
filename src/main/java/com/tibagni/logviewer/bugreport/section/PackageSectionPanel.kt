package com.tibagni.logviewer.bugreport.section

import com.tibagni.logviewer.bugreport.PackagesSection
import com.tibagni.logviewer.bugreport.content.AppPackage
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.PaddingListCellRenderer
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

class PackageSectionPanel(section: PackagesSection) : SectionPanel(section.sectionName, isSearchable = true) {
  private lateinit var packagesListModel: DefaultListModel<String>
  private lateinit var packagesList: JList<String>
  private lateinit var splitPane: JSplitPane
  private lateinit var detailsText: JTextArea

  private val packagesMap: Map<String, AppPackage>
  private val sortedPackagesSummaries: List<String>

  init {
    buildUi()

    sortedPackagesSummaries = section.packages.map { it.toSummary() }.sorted()
    packagesMap = mapOf(*(section.packages.map { it.toSummary() to it }.toTypedArray()))

    // Initialize data and setup listeners
    packagesListModel.addAll(sortedPackagesSummaries)
    packagesList.addListSelectionListener { onPackageSelected() }
  }

  override fun onSearch(searchText: String) {
    val filtered = sortedPackagesSummaries.filter { it.contains(searchText) }
    SwingUtilities.invokeLater { updatePackagesList(filtered) }
  }

  private fun onPackageSelected() {
    if (packagesList.selectedIndex > packagesListModel.size() || packagesList.selectedIndex < 0) {
      return
    }

    val selected = packagesListModel[packagesList.selectedIndex]
    val pkg = packagesMap[selected]
    detailsText.text = pkg?.rawText?.toString()
    detailsText.caretPosition = 0
  }

  private fun updatePackagesList(packages: List<String>) {
    packagesListModel.removeAllElements()
    packagesListModel.addAll(packages)
    packagesList.invalidate()
  }

  private fun buildUi() {
    packagesListModel = DefaultListModel()
    packagesList = JList(packagesListModel)
    packagesList.selectionMode = ListSelectionModel.SINGLE_SELECTION
    packagesList.cellRenderer = PaddingListCellRenderer(UIScaleUtils.dip(2))

    splitPane = JSplitPane()
    splitPane.orientation = JSplitPane.VERTICAL_SPLIT
    splitPane.resizeWeight = 0.5
    add(
      splitPane,
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(2)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )

    val detailsPane = JPanel()
    detailsPane.layout = GridBagLayout()
    detailsText = JTextArea()
    detailsText.isEditable = false
    detailsText.wrapStyleWord = true
    detailsPane.add(
      JScrollPane(detailsText),
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(1)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )

    splitPane.leftComponent = JScrollPane(packagesList) // Left or above (above in this case)
    splitPane.rightComponent = detailsPane
  }
}