package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.bugreport.section.PropertiesSection
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PropertiesSectionParserTests {
  private lateinit var propertiesSectionParser: PropertiesSectionParser

  companion object {
    const val PROPERTIES = "========================================================\n" +
        "== dumpstate: 2020-12-23 18:32:41\n" +
        "========================================================\n" +
        "\n" +
        "Build: blabla-userdebug 11 AAA11.111 57ca8 test-keys\n" +
        "Build fingerprint: 'bla/blabla/bla:11/AAA11.111/57ca8:userdebug/intcfg,test-keys'\n" +
        "Bootloader: MBM-3.0-blablabla\n" +
        "Kernel: Linux version 4.19.136+ (nobody@android-build) (clang version 10.0.7 for Android NDK, GNU ld (binutils-2.27-bd24d23f) 2.27.0.20170315) #1 SMP PREEMPT Tue Dec 8 00:30:28 CST 2020\n" +
        "Uptime: up 0 weeks, 0 days, 22 hours, 11 minutes\n"
  }

  @Before
  fun setUp() {
    propertiesSectionParser = PropertiesSectionParser()
  }

  @Test
  fun testParseProperties() {
    val section = propertiesSectionParser.parse("", PROPERTIES) as PropertiesSection

    assertEquals("blabla-userdebug 11 AAA11.111 57ca8 test-keys", section.build)
    assertEquals("'bla/blabla/bla:11/AAA11.111/57ca8:userdebug/intcfg,test-keys'", section.fingerprint)
    assertEquals("MBM-3.0-blablabla", section.bootloader)
    assertEquals("Linux version 4.19.136+ (nobody@android-build) (clang version 10.0.7 for Android NDK, GNU ld (binutils-2.27-bd24d23f) 2.27.0.20170315) #1 SMP PREEMPT Tue Dec 8 00:30:28 CST 2020", section.kernel)
    assertEquals("up 0 weeks, 0 days, 22 hours, 11 minutes", section.uptime)
  }

  @Test
  fun testParsePropertiesMissingData() {
    val section = propertiesSectionParser.parse("", "INVALID") as PropertiesSection

    assertEquals("Not Found", section.build)
    assertEquals("Not Found", section.fingerprint)
    assertEquals("Not Found", section.bootloader)
    assertEquals("Not Found", section.kernel)
    assertEquals("Not Found", section.uptime)
  }
}