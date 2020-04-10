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

        assertNotNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.UI_SCALE))
        assertNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.LOG_LEVEL))
    }

    @Test
    fun testLogLevelConfig() {
        testRcConfig.parseConfig("loglevel=v")

        assertNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.UI_SCALE))
        assertNotNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.LOG_LEVEL))
    }

    @Test
    fun testAllConfig() {
        testRcConfig.parseConfig("loglevel=verbose")
        testRcConfig.parseConfig("uiscale=auto")

        assertNotNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.UI_SCALE))
        assertNotNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.LOG_LEVEL))
    }

    @Test
    fun testNoConfig() {
        assertNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.UI_SCALE))
        assertNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.LOG_LEVEL))
    }

    @Test
    fun testOnlyInvalidConfig() {
        testRcConfig.parseConfig("invalidkey=dddddd")
        testRcConfig.parseConfig("nonexistentconfig=eeee")

        assertNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.UI_SCALE))
        assertNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.LOG_LEVEL))
    }

    @Test
    fun testInvalidConfigFormat() {
        testRcConfig.parseConfig("invalidkey")
        testRcConfig.parseConfig("=auto")
        testRcConfig.parseConfig("")
        testRcConfig.parseConfig(" ")

        assertNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.UI_SCALE))
        assertNull(RuntimeConfiguration.getConfig(RuntimeConfiguration.LOG_LEVEL))
    }
}