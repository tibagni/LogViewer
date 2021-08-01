package com.tibagni.logviewer.bugreport

import com.tibagni.logviewer.ProgressReporter
import com.tibagni.logviewer.bugreport.parser.BugReportParser

class OpenBugReportException(message: String?, cause: Throwable): java.lang.Exception(message, cause)

interface BugReportRepository {
  val bugReport: BugReport?

  @Throws(OpenBugReportException::class)
  fun loadBugReport(bugreportPath: String, bugReportText: String, progressReporter: ProgressReporter)

  fun closeBugReport()
}

class BugReportRepositoryImpl(private val bugReportParser: BugReportParser): BugReportRepository {
  private var _bugReport: BugReport? = null
  override val bugReport: BugReport?
    get() = _bugReport

  override fun loadBugReport(bugreportPath: String, bugReportText: String, progressReporter: ProgressReporter) {
    try {
      _bugReport = bugReportParser.parseBugReport(bugreportPath, bugReportText, progressReporter)
    } catch (e: Exception) {
      throw OpenBugReportException(e.message, e)
    }
  }

  override fun closeBugReport() {
    _bugReport = null
  }

}