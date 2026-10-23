package com.tibagni.logviewer.view

import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.text.NumberFormat
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.SwingConstants
import javax.swing.UIManager
import kotlin.math.roundToInt

/**
 * A lightweight, reusable status badge component displaying line count statistics
 * with a subtle rounded background pill and border stroke.
 *
 * Dynamically resolves theme colors for background, border, and foreground,
 * adapting to Light, Dark, and FlatLaf theme changes.
 */
class StatusBadge : JLabel() {

  companion object {
    /**
     * Vertical padding in density-independent pixels.
     */
    const val PADDING_VERTICAL_DP: Int = 2

    /**
     * Horizontal padding in density-independent pixels.
     */
    const val PADDING_HORIZONTAL_DP: Int = 8

    /**
     * Default corner arc radius in density-independent pixels when not
     * defined by the theme.
     */
    const val DEFAULT_ARC_DP: Int = 8
  }

  init {
    isOpaque = false
    isVisible = false
    horizontalAlignment = SwingConstants.CENTER
    applyPaddingAndTheme()
  }

  /**
   * Applies the theme-based padding and foreground color.
   */
  private fun applyPaddingAndTheme() {
    border = BorderFactory.createEmptyBorder(
      UIScaleUtils.dip(PADDING_VERTICAL_DP),
      UIScaleUtils.dip(PADDING_HORIZONTAL_DP),
      UIScaleUtils.dip(PADDING_VERTICAL_DP),
      UIScaleUtils.dip(PADDING_HORIZONTAL_DP)
    )
    foreground = resolveBadgeForeground()
  }

  /**
   * Resolves the badge background color from Look and Feel defaults,
   * falling back to a subtle semi-transparent gray.
   *
   * @return the [Color] to paint the badge background.
   */
  internal fun resolveBadgeBackground(): Color {
    return UIManager.getColor("Button.background") ?: Color(128, 128, 128, 40)
  }

  /**
   * Resolves the badge border stroke color from Look and Feel defaults,
   * falling back to a subtle semi-transparent gray.
   *
   * @return the [Color] to paint the badge boundary stroke.
   */
  internal fun resolveBorderColor(): Color {
    return UIManager.getColor("Component.borderColor") ?: Color(128, 128, 128, 60)
  }

  /**
   * Resolves the badge foreground text color from Look and Feel defaults,
   * falling back to disabled or regular text color.
   *
   * @return the [Color] for text rendering.
   */
  internal fun resolveBadgeForeground(): Color {
    return UIManager.getColor("Label.disabledForeground")
      ?: UIManager.getColor("Label.foreground")
      ?: Color.GRAY
  }

  /**
   * Resolves the corner arc radius in scaled pixels, honoring the Look and Feel
   * "Component.arc" setting if present, or falling back to [DEFAULT_ARC_DP].
   *
   * @return the scaled corner arc in pixels.
   */
  internal fun resolveArc(): Int {
    val themeArc = UIManager.getInt("Component.arc")
    return if (themeArc > 0) {
      UIScaleUtils.dip(themeArc)
    } else {
      UIScaleUtils.dip(DEFAULT_ARC_DP)
    }
  }

  /**
   * Updates component UI when look-and-feel changes.
   */
  override fun updateUI() {
    super.updateUI()
    applyPaddingAndTheme()
    repaint()
  }

  /**
   * Updates the displayed statistics given total, filtered, and dropped
   * log line counts.
   *
   * Formats numbers using [NumberFormat.getIntegerInstance].
   * - When [totalLines] <= 0: hidden ([isVisible] = false).
   * - When [droppedLines] <= 0:
   *   - When [filteredLines] <= 0: displays [I18n.LOGS_STATUS_ALL].
   *   - When [filteredLines] > 0: displays [I18n.LOGS_STATUS_FILTERED].
   * - When [droppedLines] > 0:
   *   - When [filteredLines] <= 0: displays [I18n.LOGS_STATUS_ALL_DROPPED].
   *   - When [filteredLines] > 0: displays [I18n.LOGS_STATUS_FILTERED_DROPPED].
   *
   * @param totalLines total number of log lines loaded.
   * @param filteredLines number of lines matching currently active filters.
   * @param droppedLines number of live log lines dropped due to buffer limits.
   */
  @JvmOverloads
  fun updateStats(
    totalLines: Int,
    filteredLines: Int,
    droppedLines: Long = 0L
  ) {
    if (totalLines <= 0) {
      isVisible = false
      text = ""
      return
    }

    val numberFormat = NumberFormat.getIntegerInstance()
    val formattedTotal = numberFormat.format(totalLines)
    val hasDropped = droppedLines > 0L
    val formattedDropped = if (hasDropped) {
      numberFormat.format(droppedLines)
    } else {
      ""
    }

    if (filteredLines <= 0) {
      text = if (hasDropped) {
        I18n.format(
          I18n.LOGS_STATUS_ALL_DROPPED,
          formattedTotal,
          formattedDropped
        )
      } else {
        I18n.format(I18n.LOGS_STATUS_ALL, formattedTotal)
      }
      isVisible = true
    } else {
      val formattedFiltered = numberFormat.format(filteredLines)
      val percentage = (
        (filteredLines.toDouble() / totalLines.toDouble()) * 100.0
      ).roundToInt()
      text = if (hasDropped) {
        I18n.format(
          I18n.LOGS_STATUS_FILTERED_DROPPED,
          formattedFiltered,
          formattedTotal,
          percentage,
          formattedDropped
        )
      } else {
        I18n.format(
          I18n.LOGS_STATUS_FILTERED,
          formattedFiltered,
          formattedTotal,
          percentage
        )
      }
      isVisible = true
    }
  }

  /**
   * Paints the subtle rounded background and boundary stroke before painting
   * label text.
   *
   * @param g the [Graphics] context to paint with.
   */
  public override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    try {
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON
      )
      val w = width
      val h = height
      if (w > 0 && h > 0) {
        val arc = resolveArc()
        g2.color = resolveBadgeBackground()
        g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc)
        g2.color = resolveBorderColor()
        g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc)
      }
    } finally {
      g2.dispose()
    }
    super.paintComponent(g)
  }
}
