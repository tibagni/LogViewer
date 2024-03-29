package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.ProgressReporter
import com.tibagni.logviewer.bugreport.BugReport
import com.tibagni.logviewer.logger.wrapProfiler

interface BugReportParser {
  fun parseBugReport(bugreportPath: String, bugReportText: String, progressReporter: ProgressReporter): BugReport
}

class BugReportParserImpl(private val sectionParsers: List<BugReportSectionParser>) : BugReportParser {
  override fun parseBugReport(
    bugreportPath: String,
    bugReportText: String,
    progressReporter: ProgressReporter
  ): BugReport {
    // BugReport String is usually massive (several tens of MB). For this reason, we save the original bugreport
    // String and be careful to use StringViews to manipulate each section and only create a substring when absolutely
    // necessary
    progressReporter.onProgress(0, "Opening bugreport")
    var sectionsParsed = 1
    val totalSections = sectionParsers.size

    val sections = sectionParsers.mapNotNull {
      val progress = (sectionsParsed.toDouble() / totalSections) * 100
      sectionsParsed++
      progressReporter.onProgress(progress.toInt(), "Parsing ${it.name}")
      wrapProfiler(it.name) { it.parse(bugreportPath, bugReportText) }
    }
    progressReporter.onProgress(100, "Done!")

    return BugReport(bugReportText, sections)
  }
}