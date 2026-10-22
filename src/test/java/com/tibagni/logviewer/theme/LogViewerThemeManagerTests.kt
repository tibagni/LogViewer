package com.tibagni.logviewer.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import java.awt.Color
import java.awt.Dimension
import java.awt.Insets
import javax.swing.UIManager

class LogViewerThemeManagerTests {

  @Test
  fun testAvailableThemes() {
    val themes = LogViewerThemeManager.availableThemes
    assertTrue(themes.contains("Light"))
    assertTrue(themes.contains("Dark"))
    assertTrue(themes.contains("macOS Light"))
    assertTrue(themes.contains("macOS Dark"))
    assertTrue(themes.contains("IntelliJ Light"))
    assertTrue(themes.contains("Darcula"))
    assertEquals(6, themes.size)
  }

  @Test
  fun testThemeInstallationLight() {
    LogViewerThemeManager.currentTheme = "Light"
    assertEquals("Light", LogViewerThemeManager.currentTheme)
    assertFalse(LogViewerThemeManager.isDark)
  }

  @Test
  fun testThemeInstallationDark() {
    LogViewerThemeManager.currentTheme = "Dark"
    assertEquals("Dark", LogViewerThemeManager.currentTheme)
    assertTrue(LogViewerThemeManager.isDark)
  }

  @Test
  fun testThemeInstallationMacLight() {
    LogViewerThemeManager.currentTheme = "macOS Light"
    assertEquals("macOS Light", LogViewerThemeManager.currentTheme)
    assertFalse(LogViewerThemeManager.isDark)
  }

  @Test
  fun testThemeInstallationMacDark() {
    LogViewerThemeManager.currentTheme = "macOS Dark"
    assertEquals("macOS Dark", LogViewerThemeManager.currentTheme)
    assertTrue(LogViewerThemeManager.isDark)
  }

  @Test
  fun testThemeInstallationIntelliJ() {
    LogViewerThemeManager.currentTheme = "IntelliJ Light"
    assertEquals("IntelliJ Light", LogViewerThemeManager.currentTheme)
    assertFalse(LogViewerThemeManager.isDark)
  }

  @Test
  fun testThemeInstallationDarcula() {
    LogViewerThemeManager.currentTheme = "Darcula"
    assertEquals("Darcula", LogViewerThemeManager.currentTheme)
    assertTrue(LogViewerThemeManager.isDark)
  }

  @Test
  fun testUiPropertiesDefaults() {
    LogViewerThemeManager.currentTheme = "Light"

    assertEquals(8, UIManager.get("Component.arc"))
    assertEquals(8, UIManager.get("Button.arc"))
    assertEquals(8, UIManager.get("TextComponent.arc"))
    assertEquals(999, UIManager.get("ProgressBar.arc"))
    assertEquals(1000, UIManager.get("ProgressBar.cycleTime"))
    assertEquals("chevron", UIManager.get("Component.arrowType"))
    assertEquals(4, UIManager.get("MenuItem.selectionArc"))
    assertEquals(4, UIManager.get("ComboBox.selectionArc"))
    assertEquals(4, UIManager.get("List.selectionArc"))
    assertEquals(4, UIManager.get("Table.selectionArc"))
    assertEquals(UIScaleUtils.dip(4), UIManager.get("SplitPane.dividerSize"))
    assertEquals(true, UIManager.get("SplitPane.centerOneTouchButtons"))
    assertEquals("underlined", UIManager.get("TabbedPane.tabType"))
    assertEquals(6, UIManager.get("ToolTip.arc"))
    assertEquals(6, UIManager.get("ScrollBar.thumbArc"))
    assertEquals(6, UIManager.get("ScrollBar.trackArc"))
    assertEquals(Insets(2, 2, 2, 2), UIManager.get("ScrollBar.thumbInsets"))
    assertEquals(UIScaleUtils.dip(10), UIManager.get("ScrollBar.width"))
    assertEquals(false, UIManager.get("ScrollBar.showButtons"))
    assertEquals(true, UIManager.get("Popup.dropShadowPainted"))
    assertEquals(true, UIManager.get("TabbedPane.showTabSeparators"))
    assertEquals(false, UIManager.get("TabbedPane.tabSeparatorsFullHeight"))
    assertEquals(false, UIManager.get("Table.showVerticalLines"))
    assertEquals(Dimension(0, 1), UIManager.get("Table.intercellSpacing"))
    assertEquals(UIScaleUtils.dip(26), UIManager.get("TableHeader.height"))
    val expectedMargins = Insets(
      UIScaleUtils.dip(2), UIScaleUtils.dip(6),
      UIScaleUtils.dip(2), UIScaleUtils.dip(6)
    )
    assertEquals(expectedMargins, UIManager.get("TableHeader.cellMargins"))
    assertEquals(expectedMargins, UIManager.get("Table.cellMargins"))
    assertEquals(false, UIManager.get("TableHeader.showTrailingVerticalLine"))
    assertEquals(true, UIManager.get("TitlePane.unifiedBackground"))
    assertEquals(true, UIManager.get("TitlePane.menuBarEmbedded"))
  }

