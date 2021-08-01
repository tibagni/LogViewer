package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.bugreport.section.BugReportSection
import com.tibagni.logviewer.bugreport.section.PlainTextSection

class PlainTextContentParser : BugReportSectionParser {
  override val name: String
    get() = "Raw"

  override fun parse(bugreportPath: String, bugReportText: String): BugReportSection {
    return PlainTextSection(bugreportPath)
  }
}