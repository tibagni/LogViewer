package com.tibagni.logviewer.bugreport.section.ui

import com.tibagni.logviewer.bugreport.content.AppPackage
import com.tibagni.logviewer.bugreport.section.PackagesSection
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