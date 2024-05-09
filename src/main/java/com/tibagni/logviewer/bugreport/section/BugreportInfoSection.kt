package com.tibagni.logviewer.bugreport.section

data class BugreportInfoSection(
  val build: String,
  val fingerprint: String,
  val bootloader: String,
  val kernel: String,
  val uptime: String
) : BugReportSection(SectionNames.BUGREPORT_INFO)