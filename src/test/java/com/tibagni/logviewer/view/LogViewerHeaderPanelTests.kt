package com.tibagni.logviewer.view

import com.tibagni.logviewer.i18n.I18n
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.GraphicsEnvironment
import java.awt.image.BufferedImage

/**
 * Unit tests for [LogViewerHeaderPanel] covering path updates, visibility toggles,
 * stats badge updates, reset behaviors, and headless painting.
 */
class LogViewerHeaderPanelTests {

  /**
   * Verifies that updatePath sets the label text, icon, and makes the panel visible.
   */
  @Test
  fun testUpdatePathSetsTextAndShowsPanel() {
    if (GraphicsEnvironment.isHeadless()) return

    val panel = LogViewerHeaderPanel()
    assertFalse(panel.isVisible)

    panel.updatePath("/var/log/syslog")

    assertTrue(panel.isVisible)
    assertEquals("/var/log/syslog", panel.displayedPathText)
    assertNotNull(panel.displayedPathIcon)
  }

  /**
   * Verifies that updatePath with null or empty path hides the panel and resets text.
   */
  @Test
  fun testUpdatePathNullOrEmptyHidesPanel() {
    if (GraphicsEnvironment.isHeadless()) return

    val panel = LogViewerHeaderPanel()
    panel.updatePath("/var/log/syslog")
    assertTrue(panel.isVisible)

    panel.updatePath(null)
    assertFalse(panel.isVisible)
    assertEquals("", panel.displayedPathText)

    panel.updatePath("/var/log/syslog")
    assertTrue(panel.isVisible)

    panel.updatePath("")
    assertFalse(panel.isVisible)
    assertEquals("", panel.displayedPathText)
  }

  /**
   * Verifies that updatePath truncates long text when availableWidth is small.
   */
  @Test
  fun testUpdatePathTruncatesWhenNarrow() {
    if (GraphicsEnvironment.isHeadless()) return

    val panel = LogViewerHeaderPanel()
    val longPath = "/very/long/path/to/some/deeply/nested/directory/structure/and/a/big_android_log_file.txt"
    panel.updatePath(longPath, availableWidth = 80)

    assertTrue(panel.isVisible)
    assertNotNull(panel.displayedPathText)
    assertTrue("Should truncate text", panel.displayedPathText!!.length < longPath.length)
  }

  /**
   * Verifies that updateStats delegates count values to the internal status badge.
   */
  @Test
  fun testUpdateStatsDelegatesToStatusBadge() {
    if (GraphicsEnvironment.isHeadless()) return

    val panel = LogViewerHeaderPanel()
    assertFalse(panel.isBadgeVisible)

    panel.updateStats(100, 25)

    assertTrue(panel.isBadgeVisible)
    assertEquals(I18n.format(I18n.LOGS_STATUS_FILTERED, "25", "100", 25), panel.badgeText)

    panel.updateStats(100, 0)
    assertTrue(panel.isBadgeVisible)
    assertEquals(I18n.format(I18n.LOGS_STATUS_ALL, "100"), panel.badgeText)

    panel.updateStats(0, 0)
    assertFalse(panel.isBadgeVisible)
  }

  /**
   * Verifies that reset clears the path label, resets the badge stats, and hides the panel.
   */
  @Test
  fun testResetClearsPathAndHidesPanel() {
    if (GraphicsEnvironment.isHeadless()) return

    val panel = LogViewerHeaderPanel()
    panel.updatePath("/var/log/syslog")
    panel.updateStats(50, 10)
    assertTrue(panel.isVisible)
    assertTrue(panel.isBadgeVisible)

    panel.reset()

    assertFalse(panel.isVisible)
    assertNull(panel.displayedPathText)
    assertNull(panel.displayedPathIcon)
    assertFalse(panel.isBadgeVisible)
  }

  /**
   * Verifies that paintComponent renders background in headless graphics without error.
   */
  @Test
  fun testPaintComponentRendersBackground() {
    if (GraphicsEnvironment.isHeadless()) return

    val panel = LogViewerHeaderPanel()
    panel.setSize(200, 30)

    val image = BufferedImage(200, 30, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    try {
      panel.paint(g)
    } finally {
      g.dispose()
    }

    var hasPaintedPixel = false
    for (x in 0 until 200) {
      for (y in 0 until 30) {
        if (((image.getRGB(x, y) ushr 24) and 0xFF) > 0) {
          hasPaintedPixel = true
          break
        }
      }
      if (hasPaintedPixel) break
    }
    assertTrue("Header panel should paint background pixels", hasPaintedPixel)
  }

  /**
   * Verifies that onThemeChanged updates component borders and icon without errors.
   */
  @Test
  fun testOnThemeChangedUpdatesAppearance() {
    if (GraphicsEnvironment.isHeadless()) return

    val panel = LogViewerHeaderPanel()
    panel.updatePath("/var/log/syslog")
    assertNotNull(panel.displayedPathIcon)

    panel.onThemeChanged()

    assertNotNull(panel.border)
    assertNotNull(panel.displayedPathIcon)
  }

  /**
   * Verifies that updatePath sets rawPath and toolTipText on the label, and configures hand cursor.
   */
  @Test
  fun testUpdatePathSetsTooltipAndCursor() {
    if (GraphicsEnvironment.isHeadless()) return

    val panel = LogViewerHeaderPanel()
    val path = "/var/log/syslog"
    panel.updatePath(path)

    assertEquals(path, panel.rawPath)
    assertEquals(path, panel.displayedToolTipText)
    assertEquals(java.awt.Cursor.HAND_CURSOR, panel.displayedCursor.type)
  }

  /**
   * Verifies that reset clears rawPath and toolTipText.
   */
  @Test
  fun testResetClearsRawPathAndTooltip() {
    if (GraphicsEnvironment.isHeadless()) return

    val panel = LogViewerHeaderPanel()
    panel.updatePath("/var/log/syslog")
    assertEquals("/var/log/syslog", panel.rawPath)

    panel.reset()
    assertNull(panel.rawPath)
    assertNull(panel.displayedToolTipText)
  }
}
