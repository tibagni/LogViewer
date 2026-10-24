package com.tibagni.logviewer.view

import com.tibagni.logviewer.LogViewerViewImpl
import com.tibagni.logviewer.MainView
import com.tibagni.logviewer.filter.FiltersPanel
import com.tibagni.logviewer.filter.FiltersPanelHost
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.live.LiveLogPresenter
import com.tibagni.logviewer.live.LiveLogViewImpl
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.awt.Color
import java.awt.Frame
import java.awt.image.BufferedImage
import java.text.NumberFormat
import javax.swing.JLabel
import javax.swing.UIManager

/**
 * Unit tests verifying [StatusBadge] presentation, line count formatting,
 * percentage calculations, look-and-feel color fallbacks, headless rendering,
 * and integration with [LogViewerViewImpl].
 */
class StatusBadgeTests {

  /**
   * Verifies that a newly created badge extends [JLabel], is transparent,
   * and starts in a hidden state with empty text.
   */
  @Test
  fun testExtendsJLabelAndInitiallyHidden() {
    val badge = StatusBadge()
    assertEquals(JLabel::class.java, badge.javaClass.superclass)
    assertFalse(badge.isOpaque)
    assertFalse(badge.isVisible)
    assertEquals("", badge.text)
  }

  /**
   * Verifies that passing zero or negative total lines hides the badge
   * and clears its displayed text.
   */
  @Test
  fun testZeroLinesHidesBadge() {
    val badge = StatusBadge()
    badge.updateStats(totalLines = 100, filteredLines = 20)
    assertTrue(badge.isVisible)

    badge.updateStats(totalLines = 0, filteredLines = 0)
    assertFalse(badge.isVisible)
    assertEquals("", badge.text)

    badge.updateStats(totalLines = 0, filteredLines = 15)
    assertFalse(badge.isVisible)
    assertEquals("", badge.text)

    badge.updateStats(totalLines = -5, filteredLines = -1)
    assertFalse(badge.isVisible)
    assertEquals("", badge.text)
  }

  /**
   * Verifies line count formatting when total lines are positive and no
   * active filters are applied (filtered count <= 0).
   */
  @Test
  fun testAllLinesNoFilters() {
    val badge = StatusBadge()
    val numberFormat = NumberFormat.getIntegerInstance()

    badge.updateStats(totalLines = 100, filteredLines = 0)
    assertTrue(badge.isVisible)
    assertEquals(
      I18n.format(I18n.LOGS_STATUS_ALL, numberFormat.format(100)),
      badge.text
    )

    badge.updateStats(totalLines = 100, filteredLines = -1)
    assertTrue(badge.isVisible)
    assertEquals(
      I18n.format(I18n.LOGS_STATUS_ALL, numberFormat.format(100)),
      badge.text
    )

    badge.updateStats(totalLines = 25000, filteredLines = 0)
    assertTrue(badge.isVisible)
    val expected = I18n.format(I18n.LOGS_STATUS_ALL, numberFormat.format(25000))
    assertEquals(expected, badge.text)
  }

  /**
   * Verifies line count formatting when lines are dropped and no active
   * filters are applied.
   */
  @Test
  fun testAllLinesWithDropped() {
    val badge = StatusBadge()
    val numberFormat = NumberFormat.getIntegerInstance()

    badge.updateStats(totalLines = 100, filteredLines = 0, droppedLines = 12)
    assertTrue(badge.isVisible)
    assertEquals(
      I18n.format(
        I18n.LOGS_STATUS_ALL_DROPPED,
        numberFormat.format(100),
        numberFormat.format(12)
      ),
      badge.text
    )

    badge.updateStats(
      totalLines = 25000,
      filteredLines = -1,
      droppedLines = 500
    )
    assertTrue(badge.isVisible)
    assertEquals(
      I18n.format(
        I18n.LOGS_STATUS_ALL_DROPPED,
        numberFormat.format(25000),
        numberFormat.format(500)
      ),
      badge.text
    )
  }

