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

        // With 4-digit year, comma subseconds, and colon subseconds
        assertEquals(2071, LogLineParser.findPid("2024-10-04 15:53:01.303  2071  2136 I Tag: Message"))
        assertEquals(2071, LogLineParser.findPid("10-04 15:53:01,303  2071  2136 I Tag: Message"))
        assertEquals(2071, LogLineParser.findPid("10-04 15:53:01:303  2071  2136 I Tag: Message"))

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
        assertEquals(file.absolutePath, info.filePath)
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

    @Test
    fun testParseLineInfoNullAndEmpty() {
        assertNull(LogLineParser.parseLineInfo(null))
        val entryNullText = LogEntry(null, LogLevel.DEBUG, null)
        val infoNullText = LogLineParser.parseLineInfo(entryNullText)
        assertNotNull(infoNullText)
        assertEquals("", infoNullText!!.message)
    }

    @Test
    fun testExtractLogStreamFromBeginningOfLine() {
        assertEquals(LogStream.MAIN, LogLineParser.extractLogStreamFromBeginningOfLine("--------- beginning of main"))
        assertEquals(LogStream.MAIN, LogLineParser.extractLogStreamFromBeginningOfLine("--------- beginning of crash"))
        assertEquals(LogStream.SYSTEM, LogLineParser.extractLogStreamFromBeginningOfLine("--------- beginning of system"))
        assertEquals(LogStream.EVENTS, LogLineParser.extractLogStreamFromBeginningOfLine("--------- beginning of events"))
        assertEquals(LogStream.RADIO, LogLineParser.extractLogStreamFromBeginningOfLine("--------- beginning of radio"))
        assertEquals(LogStream.KERNEL, LogLineParser.extractLogStreamFromBeginningOfLine("--------- beginning of kernel"))
        assertEquals(LogStream.EVENTS, LogLineParser.extractLogStreamFromBeginningOfLine("--------- beginning of /dev/log/events"))
        assertEquals(LogStream.SYSTEM, LogLineParser.extractLogStreamFromBeginningOfLine("--------- beginning of /dev/log/system:"))
        assertNull(LogLineParser.extractLogStreamFromBeginningOfLine("10-12 22:33:46.839  3172  3172 I am_create_activity: test"))
        assertNull(LogLineParser.extractLogStreamFromBeginningOfLine(""))
    }

    @Test
    fun testIsLogLineValidAndInvalid() {
        assertTrue(LogLineParser.isLogLine("10-12 22:32:50.264  2646  2664 I chatty  : uid=1000"))
        assertTrue(LogLineParser.isLogLine("01-06 20:46:42.501 821-1054/? I/WindowState: WIN DEATH"))
        assertTrue(LogLineParser.isLogLine("00-00 00:00:00"))
        assertTrue(LogLineParser.isLogLine("99-99 99:99:99 anything after"))

        assertFalse(LogLineParser.isLogLine("10-12 22:32:5")) // too short (< 14 chars)
        assertFalse(LogLineParser.isLogLine("10/12 22:32:50")) // wrong separator
        assertFalse(LogLineParser.isLogLine("10-12-22:32:50")) // missing space
        assertFalse(LogLineParser.isLogLine("1-12 22:32:50")) // 1-digit month
        assertFalse(LogLineParser.isLogLine("10-1 22:32:50")) // 1-digit day
        assertFalse(LogLineParser.isLogLine(""))

        // Year prefix (YYYY-MM-DD)
        assertTrue(LogLineParser.isLogLine("2026-10-12 22:32:50.264  2646  2664 I chatty  : uid=1000"))

        // Indented lines (rejected by default, allowed when allowLeadingWhitespace is true)
        val indentedLine = "    08-04 19:37:00.810 D AnimatableClockView: animateDoze"
        assertFalse(LogLineParser.isLogLine(indentedLine))
        assertTrue(LogLineParser.isLogLine(indentedLine, allowLeadingWhitespace = true))
        assertTrue(LogLineParser.isLogLine("        09-11 13:53:29.984 ROTATION_0 to ROTATION_90", allowLeadingWhitespace = true))
        assertTrue(LogLineParser.isLogLine("  2026-09-11 14:27:20.123  1000  1000 I Tag: test", allowLeadingWhitespace = true))
        assertTrue(LogLineParser.isLogLine("    08-04 11:36:56:598 AudioService()", allowLeadingWhitespace = true))

        // Stack traces and non-log lines remain false even when allowLeadingWhitespace is true
        assertFalse(LogLineParser.isLogLine("\tat com.android.server.am.ActivityManagerService.startProcess(ActivityManagerService.java:123)", allowLeadingWhitespace = true))
        assertFalse(LogLineParser.isLogLine("--------- beginning of main", allowLeadingWhitespace = true))
        assertFalse(LogLineParser.isLogLine("------ KERNEL LOG ------", allowLeadingWhitespace = true))
    }

    @Test
    fun testFindTimestampStandardAndFallback() {
        val ts = LogLineParser.findTimestamp("10-12 22:32:50.264  2646  2664 I chatty  : message")
        assertNotNull(ts)
        assertEquals(LogTimestamp(10, 12, 22, 32, 50, 264), ts)

        val tsYear = LogLineParser.findTimestamp("2026-10-12 22:32:50.264  2646  2664 I chatty  : message")
        assertNotNull(tsYear)
        assertEquals(LogTimestamp(10, 12, 22, 32, 50, 264), tsYear)

        val tsIndented = LogLineParser.findTimestamp("    08-04 19:37:00.810 D AnimatableClockView: animateDoze")
        assertNotNull(tsIndented)
        assertEquals(LogTimestamp(8, 4, 19, 37, 0, 810), tsIndented)

        val tsColon = LogLineParser.findTimestamp("08-04 11:36:56:598 AudioService()")
        assertNotNull(tsColon)
        assertEquals(LogTimestamp(8, 4, 11, 36, 56, 598), tsColon)

        val tsNoSubseconds = LogLineParser.findTimestamp("10-12 22:32:50")
        assertNotNull(tsNoSubseconds)
        assertEquals(LogTimestamp(10, 12, 22, 32, 50, 0), tsNoSubseconds)

        val tsMicro = LogLineParser.findTimestamp("12-31 23:59:59.999999 1 1 D Test: Microseconds")
        assertNotNull(tsMicro)
        assertEquals(LogTimestamp(12, 31, 23, 59, 59, 999999), tsMicro)

        val tsSingleDigit = LogLineParser.findTimestamp("1-6 20:46:42.501 821-1054/? I/WindowState: single digit month")
        assertNotNull(tsSingleDigit)
        assertEquals(LogTimestamp(1, 6, 20, 46, 42, 501), tsSingleDigit)

        assertNull(LogLineParser.findTimestamp("Not a log line"))
        assertNull(LogLineParser.findTimestamp(""))
    }

    @Test
    fun testFindLogLevel() {
        assertEquals(LogLevel.DEBUG, LogLineParser.findLogLevel("    08-04 19:37:00.810 D AnimatableClockView: animateDoze"))
        assertEquals(LogLevel.INFO, LogLineParser.findLogLevel("2026-10-12 22:32:50.264  2646  2664 I chatty  : message"))
        assertEquals(LogLevel.ERROR, LogLineParser.findLogLevel("10-12 22:32:50.264  2646  2664 F FatalTag: fatal crash"))
        assertEquals(LogLevel.WARNING, LogLineParser.findLogLevel("10-12 22:32:50.264  2646  2664 W WarnTag: warning message"))
        assertEquals(LogLevel.VERBOSE, LogLineParser.findLogLevel("10-12 22:32:50.264  2646  2664 V VerboseTag: verbose message"))

        // Level-less dumpsys lines should default to DEBUG instead of matching letters inside words
        assertEquals(LogLevel.DEBUG, LogLineParser.findLogLevel("        09-11 13:53:29.984 ROTATION_0 to ROTATION_90"))
        assertEquals(LogLevel.DEBUG, LogLineParser.findLogLevel("    08-04 11:36:56:598 FocusRequester: request focus"))
        assertEquals(LogLevel.DEBUG, LogLineParser.findLogLevel("    08-04 11:36:56:598 Event: something happened"))
    }

    @Test
    fun testParseFastTimestampWithYear() {
        val line = "2026-10-29 12:34:56.789  1000  1000 I Tag: test message"
        val ts = LogLineParser.parseFastTimestamp(line)
        assertNotNull(ts)
        assertEquals(LogTimestamp(10, 29, 12, 34, 56, 789), ts)

        val lineWithTabs = "\t2026-01-02 03:04:05.123456 \t 100 100 D Tag: message"
        val tsTabs = LogLineParser.parseFastTimestamp(lineWithTabs)
        assertNotNull(tsTabs)
        assertEquals(LogTimestamp(1, 2, 3, 4, 5, 123456), tsTabs)
    }

    @Test
    fun testParseFastTimestampWithoutYear() {
        val line = "10-29 12:34:56.789  1000  1000 I Tag: test message"
        val ts = LogLineParser.parseFastTimestamp(line)
        assertNotNull(ts)
        assertEquals(LogTimestamp(10, 29, 12, 34, 56, 789), ts)

        val lineLeadingSpaces = "   10-29 12:34:56.789  1000  1000 I Tag: message"
        val tsSpaces = LogLineParser.parseFastTimestamp(lineLeadingSpaces)
        assertNotNull(tsSpaces)
        assertEquals(LogTimestamp(10, 29, 12, 34, 56, 789), tsSpaces)
    }

    @Test
    fun testParseFastTimestampSubsecondSeparators() {
        val dotLine = "10-29 12:34:56.789  1000  1000 I Tag: message"
        assertEquals(
            LogTimestamp(10, 29, 12, 34, 56, 789),
            LogLineParser.parseFastTimestamp(dotLine)
        )

        val commaLine = "10-29 12:34:56,789  1000  1000 I Tag: message"
        assertEquals(
            LogTimestamp(10, 29, 12, 34, 56, 789),
            LogLineParser.parseFastTimestamp(commaLine)
        )

        val colonLine = "10-29 12:34:56:789  1000  1000 I Tag: message"
        assertEquals(
            LogTimestamp(10, 29, 12, 34, 56, 789),
            LogLineParser.parseFastTimestamp(colonLine)
        )

        val semiColonLine = "10-29 12:34:56;789  1000  1000 I Tag: message"
        assertNull(LogLineParser.parseFastTimestamp(semiColonLine))

        val spaceLine = "10-29 12:34:56 789  1000  1000 I Tag: message"
        assertNull(LogLineParser.parseFastTimestamp(spaceLine))
    }

    @Test
    fun testParseFastTimestampSubsecondPrecisionRange() {
        // 3 digits (milliseconds)
        val msLine = "10-29 12:34:56.123  1000  1000 I Tag: message"
        assertEquals(
            LogTimestamp(10, 29, 12, 34, 56, 123),
            LogLineParser.parseFastTimestamp(msLine)
        )

        // 4 digits
        val fourDigitsLine = "10-29 12:34:56.1234  1000  1000 I Tag: message"
        assertEquals(
            LogTimestamp(10, 29, 12, 34, 56, 1234),
            LogLineParser.parseFastTimestamp(fourDigitsLine)
        )

        // 6 digits (microseconds)
        val microLine = "10-29 12:34:56.123456  1000  1000 I Tag: message"
        assertEquals(
            LogTimestamp(10, 29, 12, 34, 56, 123456),
            LogLineParser.parseFastTimestamp(microLine)
        )

        // 9 digits (nanoseconds)
        val nanoLine = "10-29 12:34:56.123456789  1000  1000 I Tag: message"
        assertEquals(
            LogTimestamp(10, 29, 12, 34, 56, 123456789),
            LogLineParser.parseFastTimestamp(nanoLine)
        )

        // Below minimum precision (< 3 digits)
        val twoDigitsLine = "10-29 12:34:56.12  1000  1000 I Tag: message"
        assertNull(LogLineParser.parseFastTimestamp(twoDigitsLine))

        val oneDigitLine = "10-29 12:34:56.1  1000  1000 I Tag: message"
        assertNull(LogLineParser.parseFastTimestamp(oneDigitLine))

        // Above maximum precision (> 9 digits)
        val tenDigitsLine = "10-29 12:34:56.1234567890  1000  1000 I Tag: message"
        assertNull(LogLineParser.parseFastTimestamp(tenDigitsLine))
    }

    @Test
    fun testParseFastTimestampInvalidAndMalformed() {
        assertNull(LogLineParser.parseFastTimestamp(""))
        assertNull(LogLineParser.parseFastTimestamp("   "))
        assertNull(LogLineParser.parseFastTimestamp("10-29 12:34:56"))
        assertNull(LogLineParser.parseFastTimestamp("10-29 12:34"))

        assertNull(LogLineParser.parseFastTimestamp("AA-BB 12:34:56.789"))
        assertNull(LogLineParser.parseFastTimestamp("10/29 12:34:56.789"))
        assertNull(LogLineParser.parseFastTimestamp("202-10-29 12:34:56.789"))

        assertNull(LogLineParser.parseFastTimestamp("10-2912:34:56.789"))

        assertNull(LogLineParser.parseFastTimestamp("10-29 HH:MM:SS.789"))
        assertNull(LogLineParser.parseFastTimestamp("10-29 12-34-56.789"))
        assertNull(LogLineParser.parseFastTimestamp("10-29 12:34.789"))

        assertNull(LogLineParser.parseFastTimestamp("10-29 12:34:56.abc"))
    }

    @Test
    fun testParseDatePrefix() {
        val withYear = "2026-10-29 12:34:56.789"
        val resYear = LogLineParser.parseDatePrefix(withYear, 0)
        assertNotNull(resYear)
        assertEquals(10, resYear!!.month)
        assertEquals(29, resYear.day)
        assertEquals(11, resYear.nextIndex)

        val withoutYear = "10-29 12:34:56.789"
        val resNoYear = LogLineParser.parseDatePrefix(withoutYear, 0)
        assertNotNull(resNoYear)
        assertEquals(10, resNoYear!!.month)
        assertEquals(29, resNoYear.day)
        assertEquals(6, resNoYear.nextIndex)

        // Invalid: missing whitespace after date
        assertNull(LogLineParser.parseDatePrefix("10-2912:34:56.789", 0))

        // Invalid: too short
        assertNull(LogLineParser.parseDatePrefix("10-29", 0))

        // Invalid: non-numeric
        assertNull(LogLineParser.parseDatePrefix("MM-DD 12:34:56.789", 0))
    }

    @Test
    fun testParseTime() {
        val line = "10-29 12:34:56.789"
        val res = LogLineParser.parseTime(line, 6)
        assertNotNull(res)
        assertEquals(12, res!!.hour)
        assertEquals(34, res.minutes)
        assertEquals(56, res.seconds)
        assertEquals(14, res.subsecondIndex)

        // Invalid: too short (< 12 chars from startIndex)
        assertNull(LogLineParser.parseTime("12:34:56", 0))

        // Invalid: wrong separator in time
        assertNull(LogLineParser.parseTime("12-34-56.789", 0))
    }

    @Test
    fun testParseSubseconds() {
        // Dot separator
        assertEquals(789, LogLineParser.parseSubseconds(".789", 0))

        // Comma separator
        assertEquals(789, LogLineParser.parseSubseconds(",789", 0))

        // Colon separator
        assertEquals(789, LogLineParser.parseSubseconds(":789", 0))

        // 9-digit subsecond (nanoseconds)
        assertEquals(123456789, LogLineParser.parseSubseconds(".123456789", 0))

        // > 9 digits is invalid
        assertNull(LogLineParser.parseSubseconds(".1234567890", 0))

        // < 3 digits is invalid
        assertNull(LogLineParser.parseSubseconds(".12", 0))

        // Invalid separator
        assertNull(LogLineParser.parseSubseconds(";789", 0))
    }
}
