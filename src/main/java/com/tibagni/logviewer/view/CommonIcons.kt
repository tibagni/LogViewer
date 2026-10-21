package com.tibagni.logviewer.view

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import javax.swing.ImageIcon

object CommonIcons {
  private const val ICON_SIZE = 25

  fun playIcon(): ImageIcon = createIcon { g2d, size, color ->
    g2d.color = color
    val path = Path2D.Double()
    val padding = size * 0.25
    path.moveTo(padding + 1, padding)
    path.lineTo(size - padding + 1, size / 2.0)
    path.lineTo(padding + 1, size - padding)
    path.closePath()
    g2d.fill(path)
  }

  fun stopIcon(): ImageIcon = createIcon { g2d, size, color ->
    g2d.color = color
    val pad = (size * 0.28).toInt()
    val arc = 3
    g2d.fillRoundRect(pad, pad, size - 2 * pad, size - 2 * pad, arc, arc)
  }

  fun clearIcon(): ImageIcon = createIcon { g2d, size, color ->
    g2d.color = color
    g2d.stroke = BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    val r = size * 0.32
    val cx = size / 2.0
    val cy = size / 2.0
    g2d.draw(Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r))
    // Diagonal slash
    val slashOffset = r * 0.7071
    g2d.draw(Line2D.Double(cx - slashOffset, cy - slashOffset, cx + slashOffset, cy + slashOffset))
  }

  fun autoScrollIcon(): ImageIcon = createIcon { g2d, size, color ->
    g2d.color = color
    g2d.stroke = BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    val cx = size / 2.0
    // Vertical arrow down
    g2d.draw(Line2D.Double(cx, size * 0.18, cx, size * 0.65))
    // Arrow head
    val arrowHead = Path2D.Double()
    arrowHead.moveTo(cx - size * 0.22, size * 0.45)
    arrowHead.lineTo(cx, size * 0.68)
    arrowHead.lineTo(cx + size * 0.22, size * 0.45)
    g2d.draw(arrowHead)
    // Base line
    g2d.draw(Line2D.Double(size * 0.2, size * 0.82, size * 0.8, size * 0.82))
  }

  fun terminalIcon(): ImageIcon = createIcon { g2d, size, color ->
    g2d.color = color
    g2d.stroke = BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    // Prompt >
    val prompt = Path2D.Double()
    prompt.moveTo(size * 0.22, size * 0.28)
    prompt.lineTo(size * 0.46, size * 0.50)
    prompt.lineTo(size * 0.22, size * 0.72)
    g2d.draw(prompt)
    // Cursor _
    g2d.draw(Line2D.Double(size * 0.54, size * 0.72, size * 0.78, size * 0.72))
  }

  fun changeDeviceIcon(): ImageIcon = createIcon { g2d, size, color ->
    g2d.color = color
    g2d.stroke = BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    // Top arrow (pointing right)
    g2d.draw(Line2D.Double(size * 0.22, size * 0.38, size * 0.74, size * 0.38))
    val rightHead = Path2D.Double()
    rightHead.moveTo(size * 0.58, size * 0.24)
    rightHead.lineTo(size * 0.76, size * 0.38)
    rightHead.lineTo(size * 0.58, size * 0.52)
    g2d.draw(rightHead)

    // Bottom arrow (pointing left)
    g2d.draw(Line2D.Double(size * 0.78, size * 0.64, size * 0.26, size * 0.64))
    val leftHead = Path2D.Double()
    leftHead.moveTo(size * 0.42, size * 0.50)
    leftHead.lineTo(size * 0.24, size * 0.64)
    leftHead.lineTo(size * 0.42, size * 0.78)
    g2d.draw(leftHead)
  }

  fun expandIcon(): ImageIcon = createIcon { g2d, size, color ->
    g2d.color = color
    g2d.stroke = BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    val p1 = Path2D.Double()
    p1.moveTo(size * 0.45, size * 0.28)
    p1.lineTo(size * 0.25, size * 0.50)
    p1.lineTo(size * 0.45, size * 0.72)
    g2d.draw(p1)

    val p2 = Path2D.Double()
    p2.moveTo(size * 0.70, size * 0.28)
    p2.lineTo(size * 0.50, size * 0.50)
    p2.lineTo(size * 0.70, size * 0.72)
    g2d.draw(p2)
  }

  fun collapseIcon(): ImageIcon = createIcon { g2d, size, color ->
    g2d.color = color
    g2d.stroke = BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    val p1 = Path2D.Double()
    p1.moveTo(size * 0.30, size * 0.28)
    p1.lineTo(size * 0.50, size * 0.50)
    p1.lineTo(size * 0.30, size * 0.72)
    g2d.draw(p1)

    val p2 = Path2D.Double()
    p2.moveTo(size * 0.55, size * 0.28)
    p2.lineTo(size * 0.75, size * 0.50)
    p2.lineTo(size * 0.55, size * 0.72)
    g2d.draw(p2)
  }

  fun viewListIcon(): ImageIcon = createIcon { g2d, size, color ->
    g2d.color = color
    g2d.stroke = BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    // Three horizontal lines: top (long), middle (short), bottom (medium)
    g2d.draw(Line2D.Double(size * 0.20, size * 0.28, size * 0.80, size * 0.28))
    g2d.draw(Line2D.Double(size * 0.20, size * 0.50, size * 0.55, size * 0.50))
    g2d.draw(Line2D.Double(size * 0.20, size * 0.72, size * 0.75, size * 0.72))
  }

  fun myLogsIcon(): ImageIcon = viewListIcon()

  fun warningIcon(iconSize: Int = 18): ImageIcon = createCustomIcon(iconSize) { g2d, size, _ ->
    g2d.color = Color(0xEE, 0x9B, 0x00) // amber warning
    g2d.stroke = BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    val path = Path2D.Double()
    path.moveTo(size / 2.0, size * 0.12)
    path.lineTo(size * 0.90, size * 0.88)
    path.lineTo(size * 0.10, size * 0.88)
    path.closePath()
    g2d.draw(path)
    // Exclamation point
    g2d.draw(Line2D.Double(size / 2.0, size * 0.38, size / 2.0, size * 0.62))
    g2d.fill(Ellipse2D.Double(size / 2.0 - 1.0, size * 0.72, 2.0, 2.0))
  }

  fun closeIcon(iconSize: Int = 14): ImageIcon = createCustomIcon(iconSize) { g2d, size, color ->
    g2d.color = color
    g2d.stroke = BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    val pad = size * 0.25
    g2d.draw(Line2D.Double(pad, pad, size - pad, size - pad))
    g2d.draw(Line2D.Double(size - pad, pad, pad, size - pad))
  }

  fun createCustomIcon(size: Int, painter: (Graphics2D, Int, Color) -> Unit): ImageIcon {
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g2d = image.createGraphics()
    try {
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
      painter(g2d, size, Color.WHITE)
    } finally {
      g2d.dispose()
    }
    return ImageIcon(image)
  }

  private fun createIcon(painter: (Graphics2D, Int, Color) -> Unit): ImageIcon {
    return createCustomIcon(ICON_SIZE, painter)
  }
}

