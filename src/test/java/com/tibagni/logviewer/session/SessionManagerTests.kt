package com.tibagni.logviewer.session

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SessionManagerTests {
    private lateinit var tempDir: File
    private lateinit var tempSessionFile: File
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("logviewer_session_test").toFile()
        tempSessionFile = File(tempDir, "session.json")
        sessionManager = SessionManager(tempSessionFile)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testReadNonExistentSession() {
        assertNull(sessionManager.readSession())
    }

    @Test
    fun testWriteAndReadSession() {
        val data = SessionData(
            version = 1,
            cleanExit = false,
            logFiles = listOf(File("/dummy/log.txt"))
        )
        sessionManager.writeSession(data)
        
        assertTrue(tempSessionFile.exists())
        
        val readData = sessionManager.readSession()
        assertNotNull(readData)
        assertEquals(false, readData?.cleanExit)
        assertEquals(1, readData?.logFiles?.size)
        assertEquals(File("/dummy/log.txt").absolutePath, readData?.logFiles?.get(0)?.absolutePath)
    }

    @Test
    fun testMarkCleanExit() {
        val data = SessionData(cleanExit = false)
        sessionManager.writeSession(data)
        
        sessionManager.markCleanExit()
        
        val readData = sessionManager.readSession()
        assertNotNull(readData)
        assertEquals(true, readData?.cleanExit)
    }

    @Test
    fun testDeleteSession() {
        val data = SessionData()
        sessionManager.writeSession(data)
        assertTrue(tempSessionFile.exists())
        
        sessionManager.deleteSession()
        assertTrue(!tempSessionFile.exists())
    }

    @Test
    fun testCustomTargetFileReadAndWrite() {
        val customFile = File(tempDir, "custom-session.json")
        val data = SessionData(
            version = 1,
            cleanExit = true,
            logFiles = listOf(File("/dummy/custom.txt"))
        )
        sessionManager.writeSession(data, customFile)
        assertTrue(customFile.exists())

        val readData = sessionManager.readSession(customFile)
        assertNotNull(readData)
        assertEquals(true, readData?.cleanExit)
        assertEquals(1, readData?.logFiles?.size)
        assertEquals(File("/dummy/custom.txt").absolutePath, readData?.logFiles?.get(0)?.absolutePath)
    }
}