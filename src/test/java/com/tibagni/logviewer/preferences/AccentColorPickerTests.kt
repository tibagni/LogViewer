package com.tibagni.logviewer.preferences

import com.tibagni.logviewer.theme.LogViewerThemeManager
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage

/**
 * Unit tests verifying [AccentColorPickerPanel] and [RoundColorButton] behavior,
 * including color button generation, selection state management, programmatic and
 * interactive activation, paint execution, and wiring into [AppearancePreferencePanel].
 */
class AccentColorPickerTests {

  @Test
  fun testAvailableColorsConfigured() {
    val picker = AccentColorPickerPanel(LogViewerThemeManager)
    val expectedColors = LogViewerThemeManager.availableAccentColors

    for (colorName in expectedColors) {
      val btn = picker.getButton(colorName)
      assertNotNull("Picker must contain button for color: $colorName", btn)
      assertEquals("Button name must match", colorName, btn!!.colorName)
      assertEquals("Default flag must match", colorName == "Default", btn.isDefault)
    }
  }

  @Test
  fun testInitialSelectionMatchesThemeManager() {
    val picker = AccentColorPickerPanel(LogViewerThemeManager)
    assertEquals(LogViewerThemeManager.currentAccentColor, picker.selectedAccentColor)

    val selectedBtn = picker.getButton(LogViewerThemeManager.currentAccentColor)
    assertNotNull(selectedBtn)
    assertTrue("Initial button must be selected", selectedBtn!!.isSelected)
  }

  @Test
  fun testSetSelectedAccentColorValid() {
    val picker = AccentColorPickerPanel(LogViewerThemeManager)
    picker.selectedAccentColor = "Purple"

    assertEquals("Purple", picker.selectedAccentColor)
    assertTrue(picker.getButton("Purple")!!.isSelected)
    assertFalse(picker.getButton("Default")!!.isSelected)
    assertFalse(picker.getButton("Blue")!!.isSelected)

    picker.selectedAccentColor = "Teal"
    assertEquals("Teal", picker.selectedAccentColor)
    assertTrue(picker.getButton("Teal")!!.isSelected)
    assertFalse(picker.getButton("Purple")!!.isSelected)
  }

  @Test
  fun testSetSelectedAccentColorFallback() {
    val picker = AccentColorPickerPanel(LogViewerThemeManager)
    picker.selectedAccentColor = "NonExistentColor"

    assertEquals("Default", picker.selectedAccentColor)
    assertTrue(picker.getButton("Default")!!.isSelected)
  }

  @Test
  fun testButtonClickSelectsColorAndInvokesCallback() {
    val picker = AccentColorPickerPanel(LogViewerThemeManager)
    var callbackColor: String? = null
    picker.onColorSelected = { color -> callbackColor = color }

    val redBtn = picker.getButton("Red")
    assertNotNull(redBtn)
    redBtn!!.performClick()

    assertEquals("Red", picker.selectedAccentColor)
    assertEquals("Red", callbackColor)
    assertTrue(redBtn.isSelected)
  }

