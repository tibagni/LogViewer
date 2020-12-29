package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.bugreport.BugReportSection

interface BugReportSectionParser {
  fun parse(bugReportText: String): BugReportSection?
}