package com.tibagni.logviewer.session

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SessionDataTests {
    @Test
    fun testSerializationAndDeserialization() {
        val original = SessionData(
            version = 1,
            timestamp = 123456789L,
            cleanExit = true,
            logFiles = listOf(File("/test/log1.txt"), File("/test/log2.txt")),
            filterFiles = listOf(File("/test/filter1.flt")),
            appliedFilters = mapOf("filter1.flt" to listOf(0, 2)),
            window = WindowState(10, 20, 800, 600, true),
            layout = LayoutState(100, 200, 300, true, 1),
            myLogs = listOf(MyLogEntryData(5, "Test my log line"))
        )

        val jsonStr = original.toJson().toString()
        val deserialized = SessionData.fromJson(JSONObject(jsonStr))

        assertEquals(original.version, deserialized.version)
        assertEquals(original.timestamp, deserialized.timestamp)
        assertEquals(original.cleanExit, deserialized.cleanExit)
        assertEquals(original.logFiles, deserialized.logFiles)
        assertEquals(original.filterFiles, deserialized.filterFiles)
        assertEquals(original.appliedFilters, deserialized.appliedFilters)
        assertEquals(original.window, deserialized.window)
        assertEquals(original.layout, deserialized.layout)
        assertEquals(original.myLogs, deserialized.myLogs)
    }

    @Test
    fun testDeserializationWithMissingFields() {
        val json = JSONObject()
        val deserialized = SessionData.fromJson(json)

        assertEquals(1, deserialized.version)
        assertEquals(false, deserialized.cleanExit)
        assertTrue(deserialized.logFiles.isEmpty())
        assertTrue(deserialized.filterFiles.isEmpty())
        assertTrue(deserialized.appliedFilters.isEmpty())
        assertTrue(deserialized.myLogs.isEmpty())
        assertEquals(WindowState(), deserialized.window)
        assertEquals(LayoutState(), deserialized.layout)
    }
}