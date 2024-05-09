package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.bugreport.section.SystemPropertiesSection
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SystemPropertySectionParserTests {
  private lateinit var systemPropertiesSectionParser: SystemPropertiesSectionParser

  companion object {
    const val SYSTEM_PROPERTIES = "\n------ SYSTEM PROPERTIES (getprop) ------\n" +
        "[Build.STRINGPROP]: [anyString]\n" +
        "[ro.integer.property]: [2]\n" +
        "[ro.test]: [True]\n" +
        "[ro.carrier]: [carrier1]\n" +
        "[persist.test.something]: [ok]\n" +
        "------ 0.036s was the duration of 'SYSTEM PROPERTIES' ------\n"
  }

  @Before
  fun setUp() {
    systemPropertiesSectionParser = SystemPropertiesSectionParser()
  }

  @Test
  fun testParseProperties() {
    val section = systemPropertiesSectionParser.parse("", SYSTEM_PROPERTIES) as SystemPropertiesSection

    Assert.assertTrue(section.configs.containsKey("[Build.STRINGPROP]"))
    Assert.assertEquals("[anyString]", section.configs["[Build.STRINGPROP]"])

    Assert.assertTrue(section.configs.containsKey("[ro.integer.property]"))
    Assert.assertEquals("[2]", section.configs["[ro.integer.property]"])

    Assert.assertTrue(section.configs.containsKey("[ro.test]"))
    Assert.assertEquals("[True]", section.configs["[ro.test]"])

    Assert.assertTrue(section.configs.containsKey("[ro.carrier]"))
    Assert.assertEquals("[carrier1]", section.configs["[ro.carrier]"])

    Assert.assertTrue(section.configs.containsKey("[persist.test.something]"))
    Assert.assertEquals("[ok]", section.configs["[persist.test.something]"])

    Assert.assertFalse(section.configs.containsKey("ro.prop.nonexistent"))
  }
}