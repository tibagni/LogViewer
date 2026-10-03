package com.tibagni.logviewer.log.parser

import com.tibagni.logviewer.ProgressReporter
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.log.LogReader
import com.tibagni.logviewer.log.LogStream
import com.tibagni.logviewer.log.LogTimestamp
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import com.tibagni.logviewer.filter.Filter
import java.awt.Color
import java.nio.charset.StandardCharsets

class ParserTests {
    private lateinit var logParser: LogParser

    @Mock
    private lateinit var reader: LogReader

    @Mock
    private lateinit var progressReporter: ProgressReporter

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        logParser = LogParser(reader, progressReporter)
    }

    @Test
    fun testFindLogLevel() {
        val verbose = logParser.findLogLevel("01-06 20:46:26.091 821-2168/? V/ThermalMonitor: Foreground Application Changed: com.voidcorporation.carimbaai")
        val debug = logParser.findLogLevel("01-06 20:46:26.091 821-2168/? D/ThermalMonitor: Foreground Application Changed: com.voidcorporation.carimbaai")
        val info = logParser.findLogLevel("01-06 20:46:42.501 821-2810/? I/ActivityManager: Process com.voidcorporation.carimbaai (pid 25175) (adj 0) has died.")
        val warn = logParser.findLogLevel("01-06 20:46:39.491 821-1054/? W/ActivityManager:   Force finishing activity com.voidcorporation.carimbaai/.UserProfileActivity")
        val error = logParser.findLogLevel("01-06 20:46:39.481 25175-25175/? E/AndroidRuntime: FATAL EXCEPTION: main")

        val verbose2 = logParser.findLogLevel("10-12 22:33:46.839  3172  3172 V KeyguardStatusView: refresh statusview showing:true")
        val debug2 = logParser.findLogLevel("10-12 22:53:16.205  3172  3172 D StatusBarKeyguardViewManager: requestUnlock collapse=true")
        val info2 = logParser.findLogLevel("10-12 22:32:50.264  2646  2664 I chatty  : uid=1000(system) batterystats-sy expire 13 lines")
        val warn2 = logParser.findLogLevel("10-13 03:00:11.066   442  8037 W vold    : Failed to open none: No such file or directory")
        val error2 = logParser.findLogLevel("10-13 12:27:59.318 18114 18114 E ActivityThread: Activity com.facebook.katana.activity.FbMainTabActivity has leaked ServiceConnection X.8x6@fa849cf that was originally bound here")

        assertEquals(LogLevel.VERBOSE, verbose)
        assertEquals(LogLevel.DEBUG, debug)
        assertEquals(LogLevel.INFO, info)
        assertEquals(LogLevel.WARNING, warn)
        assertEquals(LogLevel.ERROR, error)

        assertEquals(LogLevel.VERBOSE, verbose2)
        assertEquals(LogLevel.DEBUG, debug2)
        assertEquals(LogLevel.INFO, info2)
        assertEquals(LogLevel.WARNING, warn2)
        assertEquals(LogLevel.ERROR, error2)
    }

    @Test
    fun testFindTimestamp() {
        val expected = LogTimestamp(10, 12, 22, 32, 50, 264)
        val actual = logParser.findTimestamp("10-12 22:32:50.264  2646  2664 I chatty  : uid=1000(system) batterystats-sy expire 13 lines")

        val expected2 = LogTimestamp(1, 6, 20, 46, 42, 501)
        val actual2 = logParser.findTimestamp("01-06 20:46:42.501 821-1054/? I/WindowState: WIN DEATH: Window{431586d0 u0 com.voidcorporation.carimbaai/com.voidcorporation.carimbaai.MainActivity}")

        val expectedMicro = LogTimestamp(12, 31, 23, 59, 59, 999999)
        val actualMicro = logParser.findTimestamp("12-31 23:59:59.999999 1 1 D Test: Microseconds")

        val expectedNano = LogTimestamp(5, 12, 13, 45, 10, 123456789)
        val actualNano = logParser.findTimestamp("05-12 13:45:10.123456789 1 1 D Test: Nanoseconds")

        val expectedSingleDigit = LogTimestamp(1, 6, 20, 46, 42, 501)
        val actualSingleDigit = logParser.findTimestamp("1-6 20:46:42.501 821-1054/? I/WindowState: single digit month")

        val expectedComma = LogTimestamp(10, 12, 22, 32, 50, 264)
        val actualComma = logParser.findTimestamp("10-12 22:32:50,264  2646  2664 I chatty  : comma separator")

        val expectedTab = LogTimestamp(10, 12, 22, 32, 50, 264)
        val actualTab = logParser.findTimestamp("10-12\t22:32:50.264  2646  2664 I chatty  : tab separator")

        assertEquals(expected, actual)
        assertEquals(expected2, actual2)
        assertEquals(expectedMicro, actualMicro)
        assertEquals(expectedNano, actualNano)
        assertEquals(expectedSingleDigit, actualSingleDigit)
        assertEquals(expectedComma, actualComma)
        assertEquals(expectedTab, actualTab)

        assertNull(logParser.findTimestamp("05-12 13:45:10.12 1 1 D Test: Invalid short ms"))
        assertNull(logParser.findTimestamp("Not a log line"))
        assertNull(logParser.findTimestamp(""))
    }

    @Test
    fun testIsLogLine() {
        assertTrue(logParser.isLogLine("10-12 22:32:50.264  2646  2664 I chatty  : uid=1000(system) batterystats-sy expire 13 lines"))
        assertTrue(logParser.isLogLine("01-06 20:46:42.501 821-1054/? I/WindowState: WIN DEATH"))
        assertTrue(logParser.isLogLine("00-00 00:00:00"))
        assertTrue(logParser.isLogLine("99-99 99:99:99 anything after"))

        assertFalse(logParser.isLogLine("10-12 22:32:5")) // too short (< 14 chars)
        assertFalse(logParser.isLogLine("10/12 22:32:50")) // wrong separator
        assertFalse(logParser.isLogLine("10-12-22:32:50")) // missing space
        assertFalse(logParser.isLogLine("1-12 22:32:50")) // 1-digit month
        assertFalse(logParser.isLogLine("10-1 22:32:50")) // 1-digit day
        assertFalse(logParser.isLogLine("\tat com.android.server.am.ActivityManagerService.startProcess(ActivityManagerService.java:123)"))
        assertFalse(logParser.isLogLine("--------- beginning of main"))
        assertFalse(logParser.isLogLine(""))
    }

    @Test
    fun testFindPid() {
        // Standard threadtime format
        assertEquals(3172, LogParser.findPid("10-12 22:33:46.839  3172  3172 V KeyguardStatusView: refresh statusview showing:true"))
        assertEquals(2646, LogParser.findPid("10-12 22:32:50.264  2646  2664 I chatty  : uid=1000(system) batterystats-sy expire 13 lines"))
        assertEquals(442, LogParser.findPid("10-13 03:00:11.066   442  8037 W vold    : Failed to open none: No such file or directory"))
        assertEquals(18114, LogParser.findPid("10-13 12:27:59.318 18114 18114 E ActivityThread: Activity leaked"))

        // Threadtime format with UID
        assertEquals(2071, LogParser.findPid("10-04 15:53:01.303 1000 2071 2136 I ActivityManager: Start proc"))
        assertEquals(2071, LogParser.findPid("10-04 15:53:01.303  1000  2071  2136 D Tag: Message"))

        // PID-TID format
        assertEquals(821, LogParser.findPid("01-06 20:46:26.091 821-2168/? V/ThermalMonitor: Foreground Application Changed"))
        assertEquals(821, LogParser.findPid("01-06 20:46:42.501 821-2810/? I/ActivityManager: Process died"))
        assertEquals(25175, LogParser.findPid("01-06 20:46:39.481 25175-25175/? E/AndroidRuntime: FATAL EXCEPTION: main"))
        assertEquals(25175, LogParser.findPid("01-06 20:46:39.481 25175-25175 E/AndroidRuntime: FATAL EXCEPTION: main"))

        // With 4-digit year and comma subseconds
        assertEquals(2071, LogParser.findPid("2024-10-04 15:53:01.303  2071  2136 I Tag: Message"))
        assertEquals(2071, LogParser.findPid("10-04 15:53:01,303  2071  2136 I Tag: Message"))

        // Non-matching lines
        assertEquals(-1, LogParser.findPid("Not a log line"))
        assertEquals(-1, LogParser.findPid(""))
        assertEquals(-1, LogParser.findPid(null))
        assertEquals(-1, LogParser.findPid("\tat com.android.server.am.ActivityManagerService.startProcess(ActivityManagerService.java:123)"))
        assertEquals(-1, LogParser.findPid("--------- beginning of main"))
        assertEquals(-1, LogParser.findPid("10-04 15:53:01.303 Some message without pid tid or level"))
    }

    @Test
    fun testPidFilterMatching() {
        val pid = 2071
        val pattern = LogParser.getFilterPatternForPid(pid)
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
    fun testParseLogs() {
        val testLogLine = "10-12 22:32:50.264  2646  2664 I test  : Test log Test Log"
        val logNames = setOf("main", "radio", "system", "events")

        val expectedLogs = Array(4) { testLogLine }

        `when`(reader.availableLogPaths).thenReturn(logNames)
        `when`(reader.get(ArgumentMatchers.any())).thenReturn(testLogLine)

        val entries = logParser.parseLogs(StandardCharsets.UTF_8)

        val progressCaptor = ArgumentCaptor.forClass(Int::class.java)
        val descriptionCaptor = ArgumentCaptor.forClass(String::class.java)
        verify<ProgressReporter>(progressReporter, times(7))
                .onProgress(progressCaptor.capture(), descriptionCaptor.capture())

        val progresses = progressCaptor.allValues
        val descriptions = descriptionCaptor.allValues

        assertTrue(progresses.contains(0))
        assertTrue(progresses.contains(22))
        assertTrue(progresses.contains(45))
        assertTrue(progresses.contains(67))
        assertTrue(progresses.contains(91))
        assertTrue(progresses.contains(95))
        assertTrue(progresses.contains(100))

        assertTrue(descriptions.contains("Reading main..."))
        assertTrue(descriptions.contains("Reading radio..."))
        assertTrue(descriptions.contains("Reading system..."))
        assertTrue(descriptions.contains("Reading events..."))
        assertTrue(descriptions.contains("Sorting..."))
        assertTrue(descriptions.contains("Setting index..."))
        assertTrue(descriptions.contains("Completed"))

        val actualLogs = entries.map { it.logText }.toTypedArray()
        assertArrayEquals(expectedLogs, actualLogs)
    }

    @Test
    fun testParseLogsWithContinuationLines() {
        val multilineLog = "10-12 22:32:50.264  2646  2664 E AndroidRuntime: FATAL EXCEPTION: main\n" +
                "\tProcess: com.android.settings, PID: 12345\n" +
                "\tjava.lang.NullPointerException: Null pointer Exception in service\n" +
                "10-12 22:32:51.100  2646  2664 I ActivityManager: Process died\n" +
                "10-12 22:32:52.000  2646  2664 D Tag: Single line log"

        `when`(reader.availableLogPaths).thenReturn(setOf("main.txt"))
        `when`(reader.get("main.txt")).thenReturn(multilineLog)

        val entries = logParser.parseLogs(StandardCharsets.UTF_8)
        assertEquals(3, entries.size)
        assertEquals(
            "10-12 22:32:50.264  2646  2664 E AndroidRuntime: FATAL EXCEPTION: main" + System.lineSeparator() +
                    "\tProcess: com.android.settings, PID: 12345" + System.lineSeparator() +
                    "\tjava.lang.NullPointerException: Null pointer Exception in service",
            entries[0].logText
        )
        assertEquals(LogLevel.ERROR, entries[0].logLevel)
        assertEquals(LogStream.MAIN, entries[0].logStream)

        assertEquals("10-12 22:32:51.100  2646  2664 I ActivityManager: Process died", entries[1].logText)
        assertEquals(LogLevel.INFO, entries[1].logLevel)

        assertEquals("10-12 22:32:52.000  2646  2664 D Tag: Single line log", entries[2].logText)
        assertEquals(LogLevel.DEBUG, entries[2].logLevel)
    }

    @Test
    fun testParseInvalidLogs() {
        val testLogLine = buildHugeLogPayload()
        val logNames = setOf("bugreport")

        `when`(reader.availableLogPaths).thenReturn(logNames)
        `when`(reader.get(ArgumentMatchers.any())).thenReturn(testLogLine)

        val parsedLogs = logParser.parseLogs(StandardCharsets.UTF_8)

        // The log should still have been parsed. But it should be truncated to the maximum allowed size
        assertEquals(0, logParser.logsSkipped.size)
        assertEquals(1, parsedLogs.size)
        assertEquals(LogParser.MAX_LOG_LINE_ALLOWED, parsedLogs[0].logText.length)
    }

    private fun buildHugeLogPayload(): String {
        val builder = StringBuilder()
        builder.append("10-12 22:32:50.264  2646  2664 I test  : Test log Test Log")

        repeat(1000) {
            builder.append(" Test log Test Log test")
            builder.append(System.lineSeparator())
        }

        return builder.toString()
    }

    @Test
    fun testAvailableLogStreamsUNKOWN() {
        `when`(reader.availableLogPaths).thenReturn(setOf(
                "bla",
                "nothing",
                "bugreport.txt",
                "logcat.txt",
                "myLogs.txt")
        )

        assertEquals(setOf(LogStream.UNKNOWN), logParser.availableStreams)
    }

    @Test
    fun testAvailableLogStreamsMAIN() {
        `when`(reader.availableLogPaths).thenReturn(setOf(
                "main.txt",
                "aplogd-m.txt")
        )

        assertEquals(setOf(LogStream.MAIN), logParser.availableStreams)
    }

    @Test
    fun testAvailableLogStreamsSYSTEM() {
        `when`(reader.availableLogPaths).thenReturn(setOf(
                "system.txt",
                "aplogd-s.txt")
        )

        assertEquals(setOf(LogStream.SYSTEM), logParser.availableStreams)
    }

    @Test
    fun testAvailableLogStreamsRADIO() {
        `when`(reader.availableLogPaths).thenReturn(setOf(
                "radio.txt",
                "aplogd-r.txt")
        )

        assertEquals(setOf(LogStream.RADIO), logParser.availableStreams)
    }

    @Test
    fun testAvailableLogStreamsEVENTS() {
        `when`(reader.availableLogPaths).thenReturn(setOf(
                "events.txt",
                "aplogd-e.txt")
        )

        assertEquals(setOf(LogStream.EVENTS), logParser.availableStreams)
    }

    @Test
    fun testAvailableLogStreamsALL() {
        `when`(reader.availableLogPaths).thenReturn(setOf(
                "aplogd-e.txt",
                "main.txt",
                "radio.txt",
                "aplogd-s.txt")
        )

        val expected = setOf(
                LogStream.MAIN,
                LogStream.SYSTEM,
                LogStream.RADIO,
                LogStream.EVENTS
        )
        val actual = logParser.availableStreams

        assertEquals(expected.size, actual.size)
        assertTrue(actual.containsAll(expected))
    }

    @Test
    fun testAvailableLogStreamsALLwithUNKNOWN() {
        `when`(reader.availableLogPaths).thenReturn(setOf(
                "bla.txt",
                "aplogd-e.txt",
                "main.txt",
                "radio.txt",
                "aplogd-s.txt")
        )

        val expected = setOf(
                LogStream.MAIN,
                LogStream.SYSTEM,
                LogStream.RADIO,
                LogStream.EVENTS,
                LogStream.UNKNOWN
        )
        val actual = logParser.availableStreams

        assertEquals(expected.size, actual.size)
        assertTrue(actual.containsAll(expected))
    }
}
