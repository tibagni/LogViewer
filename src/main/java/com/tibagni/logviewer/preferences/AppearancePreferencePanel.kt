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
import com.tibagni.logviewer.view.CommonIcons
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.UIManager
import javax.swing.border.Border

class AppearancePreferencePanel(
  private val userPrefs: LogViewerPreferences,
  private val themeManager: LogViewerThemeManager
) : PreferencePanel {

  override val id = ID
  override val title: String get() = I18n.get(I18n.PREF_CATEGORY_APPEARANCE)
  override val icon: Icon get() = CommonIcons.paletteIcon(UIScaleUtils.dip(16))

  @VisibleForTesting
  internal val lookAndFeelCbx = JComboBox<String>()
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
    get() {
      val selectedTheme = lookAndFeelCbx.selectedItem as? String
      val effectiveAccent = if (selectedTheme != null &&
        !themeManager.supportsCustomAccentColors(selectedTheme)
      ) {
        "Default"
      } else {
        accentColorPicker.selectedAccentColor
      }
      return selectedTheme != themeManager.currentTheme ||
        effectiveAccent != themeManager.currentAccentColor
    }

  init {
    initAccentColor()
    initLookAndFeel()
    initLogFont()
  }

  private fun initLookAndFeel() {
    themeManager.availableThemes.forEach { lookAndFeelCbx.addItem(it) }
    lookAndFeelCbx.selectedItem = themeManager.currentTheme
    accentColorPicker.previewTheme = themeManager.currentTheme
    accentColorPicker.setCustomAccentsSupported(
      themeManager.supportsCustomAccentColors(themeManager.currentTheme)
    )

    lookAndFeelCbx.addActionListener {
      val selectedTheme = lookAndFeelCbx.selectedItem as? String ?: themeManager.currentTheme
      accentColorPicker.previewTheme = selectedTheme
      accentColorPicker.setCustomAccentsSupported(
        themeManager.supportsCustomAccentColors(selectedTheme)
      )
    }
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

    val selectedAccent = if (selectedTheme != null &&
      !themeManager.supportsCustomAccentColors(selectedTheme)
    ) {
      "Default"
    } else {
      accentColorPicker.selectedAccentColor
    }
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

    // Card 1: Theme & Colors Card
    val themeForm = JPanel(
      FormLayout(
        "fill:pref,left:6dlu:noGrow,fill:default:grow",
        "center:d:noGrow,top:4dlu:noGrow,center:d:noGrow"
      )
    ).apply { isOpaque = false }

    val lookNFeelLbl = JLabel(I18n.get(I18n.PREF_LOOK_AND_FEEL))
    themeForm.add(lookNFeelLbl, cc.xy(1, 1))
    themeForm.add(lookAndFeelCbx, cc.xy(3, 1))

    val accentColorLbl = JLabel(I18n.get(I18n.PREF_ACCENT_COLOR))
    themeForm.add(accentColorLbl, cc.xy(1, 3))
    themeForm.add(accentColorPicker, cc.xy(3, 3))

    val themeCard = createPreferenceCard(
      title = I18n.get(I18n.PREF_SECTION_APPEARANCE_THEME),
      icon = CommonIcons.paletteIcon(UIScaleUtils.dip(16)),
      content = themeForm
    )

    // Card 2: Typography & Preview Card
    val typographyForm = JPanel(
      FormLayout(
        "fill:pref,left:6dlu:noGrow,fill:default:grow",
        "center:d:noGrow,top:4dlu:noGrow,center:d:noGrow,top:4dlu:noGrow," +
          "center:d:noGrow,top:6dlu:noGrow,center:d:noGrow"
      )
    ).apply { isOpaque = false }

    val fontFamilyLbl = JLabel(I18n.get(I18n.PREF_LOG_FONT_FAMILY))
    typographyForm.add(fontFamilyLbl, cc.xy(1, 1))
    typographyForm.add(logFontFamilyCbx, cc.xy(3, 1))

    val fontSizeLbl = JLabel(I18n.get(I18n.PREF_LOG_FONT_SIZE))
    typographyForm.add(fontSizeLbl, cc.xy(1, 3))
    typographyForm.add(logFontSizeCbx, cc.xy(3, 3))

    val boldLogLbl = JLabel(I18n.get(I18n.PREF_LOG_FONT_BOLD))
    bindLabelToCheckbox(boldLogLbl, boldLogTextChbx)
    typographyForm.add(boldLogLbl, cc.xy(1, 5))
    typographyForm.add(boldLogTextChbx, cc.xy(3, 5))

    typographyForm.add(
      fontPreviewTxt,
      cc.xyw(1, 7, 3, CellConstraints.FILL, CellConstraints.DEFAULT)
    )

    val typographyCard = createPreferenceCard(
      title = I18n.get(I18n.PREF_SECTION_APPEARANCE_TYPOGRAPHY),
      icon = CommonIcons.terminalIcon(),
      content = typographyForm
    )

    return JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      isOpaque = false
      add(themeCard)
      add(Box.createVerticalStrut(UIScaleUtils.dip(12)))
      add(typographyCard)
    }
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
