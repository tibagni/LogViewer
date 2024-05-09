package com.tibagni.logviewer.bugreport.section

data class SystemPropertiesSection(val configs: Map<String, String>) :
  BugReportSection(SectionNames.SYSTEM_PROPERTIES)
