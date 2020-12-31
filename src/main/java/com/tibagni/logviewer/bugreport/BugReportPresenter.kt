package com.tibagni.logviewer.bugreport

import com.tibagni.logviewer.ProgressReporter
import java.io.File

interface BugReportPresenter {
  fun loadBugReport(file: File)
}

class BugReportPresenterImpl(private val view: BugReportView, private val bugReportRepository: BugReportRepository) :
  BugReportPresenter {
  override fun loadBugReport(file: File) {
    // TODO do it async using async presenter and async view
    bugReportRepository.openBugReport(file, ProgressReporter { _, _ -> })
    bugReportRepository.bugReport?.let { view.showBugReport(it) }
  }
}