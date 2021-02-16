package com.tibagni.logviewer.bugreport.section

data class CarrierConfigSection(val configs: Map<String, Map<String, String>>, val loadingLogs: String) :
  BugReportSection(SectionNames.CARRIER_CONFIG)