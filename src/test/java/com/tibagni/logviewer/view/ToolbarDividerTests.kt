package com.tibagni.logviewer.view

import com.tibagni.logviewer.util.scaling.UIScaleUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Color
import java.awt.Dimension
import java.awt.image.BufferedImage
import javax.swing.JComponent
import javax.swing.UIManager

/**
 * Unit tests verifying [ToolbarDivider] dimensions, headless rendering,
 * and look-and-feel color fallbacks.
 */
class ToolbarDividerTests {

  @Test
  fun testExtendsJComponent() {
    val divider = ToolbarDivider()
    assertEquals(JComponent::class.java, divider.javaClass.superclass)
  }

  @Test
  fun testNotOpaqueByDefault() {
    val divider = ToolbarDivider()
    assertFalse(divider.isOpaque)
  }

  @Test
  fun testDefaultDimensions() {
    val divider = ToolbarDivider()
    val expectedPref = Dimension(
      UIScaleUtils.dip(ToolbarDivider.DEFAULT_WIDTH_DP),
      UIScaleUtils.dip(ToolbarDivider.DEFAULT_HEIGHT_DP)
    )
    val expectedMin = Dimension(
      UIScaleUtils.dip(ToolbarDivider.DEFAULT_WIDTH_DP),
      UIScaleUtils.dip(ToolbarDivider.DEFAULT_LINE_HEIGHT_DP)
    )
    val expectedMax = Dimension(
      UIScaleUtils.dip(ToolbarDivider.DEFAULT_WIDTH_DP),
      UIScaleUtils.dip(ToolbarDivider.DEFAULT_HEIGHT_DP)
    )

    assertEquals(expectedPref, divider.preferredSize)
    assertEquals(expectedMin, divider.minimumSize)
    assertEquals(expectedMax, divider.maximumSize)
  }

  @Test
  fun testCustomDimensions() {
    val divider = ToolbarDivider(widthDp = 10, heightDp = 24, lineHeightDp = 18)
    assertEquals(
      Dimension(UIScaleUtils.dip(10), UIScaleUtils.dip(24)),
      divider.preferredSize
    )
    assertEquals(
      Dimension(UIScaleUtils.dip(10), UIScaleUtils.dip(18)),
      divider.minimumSize
    )
    assertEquals(
      Dimension(UIScaleUtils.dip(10), UIScaleUtils.dip(24)),
      divider.maximumSize
    )
  }

  @Test
  fun testDividerColorResolution() {
    val divider = ToolbarDivider()
    val defaultColor = divider.getDividerColor()
    assertNotNull(defaultColor)

    val originalSep = UIManager.get("Separator.foreground")
    val originalBorder = UIManager.get("Component.borderColor")
    try {
      UIManager.put("Separator.foreground", Color.MAGENTA)
      assertEquals(Color.MAGENTA, divider.getDividerColor())

      UIManager.put("Separator.foreground", null)
      UIManager.put("Component.borderColor", Color.CYAN)
      // When separator is not explicitly configured, borderColor is preferred
      val resolvedBorder = divider.getDividerColor()
      assertNotNull(resolvedBorder)
    } finally {
      UIManager.put("Separator.foreground", originalSep)
      UIManager.put("Component.borderColor", originalBorder)
    }
  }

  @Test
  fun testPaintComponentHeadlessRendering() {
    val divider = ToolbarDivider()
    val w = UIScaleUtils.dip(7)
    val h = UIScaleUtils.dip(20)
    divider.setSize(w, h)

    val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    try {
      divider.paintComponent(g)
    } finally {
      g.dispose()
    }

    // Verify center column has non-zero alpha pixels drawn
    val midX = w / 2
    var hasPaintedPixel = false
    for (y in 0 until h) {
      val argb = image.getRGB(midX, y)
      val alpha = (argb ushr 24) and 0xFF
      if (alpha > 0) {
        hasPaintedPixel = true
        break
      }
    }
    assertTrue("Divider line should paint visible pixels", hasPaintedPixel)
  }

  @Test
  fun testPaintComponentZeroSizeDoesNotThrow() {
    val divider = ToolbarDivider()
    divider.setSize(0, 0)

    val image = BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    try {
      divider.paintComponent(g)
    } finally {
      g.dispose()
    }
  }

  @Test
  fun testUpdateUIDoesNotThrow() {
    val divider = ToolbarDivider()
    divider.updateUI()
    assertNotNull(divider.preferredSize)
  }
}
