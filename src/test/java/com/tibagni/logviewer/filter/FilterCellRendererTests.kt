package com.tibagni.logviewer.filter

import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.log.LogStream
import com.tibagni.logviewer.theme.LogViewerThemeManager
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Color
import java.awt.font.TextAttribute
import java.awt.image.BufferedImage
import java.util.UUID
import java.util.function.Supplier
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.UIManager

/**
 * Unit tests for [FilterCellRenderer] verifying row border padding,
 * context count badge visibility, search text highlighting, selection,
 * typography decorations, hairline divider visibility, color resolution,
 * and headless rendering.
 */
class FilterCellRendererTests {

  /**
   * Verifies that row border is preserved across [FilterCellRenderer.updateUI].
   */
  @Test
  fun testBorderPreservedOnUpdateUI() {
    val renderer = FilterCellRenderer()
    val initialBorder = renderer.border
    assertNotNull(initialBorder)

    renderer.updateUI()
    assertEquals(initialBorder, renderer.border)
  }

  /**
   * Verifies row padding insets match modern 3dp vertical / 6dp horizontal.
   */
  @Test
  fun testRowPaddingInsets() {
    val renderer = FilterCellRenderer()
    val insets = renderer.border.getBorderInsets(renderer)
    assertEquals(UIScaleUtils.dip(3), insets.top)
    assertEquals(UIScaleUtils.dip(3), insets.bottom)
    assertEquals(UIScaleUtils.dip(6), insets.left)
    assertEquals(UIScaleUtils.dip(6), insets.right)

    renderer.updateUI()
    val updatedInsets = renderer.border.getBorderInsets(renderer)
    assertEquals(UIScaleUtils.dip(3), updatedInsets.top)
    assertEquals(UIScaleUtils.dip(3), updatedInsets.bottom)
    assertEquals(UIScaleUtils.dip(6), updatedInsets.left)
    assertEquals(UIScaleUtils.dip(6), updatedInsets.right)
  }

  /**
   * Verifies filter text and verbosity initial render colors on theme change.
   */
  @Test
  fun testRendererComponentOnThemeChange() {
    LogViewerThemeManager.currentTheme = "Light"
    val renderer = FilterCellRenderer()
    val list = JList<Filter>()
    val model = DefaultListModel<Filter>()
    val filter = Filter("TestFilter", "pattern", Color.RED, LogLevel.DEBUG)
    model.addElement(filter)
    list.model = model

    val compLight = renderer.getListCellRendererComponent(
      list, filter, 0, false, false
    ) as FilterCellRenderer
    assertTrue(compLight.text.contains("TestFilter"))
    val hex = LogViewerThemeManager.textForegroundHex
    assertTrue(compLight.text.contains("color=\"$hex\""))

    LogViewerThemeManager.currentTheme = "Dark"
    val compDark = renderer.getListCellRendererComponent(
      list, filter, 0, false, false
    ) as FilterCellRenderer
    val darkHex = LogViewerThemeManager.textForegroundHex
    assertTrue(compDark.text.contains("color=\"$darkHex\""))
  }

  /**
   * Verifies that line count badge is visible when context info is present.
   */
  @Test
  fun testCountBadgeVisibleWhenContextInfoPresent() {
    val tag = FilterContextTag.register("testContext-" + UUID.randomUUID())
    val renderer = FilterCellRenderer(Supplier { tag })
    val list = JList<Filter>()
    val filter = Filter(
      "ActivityFilter",
      "ActivityManager",
      Color.BLUE,
      LogLevel.INFO
    )
    val contextInfo = filter.initContext(tag)
    contextInfo.incrementLineCount(LogStream.MAIN)
    contextInfo.incrementLineCount(LogStream.MAIN)
    contextInfo.incrementLineCount(LogStream.SYSTEM)

    val comp = renderer.getListCellRendererComponent(
      list, filter, 0, false, false
    ) as FilterCellRenderer
    assertTrue(comp.countBadge.isVisible)
    assertEquals("3", comp.countBadge.text)
  }

  /**
   * Verifies that count badge is hidden when no context info is available.
   */
  @Test
  fun testCountBadgeHiddenWhenContextInfoAbsent() {
    val tag = FilterContextTag.register("testContext-" + UUID.randomUUID())
    val renderer = FilterCellRenderer(Supplier { tag })
    val list = JList<Filter>()
    val filter = Filter(
      "ActivityFilter",
      "ActivityManager",
      Color.BLUE,
      LogLevel.INFO
    )

    val comp = renderer.getListCellRendererComponent(
      list, filter, 0, false, false
    ) as FilterCellRenderer
    assertFalse(comp.countBadge.isVisible)
    assertEquals("", comp.countBadge.text)
  }