  /**
   * Verifies line count formatting, percentage calculation, and dropped count
   * when active filters match a subset of lines and dropped lines > 0.
   */
  @Test
  fun testFilteredLinesWithDropped() {
    val badge = StatusBadge()
    val numberFormat = NumberFormat.getIntegerInstance()

    badge.updateStats(totalLines = 100, filteredLines = 25, droppedLines = 10)
    assertTrue(badge.isVisible)
    assertEquals(
      I18n.format(
        I18n.LOGS_STATUS_FILTERED_DROPPED,
        numberFormat.format(25),
        numberFormat.format(100),
        25,
        numberFormat.format(10)
      ),
      badge.text
    )

    badge.updateStats(totalLines = 200, filteredLines = 50, droppedLines = 1)
    assertTrue(badge.isVisible)
    assertEquals(
      I18n.format(
        I18n.LOGS_STATUS_FILTERED_DROPPED,
        numberFormat.format(50),
        numberFormat.format(200),
        25,
        numberFormat.format(1)
      ),
      badge.text
    )
  }

  /**
   * Verifies line count formatting and percentage calculation when active
   * filters match a subset of loaded log lines.
   */
  @Test
  fun testFilteredLinesWithPercentageCalculation() {
    val badge = StatusBadge()
    val numberFormat = NumberFormat.getIntegerInstance()

    badge.updateStats(totalLines = 100, filteredLines = 25)
    assertTrue(badge.isVisible)
    assertEquals(
      I18n.format(
        I18n.LOGS_STATUS_FILTERED,
        numberFormat.format(25),
        numberFormat.format(100),
        25
      ),
      badge.text
    )

    badge.updateStats(totalLines = 200, filteredLines = 50)
    assertTrue(badge.isVisible)
    assertEquals(
      I18n.format(
        I18n.LOGS_STATUS_FILTERED,
        numberFormat.format(50),
        numberFormat.format(200),
        25
      ),
      badge.text
    )

    badge.updateStats(totalLines = 300, filteredLines = 100)
    assertTrue(badge.isVisible)
    assertEquals(
      I18n.format(
        I18n.LOGS_STATUS_FILTERED,
        numberFormat.format(100),
        numberFormat.format(300),
        33
      ),
      badge.text
    )

    badge.updateStats(totalLines = 1000, filteredLines = 1000)
    assertTrue(badge.isVisible)
    assertEquals(
      I18n.format(
        I18n.LOGS_STATUS_FILTERED,
        numberFormat.format(1000),
        numberFormat.format(1000),
        100
      ),
      badge.text
    )

    badge.updateStats(totalLines = 10, filteredLines = 1)
    assertTrue(badge.isVisible)
    assertEquals(
      I18n.format(
        I18n.LOGS_STATUS_FILTERED,
        numberFormat.format(1),
        numberFormat.format(10),
        10
      ),
      badge.text
    )
  }

  /**
   * Verifies state transitions across the full lifecycle of loading logs,
   * applying filters, clearing filters, and closing logs.
   */
  @Test
  fun testStatsTransitionLifecycle() {
    val badge = StatusBadge()
    val numberFormat = NumberFormat.getIntegerInstance()

    assertFalse(badge.isVisible)

    badge.updateStats(totalLines = 500, filteredLines = 0)
    assertTrue(badge.isVisible)
    assertEquals(
      I18n.format(I18n.LOGS_STATUS_ALL, numberFormat.format(500)),
      badge.text
    )

    badge.updateStats(totalLines = 500, filteredLines = 100)
    assertTrue(badge.isVisible)
    assertEquals(
      I18n.format(
        I18n.LOGS_STATUS_FILTERED,
        numberFormat.format(100),
        numberFormat.format(500),
        20
      ),
      badge.text
    )

    badge.updateStats(totalLines = 500, filteredLines = 0)
    assertTrue(badge.isVisible)
    assertEquals(
      I18n.format(I18n.LOGS_STATUS_ALL, numberFormat.format(500)),
      badge.text
    )

    badge.updateStats(totalLines = 0, filteredLines = 0)
    assertFalse(badge.isVisible)
    assertEquals("", badge.text)
  }

  /**
   * Verifies theme color resolution and fallback behavior for background,
   * border, text foreground, and corner arc.
   */
  @Test
  fun testDynamicThemeColorsAndArc() {
    val badge = StatusBadge()

    assertNotNull(badge.resolveBadgeBackground())
    assertNotNull(badge.resolveBorderColor())
    assertNotNull(badge.resolveBadgeForeground())
    assertTrue(badge.resolveArc() > 0)

    val origBtnBg = UIManager.get("Button.background")
    val origBorder = UIManager.get("Component.borderColor")
    val origArc = UIManager.get("Component.arc")
    try {
      UIManager.put("Button.background", Color.ORANGE)
      assertEquals(Color.ORANGE, badge.resolveBadgeBackground())

      UIManager.put("Component.borderColor", Color.CYAN)
      assertEquals(Color.CYAN, badge.resolveBorderColor())

      UIManager.put("Component.arc", 16)
      assertEquals(UIScaleUtils.dip(16), badge.resolveArc())
    } finally {
      UIManager.put("Button.background", origBtnBg)
      UIManager.put("Component.borderColor", origBorder)
      UIManager.put("Component.arc", origArc)
    }
  }

