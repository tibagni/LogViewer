package com.tibagni.logviewer.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Dimension
import javax.swing.UIManager

class LogViewerThemeManagerTests {

  @Test
  fun testAvailableThemes() {
    val themes = LogViewerThemeManager.availableThemes
    assertTrue(themes.contains("Light"))
    assertTrue(themes.contains("Dark"))
    assertEquals(2, themes.size)
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
  fun testTokenDefaults() {
    LogViewerThemeManager.currentTheme = "Light"

    assertEquals(8, UIManager.get("Component.arc"))
    assertEquals(8, UIManager.get("Button.arc"))
    assertEquals(8, UIManager.get("TextComponent.arc"))
    assertEquals("chevron", UIManager.get("Component.arrowType"))
    assertEquals(4, UIManager.get("MenuItem.selectionArc"))
    assertEquals(4, UIManager.get("ComboBox.selectionArc"))
    assertEquals(4, UIManager.get("List.selectionArc"))
    assertEquals(4, UIManager.get("Table.selectionArc"))
    assertEquals(6, UIManager.get("ScrollBar.thumbArc"))
    assertEquals(6, UIManager.get("ScrollBar.trackArc"))
    assertEquals(false, UIManager.get("ScrollBar.showButtons"))
    assertEquals(true, UIManager.get("TabbedPane.showTabSeparators"))
    assertEquals(false, UIManager.get("TabbedPane.tabSeparatorsFullHeight"))
    assertEquals(false, UIManager.get("Table.showVerticalLines"))
    assertEquals(Dimension(0, 1), UIManager.get("Table.intercellSpacing"))
  }

  @Test
  fun testInvalidThemeKeepsCurrentOrFallsBack() {
    LogViewerThemeManager.currentTheme = "Light"
    assertEquals("Light", LogViewerThemeManager.currentTheme)

    LogViewerThemeManager.currentTheme = "NonExistentTheme"
    // When a theme is already installed, invalid theme should be ignored
    assertEquals("Light", LogViewerThemeManager.currentTheme)
  }
}
