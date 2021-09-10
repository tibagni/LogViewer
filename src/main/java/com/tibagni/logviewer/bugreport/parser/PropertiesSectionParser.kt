package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.bugreport.section.BugReportSection
import com.tibagni.logviewer.bugreport.section.PropertiesSection

class PropertiesSectionParser : BugReportSectionParser {
  override val name = "BugreportInfo"
  companion object {
    private const val NOT_FOUND = "Not Found"
  }

  override fun parse(bugreportPath: String, bugReportText: String): BugReportSection {
    val build = parseBRProperty(bugReportText, "Build: ")
    val buildFingerprint = parseBRProperty(bugReportText, "Build fingerprint: ")
    val bootloader = parseBRProperty(bugReportText, "Bootloader: ")
    val kernel = parseBRProperty(bugReportText, "Kernel: ")
    val uptime = parseBRProperty(bugReportText, "Uptime: ")

    return PropertiesSection(build, buildFingerprint, bootloader, kernel, uptime)
  }

  private fun parseBRProperty(text: String, prefix: String): String {
    val match = "\\n$prefix.*\\n".toRegex().find(text)
    return if (match != null) {
      val offset = prefix.length
      text.substring(match.range.first + offset, match.range.last).trim()
    } else NOT_FOUND
  }
}