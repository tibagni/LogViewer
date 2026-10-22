package com.tibagni.logviewer.preferences

import com.tibagni.logviewer.theme.LogViewerThemeManager
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.UIManager

/**
 * A circular color button used in [AccentColorPickerPanel] representing a single
 * accent color choice.
 *
 * Renders an antialiased circular swatch with hover tooltip, hand cursor, and:
 * - A filled circle with subtle boundary outline when unselected.
 * - An outer selection ring and crisp antialiased white checkmark when selected.
 * - A diagonal indicator marker when representing the "Default" theme accent color.
 *
 * @param colorName the name of the accent color (e.g. "Default", "Blue", etc.).
 * @param isDefault true if this button represents the default theme accent color.
 * @param themeManager the [LogViewerThemeManager] resolving accent colors and theme states.
 */
class RoundColorButton(
  val colorName: String,
  val isDefault: Boolean = false,
  private val themeManager: LogViewerThemeManager = LogViewerThemeManager
) : JComponent() {

  /**
   * The resolved [Color] for this accent color choice.
   */
  val color: Color get() = themeManager.getColorForAccent(colorName)

  /**
   * Whether this button is currently selected.
   */
  var isSelected: Boolean = false
    set(value) {
      if (field != value) {
        field = value
        repaint()
      }
    }

  /**
   * Callback invoked when this button is activated via mouse click or keyboard.
   */
  var onSelected: (() -> Unit)? = null

  init {
    isOpaque = false
    isFocusable = true
    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    toolTipText = colorName

    addMouseListener(object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent?) {
        if (isEnabled && SwingUtilities.isLeftMouseButton(e)) {
          requestFocusInWindow()
          performClick()
        }
      }
    })

    addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent?) {
        if (isEnabled && (e?.keyCode == KeyEvent.VK_SPACE || e?.keyCode == KeyEvent.VK_ENTER)) {
          performClick()
        }
      }
    })
  }

  /**
   * Triggers this button's selection action programmatically.
   */
  fun performClick() {
    if (isEnabled) {
      onSelected?.invoke()
    }
  }

  override fun getPreferredSize(): Dimension {
    val size = UIScaleUtils.dip(24)
    return Dimension(size, size)
  }

  override fun getMinimumSize(): Dimension = preferredSize

  override fun getMaximumSize(): Dimension = preferredSize

  public override fun paintComponent(g: Graphics?) {
    if (g == null) return
    val g2 = g.create() as? Graphics2D ?: return
    try {
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON
      )
      g2.setRenderingHint(
        RenderingHints.KEY_STROKE_CONTROL,
        RenderingHints.VALUE_STROKE_PURE
      )

      val size = UIScaleUtils.dip(24).toFloat()
      val w = width.toFloat()
      val h = height.toFloat()
      val d = minOf(w, h, size)
      val x = (w - d) / 2f
      val y = (h - d) / 2f

      val btnColor = color
      val circle: Ellipse2D.Float

      if (isSelected) {
        // Outer selection ring
        val ringStroke = UIScaleUtils.dipf(2f)
        val ringDiameter = d - ringStroke
        val ringX = x + ringStroke / 2f
        val ringY = y + ringStroke / 2f

        g2.color = btnColor
        g2.stroke = BasicStroke(ringStroke)
        g2.draw(Ellipse2D.Float(ringX, ringY, ringDiameter, ringDiameter))

        // Inner circle inset with gap
        val gap = UIScaleUtils.dipf(2.5f) + ringStroke
        val innerDiameter = d - gap * 2f
        val innerX = x + gap
        val innerY = y + gap
        circle = Ellipse2D.Float(innerX, innerY, innerDiameter, innerDiameter)

        g2.color = btnColor
        g2.fill(circle)
      } else {
        // Unselected circle
        val inset = UIScaleUtils.dipf(2f)
        val circleDiameter = d - inset * 2f
        val circleX = x + inset
        val circleY = y + inset
        circle = Ellipse2D.Float(circleX, circleY, circleDiameter, circleDiameter)

        g2.color = btnColor
        g2.fill(circle)

        // Subtle boundary outline
        val boundaryStroke = UIScaleUtils.dipf(1f)
        g2.color = if (themeManager.isDark) Color(255, 255, 255, 60) else Color(0, 0, 0, 50)
        g2.stroke = BasicStroke(boundaryStroke)
        g2.draw(
          Ellipse2D.Float(
            circleX + boundaryStroke / 2f,
            circleY + boundaryStroke / 2f,
            circleDiameter - boundaryStroke,
            circleDiameter - boundaryStroke
          )
        )

        // Focus indicator when navigating with keyboard
        if (hasFocus()) {
          val focusColor = UIManager.getColor("Component.focusColor")
            ?: Color(0, 122, 255, 180)
          g2.color = focusColor
          g2.stroke = BasicStroke(UIScaleUtils.dipf(1.5f))
          g2.draw(Ellipse2D.Float(x + 1f, y + 1f, d - 2f, d - 2f))
        }
      }

      // Default theme accent indicator: subtle diagonal line marker
      if (isDefault) {
        val oldClip = g2.clip
        g2.clip(circle)
        g2.color = if (isSelected) Color(255, 255, 255, 140) else Color(255, 255, 255, 200)
        g2.stroke = BasicStroke(
          UIScaleUtils.dipf(1.6f),
          BasicStroke.CAP_ROUND,
          BasicStroke.JOIN_ROUND
        )
        g2.draw(
          Line2D.Float(
            circle.x,
            circle.y + circle.height,
            circle.x + circle.width,
            circle.y
          )
        )
        g2.clip = oldClip
      }

      // Crisp antialiased white checkmark when selected
      if (isSelected) {
        val cx = circle.centerX.toFloat()
        val cy = circle.centerY.toFloat()
        val u = UIScaleUtils.dipf(1f)
        val checkPath = Path2D.Float().apply {
          moveTo(cx - 3.2f * u, cy - 0.2f * u)
          lineTo(cx - 0.8f * u, cy + 2.4f * u)
          lineTo(cx + 3.2f * u, cy - 2.4f * u)
        }
        g2.color = Color.WHITE
        g2.stroke = BasicStroke(
          UIScaleUtils.dipf(1.8f),
          BasicStroke.CAP_ROUND,
          BasicStroke.JOIN_ROUND
        )
        g2.draw(checkPath)
      }
    } finally {
      g2.dispose()
    }
  }
}

