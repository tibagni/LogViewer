package com.tibagni.logviewer.log.parser

import com.tibagni.logviewer.ProgressReporter
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

        assertEquals(expected, actual)
        assertEquals(expected2, actual2)
    }

    @Test
    fun testParseLogs() {
        val testLogLine = "10-12 22:32:50.264  2646  2664 I test  : Test log Test Log"
        val logNames = setOf("main", "radio", "system", "events")

        val expectedLogs = Array(4) { testLogLine }

        `when`(reader.availableLogsNames).thenReturn(logNames)
        `when`(reader.get(ArgumentMatchers.any())).thenReturn(testLogLine)

        val entries = logParser.parseLogs()

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
    fun testParseInvalidLogs() {
        val testLogLine = buildHugeLogPayload()
        val logNames = setOf("bugreport")

        `when`(reader.availableLogsNames).thenReturn(logNames)
        `when`(reader.get(ArgumentMatchers.any())).thenReturn(testLogLine)

        logParser.parseLogs()

        assertEquals(1, logParser.logsSkipped.size)
        assertEquals("bugreport", logParser.logsSkipped[0])
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
        `when`(reader.availableLogsNames).thenReturn(setOf(
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
        `when`(reader.availableLogsNames).thenReturn(setOf(
                "main.txt",
                "aplogd-m.txt")
        )

        assertEquals(setOf(LogStream.MAIN), logParser.availableStreams)
    }

    @Test
    fun testAvailableLogStreamsSYSTEM() {
        `when`(reader.availableLogsNames).thenReturn(setOf(
                "system.txt",
                "aplogd-s.txt")
        )

        assertEquals(setOf(LogStream.SYSTEM), logParser.availableStreams)
    }

    @Test
    fun testAvailableLogStreamsRADIO() {
        `when`(reader.availableLogsNames).thenReturn(setOf(
                "radio.txt",
                "aplogd-r.txt")
        )

        assertEquals(setOf(LogStream.RADIO), logParser.availableStreams)
    }

    @Test
    fun testAvailableLogStreamsEVENTS() {
        `when`(reader.availableLogsNames).thenReturn(setOf(
                "events.txt",
                "aplogd-e.txt")
        )

        assertEquals(setOf(LogStream.EVENTS), logParser.availableStreams)
    }

    @Test
    fun testAvailableLogStreamsALL() {
        `when`(reader.availableLogsNames).thenReturn(setOf(
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
        `when`(reader.availableLogsNames).thenReturn(setOf(
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
