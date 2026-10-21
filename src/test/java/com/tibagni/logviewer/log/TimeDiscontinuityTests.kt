package com.tibagni.logviewer.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TimeDiscontinuityTests {

  @Test
  fun testFormatDisplay() {
    val t = LogTimestamp(10, 21, 14, 30, 45, 12)
    assertEquals("10-21 14:30:45", t.formatDisplay())
  }

  @Test
  fun testYearRollover() {
    val prev = LogTimestamp(12, 31, 23, 59, 59, 99)
    val curr = LogTimestamp(1, 1, 0, 0, 0, 1)
    val discontinuity = TimeDiscontinuity(
      sourceFile = File("/path/to/log.txt"),
      sourceFileName = "log.txt",
      lineNumber = 100,
      previousTimestamp = prev,
      currentTimestamp = curr
    )

    assertTrue(discontinuity.isYearRollover)
    assertEquals("Year rollover", discontinuity.formatDelta())
  }

  @Test
  fun testFormatDeltaDifferentSeconds() {
    val prev = LogTimestamp(5, 10, 12, 0, 45, 0)
    val curr = LogTimestamp(5, 10, 12, 0, 10, 0)
    val discontinuity = TimeDiscontinuity(
      sourceFile = null,
      sourceFileName = "test.log",
      lineNumber = 50,
      previousTimestamp = prev,
      currentTimestamp = curr
    )

    assertFalse(discontinuity.isYearRollover)
    assertEquals("-35s", discontinuity.formatDelta())
  }

  @Test
  fun testFormatDeltaHoursMinutesSeconds() {
    val prev = LogTimestamp(5, 10, 14, 35, 10, 0)
    val curr = LogTimestamp(5, 10, 12, 10, 5, 0)
    val discontinuity = TimeDiscontinuity(
      sourceFile = null,
      sourceFileName = "test.log",
      lineNumber = 120,
      previousTimestamp = prev,
      currentTimestamp = curr
    )

    assertEquals("-2h 25m 5s", discontinuity.formatDelta())
  }

  @Test
  fun testFormatDeltaDaysAndHours() {
    val prev = LogTimestamp(5, 15, 12, 0, 0, 0)
    val curr = LogTimestamp(5, 13, 10, 0, 0, 0)
    val discontinuity = TimeDiscontinuity(
      sourceFile = null,
      sourceFileName = "test.log",
      lineNumber = 300,
      previousTimestamp = prev,
      currentTimestamp = curr
    )

    assertEquals("-2d 2h", discontinuity.formatDelta())
  }

  @Test
  fun testFormatDeltaDifferentMonths() {
    val prev = LogTimestamp(5, 1, 12, 0, 0, 0)
    val curr = LogTimestamp(3, 1, 12, 0, 0, 0)
    val discontinuity = TimeDiscontinuity(
      sourceFile = null,
      sourceFileName = "test.log",
      lineNumber = 5,
      previousTimestamp = prev,
      currentTimestamp = curr
    )

    assertEquals("-2mo", discontinuity.formatDelta())
  }
}
