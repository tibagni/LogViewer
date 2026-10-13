package com.tibagni.logviewer.view

import com.tibagni.logviewer.theme.LogViewerThemeManager
import org.junit.Assert.*
import org.junit.Test
import java.awt.Component
import java.awt.Dimension

class SideButtonTests {

  @Test
  fun testSideButtonInitialization() {
    LogViewerThemeManager.currentTheme = "Light"
    val icon = CommonIcons.playIcon()
    val button = SideButton(icon, "Play Tooltip")

    assertEquals("Play Tooltip", button.toolTipText)
    assertEquals(Component.CENTER_ALIGNMENT, button.alignmentX, 0.001f)
    assertNotNull(button.icon)
    assertNotNull(button.rolloverIcon)
    assertNotNull(button.disabledIcon)
  }

  @Test
  fun testSetIconAndTooltipUpdatesProperties() {
    LogViewerThemeManager.currentTheme = "Light"
    val button = SideButton(CommonIcons.stopIcon(), "Stop")

    assertEquals("Stop", button.toolTipText)

    button.setIconAndTooltip(CommonIcons.playIcon(), "Start")
    assertEquals("Start", button.toolTipText)
    assertNotNull(button.icon)
  }

  @Test
  fun testSideButtonThemeChangeUpdatesUI() {
    LogViewerThemeManager.currentTheme = "Light"
    val button = SideButton(CommonIcons.clearIcon(), "Clear")

    LogViewerThemeManager.currentTheme = "Dark"
    button.updateUI()

    assertNotNull(button.icon)
    assertNotNull(button.rolloverIcon)
    assertNotNull(button.disabledIcon)
  }

  @Test
  fun testMouseHoverSwapsIcon() {
    LogViewerThemeManager.currentTheme = "Light"
    val button = SideButton(CommonIcons.terminalIcon(), "Terminal")
    val normalIcon = button.icon

    val enterEvent = java.awt.event.MouseEvent(
      button, java.awt.event.MouseEvent.MOUSE_ENTERED,
      System.currentTimeMillis(), 0, 0, 0, 0, false
    )
    button.dispatchEvent(enterEvent)
    assertNotEquals(normalIcon, button.icon)

    val exitEvent = java.awt.event.MouseEvent(
      button, java.awt.event.MouseEvent.MOUSE_EXITED,
      System.currentTimeMillis(), 0, 0, 0, 0, false
    )
    button.dispatchEvent(exitEvent)
    assertEquals(normalIcon, button.icon)
  }

  @Test
  fun testExpansionToggle() {
    LogViewerThemeManager.currentTheme = "Light"
    val button = SideButton(CommonIcons.changeDeviceIcon(), "Change Device", "Change Device")
    assertFalse(button.isExpanded)
    assertEquals("", button.text)
    assertEquals(javax.swing.SwingConstants.CENTER, button.horizontalAlignment)

    button.isExpanded = true
    assertTrue(button.isExpanded)
    assertEquals("Change Device", button.text)
    assertEquals(javax.swing.SwingConstants.LEFT, button.horizontalAlignment)

    button.isExpanded = false
    assertFalse(button.isExpanded)
    assertEquals("", button.text)
    assertEquals(javax.swing.SwingConstants.CENTER, button.horizontalAlignment)
  }

  @Test
  fun testSetIconAndTooltipWhenExpandedUpdatesText() {
    LogViewerThemeManager.currentTheme = "Light"
    val button = SideButton(CommonIcons.stopIcon(), "Stop", "Stop")
    button.isExpanded = true
    assertEquals("Stop", button.text)

    button.setIconAndTooltip(CommonIcons.playIcon(), "Start", "Start")
    assertEquals("Start", button.text)
    assertEquals("Start", button.toolTipText)
  }
}