  @Test
  fun testRoundColorButtonProperties() {
    val button = RoundColorButton(
      colorName = "Orange",
      isDefault = false,
      themeManager = LogViewerThemeManager
    )

    val expectedSize = Dimension(UIScaleUtils.dip(24), UIScaleUtils.dip(24))
    assertEquals(expectedSize, button.preferredSize)
    assertEquals(expectedSize, button.minimumSize)
    assertEquals(expectedSize, button.maximumSize)
    assertEquals("Orange", button.colorName)
    assertFalse(button.isDefault)
    assertEquals("Orange", button.toolTipText)
    assertEquals(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR), button.cursor)
    assertTrue(button.isFocusable)
    assertFalse(button.isOpaque)
  }

  @Test
  fun testRoundColorButtonColorResolution() {
    for (colorName in LogViewerThemeManager.availableAccentColors) {
      val button = RoundColorButton(
        colorName = colorName,
        isDefault = (colorName == "Default"),
        themeManager = LogViewerThemeManager
      )
      assertEquals(
        "Button color must match theme manager accent color",
        LogViewerThemeManager.getColorForAccent(colorName),
        button.color
      )
    }
  }

  @Test
  fun testRoundColorButtonPaintUnselected() {
    val button = RoundColorButton(
      colorName = "Blue",
      isDefault = false,
      themeManager = LogViewerThemeManager
    )
    button.isSelected = false
    button.setSize(UIScaleUtils.dip(24), UIScaleUtils.dip(24))

    val image = BufferedImage(
      UIScaleUtils.dip(24),
      UIScaleUtils.dip(24),
      BufferedImage.TYPE_INT_ARGB
    )
    val g = image.createGraphics()
    try {
      button.paint(g)
    } finally {
      g.dispose()
    }

    val centerPixel = image.getRGB(UIScaleUtils.dip(12), UIScaleUtils.dip(12))
    val alpha = (centerPixel shr 24) and 0xFF
    assertTrue("Unselected circle must paint pixels at center", alpha > 0)
  }

  @Test
  fun testRoundColorButtonPaintSelected() {
    val button = RoundColorButton(
      colorName = "Green",
      isDefault = false,
      themeManager = LogViewerThemeManager
    )
    button.isSelected = true
    button.setSize(UIScaleUtils.dip(24), UIScaleUtils.dip(24))

    val image = BufferedImage(
      UIScaleUtils.dip(24),
      UIScaleUtils.dip(24),
      BufferedImage.TYPE_INT_ARGB
    )
    val g = image.createGraphics()
    try {
      button.paint(g)
    } finally {
      g.dispose()
    }

    val centerPixel = image.getRGB(UIScaleUtils.dip(12), UIScaleUtils.dip(12))
    val alpha = (centerPixel shr 24) and 0xFF
    assertTrue("Selected circle must paint checkmark and fill", alpha > 0)
  }

  @Test
  fun testRoundColorButtonPaintDefault() {
    val defaultBtn = RoundColorButton(
      colorName = "Default",
      isDefault = true,
      themeManager = LogViewerThemeManager
    )
    defaultBtn.isSelected = false
    defaultBtn.setSize(UIScaleUtils.dip(24), UIScaleUtils.dip(24))

    val imageUnselected = BufferedImage(
      UIScaleUtils.dip(24),
      UIScaleUtils.dip(24),
      BufferedImage.TYPE_INT_ARGB
    )
    val g1 = imageUnselected.createGraphics()
    try {
      defaultBtn.paint(g1)
    } finally {
      g1.dispose()
    }

    val alphaUnselected = (imageUnselected.getRGB(
      UIScaleUtils.dip(12),
      UIScaleUtils.dip(12)
    ) shr 24) and 0xFF
    assertTrue("Default unselected button must paint pixels", alphaUnselected > 0)

    defaultBtn.isSelected = true
    val imageSelected = BufferedImage(
      UIScaleUtils.dip(24),
      UIScaleUtils.dip(24),
      BufferedImage.TYPE_INT_ARGB
    )
    val g2 = imageSelected.createGraphics()
    try {
      defaultBtn.paint(g2)
    } finally {
      g2.dispose()
    }

    val alphaSelected = (imageSelected.getRGB(
      UIScaleUtils.dip(12),
      UIScaleUtils.dip(12)
    ) shr 24) and 0xFF
    assertTrue("Default selected button must paint pixels", alphaSelected > 0)
  }

  @Test
  fun testRoundColorButtonPaintHandlesNullGraphics() {
    val button = RoundColorButton(
      colorName = "Teal",
      isDefault = false,
      themeManager = LogViewerThemeManager
    )
    button.paintComponent(null)
  }

  @Test
  fun testRoundColorButtonKeyboardActivation() {
    val button = RoundColorButton(
      colorName = "Red",
      isDefault = false,
      themeManager = LogViewerThemeManager
    )
    var clicked = false
    button.onSelected = { clicked = true }

    val keyListeners = button.keyListeners
    assertTrue("Button must register key listener", keyListeners.isNotEmpty())

    val spaceEvent = KeyEvent(
      button,
      KeyEvent.KEY_PRESSED,
      System.currentTimeMillis(),
      0,
      KeyEvent.VK_SPACE,
      ' '
    )
    button.keyListeners.forEach { it.keyPressed(spaceEvent) }
    assertTrue("Space key must trigger click", clicked)

    clicked = false
    val enterEvent = KeyEvent(
      button,
      KeyEvent.KEY_PRESSED,
      System.currentTimeMillis(),
      0,
      KeyEvent.VK_ENTER,
      '\n'
    )
    button.keyListeners.forEach { it.keyPressed(enterEvent) }
    assertTrue("Enter key must trigger click", clicked)
  }

  @Test
  fun testAppearancePreferencePanelWiring() {
    val mockPrefs = mock(LogViewerPreferences::class.java)
    `when`(mockPrefs.lookAndFeel).thenReturn("Light")
    `when`(mockPrefs.accentColor).thenReturn("Default")
    `when`(mockPrefs.logFontSize).thenReturn(12)
    `when`(mockPrefs.logFontFamily).thenReturn("Monospaced")
    `when`(mockPrefs.logFontBold).thenReturn(false)

    LogViewerThemeManager.currentTheme = "Light"
    LogViewerThemeManager.setAccentColor("Default")

    val panel = AppearancePreferencePanel(mockPrefs, LogViewerThemeManager)

    assertEquals("Default", panel.selectedAccentColor)
    assertFalse(panel.hasTimeConsumingOperation)

    panel.selectedAccentColor = "Teal"
    assertEquals("Teal", panel.selectedAccentColor)
    assertTrue(panel.hasTimeConsumingOperation)

    panel.apply()
    verify(mockPrefs).accentColor = "Teal"
  }

  @Test
  fun testAppearancePreferencePanelApplyUnchangedDoesNotUpdate() {
    val mockPrefs = mock(LogViewerPreferences::class.java)
    `when`(mockPrefs.lookAndFeel).thenReturn("Light")
    `when`(mockPrefs.accentColor).thenReturn("Default")
    `when`(mockPrefs.logFontSize).thenReturn(12)
    `when`(mockPrefs.logFontFamily).thenReturn("Monospaced")
    `when`(mockPrefs.logFontBold).thenReturn(false)

    LogViewerThemeManager.currentTheme = "Light"
    LogViewerThemeManager.setAccentColor("Default")

    val panel = AppearancePreferencePanel(mockPrefs, LogViewerThemeManager)
    panel.apply()

    verify(mockPrefs, never()).accentColor = "Default"
  }
}
