package com.tibagni.logviewer.util

import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class PlatformUtilsTests {

  @After
  fun tearDown() {
    PlatformUtils.reset()
  }

  @Test
  fun testWindowsDetection() {
    PlatformUtils.propertyProvider = { if (it == "os.name") "Windows 11" else null }
    assertTrue(PlatformUtils.isWindows)
    assertFalse(PlatformUtils.isMac)
    assertFalse(PlatformUtils.isLinux)
  }

  @Test
  fun testMacDetection() {
    PlatformUtils.propertyProvider = { if (it == "os.name") "Mac OS X" else null }
    assertFalse(PlatformUtils.isWindows)
    assertTrue(PlatformUtils.isMac)
    assertFalse(PlatformUtils.isLinux)
  }

  @Test
  fun testLinuxDetection() {
    PlatformUtils.propertyProvider = { if (it == "os.name") "Linux" else null }
    assertFalse(PlatformUtils.isWindows)
    assertFalse(PlatformUtils.isMac)
    assertTrue(PlatformUtils.isLinux)
  }

  @Test
  fun testUserHome() {
    PlatformUtils.propertyProvider = { if (it == "user.home") "/custom/home" else null }
    assertEquals("/custom/home", PlatformUtils.userHome)
  }

  @Test
  fun testUserHomeDefaultWhenNull() {
    PlatformUtils.propertyProvider = { null }
    assertEquals("", PlatformUtils.userHome)
  }

  @Test
  fun testLineSeparator() {
    PlatformUtils.propertyProvider = { if (it == "line.separator") "\r\n" else null }
    assertEquals("\r\n", PlatformUtils.lineSeparator)
  }

  @Test
  fun testGetEnv() {
    PlatformUtils.envProvider = { if (it == "TEST_VAR") "test_val" else null }
    assertEquals("test_val", PlatformUtils.getEnv("TEST_VAR"))
    assertNull(PlatformUtils.getEnv("NON_EXISTENT"))
  }

  @Test
  fun testGetSystemProperty() {
    PlatformUtils.propertyProvider = { if (it == "some.prop") "prop_val" else null }
    assertEquals("prop_val", PlatformUtils.getSystemProperty("some.prop"))
    assertNull(PlatformUtils.getSystemProperty("non.existent"))
  }

  @Test
  fun testResetRestoresDefaults() {
    PlatformUtils.propertyProvider = { "overridden" }
    PlatformUtils.envProvider = { "overridden_env" }

    PlatformUtils.reset()

    // After reset, propertyProvider should match real System.getProperty
    assertEquals(System.getProperty("os.name"), PlatformUtils.propertyProvider("os.name"))
    assertEquals(System.getenv("PATH"), PlatformUtils.envProvider("PATH"))
  }
}
