package com.tibagni.logviewer.adb

import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.util.PlatformUtils
import java.io.File
import java.io.IOException

internal object AdbTerminalLauncher {

  var processLauncher: (List<String>) -> Process = { cmd -> ProcessBuilder(cmd).start() }

  fun launchTerminal(adbExecutable: File, serialNumber: String): Boolean {
    return when {
      PlatformUtils.isWindows -> launchWindows(adbExecutable, serialNumber)
      PlatformUtils.isMac -> launchMac(adbExecutable, serialNumber)
      else -> launchLinux(adbExecutable, serialNumber)
    }
  }

  private fun launchWindows(adb: File, serial: String): Boolean {
    return try {
      val cmd = listOf("cmd.exe", "/c", "start", "cmd.exe", "/k", "\"${adb.absolutePath}\" -s $serial shell")
      processLauncher(cmd)
      true
    } catch (e: Exception) {
      Logger.error("Failed to launch Windows terminal for adb shell", e)
      false
    }
  }

  private fun launchMac(adb: File, serial: String): Boolean {
    return try {
      val script = "tell application \"Terminal\" to do script \"\\\"${adb.absolutePath}\\\" -s $serial shell\""
      processLauncher(listOf("osascript", "-e", script))
      processLauncher(listOf("osascript", "-e", "tell application \"Terminal\" to activate"))
      true
    } catch (e: Exception) {
      Logger.error("Failed to launch macOS Terminal for adb shell", e)
      false
    }
  }

  private fun launchLinux(adb: File, serial: String): Boolean {
    val shellCommand = "\"${adb.absolutePath}\" -s $serial shell"
    val terminalCandidates = listOf(
      listOf("x-terminal-emulator", "-e", shellCommand),
      listOf("gnome-terminal", "--", adb.absolutePath, "-s", serial, "shell"),
      listOf("konsole", "-e", adb.absolutePath, "-s", serial, "shell"),
      listOf("xfce4-terminal", "-e", shellCommand),
      listOf("kitty", adb.absolutePath, "-s", serial, "shell"),
      listOf("alacritty", "-e", adb.absolutePath, "-s", serial, "shell"),
      listOf("xterm", "-e", shellCommand)
    )

    for (cmd in terminalCandidates) {
      try {
        processLauncher(cmd)
        return true
      } catch (ignored: IOException) {
        // Continue trying next candidate
      }
    }

    Logger.error("Could not find a supported terminal emulator on Linux")
    return false
  }
}
