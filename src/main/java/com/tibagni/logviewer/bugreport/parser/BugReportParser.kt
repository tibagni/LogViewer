package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.ProgressReporter
import com.tibagni.logviewer.bugreport.BugReport
import java.io.File

interface BugReportParser {
  fun parseBugReport(bugReportFile: File, progressReporter: ProgressReporter): BugReport
}

class BugReportParserImpl(private val sectionParsers: List<BugReportSectionParser>): BugReportParser {
  override fun parseBugReport(bugReportFile: File, progressReporter: ProgressReporter): BugReport {
    // BugReport String is usually massive (several tens of MB). For this reason, we save the original bugreport
    // String and be careful to use StringViews to manipulate each section and only create a substring when absolutely
    // necessary
    val bugReportText = bugReportFile.inputStream().bufferedReader().readText()
    val sections = sectionParsers.mapNotNull { it.parse(bugReportText) }

    return BugReport(bugReportText, sections)
  }
}