  /**
   * Verifies that [StatusBadge.updateUI] executes without exceptions
   * and refreshes borders and foreground colors.
   */
  @Test
  fun testUpdateUiDoesNotThrow() {
    val badge = StatusBadge()
    badge.updateUI()
    assertNotNull(badge.border)
    assertNotNull(badge.foreground)
  }

  /**
   * Verifies headless painting onto a [BufferedImage], ensuring non-zero
   * alpha pixels are rendered for the pill background and border stroke.
   */
  @Test
  fun testHeadlessPaintingAndDimensions() {
    val badge = StatusBadge()
    badge.updateStats(totalLines = 100, filteredLines = 25)

    val w = UIScaleUtils.dip(120)
    val h = UIScaleUtils.dip(24)
    badge.setSize(w, h)

    val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    try {
      badge.paintComponent(g)
    } finally {
      g.dispose()
    }

    var hasPaintedPixel = false
    val midX = w / 2
    for (y in 0 until h) {
      val argb = image.getRGB(midX, y)
      val alpha = (argb ushr 24) and 0xFF
      if (alpha > 0) {
        hasPaintedPixel = true
        break
      }
    }
    assertTrue(
      "Badge should paint visible pixels in headless graphics",
      hasPaintedPixel
    )
  }

  /**
   * Verifies that painting a zero-sized component does not throw exceptions.
   */
  @Test
  fun testPaintComponentZeroSizeDoesNotThrow() {
    val badge = StatusBadge()
    badge.setSize(0, 0)

    val image = BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    try {
      badge.paintComponent(g)
    } finally {
      g.dispose()
    }
  }

  /**
   * Verifies integration with [LogViewerViewImpl], ensuring that
   * [LogViewerViewImpl.statusBadge] updates appropriately on
   * [LogViewerViewImpl.showLogs], [LogViewerViewImpl.showFilteredLogs],
   * and [LogViewerViewImpl.showCurrentLogsLocation], and that
   * [LogViewerViewImpl.headerPanel] and [LogViewerViewImpl.currentLogsLbl]
   * toggle visibility and icon correctly when logs open and close.
   */
  @Test
  fun testLogViewerViewIntegration() {
    val mockMain = mock(MainView::class.java)
    val mockHost = mock(FiltersPanelHost::class.java)
    val mockFrame = mock(Frame::class.java)
    `when`(mockHost.parentWindow).thenReturn(mockFrame)

    val filtersPanel = FiltersPanel(mockHost)
    val logView = LogViewerViewImpl(mockMain, filtersPanel, emptySet())
    val badge = logView.statusBadge
    val headerPanel = logView.headerPanel
    val currentLogsLbl = logView.currentLogsLbl

    assertNotNull(badge)
    assertNotNull(headerPanel)
    assertNotNull(currentLogsLbl)
    assertFalse(badge.isVisible)
    assertFalse(headerPanel.isVisible)

    val entry1 = LogEntry("Line 1", LogLevel.INFO, null)
    val entry2 = LogEntry("Line 2", LogLevel.ERROR, null)

    logView.showLogs(listOf(entry1, entry2))
    assertTrue(badge.isVisible)
    assertEquals(I18n.format(I18n.LOGS_STATUS_ALL, "2"), badge.text)

    logView.showFilteredLogs(listOf(entry1))
    assertTrue(badge.isVisible)
    assertEquals(
      I18n.format(I18n.LOGS_STATUS_FILTERED, "1", "2", 50),
      badge.text
    )

    logView.showFilteredLogs(emptyList())
    assertTrue(badge.isVisible)
    assertEquals(I18n.format(I18n.LOGS_STATUS_ALL, "2"), badge.text)

    // Verify opening logs location updates headerPanel and currentLogsLbl icon
    logView.showCurrentLogsLocation("/path/to/android_logs.txt")
    assertTrue(headerPanel.isVisible)
    assertNotNull(currentLogsLbl.icon)
    assertEquals(16, currentLogsLbl.icon.iconWidth)
    assertEquals(16, currentLogsLbl.icon.iconHeight)
    assertEquals("/path/to/android_logs.txt", currentLogsLbl.text)

    // Verify closing logs clears icon, text, and hides header panel
    logView.showCurrentLogsLocation(null)
    assertFalse(badge.isVisible)
    assertEquals("", badge.text)
    assertFalse(headerPanel.isVisible)
    assertNull(currentLogsLbl.icon)
    assertNull(currentLogsLbl.text)

    // Reopening logs restores icon and visibility
    logView.showCurrentLogsLocation("/another/path/to/log.txt")
    assertTrue(headerPanel.isVisible)
    assertNotNull(currentLogsLbl.icon)
    assertEquals("/another/path/to/log.txt", currentLogsLbl.text)
  }

