package com.tibagni.logviewer.adb

import com.tibagni.logviewer.util.PlatformUtils
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException

class AdbManagerTests {

  @After
  fun tearDown() {
    PlatformUtils.reset()
  }

  @Test
  fun testDeviceStateParsing() {
    assertEquals(DeviceState.ONLINE, DeviceState.fromString("device"))
    assertEquals(DeviceState.ONLINE, DeviceState.fromString("  DEVICE  "))
    assertEquals(DeviceState.OFFLINE, DeviceState.fromString("offline"))
    assertEquals(DeviceState.UNAUTHORIZED, DeviceState.fromString("unauthorized"))
    assertEquals(DeviceState.BOOTLOADER, DeviceState.fromString("bootloader"))
    assertEquals(DeviceState.RECOVERY, DeviceState.fromString("recovery"))
    assertEquals(DeviceState.SIDELOAD, DeviceState.fromString("sideload"))
    assertEquals(DeviceState.UNKNOWN, DeviceState.fromString("something_else"))
  }

  @Test
  fun testAdbDeviceDisplayName() {
    val onlinePixel = AdbDevice(
      serialNumber = "28241FDH2002DF",
      state = DeviceState.ONLINE,
      model = "Pixel_8_Pro",
      product = "husky",
      device = "husky"
    )
    assertTrue(onlinePixel.isOnline)
    assertEquals("Pixel 8 Pro (28241FDH2002DF)", onlinePixel.displayName)
    assertEquals("Pixel 8 Pro (28241FDH2002DF)", onlinePixel.toString())

    val unauthorizedDevice = AdbDevice(
      serialNumber = "ZY2243WXYZ",
      state = DeviceState.UNAUTHORIZED,
      model = "moto_g_stylus",
      product = "milan"
    )
    assertFalse(unauthorizedDevice.isOnline)
    assertEquals("moto g stylus (ZY2243WXYZ) [unauthorized]", unauthorizedDevice.displayName)

    val fallbackProduct = AdbDevice(
      serialNumber = "12345",
      state = DeviceState.ONLINE,
      product = "sunfish"
    )
    assertEquals("sunfish (12345)", fallbackProduct.displayName)

    val fallbackSerialOnly = AdbDevice(
      serialNumber = "emulator-5554",
      state = DeviceState.ONLINE
    )
    assertEquals("emulator-5554", fallbackSerialOnly.displayName)
  }

  @Test
  fun testParseDevicesOutputEmpty() {
    val emptyOutput = ""
    assertTrue(AdbManagerImpl.parseDevicesOutput(emptyOutput).isEmpty())

    val headerOnlyOutput = """
      List of devices attached
      
    """.trimIndent()
    assertTrue(AdbManagerImpl.parseDevicesOutput(headerOnlyOutput).isEmpty())
  }

  @Test
  fun testParseDevicesOutputWithMultipleDevicesAndDaemonOutput() {
    val output = """
      * daemon not running; starting now at tcp:5037 *
      * daemon started successfully *
      List of devices attached
      emulator-5554          device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64 device:emu64a transport_id:1
      28241FDH2002DF         device usb:3-1 product:husky model:Pixel_8_Pro device:husky transport_id:2
      ZY2243WXYZ             unauthorized usb:1-4 transport_id:3
      192.168.1.100:5555     offline transport_id:4
    """.trimIndent()

    val devices = AdbManagerImpl.parseDevicesOutput(output)
    assertEquals(4, devices.size)

    assertEquals("emulator-5554", devices[0].serialNumber)
    assertEquals(DeviceState.ONLINE, devices[0].state)
    assertEquals("sdk_gphone64_arm64", devices[0].model)
    assertEquals("sdk_gphone64_arm64", devices[0].product)
    assertEquals("emu64a", devices[0].device)
    assertEquals("1", devices[0].transportId)

    assertEquals("28241FDH2002DF", devices[1].serialNumber)
    assertEquals(DeviceState.ONLINE, devices[1].state)
    assertEquals("Pixel_8_Pro", devices[1].model)
    assertEquals("husky", devices[1].product)
    assertEquals("husky", devices[1].device)
    assertEquals("2", devices[1].transportId)

    assertEquals("ZY2243WXYZ", devices[2].serialNumber)
    assertEquals(DeviceState.UNAUTHORIZED, devices[2].state)
    assertNull(devices[2].model)
    assertEquals("3", devices[2].transportId)

    assertEquals("192.168.1.100:5555", devices[3].serialNumber)
    assertEquals(DeviceState.OFFLINE, devices[3].state)
    assertEquals("4", devices[3].transportId)
  }

