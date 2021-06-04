package com.tibagni.logviewer.updates

import com.tibagni.logviewer.AppInfo
import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.util.SwingUtils
import org.apache.commons.io.IOUtils
import org.json.JSONObject
import java.net.URL
import java.nio.charset.Charset

class UpdateManager(private val listener: UpdateListener) {
  interface UpdateListener {
    fun onUpdateFound(newRelease: ReleaseInfo)
    fun onUpToDate()
    fun onFailedToCheckForUpdate(tr: Throwable)
  }

  fun checkForUpdates() {
    // If we fail to read the current version for some reason, do not proceed
    if (AppInfo.currentVersionNumber < 0) {
      Logger.error("Not a valid current version. Do not check for updates")
      return
    }
    Logger.debug("Start checking for updates...")
    SwingUtils.doAsync(
      { latestReleaseInfo },
      { latest: ReleaseInfo -> notifyIfUpdateAvailable(latest, listener) }
    ) { tr: Throwable ->
      Logger.error("Not possible to get latest release info", tr)
      listener.onFailedToCheckForUpdate(tr)
    }
  }

  private fun notifyIfUpdateAvailable(latest: ReleaseInfo, listener: UpdateListener) {
    val currentVersion = AppInfo.currentVersionNumber
    if (latest.version > currentVersion) {
      Logger.debug("New version available: " + latest.versionName)
      listener.onUpdateFound(latest)
    } else {
      Logger.debug("LogViewer is up to date on version $currentVersion")
      listener.onUpToDate()
    }
  }

  @get:Throws(InvalidReleaseException::class)
  private val latestReleaseInfo: ReleaseInfo
    get() = try {
      val url = URL(AppInfo.LATEST_RELEASE_URL)
      val jsonResult = JSONObject(IOUtils.toString(url, Charset.forName("UTF-8")))
      ReleaseInfo(jsonResult)
    } catch (e: Exception) {
      throw InvalidReleaseException(e)
    }
}