package com.tibagni.logviewer.adb

import org.junit.Assert.*
import org.junit.Test
import java.awt.GraphicsEnvironment
import java.io.File

class AdbDevicesDialogTests {

  private val mockRunner = object : CommandRunner {
    override fun exec(command: List<String>): CommandResult {
      return if (command.contains("devices")) {
        CommandResult(
          0,
          "List of devices attached\n" +
              "device123             device product:husky model:Pixel_8_Pro device:husky transport_id:1\n" +
              "device456             unauthorized transport_id:2\n",
          ""
        )
      } else {
        CommandResult(0, "", "")
      }
    }

    override fun startProcess(command: List<String>): Process {
      return ProcessBuilder("echo").start()
    }
  }

  @Test
  fun testDialogInstantiation() {
    if (GraphicsEnvironment.isHeadless()) {
      return
    }

    val adbManager = AdbManagerImpl(
      commandRunner = mockRunner,
      fileChecker = { true }
    )
    adbManager.setCustomAdbPath(File("/fake/adb"))
    val devicesDialog = AdbDevicesDialog(null, adbManager)
    assertNotNull(devicesDialog)
    devicesDialog.dispose()
  }

  @Test
  fun testAdbDeviceDisplayName() {
    val deviceWithModel = AdbDevice("serial1", DeviceState.ONLINE, "husky", "Pixel 8 Pro", "husky", "1")
    assertEquals("Pixel 8 Pro (serial1)", deviceWithModel.displayName)

    val deviceWithoutModel = AdbDevice("serial2", DeviceState.UNAUTHORIZED, null, null, null, null)
    assertEquals("serial2 [unauthorized]", deviceWithoutModel.displayName)
  }
}
