package com.tibagni.logviewer

import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.util.PropertiesWrapper
import java.io.IOException
import java.lang.NumberFormatException

object AppInfo {
  const val APPLICATION_NAME = "Log Viewer"
  const val LATEST_RELEASE_URL = "https://api.github.com/repos/tibagni/LogViewer/releases/latest"
  const val USER_GUIDE_URL = "https://tibagni.github.io/LogViewer/"
  const val GITHUB_URL = "https://github.com/tibagni/LogViewer"

  private const val APP_PROPERTIES_FILE = "properties/app.properties"
  private const val VERSION_KEY = "version"

  private lateinit var cachedVersionStr: String

  val currentVersion: String
    get() {
      if (this::cachedVersionStr.isInitialized) {
        return cachedVersionStr
      }
      var currentVersion = "unknown"
      try {
        val appProperties = PropertiesWrapper(APP_PROPERTIES_FILE)
        currentVersion = appProperties[VERSION_KEY]
        cachedVersionStr = currentVersion
      } catch (e: IOException) {
        Logger.error("Failed to get current version", e)
      }
      return currentVersion
    }

  val currentVersionNumber: Double
    get() {
      val versionName = currentVersion
      return try {
        versionName.toDouble()
      } catch (nfe: NumberFormatException) {
        Logger.error(
          "Not possible to parse current version: "
              + versionName, nfe
        )
        (-1).toDouble()
      }
    }
}