  @Test
  fun testFindAdbWithCustomPath() {
    val customAdb = File("/custom/bin/adb")
    val manager = AdbManagerImpl(
      fileChecker = { it == customAdb }
    )

    manager.setCustomAdbPath(customAdb)
    assertEquals(customAdb, manager.getAdbExecutable())
    assertTrue(manager.isAdbAvailable)

    // When custom path does not exist, it falls back
    manager.setCustomAdbPath(File("/non/existent/adb"))
    assertNull(manager.getAdbExecutable())
    assertFalse(manager.isAdbAvailable)
  }

  @Test
  fun testFindAdbViaAndroidHome() {
    val androidHome = "/sdk/android"
    val expectedAdb = File("/sdk/android/platform-tools/adb")

    PlatformUtils.envProvider = { if (it == "ANDROID_HOME") androidHome else null }
    val manager = AdbManagerImpl(
      fileChecker = { it == expectedAdb }
    )

    assertEquals(expectedAdb, manager.getAdbExecutable())
    assertTrue(manager.isAdbAvailable)
  }

  @Test
  fun testFindAdbViaPathEnv() {
    val expectedAdb = File("/usr/local/bin/adb")
    val pathEnv = "/usr/bin${File.pathSeparator}/usr/local/bin${File.pathSeparator}/bin"

    PlatformUtils.envProvider = { if (it == "PATH") pathEnv else null }
    val manager = AdbManagerImpl(
      fileChecker = { it == expectedAdb }
    )

    assertEquals(expectedAdb, manager.getAdbExecutable())
    assertTrue(manager.isAdbAvailable)
  }

  @Test
  fun testFindAdbWindowsPlatformDefaults() {
    val localAppData = "C:\\Users\\TestUser\\AppData\\Local"
    val expectedAdb = File("$localAppData\\Android\\Sdk\\platform-tools\\adb.exe")

    PlatformUtils.propertyProvider = { if (it == "os.name") "Windows 11" else null }
    PlatformUtils.envProvider = { if (it == "LOCALAPPDATA") localAppData else null }
    val manager = AdbManagerImpl(
      fileChecker = { it == expectedAdb }
    )

    assertEquals(expectedAdb, manager.getAdbExecutable())
    assertTrue(manager.isAdbAvailable)
  }

  @Test
  fun testFindAdbNotFound() {
    PlatformUtils.propertyProvider = { null }
    PlatformUtils.envProvider = { null }
    val manager = AdbManagerImpl(
      fileChecker = { false }
    )

    assertNull(manager.getAdbExecutable())
    assertFalse(manager.isAdbAvailable)
  }

  @Test
  fun testGetConnectedDevices() = runBlocking {
    val dummyAdb = File("/mock/bin/adb")
    val mockRunner = object : CommandRunner {
      override fun exec(command: List<String>): CommandResult {
        assertEquals(listOf("/mock/bin/adb", "devices", "-l"), command)
        return CommandResult(
          exitCode = 0,
          output = """
            List of devices attached
            SERIAL123    device model:Pixel_7
          """.trimIndent(),
          error = ""
        )
      }

      override fun startProcess(command: List<String>): Process {
        throw UnsupportedOperationException()
      }
    }

    val manager = AdbManagerImpl(
      commandRunner = mockRunner,
      fileChecker = { it == dummyAdb }
    )
    manager.setCustomAdbPath(dummyAdb)

    val devices = manager.getConnectedDevices()
    assertEquals(1, devices.size)
    assertEquals("SERIAL123", devices[0].serialNumber)
    assertEquals("Pixel_7", devices[0].model)
  }

