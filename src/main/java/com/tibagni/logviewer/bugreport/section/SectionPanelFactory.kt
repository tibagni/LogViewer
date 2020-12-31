package com.tibagni.logviewer.bugreport.section

import com.tibagni.logviewer.bugreport.BugReportSection
import com.tibagni.logviewer.bugreport.PackagesSection
import javax.swing.JLabel
import javax.swing.JPanel

object SectionPanelFactory {
  fun createPanelFor(sectionName: String, section: BugReportSection): SectionPanel {
    return when(sectionName) {
      SectionNames.APPLICATION_PKG -> PackageSectionPanel(section as PackagesSection)
      SectionNames.SYSTEM_HIDDEN_PKG -> PackageSectionPanel(section as PackagesSection)
      else -> object: SectionPanel("Invalid Section") {}.also { it.add(JLabel("Invalid Section")) } // Should never reach here
    }
  }
}