  /**
   * Tests table header height, cell margins, and trailing vertical line
   * suppression configured in application UI defaults.
   */
  @Test
  fun testTableHeaderAndCellMetrics() {
    LogViewerThemeManager.currentTheme = "Light"

    val expectedMargins = Insets(
      UIScaleUtils.dip(2), UIScaleUtils.dip(6),
      UIScaleUtils.dip(2), UIScaleUtils.dip(6)
    )
    assertEquals(UIScaleUtils.dip(26), UIManager.get("TableHeader.height"))
    assertEquals(expectedMargins, UIManager.get("TableHeader.cellMargins"))
    assertEquals(expectedMargins, UIManager.get("Table.cellMargins"))
    assertEquals(false, UIManager.get("TableHeader.showTrailingVerticalLine"))
  }

  @Test
  fun testInvalidThemeKeepsCurrentOrFallsBack() {
    LogViewerThemeManager.currentTheme = "Light"
    assertEquals("Light", LogViewerThemeManager.currentTheme)

    LogViewerThemeManager.currentTheme = "NonExistentTheme"
    // When a theme is already installed, invalid theme should be ignored
    assertEquals("Light", LogViewerThemeManager.currentTheme)
  }

  @Test
  fun testAvailableAccentColors() {
    val expected = listOf(
      "Default",
      "Blue",
      "Purple",
      "Teal",
      "Red",
      "Orange",
      "Green"
    )
    assertEquals(expected, LogViewerThemeManager.availableAccentColors)
  }

  @Test
  fun testSettingAccentColor() {
    LogViewerThemeManager.setAccentColor("Blue")
    assertEquals("Blue", LogViewerThemeManager.currentAccentColor)

    LogViewerThemeManager.setAccentColor("Purple")
    assertEquals("Purple", LogViewerThemeManager.currentAccentColor)

    LogViewerThemeManager.setAccentColor("Default")
    assertEquals("Default", LogViewerThemeManager.currentAccentColor)
  }

  @Test
  fun testSettingUnknownAccentColorFallback() {
    LogViewerThemeManager.setAccentColor("UnknownColor")
    assertEquals("Default", LogViewerThemeManager.currentAccentColor)
  }

  @Test
  fun testDefaultAccentColorResolvesProperly() {
    LogViewerThemeManager.currentTheme = "Light"
    LogViewerThemeManager.setAccentColor("Default")
    val lightDefault = LogViewerThemeManager.getDefaultAccentColor()
    assertEquals(Color.decode("#2675BF"), lightDefault)

    LogViewerThemeManager.currentTheme = "Dark"
    val darkDefault = LogViewerThemeManager.getDefaultAccentColor()
    assertEquals(Color.decode("#4B6EAF"), darkDefault)

    LogViewerThemeManager.currentTheme = "macOS Light"
    val macDefault = LogViewerThemeManager.getDefaultAccentColor()
    assertEquals(Color.decode("#007AFF"), macDefault)
  }

  @Test
  fun testColorForAccentKnownAndFallback() {
    assertEquals(Color.decode("#2670E8"), LogViewerThemeManager.getColorForAccent("Blue"))
    assertEquals(Color.decode("#7C4DFF"), LogViewerThemeManager.getColorForAccent("Purple"))
    assertEquals(Color.decode("#00BFA5"), LogViewerThemeManager.getColorForAccent("Teal"))
    assertEquals(Color.decode("#E53935"), LogViewerThemeManager.getColorForAccent("Red"))
    assertEquals(Color.decode("#FB8C00"), LogViewerThemeManager.getColorForAccent("Orange"))
    assertEquals(Color.decode("#43A047"), LogViewerThemeManager.getColorForAccent("Green"))

    // "Default" matches getDefaultAccentColor()
    assertEquals(
      LogViewerThemeManager.getDefaultAccentColor(),
      LogViewerThemeManager.getColorForAccent("Default")
    )

    // Unrecognized accent color falls back to getDefaultAccentColor()
    assertEquals(
      LogViewerThemeManager.getDefaultAccentColor(),
      LogViewerThemeManager.getColorForAccent("NonExistentColor")
    )
  }
}
