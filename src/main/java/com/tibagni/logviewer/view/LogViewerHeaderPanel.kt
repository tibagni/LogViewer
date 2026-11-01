package com.tibagni.logviewer.view

import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.util.SwingUtils
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import org.jetbrains.annotations.VisibleForTesting
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.UIManager
import javax.swing.border.Border

/**
 * Header panel displayed above the log tables showing the current log file path,
 * file icon, and log count status badge.
 */
open class LogViewerHeaderPanel : JPanel(BorderLayout()) {

  private val currentLogsLbl: JLabel = JLabel().apply {
    autoscrolls = true
    text = ""
    icon = CommonIcons.fileDocumentIcon()
    iconTextGap = UIScaleUtils.dip(6)
  }

  private val statusBadge: StatusBadge = StatusBadge()

  @get:VisibleForTesting
  val displayedPathText: String?
    get() = currentLogsLbl.text

  @get:VisibleForTesting
  val displayedPathIcon: Icon?
    get() = currentLogsLbl.icon

  @get:VisibleForTesting
  val isBadgeVisible: Boolean
    get() = statusBadge.isVisible

  @get:VisibleForTesting
  val badgeText: String
    get() = statusBadge.text

  init {
    isOpaque = false
    isVisible = false
    border = createHeaderBorder()
    add(currentLogsLbl, BorderLayout.CENTER)
    add(statusBadge, BorderLayout.EAST)
  }

  /**
   * Updates the displayed log path by truncating to [availableWidth] if needed
   * and toggles header visibility.
   *
   * @param path File path or description of currently opened logs, or null to hide.
   * @param availableWidth Maximum available width in pixels for text truncation.
   */
  fun updatePath(path: String?, availableWidth: Int = 1000) {
    if (path.isNullOrEmpty()) {
      currentLogsLbl.text = ""
      currentLogsLbl.icon = null
      isVisible = false
      return
    }

    val width = if (availableWidth > 0) availableWidth else 1000
    val truncated = SwingUtils.truncateTextFor(currentLogsLbl, "", path, width).trim()
    currentLogsLbl.text = truncated
    currentLogsLbl.icon = CommonIcons.fileDocumentIcon()
    isVisible = true
  }

  /**
   * Updates the status badge with total and filtered log counts.
   *
   * @param totalLogsCount Total number of loaded log lines.
   * @param filteredLogsCount Number of log lines matching active filters.
   */
  fun updateStats(totalLogsCount: Int, filteredLogsCount: Int) {
    statusBadge.updateStats(totalLogsCount, filteredLogsCount)
  }

  /**
   * Resets the header panel by clearing the log path, resetting badge stats, and hiding.
   */
  fun reset() {
    currentLogsLbl.text = null
    currentLogsLbl.icon = null
    statusBadge.updateStats(0, 0)
    isVisible = false
  }

  /**
   * Refreshes dynamic theme colors, icons, borders, and repaints the header panel.
   */
  fun onThemeChanged() {
    border = createHeaderBorder()
    if (currentLogsLbl.icon != null) {
      currentLogsLbl.icon = CommonIcons.fileDocumentIcon()
    }
    repaint()
  }

  /**
   * Creates the compound border for the header panel with a theme-aware bottom matte line
   * and inset padding.
   *
   * @return [Border] compound border for the header panel.
   */
  internal fun createHeaderBorder(): Border {
    val matteBorder = BorderFactory.createMatteBorder(
      0,
      0,
      UIScaleUtils.dip(1),
      0,
      CardBorderUtils.resolveCardBorderColor()
    )
    val emptyBorder = BorderFactory.createEmptyBorder(
      UIScaleUtils.dip(4),
      UIScaleUtils.dip(8),
      UIScaleUtils.dip(4),
      UIScaleUtils.dip(8)
    )
    return BorderFactory.createCompoundBorder(matteBorder, emptyBorder)
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    try {
      if (width > 0 && height > 0) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val bg = UIManager.getColor("Panel.background") ?: background ?: Color(240, 240, 240)
        val isDark = isDarkTheme(bg)
        val tint = if (isDark) Color(255, 255, 255, 8) else Color(0, 0, 0, 10)
        g2.color = bg
        g2.fillRect(0, 0, width, height)
        g2.color = tint
        g2.fillRect(0, 0, width, height)
      }
    } finally {
      g2.dispose()
    }
    super.paintComponent(g)
  }

  /**
   * Determines whether the active theme is a dark theme, using the theme
   * manager or falling back to relative luminance of [fallbackBg].
   *
   * @param fallbackBg Background color used to calculate luminance fallback.
   * @return True if dark theme is active or fallback background is dark, false otherwise.
   */
  private fun isDarkTheme(fallbackBg: Color): Boolean {
    return try {
      ServiceLocator.themeManager.isDark
    } catch (_: Throwable) {
      val r = 0.299 * fallbackBg.red
      val g = 0.587 * fallbackBg.green
      val b = 0.114 * fallbackBg.blue
      val lum = (r + g + b) / 255.0
      lum < 0.5
    }
  }
}
