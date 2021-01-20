package com.tibagni.logviewer.bugreport.section.ui

import com.tibagni.logviewer.bugreport.section.BugReportSection
import com.tibagni.logviewer.bugreport.section.PackagesSection
import com.tibagni.logviewer.bugreport.section.PlainTextSection
import com.tibagni.logviewer.bugreport.section.SectionNames

object SectionPanelFactory {
  fun createPanelFor(sectionName: String, section: BugReportSection): SectionPanel {
    return when(sectionName) {
      SectionNames.PLAIN_TEXT -> PlainTextBugreportPanel(section as PlainTextSection)
      SectionNames.APPLICATION_PKG -> PackageSectionPanel(section as PackagesSection)
      SectionNames.SYSTEM_HIDDEN_PKG -> PackageSectionPanel(section as PackagesSection)
      else -> object: SectionPanel("Invalid Section") {}// Should never reach here
    }
  }
}