package com.tibagni.logviewer.rc

import org.junit.Test
import org.junit.Assert.assertEquals

class LogLevelConfigTests {

    @Test
    fun testVerboseConfig() {
        val config1 = Pair(LogLevelConfig("v"), "lower case single letter")
        val config2 = Pair(LogLevelConfig("V"), "upper case single letter")
        val config3 = Pair(LogLevelConfig("verbose"), "lower case word")
        val config4 = Pair(LogLevelConfig("VERBOSE"), "upper case word")

        assertEquals(config1.second, LogLevelConfig.Level.VERBOSE, config1.first.configValue)
        assertEquals(config2.second, LogLevelConfig.Level.VERBOSE, config2.first.configValue)
        assertEquals(config3.second, LogLevelConfig.Level.VERBOSE, config3.first.configValue)
        assertEquals(config4.second, LogLevelConfig.Level.VERBOSE, config4.first.configValue)
    }

    @Test
    fun testDebugConfig() {
        val config1 = Pair(LogLevelConfig("d"), "lower case single letter")
        val config2 = Pair(LogLevelConfig("D"), "upper case single letter")
        val config3 = Pair(LogLevelConfig("debug"), "lower case word")
        val config4 = Pair(LogLevelConfig("DEBUG"), "upper case word")

        assertEquals(config1.second, LogLevelConfig.Level.DEBUG, config1.first.configValue)
        assertEquals(config2.second, LogLevelConfig.Level.DEBUG, config2.first.configValue)
        assertEquals(config3.second, LogLevelConfig.Level.DEBUG, config3.first.configValue)
        assertEquals(config4.second, LogLevelConfig.Level.DEBUG, config4.first.configValue)
    }

    @Test
    fun testInfoConfig() {
        val config1 = Pair(LogLevelConfig("i"), "lower case single letter")
        val config2 = Pair(LogLevelConfig("I"), "upper case single letter")
        val config3 = Pair(LogLevelConfig("info"), "lower case word")
        val config4 = Pair(LogLevelConfig("INFO"), "upper case word")

        assertEquals(config1.second, LogLevelConfig.Level.INFO, config1.first.configValue)
        assertEquals(config2.second, LogLevelConfig.Level.INFO, config2.first.configValue)
        assertEquals(config3.second, LogLevelConfig.Level.INFO, config3.first.configValue)
        assertEquals(config4.second, LogLevelConfig.Level.INFO, config4.first.configValue)
    }

    @Test
    fun testWarningConfig() {
        val config1 = Pair(LogLevelConfig("w"), "lower case single letter")
        val config2 = Pair(LogLevelConfig("W"), "upper case single letter")
        val config3 = Pair(LogLevelConfig("warning"), "lower case word")
        val config4 = Pair(LogLevelConfig("WARNING"), "upper case word")

        assertEquals(config1.second, LogLevelConfig.Level.WARNING, config1.first.configValue)
        assertEquals(config2.second, LogLevelConfig.Level.WARNING, config2.first.configValue)
        assertEquals(config3.second, LogLevelConfig.Level.WARNING, config3.first.configValue)
        assertEquals(config4.second, LogLevelConfig.Level.WARNING, config4.first.configValue)
    }

    @Test
    fun testErrorConfig() {
        val config1 = Pair(LogLevelConfig("e"), "lower case single letter")
        val config2 = Pair(LogLevelConfig("E"), "upper case single letter")
        val config3 = Pair(LogLevelConfig("error"), "lower case word")
        val config4 = Pair(LogLevelConfig("ERROR"), "upper case word")

        assertEquals(config1.second, LogLevelConfig.Level.ERROR, config1.first.configValue)
        assertEquals(config2.second, LogLevelConfig.Level.ERROR, config2.first.configValue)
        assertEquals(config3.second, LogLevelConfig.Level.ERROR, config3.first.configValue)
        assertEquals(config4.second, LogLevelConfig.Level.ERROR, config4.first.configValue)
    }

    @Test
    fun testInvalidConfig() {
        val config1 = Pair(LogLevelConfig(""), "empty")
        val config2 = Pair(LogLevelConfig("4"), "invalid char")
        val config3 = Pair(LogLevelConfig("invalid"), "invalid word")
        val config4 = Pair(LogLevelConfig("INVALID"), "upper case word")

        assertEquals(config1.second, LogLevelConfig.DEFAULT_LEVEL, config1.first.configValue)
        assertEquals(config2.second, LogLevelConfig.DEFAULT_LEVEL, config2.first.configValue)
        assertEquals(config3.second, LogLevelConfig.DEFAULT_LEVEL, config3.first.configValue)
        assertEquals(config4.second, LogLevelConfig.DEFAULT_LEVEL, config4.first.configValue)
    }
}