  @Test
  fun testGetConnectedDevicesFailureReturnsEmptyList() = runBlocking {
    val dummyAdb = File("/mock/bin/adb")
    val mockRunner = object : CommandRunner {
      override fun exec(command: List<String>): CommandResult {
        return CommandResult(exitCode = 1, output = "", error = "error running adb")
      }

      override fun startProcess(command: List<String>): Process = throw UnsupportedOperationException()
    }

    val manager = AdbManagerImpl(
      commandRunner = mockRunner,
      fileChecker = { it == dummyAdb }
    )
    manager.setCustomAdbPath(dummyAdb)

    val devices = manager.getConnectedDevices()
    assertTrue(devices.isEmpty())
  }

  @Test
  fun testGetConnectedDevicesExceptionReturnsEmptyList() = runBlocking {
    val dummyAdb = File("/mock/bin/adb")
    val mockRunner = object : CommandRunner {
      override fun exec(command: List<String>): CommandResult {
        throw IOException("Process could not start")
      }

      override fun startProcess(command: List<String>): Process = throw UnsupportedOperationException()
    }

    val manager = AdbManagerImpl(
      commandRunner = mockRunner,
      fileChecker = { it == dummyAdb }
    )
    manager.setCustomAdbPath(dummyAdb)

    val devices = manager.getConnectedDevices()
    assertTrue(devices.isEmpty())
  }

  @Test
  fun testRestartServer() = runBlocking {
    val dummyAdb = File("/mock/bin/adb")
    val commandsRun = mutableListOf<List<String>>()
    val mockRunner = object : CommandRunner {
      override fun exec(command: List<String>): CommandResult {
        commandsRun.add(command)
        return CommandResult(exitCode = 0, output = "", error = "")
      }

      override fun startProcess(command: List<String>): Process = throw UnsupportedOperationException()
    }

    val manager = AdbManagerImpl(
      commandRunner = mockRunner,
      fileChecker = { it == dummyAdb }
    )
    manager.setCustomAdbPath(dummyAdb)

    val result = manager.restartServer()
    assertTrue(result)
    assertEquals(2, commandsRun.size)
    assertEquals(listOf("/mock/bin/adb", "kill-server"), commandsRun[0])
    assertEquals(listOf("/mock/bin/adb", "start-server"), commandsRun[1])
  }

  @Test
  fun testLaunchDeviceTerminalSuccess() {
    val dummyAdb = File("/mock/bin/adb")
    val testDevice = AdbDevice("device123", DeviceState.ONLINE, "husky", "Pixel 8 Pro", "husky", "1")
    var passedAdb: File? = null
    var passedSerial: String? = null

    val mockLauncher = object : AdbTerminalLauncher {
      override fun launchTerminal(adbExecutable: File, serialNumber: String): Boolean {
        passedAdb = adbExecutable
        passedSerial = serialNumber
        return true
      }
    }

    val manager = AdbManagerImpl(
      fileChecker = { it == dummyAdb },
      terminalLauncher = mockLauncher
    )
    manager.setCustomAdbPath(dummyAdb)

    val launched = manager.launchDeviceTerminal(testDevice)
    assertTrue(launched)
    assertEquals(dummyAdb, passedAdb)
    assertEquals("device123", passedSerial)
  }

  @Test
  fun testLaunchDeviceTerminalWhenAdbNotAvailable() {
    val testDevice = AdbDevice("device123", DeviceState.ONLINE, "husky", "Pixel 8 Pro", "husky", "1")
    var launcherCalled = false

    val mockLauncher = object : AdbTerminalLauncher {
      override fun launchTerminal(adbExecutable: File, serialNumber: String): Boolean {
        launcherCalled = true
        return true
      }
    }

    val manager = AdbManagerImpl(
      fileChecker = { false },
      terminalLauncher = mockLauncher
    )

    val launched = manager.launchDeviceTerminal(testDevice)
    assertFalse(launched)
    assertFalse(launcherCalled)
  }
}
