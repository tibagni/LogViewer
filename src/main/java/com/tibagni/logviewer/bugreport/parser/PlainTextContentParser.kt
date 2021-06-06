package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.bugreport.section.BugReportSection
import com.tibagni.logviewer.bugreport.section.PlainTextSection

class PlainTextContentParser : BugReportSectionParser {
  override val name: String
    get() = "Raw"

  override fun parse(bugReportText: String): BugReportSection {
    // TODO improve performance
    return PlainTextSection(bugReportText.lines())
  }
}