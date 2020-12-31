package com.tibagni.logviewer.view

import java.awt.Color
import java.awt.RenderingHints
import java.awt.Graphics2D
import java.awt.Graphics
import javax.swing.JTextField

class HintTextField(private val _hint: String) : JTextField() {
  override fun paint(g: Graphics) {
    super.paint(g)
    if (text.isEmpty()) {
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