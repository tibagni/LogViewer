package com.tibagni.logviewer.bugreport

import com.tibagni.logviewer.AsyncPresenter

interface BugReportPresenter {
  fun loadBugReport(bugreportPath: String, bugreportText: String)
  fun closeBugReport()
  fun finishing()
}

class BugReportPresenterImpl(
  private val view: BugReportPresenterView,
  private val bugReportRepository: BugReportRepository
) : AsyncPresenter(view),
  BugReportPresenter {
  override fun loadBugReport(bugreportPath: String, bugreportText: String) {
    doAsync {
      try {
        bugReportRepository.loadBugReport(bugreportPath, bugreportText) { progress, note ->
          updateAsyncProgress(progress, note)
        }
        doOnUiThread {
          bugReportRepository.bugReport?.let { view.showBugReport(it) } ?: view.showErrorMessage("Empty bug report!")
        }
      } catch (e: OpenBugReportException) {
        doOnUiThread {
          // Make sure to finish loading when something fails
          view.finishLoading()
          view.showErrorMessage(e.message)
        }
      }
    }
  }

  override fun closeBugReport() {
    bugReportRepository.closeBugReport()
  }

  override fun finishing() {
    // We only care about releasing the thread pool here
    release()
  }
}