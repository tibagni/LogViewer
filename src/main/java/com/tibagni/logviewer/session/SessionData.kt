package com.tibagni.logviewer.session

import org.json.JSONObject
import java.io.File
import java.util.HashMap

data class WindowState(
    val x: Int = 0,
    val y: Int = 0,
    val width: Int = 1000,
    val height: Int = 500,
    val maximized: Boolean = false
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("x", x)
            put("y", y)
            put("width", width)
            put("height", height)
            put("maximized", maximized)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): WindowState {
            return WindowState(
                x = json.optInt("x", 0),
                y = json.optInt("y", 0),
                width = json.optInt("width", 1000),
                height = json.optInt("height", 500),
                maximized = json.optBoolean("maximized", false)
            )
        }
    }
}

data class LayoutState(
    val mainSplit: Int = -1,
    val logsSplit: Int = -1,
    val mainLogSplit: Int = -1,
    val myLogsVisible: Boolean = false,
    val selectedTab: Int = 0
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("mainSplit", mainSplit)
            put("logsSplit", logsSplit)
            put("mainLogSplit", mainLogSplit)
            put("myLogsVisible", myLogsVisible)
            put("selectedTab", selectedTab)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): LayoutState {
            return LayoutState(
                mainSplit = json.optInt("mainSplit", -1),
                logsSplit = json.optInt("logsSplit", -1),
                mainLogSplit = json.optInt("mainLogSplit", -1),
                myLogsVisible = json.optBoolean("myLogsVisible", false),
                selectedTab = json.optInt("selectedTab", 0)
            )
        }
    }
}

data class MyLogEntryData(
    val index: Int = -1,
    val text: String = ""
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("index", index)
            put("text", text)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): MyLogEntryData {
            return MyLogEntryData(
                index = json.optInt("index", -1),
                text = json.optString("text", "")
            )
        }
    }
}

data class SessionData @JvmOverloads constructor(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    var cleanExit: Boolean = false,
    val logFiles: List<File> = emptyList(),
    val filterFiles: List<File> = emptyList(),
    val appliedFilters: Map<String, List<Int>> = emptyMap(),
    val window: WindowState = WindowState(),
    val layout: LayoutState = LayoutState(),
    val myLogs: List<MyLogEntryData> = emptyList()
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("version", version)
        json.put("timestamp", timestamp)
        json.put("cleanExit", cleanExit)
        
        val logsArr = org.json.JSONArray()
        logFiles.forEach { logsArr.put(it.absolutePath) }
        json.put("logFiles", logsArr)

        val filtersArr = org.json.JSONArray()
        filterFiles.forEach { filtersArr.put(it.absolutePath) }
        json.put("filterFiles", filtersArr)

        val appliedFiltersObj = JSONObject()
        appliedFilters.forEach { (group, indices) ->
            val indicesArr = org.json.JSONArray()
            indices.forEach { indicesArr.put(it) }
            appliedFiltersObj.put(group, indicesArr)
        }
        json.put("appliedFilters", appliedFiltersObj)
        
        json.put("window", window.toJson())
        json.put("layout", layout.toJson())

        val myLogsArr = org.json.JSONArray()
        myLogs.forEach { myLogsArr.put(it.toJson()) }
        json.put("myLogs", myLogsArr)
        
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): SessionData {
            val logFiles = mutableListOf<File>()
            json.optJSONArray("logFiles")?.let { arr ->
                for (i in 0 until arr.length()) {
                    logFiles.add(File(arr.getString(i)))
                }
            }

            val filterFiles = mutableListOf<File>()
            json.optJSONArray("filterFiles")?.let { arr ->
                for (i in 0 until arr.length()) {
                    filterFiles.add(File(arr.getString(i)))
                }
            }

            val appliedFilters = mutableMapOf<String, List<Int>>()
            json.optJSONObject("appliedFilters")?.let { obj ->
                for (key in obj.keys()) {
                    val arr = obj.getJSONArray(key)
                    val indices = mutableListOf<Int>()
                    for (i in 0 until arr.length()) {
                        indices.add(arr.getInt(i))
                    }
                    appliedFilters[key] = indices
                }
            }

            val myLogs = mutableListOf<MyLogEntryData>()
            json.optJSONArray("myLogs")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    if (obj != null) {
                        myLogs.add(MyLogEntryData.fromJson(obj))
                    } else {
                        val idx = arr.optInt(i, -1)
                        if (idx >= 0) {
                            myLogs.add(MyLogEntryData(idx, ""))
                        }
                    }
                }
            }

            return SessionData(
                version = json.optInt("version", 1),
                timestamp = json.optLong("timestamp", 0),
                cleanExit = json.optBoolean("cleanExit", false),
                logFiles = logFiles,
                filterFiles = filterFiles,
                appliedFilters = appliedFilters,
                window = WindowState.fromJson(json.optJSONObject("window") ?: JSONObject()),
                layout = LayoutState.fromJson(json.optJSONObject("layout") ?: JSONObject()),
                myLogs = myLogs
            )
        }
    }
}