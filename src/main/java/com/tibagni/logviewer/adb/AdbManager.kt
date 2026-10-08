package com.tibagni.logviewer.adb

import com.tibagni.logviewer.logger.Logger
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
  private val terminalLauncher: (File, String) -> Boolean = AdbTerminalLauncher::launchTerminal
) : AdbManager {

  private var customAdbPath: File? = null

  override fun setCustomAdbPath(path: File?) {
    customAdbPath = path
  }

  override val isAdbAvailable: Boolean
    get() = getAdbExecutable() != null

  override fun getAdbExecutable(): File? {
    val custom = customAdbPath
    if (custom != null && fileChecker(custom)) {
      return custom
    }

    val isWindows = PlatformUtils.isWindows
    val adbBinaryName = if (isWindows) "adb.exe" else "adb"

    // 1. Check ANDROID_HOME or ANDROID_SDK_ROOT
    val androidHome = PlatformUtils.getEnv("ANDROID_HOME") ?: PlatformUtils.getEnv("ANDROID_SDK_ROOT")
    if (!androidHome.isNullOrBlank()) {
      val candidate = File(androidHome, "platform-tools/$adbBinaryName")
      if (fileChecker(candidate)) return candidate
    }

    // 2. Check PATH environment variable
    val pathEnv = PlatformUtils.getEnv("PATH")
    if (!pathEnv.isNullOrBlank()) {
      val paths = pathEnv.split(File.pathSeparatorChar)
      for (pathDir in paths) {
        val candidate = File(pathDir, adbBinaryName)
        if (fileChecker(candidate)) return candidate
      }
    }

    // 3. Check well-known default locations
    val userHome = PlatformUtils.userHome
    val defaultLocations = when {
      isWindows -> listOf(
        "${PlatformUtils.getEnv("LOCALAPPDATA")}\\Android\\Sdk\\platform-tools\\$adbBinaryName",
        "${PlatformUtils.getEnv("PROGRAMFILES")}\\Android\\android-sdk\\platform-tools\\$adbBinaryName"
      )
      PlatformUtils.isMac -> listOf(
        "$userHome/Library/Android/sdk/platform-tools/$adbBinaryName",
        "/usr/local/bin/$adbBinaryName",
        "/opt/homebrew/bin/$adbBinaryName"
      )
      else -> listOf(
        "$userHome/Android/Sdk/platform-tools/$adbBinaryName",
        "/usr/bin/$adbBinaryName",
        "/usr/local/bin/$adbBinaryName"
      )
    }

    for (location in defaultLocations) {
      val candidate = File(location)
      if (fileChecker(candidate)) return candidate
    }

    return null
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
    return terminalLauncher(adb, device.serialNumber)
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
