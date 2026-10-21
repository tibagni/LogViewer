package com.tibagni.logviewer.log

import com.tibagni.logviewer.i18n.I18n
import org.junit.Assert.*
import org.junit.Test
import java.time.Duration
import java.util.Locale

class ElapsedTimeHelperTests {

  @Test
  fun testSubsecondDuration() {
    val t1 = LogTimestamp(5, 10, 14, 0, 0, 50)
    val t2 = LogTimestamp(5, 10, 14, 0, 0, 500)

    val result = ElapsedTimeHelper.calculateDuration(t1, t2)
    assertNotNull(result)
    assertFalse(result!!.isNegative)
    assertEquals(450, result.totalMillis)

    assertEquals("450ms", ElapsedTimeHelper.formatBreakdown(result.duration))
    assertEquals("450ms (0.450s)", ElapsedTimeHelper.formatDurationWithSeconds(result))
  }

  @Test
  fun testSecondsDuration() {
    val t1 = LogTimestamp(5, 10, 14, 0, 0, 50)
    val t2 = LogTimestamp(5, 10, 14, 0, 5, 170)

    val result = ElapsedTimeHelper.calculateDuration(t1, t2)
    assertNotNull(result)
    assertFalse(result!!.isNegative)
    assertEquals(5120, result.totalMillis)

    assertEquals("5s 120ms", ElapsedTimeHelper.formatBreakdown(result.duration))
    assertEquals("5s 120ms (5.120s)", ElapsedTimeHelper.formatDurationWithSeconds(result))
  }

  @Test
  fun testMinutesAndSecondsDuration() {
    val t1 = LogTimestamp(5, 15, 14, 15, 0, 50)
    val t2 = LogTimestamp(5, 15, 14, 16, 23, 500)

    val result = ElapsedTimeHelper.calculateDuration(t1, t2)
    assertNotNull(result)
    assertFalse(result!!.isNegative)
    assertEquals(83450, result.totalMillis)

    assertEquals("1m 23s 450ms", ElapsedTimeHelper.formatBreakdown(result.duration))
    assertEquals("1m 23s 450ms (83.450s)", ElapsedTimeHelper.formatDurationWithSeconds(result))
  }

  @Test
  fun testHoursDuration() {
    val t1 = LogTimestamp(5, 15, 14, 15, 0, 0)
    val t2 = LogTimestamp(5, 15, 16, 30, 15, 123)

    val result = ElapsedTimeHelper.calculateDuration(t1, t2)
    assertNotNull(result)
    assertFalse(result!!.isNegative)

    assertEquals("2h 15m 15s 123ms", ElapsedTimeHelper.formatBreakdown(result.duration))
    assertEquals("2h 15m 15s 123ms (8115.123s)", ElapsedTimeHelper.formatDurationWithSeconds(result))
  }

  @Test
  fun testDaysDuration() {
    val t1 = LogTimestamp(5, 10, 10, 0, 0, 0)
    val t2 = LogTimestamp(5, 12, 12, 30, 0, 0)

    val result = ElapsedTimeHelper.calculateDuration(t1, t2)
    assertNotNull(result)
    assertFalse(result!!.isNegative)

    assertEquals("2d 2h 30m 0s 0ms", ElapsedTimeHelper.formatBreakdown(result.duration))
    assertEquals("2d 2h 30m 0s 0ms (181800.000s)", ElapsedTimeHelper.formatDurationWithSeconds(result))
  }

  @Test
  fun testZeroDuration() {
    val t1 = LogTimestamp(5, 10, 14, 0, 0, 100)
    val t2 = LogTimestamp(5, 10, 14, 0, 0, 100)

    val result = ElapsedTimeHelper.calculateDuration(t1, t2)
    assertNotNull(result)
    assertFalse(result!!.isNegative)
    assertEquals(0, result.totalMillis)

    assertEquals("0ms", ElapsedTimeHelper.formatBreakdown(result.duration))
    assertEquals("0ms (0.000s)", ElapsedTimeHelper.formatDurationWithSeconds(result))
  }