  /**
   * Verifies that [LogViewerViewImpl.headerPanel] paints correctly
   * in headless mode without throwing exceptions.
   */
  @Test
  fun testHeaderPanelPaintInHeadlessMode() {
    val mockMain = mock(MainView::class.java)
    val mockHost = mock(FiltersPanelHost::class.java)
    val mockFrame = mock(Frame::class.java)
    `when`(mockHost.parentWindow).thenReturn(mockFrame)

    val filtersPanel = FiltersPanel(mockHost)
    val logView = LogViewerViewImpl(mockMain, filtersPanel, emptySet())
    val headerPanel = logView.headerPanel
    headerPanel.setSize(200, 30)

    val image = BufferedImage(200, 30, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    try {
      headerPanel.paint(g)
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
    assertTrue(
      "Header panel should paint background in headless graphics",
      hasPaintedPixel
    )
  }

  /**
   * Verifies that [LiveLogViewImpl.statusBadge] updates properly on
   * [LiveLogViewImpl.showStats].
   */
  @Test
  fun testIntegrationWithLiveLogView() {
    val mockPresenter = mock(LiveLogPresenter::class.java)
    val liveLogView = LiveLogViewImpl { mockPresenter }
    val badge = liveLogView.statusBadge

    assertNotNull(badge)
    assertFalse(badge.isVisible)

    // When allCount == 0, statusBadge is hidden
    liveLogView.showStats(
      totalReceived = 0,
      droppedCount = 0,
      allCount = 0,
      filteredCount = 0
    )
    assertFalse(badge.isVisible)
    assertEquals("", badge.text)

    // When allCount == 100, filteredCount == 0, statusBadge displays 100 lines
    liveLogView.showStats(
      totalReceived = 100,
      droppedCount = 0,
      allCount = 100,
      filteredCount = 0
    )
    assertTrue(badge.isVisible)
    assertEquals("100 lines", badge.text)
    assertEquals(I18n.format(I18n.LOGS_STATUS_ALL, "100"), badge.text)

    // When allCount == 100, filteredCount == 25, statusBadge displays 25 of 100
    liveLogView.showStats(
      totalReceived = 100,
      droppedCount = 0,
      allCount = 100,
      filteredCount = 25
    )
    assertTrue(badge.isVisible)
    assertEquals("25 of 100 lines (25%)", badge.text)
    assertEquals(
      I18n.format(I18n.LOGS_STATUS_FILTERED, "25", "100", 25),
      badge.text
    )

    // When droppedCount > 0 and filteredCount == 0
    liveLogView.showStats(
      totalReceived = 120,
      droppedCount = 20,
      allCount = 100,
      filteredCount = 0
    )
    assertTrue(badge.isVisible)
    assertEquals(
      I18n.format(I18n.LOGS_STATUS_ALL_DROPPED, "100", "20"),
      badge.text
    )

    // When droppedCount > 0 and filteredCount == 25
    liveLogView.showStats(
      totalReceived = 120,
      droppedCount = 20,
      allCount = 100,
      filteredCount = 25
    )
    assertTrue(badge.isVisible)
    assertEquals(
      I18n.format(
        I18n.LOGS_STATUS_FILTERED_DROPPED,
        "25",
        "100",
        25,
        "20"
      ),
      badge.text
    )

    // When allCount == 0 again, statusBadge is hidden
    liveLogView.showStats(
      totalReceived = 0,
      droppedCount = 0,
      allCount = 0,
      filteredCount = 0
    )
    assertFalse(badge.isVisible)
    assertEquals("", badge.text)
  }

  @Test
  fun testUpdateCount() {
    val badge = StatusBadge()
    badge.updateCount(12345)
    assertTrue(badge.isVisible)
    assertEquals(NumberFormat.getIntegerInstance().format(12345), badge.text)

    badge.updateCount(0)
    assertTrue(badge.isVisible)
    assertEquals("0", badge.text)
  }

  @Test
  fun testUpdateResult() {
    val badge = StatusBadge()

    badge.updateResult(currentMatch = 3, totalMatches = 10)
    assertTrue(badge.isVisible)
    assertEquals("3/10", badge.text)

    badge.updateResult(currentMatch = 0, totalMatches = 5)
    assertTrue(badge.isVisible)
    assertEquals(I18n.format(I18n.SEARCH_RESULTS_COUNT, 5), badge.text)

    badge.updateResult(
      currentMatch = 0,
      totalMatches = 0,
      targetVariant = StatusBadge.Variant.NO_RESULTS
    )
    assertTrue(badge.isVisible)
    assertEquals(I18n.format(I18n.SEARCH_RESULTS_COUNT, 0), badge.text)

    badge.updateResult(
      currentMatch = 0,
      totalMatches = 0,
      targetVariant = StatusBadge.Variant.ERROR
    )
    assertTrue(badge.isVisible)
    assertEquals(I18n.get(I18n.SEARCH_BAD_PATTERN), badge.text)

    badge.updateResult(
      currentMatch = 0,
      totalMatches = 0,
      customMessage = "Custom Msg"
    )
    assertTrue(badge.isVisible)
    assertEquals("Custom Msg", badge.text)
  }

  @Test
  fun testClear() {
    val badge = StatusBadge()
    badge.updateCount(42)
    assertTrue(badge.isVisible)
    assertEquals("42", badge.text)

    badge.clear()
    assertFalse(badge.isVisible)
    assertEquals("", badge.text)
    assertEquals(StatusBadge.Variant.DEFAULT, badge.variant)
  }

  @Test
  fun testVariantColors() {
    val badge = StatusBadge()
    val defaultBg = badge.resolveBadgeBackground(StatusBadge.Variant.DEFAULT)
    val errorBg = badge.resolveBadgeBackground(StatusBadge.Variant.ERROR)
    val warningBg = badge.resolveBadgeBackground(StatusBadge.Variant.WARNING)
    val mutedBg = badge.resolveBadgeBackground(StatusBadge.Variant.MUTED)
    val elevatedBg = badge.resolveBadgeBackground(StatusBadge.Variant.ELEVATED)

    assertNotNull(defaultBg)
    assertNotNull(errorBg)
    assertNotNull(warningBg)
    assertNotNull(mutedBg)
    assertNotNull(elevatedBg)
    assertNotEquals(defaultBg, errorBg)

    badge.variant = StatusBadge.Variant.WARNING
    assertEquals(StatusBadge.Variant.WARNING, badge.variant)
  }

  /**
   * Verifies that [StatusBadge.Variant.ELEVATED] configures default 16dp arc
   * and 8dp/16dp vertical/horizontal padding.
   */
  @Test
  fun testElevatedVariantDefaults() {
    val badge = StatusBadge(StatusBadge.Variant.ELEVATED)
    assertEquals(StatusBadge.Variant.ELEVATED, badge.variant)
    assertEquals(16, badge.arcDp)
    assertEquals(16, badge.paddingHorizontalDp)
    assertEquals(8, badge.paddingVerticalDp)

    val insets = badge.border.getBorderInsets(badge)
    assertEquals(UIScaleUtils.dip(8), insets.top)
    assertEquals(UIScaleUtils.dip(8), insets.bottom)
    assertEquals(UIScaleUtils.dip(16), insets.left)
    assertEquals(UIScaleUtils.dip(16), insets.right)
  }

  /**
   * Verifies that [StatusBadge.Variant.SURFACE] is an alias for ELEVATED.
   */
  @Test
  fun testVariantElevatedSurfaceAlias() {
    assertEquals(StatusBadge.Variant.ELEVATED, StatusBadge.Variant.SURFACE)
    val badge = StatusBadge(StatusBadge.Variant.SURFACE)
    assertEquals(StatusBadge.Variant.ELEVATED, badge.variant)
    assertEquals(16, badge.arcDp)
    assertEquals(16, badge.paddingHorizontalDp)
    assertEquals(8, badge.paddingVerticalDp)
  }

  /**
   * Verifies that transitioning to and from ELEVATED variant adjusts padding
   * and arc radius dynamically.
   */
  @Test
  fun testVariantTransitionToElevatedUpdatesPaddingAndArc() {
    val badge = StatusBadge(StatusBadge.Variant.DEFAULT)
    assertEquals(8, badge.arcDp)
    assertEquals(8, badge.paddingHorizontalDp)
    assertEquals(2, badge.paddingVerticalDp)

    badge.variant = StatusBadge.Variant.ELEVATED
    assertEquals(16, badge.arcDp)
    assertEquals(16, badge.paddingHorizontalDp)
    assertEquals(8, badge.paddingVerticalDp)

    badge.variant = StatusBadge.Variant.DEFAULT
    assertEquals(8, badge.arcDp)
    assertEquals(8, badge.paddingHorizontalDp)
    assertEquals(2, badge.paddingVerticalDp)
  }

  /**
   * Verifies background and foreground color resolution for ELEVATED variant
   * in both light and dark modes.
   */
  @Test
  fun testElevatedVariantColorsInLightAndDarkMode() {
    val badge = StatusBadge(StatusBadge.Variant.ELEVATED)

    // Background
    val lightBg = badge.resolveBadgeBackground(
      StatusBadge.Variant.ELEVATED,
      isDark = false
    )
    val darkBg = badge.resolveBadgeBackground(
      StatusBadge.Variant.ELEVATED,
      isDark = true
    )
    assertEquals(Color(255, 255, 255), lightBg)
    assertEquals(Color(48, 50, 56), darkBg)

    // Foreground
    val lightFg = badge.resolveBadgeForeground(
      StatusBadge.Variant.ELEVATED,
      isDark = false
    )
    val darkFg = badge.resolveBadgeForeground(
      StatusBadge.Variant.ELEVATED,
      isDark = true
    )
    assertEquals(Color(33, 37, 41), lightFg)
    assertEquals(Color(245, 245, 245), darkFg)
  }

  /**
   * Verifies border color resolution for ELEVATED variant honoring theme
   * Component.borderColor or fallback values.
   */
  @Test
  fun testElevatedVariantBorderColorRespectsThemeOrFallback() {
    val badge = StatusBadge(StatusBadge.Variant.ELEVATED)
    val origBorder = UIManager.get("Component.borderColor")
    val origDefaultsBorder = UIManager.getDefaults()["Component.borderColor"]
    try {
      UIManager.put("Component.borderColor", null)
      UIManager.getDefaults().remove("Component.borderColor")
      val lightBorder = badge.resolveBorderColor(
        StatusBadge.Variant.ELEVATED,
        isDark = false
      )
      val darkBorder = badge.resolveBorderColor(
        StatusBadge.Variant.ELEVATED,
        isDark = true
      )
      assertEquals(Color(0, 0, 0, 45), lightBorder)
      assertEquals(Color(255, 255, 255, 36), darkBorder)

      val customColor = Color(12, 34, 56)
      UIManager.put("Component.borderColor", customColor)
      assertEquals(
        customColor,
        badge.resolveBorderColor(StatusBadge.Variant.ELEVATED, isDark = false)
      )
      assertEquals(
        customColor,
        badge.resolveBorderColor(StatusBadge.Variant.ELEVATED, isDark = true)
      )
    } finally {
      UIManager.put("Component.borderColor", origBorder)
      if (origDefaultsBorder != null) {
        UIManager.getDefaults()["Component.borderColor"] = origDefaultsBorder
      }
    }
  }

  /**
   * Verifies that ELEVATED variant paints cleanly onto an off-screen buffer
   * without headless exceptions.
   */
  @Test
  fun testElevatedVariantHeadlessPainting() {
    val badge = StatusBadge(StatusBadge.Variant.ELEVATED)
    badge.text = "Toast Message"
    val w = UIScaleUtils.dip(150)
    val h = UIScaleUtils.dip(36)
    badge.setSize(w, h)

    val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    try {
      badge.paintComponent(g)
    } finally {
      g.dispose()
    }

    var hasPaintedPixel = false
    val midX = w / 2
    for (y in 0 until h) {
      val argb = image.getRGB(midX, y)
      val alpha = (argb ushr 24) and 0xFF
      if (alpha > 0) {
        hasPaintedPixel = true
        break
      }
    }
    assertTrue(hasPaintedPixel)
  }
}
