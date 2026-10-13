package com.tibagni.logviewer.view

import com.tibagni.logviewer.theme.LogViewerThemeManager
import org.junit.Assert.*
import org.junit.Test
import java.awt.Component
import java.awt.event.MouseEvent

class ToggleSideButtonTests {

  @Test
  fun testInitialActiveStateFalse() {
    LogViewerThemeManager.currentTheme = "Light"
    var toggled = false
    val button = ToggleSideButton(CommonIcons.autoScrollIcon()) { toggled = it }

    assertFalse(button.isActive)
    assertEquals(Component.CENTER_ALIGNMENT, button.alignmentX, 0.001f)

    button.toggle()
    assertTrue(button.isActive)
    assertTrue(toggled)

    button.toggle()
    assertFalse(button.isActive)
    assertFalse(toggled)
  }

  @Test
  fun testInitialActiveStateTrue() {
    LogViewerThemeManager.currentTheme = "Light"
    val button = ToggleSideButton(CommonIcons.autoScrollIcon(), initialActive = true) { }

    assertTrue(button.isActive)
  }

  @Test
  fun testProgrammaticSetActiveDoesNotTriggerListener() {
    LogViewerThemeManager.currentTheme = "Light"
    var listenerTriggered = false
    val button = ToggleSideButton(CommonIcons.autoScrollIcon(), initialActive = true) {
      listenerTriggered = true
    }

    assertTrue(button.isActive)
    button.isActive = false
    assertFalse(button.isActive)
    assertFalse("Programmatic setter should not fire click listener", listenerTriggered)

    button.isActive = true
    assertTrue(button.isActive)
    assertFalse("Programmatic setter should not fire click listener", listenerTriggered)
  }

  @Test
  fun testExpansionToggle() {
    LogViewerThemeManager.currentTheme = "Light"
    val button = ToggleSideButton(CommonIcons.autoScrollIcon(), initialActive = true, textLabel = "Auto-scroll") { }
    assertFalse(button.isExpanded)
    assertEquals("", button.text)
    assertEquals(javax.swing.SwingConstants.CENTER, button.horizontalAlignment)

    button.isExpanded = true
    assertTrue(button.isExpanded)
    assertEquals("Auto-scroll", button.text)
    assertEquals(javax.swing.SwingConstants.LEFT, button.horizontalAlignment)

    button.isExpanded = false
    assertFalse(button.isExpanded)
    assertEquals("", button.text)
    assertEquals(javax.swing.SwingConstants.CENTER, button.horizontalAlignment)
  }

  @Test
  fun testMouseHoverWhenInactiveAndActive() {
    LogViewerThemeManager.currentTheme = "Light"
    val button = ToggleSideButton(CommonIcons.autoScrollIcon()) { }
    val initialIcon = button.icon

    // When inactive, hover should change the icon
    val enterEvent = MouseEvent(button, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 0, 0, 0, false)
    button.dispatchEvent(enterEvent)
    val hoveredIcon = button.icon
    assertNotEquals(initialIcon, hoveredIcon)

    val exitEvent = MouseEvent(button, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, 0, 0, 0, false)
    button.dispatchEvent(exitEvent)
    assertEquals(initialIcon, button.icon)

    // When active, icon is already selected and exiting should not reset to normal
    button.isActive = true
    val activeIcon = button.icon
    assertEquals(hoveredIcon, activeIcon)

    button.dispatchEvent(enterEvent)
    assertEquals(activeIcon, button.icon)

    button.dispatchEvent(exitEvent)
    assertEquals(activeIcon, button.icon)
  }

  @Test
  fun testThemeChangeUpdatesUI() {
    LogViewerThemeManager.currentTheme = "Light"
    val button = ToggleSideButton(CommonIcons.autoScrollIcon(), initialActive = true) { }
    val lightBorder = button.border

    LogViewerThemeManager.currentTheme = "Dark"
    button.updateUI()
    assertNotNull(button.border)
    assertNotSame(lightBorder, button.border)
  }
}
