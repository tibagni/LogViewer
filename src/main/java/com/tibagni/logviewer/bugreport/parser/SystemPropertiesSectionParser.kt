package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.bugreport.section.BugReportSection
import com.tibagni.logviewer.bugreport.section.SystemPropertiesSection
import com.tibagni.logviewer.stringView

class SystemPropertiesSectionParser : BugReportSectionParser {

  companion object {
    private val SYS_PROP_SECTION_START_REGEX = "\\n-+ SYSTEM PROPERTIES.* -+\\n".toRegex()
    private val SYS_PROP_SECTION_END_REGEX = "\\n-+ .*SYSTEM PROPERTIES.* -+\\n".toRegex()
    private val SYS_PROP_LINE_REGEX = Regex("\\[.+]: ?\\[.+]")
  }

  override val name = "SystemProperties"
  override fun parse(bugreportPath: String, bugReportText: String): BugReportSection? {

    val sysPropStartMatch = SYS_PROP_SECTION_START_REGEX.find(bugReportText) ?: return null
    val sysPropEndMatch = SYS_PROP_SECTION_END_REGEX.find(bugReportText, sysPropStartMatch.range.last) ?: return null
    val sysPropSection = bugReportText.stringView(sysPropStartMatch.range.first + 1, sysPropEndMatch.range.last)

    return SystemPropertiesSection(sysPropSection.lines()
      .filter { line -> line.matches(SYS_PROP_LINE_REGEX) }
      .associate { line ->
        val (key, value) = line.split(":").map { it.trim() }
        key to value
      })
  }
}