  /**
   * Verifies that matching search text is highlighted with HTML formatting.
   */
  @Test
  fun testHighlightedSearchText() {
    val renderer = FilterCellRenderer()
    renderer.highlightedText = "Activity"
    val list = JList<Filter>()
    val filter = Filter(
      "ActivityManager",
      "ActivityManager",
      Color.BLUE,
      LogLevel.INFO
    )

    val comp = renderer.getListCellRendererComponent(
      list, filter, 0, false, false
    ) as FilterCellRenderer
    val expectedHighlight =
      "<span style=\"background-color:yellow;color:black\">Activity</span>"
    assertTrue(comp.text.contains(expectedHighlight))
  }

  /**
   * Verifies that selected rows use a translucent selection background.
   */
  @Test
  fun testSelectionBackground() {
    val renderer = FilterCellRenderer()
    val list = JList<Filter>()
    list.selectionBackground = Color(0, 120, 215)
    list.background = Color.WHITE
    val filter = Filter("Name", "Name", Color.BLACK, LogLevel.VERBOSE)

    val unselectedComp = renderer.getListCellRendererComponent(
      list, filter, 0, false, false
    ) as FilterCellRenderer
    assertEquals(Color.WHITE, unselectedComp.background)

    val selectedComp = renderer.getListCellRendererComponent(
      list, filter, 0, true, false
    ) as FilterCellRenderer
    assertNotEquals(Color.WHITE, selectedComp.background)
  }

  /**
   * Verifies custom names are underlined while pattern names are not.
   */
  @Test
  fun testUnderlineForCustomFilterName() {
    val renderer = FilterCellRenderer()
    val list = JList<Filter>()
    val filterCustomName = Filter(
      "Custom Name",
      "ActualPattern",
      Color.BLACK,
      LogLevel.VERBOSE
    )
    val compCustom = renderer.getListCellRendererComponent(
      list, filterCustomName, 0, false, false
    ) as FilterCellRenderer
    assertEquals(
      TextAttribute.UNDERLINE_ON,
      compCustom.checkBox.font.attributes[TextAttribute.UNDERLINE]
    )

    val filterSameName = Filter(
      "SamePattern",
      "SamePattern",
      Color.BLACK,
      LogLevel.VERBOSE
    )
    val compSame = renderer.getListCellRendererComponent(
      list, filterSameName, 0, false, false
    ) as FilterCellRenderer
    assertNull(compSame.checkBox.font.attributes[TextAttribute.UNDERLINE])
  }

  /**
   * Verifies that when a list has multiple filters, intermediate rows have
   * [FilterCellRenderer.showDivider] set to true and the last row has it set
   * to false.
   */
  @Test
  fun testDividerVisibilityBetweenFilters() {
    val renderer = FilterCellRenderer()
    val list = JList<Filter>()
    val model = DefaultListModel<Filter>()
    val f1 = Filter("Filter1", "p1", Color.RED, LogLevel.DEBUG)
    val f2 = Filter("Filter2", "p2", Color.GREEN, LogLevel.INFO)
    val f3 = Filter("Filter3", "p3", Color.BLUE, LogLevel.VERBOSE)
    model.addElement(f1)
    model.addElement(f2)
    model.addElement(f3)
    list.model = model

    renderer.getListCellRendererComponent(list, f1, 0, false, false)
    assertTrue("First item should show divider", renderer.showDivider)

    renderer.getListCellRendererComponent(list, f2, 1, false, false)
    assertTrue("Second item should show divider", renderer.showDivider)

    renderer.getListCellRendererComponent(list, f3, 2, false, false)
    assertFalse("Last item should not show divider", renderer.showDivider)
  }

  /**
   * Verifies that a list with only a single filter row does not render
   * a bottom divider line.
   */
  @Test
  fun testDividerVisibilitySingleFilter() {
    val renderer = FilterCellRenderer()
    val list = JList<Filter>()
    val model = DefaultListModel<Filter>()
    val filter = Filter("SoloFilter", "p", Color.RED, LogLevel.DEBUG)
    model.addElement(filter)
    list.model = model

    renderer.getListCellRendererComponent(list, filter, 0, false, false)
    assertFalse("Single item list must not show divider", renderer.showDivider)
  }

