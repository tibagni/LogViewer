package com.tibagni.logviewer.view

import com.tibagni.logviewer.ServiceLocator
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
 * A lightweight, reusable status pill badge component displaying line counts,
 * search match statistics, or custom indicators with a subtle rounded background
 * and boundary stroke.
 *
 * Dynamically resolves theme colors for background, border, and foreground,
 * adapting to Light, Dark, and FlatLaf theme changes.
 */
class StatusBadge @JvmOverloads constructor(
  initialVariant: Variant = Variant.DEFAULT
) : JLabel() {

  enum class Variant {
    DEFAULT,
    MUTED,
    WARNING,
    ERROR;

    companion object {
      @JvmField val NORMAL: Variant = DEFAULT
      @JvmField val NO_RESULTS: Variant = MUTED
    }
  }

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

  var variant: Variant = initialVariant
    set(value) {
      field = value
      applyPaddingAndTheme()
      repaint()
    }

  var arcDp: Int = DEFAULT_ARC_DP
    set(value) {
      field = value
      repaint()
    }

  var paddingHorizontalDp: Int = PADDING_HORIZONTAL_DP
    set(value) {
      field = value
      applyPaddingAndTheme()
      repaint()
    }

  var paddingVerticalDp: Int = PADDING_VERTICAL_DP
    set(value) {
      field = value
      applyPaddingAndTheme()
      repaint()
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
  internal fun applyPaddingAndTheme() {
    val vPad = if (paddingVerticalDp > 0) paddingVerticalDp else PADDING_VERTICAL_DP
    val hPad = if (paddingHorizontalDp > 0) paddingHorizontalDp else PADDING_HORIZONTAL_DP
    border = BorderFactory.createEmptyBorder(
      UIScaleUtils.dip(vPad),
      UIScaleUtils.dip(hPad),
      UIScaleUtils.dip(vPad),
      UIScaleUtils.dip(hPad)
    )
    foreground = resolveBadgeForeground()
  }

  private fun isDarkTheme(): Boolean {
    return try {
      ServiceLocator.themeManager.isDark
    } catch (_: Throwable) {
      val bg = UIManager.getColor("Panel.background") ?: Color.WHITE
      val lum = (0.299 * bg.red + 0.587 * bg.green + 0.114 * bg.blue) / 255.0
      lum < 0.5
    }
  }

  /**
   * Resolves the badge background color from Look and Feel defaults,
   * adapting based on the active [variant].
   *
   * @param targetVariant the [Variant] to resolve for, defaulting to [variant].
   * @return the [Color] to paint the badge background.
   */
  internal fun resolveBadgeBackground(targetVariant: Variant? = null): Color {
    val active = targetVariant ?: variant ?: Variant.DEFAULT
    val isDark = isDarkTheme()
    return when (active) {
      Variant.DEFAULT -> {
        UIManager.getColor("Button.background")
          ?: if (isDark) Color(70, 70, 70) else Color(128, 128, 128, 40)
      }
      Variant.MUTED -> {
        if (isDark) Color(140, 50, 50, 45) else Color(245, 220, 220)
      }
      Variant.WARNING -> {
        if (isDark) Color(150, 110, 40, 50) else Color(254, 240, 215)
      }
      Variant.ERROR -> {
        if (isDark) Color(160, 50, 50, 60) else Color(255, 215, 215)
      }
    }
  }

  /**
   * Resolves the badge border stroke color from Look and Feel defaults,
   * adapting based on the active [variant].
   *
   * @param targetVariant the [Variant] to resolve for, defaulting to [variant].
   * @return the [Color] to paint the badge boundary stroke.
   */
  internal fun resolveBorderColor(targetVariant: Variant? = null): Color {
    val active = targetVariant ?: variant ?: Variant.DEFAULT
    val isDark = isDarkTheme()
    return when (active) {
      Variant.DEFAULT -> {
        UIManager.getColor("Component.borderColor")
          ?: if (isDark) Color(90, 90, 90) else Color(128, 128, 128, 60)
      }
      Variant.MUTED -> {
        if (isDark) Color(200, 80, 80, 120) else Color(230, 140, 140)
      }
      Variant.WARNING -> {
        if (isDark) Color(220, 150, 50, 140) else Color(235, 175, 75)
      }
      Variant.ERROR -> {
        if (isDark) Color(230, 80, 80, 160) else Color(225, 100, 100)
      }
    }
  }

  /**
   * Resolves the badge foreground text color from Look and Feel defaults,
   * adapting based on the active [variant].
   *
   * @param targetVariant the [Variant] to resolve for, defaulting to [variant].
   * @return the [Color] for text rendering.
   */
  internal fun resolveBadgeForeground(targetVariant: Variant? = null): Color {
    val active = targetVariant ?: variant ?: Variant.DEFAULT
    val isDark = isDarkTheme()
    return when (active) {
      Variant.DEFAULT -> {
        UIManager.getColor("Label.disabledForeground")
          ?: UIManager.getColor("Label.foreground")
          ?: if (isDark) Color(200, 200, 200) else Color.GRAY
      }
      Variant.MUTED -> {
        if (isDark) Color(245, 140, 140) else Color(185, 45, 45)
      }
      Variant.WARNING -> {
        if (isDark) Color(245, 185, 90) else Color(175, 100, 20)
      }
      Variant.ERROR -> {
        if (isDark) Color(255, 130, 130) else Color(195, 30, 30)
      }
    }
  }

  /**
   * Resolves the corner arc radius in scaled pixels, honoring the Look and Feel
   * "Component.arc" setting if present, or falling back to [arcDp].
   *
   * @return the scaled corner arc in pixels.
   */
  internal fun resolveArc(): Int {
    val themeArc = UIManager.getInt("Component.arc")
    val defaultArc = if (arcDp > 0) arcDp else DEFAULT_ARC_DP
    return if (themeArc > 0) {
      UIScaleUtils.dip(themeArc)
    } else {
      UIScaleUtils.dip(defaultArc)
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
   * Updates the displayed single count with standard integer formatting.
   *
   * @param count the integer count to display.
   * @param targetVariant visual styling variant for the badge.
   */
  @JvmOverloads
  fun updateCount(
    count: Int,
    targetVariant: Variant = Variant.DEFAULT
  ) {
    this.variant = targetVariant
    this.text = NumberFormat.getIntegerInstance().format(count)
    this.isVisible = true
    repaint()
  }

  /**
   * Updates search result counts and styling state.
   *
   * @param currentMatch 1-based index of highlighted match, or 0.
   * @param totalMatches total count of matches found.
   * @param targetVariant visual styling variant for the badge.
   * @param customMessage optional custom text to display.
   */
  @JvmOverloads
  fun updateResult(
    currentMatch: Int,
    totalMatches: Int,
    targetVariant: Variant = Variant.DEFAULT,
    customMessage: String? = null
  ) {
    this.variant = targetVariant
    this.text = when {
      customMessage != null -> customMessage
      targetVariant == Variant.WARNING || targetVariant == Variant.ERROR -> {
        I18n.get(I18n.SEARCH_BAD_PATTERN)
      }
      targetVariant == Variant.MUTED || totalMatches == 0 -> {
        I18n.format(I18n.SEARCH_RESULTS_COUNT, 0)
      }
      currentMatch > 0 -> "$currentMatch/$totalMatches"
      else -> I18n.format(I18n.SEARCH_RESULTS_COUNT, totalMatches)
    }
    this.isVisible = text.isNotEmpty()
    repaint()
  }

  /**
   * Clears the badge text, resets to default variant, and hides it.
   */
  fun clear() {
    this.variant = Variant.DEFAULT
    this.text = ""
    this.isVisible = false
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

    this.variant = Variant.DEFAULT
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
