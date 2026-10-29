package com.tibagni.logviewer.view.color

import com.formdev.flatlaf.FlatClientProperties
import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.theme.LogViewerThemeManager
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.CardBorderUtils
import com.tibagni.logviewer.view.CommonIcons
import java.awt.BasicStroke
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.LinearGradientPaint
import java.awt.MultipleGradientPaint
import java.awt.RenderingHints
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D
import java.awt.geom.RoundRectangle2D
import java.util.function.Consumer
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.JTextField
import javax.swing.SpinnerNumberModel
import javax.swing.SwingConstants
import javax.swing.border.EmptyBorder
import javax.swing.colorchooser.AbstractColorChooserPanel
import javax.swing.colorchooser.ColorSelectionModel
import javax.swing.colorchooser.DefaultColorSelectionModel

/**
 * Modern Kotlin color chooser panel implementing [AbstractColorChooserPanel].
 *
 * Can be installed as a tab inside [javax.swing.JColorChooser] alongside legacy panels
 * (Swatches, HSV, HSL, RGB, CMYK) or used standalone. Features an interactive 2D
 * saturation-brightness canvas, 1D rainbow hue slider, synchronized numeric controls
 * (HEX, RGB, HSB), and quick palette presets.
 *
 * @param initialColor the initial color selected in the chooser.
 * @param themeManager provider of theme information.
 * @param showInternalPreview whether to embed an internal preview card in the panel.
 */
