package com.tibagni.logviewer.theme

class LogViewerTheme(val name: String, private val installer: () -> Unit) {
  fun install() {
    installer()
  }

  override fun toString() = name
}