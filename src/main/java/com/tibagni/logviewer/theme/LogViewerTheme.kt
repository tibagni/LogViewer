package com.tibagni.logviewer.theme

class LogViewerTheme(val name: String, val isDark: Boolean, private val installer: () -> Unit) {
  fun install() {
    installer()
  }

  override fun toString() = name
}