class ModernColorChooserPanel(
  initialColor: Color = Color.RED,
  private val themeManager: LogViewerThemeManager = ServiceLocator.themeManager,
  private val showInternalPreview: Boolean = true
) : AbstractColorChooserPanel() {

  companion object {
    /** Curated vibrant high-contrast swatches for dark theme. */
    val DARK_PRESETS = listOf(
      Color(255, 82, 82),    // Coral Red
      Color(255, 110, 64),   // Orange
      Color(255, 215, 64),   // Amber
      Color(255, 238, 88),   // Yellow
      Color(105, 240, 174),  // Mint Green
      Color(29, 233, 182),   // Teal
      Color(0, 229, 255),    // Cyan
      Color(64, 196, 255),   // Sky Blue
      Color(179, 136, 255),  // Lavender
      Color(224, 64, 251),   // Purple
      Color(255, 64, 129),   // Hot Pink
      Color(224, 224, 224)   // Silver
    )

    /** Curated deep high-contrast swatches for light theme. */
    val LIGHT_PRESETS = listOf(
      Color(198, 40, 40),    // Crimson Red
      Color(216, 67, 21),    // Dark Orange
      Color(130, 119, 23),   // Olive Gold
      Color(46, 125, 50),    // Forest Green
      Color(0, 105, 92),     // Pine Teal
      Color(0, 131, 143),    // Deep Cyan
      Color(21, 101, 192),   // Ocean Blue
      Color(40, 53, 147),    // Indigo
      Color(106, 27, 154),   // Deep Purple
      Color(136, 14, 79),    // Wine Red
      Color(78, 52, 46),     // Chocolate
      Color(55, 71, 79)      // Charcoal
    )
  }

  // Canonical HSB state
  @VisibleForTesting internal var hue: Float = 0f
  @VisibleForTesting internal var saturation: Float = 1f
  @VisibleForTesting internal var brightness: Float = 1f
  private var isUpdatingInternally: Boolean = false
  private var isBuilt: Boolean = false
  private var fallbackModel: ColorSelectionModel? = null

  /** Callback invoked whenever the selected color changes. */
  var onColorChanged: ((Color) -> Unit)? = null

  /**
   * Sets the color change listener with Java-friendly [Consumer] compatibility.
   *
   * @param listener consumer receiving updated [Color]
   */
  fun setOnColorChanged(listener: Consumer<Color>) {
    this.onColorChanged = { listener.accept(it) }
  }

  // Visual Components
  private val sbCanvas = SaturationBrightnessCanvas()
  private val hueSlider = HueSlider()

  @VisibleForTesting internal val hexField = JTextField()
  @VisibleForTesting internal val rSpinner = JSpinner(SpinnerNumberModel(255, 0, 255, 1))
  @VisibleForTesting internal val gSpinner = JSpinner(SpinnerNumberModel(0, 0, 255, 1))
  @VisibleForTesting internal val bSpinner = JSpinner(SpinnerNumberModel(0, 0, 255, 1))
  @VisibleForTesting internal val hSpinner = JSpinner(SpinnerNumberModel(0, 0, 360, 1))
  @VisibleForTesting internal val sSpinner = JSpinner(SpinnerNumberModel(100, 0, 100, 1))
  @VisibleForTesting internal val vSpinner = JSpinner(SpinnerNumberModel(100, 0, 100, 1))

  private val swatchesPanel = JPanel(FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(4), 0))
  @VisibleForTesting internal val swatchButtons = mutableListOf<MiniSwatchButton>()

  /** The uncropped live preview label. */
  val previewLabel: JLabel = object : JLabel(
    I18n.get(I18n.FILTER_PREVIEW_COLOR),
    SwingConstants.CENTER
  ) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      return Dimension(d.width, Math.max(d.height, UIScaleUtils.dip(42)))
    }
  }.apply {
    font = font.deriveFont(Font.BOLD, UIScaleUtils.dipf(13f))
    border = BorderFactory.createCompoundBorder(
      CardBorderUtils.createCardBorder(),
      EmptyBorder(UIScaleUtils.dip(10), UIScaleUtils.dip(18),
        UIScaleUtils.dip(10), UIScaleUtils.dip(18))
    )
  }

  /** The currently selected [Color]. */
  var color: Color
    get() = Color.getHSBColor(hue, saturation, brightness)
    set(value) {
      setColorInternal(value, updateHex = true, updateRgb = true, updateHsb = true)
      val model = colorSelectionModel
      if (model != null && model.selectedColor != value) {
        model.selectedColor = value
      }
    }

  init {
    isOpaque = false
    buildChooser()
    color = initialColor
  }

  /** Returns the display name shown on the chooser panel tab. */
  override fun getDisplayName(): String = "Modern"

  /** Returns the 16x16 palette icon displayed on the chooser tab. */
  override fun getSmallDisplayIcon(): Icon = CommonIcons.paletteIcon(16)

  /** Returns the large display icon, or null as unused. */
  override fun getLargeDisplayIcon(): Icon? = null

  /**
   * Retrieves the active [ColorSelectionModel], falling back to a default model if unparented.
   */
  override fun getColorSelectionModel(): ColorSelectionModel? {
    val superModel = super.getColorSelectionModel()
    if (superModel != null) return superModel
    if (fallbackModel == null) {
      fallbackModel = DefaultColorSelectionModel(Color.RED)
    }
    return fallbackModel
  }

  /**
   * Constructs and arranges visual panels and controls if not already built.
   */
  override fun buildChooser() {
    if (isBuilt) return
    isBuilt = true
    buildUi()
  }

  /**
   * Synchronizes visual sliders and numeric inputs when the underlying selection model changes.
   */
  override fun updateChooser() {
    if (isUpdatingInternally) return
    val selected = colorFromModel ?: return
    val current = Color.getHSBColor(hue, saturation, brightness)
    if (selected != current) {
      setColorInternal(selected, updateHex = true, updateRgb = true, updateHsb = true)
    }
  }

  /**
   * Constructs and arranges the visual panels, numeric inputs, and preview card.
   */
  private fun buildUi() {
    layout = BorderLayout(UIScaleUtils.dip(12), UIScaleUtils.dip(10))

    // Left Column: 2D Canvas + Hue Slider (+ Swatches if showing internal preview)
    val leftCol = JPanel(GridBagLayout()).apply { isOpaque = false }
    leftCol.add(
      sbCanvas,
      GBConstraintsBuilder()
        .withGridx(0).withGridy(0)
        .withWeightx(1.0).withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .withInsets(Insets(0, 0, UIScaleUtils.dip(8), 0))
        .build()
    )
    leftCol.add(
      hueSlider,
      GBConstraintsBuilder()
        .withGridx(0).withGridy(1)
        .withWeightx(1.0)
        .withFill(GridBagConstraints.HORIZONTAL)
        .withInsets(Insets(0, 0, if (showInternalPreview) UIScaleUtils.dip(8) else 0, 0))
        .build()
    )

    buildSwatches()
    swatchesPanel.isOpaque = false
    if (showInternalPreview) {
      leftCol.add(
        swatchesPanel,
        GBConstraintsBuilder()
          .withGridx(0).withGridy(2)
          .withWeightx(1.0)
          .withFill(GridBagConstraints.HORIZONTAL)
          .build()
      )
    }
    add(leftCol, BorderLayout.CENTER)

    // Right Column: Numeric Inputs + (Preview or Swatches)
    val rightCol = JPanel(GridBagLayout()).apply {
      isOpaque = false
      border = CardBorderUtils.createCardBorder()
      preferredSize = Dimension(UIScaleUtils.dip(210), UIScaleUtils.dip(200))
    }

    val innerRight = JPanel(GridBagLayout()).apply {
      isOpaque = false
      border = EmptyBorder(UIScaleUtils.dip(8), UIScaleUtils.dip(10),
        UIScaleUtils.dip(8), UIScaleUtils.dip(10))
    }
    val sec = CardBorderUtils.resolveSecondaryLabelColor()

    // HEX Row
    val hexLbl = JLabel("HEX").apply { // i18n:ignore
      foreground = sec
      font = font.deriveFont(Font.BOLD)
    }
    hexField.font = Font(Font.MONOSPACED, Font.PLAIN, UIScaleUtils.dip(12))
    hexField.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, false)
    hexField.addActionListener { applyHexInput() }
    hexField.addFocusListener(object : FocusListener {
      override fun focusGained(e: FocusEvent?) {}
      override fun focusLost(e: FocusEvent?) = applyHexInput()
    })

    innerRight.add(
      hexLbl,
      GBConstraintsBuilder().withGridx(0).withGridy(0)
        .withAnchor(GridBagConstraints.WEST)
        .withInsets(Insets(0, 0, UIScaleUtils.dip(6), UIScaleUtils.dip(8)))
        .build()
    )
    innerRight.add(
      hexField,
      GBConstraintsBuilder().withGridx(1).withGridy(0)
        .withGridWidth(3)
        .withWeightx(1.0).withFill(GridBagConstraints.HORIZONTAL)
        .withInsets(Insets(0, 0, UIScaleUtils.dip(6), 0))
        .build()
    )

    // RGB Row
    val rgbLbl = JLabel("RGB").apply { // i18n:ignore
      foreground = sec
      font = font.deriveFont(Font.BOLD)
    }
    configureSpinner(rSpinner) { onRgbSpinnersChanged() }
    configureSpinner(gSpinner) { onRgbSpinnersChanged() }
    configureSpinner(bSpinner) { onRgbSpinnersChanged() }

    innerRight.add(
      rgbLbl,
      GBConstraintsBuilder().withGridx(0).withGridy(1)
        .withAnchor(GridBagConstraints.WEST)
        .withInsets(Insets(0, 0, UIScaleUtils.dip(6), UIScaleUtils.dip(8)))
        .build()
    )
    val rgbPanel = JPanel(FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(3), 0)).apply {
      isOpaque = false
      add(rSpinner)
      add(gSpinner)
      add(bSpinner)
    }
    innerRight.add(
      rgbPanel,
      GBConstraintsBuilder().withGridx(1).withGridy(1)
        .withGridWidth(3).withWeightx(1.0).withFill(GridBagConstraints.HORIZONTAL)
        .withInsets(Insets(0, 0, UIScaleUtils.dip(6), 0))
        .build()
    )

    // HSB Row
    val hsbLbl = JLabel("HSB").apply { // i18n:ignore
      foreground = sec
      font = font.deriveFont(Font.BOLD)
    }
    configureSpinner(hSpinner) { onHsbSpinnersChanged() }
    configureSpinner(sSpinner) { onHsbSpinnersChanged() }
    configureSpinner(vSpinner) { onHsbSpinnersChanged() }

    innerRight.add(
      hsbLbl,
      GBConstraintsBuilder().withGridx(0).withGridy(2)
        .withAnchor(GridBagConstraints.WEST)
        .withInsets(Insets(0, 0, UIScaleUtils.dip(8), UIScaleUtils.dip(8)))
        .build()
    )
    val hsbPanel = JPanel(FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(3), 0)).apply {
      isOpaque = false
      add(hSpinner)
      add(sSpinner)
      add(vSpinner)
    }
    innerRight.add(
      hsbPanel,
      GBConstraintsBuilder().withGridx(1).withGridy(2)
        .withGridWidth(3).withWeightx(1.0).withFill(GridBagConstraints.HORIZONTAL)
        .withInsets(Insets(0, 0, UIScaleUtils.dip(8), 0))
        .build()
    )

    // Row 3: Preview or Swatches
    if (showInternalPreview) {
      innerRight.add(
        previewLabel,
        GBConstraintsBuilder().withGridx(0).withGridy(3)
          .withGridWidth(4).withWeightx(1.0).withWeighty(1.0)
          .withFill(GridBagConstraints.BOTH)
          .build()
      )
    } else {
      val presetsTitle = JLabel(I18n.get(I18n.COLOR_CHOOSER_PRESETS)).apply {
        foreground = sec
        font = font.deriveFont(Font.BOLD)
      }
      innerRight.add(
        presetsTitle,
        GBConstraintsBuilder().withGridx(0).withGridy(3)
          .withAnchor(GridBagConstraints.WEST)
          .withInsets(Insets(0, 0, UIScaleUtils.dip(4), UIScaleUtils.dip(8)))
          .build()
      )
      innerRight.add(
        swatchesPanel,
        GBConstraintsBuilder().withGridx(0).withGridy(4)
          .withGridWidth(4).withWeightx(1.0).withWeighty(1.0)
          .withFill(GridBagConstraints.BOTH)
          .build()
      )
    }

    rightCol.add(
      innerRight,
      GBConstraintsBuilder().withGridx(0).withGridy(0)
        .withWeightx(1.0).withWeighty(1.0).withFill(GridBagConstraints.BOTH)
        .build()
    )
    add(rightCol, BorderLayout.EAST)
  }

  /**
   * Configures dimension and change listener callback on numeric spinners.
   *
   * @param spinner the spinner to configure
   * @param onChange callback invoked on user value modification
   */
  private fun configureSpinner(spinner: JSpinner, onChange: () -> Unit) {
    spinner.preferredSize = Dimension(UIScaleUtils.dip(44), UIScaleUtils.dip(24))
    spinner.addChangeListener {
      if (!isUpdatingInternally) {
        onChange()
      }
    }
  }

  /**
   * Reconstructs the mini swatch buttons for the current theme presets.
   */
  private fun buildSwatches() {
    swatchesPanel.removeAll()
    swatchButtons.clear()
    val presets = if (themeManager.isDark) DARK_PRESETS else LIGHT_PRESETS
    presets.forEach { swatchColor ->
      val btn = MiniSwatchButton(swatchColor) {
        color = swatchColor
      }
      swatchButtons.add(btn)
      swatchesPanel.add(btn)
    }
    swatchesPanel.revalidate()
    swatchesPanel.repaint()
  }

  /**
   * Updates state from canonical HSB components, keeping hue intact across black/white values.
   *
   * @param newHue hue component (0..1)
   * @param newSat saturation component (0..1)
   * @param newBri brightness component (0..1)
   * @param updateHex whether to format and update [hexField]
   * @param updateRgb whether to update RGB numeric spinners
   * @param updateHsbSpinners whether to update HSB numeric spinners
   * @param updateCanvas whether to repaint [sbCanvas]
   * @param updateSlider whether to repaint [hueSlider]
   */
  private fun setHsbInternal(
    newHue: Float,
    newSat: Float,
    newBri: Float,
    updateHex: Boolean = true,
    updateRgb: Boolean = true,
    updateHsbSpinners: Boolean = true,
    updateCanvas: Boolean = true,
    updateSlider: Boolean = true
  ) {
    hue = newHue.coerceIn(0f, 1f)
    saturation = newSat.coerceIn(0f, 1f)
    brightness = newBri.coerceIn(0f, 1f)
    val computedColor = Color.getHSBColor(hue, saturation, brightness)

    isUpdatingInternally = true
    try {
      if (updateHex) {
        hexField.text = String.format(
          "#%02X%02X%02X",
          computedColor.red,
          computedColor.green,
          computedColor.blue
        )
      }
      if (updateRgb) {
        rSpinner.value = computedColor.red
        gSpinner.value = computedColor.green
        bSpinner.value = computedColor.blue
      }
      if (updateHsbSpinners) {
        hSpinner.value = (hue * 360f).toInt().coerceIn(0, 360)
        sSpinner.value = (saturation * 100f).toInt().coerceIn(0, 100)
        vSpinner.value = (brightness * 100f).toInt().coerceIn(0, 100)
      }

      val model = colorSelectionModel
      if (model != null && model.selectedColor != computedColor) {
        model.selectedColor = computedColor
      }
    } finally {
      isUpdatingInternally = false
    }

    previewLabel.foreground = computedColor
    if (updateCanvas) sbCanvas.repaint()
    if (updateSlider) hueSlider.repaint()
    onColorChanged?.invoke(computedColor)
  }

  /**
   * Updates state from external [Color] instances, preserving selected hue on grayscale values.
   *
   * @param newColor the target color
   * @param updateHex whether to update [hexField]
   * @param updateRgb whether to update RGB spinners
   * @param updateHsb whether to update HSB spinners
   */
  private fun setColorInternal(
    newColor: Color,
    updateHex: Boolean,
    updateRgb: Boolean,
    updateHsb: Boolean
  ) {
    val hsbVals = FloatArray(3)
    Color.RGBtoHSB(newColor.red, newColor.green, newColor.blue, hsbVals)
    // Preserve existing hue if the color is pure grayscale/white/black where hue is undefined
    val targetHue = if (hsbVals[1] > 0.001f && hsbVals[2] > 0.001f) hsbVals[0] else hue
    setHsbInternal(
      newHue = targetHue,
      newSat = hsbVals[1],
      newBri = hsbVals[2],
      updateHex = updateHex,
      updateRgb = updateRgb,
      updateHsbSpinners = updateHsb
    )
  }

  /**
   * Parses and applies hex text from [hexField], reverting invalid input to current color.
   */
  private fun applyHexInput() {
    val raw = hexField.text.trim().removePrefix("#")
    if (raw.length == 6) {
      try {
        val parsed = Color(raw.toInt(16))
        setColorInternal(parsed, updateHex = false, updateRgb = true, updateHsb = true)
        return
      } catch (_: Exception) {
        // Fall through to restore current color
      }
    }
    val c = color
    hexField.text = String.format("#%02X%02X%02X", c.red, c.green, c.blue)
  }

  /**
   * Synchronizes HSB state when Red, Green, or Blue spinners change.
   */
  private fun onRgbSpinnersChanged() {
    val r = (rSpinner.value as Number).toInt().coerceIn(0, 255)
    val g = (gSpinner.value as Number).toInt().coerceIn(0, 255)
    val b = (bSpinner.value as Number).toInt().coerceIn(0, 255)
    val parsed = Color(r, g, b)
    setColorInternal(parsed, updateHex = true, updateRgb = false, updateHsb = true)
  }

  /**
   * Synchronizes RGB state when Hue, Saturation, or Brightness spinners change.
   */
  private fun onHsbSpinnersChanged() {
    val h = ((hSpinner.value as Number).toFloat() / 360f).coerceIn(0f, 1f)
    val s = ((sSpinner.value as Number).toFloat() / 100f).coerceIn(0f, 1f)
    val v = ((vSpinner.value as Number).toFloat() / 100f).coerceIn(0f, 1f)
    setHsbInternal(h, s, v, updateHsbSpinners = false)
  }

  /**
   * Refreshes presets and swatches when theme Look and Feel changes.
   */
  fun onThemeChanged() {
    buildSwatches()
    repaint()
  }

  /**
   * 2D Canvas component rendering horizontal saturation and vertical brightness gradients.
   */
  private inner class SaturationBrightnessCanvas : JComponent() {
    init {
      isOpaque = false
      cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
      preferredSize = Dimension(UIScaleUtils.dip(220), UIScaleUtils.dip(140))
      minimumSize = Dimension(UIScaleUtils.dip(160), UIScaleUtils.dip(100))

      val mouseListener = object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) = updateFromPoint(e.x, e.y)
        override fun mouseDragged(e: MouseEvent) = updateFromPoint(e.x, e.y)
      }
      addMouseListener(mouseListener)
      addMouseMotionListener(mouseListener)
    }

    /**
     * Updates saturation and brightness coordinates based on mouse position.
     *
     * @param px mouse x position
     * @param py mouse y position
     */
    private fun updateFromPoint(px: Int, py: Int) {
      val w = width.coerceAtLeast(1)
      val h = height.coerceAtLeast(1)
      val newSat = (px.toFloat() / w.toFloat()).coerceIn(0f, 1f)
      val newBri = (1f - (py.toFloat() / h.toFloat())).coerceIn(0f, 1f)
      setHsbInternal(
        newHue = hue,
        newSat = newSat,
        newBri = newBri,
        updateCanvas = true,
        updateSlider = false
      )
    }

    override fun paintComponent(g: Graphics?) {
      if (g == null || width <= 0 || height <= 0) return
      val g2 = g.create() as? Graphics2D ?: return
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val w = width.toFloat()
        val h = height.toFloat()
        val arc = UIScaleUtils.dipf(8f)

        // Clip to rounded card shape
        val clipShape = RoundRectangle2D.Float(0f, 0f, w, h, arc, arc)
        g2.clip(clipShape)

        // 1. Horizontal gradient: White -> Pure Hue Color
        val pureHueColor = Color.getHSBColor(hue, 1f, 1f)
        val hGrad = LinearGradientPaint(
          0f, 0f, w, 0f,
          floatArrayOf(0f, 1f),
          arrayOf(Color.WHITE, pureHueColor)
        )
        g2.paint = hGrad
        g2.fillRect(0, 0, width, height)

        // 2. Vertical gradient: Transparent -> Black
        val vGrad = LinearGradientPaint(
          0f, 0f, 0f, h,
          floatArrayOf(0f, 1f),
          arrayOf(Color(0, 0, 0, 0), Color.BLACK)
        )
        g2.paint = vGrad
        g2.fillRect(0, 0, width, height)

        // 3. Draw border
        g2.clip = null
        g2.color = CardBorderUtils.resolveCardBorderColor()
        g2.stroke = BasicStroke(UIScaleUtils.dipf(1f))
        g2.draw(clipShape)

        // 4. Crosshair indicator
        val cx = (saturation * w).coerceIn(0f, w)
        val cy = ((1f - brightness) * h).coerceIn(0f, h)
        val r = UIScaleUtils.dipf(5f)
        g2.color = Color(0, 0, 0, 180)
        g2.stroke = BasicStroke(UIScaleUtils.dipf(2f))
        g2.draw(Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2))
        g2.color = Color.WHITE
        g2.stroke = BasicStroke(UIScaleUtils.dipf(1.2f))
        g2.draw(Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2))
      } finally {
        g2.dispose()
      }
    }
  }

  /**
   * 1D Rainbow Hue spectrum slider bar.
   */
  private inner class HueSlider : JComponent() {
    init {
      isOpaque = false
      cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
      preferredSize = Dimension(UIScaleUtils.dip(220), UIScaleUtils.dip(16))
      minimumSize = Dimension(UIScaleUtils.dip(160), UIScaleUtils.dip(16))

      val mouseListener = object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) = updateFromPoint(e.x)
        override fun mouseDragged(e: MouseEvent) = updateFromPoint(e.x)
      }
      addMouseListener(mouseListener)
      addMouseMotionListener(mouseListener)
    }

    /**
     * Updates hue degree from horizontal mouse position.
     *
     * @param px mouse x position
     */
    private fun updateFromPoint(px: Int) {
      val w = width.coerceAtLeast(1)
      val newHue = (px.toFloat() / w.toFloat()).coerceIn(0f, 1f)
      setHsbInternal(
        newHue = newHue,
        newSat = saturation,
        newBri = brightness,
        updateCanvas = true,
        updateSlider = true
      )
    }

    override fun paintComponent(g: Graphics?) {
      if (g == null || width <= 0 || height <= 0) return
      val g2 = g.create() as? Graphics2D ?: return
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val w = width.toFloat()
        val h = height.toFloat()
        val arc = UIScaleUtils.dipf(8f)

        val clipShape = RoundRectangle2D.Float(0f, 0f, w, h, arc, arc)
        g2.clip(clipShape)

        // 7-stop rainbow spectrum gradient
        val fractions = floatArrayOf(
          0.0f, 0.166f, 0.333f, 0.5f, 0.666f, 0.833f, 1.0f
        )
        val colors = arrayOf(
          Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
        )
        g2.paint = LinearGradientPaint(
          0f, 0f, w, 0f,
          fractions,
          colors,
          MultipleGradientPaint.CycleMethod.NO_CYCLE
        )
        g2.fillRect(0, 0, width, height)

        g2.clip = null
        g2.color = CardBorderUtils.resolveCardBorderColor()
        g2.stroke = BasicStroke(UIScaleUtils.dipf(1f))
        g2.draw(clipShape)

        // Thumb indicator ring
        val tx = (hue * w).coerceIn(0f, w)
        val thumbR = UIScaleUtils.dipf(6f)
        g2.color = Color(0, 0, 0, 200)
        g2.stroke = BasicStroke(UIScaleUtils.dipf(2f))
        g2.draw(Ellipse2D.Float(tx - thumbR, (h / 2f) - thumbR, thumbR * 2, thumbR * 2))
        g2.color = Color.WHITE
        g2.stroke = BasicStroke(UIScaleUtils.dipf(1.4f))
        g2.draw(Ellipse2D.Float(tx - thumbR, (h / 2f) - thumbR, thumbR * 2, thumbR * 2))
      } finally {
        g2.dispose()
      }
    }
  }

  /**
   * Compact circular swatch button for quick palette selection.
   *
   * @param swatchColor color represented by this mini swatch
   * @param onClick action invoked upon clicking
   */
  internal class MiniSwatchButton(
    val swatchColor: Color,
    val onClick: () -> Unit
  ) : JComponent() {
    init {
      isOpaque = false
      cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
      toolTipText = String.format(
        "#%02X%02X%02X",
        swatchColor.red,
        swatchColor.green,
        swatchColor.blue
      )
      preferredSize = Dimension(UIScaleUtils.dip(16), UIScaleUtils.dip(16))

      addMouseListener(object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
          if (isEnabled && e.button == MouseEvent.BUTTON1) onClick()
        }
      })
    }

    override fun paintComponent(g: Graphics?) {
      if (g == null || width <= 0 || height <= 0) return
      val g2 = g.create() as? Graphics2D ?: return
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val s = width.coerceAtMost(height) - 2f
        val circle = Ellipse2D.Float(1f, 1f, s, s)
        g2.color = swatchColor
        g2.fill(circle)
        g2.color = Color(0, 0, 0, 40)
        g2.stroke = BasicStroke(UIScaleUtils.dipf(0.8f))
        g2.draw(circle)
      } finally {
        g2.dispose()
      }
    }
  }
}