/**
 * A horizontal round color picker panel displaying circular color buttons
 * side-by-side for picking FlatLaf theme accent colors.
 *
 * @param themeManager the [LogViewerThemeManager] supplying available accent colors.
 */
class AccentColorPickerPanel(
  private val themeManager: LogViewerThemeManager = LogViewerThemeManager
) : JPanel(FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(6), 0)) {

  private val colorButtons = mutableMapOf<String, RoundColorButton>()

  /**
   * Optional listener invoked whenever a new accent color is selected.
   */
  var onColorSelected: ((String) -> Unit)? = null

  /**
   * The name of the currently selected accent color.
   *
   * Setting this property updates the selected state across all child color buttons.
   * If an unrecognized color is supplied, falls back to selecting "Default".
   */
  var selectedAccentColor: String
    get() = colorButtons.entries.firstOrNull { it.value.isSelected }?.key ?: "Default"
    set(value) {
      val target = if (colorButtons.containsKey(value)) value else "Default"
      colorButtons.forEach { (name, btn) ->
        btn.isSelected = (name == target)
      }
    }

  init {
    isOpaque = false
    themeManager.availableAccentColors.forEach { colorName ->
      val btn = RoundColorButton(
        colorName = colorName,
        isDefault = (colorName == "Default"),
        themeManager = themeManager
      ).apply {
        onSelected = {
          this@AccentColorPickerPanel.selectedAccentColor = colorName
          onColorSelected?.invoke(colorName)
        }
      }
      colorButtons[colorName] = btn
      add(btn)
    }
    selectedAccentColor = themeManager.currentAccentColor
  }

  /**
   * Returns the [RoundColorButton] associated with the specified accent color name,
   * or null if not found.
   *
   * @param colorName the name of the accent color button to retrieve.
   * @return the corresponding [RoundColorButton], or null.
   */
  fun getButton(colorName: String): RoundColorButton? = colorButtons[colorName]
}
