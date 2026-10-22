package com.tibagni.logviewer.view

import com.tibagni.logviewer.util.scaling.UIScaleUtils
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JComponent
import javax.swing.UIManager

/**
 * A subtle vertical divider component designed to separate adjacent
 * toolbar buttons.
 *
 * Renders a centered 1-pixel-wide vertical line with configurable
 * height and width, using theme separator colors or falling back to a
 * subtle semi-transparent gray. Supports DPI scaling via [UIScaleUtils.dip].
 *
 * @param widthDp the total component width in density-independent pixels.
 * @param heightDp the preferred component height in density-independent pixels.
 * @param lineHeightDp the painted line height in density-independent pixels.
 */
class ToolbarDivider @JvmOverloads constructor(
  private val widthDp: Int = DEFAULT_WIDTH_DP,
  private val heightDp: Int = DEFAULT_HEIGHT_DP,
  private val lineHeightDp: Int = DEFAULT_LINE_HEIGHT_DP
) : JComponent() {

  companion object {
    /**
     * Default width of the divider component in density-independent pixels.
     */
    const val DEFAULT_WIDTH_DP: Int = 7

    /**
     * Default preferred height in density-independent pixels.
     */
    const val DEFAULT_HEIGHT_DP: Int = 16

    /**
     * Default painted line height in density-independent pixels.
     */
    const val DEFAULT_LINE_HEIGHT_DP: Int = 12
  }

  init {
    isOpaque = false
  }

  /**
   * Returns the preferred dimensions of this divider component.
   */
  override fun getPreferredSize(): Dimension {
    return Dimension(UIScaleUtils.dip(widthDp), UIScaleUtils.dip(heightDp))
  }

  /**
   * Returns the minimum dimensions of this divider component.
   */
  override fun getMinimumSize(): Dimension {
    return Dimension(UIScaleUtils.dip(widthDp), UIScaleUtils.dip(lineHeightDp))
  }

  /**
   * Returns the maximum dimensions of this divider component.
   */
  override fun getMaximumSize(): Dimension {
    return Dimension(UIScaleUtils.dip(widthDp), UIScaleUtils.dip(heightDp))
  }

  /**
   * Resolves the divider line color from look-and-feel UI defaults,
   * falling back to component border color or a subtle semi-transparent gray.
   *
   * @return the [Color] to paint the vertical separator line.
   */
  internal fun getDividerColor(): Color {
    return UIManager.getColor("Separator.foreground")
      ?: UIManager.getColor("Component.borderColor")
      ?: Color(128, 128, 128, 80)
  }

  /**
   * Updates component UI when look-and-feel changes.
   */
  override fun updateUI() {
    super.updateUI()
    repaint()
  }

  /**
   * Paints the subtle vertical line centered inside this component.
   *
   * @param g the [Graphics] context to draw with.
   */
  public override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    try {
      g2.color = getDividerColor()
      val scaledLineHeight = UIScaleUtils.dip(lineHeightDp).coerceAtMost(height)
      if (scaledLineHeight > 0) {
        val x = width / 2
        val y = (height - scaledLineHeight) / 2
        g2.drawLine(x, y, x, y + scaledLineHeight - 1)
      }
    } finally {
      g2.dispose()
    }
  }
}
