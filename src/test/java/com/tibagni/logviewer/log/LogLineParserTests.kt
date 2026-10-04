package com.tibagni.logviewer.log

import com.tibagni.logviewer.filter.Filter
import org.junit.Assert.*
import org.junit.Test
import java.awt.Color
import java.io.File

class LogLineParserTests {

    @Test
    fun testFindPid() {
        // Standard threadtime format
        assertEquals(3172, LogLineParser.findPid("10-12 22:33:46.839  3172  3172 V KeyguardStatusView: refresh statusview showing:true"))
        assertEquals(2646, LogLineParser.findPid("10-12 22:32:50.264  2646  2664 I chatty  : uid=1000(system) batterystats-sy expire 13 lines"))
        assertEquals(442, LogLineParser.findPid("10-13 03:00:11.066   442  8037 W vold    : Failed to open none: No such file or directory"))
        assertEquals(18114, LogLineParser.findPid("10-13 12:27:59.318 18114 18114 E ActivityThread: Activity leaked"))

        // Threadtime format with UID
        assertEquals(2071, LogLineParser.findPid("10-04 15:53:01.303 1000 2071 2136 I ActivityManager: Start proc"))
        assertEquals(2071, LogLineParser.findPid("10-04 15:53:01.303  1000  2071  2136 D Tag: Message"))

        // PID-TID format
        assertEquals(821, LogLineParser.findPid("01-06 20:46:26.091 821-2168/? V/ThermalMonitor: Foreground Application Changed"))
        assertEquals(821, LogLineParser.findPid("01-06 20:46:42.501 821-2810/? I/ActivityManager: Process died"))
        assertEquals(25175, LogLineParser.findPid("01-06 20:46:39.481 25175-25175/? E/AndroidRuntime: FATAL EXCEPTION: main"))
        assertEquals(25175, LogLineParser.findPid("01-06 20:46:39.481 25175-25175 E/AndroidRuntime: FATAL EXCEPTION: main"))

        // With 4-digit year and comma subseconds
        assertEquals(2071, LogLineParser.findPid("2024-10-04 15:53:01.303  2071  2136 I Tag: Message"))
        assertEquals(2071, LogLineParser.findPid("10-04 15:53:01,303  2071  2136 I Tag: Message"))

        // Non-matching lines
        assertEquals(-1, LogLineParser.findPid("Not a log line"))
        assertEquals(-1, LogLineParser.findPid(""))
        assertEquals(-1, LogLineParser.findPid(null))
        assertEquals(-1, LogLineParser.findPid("\tat com.android.server.am.ActivityManagerService.startProcess(ActivityManagerService.java:123)"))
        assertEquals(-1, LogLineParser.findPid("--------- beginning of main"))
        assertEquals(-1, LogLineParser.findPid("10-04 15:53:01.303 Some message without pid tid or level"))
    }

    @Test
    fun testPidFilterMatching() {
        val pid = 2071
        val pattern = LogLineParser.getFilterPatternForPid(pid)
        val filter = Filter("PID $pid", pattern, Color.RED, LogLevel.VERBOSE, false)

        fun entry(text: String) = LogEntry(text, LogLevel.DEBUG, null)

        // Positive matches
        assertTrue(filter.appliesTo(entry("10-04 15:53:01.303  2071  2136 I ActivityManager: Start proc")))
        assertTrue(filter.appliesTo(entry("10-04 15:53:01.303 1000 2071 2136 I ActivityManager: Start proc")))
        assertTrue(filter.appliesTo(entry("01-06 20:46:26.091 2071-2168/? V/ThermalMonitor: Message")))
        assertTrue(filter.appliesTo(entry("2024-10-04 15:53:01.303  2071  2136 I Tag: Message")))

        // Negative matches: PID appearing elsewhere
        assertFalse(filter.appliesTo(entry("10-04 15:53:01.303  1000  2136 I ActivityManager: packet with 2071 bytes")))
        assertFalse(filter.appliesTo(entry("10-04 15:53:01.303  1000  2071 I ActivityManager: TID match only")))
        assertFalse(filter.appliesTo(entry("10-04 15:53:01.303  20710  2136 I ActivityManager: prefix PID")))
        assertFalse(filter.appliesTo(entry("10-04 15:53:01.303  12071  2136 I ActivityManager: suffix PID")))
        assertFalse(filter.appliesTo(entry("01-06 20:46:26.091 20710-2168/? V/ThermalMonitor: Message")))
    }

