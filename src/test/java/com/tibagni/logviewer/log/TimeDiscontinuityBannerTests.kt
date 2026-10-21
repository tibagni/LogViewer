package com.tibagni.logviewer.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TimeDiscontinuityBannerTests {

  @Test
  fun testBannerInitialState() {
    val banner = TimeDiscontinuityBanner { null }
    assertFalse(banner.isVisible)
    assertTrue(banner.currentDiscontinuities.isEmpty())
  }

  @Test
  fun testShowSingleDiscontinuity() {
    val banner = TimeDiscontinuityBanner { null }
    val d = TimeDiscontinuity(
      sourceFile = File("/path/to/log.txt"),
      sourceFileName = "log.txt",
      lineNumber = 42,
      previousTimestamp = LogTimestamp(10, 21, 14, 30, 0, 0),
      currentTimestamp = LogTimestamp(10, 21, 13, 0, 0, 0)
    )

    banner.showDiscontinuities(listOf(d))

    assertTrue(banner.isVisible)
    assertEquals(1, banner.currentDiscontinuities.size)
  }

  @Test
  fun testShowMultipleDiscontinuities() {
    val banner = TimeDiscontinuityBanner { null }
    val d1 = TimeDiscontinuity(
      sourceFile = File("/path/to/log1.txt"),
      sourceFileName = "log1.txt",
      lineNumber = 42,
      previousTimestamp = LogTimestamp(10, 21, 14, 30, 0, 0),
      currentTimestamp = LogTimestamp(10, 21, 13, 0, 0, 0)
    )
    val d2 = TimeDiscontinuity(
      sourceFile = File("/path/to/log2.txt"),
      sourceFileName = "log2.txt",
      lineNumber = 88,
      previousTimestamp = LogTimestamp(10, 21, 18, 0, 0, 0),
      currentTimestamp = LogTimestamp(10, 21, 15, 0, 0, 0)
    )

    banner.showDiscontinuities(listOf(d1, d2))

    assertTrue(banner.isVisible)
    assertEquals(2, banner.currentDiscontinuities.size)
  }

  @Test
  fun testShowEmptyListDismisses() {
    val banner = TimeDiscontinuityBanner { null }
    val d = TimeDiscontinuity(
      sourceFile = null,
      sourceFileName = "log.txt",
      lineNumber = 1,
      previousTimestamp = LogTimestamp(10, 21, 14, 0, 0, 0),
      currentTimestamp = LogTimestamp(10, 21, 13, 0, 0, 0)
    )
    banner.showDiscontinuities(listOf(d))
    assertTrue(banner.isVisible)

    banner.showDiscontinuities(emptyList())
    assertFalse(banner.isVisible)
    assertTrue(banner.currentDiscontinuities.isEmpty())
  }

  @Test
  fun testDismissHidesAndClears() {
    val banner = TimeDiscontinuityBanner { null }
    val d = TimeDiscontinuity(
      sourceFile = null,
      sourceFileName = "log.txt",
      lineNumber = 1,
      previousTimestamp = LogTimestamp(10, 21, 14, 0, 0, 0),
      currentTimestamp = LogTimestamp(10, 21, 13, 0, 0, 0)
    )
    banner.showDiscontinuities(listOf(d))
    assertTrue(banner.isVisible)

    banner.dismiss()
    assertFalse(banner.isVisible)
    assertTrue(banner.currentDiscontinuities.isEmpty())
  }
}
