package com.tibagni.logviewer.bugreport.section

data class PropertiesSection(
  val build: String,
  val fingerprint: String,
  val bootloader: String,
  val kernel: String,
  val uptime: String
) : BugReportSection(SectionNames.PROPERTIES)