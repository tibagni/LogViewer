package com.tibagni.logviewer.bugreport.section.ui

import com.tibagni.logviewer.bugreport.section.*

object SectionPanelFactory {
  fun createPanelFor(sectionName: String, section: BugReportSection): SectionPanel {
    return when (sectionName) {
      SectionNames.PROPERTIES -> PropertiesSectionPanel(section as PropertiesSection)
      SectionNames.PLAIN_TEXT -> PlainTextBugreportPanel(section as PlainTextSection)
      SectionNames.APPLICATION_PKG -> PackageSectionPanel(section as PackagesSection)
      SectionNames.SYSTEM_HIDDEN_PKG -> PackageSectionPanel(section as PackagesSection)
      SectionNames.CARRIER_CONFIG -> CarrierConfigSectionPanel(section as CarrierConfigSection)
      SectionNames.SUBSCRIPTIONS -> SubscriptionsSectionPanel(section as SubscriptionsSection)
      else -> object : SectionPanel("Invalid Section") {}// Should never reach here
    }
  }
}