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
    assertTrue(themes.contains("IntelliJ Darcula"))
    assertTrue(themes.contains("Nord"))
    assertTrue(themes.contains("One Dark"))
    assertTrue(themes.contains("Dracula"))
    assertTrue(themes.contains("Gruvbox Dark"))
    assertTrue(themes.contains("Solarized Dark"))
    assertTrue(themes.contains("Solarized Light"))
    assertTrue(themes.contains("Arc Light"))
    assertTrue(themes.contains("Arc Dark"))
    assertEquals(14, themes.size)
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
  fun testThemeInstallationIntelliJDarcula() {
    LogViewerThemeManager.currentTheme = "IntelliJ Darcula"
    assertEquals("IntelliJ Darcula", LogViewerThemeManager.currentTheme)
    assertTrue(LogViewerThemeManager.isDark)
  }

  @Test
  fun testThemeInstallationDarculaLegacyAlias() {
    LogViewerThemeManager.currentTheme = "Darcula"
    assertEquals("IntelliJ Darcula", LogViewerThemeManager.currentTheme)
    assertTrue(LogViewerThemeManager.isDark)
  }

  @Test
  fun testThemeInstallationNord() {
    LogViewerThemeManager.currentTheme = "Nord"
    assertEquals("Nord", LogViewerThemeManager.currentTheme)
    assertTrue(LogViewerThemeManager.isDark)
  }

  @Test
  fun testThemeInstallationOneDark() {
    LogViewerThemeManager.currentTheme = "One Dark"
    assertEquals("One Dark", LogViewerThemeManager.currentTheme)
    assertTrue(LogViewerThemeManager.isDark)
  }

  @Test
  fun testThemeInstallationDracula() {
    LogViewerThemeManager.currentTheme = "Dracula"
    assertEquals("Dracula", LogViewerThemeManager.currentTheme)
    assertTrue(LogViewerThemeManager.isDark)
  }

  @Test
  fun testThemeInstallationGruvboxDark() {
    LogViewerThemeManager.currentTheme = "Gruvbox Dark"
    assertEquals("Gruvbox Dark", LogViewerThemeManager.currentTheme)
    assertTrue(LogViewerThemeManager.isDark)
  }

  @Test
  fun testThemeInstallationSolarizedDark() {
    LogViewerThemeManager.currentTheme = "Solarized Dark"
    assertEquals("Solarized Dark", LogViewerThemeManager.currentTheme)
    assertTrue(LogViewerThemeManager.isDark)
  }

  @Test
  fun testThemeInstallationSolarizedLight() {
    LogViewerThemeManager.currentTheme = "Solarized Light"
    assertEquals("Solarized Light", LogViewerThemeManager.currentTheme)
    assertFalse(LogViewerThemeManager.isDark)
  }

  @Test
  fun testThemeInstallationArcLight() {
    LogViewerThemeManager.currentTheme = "Arc Light"
    assertEquals("Arc Light", LogViewerThemeManager.currentTheme)
    assertFalse(LogViewerThemeManager.isDark)
  }

  @Test
  fun testThemeInstallationArcDark() {
    LogViewerThemeManager.currentTheme = "Arc Dark"
    assertEquals("Arc Dark", LogViewerThemeManager.currentTheme)
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

    LogViewerThemeManager.currentTheme = "Nord"
    assertEquals(Color.decode("#88C0D0"), LogViewerThemeManager.getDefaultAccentColor())

    LogViewerThemeManager.currentTheme = "One Dark"
    assertEquals(Color.decode("#568AF2"), LogViewerThemeManager.getDefaultAccentColor())

    LogViewerThemeManager.currentTheme = "Dracula"
    assertEquals(Color.decode("#BD93F9"), LogViewerThemeManager.getDefaultAccentColor())

    LogViewerThemeManager.currentTheme = "Gruvbox Dark"
    assertEquals(Color.decode("#4C87C8"), LogViewerThemeManager.getDefaultAccentColor())

    LogViewerThemeManager.currentTheme = "Solarized Dark"
    assertEquals(Color.decode("#3985C7"), LogViewerThemeManager.getDefaultAccentColor())

    LogViewerThemeManager.currentTheme = "Solarized Light"
    assertEquals(Color.decode("#3D7EC0"), LogViewerThemeManager.getDefaultAccentColor())

    LogViewerThemeManager.currentTheme = "Arc Dark"
    assertEquals(Color.decode("#2679DB"), LogViewerThemeManager.getDefaultAccentColor())

    // Parameterized theme querying
    assertEquals(Color.decode("#88C0D0"), LogViewerThemeManager.getDefaultAccentColor("Nord"))
    assertEquals(Color.decode("#BD93F9"), LogViewerThemeManager.getDefaultAccentColor("Dracula"))
  }

  @Test
  fun testIsThemeDarkResolution() {
    assertTrue(LogViewerThemeManager.isThemeDark("Dracula"))
    assertTrue(LogViewerThemeManager.isThemeDark("Darcula"))
    assertTrue(LogViewerThemeManager.isThemeDark("IntelliJ Darcula"))
    assertTrue(LogViewerThemeManager.isThemeDark("Nord"))
    assertTrue(LogViewerThemeManager.isThemeDark("One Dark"))
    assertTrue(LogViewerThemeManager.isThemeDark("Gruvbox Dark"))
    assertTrue(LogViewerThemeManager.isThemeDark("Solarized Dark"))
    assertTrue(LogViewerThemeManager.isThemeDark("Arc Dark"))
    assertTrue(LogViewerThemeManager.isThemeDark("Dark"))
    assertTrue(LogViewerThemeManager.isThemeDark("macOS Dark"))

    assertFalse(LogViewerThemeManager.isThemeDark("Light"))
    assertFalse(LogViewerThemeManager.isThemeDark("IntelliJ Light"))
    assertFalse(LogViewerThemeManager.isThemeDark("Solarized Light"))
    assertFalse(LogViewerThemeManager.isThemeDark("Arc Light"))
    assertFalse(LogViewerThemeManager.isThemeDark("macOS Light"))
  }

  @Test
  fun testSupportsCustomAccentColors() {
    assertTrue(LogViewerThemeManager.supportsCustomAccentColors("Light"))
    assertTrue(LogViewerThemeManager.supportsCustomAccentColors("Dark"))
    assertTrue(LogViewerThemeManager.supportsCustomAccentColors("macOS Light"))
    assertTrue(LogViewerThemeManager.supportsCustomAccentColors("macOS Dark"))
    assertTrue(LogViewerThemeManager.supportsCustomAccentColors("IntelliJ Light"))
    assertTrue(LogViewerThemeManager.supportsCustomAccentColors("IntelliJ Darcula"))
    assertTrue(LogViewerThemeManager.supportsCustomAccentColors("Darcula"))

    assertFalse(LogViewerThemeManager.supportsCustomAccentColors("Nord"))
    assertFalse(LogViewerThemeManager.supportsCustomAccentColors("One Dark"))
    assertFalse(LogViewerThemeManager.supportsCustomAccentColors("Dracula"))
    assertFalse(LogViewerThemeManager.supportsCustomAccentColors("Gruvbox Dark"))
    assertFalse(LogViewerThemeManager.supportsCustomAccentColors("Solarized Dark"))
    assertFalse(LogViewerThemeManager.supportsCustomAccentColors("Solarized Light"))
    assertFalse(LogViewerThemeManager.supportsCustomAccentColors("Arc Light"))
    assertFalse(LogViewerThemeManager.supportsCustomAccentColors("Arc Dark"))
  }

  @Test
  fun testSettingAccentColorOnUnsupportedThemeFallsBackToDefault() {
    LogViewerThemeManager.currentTheme = "Nord"
    LogViewerThemeManager.setAccentColor("Purple")
    assertEquals("Default", LogViewerThemeManager.currentAccentColor)

    LogViewerThemeManager.currentTheme = "Dracula"
    LogViewerThemeManager.setAccentColor("Blue")
    assertEquals("Default", LogViewerThemeManager.currentAccentColor)
  }

  @Test
  fun testInstallingUnsupportedThemeResetsAccentToDefault() {
    LogViewerThemeManager.currentTheme = "Light"
    LogViewerThemeManager.setAccentColor("Purple")
    assertEquals("Purple", LogViewerThemeManager.currentAccentColor)

    LogViewerThemeManager.currentTheme = "Nord"
    assertEquals("Default", LogViewerThemeManager.currentAccentColor)
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

  @Test
  fun testTextForegroundProperties() {
    LogViewerThemeManager.currentTheme = "Light"
    val lightFg = LogViewerThemeManager.textForeground
    val lightHex = LogViewerThemeManager.textForegroundHex
    val lightMutedHex = LogViewerThemeManager.mutedTextForegroundHex

    assertTrue(lightHex.startsWith("#"))
    assertEquals(7, lightHex.length)
    assertTrue(lightMutedHex.startsWith("#"))
    assertEquals(7, lightMutedHex.length)

    LogViewerThemeManager.currentTheme = "Dark"
    val darkFg = LogViewerThemeManager.textForeground
    val darkHex = LogViewerThemeManager.textForegroundHex
    val darkMutedHex = LogViewerThemeManager.mutedTextForegroundHex

    assertTrue(darkHex.startsWith("#"))
    assertEquals(7, darkHex.length)
    assertTrue(darkMutedHex.startsWith("#"))
    assertEquals(7, darkMutedHex.length)
    org.junit.Assert.assertNotEquals(lightFg, darkFg)
  }
}