  @Test
  fun testNegativeDurationWhenClockJumpedBackward() {
    val t1 = LogTimestamp(5, 15, 16, 30, 0, 0)
    val t2 = LogTimestamp(5, 15, 14, 15, 0, 0)

    val result = ElapsedTimeHelper.calculateDuration(t1, t2)
    assertNotNull(result)
    assertTrue(result!!.isNegative)
    assertEquals(8100000, result.totalMillis)

    assertEquals("2h 15m 0s 0ms", ElapsedTimeHelper.formatBreakdown(result.duration))
    assertEquals("-2h 15m 0s 0ms (-8100.000s)", ElapsedTimeHelper.formatDurationWithSeconds(result))
  }

  @Test
  fun testYearRolloverForward() {
    val t1 = LogTimestamp(12, 31, 23, 59, 50, 0)
    val t2 = LogTimestamp(1, 1, 0, 0, 10, 0)

    val result = ElapsedTimeHelper.calculateDuration(t1, t2)
    assertNotNull(result)
    assertFalse(result!!.isNegative)
    assertEquals(20000, result.totalMillis)

    assertEquals("20s 0ms", ElapsedTimeHelper.formatBreakdown(result.duration))
    assertEquals("20s 0ms (20.000s)", ElapsedTimeHelper.formatDurationWithSeconds(result))
  }

  @Test
  fun testYearRolloverBackward() {
    val t1 = LogTimestamp(1, 1, 0, 0, 10, 0)
    val t2 = LogTimestamp(12, 31, 23, 59, 50, 0)

    val result = ElapsedTimeHelper.calculateDuration(t1, t2)
    assertNotNull(result)
    assertTrue(result!!.isNegative)
    assertEquals(20000, result.totalMillis)

    assertEquals("-20s 0ms (-20.000s)", ElapsedTimeHelper.formatDurationWithSeconds(result))
  }

  @Test
  fun testLeapYearDateSupported() {
    val t1 = LogTimestamp(2, 29, 10, 0, 0, 0)
    val t2 = LogTimestamp(2, 29, 10, 1, 0, 0)

    val result = ElapsedTimeHelper.calculateDuration(t1, t2)
    assertNotNull(result)
    assertEquals(60000, result!!.totalMillis)
    assertEquals("1m 0s 0ms (60.000s)", ElapsedTimeHelper.formatDurationWithSeconds(result))
  }

  @Test
  fun testMicrosecondPrecisionNormalizesCorrectly() {
    // 500000 microseconds = 500 milliseconds
    val t1 = LogTimestamp(5, 10, 12, 0, 0, 0)
    val t2 = LogTimestamp(5, 10, 12, 0, 0, 500000)

    val result = ElapsedTimeHelper.calculateDuration(t1, t2)
    assertNotNull(result)
    assertEquals(500, result!!.totalMillis)
    assertEquals("500ms (0.500s)", ElapsedTimeHelper.formatDurationWithSeconds(result))
  }

  @Test
  fun testCreateSnippetFormatting() {
    val t1 = LogTimestamp(5, 15, 14, 15, 0, 50)
    val t2 = LogTimestamp(5, 15, 14, 16, 23, 500)
    val entry1 = LogEntry("05-15 14:15:00.050 1000 1300 I App: First event", LogLevel.INFO, t1)
    val entry2 = LogEntry("05-15 14:16:23.500 1000 1050 I App: Second event", LogLevel.INFO, t2)

    val result = ElapsedTimeHelper.calculateDuration(t1, t2)!!
    val snippet = ElapsedTimeHelper.createSnippet(entry1, entry2, result)

    val nl = System.lineSeparator()
    val expected = "05-15 14:15:00.050 1000 1300 I App: First event" +
      nl + "... Elapsed Time: 1m 23s 450ms (83.450s) ..." +
      nl + "05-15 14:16:23.500 1000 1050 I App: Second event"

    assertEquals(expected, snippet)
  }

  @Test
  fun testCreateSnippetWithNegativeClockShift() {
    val t1 = LogTimestamp(5, 15, 16, 0, 0, 0)
    val t2 = LogTimestamp(5, 15, 14, 0, 0, 0)
    val entry1 = LogEntry("05-15 16:00:00.000 Event 1", LogLevel.INFO, t1)
    val entry2 = LogEntry("05-15 14:00:00.000 Event 2", LogLevel.INFO, t2)

    val result = ElapsedTimeHelper.calculateDuration(t1, t2)!!
    val snippet = ElapsedTimeHelper.createSnippet(entry1, entry2, result)

    assertTrue(snippet.contains("clock shifted backward"))
    assertTrue(snippet.contains("-2h 0m 0s 0ms (-7200.000s)"))
  }
}
