package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.bugreport.section.BugReportSection

interface BugReportSectionParser {
  val name: String
  fun parse(bugreportPath: String, bugReportText: String): BugReportSection?
}