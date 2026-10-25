package com.tibagni.logviewer.preferences

import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.ui.FlatLineBorder
import com.jgoodies.forms.layout.CellConstraints
import com.jgoodies.forms.layout.FormLayout
import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.theme.LogViewerThemeManager
import com.tibagni.logviewer.util.LogFontManager
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.CardBorderUtils
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.JTextField
import javax.swing.UIManager
import javax.swing.border.Border

class AppearancePreferencePanel(
  private val userPrefs: LogViewerPreferences,
  private val themeManager: LogViewerThemeManager
) : PreferencePanel {

  override val id = ID
  override val title: String get() = I18n.get(I18n.PREF_CATEGORY_APPEARANCE)

  private val lookAndFeelCbx = JComboBox<String>()
  internal val accentColorPicker = AccentColorPickerPanel(themeManager)
  private val logFontSizeCbx = JComboBox<Int>()
  private val logFontFamilyCbx = JComboBox<String>()
  private val boldLogTextChbx = JCheckBox()

  @VisibleForTesting
  internal val fontPreviewTxt = object :
    JTextField(SAMPLE_PREVIEW_TEXT) {
    override fun getMinimumSize(): Dimension {
      val min = super.getMinimumSize()
      min.width = UIScaleUtils.dip(100)
      return min
    }

    override fun getPreferredSize(): Dimension {
      val pref = super.getPreferredSize()
      pref.width = UIScaleUtils.dip(200)
      return pref
    }

    override fun updateUI() {
      super.updateUI()
      try {
        border = createPreviewCompoundBorder()
      } catch (_: Exception) {
        // Fallback during super-constructor initialization
      }
    }
  }.apply {
    isEditable = false
    horizontalAlignment = JTextField.CENTER
    border = createPreviewCompoundBorder()
  }

  /**
   * The name of the accent color selected in the picker.
   */
  var selectedAccentColor: String
    get() = accentColorPicker.selectedAccentColor
    set(value) {
      accentColorPicker.selectedAccentColor = value
    }

  /**
   * Resolves the border color for the typography preview card.
   *
   * @return theme-adaptive [Color] for the preview card border
   */
  internal fun resolvePreviewBorderColor(): Color =
    CardBorderUtils.resolveCardBorderColor()

  /**
   * Creates a compound border for the font preview card consisting of
   * an 8dp rounded [Border] and subtle [EmptyBorder] padding.
   *
   * @return configured compound [Border]
   */
  internal fun createPreviewCompoundBorder(): Border {
    val roundedBorder = CardBorderUtils.createCardBorder(
      insets = Insets(0, 0, 0, 0)
    )
    val paddingBorder = BorderFactory.createEmptyBorder(
      UIScaleUtils.dip(10),
      UIScaleUtils.dip(14),
      UIScaleUtils.dip(10),
      UIScaleUtils.dip(14)
    )
    return BorderFactory.createCompoundBorder(roundedBorder, paddingBorder)
  }

  override val panel: JComponent = buildPanel()

  override val hasTimeConsumingOperation: Boolean
    get() = (lookAndFeelCbx.selectedItem as? String) !=
      themeManager.currentTheme ||
      accentColorPicker.selectedAccentColor !=
      themeManager.currentAccentColor

  init {
    initLookAndFeel()
    initAccentColor()
    initLogFont()
  }

  private fun initLookAndFeel() {
    themeManager.availableThemes.forEach { lookAndFeelCbx.addItem(it) }
    lookAndFeelCbx.selectedItem = themeManager.currentTheme
  }

  /**
   * Initializes the accent color picker and selects the currently
   * configured accent color.
   */
  private fun initAccentColor() {
    accentColorPicker.selectedAccentColor = userPrefs.accentColor
  }

  private fun initLogFont() {
    SUPPORTED_FONT_SIZES.forEach { logFontSizeCbx.addItem(it) }
    logFontSizeCbx.selectedItem = userPrefs.logFontSize

    LogFontManager.getSupportedFonts().forEach { logFontFamilyCbx.addItem(it) }
    logFontFamilyCbx.selectedItem = userPrefs.logFontFamily

    boldLogTextChbx.isSelected = userPrefs.logFontBold
    updateFontPreview()

    logFontFamilyCbx.addActionListener { updateFontPreview() }
    logFontSizeCbx.addActionListener { updateFontPreview() }
    boldLogTextChbx.addActionListener { updateFontPreview() }
  }

  private fun updateFontPreview() {
    val family = (logFontFamilyCbx.selectedItem as? String)
      ?: userPrefs.logFontFamily
    val size = (logFontSizeCbx.selectedItem as? Int) ?: userPrefs.logFontSize
    val bold = boldLogTextChbx.isSelected
    val style = if (bold) Font.BOLD else Font.PLAIN
    fontPreviewTxt.font = LogFontManager.getFont(family, style, size)
  }

  override fun apply() {
    val selectedTheme = lookAndFeelCbx.selectedItem as? String
    if (selectedTheme != null && selectedTheme != themeManager.currentTheme) {
      userPrefs.lookAndFeel = selectedTheme
    }

    val selectedAccent = accentColorPicker.selectedAccentColor
    if (selectedAccent != userPrefs.accentColor) {
      userPrefs.accentColor = selectedAccent
    }

    val selectedFont = logFontFamilyCbx.selectedItem as? String
    if (selectedFont != null && selectedFont != userPrefs.logFontFamily) {
      userPrefs.logFontFamily = selectedFont
    }

    val selectedSize = logFontSizeCbx.selectedItem as? Int
    if (selectedSize != null && selectedSize != userPrefs.logFontSize) {
      userPrefs.logFontSize = selectedSize
    }

    if (boldLogTextChbx.isSelected != userPrefs.logFontBold) {
      userPrefs.logFontBold = boldLogTextChbx.isSelected
    }
  }

  private fun buildPanel(): JComponent {
    val cc = CellConstraints()
    val form = JPanel(
      FormLayout(
        "fill:pref,left:6dlu:noGrow,fill:default:grow",
        "center:d:noGrow,top:4dlu:noGrow,center:d:noGrow,top:4dlu:noGrow," +
          "center:d:noGrow,top:4dlu:noGrow,center:d:noGrow,top:4dlu:noGrow," +
          "center:d:noGrow,top:4dlu:noGrow,center:d:noGrow,top:6dlu:noGrow," +
          "center:d:noGrow"
      )
    ).apply { isOpaque = false }

    // Row 1: Look and Feel
    val lookNFeelLbl = JLabel(I18n.get(I18n.PREF_LOOK_AND_FEEL))
    form.add(lookNFeelLbl, cc.xy(1, 1))
    form.add(lookAndFeelCbx, cc.xy(3, 1))

    // Row 3: Accent Color
    val accentColorLbl = JLabel(I18n.get(I18n.PREF_ACCENT_COLOR))
    form.add(accentColorLbl, cc.xy(1, 3))
    form.add(accentColorPicker, cc.xy(3, 3))

    // Row 5: Separator
    form.add(
      JSeparator(),
      cc.xyw(1, 5, 3, CellConstraints.FILL, CellConstraints.DEFAULT)
    )

    // Row 7: Log Font Family
    val fontFamilyLbl = JLabel(I18n.get(I18n.PREF_LOG_FONT_FAMILY))
    form.add(fontFamilyLbl, cc.xy(1, 7))
    form.add(logFontFamilyCbx, cc.xy(3, 7))

    // Row 9: Log Font Size
    val fontSizeLbl = JLabel(I18n.get(I18n.PREF_LOG_FONT_SIZE))
    form.add(fontSizeLbl, cc.xy(1, 9))
    form.add(logFontSizeCbx, cc.xy(3, 9))

    // Row 11: Bold Log Text
    val boldLogLbl = JLabel(I18n.get(I18n.PREF_LOG_FONT_BOLD))
    bindLabelToCheckbox(boldLogLbl, boldLogTextChbx)
    form.add(boldLogLbl, cc.xy(1, 11))
    form.add(boldLogTextChbx, cc.xy(3, 11))

    // Row 13: Font Preview
    form.add(
      fontPreviewTxt,
      cc.xyw(1, 13, 3, CellConstraints.FILL, CellConstraints.DEFAULT)
    )

    return form
  }

  companion object {
    const val ID = "appearance"

    /**
     * Standard readable pangram sample text displayed in the font preview card.
     */
    const val SAMPLE_PREVIEW_TEXT =
      "The quick brown fox jumps over the lazy dog 0123456789"

    private val SUPPORTED_FONT_SIZES = arrayOf(
      8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 32
    )
  }
}
