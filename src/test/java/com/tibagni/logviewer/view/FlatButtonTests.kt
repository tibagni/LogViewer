package com.tibagni.logviewer.view

import com.formdev.flatlaf.FlatClientProperties
import com.tibagni.logviewer.theme.LogViewerThemeManager
import org.junit.Assert.*
import org.junit.Test
import java.awt.GraphicsEnvironment
import java.awt.event.MouseEvent
import javax.swing.UIManager

class FlatButtonTests {

  @Test
  fun testToolbarButtonClientProperty() {
    val button = FlatButton("Test")
    assertEquals(
      FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON,
      button.getClientProperty(FlatClientProperties.BUTTON_TYPE)
    )
  }

  @Test
  fun testInitialColorsFromUIManager() {
    if (GraphicsEnvironment.isHeadless()) return;

    LogViewerThemeManager.currentTheme = "Light"
    val button = FlatButton("Test")
    val expectedForeground = UIManager.getColor("Button.foreground")
    assertEquals(expectedForeground.rgb, button.foreground.rgb)
  }

  @Test
  fun testUpdateUIUpdatesColorsOnThemeChange() {
    if (GraphicsEnvironment.isHeadless()) return;

    LogViewerThemeManager.currentTheme = "Light"
    val button = FlatButton("Test")
    val lightForeground = button.foreground

    LogViewerThemeManager.currentTheme = "Dark"
    button.updateUI()
    val darkForeground = button.foreground

    assertNotEquals("Foreground color should update when theme changes", lightForeground, darkForeground)
    val expectedDark = UIManager.getColor("Button.foreground")
    assertEquals(expectedDark.rgb, darkForeground.rgb)
  }

  @Test
  fun testMouseEnteredAndExited() {
    if (GraphicsEnvironment.isHeadless()) return;

    LogViewerThemeManager.currentTheme = "Light"
    val button = FlatButton("Test")
    val normalColor = button.foreground
    val rolloverColor = UIManager.getColor("textHighlight")

    val enterEvent = MouseEvent(button, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 0, 0, 0, false)
    button.dispatchEvent(enterEvent)
    assertEquals(rolloverColor.rgb, button.foreground.rgb)

    val exitEvent = MouseEvent(button, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, 0, 0, 0, false)
    button.dispatchEvent(exitEvent)
    assertEquals(normalColor.rgb, button.foreground.rgb)
  }
}
