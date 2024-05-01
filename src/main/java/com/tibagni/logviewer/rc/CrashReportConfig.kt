package com.tibagni.logviewer.rc

class CrashReportConfig(configValue: String) : Config<Boolean> {
  private val state: Boolean
  init {
    state = configValue.lowercase() == "on"
  }

  override fun getConfigValue() = state
}