    @Test
    fun testTagFilterMatching() {
        val tag = "ActivityManager"
        val pattern = LogLineParser.getFilterPatternForTag(tag)
        val filter = Filter(tag, pattern, Color.BLUE, LogLevel.VERBOSE, false)

        fun entry(text: String) = LogEntry(text, LogLevel.DEBUG, null)

        // Positive matches
        assertTrue(filter.appliesTo(entry("10-12 22:32:50.264  2646  2664 I ActivityManager: Process com.void has died.")))
        assertTrue(filter.appliesTo(entry("10-04 15:53:01.303 1000 2071 2136 I ActivityManager: Start proc")))
        assertTrue(filter.appliesTo(entry("01-06 20:46:42.501 821-2810/? I/ActivityManager: Process died.")))
        assertTrue(filter.appliesTo(entry("01-06 20:46:42.501 821-2810 I ActivityManager: Process died.")))

        // Negative matches: tag appearing only in message payload or prefix/suffix match
        assertFalse(filter.appliesTo(entry("10-12 22:32:50.264  2646  2664 I chatty: ActivityManager: died")))
        assertFalse(filter.appliesTo(entry("10-12 22:32:50.264  2646  2664 I ActivityManagerService: Process died.")))
        assertFalse(filter.appliesTo(entry("01-06 20:46:42.501 821-2810/? I/MyActivityManager: Process died.")))

        // Tag with regex special characters
        val specialTag = "Tag[1].Sub"
        val specialPattern = LogLineParser.getFilterPatternForTag(specialTag)
        val specialFilter = Filter(specialTag, specialPattern, Color.GREEN, LogLevel.VERBOSE, false)
        assertTrue(specialFilter.appliesTo(entry("10-12 22:32:50.264  2646  2664 D Tag[1].Sub: Special tag test")))
        assertTrue(specialFilter.appliesTo(entry("01-06 20:46:42.501 821-2810/? D/Tag[1].Sub: Special tag test")))

        // Padded tag before colon (like Android threadtime format with column padding)
        val csstTag = "CSST"
        val csstPattern = LogLineParser.getFilterPatternForTag(csstTag)
        val csstFilter = Filter(csstTag, csstPattern, Color.MAGENTA, LogLevel.VERBOSE, false)
        assertTrue(csstFilter.appliesTo(entry("09-28 10:01:00.422  8481  8481 I CSST    : reading time to delay notification pref network: -1")))
        assertTrue(csstFilter.appliesTo(entry("09-28 10:01:00.422 I/CSST( 8481): message")))
        assertFalse(csstFilter.appliesTo(entry("09-28 10:01:00.422  8481  8481 I CSST_EXTRA: message")))
    }

    @Test
    fun testParseLineInfoThreadtime() {
        val line = "10-12 22:33:46.839  3172  3172 V KeyguardStatusView: refresh statusview showing:true"
        val file = File("/tmp/logcat.txt")
        val entry = LogEntry(line, LogLevel.VERBOSE, LogTimestamp(10, 12, 22, 33, 46, 839), LogStream.MAIN, file, 105)

        val info = LogLineParser.parseLineInfo(entry)
        assertNotNull(info)
        assertEquals(file, info!!.sourceFile)
        assertEquals("logcat.txt", info.fileName)
        assertEquals("/tmp/logcat.txt", info.filePath)
        assertEquals(105, info.lineNumber)
        assertEquals("105", info.formattedLineNumber)
        assertEquals(LogStream.MAIN, info.logStream)
        assertEquals(LogLevel.VERBOSE, info.logLevel)
        assertEquals(3172, info.pid)
        assertEquals("3172", info.formattedPid)
        assertEquals(3172, info.tid)
        assertEquals("3172", info.formattedTid)
        assertEquals("KeyguardStatusView", info.tag)
        assertEquals("refresh statusview showing:true", info.message)
    }

    @Test
    fun testParseLineInfoThreadtimeWithUid() {
        val line = "10-04 15:53:01.303  1000  2071  2136 I ActivityManager: Start proc com.example"
        val entry = LogEntry(line, LogLevel.INFO, null)

        val info = LogLineParser.parseLineInfo(entry)
        assertNotNull(info)
        assertEquals(2071, info!!.pid)
        assertEquals(2136, info.tid)
        assertEquals("ActivityManager", info.tag)
        assertEquals("Start proc com.example", info.message)
    }

    @Test
    fun testParseLineInfoPidTidSlash() {
        val line = "01-06 20:46:26.091 821-2168/? V/ThermalMonitor: Foreground Application Changed: com.void"
        val entry = LogEntry(line, LogLevel.VERBOSE, null)

        val info = LogLineParser.parseLineInfo(entry)
        assertNotNull(info)
        assertEquals(821, info!!.pid)
        assertEquals(2168, info.tid)
        assertEquals("ThermalMonitor", info.tag)
        assertEquals("Foreground Application Changed: com.void", info.message)
    }

    @Test
    fun testParseLineInfoPidTidSpace() {
        val line = "01-06 20:46:26.091 821-2168 V ThermalMonitor: Foreground Application Changed"
        val entry = LogEntry(line, LogLevel.VERBOSE, null)

        val info = LogLineParser.parseLineInfo(entry)
        assertNotNull(info)
        assertEquals(821, info!!.pid)
        assertEquals(2168, info.tid)
        assertEquals("ThermalMonitor", info.tag)
        assertEquals("Foreground Application Changed", info.message)
    }

    @Test
    fun testParseLineInfoFallback() {
        val line = "Random log line with pid 9341 embedded in message"
        val entry = LogEntry(line, LogLevel.DEBUG, null)

        val info = LogLineParser.parseLineInfo(entry)
        assertNotNull(info)
        assertNull(info!!.pid)
        assertNull(info.tid)
        assertNull(info.tag)
        assertEquals(line, info.message)
        assertEquals("-", info.formattedTid)
        assertEquals("-", info.formattedTag)
        assertEquals("-", info.formattedLineNumber)
        assertFalse(info.hasFile)

        // Line with valid timestamp and PID prefix but non-standard tag format (no colon)
        val lineWithPid = "10-04 15:53:01.303  9341  1234 D NonStandardLineWithoutColon"
        val entryWithPid = LogEntry(lineWithPid, LogLevel.DEBUG, null)
        val infoWithPid = LogLineParser.parseLineInfo(entryWithPid)
        assertNotNull(infoWithPid)
        assertEquals(9341, infoWithPid!!.pid)
    }
}
