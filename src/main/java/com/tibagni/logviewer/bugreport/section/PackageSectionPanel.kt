package com.tibagni.logviewer.bugreport.section

import com.tibagni.logviewer.bugreport.PackagesSection
import com.tibagni.logviewer.bugreport.content.AppPackage
import javax.swing.*

class PackageSectionPanel(section: PackagesSection) : ListSectionPanel(section.sectionName) {

  private val packagesMap: Map<String, AppPackage> = mapOf(*(section.packages.map { it.toSummary() to it }.toTypedArray()))
  private val sortedPackagesSummaries: List<String> = section.packages.map { it.toSummary() }.sorted()

  init {
    updateListData(sortedPackagesSummaries)
  }

  override fun onSearch(searchText: String) {
    val filtered = sortedPackagesSummaries.filter { it.contains(searchText) }
    SwingUtilities.invokeLater { updateListData(filtered) }
  }

  override fun onItemSelected(selectedValue: String) {
    val pkg = packagesMap[selectedValue]
    setDetailsText(pkg?.rawText?.toString())
  }
}