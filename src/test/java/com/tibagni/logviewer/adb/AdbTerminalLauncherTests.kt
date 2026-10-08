package com.tibagni.logviewer.adb

import com.tibagni.logviewer.util.PlatformUtils
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException

class AdbTerminalLauncherTests {

  private var originalLauncher: ((List<String>) -> Process)? = null

  @Before
  fun setUp() {
    originalLauncher = AdbTerminalLauncher.processLauncher
  }

  @After
  fun tearDown() {
    originalLauncher?.let { AdbTerminalLauncher.processLauncher = it }
    PlatformUtils.reset()
  }

  @Test
  fun testLaunchTerminalWindows() {
    PlatformUtils.propertyProvider = { if (it == "os.name") "Windows 11" else null }
    val launchedCommands = mutableListOf<List<String>>()
    AdbTerminalLauncher.processLauncher = { cmd ->
      launchedCommands.add(cmd)
      ProcessBuilder("echo").start()
    }

    val adb = File("C:\\dummy\\adb.exe")
    val serial = "TEST_SERIAL_WIN"

    val result = AdbTerminalLauncher.launchTerminal(adb, serial)
    assertTrue(result)
    assertEquals(1, launchedCommands.size)
    assertEquals("cmd.exe", launchedCommands[0][0])
    assertTrue(launchedCommands[0].contains("\"${adb.absolutePath}\" -s $serial shell"))
  }

  @Test
  fun testLaunchTerminalMac() {
    PlatformUtils.propertyProvider = { if (it == "os.name") "Mac OS X" else null }
    val launchedCommands = mutableListOf<List<String>>()
    AdbTerminalLauncher.processLauncher = { cmd ->
      launchedCommands.add(cmd)
      ProcessBuilder("echo").start()
    }

    val adb = File("/dummy/adb")
    val serial = "TEST_SERIAL_MAC"

    val result = AdbTerminalLauncher.launchTerminal(adb, serial)
    assertTrue(result)
    assertEquals(2, launchedCommands.size)
    assertEquals("osascript", launchedCommands[0][0])
  }

  @Test
  fun testLaunchTerminalLinux() {
    PlatformUtils.propertyProvider = { if (it == "os.name") "Linux" else null }
    val launchedCommands = mutableListOf<List<String>>()
    AdbTerminalLauncher.processLauncher = { cmd ->
      launchedCommands.add(cmd)
      ProcessBuilder("echo").start()
    }

    val adb = File("/dummy/adb")
    val serial = "TEST_SERIAL_LINUX"

    val result = AdbTerminalLauncher.launchTerminal(adb, serial)
    assertTrue(result)
    assertTrue(launchedCommands.isNotEmpty())
    val cmdStr = launchedCommands.first().joinToString(" ")
    assertTrue(cmdStr.contains(serial))
  }

  @Test
  fun testLaunchTerminalHandlesAllCandidateFailures() {
    AdbTerminalLauncher.processLauncher = {
      throw IOException("Simulated terminal launch failure")
    }

    val adb = File("/dummy/adb")
    val serial = "TEST_SERIAL_123"

    val result = AdbTerminalLauncher.launchTerminal(adb, serial)
    assertFalse(result)
  }
}
