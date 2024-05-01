package com.tibagni.logviewer.rc

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class RuntimeConfigurationTests {
    lateinit var testRcConfig: RuntimeConfiguration

    @Before
    fun setUp() {
        testRcConfig = RuntimeConfiguration.initializeForTest()
    }

    @Test
    fun testUiScaleConfig() {
        testRcConfig.parseConfig("uiscale=2")

        assertNotNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.UI_SCALE, UIScaleConfig::class.java))
        assertNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.LOG_LEVEL, LogLevelConfig::class.java))
        assertNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.CRASH_REPORT, CrashReportConfig::class.java))
    }

    @Test
    fun testLogLevelConfig() {
        testRcConfig.parseConfig("loglevel=v")

        assertNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.UI_SCALE, UIScaleConfig::class.java))
        assertNotNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.LOG_LEVEL, LogLevelConfig::class.java))
        assertNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.CRASH_REPORT, CrashReportConfig::class.java))
    }

    @Test
    fun testCrashReportConfig() {
        testRcConfig.parseConfig("crashreport=on")

        assertNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.UI_SCALE, UIScaleConfig::class.java))
        assertNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.LOG_LEVEL, LogLevelConfig::class.java))
        assertNotNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.CRASH_REPORT, CrashReportConfig::class.java))
    }

    @Test
    fun testAllConfig() {
        testRcConfig.parseConfig("loglevel=verbose")
        testRcConfig.parseConfig("uiscale=5")
        testRcConfig.parseConfig("crashreport=ON")

        assertNotNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.UI_SCALE, UIScaleConfig::class.java))
        assertNotNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.LOG_LEVEL, LogLevelConfig::class.java))
        assertNotNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.CRASH_REPORT, CrashReportConfig::class.java))
    }

    @Test
    fun testNoConfig() {
        assertNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.UI_SCALE, UIScaleConfig::class.java))
        assertNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.LOG_LEVEL, LogLevelConfig::class.java))
        assertNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.CRASH_REPORT, CrashReportConfig::class.java))
    }

    @Test
    fun testOnlyInvalidConfig() {
        testRcConfig.parseConfig("invalidkey=dddddd")
        testRcConfig.parseConfig("nonexistentconfig=eeee")

        assertNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.UI_SCALE, UIScaleConfig::class.java))
        assertNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.LOG_LEVEL, LogLevelConfig::class.java))
        assertNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.CRASH_REPORT, CrashReportConfig::class.java))
    }

    @Test
    fun testInvalidConfigFormat() {
        testRcConfig.parseConfig("invalidkey")
        testRcConfig.parseConfig("=4")
        testRcConfig.parseConfig("")
        testRcConfig.parseConfig(" ")

        assertNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.UI_SCALE, UIScaleConfig::class.java))
        assertNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.LOG_LEVEL, LogLevelConfig::class.java))
        assertNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.CRASH_REPORT, CrashReportConfig::class.java))
    }
}