package com.tibagni.logviewer.view

import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.FlatLightLaf
import org.junit.Assert.assertEquals
import org.junit.Test
import java.awt.image.BufferedImage
import javax.swing.UIManager
import javax.swing.plaf.metal.MetalLookAndFeel

/**
 * Unit tests for [HintTextField].
 */
class HintTextFieldTests {

  /**
   * Verifies that [HintTextField] sets the FlatLaf client properties for placeholder text
   * and the clear button upon initialization.
   */
  @Test
  fun testHintTextFieldClientProperties() {
    val hint = "Search log entries..."
    val textField = HintTextField(hint)

    assertEquals(hint, textField.getClientProperty(FlatClientProperties.PLACEHOLDER_TEXT))
    assertEquals(true, textField.getClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON))
  }

  /**
   * Verifies that [HintTextField] renders properly via fallback painting when non-FlatLaf
   * is active, and does not throw any exceptions when painting.
   */
  @Test
  fun testHintTextFieldFallbackPainting() {
    val originalLaf = UIManager.getLookAndFeel()
    try {
      UIManager.setLookAndFeel(MetalLookAndFeel())
      val textField = HintTextField("Search hint")
      textField.setSize(200, 30)

      val image = BufferedImage(200, 30, BufferedImage.TYPE_INT_ARGB)
      val g = image.createGraphics()
      try {
        // Exercise the custom fallback hint painting path when text is empty
        textField.paint(g)

        // Exercise painting when text is populated
        textField.text = "Filtered text"
        textField.paint(g)
      } finally {
        g.dispose()
      }

      // Exercise painting when FlatLaf is active
      UIManager.setLookAndFeel(FlatLightLaf())
      val flatField = HintTextField("Flat hint")
      flatField.setSize(200, 30)
      val flatImage = BufferedImage(200, 30, BufferedImage.TYPE_INT_ARGB)
      val flatG = flatImage.createGraphics()
      try {
        flatField.paint(flatG)
        flatField.text = "Flat text"
        flatField.paint(flatG)
      } finally {
        flatG.dispose()
      }
    } finally {
      UIManager.setLookAndFeel(originalLaf)
    }
  }
}
