package com.tibagni.logviewer

import java.io.File

interface MainUi {
  fun showOpenMultipleLogsFileChooser(): Array<File>?
  fun showOpenSingleLogFileChooser(): File?
}