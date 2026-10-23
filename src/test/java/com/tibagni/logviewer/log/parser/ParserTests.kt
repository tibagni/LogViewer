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

        assertEquals(1, entries[0].lineNumber)
        assertEquals(4, entries[1].lineNumber)
        assertEquals(5, entries[2].lineNumber)
    }

    @Test
    fun testParseBugreportWithIndentedLogsAndSectionBoundaries() {
        val bugreportContent = "========================================================\n" +
                "== dumpstate: 2026-09-11 14:27:20\n" +
                "========================================================\n" +
                "Bugreport format version: 2.0\n" +
                "------ SYSTEM LOG (logcat -v threadtime -d *:v) ------\n" +
                "09-11 14:27:20.100  1000  1000 I ActivityManager: Line 1\n" +
                "09-11 14:27:20.200  1000  1000 I ActivityManager: Line 2\n" +
                "------ 0.120s was the duration of 'SYSTEM LOG' ------\n" +
                "Non-log section duration text that should not bleed into Line 2\n" +
                "------ DUMPSYS (dumpsys) ------\n" +
                "    08-04 19:37:00.810 D AnimatableClockView: animateDoze(isDozing=false)\n" +
                "    08-04 19:37:01.000 D AnimatableClockView: animateDoze(isDozing=true)\n" +
                "        09-11 13:53:29.984 ROTATION_0 to ROTATION_90\n" +
                "          source=WindowManager\n" +
                "------ 0.200s was the duration of 'DUMPSYS' ------\n"

        `when`(reader.availableLogPaths).thenReturn(setOf("bugreport.txt"))
        `when`(reader.get("bugreport.txt")).thenReturn(bugreportContent)

        val entries = logParser.parseLogs(StandardCharsets.UTF_8)

        // 5 distinct entries sorted chronologically: 08-04 entries come before 09-11 entries
        assertEquals(5, entries.size)
        assertEquals("    08-04 19:37:00.810 D AnimatableClockView: animateDoze(isDozing=false)", entries[0].logText)
        assertEquals(LogLevel.DEBUG, entries[0].logLevel)
        assertEquals(LogTimestamp(8, 4, 19, 37, 0, 810), entries[0].timestamp)

        assertEquals("    08-04 19:37:01.000 D AnimatableClockView: animateDoze(isDozing=true)", entries[1].logText)
        assertEquals(LogLevel.DEBUG, entries[1].logLevel)
        assertEquals(LogTimestamp(8, 4, 19, 37, 1, 0), entries[1].timestamp)

        assertEquals(
            "        09-11 13:53:29.984 ROTATION_0 to ROTATION_90" + System.lineSeparator() +
                    "          source=WindowManager",
            entries[2].logText
        )
        assertEquals(LogTimestamp(9, 11, 13, 53, 29, 984), entries[2].timestamp)

        assertEquals("09-11 14:27:20.100  1000  1000 I ActivityManager: Line 1", entries[3].logText)
        assertEquals("09-11 14:27:20.200  1000  1000 I ActivityManager: Line 2", entries[4].logText)

        // Verify potentialBugReports map is populated
        assertTrue(logParser.potentialBugReports.containsKey("bugreport.txt"))
    }

    @Test
    fun testParseNormalLogsPreservesIndentedContinuations() {
        val normalLogContent = "10-12 22:32:50.264  2646  2664 I MyTag: Starting JSON dump:\n" +
                "    {\n" +
                "        \"id\": 12345,\n" +
                "        \"name\": \"test\"\n" +
                "    }\n" +
                "2026-10-12 22:32:51.000  2646  2664 D MyTag: logcat -v year format\n" +
                "10-12 22:32:52.000  2646  2664 E CrashTag: Fatal Exception:\n" +
                "\tat com.android.server.am.ActivityManagerService.startProcess(ActivityManagerService.java:123)\n" +
                "\t... 12 more\n"

        `when`(reader.availableLogPaths).thenReturn(setOf("main.txt"))
        `when`(reader.get("main.txt")).thenReturn(normalLogContent)

        val entries = logParser.parseLogs(StandardCharsets.UTF_8)

        assertEquals(3, entries.size)
        // Entry 1 has JSON indented continuation preserved
        assertEquals(
            "10-12 22:32:50.264  2646  2664 I MyTag: Starting JSON dump:" + System.lineSeparator() +
                    "    {" + System.lineSeparator() +
                    "        \"id\": 12345," + System.lineSeparator() +
                    "        \"name\": \"test\"" + System.lineSeparator() +
                    "    }",
            entries[0].logText
        )
        assertEquals(LogLevel.INFO, entries[0].logLevel)

        // Entry 2 is logcat -v year format
        assertEquals("2026-10-12 22:32:51.000  2646  2664 D MyTag: logcat -v year format", entries[1].logText)
        assertEquals(LogLevel.DEBUG, entries[1].logLevel)
        assertEquals(LogTimestamp(10, 12, 22, 32, 51, 0), entries[1].timestamp)

        // Entry 3 has stack trace continuation preserved
        assertEquals(
            "10-12 22:32:52.000  2646  2664 E CrashTag: Fatal Exception:" + System.lineSeparator() +
                    "\tat com.android.server.am.ActivityManagerService.startProcess(ActivityManagerService.java:123)" + System.lineSeparator() +
                    "\t... 12 more",
            entries[2].logText
        )
        assertEquals(LogLevel.ERROR, entries[2].logLevel)

        // Verify potentialBugReports map is empty for normal logs
        assertTrue(logParser.potentialBugReports.isEmpty())
    }

    @Test
    fun testGetLogEntriesEmptyAndWhitespaceOnly() {
        val emptyResult = logParser.getLogEntries("", "main.txt", null)
        assertTrue(emptyResult.isEmpty())

        val whitespaceResult = logParser.getLogEntries("   \n\t\n  \n", "main.txt", null)
        assertTrue(whitespaceResult.isEmpty())
    }

    @Test
    fun testGetLogEntriesSingleLineWithLineNumbers() {
        val text = "\n\n10-12 22:32:50.264  2646  2664 I Tag: Line at 3\n"
        val entries = logParser.getLogEntries(text, "main.txt", null)

        assertEquals(1, entries.size)
        assertEquals("10-12 22:32:50.264  2646  2664 I Tag: Line at 3", entries[0].logText)
        assertEquals(3, entries[0].lineNumber)
        assertEquals(LogStream.MAIN, entries[0].logStream)
    }

    @Test
    fun testGetLogEntriesCrlfAndLf() {
        val text = "10-12 22:32:50.100  1000  1000 I Tag: Line 1\r\n" +
                "10-12 22:32:50.200  1000  1000 I Tag: Line 2\n" +
                "10-12 22:32:50.300  1000  1000 I Tag: Line 3\r\n"
        val entries = logParser.getLogEntries(text, "system.txt", null)

        assertEquals(3, entries.size)
        assertEquals("10-12 22:32:50.100  1000  1000 I Tag: Line 1", entries[0].logText)
        assertEquals("10-12 22:32:50.200  1000  1000 I Tag: Line 2", entries[1].logText)
        assertEquals("10-12 22:32:50.300  1000  1000 I Tag: Line 3", entries[2].logText)
        assertEquals(LogStream.SYSTEM, entries[0].logStream)
    }

    @Test
    fun testGetLogEntriesTrailingNullCharacterSanitization() {
        val text = "10-12 22:32:50.100  1000  1000 I Tag: Clean\u0000\n"
        val entries = logParser.getLogEntries(text, "main.txt", null)

        assertEquals(1, entries.size)
        assertEquals("10-12 22:32:50.100  1000  1000 I Tag: Clean", entries[0].logText)
        assertFalse(entries[0].logText.contains("\u0000"))
    }

    @Test
    fun testGetLogEntriesContinuationLines() {
        val text = "10-12 22:32:50.100  1000  1000 E Crash: Exception occurred:\n" +
                "    java.lang.RuntimeException: test\n" +
                "        at com.test.Main.run(Main.java:42)\n" +
                "10-12 22:32:51.000  1000  1000 I Normal: Recovered\n"
        val entries = logParser.getLogEntries(text, "main.txt", null)

        assertEquals(2, entries.size)
        assertEquals(
            "10-12 22:32:50.100  1000  1000 E Crash: Exception occurred:" + System.lineSeparator() +
                    "    java.lang.RuntimeException: test" + System.lineSeparator() +
                    "        at com.test.Main.run(Main.java:42)",
            entries[0].logText
        )
        assertEquals(1, entries[0].lineNumber)
        assertEquals(LogLevel.ERROR, entries[0].logLevel)

        assertEquals("10-12 22:32:51.000  1000  1000 I Normal: Recovered", entries[1].logText)
        assertEquals(4, entries[1].lineNumber)
        assertEquals(LogLevel.INFO, entries[1].logLevel)
    }

    @Test
    fun testGetLogEntriesIgnoredLinesFlushesAndSkips() {
        val text = "10-12 22:32:50.100  1000  1000 I Tag: Line before ignore\n" +
                "--------- beginning of system\n" +
                "10-12 22:32:51.000  1000  1000 I Tag: Line after ignore\n"
        val entries = logParser.getLogEntries(text, "main.txt", null)

        assertEquals(2, entries.size)
        assertEquals("10-12 22:32:50.100  1000  1000 I Tag: Line before ignore", entries[0].logText)
        assertEquals("10-12 22:32:51.000  1000  1000 I Tag: Line after ignore", entries[1].logText)
    }

    @Test
    fun testGetLogEntriesBugreportSectionBoundaries() {
        val text = "========================================================\n" +
                "== dumpstate: 2026-09-11 14:27:20\n" +
                "========================================================\n" +
                "Bugreport format version: 2.0\n" +
                "------ SYSTEM LOG (logcat -v threadtime -d *:v) ------\n" +
                "    08-04 19:37:00.810 D Anim: Dumpsys line\n" +
                "        with continuation\n" +
                "------ 0.120s was the duration of 'SYSTEM LOG' ------\n" +
                "Section summary metadata\n" +
                "    08-04 19:37:02.000 D Anim: Next section line\n"
        val entries = logParser.getLogEntries(text, "bugreport.txt", null)

        assertEquals(2, entries.size)
        assertEquals(
            "    08-04 19:37:00.810 D Anim: Dumpsys line" + System.lineSeparator() +
                    "        with continuation",
            entries[0].logText
        )
        assertEquals("    08-04 19:37:02.000 D Anim: Next section line", entries[1].logText)
    }

    @Test
    fun testGetLogEntriesOversizedContinuationTruncation() {
        val hugePayload = buildHugeLogPayload()
        val text = hugePayload + "\n10-12 22:32:55.000  1000  1000 I Tag: Next line after oversized\n"
        val entries = logParser.getLogEntries(text, "main.txt", null)

        assertEquals(2, entries.size)
        assertEquals(LogParser.MAX_LOG_LINE_ALLOWED, entries[0].logText.length)
        assertEquals("10-12 22:32:55.000  1000  1000 I Tag: Next line after oversized", entries[1].logText)
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

    @Test
    fun testIsSorted() {
        val entry1 = LogEntry("line 1", LogLevel.DEBUG, LogTimestamp(10, 12, 10, 0, 0, 0))
        val entry2 = LogEntry("line 2", LogLevel.DEBUG, LogTimestamp(10, 12, 10, 0, 1, 0))
        val entry3 = LogEntry("line 3", LogLevel.DEBUG, LogTimestamp(10, 12, 10, 0, 2, 0))

        assertTrue(logParser.isSorted(emptyArray()))
        assertTrue(logParser.isSorted(arrayOf(entry1)))
        assertTrue(logParser.isSorted(arrayOf(entry1, entry2, entry3)))
        assertFalse(logParser.isSorted(arrayOf(entry2, entry1, entry3)))
        assertFalse(logParser.isSorted(arrayOf(entry3, entry2, entry1)))
    }

    @Test
    fun testParseLogsSingleFileOutOfOrderSortsChronologically() {
        // Simulates bugreport format where SYSTEM logs appear first, followed by earlier EVENT logs
        val bugreportLogs = "10-12 22:35:00.000  1000  1000 I SystemLog: Line later in time\n" +
                "10-12 22:30:00.000  1000  1000 I EventLog: Line earlier in time"

        `when`(reader.availableLogPaths).thenReturn(setOf("bugreport.txt"))
        `when`(reader.get("bugreport.txt")).thenReturn(bugreportLogs)

        val entries = logParser.parseLogs(StandardCharsets.UTF_8)

        assertEquals(2, entries.size)
        assertEquals("10-12 22:30:00.000  1000  1000 I EventLog: Line earlier in time", entries[0].logText)
        assertEquals(0, entries[0].index)
        assertEquals("10-12 22:35:00.000  1000  1000 I SystemLog: Line later in time", entries[1].logText)
        assertEquals(1, entries[1].index)

        val descriptionCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(progressReporter, atLeastOnce()).onProgress(anyInt(), descriptionCaptor.capture())
        assertTrue(descriptionCaptor.allValues.contains("Sorting..."))
    }

    @Test
    fun testParseLogsSingleFileAlreadySortedBypassesSorting() {
        val sortedLogs = "10-12 22:30:00.000  1000  1000 I SystemLog: Line earlier\n" +
                "10-12 22:35:00.000  1000  1000 I SystemLog: Line later"

        `when`(reader.availableLogPaths).thenReturn(setOf("logcat.txt"))
        `when`(reader.get("logcat.txt")).thenReturn(sortedLogs)

        val entries = logParser.parseLogs(StandardCharsets.UTF_8)

        assertEquals(2, entries.size)
        assertEquals("10-12 22:30:00.000  1000  1000 I SystemLog: Line earlier", entries[0].logText)
        assertEquals(0, entries[0].index)
        assertEquals("10-12 22:35:00.000  1000  1000 I SystemLog: Line later", entries[1].logText)
        assertEquals(1, entries[1].index)

        val descriptionCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(progressReporter, atLeastOnce()).onProgress(anyInt(), descriptionCaptor.capture())
        assertFalse(descriptionCaptor.allValues.contains("Sorting..."))
    }
}
