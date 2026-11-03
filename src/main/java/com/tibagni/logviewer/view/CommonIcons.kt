package com.tibagni.logviewer.view

import com.tibagni.logviewer.ServiceLocator

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import javax.swing.ImageIcon
import javax.swing.UIManager

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

  /**
   * Returns a theme-aware clear/ban icon of the given [size].
   *
   * @param size icon width and height in pixels (defaults to [ICON_SIZE])
   * @return theme-aware [ImageIcon] that dynamically resolves theme colors
   */
  @JvmStatic
  @JvmOverloads
  fun clearIcon(size: Int = ICON_SIZE): ImageIcon = ClearIcon(size)

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

  /**
   * Creates a theme-aware vector document icon representing a log file
   * with a subtle folded corner and inner content lines.
   *
   * @param size icon width and height in pixels (defaults to 16)
   * @return theme-aware [ImageIcon] that dynamically resolves theme colors
   */
  fun fileDocumentIcon(size: Int = 16): ImageIcon = FileDocumentIcon(size)

  /**
   * Creates a theme-aware vector smartphone icon representing a connected
   * device with rounded body, speaker notch, and home indicator.
   *
   * @param size icon width and height in pixels (defaults to 16)
   * @return theme-aware [ImageIcon] that dynamically resolves theme colors
   */
  fun smartphoneIcon(size: Int = 16): ImageIcon = SmartphoneIcon(size)

  /**
   * Creates a status indicator dot icon representing the live streaming state.
   *
   * @param active whether streaming is currently active
   * @param size icon width and height in pixels (defaults to 16)
   * @return [ImageIcon] displaying a vibrant green dot with soft glow ring
   *         when active, or a subtle neutral dot when inactive.
   */
  fun statusIndicatorIcon(active: Boolean, size: Int = 16): ImageIcon =
    StatusIndicatorIcon(active, size)

  /**
   * Creates a theme-aware vector chevron up icon with smooth rounded stroke.
   *
   * @param size icon width and height in pixels (defaults to 16)
   * @return theme-aware [ImageIcon] that dynamically resolves theme colors
   */
  @JvmStatic
  @JvmOverloads
  fun chevronUpIcon(size: Int = 16): ImageIcon = ChevronUpIcon(size)

  /**
   * Creates a theme-aware vector chevron down icon with smooth rounded stroke.
   *
   * @param size icon width and height in pixels (defaults to 16)
   * @return theme-aware [ImageIcon] that dynamically resolves theme colors
   */
  @JvmStatic
  @JvmOverloads
  fun chevronDownIcon(size: Int = 16): ImageIcon = ChevronDownIcon(size)

  /**
   * Creates a theme-aware vector chevron right icon with smooth rounded
   * stroke.
   *
   * @param size icon width and height in pixels (defaults to 16)
   * @return theme-aware [ImageIcon] that dynamically resolves theme colors
   */
  @JvmStatic
  @JvmOverloads
  fun chevronRightIcon(size: Int = 16): ImageIcon = ChevronRightIcon(size)

  /**
   * Creates a theme-aware vector chevron left icon with smooth rounded
   * stroke.
   *
   * @param size icon width and height in pixels (defaults to 16)
   * @return theme-aware [ImageIcon] that dynamically resolves theme colors
   */
  @JvmStatic
  @JvmOverloads
  fun chevronLeftIcon(size: Int = 16): ImageIcon = ChevronLeftIcon(size)

  /**
   * Creates a theme-aware vector plus icon with smooth rounded stroke.
   *
   * @param size icon width and height in pixels (defaults to 16)
   * @return theme-aware [ImageIcon] that dynamically resolves theme colors
   */
  @JvmStatic
  @JvmOverloads
  fun plusIcon(size: Int = 16): ImageIcon = PlusIcon(size)

  /**
   * Creates a theme-aware vector close cross icon with smooth rounded stroke.
   *
   * @param size icon width and height in pixels (defaults to 16)
   * @return theme-aware [ImageIcon] that dynamically resolves theme colors
   */
  @JvmStatic
  @JvmOverloads
  fun closeIcon(size: Int = 16): ImageIcon = CloseIcon(size)

  /**
   * Creates a theme-aware vector folder icon with smooth rounded stroke.
   *
   * @param size icon width and height in pixels (defaults to 16)
   * @return theme-aware [ImageIcon] that dynamically resolves theme colors
   */
  @JvmStatic
  @JvmOverloads
  fun folderIcon(size: Int = 16): ImageIcon = FolderIcon(size)

  /**
   * Creates a theme-aware vector copy icon with two overlapping document outlines.
   *
   * @param size icon width and height in pixels (defaults to 16)
   * @return theme-aware [ImageIcon] that dynamically resolves theme colors
   */
  @JvmStatic
  @JvmOverloads
  fun copyIcon(size: Int = 16): ImageIcon = CopyIcon(size)

  /**
   * Returns a theme-aware search (magnifying glass) icon of the given [size].
   *
   * @param size icon width and height in pixels (defaults to 16)
   * @return theme-aware [ImageIcon] that dynamically resolves theme colors
   */
  @JvmStatic
  @JvmOverloads
  fun searchIcon(size: Int = 16): ImageIcon = SearchIcon(size)

  /**
   * Returns a theme-aware palette icon of the given [size].
   *
   * @param size icon width and height in pixels (defaults to 16)
   * @return theme-aware [ImageIcon] that dynamically resolves theme colors
   */
  @JvmStatic
  @JvmOverloads
  fun paletteIcon(size: Int = 16): ImageIcon = PaletteIcon(size)

  /**
   * Returns a theme-aware filter funnel icon of the given [size].
   *
   * @param size icon width and height in pixels (defaults to 16)
   * @return theme-aware [ImageIcon] that dynamically resolves theme colors
   */
  @JvmStatic
  @JvmOverloads
  fun filterIcon(size: Int = 16): ImageIcon = FilterIcon(size)

  /**
   * Returns a theme-aware logs tab icon of the given [size].
   *
   * @param size icon width and height in pixels (defaults to 16)
   * @return theme-aware [ImageIcon] that dynamically resolves theme colors
   */
  @JvmStatic
  @JvmOverloads
  fun logsTabIcon(size: Int = 16): ImageIcon = fileDocumentIcon(size)

  /**
   * Returns a theme-aware bug report tab icon of the given [size].
   *
   * @param size icon width and height in pixels (defaults to 16)
   * @return theme-aware [ImageIcon] that dynamically resolves theme colors
   */
  @JvmStatic
  @JvmOverloads
  fun bugReportTabIcon(size: Int = 16): ImageIcon = BugReportTabIcon(size)

  /**
   * Returns a theme-aware live logs streaming tab icon of the given [size].
   *
   * @param size icon width and height in pixels (defaults to 16)
   * @return theme-aware [ImageIcon] that dynamically resolves theme colors
   */
  @JvmStatic
  @JvmOverloads
  fun liveLogsTabIcon(size: Int = 16): ImageIcon = LiveLogsTabIcon(size)

  /**
   * Returns a theme-aware more options (vertical ellipsis) icon of the given [size].
   *
   * @param size icon width and height in pixels (defaults to 16)
   * @return theme-aware [ImageIcon] that dynamically resolves theme colors
   */
  @JvmStatic
  @JvmOverloads
  fun moreVerticalIcon(size: Int = 16): ImageIcon = MoreVerticalIcon(size)

  /**
   * Returns a theme-aware save (floppy disk) icon of the given [size].
   *
   * @param size icon width and height in pixels (defaults to 16)
   * @return theme-aware [ImageIcon] that dynamically resolves theme colors
   */
  @JvmStatic
  @JvmOverloads
  fun saveIcon(size: Int = 16): ImageIcon = SaveIcon(size)

  /**
   * Returns a theme-aware checkmark icon of the given [size].
   *
   * @param size icon width and height in pixels (defaults to 16)
   * @return theme-aware [ImageIcon] that dynamically resolves theme colors
   */
  @JvmStatic
  @JvmOverloads
  fun checkIcon(size: Int = 16): ImageIcon = CheckIcon(size)

  /**
   * Returns a theme-aware info badge icon of the given [size].
   *
   * @param size icon width and height in pixels (defaults to 16)
   * @return theme-aware [ImageIcon] that dynamically resolves theme colors
   */
  @JvmStatic
  @JvmOverloads
  fun infoIcon(size: Int = 16): ImageIcon = InfoIcon(size)

  /**
   * Returns a theme-aware circular refresh/reload icon of the given [size].
   *
   * @param size icon width and height in pixels (defaults to 16)
   * @return theme-aware [ImageIcon] that dynamically resolves theme colors
   */
  @JvmStatic
  @JvmOverloads
  fun refreshIcon(size: Int = 16): ImageIcon = RefreshIcon(size)

  /**
   * Dynamic theme-aware [ImageIcon] that paints a smartphone device icon
   * using current Look and Feel foreground colors directly at paint time.
   */
  private class SmartphoneIcon(
    private val iconSize: Int = 16
  ) : ImageIcon() {
    override fun getIconWidth(): Int = iconSize
    override fun getIconHeight(): Int = iconSize

    override fun getImage(): java.awt.Image {
      val image = BufferedImage(
        iconSize,
        iconSize,
        BufferedImage.TYPE_INT_ARGB
      )
      val g2d = image.createGraphics()
      try {
        g2d.setRenderingHint(
          RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON
        )
        g2d.setRenderingHint(
          RenderingHints.KEY_STROKE_CONTROL,
          RenderingHints.VALUE_STROKE_PURE
        )
        paintSmartphone(g2d, iconSize)
      } finally {
        g2d.dispose()
      }
      return image
    }

    override fun paintIcon(
      c: java.awt.Component?,
      g: java.awt.Graphics,
      x: Int,
      y: Int
    ) {
      val g2d = g.create() as? Graphics2D ?: return
      try {
        g2d.translate(x, y)
        g2d.setRenderingHint(
          RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON
        )
        g2d.setRenderingHint(
          RenderingHints.KEY_STROKE_CONTROL,
          RenderingHints.VALUE_STROKE_PURE
        )
        paintSmartphone(g2d, iconSize)
      } finally {
        g2d.dispose()
      }
    }

    private fun paintSmartphone(g2d: Graphics2D, s: Int) {
      val fgColor = UIManager.getColor("Label.foreground")
        ?: UIManager.getColor("Component.foreground")
        ?: Color(128, 128, 128)

      val phoneWidth = s * 0.58
      val phoneHeight = s * 0.86
      val x = (s - phoneWidth) / 2.0
      val y = (s - phoneHeight) / 2.0
      val arc = s * 0.18

      val strokeWidth = (s * 0.08f).coerceAtLeast(1.2f)
      g2d.stroke = BasicStroke(
        strokeWidth,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND
      )
      g2d.color = fgColor

      // Phone body outline
      val body = RoundRectangle2D.Double(
        x,
        y,
        phoneWidth,
        phoneHeight,
        arc,
        arc
      )
      g2d.draw(body)

      // Speaker notch
      val speakerW = phoneWidth * 0.35
      val speakerX = (s - speakerW) / 2.0
      val speakerY = y + phoneHeight * 0.14
      g2d.draw(
        Line2D.Double(speakerX, speakerY, speakerX + speakerW, speakerY)
      )

      // Home indicator
      val homeW = phoneWidth * 0.35
      val homeX = (s - homeW) / 2.0
      val homeY = y + phoneHeight * 0.86
      g2d.draw(Line2D.Double(homeX, homeY, homeX + homeW, homeY))
    }
  }

  /**
   * Dynamic [ImageIcon] that paints an active or inactive status dot indicator
   * using a vibrant green dot with soft glow ring when active, or a subtle
   * neutral dot when inactive.
   */
  private class StatusIndicatorIcon(
    val active: Boolean,
    private val iconSize: Int = 16
  ) : ImageIcon() {
    override fun getIconWidth(): Int = iconSize
    override fun getIconHeight(): Int = iconSize

    override fun getImage(): java.awt.Image {
      val image = BufferedImage(
        iconSize,
        iconSize,
        BufferedImage.TYPE_INT_ARGB
      )
      val g2d = image.createGraphics()
      try {
        g2d.setRenderingHint(
          RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON
        )
        g2d.setRenderingHint(
          RenderingHints.KEY_STROKE_CONTROL,
          RenderingHints.VALUE_STROKE_PURE
        )
        paintIndicator(g2d, iconSize)
      } finally {
        g2d.dispose()
      }
      return image
    }

    override fun paintIcon(
      c: java.awt.Component?,
      g: java.awt.Graphics,
      x: Int,
      y: Int
    ) {
      val g2d = g.create() as? Graphics2D ?: return
      try {
        g2d.translate(x, y)
        g2d.setRenderingHint(
          RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON
        )
        g2d.setRenderingHint(
          RenderingHints.KEY_STROKE_CONTROL,
          RenderingHints.VALUE_STROKE_PURE
        )
        paintIndicator(g2d, iconSize)
      } finally {
        g2d.dispose()
      }
    }

    private fun paintIndicator(g2d: Graphics2D, s: Int) {
      val cx = s / 2.0
      val cy = s / 2.0

      if (active) {
        val green = Color(46, 180, 87)
        val glow = Color(46, 180, 87, 60)

        // Outer translucent glow ring
        val outerR = s * 0.38
        g2d.color = glow
        g2d.fill(
          Ellipse2D.Double(
            cx - outerR,
            cy - outerR,
            outerR * 2.0,
            outerR * 2.0
          )
        )

        // Inner vibrant dot
        val innerR = s * 0.22
        g2d.color = green
        g2d.fill(
          Ellipse2D.Double(
            cx - innerR,
            cy - innerR,
            innerR * 2.0,
            innerR * 2.0
          )
        )
      } else {
        val neutral = UIManager.getColor("Label.disabledForeground")
          ?: Color(140, 140, 140)
        val dotR = s * 0.22
        g2d.color = neutral
        g2d.fill(
          Ellipse2D.Double(
            cx - dotR,
            cy - dotR,
            dotR * 2.0,
            dotR * 2.0
          )
        )
      }
    }
  }

  /**
   * Dynamic theme-aware [ImageIcon] that paints the file document icon
   * using current Look and Feel colors directly at paint time.
   */
  private class FileDocumentIcon(private val iconSize: Int = 16) : ImageIcon() {
    override fun getIconWidth(): Int = iconSize
    override fun getIconHeight(): Int = iconSize

    override fun getImage(): java.awt.Image {
      val image = BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB)
      val g2d = image.createGraphics()
      try {
        g2d.setRenderingHint(
          RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON
        )
        g2d.setRenderingHint(
          RenderingHints.KEY_STROKE_CONTROL,
          RenderingHints.VALUE_STROKE_PURE
        )
        paintDocument(g2d, iconSize)
      } finally {
        g2d.dispose()
      }
      return image
    }

    override fun paintIcon(
      c: java.awt.Component?,
      g: java.awt.Graphics,
      x: Int,
      y: Int
    ) {
      val g2d = g.create() as? Graphics2D ?: return
      try {
        g2d.translate(x, y)
        g2d.setRenderingHint(
          RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON
        )
        g2d.setRenderingHint(
          RenderingHints.KEY_STROKE_CONTROL,
          RenderingHints.VALUE_STROKE_PURE
        )
        paintDocument(g2d, iconSize)
      } finally {
        g2d.dispose()
      }
    }

    private fun paintDocument(g2d: Graphics2D, s: Int) {
      val isDark = try {
        ServiceLocator.themeManager.isDark
      } catch (_: Throwable) {
        val bg = UIManager.getColor("Panel.background") ?: Color.WHITE
        val lum = (0.299 * bg.red + 0.587 * bg.green + 0.114 * bg.blue) / 255.0
        lum < 0.5
      }

      val outlineColor = UIManager.getColor("Label.foreground")
        ?: if (isDark) Color(230, 230, 230) else Color(30, 30, 30)

      val bodyFillColor = if (isDark) {
        Color(255, 255, 255, 25)
      } else {
        Color(255, 255, 255, 230)
      }

      val foldFillColor = if (isDark) {
        Color(255, 255, 255, 50)
      } else {
        Color(210, 210, 210, 240)
      }

      val strokeWidth = (s * 0.09f).coerceAtLeast(1.25f)
      g2d.stroke = BasicStroke(
        strokeWidth,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND
      )

      val left = s * 0.18
      val right = s * 0.82
      val top = s * 0.10
      val bottom = s * 0.90
      val fold = s * 0.26

      // 1. Document body shape with folded top-right corner
      val docPath = Path2D.Double()
      docPath.moveTo(left, top)
      docPath.lineTo(right - fold, top)
      docPath.lineTo(right, top + fold)
      docPath.lineTo(right, bottom)
      docPath.lineTo(left, bottom)
      docPath.closePath()

      // Fill document body
      g2d.color = bodyFillColor
      g2d.fill(docPath)

      // 2. Folded corner flap
      val foldPath = Path2D.Double()
      foldPath.moveTo(right - fold, top)
      foldPath.lineTo(right - fold, top + fold)
      foldPath.lineTo(right, top + fold)
      foldPath.closePath()

      // Fill folded corner flap
      g2d.color = foldFillColor
      g2d.fill(foldPath)

      // Draw document outline
      g2d.color = outlineColor
      g2d.draw(docPath)

      // Draw fold crease line
      val foldOutline = Path2D.Double()
      foldOutline.moveTo(right - fold, top)
      foldOutline.lineTo(right - fold, top + fold)
      foldOutline.lineTo(right, top + fold)
      g2d.draw(foldOutline)

      // 3. Document text lines with high contrast
      val lineStroke = BasicStroke(
        (strokeWidth * 0.9f).coerceAtLeast(1.0f),
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND
      )
      g2d.stroke = lineStroke
      val lineLeft = left + s * 0.15
      val lineRight = right - s * 0.15
      val line1Y = top + fold + s * 0.16
      val line2Y = line1Y + s * 0.18
      g2d.draw(Line2D.Double(lineLeft, line1Y, lineRight, line1Y))
      g2d.draw(
        Line2D.Double(
          lineLeft,
          line2Y,
          lineLeft + (lineRight - lineLeft) * 0.65,
          line2Y
        )
      )
    }
  }

  private abstract class DynamicVectorIcon(
    protected val iconSize: Int
  ) : ImageIcon() {
    override fun getIconWidth(): Int = iconSize
    override fun getIconHeight(): Int = iconSize

    override fun getImage(): java.awt.Image {
      val image = BufferedImage(
        iconSize,
        iconSize,
        BufferedImage.TYPE_INT_ARGB
      )
      val g2d = image.createGraphics()
      try {
        setupHints(g2d)
        paintIconContent(g2d, iconSize, resolveColor(null))
      } finally {
        g2d.dispose()
      }
      return image
    }

    override fun paintIcon(
      c: java.awt.Component?,
      g: java.awt.Graphics,
      x: Int,
      y: Int
    ) {
      val g2d = g.create() as? Graphics2D ?: return
      try {
        g2d.translate(x, y)
        setupHints(g2d)
        paintIconContent(g2d, iconSize, resolveColor(c))
      } finally {
        g2d.dispose()
      }
    }

    protected fun setupHints(g2d: Graphics2D) {
      g2d.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON
      )
      g2d.setRenderingHint(
        RenderingHints.KEY_STROKE_CONTROL,
        RenderingHints.VALUE_STROKE_PURE
      )
    }

    protected fun resolveColor(c: java.awt.Component?): Color {
      return c?.foreground
        ?: UIManager.getColor("Label.foreground")
        ?: UIManager.getColor("Component.foreground")
        ?: Color(128, 128, 128)
    }

    protected abstract fun paintIconContent(
      g2d: Graphics2D,
      s: Int,
      color: Color
    )
  }

  private class ChevronUpIcon(iconSize: Int = 16) :
    DynamicVectorIcon(iconSize) {
    override fun paintIconContent(
      g2d: Graphics2D,
      s: Int,
      color: Color
    ) {
      g2d.stroke = BasicStroke(
        1.8f,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND
      )
      g2d.color = color
      val path = Path2D.Double()
      path.moveTo(s * 0.28, s * 0.62)
      path.lineTo(s * 0.50, s * 0.38)
      path.lineTo(s * 0.72, s * 0.62)
      g2d.draw(path)
    }
  }

  private class ChevronDownIcon(iconSize: Int = 16) :
    DynamicVectorIcon(iconSize) {
    override fun paintIconContent(
      g2d: Graphics2D,
      s: Int,
      color: Color
    ) {
      g2d.stroke = BasicStroke(
        1.8f,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND
      )
      g2d.color = color
      val path = Path2D.Double()
      path.moveTo(s * 0.28, s * 0.38)
      path.lineTo(s * 0.50, s * 0.62)
      path.lineTo(s * 0.72, s * 0.38)
      g2d.draw(path)
    }
  }

  private class ChevronRightIcon(iconSize: Int = 16) :
    DynamicVectorIcon(iconSize) {
    override fun paintIconContent(
      g2d: Graphics2D,
      s: Int,
      color: Color
    ) {
      g2d.stroke = BasicStroke(
        1.8f,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND
      )
      g2d.color = color
      val path = Path2D.Double()
      path.moveTo(s * 0.38, s * 0.28)
      path.lineTo(s * 0.62, s * 0.50)
      path.lineTo(s * 0.38, s * 0.72)
      g2d.draw(path)
    }
  }

  private class ChevronLeftIcon(iconSize: Int = 16) :
    DynamicVectorIcon(iconSize) {
    override fun paintIconContent(
      g2d: Graphics2D,
      s: Int,
      color: Color
    ) {
      g2d.stroke = BasicStroke(
        1.8f,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND
      )
      g2d.color = color
      val path = Path2D.Double()
      path.moveTo(s * 0.62, s * 0.28)
      path.lineTo(s * 0.38, s * 0.50)
      path.lineTo(s * 0.62, s * 0.72)
      g2d.draw(path)
    }
  }

  private class PlusIcon(iconSize: Int = 16) :
    DynamicVectorIcon(iconSize) {
    override fun paintIconContent(
      g2d: Graphics2D,
      s: Int,
      color: Color
    ) {
      g2d.stroke = BasicStroke(
        1.8f,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND
      )
      g2d.color = color
      val mid = s / 2.0
      val start = s * 0.28
      val end = s * 0.72
      g2d.draw(Line2D.Double(start, mid, end, mid))
      g2d.draw(Line2D.Double(mid, start, mid, end))
    }
  }

  private class CloseIcon(iconSize: Int = 16) :
    DynamicVectorIcon(iconSize) {
    override fun paintIconContent(
      g2d: Graphics2D,
      s: Int,
      color: Color
    ) {
      g2d.stroke = BasicStroke(
        1.8f,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND
      )
      g2d.color = color
      g2d.draw(Line2D.Double(s * 0.30, s * 0.30, s * 0.70, s * 0.70))
      g2d.draw(Line2D.Double(s * 0.70, s * 0.30, s * 0.30, s * 0.70))
    }
  }

  /**
   * Dynamic theme-aware [ImageIcon] that paints a folder icon
   * using current Look and Feel colors directly at paint time.
   */
  private class FolderIcon(iconSize: Int = 16) :
    DynamicVectorIcon(iconSize) {
    override fun paintIconContent(
      g2d: Graphics2D,
      s: Int,
      color: Color
    ) {
      val strokeWidth = (s * 0.09f).coerceAtLeast(1.2f)
      g2d.stroke = BasicStroke(
        strokeWidth,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND
      )
      g2d.color = color

      val left = s * 0.12
      val right = s * 0.88
      val top = s * 0.20
      val bottom = s * 0.82
      val tabW = s * 0.32
      val tabH = s * 0.15
      val arc = (s * 0.10).coerceAtLeast(1.5)

      val path = Path2D.Double()
      path.moveTo(left, top + tabH)
      path.lineTo(left, bottom - arc)
      path.quadTo(left, bottom, left + arc, bottom)
      path.lineTo(right - arc, bottom)
      path.quadTo(right, bottom, right, bottom - arc)
      path.lineTo(right, top + tabH + arc)
      path.quadTo(right, top + tabH, right - arc, top + tabH)
      path.lineTo(left + tabW + arc, top + tabH)
      path.lineTo(left + tabW, top + arc)
      path.quadTo(left + tabW - arc * 0.5, top, left + tabW - arc, top)
      path.lineTo(left + arc, top)
      path.quadTo(left, top, left, top + arc)
      path.closePath()

      g2d.draw(path)
    }
  }

  /**
   * Dynamic theme-aware [ImageIcon] that paints a search magnifying glass
   * icon using current Look and Feel foreground colors directly at paint time.
   */
  private class SearchIcon(iconSize: Int = 16) :
    DynamicVectorIcon(iconSize) {
    override fun paintIconContent(
      g2d: Graphics2D,
      s: Int,
      color: Color
    ) {
      val strokeWidth = (s * 0.11f).coerceAtLeast(1.5f)
      g2d.stroke = BasicStroke(
        strokeWidth,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND
      )
      g2d.color = color

      val r = s * 0.28
      val cx = s * 0.42
      val cy = s * 0.42
      g2d.draw(Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r))

      val handleLen = s * 0.30
      val startX = cx + r * 0.7071
      val startY = cy + r * 0.7071
      g2d.draw(
        Line2D.Double(
          startX,
          startY,
          startX + handleLen * 0.7071,
          startY + handleLen * 0.7071
        )
      )
    }
  }

  /**
   * Dynamic theme-aware [ImageIcon] that paints an artist palette icon
   * using current Look and Feel foreground colors directly at paint time.
   */
  private class PaletteIcon(iconSize: Int = 16) :
    DynamicVectorIcon(iconSize) {
    override fun paintIconContent(
      g2d: Graphics2D,
      s: Int,
      color: Color
    ) {
      val strokeWidth = (s * 0.10f).coerceAtLeast(1.2f)
      g2d.stroke = BasicStroke(
        strokeWidth,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND
      )
      g2d.color = color

      val r = s * 0.38
      val cx = s * 0.5
      val cy = s * 0.5
      g2d.draw(Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r))

      val dotR = (s * 0.08).coerceAtLeast(1.2)
      g2d.fill(Ellipse2D.Double(s * 0.36 - dotR, s * 0.36 - dotR, 2 * dotR, 2 * dotR))
      g2d.fill(Ellipse2D.Double(s * 0.64 - dotR, s * 0.36 - dotR, 2 * dotR, 2 * dotR))
      g2d.fill(Ellipse2D.Double(s * 0.50 - dotR, s * 0.64 - dotR, 2 * dotR, 2 * dotR))
    }
  }

  /**
   * Dynamic theme-aware [ImageIcon] that paints a bug / diagnostic report icon
   * using current Look and Feel foreground colors directly at paint time.
   */
  private class BugReportTabIcon(iconSize: Int = 16) :
    DynamicVectorIcon(iconSize) {
    override fun paintIconContent(
      g2d: Graphics2D,
      s: Int,
      color: Color
    ) {
      val strokeWidth = (s * 0.08f).coerceAtLeast(1.2f)
      g2d.stroke = BasicStroke(
        strokeWidth,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND
      )
      g2d.color = color

      val cx = s * 0.50
      val cy = s * 0.54
      val rw = s * 0.22
      val rh = s * 0.28

      // Body ellipse
      g2d.draw(Ellipse2D.Double(cx - rw, cy - rh, rw * 2.0, rh * 2.0))

      // Head arc
      val headR = s * 0.14
      g2d.draw(Ellipse2D.Double(cx - headR, s * 0.18, headR * 2.0, headR * 2.0))

      // Antennae
      g2d.draw(Line2D.Double(cx - headR * 0.5, s * 0.20, cx - headR * 1.5, s * 0.08))
      g2d.draw(Line2D.Double(cx + headR * 0.5, s * 0.20, cx + headR * 1.5, s * 0.08))

      // 3 Pairs of Legs
      // Top legs
      g2d.draw(Line2D.Double(cx - rw, cy - rh * 0.4, cx - s * 0.38, cy - rh * 0.8))
      g2d.draw(Line2D.Double(cx + rw, cy - rh * 0.4, cx + s * 0.38, cy - rh * 0.8))

      // Middle legs
      g2d.draw(Line2D.Double(cx - rw, cy, cx - s * 0.42, cy))
      g2d.draw(Line2D.Double(cx + rw, cy, cx + s * 0.42, cy))

      // Bottom legs
      g2d.draw(Line2D.Double(cx - rw, cy + rh * 0.4, cx - s * 0.38, cy + rh * 0.8))
      g2d.draw(Line2D.Double(cx + rw, cy + rh * 0.4, cx + s * 0.38, cy + rh * 0.8))
    }
  }

  /**
   * Dynamic theme-aware [ImageIcon] that paints a live streaming / radio waves icon
   * using current Look and Feel foreground colors directly at paint time.
   */
  private class LiveLogsTabIcon(iconSize: Int = 16) :
    DynamicVectorIcon(iconSize) {
    override fun paintIconContent(
      g2d: Graphics2D,
      s: Int,
      color: Color
    ) {
      val strokeWidth = (s * 0.09f).coerceAtLeast(1.2f)
      g2d.stroke = BasicStroke(
        strokeWidth,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND
      )
      g2d.color = color

      val cx = s * 0.50
      val cy = s * 0.65

      // Center transmission dot
      val dotR = (s * 0.08).coerceAtLeast(1.2)
      g2d.fill(Ellipse2D.Double(cx - dotR, cy - dotR, dotR * 2.0, dotR * 2.0))

      // Inner radio wave arc
      val r1 = s * 0.24
      val arc1 = java.awt.geom.Arc2D.Double(
        cx - r1, cy - r1, r1 * 2.0, r1 * 2.0, 45.0, 90.0, java.awt.geom.Arc2D.OPEN
      )
      g2d.draw(arc1)

      // Outer radio wave arc
      val r2 = s * 0.40
      val arc2 = java.awt.geom.Arc2D.Double(
        cx - r2, cy - r2, r2 * 2.0, r2 * 2.0, 40.0, 100.0, java.awt.geom.Arc2D.OPEN
      )
      g2d.draw(arc2)

      // Base antenna stem
      g2d.draw(Line2D.Double(cx, cy, cx, s * 0.88))
    }
  }

  /**
   * Dynamic theme-aware [ImageIcon] that paints a filter/funnel icon
   * using current Look and Feel foreground colors directly at paint time.
   */
  private class FilterIcon(iconSize: Int = 16) :
    DynamicVectorIcon(iconSize) {
    override fun paintIconContent(
      g2d: Graphics2D,
      s: Int,
      color: Color
    ) {
      val strokeWidth = (s * 0.08f).coerceAtLeast(1.2f)
      g2d.stroke = BasicStroke(
        strokeWidth,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND
      )
      g2d.color = color

      val scale = s / 16.0
      val path = java.awt.geom.Path2D.Double().apply {
        moveTo(2.5 * scale, 2.5 * scale)
        lineTo(13.5 * scale, 2.5 * scale)
        lineTo(9.5 * scale, 7.5 * scale)
        lineTo(9.5 * scale, 13.5 * scale)
        lineTo(6.5 * scale, 11.5 * scale)
        lineTo(6.5 * scale, 7.5 * scale)
        closePath()
      }
      g2d.draw(path)
    }
  }

  /**
   * Dynamic theme-aware [ImageIcon] that paints a copy icon with two overlapping cards.
   */
  private class CopyIcon(iconSize: Int = 16) :
    DynamicVectorIcon(iconSize) {
    override fun paintIconContent(
      g2d: Graphics2D,
      s: Int,
      color: Color
    ) {
      val strokeWidth = (s * 0.08f).coerceAtLeast(1.2f)
      g2d.stroke = BasicStroke(
        strokeWidth,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND
      )
      g2d.color = color

      val scale = s / 16.0
      val arc = 2.0 * scale

      // Back document outline (top-right)
      val back = Path2D.Double().apply {
        moveTo(6.0 * scale, 2.5 * scale)
        lineTo(13.0 * scale, 2.5 * scale)
        lineTo(13.0 * scale, 10.5 * scale)
      }
      g2d.draw(back)

      // Front document (bottom-left)
      val front = RoundRectangle2D.Double(
        2.5 * scale,
        4.5 * scale,
        8.0 * scale,
        9.0 * scale,
        arc,
        arc
      )
      g2d.draw(front)
    }
  }

  /**
* Theme-aware vector icon rendering a three-dot vertical overflow menu.
   */
  private class MoreVerticalIcon(iconSize: Int = 16) :
    DynamicVectorIcon(iconSize) {
    override fun paintIconContent(
      g2d: Graphics2D,
      s: Int,
      color: Color
    ) {
      g2d.color = color
      val r = (s * 0.09).coerceAtLeast(1.2)
      val cx = s / 2.0
      val y1 = s * 0.25
      val y2 = s * 0.50
      val y3 = s * 0.75
      g2d.fill(Ellipse2D.Double(cx - r, y1 - r, r * 2, r * 2))
      g2d.fill(Ellipse2D.Double(cx - r, y2 - r, r * 2, r * 2))
      g2d.fill(Ellipse2D.Double(cx - r, y3 - r, r * 2, r * 2))
    }
  }

  /**
   * Theme-aware vector icon rendering a floppy disk save symbol.
   */
  private class SaveIcon(iconSize: Int = 16) :
    DynamicVectorIcon(iconSize) {
    override fun paintIconContent(
      g2d: Graphics2D,
      s: Int,
      color: Color
    ) {
      val strokeWidth = (s * 0.08f).coerceAtLeast(1.2f)
      g2d.stroke = BasicStroke(
        strokeWidth,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND
      )
      g2d.color = color

      val scale = s / 16.0
      val pad = 2.0 * scale
      val w = 12.0 * scale
      val h = 12.0 * scale
      val chamfer = 2.5 * scale

      val body = Path2D.Double().apply {
        moveTo(pad, pad)
        lineTo(pad + w - chamfer, pad)
        lineTo(pad + w, pad + chamfer)
        lineTo(pad + w, pad + h)
        lineTo(pad, pad + h)
        closePath()
      }
      g2d.draw(body)

      val shutterPad = pad + 2.0 * scale
      val shutterW = w - 4.0 * scale
      val shutterH = 3.5 * scale
      g2d.draw(RoundRectangle2D.Double(shutterPad, pad, shutterW, shutterH, 1.0, 1.0))

      val labelPad = pad + 1.5 * scale
      val labelW = w - 3.0 * scale
      val labelH = 4.0 * scale
      val labelY = pad + h - labelH
      g2d.draw(RoundRectangle2D.Double(labelPad, labelY, labelW, labelH, 1.0, 1.0))
    }
  }

  /**
   * Theme-aware vector icon rendering a checkmark confirmation symbol.
   */
  private class CheckIcon(iconSize: Int = 16) :
    DynamicVectorIcon(iconSize) {
    override fun paintIconContent(
      g2d: Graphics2D,
      s: Int,
      color: Color
    ) {
      val strokeWidth = (s * 0.12f).coerceAtLeast(1.5f)
      g2d.stroke = BasicStroke(
        strokeWidth,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND
      )
      g2d.color = color

      val scale = s / 16.0
      val check = Path2D.Double().apply {
        moveTo(3.0 * scale, 8.5 * scale)
        lineTo(6.5 * scale, 12.0 * scale)
        lineTo(13.5 * scale, 4.5 * scale)
      }
      g2d.draw(check)
    }
  }

  /**
   * Theme-aware vector icon rendering a clear/cancel slashed-circle symbol.
   */
  private class ClearIcon(iconSize: Int = 16) :
    DynamicVectorIcon(iconSize) {
    override fun paintIconContent(
      g2d: Graphics2D,
      s: Int,
      color: Color
    ) {
      val strokeWidth = (s * 0.09f).coerceAtLeast(1.2f)
      g2d.stroke = BasicStroke(
        strokeWidth,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND
      )
      g2d.color = color

      val scale = s / 16.0
      val cx = s / 2.0
      val cy = s / 2.0
      val r = 5.2 * scale

      g2d.draw(Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r))
      val slashOffset = r * 0.7071
      g2d.draw(
        Line2D.Double(
          cx - slashOffset,
          cy - slashOffset,
          cx + slashOffset,
          cy + slashOffset
        )
      )
    }
  }

  /**
   * Theme-aware vector icon rendering an information circle badge.
   */
  private class InfoIcon(iconSize: Int = 16) :
    DynamicVectorIcon(iconSize) {
    override fun paintIconContent(
      g2d: Graphics2D,
      s: Int,
      color: Color
    ) {
      val strokeWidth = (s * 0.08f).coerceAtLeast(1.2f)
      g2d.stroke = BasicStroke(
        strokeWidth,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND
      )
      g2d.color = color

      val pad = s * 0.12
      val d = s - 2 * pad
      g2d.draw(Ellipse2D.Double(pad, pad, d, d))

      val cx = s / 2.0
      val dotR = (s * 0.05).coerceAtLeast(0.8)
      val dotY = s * 0.32
      g2d.fill(Ellipse2D.Double(cx - dotR, dotY - dotR, dotR * 2, dotR * 2))

      val stemTop = s * 0.44
      val stemBottom = s * 0.70
      g2d.draw(Line2D.Double(cx, stemTop, cx, stemBottom))
    }
  }

  /**
   * Theme-aware vector icon rendering a circular refresh/reload arrow.
   */
  private class RefreshIcon(iconSize: Int = 16) :
    DynamicVectorIcon(iconSize) {
    override fun paintIconContent(
      g2d: Graphics2D,
      s: Int,
      color: Color
    ) {
      val strokeWidth = (s * 0.09f).coerceAtLeast(1.3f)
      g2d.stroke = BasicStroke(
        strokeWidth,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND
      )
      g2d.color = color

      val scale = s / 16.0
      val cx = s / 2.0
      val cy = s / 2.0
      val r = 5.0 * scale

      val arc = java.awt.geom.Arc2D.Double(
        cx - r,
        cy - r,
        r * 2,
        r * 2,
        45.0,
        280.0,
        java.awt.geom.Arc2D.OPEN
      )
      g2d.draw(arc)

      val arrow = Path2D.Double().apply {
        moveTo(cx + 1.0 * scale, cy - r - 2.5 * scale)
        lineTo(cx + 4.5 * scale, cy - r + 0.5 * scale)
        lineTo(cx + 1.5 * scale, cy - r + 3.0 * scale)
      }
      g2d.draw(arrow)
    }
  }

  private fun createIcon(
    size: Int = ICON_SIZE,
    color: Color? = null,
    painter: (Graphics2D, Int, Color) -> Unit
  ): ImageIcon {
    val resolvedColor = color
      ?: UIManager.getColor("Label.foreground")
      ?: UIManager.getColor("Component.foreground")
      ?: Color.WHITE
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g2d = image.createGraphics()
    try {
      g2d.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON
      )
      g2d.setRenderingHint(
        RenderingHints.KEY_STROKE_CONTROL,
        RenderingHints.VALUE_STROKE_PURE
      )
      painter(g2d, size, resolvedColor)
    } finally {
      g2d.dispose()
    }
    return ImageIcon(image)
  }
}
