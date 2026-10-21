package com.tibagni.logviewer.view

import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.FlatLaf
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JTextField
import javax.swing.UIManager

/**
 * A customized [JTextField] that displays a placeholder hint when the text field is empty.
 *
 * Configures FlatLaf client properties [FlatClientProperties.PLACEHOLDER_TEXT] and
 * [FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON] for native FlatLaf integration.
 * If FlatLaf is not the active look and feel, falls back to custom painting of the hint string.
 *
 * @param _hint the placeholder text to display when the field is empty
 */
class HintTextField(private val _hint: String) : JTextField() {

  init {
    putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, _hint)
    putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true)
  }

  /**
   * Custom paint implementation that renders the placeholder text if the text field
   * is currently empty and the active look and feel is not [FlatLaf].
   *
   * @param g the [Graphics] context used for painting
   */
  override fun paint(g: Graphics) {
    super.paint(g)
    if (text.isEmpty() && !(UIManager.getLookAndFeel() is FlatLaf)) {
      val h = height
      (g as Graphics2D).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
      val fm = g.getFontMetrics()
      val c0 = background.rgb
      val c1 = foreground.rgb
      val m = -0x1010102
      val c2 = (c0 and m ushr 1) + (c1 and m ushr 1)
      g.setColor(Color(c2, true))
      g.drawString(_hint, insets.left, h / 2 + fm.ascent / 2 - 2)
    }
  }
}