  /**
   * Verifies that [FilterCellRenderer.resolveDividerColor] respects theme
   * Component.borderColor, Separator.foreground, and theme-aware fallbacks.
   */
  @Test
  fun testDividerColorResolution() {
    val renderer = FilterCellRenderer()
    val origBorder = UIManager.get("Component.borderColor")
    val origSep = UIManager.get("Separator.foreground")
    try {
      // 1. Translucent Component.borderColor takes precedence directly
      val translucentBorder = Color(100, 110, 120, 80)
      UIManager.put("Component.borderColor", translucentBorder)
      UIManager.put("Separator.foreground", Color.MAGENTA)
      assertEquals(translucentBorder, renderer.resolveDividerColor())

      // 2. Opaque Component.borderColor is softened to alpha 35
      val opaqueBorder = Color(100, 110, 120)
      UIManager.put("Component.borderColor", opaqueBorder)
      val expectedSoftenedBorder = Color(100, 110, 120, 35)
      assertEquals(expectedSoftenedBorder, renderer.resolveDividerColor())

      // 3. Clear Component.borderColor override to test Separator.foreground
      UIManager.put("Component.borderColor", "")

      val translucentSep = Color(200, 210, 220, 60)
      UIManager.put("Separator.foreground", translucentSep)
      assertEquals(translucentSep, renderer.resolveDividerColor())

      val opaqueSep = Color(200, 210, 220)
      UIManager.put("Separator.foreground", opaqueSep)
      val expectedSoftenedSep = Color(200, 210, 220, 35)
      assertEquals(expectedSoftenedSep, renderer.resolveDividerColor())

      // 4. Clear Separator.foreground to test theme fallbacks
      UIManager.put("Separator.foreground", "")

      LogViewerThemeManager.currentTheme = "Light"
      assertEquals(Color(0, 0, 0, 20), renderer.resolveDividerColor())

      LogViewerThemeManager.currentTheme = "Dark"
      assertEquals(Color(255, 255, 255, 24), renderer.resolveDividerColor())
    } finally {
      UIManager.put("Component.borderColor", origBorder)
      UIManager.put("Separator.foreground", origSep)
    }
  }

  /**
   * Verifies off-screen painting onto a [BufferedImage] in headless mode,
   * confirming that bottom divider pixels are drawn within modern insets when
   * showDivider is true and omitted when showDivider is false.
   */
  @Test
  fun testDividerPaintingHeadless() {
    val renderer = FilterCellRenderer()
    renderer.isOpaque = false
    val width = 100
    val height = 30
    renderer.setSize(width, height)

    // With showDivider = false, bottom row remains unpainted (alpha == 0)
    renderer.showDivider = false
    val imgWithoutDivider = BufferedImage(
      width,
      height,
      BufferedImage.TYPE_INT_ARGB
    )
    val g1 = imgWithoutDivider.createGraphics()
    try {
      renderer.paint(g1)
    } finally {
      g1.dispose()
    }
    val alphaWithout = (imgWithoutDivider.getRGB(
      width / 2,
      height - 1
    ) ushr 24) and 0xFF
    assertEquals(0, alphaWithout)

    // With showDivider = true, hairline divider is painted within insets
    renderer.showDivider = true
    val imgWithDivider = BufferedImage(
      width,
      height,
      BufferedImage.TYPE_INT_ARGB
    )
    val g2 = imgWithDivider.createGraphics()
    try {
      renderer.paint(g2)
    } finally {
      g2.dispose()
    }
    val alphaCenter = (imgWithDivider.getRGB(
      width / 2,
      height - 1
    ) ushr 24) and 0xFF
    assertTrue(
      "Divider line at y = height - 1 must be painted at center",
      alphaCenter > 0
    )

    // Edge margin is inset (at x = 2, divider is not drawn)
    val alphaMargin = (imgWithDivider.getRGB(
      2,
      height - 1
    ) ushr 24) and 0xFF
    assertEquals("Divider should not reach outer card margin", 0, alphaMargin)

    // Narrow width safety: when width is smaller than 2 * inset, skips line
    renderer.setSize(10, 30)
    val narrowImg = BufferedImage(10, 30, BufferedImage.TYPE_INT_ARGB)
    val narrowG = narrowImg.createGraphics()
    try {
      renderer.paint(narrowG)
    } finally {
      narrowG.dispose()
    }

    // Zero-size safety: paint should not throw when dimensions are zero
    renderer.setSize(0, 0)
    val zeroImg = BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB)
    val zeroG = zeroImg.createGraphics()
    try {
      renderer.paint(zeroG)
    } finally {
      zeroG.dispose()
    }
  }
}
