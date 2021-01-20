package com.tibagni.logviewer.bugreport

import com.tibagni.logviewer.bugreport.section.BugReportSection

/**
 * A representation of a BugReport after parsed
 */
data class BugReport(val rawText: String, val sections: List<BugReportSection>) {
  val sectionsMap = mapOf(*(sections.map { it.sectionName to it }.toTypedArray()))
}