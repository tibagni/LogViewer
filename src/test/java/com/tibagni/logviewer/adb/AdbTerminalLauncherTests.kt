package com.tibagni.logviewer.adb

import com.tibagni.logviewer.util.PlatformUtils
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class AdbTerminalLauncherTests {

  private class FakeProcess : Process() {
    override fun getOutputStream(): OutputStream = ByteArrayOutputStream()
    override fun getInputStream(): InputStream = ByteArrayInputStream(ByteArray(0))
    override fun getErrorStream(): InputStream = ByteArrayInputStream(ByteArray(0))
    override fun waitFor(): Int = 0
    override fun exitValue(): Int = 0
    override fun destroy() {}
  }

  @After
  fun tearDown() {
    PlatformUtils.reset()
  }

  @Test
  fun testFactoryCreatesCorrectLauncherPerPlatform() {
    PlatformUtils.propertyProvider = { if (it == "os.name") "Windows 11" else null }
    assertTrue(AdbTerminalLauncher.create() is WindowsAdbTerminalLauncher)

    PlatformUtils.propertyProvider = { if (it == "os.name") "Mac OS X" else null }
    assertTrue(AdbTerminalLauncher.create() is MacAdbTerminalLauncher)

    PlatformUtils.propertyProvider = { if (it == "os.name") "Linux" else null }
    assertTrue(AdbTerminalLauncher.create() is LinuxAdbTerminalLauncher)
  }

  @Test
  fun testWindowsTerminalLauncher() {
    val launchedCommands = mutableListOf<List<String>>()
    val launcher = WindowsAdbTerminalLauncher(
      processLauncher = { cmd ->
        launchedCommands.add(cmd)
        FakeProcess()
      }
    )

    val adb = File("C:\\dummy\\adb.exe")
    val serial = "TEST_SERIAL_WIN"

    val result = launcher.launchTerminal(adb, serial)
    assertTrue(result)
    assertEquals(1, launchedCommands.size)
    assertEquals("cmd.exe", launchedCommands[0][0])
    assertTrue(launchedCommands[0].contains("\"${adb.absolutePath}\" -s $serial shell"))
  }

  @Test
  fun testWindowsTerminalLauncherHandlesFailure() {
    val launcher = WindowsAdbTerminalLauncher(
      processLauncher = { throw IOException("Failed to start cmd") }
    )

    val adb = File("C:\\dummy\\adb.exe")
    assertFalse(launcher.launchTerminal(adb, "SERIAL1"))
  }

  @Test
  fun testMacTerminalLauncher() {
    val launchedCommands = mutableListOf<List<String>>()
    val launcher = MacAdbTerminalLauncher(
      processLauncher = { cmd ->
        launchedCommands.add(cmd)
        FakeProcess()
      }
    )

    val adb = File("/dummy/adb")
    val serial = "TEST_SERIAL_MAC"

    val result = launcher.launchTerminal(adb, serial)
    assertTrue(result)
    assertEquals(2, launchedCommands.size)
    assertEquals("osascript", launchedCommands[0][0])
  }

  @Test
  fun testMacTerminalLauncherHandlesFailure() {
    val launcher = MacAdbTerminalLauncher(
      processLauncher = { throw IOException("Failed to start osascript") }
    )

    val adb = File("/dummy/adb")
    assertFalse(launcher.launchTerminal(adb, "SERIAL1"))
  }

  @Test
  fun testLinuxTerminalLauncher() {
    val launchedCommands = mutableListOf<List<String>>()
    val launcher = LinuxAdbTerminalLauncher(
      processLauncher = { cmd ->
        launchedCommands.add(cmd)
        FakeProcess()
      }
    )

    val adb = File("/dummy/adb")
    val serial = "TEST_SERIAL_LINUX"

    val result = launcher.launchTerminal(adb, serial)
    assertTrue(result)
    assertEquals(1, launchedCommands.size)
    val cmdStr = launchedCommands.first().joinToString(" ")
    assertTrue(cmdStr.contains(serial))
  }

  @Test
  fun testLinuxTerminalLauncherFallsBackOnCandidateFailure() {
    val launchedCommands = mutableListOf<List<String>>()
    var callCount = 0
    val launcher = LinuxAdbTerminalLauncher(
      processLauncher = { cmd ->
        callCount++
        if (callCount < 3) {
          throw IOException("Binary not found: ${cmd.first()}")
        }
        launchedCommands.add(cmd)
        FakeProcess()
      }
    )

    val adb = File("/dummy/adb")
    val serial = "TEST_SERIAL_LINUX"

    val result = launcher.launchTerminal(adb, serial)
    assertTrue(result)
    assertEquals(3, callCount)
    assertEquals(1, launchedCommands.size)
  }

  @Test
  fun testLinuxTerminalLauncherHandlesAllCandidateFailures() {
    val launcher = LinuxAdbTerminalLauncher(
      processLauncher = { throw IOException("Simulated terminal launch failure") }
    )

    val adb = File("/dummy/adb")
    val serial = "TEST_SERIAL_123"

    val result = launcher.launchTerminal(adb, serial)
    assertFalse(result)
  }
}
