package com.tibagni.logviewer.view

import com.tibagni.logviewer.theme.LogViewerThemeManager
import org.junit.Assert.*
import org.junit.Test
import java.awt.image.BufferedImage
import javax.swing.SwingUtilities

class TriStateCheckboxTests {

  @Test
  fun testInitialState() {
    val cb = TriStateCheckbox("Test")
    assertEquals(TriStateCheckbox.SelectionState.NOT_SELECTED, cb.selectionState)
    assertFalse(cb.isSelected)
    assertNotNull(cb.icon)
  }

  @Test
  fun testSetSelectionState() {
    val cb = TriStateCheckbox()

    cb.selectionState = TriStateCheckbox.SelectionState.SELECTED
    assertEquals(TriStateCheckbox.SelectionState.SELECTED, cb.selectionState)
    assertTrue(cb.isSelected)

    cb.selectionState = TriStateCheckbox.SelectionState.PARTIALLY_SELECTED
    assertEquals(TriStateCheckbox.SelectionState.PARTIALLY_SELECTED, cb.selectionState)
    assertFalse(cb.isSelected)

    cb.selectionState = TriStateCheckbox.SelectionState.NOT_SELECTED
    assertEquals(TriStateCheckbox.SelectionState.NOT_SELECTED, cb.selectionState)
    assertFalse(cb.isSelected)
  }

  @Test
  fun testActionCyclesThroughStates() {
    val cb = TriStateCheckbox()
    var lastNotifiedState: TriStateCheckbox.SelectionState? = null
    cb.addSelectionChangedListener(object : TriStateCheckbox.SelectionChangedListener {
      override fun onSelectionChanged(newSelectionState: TriStateCheckbox.SelectionState) {
        lastNotifiedState = newSelectionState
      }
    })

    cb.doClick()
    assertEquals(TriStateCheckbox.SelectionState.SELECTED, cb.selectionState)
    assertEquals(TriStateCheckbox.SelectionState.SELECTED, lastNotifiedState)

    cb.doClick()
    assertEquals(TriStateCheckbox.SelectionState.NOT_SELECTED, cb.selectionState)
    assertEquals(TriStateCheckbox.SelectionState.NOT_SELECTED, lastNotifiedState)
  }

  @Test
  fun testThemeChangeUpdatesIconAndPaintsProperly() {
    LogViewerThemeManager.currentTheme = "Light"
    val cb = TriStateCheckbox()
    val initialIcon = cb.icon
    assertNotNull(initialIcon)

    // Paint in light theme
    cb.selectionState = TriStateCheckbox.SelectionState.PARTIALLY_SELECTED
    cb.size = cb.preferredSize
    val img1 = BufferedImage(cb.width.coerceAtLeast(1), cb.height.coerceAtLeast(1), BufferedImage.TYPE_INT_ARGB)
    cb.paint(img1.graphics)

    // Switch to dark theme
    LogViewerThemeManager.currentTheme = "Dark"
    SwingUtilities.updateComponentTreeUI(cb)

    val updatedIcon = cb.icon
    assertNotNull(updatedIcon)
    assertNotSame("Icon instance should be refreshed after updateUI", initialIcon, updatedIcon)

    // Paint in dark theme
    val img2 = BufferedImage(cb.width.coerceAtLeast(1), cb.height.coerceAtLeast(1), BufferedImage.TYPE_INT_ARGB)
    cb.paint(img2.graphics)

    // Ensure icon dimensions are positive
    assertTrue(updatedIcon.iconWidth > 0)
    assertTrue(updatedIcon.iconHeight > 0)
  }
}
