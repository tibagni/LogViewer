package com.tibagni.logviewer.view

import com.formdev.flatlaf.ui.FlatLineBorder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Cursor
import javax.swing.border.EmptyBorder

/**
 * Unit tests for [SingleChoiceDialog] verifying card styling,
 * radio button choices, selection state, and scrolling behavior.
 */
class SingleChoiceDialogTests {

  /**
   * Verifies initial dialog state including message text, choices
   * button count, default selection, and 8dp FlatLineBorder.
   */
  @Test
  fun testInitialState() {
    val choices = arrayOf("Option A", "Option B", "Option C")
    val dialog = SingleChoiceDialog(
      "Select One",
      "Please select an option:",
      choices,
      1
    )

    assertEquals("Please select an option:", dialog.messageLabel.text)
    assertEquals(3, dialog.choicesButtons.size)
    assertFalse(dialog.choicesButtons[0].isSelected)
    assertTrue(dialog.choicesButtons[1].isSelected)
    assertFalse(dialog.choicesButtons[2].isSelected)
    assertEquals(1, dialog.currentSelection)

    assertTrue(dialog.cardPanel.border is FlatLineBorder)
    val border = dialog.cardPanel.border as FlatLineBorder
    assertEquals(UIScaleUtils.dip(8), border.arc)
    assertEquals(1f, border.lineThickness, 0.001f)
    assertNotNull(border.lineColor)
  }

  /**
   * Verifies that selecting a different radio button updates
   * [SingleChoiceDialog.currentSelection].
   */
  @Test
  fun testSelectingDifferentRadioButtonUpdatesSelection() {
    val choices = arrayOf("First", "Second", "Third")
    val dialog = SingleChoiceDialog(
      "Choose",
      "Select:",
      choices,
      0
    )

    assertEquals(0, dialog.currentSelection)
    dialog.choicesButtons[2].doClick()
    assertEquals(2, dialog.currentSelection)
    assertTrue(dialog.choicesButtons[2].isSelected)
    assertFalse(dialog.choicesButtons[0].isSelected)
  }

  /**
   * Verifies that when choices count exceeds 6, choices card is
   * wrapped inside a JScrollPane.
   */
  @Test
  fun testChoicesOverLimitWrapInScrollPane() {
    val choices = Array(8) { "Choice #$it" }
    val dialog = SingleChoiceDialog(
      "Many Options",
      "Choose:",
      choices,
      0
    )

    assertNotNull(dialog.scrollPane)
    assertEquals(dialog.cardPanel, dialog.scrollPane?.viewport?.view)
    assertTrue(dialog.cardPanel.border is FlatLineBorder)
  }

  /**
   * Verifies that when choices count is within 6, no scroll pane
   * is created and card panel is placed directly in dialog panel.
   */
  @Test
  fun testChoicesWithinLimitNoScrollPane() {
    val choices = Array(4) { "Choice #$it" }
    val dialog = SingleChoiceDialog(
      "Few Options",
      "Choose:",
      choices,
      0
    )

    assertNull(dialog.scrollPane)
    assertTrue(dialog.cardPanel.border is FlatLineBorder)
  }

  /**
   * Verifies each radio button row has hand cursor and 4dp x 6dp
   * empty border padding.
   */
  @Test
  fun testRadioButtonPaddingAndCursor() {
    val choices = arrayOf("Item 1", "Item 2")
    val dialog = SingleChoiceDialog("Title", "Prompt", choices, 0)

    val handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    dialog.choicesButtons.forEach { rb ->
      assertEquals(handCursor, rb.cursor)
      assertTrue(rb.border is EmptyBorder)
      val insets = rb.insets
      assertEquals(UIScaleUtils.dip(4), insets.top)
      assertEquals(UIScaleUtils.dip(6), insets.left)
      assertEquals(UIScaleUtils.dip(4), insets.bottom)
      assertEquals(UIScaleUtils.dip(6), insets.right)
    }
  }

  /**
   * Verifies that negative defaultSelectedIndex leaves all options
   * unselected.
   */
  @Test
  fun testNegativeDefaultSelectedIndex() {
    val choices = arrayOf("Item 1", "Item 2")
    val dialog = SingleChoiceDialog("Title", "Prompt", choices, -1)

    assertEquals(-1, dialog.currentSelection)
    assertTrue(dialog.choicesButtons.none { it.isSelected })
  }
}
