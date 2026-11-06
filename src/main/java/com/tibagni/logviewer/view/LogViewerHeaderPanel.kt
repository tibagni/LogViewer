package com.tibagni.logviewer.view

import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.util.CommonUtils
import com.tibagni.logviewer.util.SwingUtils
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import kotlin.concurrent.thread
import org.jetbrains.annotations.VisibleForTesting
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Desktop
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities
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
    icon = CommonIcons.fileDocumentIcon(UIScaleUtils.dip(16))
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
  val displayedToolTipText: String?
    get() = currentLogsLbl.toolTipText

  @get:VisibleForTesting
  val displayedCursor: Cursor
    get() = currentLogsLbl.cursor

  @get:VisibleForTesting
  val isBadgeVisible: Boolean
    get() = statusBadge.isVisible

  @get:VisibleForTesting
  val badgeText: String
    get() = statusBadge.text

  @get:VisibleForTesting
  internal var rawPath: String? = null

  init {
    isOpaque = false
    isVisible = false
    border = createHeaderBorder()

    currentLogsLbl.apply {
      cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
      addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
          if (SwingUtilities.isRightMouseButton(e) || e.clickCount == 2) {
            showContextMenu(e)
          }
        }
      })
    }

    add(currentLogsLbl, BorderLayout.CENTER)
    add(statusBadge, BorderLayout.EAST)
  }

  /**
   * Displays a context menu allowing the user to copy the log path
   * or open the containing folder.
   */
  private fun showContextMenu(e: MouseEvent) {
    val path = rawPath ?: return
    val popup = JPopupMenu()

    val copyItem = JMenuItem(I18n.get(I18n.LOG_DETAILS_COPY_PATH)).apply {
      icon = CommonIcons.copyIcon(UIScaleUtils.dip(14))
      addActionListener {
        CommonUtils.copyToClipboard(path)
        Toast.showToast(
          this@LogViewerHeaderPanel,
          I18n.get(I18n.COMMON_COPIED_TO_CLIPBOARD),
          Toast.LENGTH_SHORT
        )
      }
    }
    popup.add(copyItem)

    val file = File(path)
    val folder = if (file.isDirectory) file else file.parentFile
    if (folder != null && folder.exists() && Desktop.isDesktopSupported() &&
      Desktop.getDesktop().isSupported(Desktop.Action.OPEN)
    ) {
      val openFolderItem = JMenuItem(I18n.get(I18n.LOGS_HEADER_OPEN_FOLDER)).apply {
        icon = CommonIcons.folderIcon(UIScaleUtils.dip(14))
        addActionListener {
          thread(isDaemon = true, name = "OpenFolderThread") {
            try {
              Desktop.getDesktop().open(folder)
            } catch (t: Throwable) {
              Logger.error("Failed to open directory $folder", t)
            }
          }
        }
      }
      popup.add(openFolderItem)
    }

    popup.show(e.component, e.x, e.y)
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
      rawPath = null
      currentLogsLbl.text = ""
      currentLogsLbl.icon = null
      currentLogsLbl.toolTipText = null
      isVisible = false
      return
    }

    rawPath = path
    val width = if (availableWidth > 0) availableWidth else 1000
    val truncated = SwingUtils.truncateTextFor(currentLogsLbl, "", path, width).trim()
    currentLogsLbl.text = truncated
    currentLogsLbl.icon = CommonIcons.fileDocumentIcon(UIScaleUtils.dip(16))
    currentLogsLbl.toolTipText = path
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
    rawPath = null
    currentLogsLbl.text = null
    currentLogsLbl.icon = null
    currentLogsLbl.toolTipText = null
    statusBadge.updateStats(0, 0)
    isVisible = false
  }

  /**
   * Refreshes dynamic theme colors, icons, borders, and repaints the header panel.
   */
  fun onThemeChanged() {
    border = createHeaderBorder()
    if (currentLogsLbl.icon != null) {
      currentLogsLbl.icon = CommonIcons.fileDocumentIcon(UIScaleUtils.dip(16))
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
