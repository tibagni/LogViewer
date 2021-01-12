package com.tibagni.logviewer.bugreport

import com.tibagni.logviewer.AsyncPresenter
import com.tibagni.logviewer.ProgressReporter
import java.io.File

interface BugReportPresenter {
  fun loadBugReport(file: File)
  fun finishing()
}

class BugReportPresenterImpl(
  private val view: BugReportPresenterView,
  private val bugReportRepository: BugReportRepository
) : AsyncPresenter(view),
  BugReportPresenter {
  override fun loadBugReport(file: File) {
    doAsync {
      try {
        bugReportRepository.openBugReport(file, ProgressReporter { progress, note ->
          updateAsyncProgress(progress, note)
        })
        doOnUiThread {
          bugReportRepository.bugReport?.let { view.showBugReport(it) }
        }
      } catch (e: OpenBugReportException) {
        doOnUiThread{view.showErrorMessage(e.message)}
      }
    }
  }

  override fun finishing() {
    // We only care about releasing the thread pool here
    release()
  }
}