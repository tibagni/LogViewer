package com.tibagni.logviewer.session

import com.tibagni.logviewer.logger.Logger
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.schedule

class SessionManager(private val sessionFile: File = defaultSessionFile()) {
    
    private var saveTimer: Timer? = null
    private var saveTask: TimerTask? = null
    private val debounceDelayMs = 500L

    companion object {
        fun defaultSessionFile(): File {
            val home = System.getProperty("user.home")
            val logviewerDir = File(home, ".logviewer")
            if (!logviewerDir.exists()) {
                logviewerDir.mkdirs()
            }
            return File(logviewerDir, "session.json")
        }
    }

    @JvmOverloads
    fun readSession(targetFile: File = sessionFile): SessionData? {
        if (!targetFile.exists() || !targetFile.isFile) return null
        
        return try {
            val jsonStr = targetFile.readText()
            SessionData.fromJson(JSONObject(jsonStr))
        } catch (e: Exception) {
            Logger.error("Failed to read session data from ${targetFile.absolutePath}", e)
            null
        }
    }

    @JvmOverloads
    fun writeSession(sessionData: SessionData, targetFile: File = sessionFile) {
        try {
            val tempFile = File(targetFile.absolutePath + ".tmp")
            tempFile.writeText(sessionData.toJson().toString(2))
            
            if (tempFile.renameTo(targetFile)) {
                // Success
            } else {
                // If rename fails, try normal copy then delete
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
        } catch (e: IOException) {
            Logger.error("Failed to write session data to ${targetFile.absolutePath}", e)
        }
    }

    fun writeSessionDebounced(sessionData: SessionData) {
        if (saveTimer == null) {
            saveTimer = Timer("SessionSaveTimer", true)
        }
        saveTask?.cancel()
        saveTask = saveTimer?.schedule(debounceDelayMs) {
            writeSession(sessionData)
        }
    }

    fun markCleanExit() {
        saveTask?.cancel()
        val session = readSession()
        if (session != null) {
            session.cleanExit = true
            writeSession(session)
        }
    }
    
    fun deleteSession() {
        saveTask?.cancel()
        if (sessionFile.exists()) {
            sessionFile.delete()
        }
    }
}
