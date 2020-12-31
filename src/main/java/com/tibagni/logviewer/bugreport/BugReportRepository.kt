package com.tibagni.logviewer.bugreport

import com.tibagni.logviewer.ProgressReporter
import com.tibagni.logviewer.bugreport.parser.BugReportParser
import java.io.File

class OpenBugReportException(message: String?, cause: Throwable): java.lang.Exception(message, cause)

interface BugReportRepository {
  val bugReport: BugReport?

  @Throws(OpenBugReportException::class)
  fun openBugReport(file: File, progressReporter: ProgressReporter)
}

class BugReportRepositoryImpl(private val bugReportParser: BugReportParser): BugReportRepository {
  private var _bugReport: BugReport? = null
  override val bugReport: BugReport?
    get() = _bugReport

  @Throws(OpenBugReportException::class)
  override fun openBugReport(file: File, progressReporter: ProgressReporter) {
    try {
      _bugReport = bugReportParser.parseBugReport(file, progressReporter)
    } catch (e: Exception) {
      throw OpenBugReportException(e.message, e)
    }
  }

}