package com.tibagni.logviewer.adb

import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.preferences.LogViewerPreferences
import com.tibagni.logviewer.util.PlatformUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

interface AdbManager {
  fun getAdbExecutable(): File?
  val isAdbAvailable: Boolean
  fun setCustomAdbPath(path: File?)

  suspend fun getConnectedDevices(): List<AdbDevice>
  suspend fun restartServer(): Boolean
  fun launchDeviceTerminal(device: AdbDevice): Boolean
}

class AdbManagerImpl(
  private val commandRunner: CommandRunner = DefaultCommandRunner(),
  private val fileChecker: (File) -> Boolean = { it.isFile && (it.canExecute() || PlatformUtils.isWindows) },
  private val terminalLauncher: AdbTerminalLauncher = ServiceLocator.adbTerminalLauncher,
  private val userPrefs: LogViewerPreferences? = ServiceLocator.logViewerPrefs
) : AdbManager {

  private val adbPathLock = Any()
  private var customAdbPath: File? = userPrefs?.adbPath
  private var cachedAdbPath: File? = null

  init {
    userPrefs?.addPreferenceListener(object : LogViewerPreferences.Adapter() {
      override fun onAdbPathChanged() {
        setCustomAdbPath(userPrefs.adbPath)
      }
    })
  }

  override fun setCustomAdbPath(path: File?) {
    synchronized(adbPathLock) {
      customAdbPath = path
      cachedAdbPath = null
    }
  }

  override val isAdbAvailable: Boolean
    get() = getAdbExecutable() != null

  override fun getAdbExecutable(): File? = synchronized(adbPathLock) {
    val cached = cachedAdbPath
    if (cached != null && fileChecker(cached)) {
      return cached
    }

    val resolved = resolveCustomPath()
      ?: resolveFromAndroidSdkHome()
      ?: resolveFromPathEnv()
      ?: resolveFromDefaultLocations()

    cachedAdbPath = resolved
    return resolved
  }

  private val adbBinaryName: String
    get() = if (PlatformUtils.isWindows) "adb.exe" else "adb"

  private fun resolveCustomPath(): File? {
    val custom = customAdbPath
    return if (custom != null && fileChecker(custom)) custom else null
  }

  private fun resolveFromAndroidSdkHome(): File? {
    val androidHome = PlatformUtils.getEnv("ANDROID_HOME") ?: PlatformUtils.getEnv("ANDROID_SDK_ROOT")
    if (androidHome.isNullOrBlank()) return null
    val candidate = File(androidHome, "platform-tools/$adbBinaryName")
    return if (fileChecker(candidate)) candidate else null
  }

  private fun resolveFromPathEnv(): File? {
    val pathEnv = PlatformUtils.getEnv("PATH") ?: return null
    return pathEnv.split(File.pathSeparatorChar)
      .asSequence()
      .map { File(it, adbBinaryName) }
      .firstOrNull { fileChecker(it) }
  }

  private fun resolveFromDefaultLocations(): File? {
    return defaultSearchLocations
      .asSequence()
      .map { File(it) }
      .firstOrNull { fileChecker(it) }
  }

  private val defaultSearchLocations: List<String>
    get() = when {
      PlatformUtils.isWindows -> listOf(
        "${PlatformUtils.getEnv("LOCALAPPDATA")}\\Android\\Sdk\\platform-tools\\$adbBinaryName",
        "${PlatformUtils.getEnv("PROGRAMFILES")}\\Android\\android-sdk\\platform-tools\\$adbBinaryName"
      )
      PlatformUtils.isMac -> listOf(
        "${PlatformUtils.userHome}/Library/Android/sdk/platform-tools/$adbBinaryName",
        "/usr/local/bin/$adbBinaryName",
        "/opt/homebrew/bin/$adbBinaryName"
      )
      else -> listOf(
        "${PlatformUtils.userHome}/Android/Sdk/platform-tools/$adbBinaryName",
        "/usr/bin/$adbBinaryName",
        "/usr/local/bin/$adbBinaryName"
      )
    }

  override suspend fun getConnectedDevices(): List<AdbDevice> = withContext(Dispatchers.IO) {
    val adb = getAdbExecutable() ?: return@withContext emptyList()
    try {
      val result = commandRunner.exec(listOf(adb.absolutePath, "devices", "-l"))
      if (result.isSuccess) {
        parseDevicesOutput(result.output)
      } else {
        Logger.error("Failed to execute adb devices: ${result.error}")
        emptyList()
      }
    } catch (e: Exception) {
      Logger.error("Exception while running adb devices", e)
      emptyList()
    }
  }

  override suspend fun restartServer(): Boolean = withContext(Dispatchers.IO) {
    val adb = getAdbExecutable() ?: return@withContext false
    try {
      commandRunner.exec(listOf(adb.absolutePath, "kill-server"))
      val result = commandRunner.exec(listOf(adb.absolutePath, "start-server"))
      result.isSuccess
    } catch (e: Exception) {
      Logger.error("Failed to restart adb server", e)
      false
    }
  }

  override fun launchDeviceTerminal(device: AdbDevice): Boolean {
    val adb = getAdbExecutable() ?: return false
    return terminalLauncher.launchTerminal(adb, device.serialNumber)
  }

  companion object {
    internal fun parseDevicesOutput(output: String): List<AdbDevice> {
      val devices = mutableListOf<AdbDevice>()
      var headerFound = false

      for (rawLine in output.lines()) {
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("*")) {
          // Skip empty lines and adb daemon status output
          continue
        }

        if (line.startsWith("List of devices attached")) {
          headerFound = true
          continue
        }

        if (!headerFound) continue

        val tokens = line.split("\\s+".toRegex())
        if (tokens.size < 2) continue

        val serial = tokens[0]
        val state = DeviceState.fromString(tokens[1])

        var product: String? = null
        var model: String? = null
        var device: String? = null
        var transportId: String? = null

        for (i in 2 until tokens.size) {
          val token = tokens[i]
          val colonIdx = token.indexOf(':')
          if (colonIdx > 0 && colonIdx < token.length - 1) {
            val key = token.substring(0, colonIdx)
            val value = token.substring(colonIdx + 1)
            when (key) {
              "product" -> product = value
              "model" -> model = value
              "device" -> device = value
              "transport_id" -> transportId = value
            }
          }
        }

        devices.add(AdbDevice(serial, state, product, model, device, transportId))
      }

      return devices
    }
  }
}
