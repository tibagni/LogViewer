package com.tibagni.logviewer.view.color

import com.tibagni.logviewer.theme.LogViewerThemeManager
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.awt.Color
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.image.BufferedImage

/**
 * Unit tests for [ModernColorChooserPanel] verifying color synchronization,
 * hex parsing, numeric updates, and headless compatibility.
 */
class ModernColorChooserPanelTests {

  /**
   * Verifies that the initial color sets up the internal HSB state and preview.
   */
  @Test
  fun testInitialColorConfigured() {
    assumeFalse(GraphicsEnvironment.isHeadless())
    val panel = ModernColorChooserPanel(
      initialColor = Color.BLUE,
      themeManager = LogViewerThemeManager
    )

    assertEquals(Color.BLUE, panel.color)
    assertEquals(Color.BLUE, panel.previewLabel.foreground)
    assertEquals("#0000FF", panel.hexField.text)
  }

  /**
   * Verifies that setting color updates property, callback, and preview label.
   */
  @Test
  fun testColorChangeInvokesCallback() {
    assumeFalse(GraphicsEnvironment.isHeadless())
    var updatedColor: Color? = null
    val panel = ModernColorChooserPanel(
      initialColor = Color.BLACK,
      themeManager = LogViewerThemeManager
    )
    panel.onColorChanged = { updatedColor = it }

    panel.color = Color.MAGENTA
    assertEquals(Color.MAGENTA, panel.color)
    assertEquals(Color.MAGENTA, updatedColor)
    assertEquals(Color.MAGENTA, panel.previewLabel.foreground)
  }

  /**
   * Verifies that entering valid 6-char hex updates color and numeric spinners.
   */
  @Test
  fun testHexInputUpdatesColorAndFields() {
    assumeFalse(GraphicsEnvironment.isHeadless())
    val panel = ModernColorChooserPanel(
      initialColor = Color.BLACK,
      themeManager = LogViewerThemeManager
    )

    panel.hexField.text = "#00FF00"
    panel.hexField.postActionEvent()

    assertEquals(Color.GREEN, panel.color)
    assertEquals(0, panel.rSpinner.value)
    assertEquals(255, panel.gSpinner.value)
    assertEquals(0, panel.bSpinner.value)
  }

  /**
   * Verifies that entering invalid hex reverts back to current color's hex.
   */
  @Test
  fun testInvalidHexRevertsToCurrentColor() {
    assumeFalse(GraphicsEnvironment.isHeadless())
    val panel = ModernColorChooserPanel(
      initialColor = Color.RED,
      themeManager = LogViewerThemeManager
    )

    panel.hexField.text = "invalid"
    panel.hexField.postActionEvent()

    assertEquals("#FF0000", panel.hexField.text)
    assertEquals(Color.RED, panel.color)
  }

  /**
   * Verifies that changing RGB numeric spinners recalculates color and HSB.
   */
  @Test
  fun testRgbSpinnersUpdateColorAndHsb() {
    assumeFalse(GraphicsEnvironment.isHeadless())
    val panel = ModernColorChooserPanel(
      initialColor = Color.BLACK,
      themeManager = LogViewerThemeManager
    )

    panel.rSpinner.value = 255
    panel.gSpinner.value = 255
    panel.bSpinner.value = 0

    assertEquals(Color.YELLOW, panel.color)
    assertEquals("#FFFF00", panel.hexField.text)
    assertEquals(60, panel.hSpinner.value)
    assertEquals(100, panel.sSpinner.value)
  }

  /**
   * Verifies that changing HSB numeric spinners recalculates color and RGB.
   */
  @Test
  fun testHsbSpinnersUpdateColorAndRgb() {
    assumeFalse(GraphicsEnvironment.isHeadless())
    val panel = ModernColorChooserPanel(
      initialColor = Color.BLACK,
      themeManager = LogViewerThemeManager
    )

    panel.hSpinner.value = 180 // Cyan hue
    panel.sSpinner.value = 100
    panel.vSpinner.value = 100

    assertEquals(Color.CYAN, panel.color)
    assertEquals(0, panel.rSpinner.value)
    assertEquals(255, panel.gSpinner.value)
    assertEquals(255, panel.bSpinner.value)
  }

