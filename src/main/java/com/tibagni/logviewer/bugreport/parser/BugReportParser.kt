package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.bugreport.BugReport
import java.io.File

class BugReportParser(private val bugReportFile: File, private val sectionParsers: List<BugReportSectionParser>) {
//  var brp = BugReportParser(
//    File("/home/tbagni/Projects/Personal/br/bugreport.txt"),
//    bugreportSectionParsers
//  )
//  brp.parseBugReport()

  fun parseBugReport(): BugReport {
    // BugReport String is usually massive (several tens of MB). For this reason, we save the original bugreport
    // String and be careful to use StringViews to manipulate each section and only create a substring when absolutely
    // necessary
    val bugReportText = bugReportFile.inputStream().bufferedReader().readText()
    val sections = sectionParsers.mapNotNull { it.parse(bugReportText) }

    return BugReport(bugReportText, sections)
  }
}