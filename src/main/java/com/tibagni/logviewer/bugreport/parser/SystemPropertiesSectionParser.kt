package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.bugreport.section.BugReportSection
import com.tibagni.logviewer.bugreport.section.SystemPropertiesSection
import com.tibagni.logviewer.stringView

class SystemPropertiesSectionParser : BugReportSectionParser {

  override val name = "SystemProperties"
  override fun parse(bugreportPath: String, bugReportText: String): BugReportSection? {

    val sysPropSectionStart = "\\n-+ SYSTEM PROPERTIES.* -+\\n"
    val sysPropSectionEnd = "\\n-+ .*SYSTEM PROPERTIES.* -+\\n"
    val sysPropStartMatch = sysPropSectionStart.toRegex().find(bugReportText) ?: return null
    val sysPropEndMatch = sysPropSectionEnd.toRegex().find(bugReportText, sysPropStartMatch.range.last) ?: return null
    val sysPropSection = bugReportText.stringView(sysPropStartMatch.range.first + 1, sysPropEndMatch.range.last)

    return SystemPropertiesSection(sysPropSection.lines()
      .filter { line -> line.matches(Regex("\\[.+]: ?\\[.+]")) }
      .associate { line ->
        val (key, value) = line.split(":").map { it.trim() }
        key to value
      })
  }
}