  /**
   * Verifies that setting color to white or black preserves the selected hue.
   */
  @Test
  fun testGrayscaleColorPreservesHue() {
    assumeFalse(GraphicsEnvironment.isHeadless())
    val panel = ModernColorChooserPanel(
      initialColor = Color.CYAN,
      themeManager = LogViewerThemeManager
    )
    val origHue = panel.hue

    panel.color = Color.WHITE
    assertEquals(origHue, panel.hue, 0.001f)

    panel.color = Color.BLACK
    assertEquals(origHue, panel.hue, 0.001f)
  }

  /**
   * Verifies that clicking mini swatch buttons selects the preset color.
   */
  @Test
  fun testMiniSwatchClickUpdatesColor() {
    assumeFalse(GraphicsEnvironment.isHeadless())
    val panel = ModernColorChooserPanel(
      initialColor = Color.BLACK,
      themeManager = LogViewerThemeManager
    )

    val targetSwatch = panel.swatchButtons[2]
    val targetColor = targetSwatch.swatchColor
    targetSwatch.onClick()

    assertEquals(targetColor, panel.color)
  }

  /**
   * Verifies that previewLabel enforces a minimum uncropped height.
   */
  @Test
  fun testPreviewLabelHasUncroppedMinimumHeight() {
    assumeFalse(GraphicsEnvironment.isHeadless())
    val panel = ModernColorChooserPanel(
      initialColor = Color.GREEN,
      themeManager = LogViewerThemeManager
    )

    assertTrue(panel.previewLabel.preferredSize.height >= 40)
  }

  /**
   * Verifies that painting with zero dimensions does not throw exceptions.
   */
  @Test
  fun testZeroDimensionPaintingDoesNotThrow() {
    assumeFalse(GraphicsEnvironment.isHeadless())
    val panel = ModernColorChooserPanel(
      initialColor = Color.RED,
      themeManager = LogViewerThemeManager
    )
    panel.size = Dimension(0, 0)

    val img = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
    val g = img.createGraphics()
    try {
      panel.paint(g)
    } finally {
      g.dispose()
    }
  }

  /**
   * Verifies that refreshing presets on theme change does not throw.
   */
  @Test
  fun testOnThemeChangedDoesNotThrow() {
    assumeFalse(GraphicsEnvironment.isHeadless())
    val panel = ModernColorChooserPanel(
      initialColor = Color.CYAN,
      themeManager = LogViewerThemeManager
    )

    panel.onThemeChanged()
    assertEquals(Color.CYAN, panel.color)
  }

  /**
   * Verifies installation as an AbstractColorChooserPanel inside JColorChooser.
   */
  @Test
  fun testInstallationInJColorChooser() {
    assumeFalse(GraphicsEnvironment.isHeadless())
    val chooser = javax.swing.JColorChooser()
    val panel = ModernColorChooserPanel()
    val existing = chooser.chooserPanels
    chooser.chooserPanels = arrayOf(panel, *existing)

    assertEquals("Modern", panel.displayName)
    assertNotNull(panel.smallDisplayIcon)
    assertEquals(panel, chooser.chooserPanels[0])
    assertTrue(chooser.chooserPanels.size > 1)
  }

  /**
   * Verifies that updating chooser color synchronizes panel's visual controls.
   */
  @Test
  fun testModelColorChangeUpdatesVisualControls() {
    assumeFalse(GraphicsEnvironment.isHeadless())
    val chooser = javax.swing.JColorChooser()
    val panel = ModernColorChooserPanel()
    val existing = chooser.chooserPanels
    chooser.chooserPanels = arrayOf(panel, *existing)

    chooser.color = Color.MAGENTA
    assertEquals(Color.MAGENTA, panel.color)
    assertEquals("#FF00FF", panel.hexField.text)
    assertEquals(255, panel.rSpinner.value)
    assertEquals(0, panel.gSpinner.value)
    assertEquals(255, panel.bSpinner.value)
  }
}
