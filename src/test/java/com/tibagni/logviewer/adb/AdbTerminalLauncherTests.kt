package com.tibagni.logviewer.adb

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
  }

  @Test
  fun testLaunchTerminalFormatsCommand() {
    val launchedCommands = mutableListOf<List<String>>()
    AdbTerminalLauncher.processLauncher = { cmd ->
      launchedCommands.add(cmd)
      // Return a dummy mock process
      ProcessBuilder("echo").start()
    }

    val adb = File("/dummy/adb")
    val serial = "TEST_SERIAL_123"

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
