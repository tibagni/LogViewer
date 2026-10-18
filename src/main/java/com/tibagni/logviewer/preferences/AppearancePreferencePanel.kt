package com.tibagni.logviewer.preferences

import com.jgoodies.forms.layout.CellConstraints
import com.jgoodies.forms.layout.FormLayout
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.theme.LogViewerThemeManager
import com.tibagni.logviewer.util.LogFontManager
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import java.awt.Dimension
import java.awt.Font
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.JTextField

class AppearancePreferencePanel(
  private val userPrefs: LogViewerPreferences,
  private val themeManager: LogViewerThemeManager
) : PreferencePanel {

  override val id = ID
  override val title: String get() = I18n.get(I18n.PREF_CATEGORY_APPEARANCE)

  private val lookAndFeelCbx = JComboBox<String>()
  private val logFontSizeCbx = JComboBox<Int>()
  private val logFontFamilyCbx = JComboBox<String>()
  private val boldLogTextChbx = JCheckBox()
  private val fontPreviewTxt = object : JTextField("Preview: 0123456789 ABCabc") { // no-i18n
    override fun getMinimumSize(): Dimension {
      val min = super.getMinimumSize()
      min.width = UIScaleUtils.dip(80)
      return min
    }

    override fun getPreferredSize(): Dimension {
      val pref = super.getPreferredSize()
      pref.width = UIScaleUtils.dip(150)
      return pref
    }
  }.apply {
    isEditable = false
  }

  override val panel: JComponent = buildPanel()

  override val hasTimeConsumingOperation: Boolean
    get() = (lookAndFeelCbx.selectedItem as? String) != themeManager.currentTheme

  init {
    initLookAndFeel()
    initLogFont()
  }

  private fun initLookAndFeel() {
    themeManager.availableThemes.forEach { lookAndFeelCbx.addItem(it) }
    lookAndFeelCbx.selectedItem = themeManager.currentTheme
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
    val family = (logFontFamilyCbx.selectedItem as? String) ?: userPrefs.logFontFamily
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
        "center:d:noGrow,top:4dlu:noGrow,center:d:noGrow,top:4dlu:noGrow,center:d:noGrow,top:4dlu:noGrow,center:d:noGrow,top:4dlu:noGrow,center:d:noGrow,top:4dlu:noGrow,center:d:noGrow"
      )
    ).apply { isOpaque = false }

    // Row 1: Look and Feel
    val lookNFeelLbl = JLabel(I18n.get(I18n.PREF_LOOK_AND_FEEL))
    form.add(lookNFeelLbl, cc.xy(1, 1))
    form.add(lookAndFeelCbx, cc.xy(3, 1))

    // Row 3: Separator
    form.add(JSeparator(), cc.xyw(1, 3, 3, CellConstraints.FILL, CellConstraints.DEFAULT))

    // Row 5: Log Font Family
    val fontFamilyLbl = JLabel(I18n.get(I18n.PREF_LOG_FONT_FAMILY))
    form.add(fontFamilyLbl, cc.xy(1, 5))
    form.add(logFontFamilyCbx, cc.xy(3, 5))

    // Row 7: Log Font Size
    val fontSizeLbl = JLabel(I18n.get(I18n.PREF_LOG_FONT_SIZE))
    form.add(fontSizeLbl, cc.xy(1, 7))
    form.add(logFontSizeCbx, cc.xy(3, 7))

    // Row 9: Bold Log Text
    val boldLogLbl = JLabel(I18n.get(I18n.PREF_LOG_FONT_BOLD))
    bindLabelToCheckbox(boldLogLbl, boldLogTextChbx)
    form.add(boldLogLbl, cc.xy(1, 9))
    form.add(boldLogTextChbx, cc.xy(3, 9))

    // Row 11: Font Preview
    form.add(fontPreviewTxt, cc.xyw(1, 11, 3, CellConstraints.FILL, CellConstraints.DEFAULT))

    return form
  }

  companion object {
    const val ID = "appearance"
    private val SUPPORTED_FONT_SIZES = arrayOf(
      8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 32
    )
  }
}
