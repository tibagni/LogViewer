package com.tibagni.logviewer.adb

import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.util.PlatformUtils
import java.io.File
import java.io.IOException

interface AdbTerminalLauncher {
  fun launchTerminal(adbExecutable: File, serialNumber: String): Boolean

  companion object {
    fun create(): AdbTerminalLauncher = when {
      PlatformUtils.isWindows -> WindowsAdbTerminalLauncher()
      PlatformUtils.isMac -> MacAdbTerminalLauncher()
      else -> LinuxAdbTerminalLauncher()
    }
  }
}

class WindowsAdbTerminalLauncher(
  private val processLauncher: (List<String>) -> Process = { cmd -> ProcessBuilder(cmd).start() }
) : AdbTerminalLauncher {
  override fun launchTerminal(adbExecutable: File, serialNumber: String): Boolean {
    return try {
      val cmd = listOf("cmd.exe", "/c", "start", "cmd.exe", "/k", "\"${adbExecutable.absolutePath}\" -s $serialNumber shell")
      processLauncher(cmd)
      true
    } catch (e: Exception) {
      Logger.error("Failed to launch Windows terminal for adb shell", e)
      false
    }
  }
}

class MacAdbTerminalLauncher(
  private val processLauncher: (List<String>) -> Process = { cmd -> ProcessBuilder(cmd).start() }
) : AdbTerminalLauncher {
  override fun launchTerminal(adbExecutable: File, serialNumber: String): Boolean {
    return try {
      val script = "tell application \"Terminal\" to do script \"\\\"${adbExecutable.absolutePath}\\\" -s $serialNumber shell\""
      processLauncher(listOf("osascript", "-e", script))
      processLauncher(listOf("osascript", "-e", "tell application \"Terminal\" to activate"))
      true
    } catch (e: Exception) {
      Logger.error("Failed to launch macOS Terminal for adb shell", e)
      false
    }
  }
}

class LinuxAdbTerminalLauncher(
  private val processLauncher: (List<String>) -> Process = { cmd -> ProcessBuilder(cmd).start() }
) : AdbTerminalLauncher {
  override fun launchTerminal(adbExecutable: File, serialNumber: String): Boolean {
    val shellCommand = "\"${adbExecutable.absolutePath}\" -s $serialNumber shell"
    val terminalCandidates = listOf(
      listOf("x-terminal-emulator", "-e", shellCommand),
      listOf("gnome-terminal", "--", adbExecutable.absolutePath, "-s", serialNumber, "shell"),
      listOf("konsole", "-e", adbExecutable.absolutePath, "-s", serialNumber, "shell"),
      listOf("xfce4-terminal", "-e", shellCommand),
      listOf("kitty", adbExecutable.absolutePath, "-s", serialNumber, "shell"),
      listOf("alacritty", "-e", adbExecutable.absolutePath, "-s", serialNumber, "shell"),
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
