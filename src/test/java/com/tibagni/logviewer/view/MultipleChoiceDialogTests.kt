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
 * Unit tests for [MultipleChoiceDialog] verifying card styling,
 * checkbox choices, select-all behavior, status badge, and scrolling.
 */
class MultipleChoiceDialogTests {

  /**
   * Verifies initial state when allSelectedByDefault is true:
   * all checkboxes are selected, selectAllCb is in SELECTED state,
   * and selectionBadge displays "N / N".
   */
  @Test
  fun testInitialStateAllSelectedByDefaultTrue() {
    val choices = arrayOf("Alpha", "Beta", "Gamma")
    val dialog = MultipleChoiceDialog("Title", "Select:", choices, true)

    assertEquals(3, dialog.choicesButtons.size)
    assertTrue(dialog.choicesButtons.all { it.isSelected })
    assertEquals(
      TriStateCheckbox.SelectionState.SELECTED,
      dialog.selectAllCb.selectionState
    )
    assertEquals("3 / 3", dialog.selectionBadge.text)
    assertTrue(dialog.selectionBadge.isVisible)
  }

  /**
   * Verifies initial state when allSelectedByDefault is false:
   * no checkboxes are selected, selectAllCb is in NOT_SELECTED state,
   * and selectionBadge displays "0 / N".
   */
  @Test
  fun testInitialStateAllSelectedByDefaultFalse() {
    val choices = arrayOf("Alpha", "Beta", "Gamma")
    val dialog = MultipleChoiceDialog("Title", "Select:", choices, false)

    assertEquals(3, dialog.choicesButtons.size)
    assertTrue(dialog.choicesButtons.none { it.isSelected })
    assertEquals(
      TriStateCheckbox.SelectionState.NOT_SELECTED,
      dialog.selectAllCb.selectionState
    )
    assertEquals("0 / 3", dialog.selectionBadge.text)
    assertTrue(dialog.selectionBadge.isVisible)
  }

  /**
   * Verifies that selecting individual choices updates selectAllCb
   * to PARTIALLY_SELECTED and updates the selection badge count.
   */
  @Test
  fun testSelectingIndividualChoiceUpdatesSelectAllAndBadge() {
    val choices = arrayOf("Alpha", "Beta", "Gamma")
    val dialog = MultipleChoiceDialog("Title", "Select:", choices, false)

    assertEquals("0 / 3", dialog.selectionBadge.text)

    dialog.choicesButtons[0].doClick()
    assertEquals(
      TriStateCheckbox.SelectionState.PARTIALLY_SELECTED,
      dialog.selectAllCb.selectionState
    )
    assertEquals("1 / 3", dialog.selectionBadge.text)

    dialog.choicesButtons[1].doClick()
    assertEquals(
      TriStateCheckbox.SelectionState.PARTIALLY_SELECTED,
      dialog.selectAllCb.selectionState
    )
    assertEquals("2 / 3", dialog.selectionBadge.text)

    dialog.choicesButtons[2].doClick()
    assertEquals(
      TriStateCheckbox.SelectionState.SELECTED,
      dialog.selectAllCb.selectionState
    )
    assertEquals("3 / 3", dialog.selectionBadge.text)

    dialog.choicesButtons[1].doClick()
    assertEquals(
      TriStateCheckbox.SelectionState.PARTIALLY_SELECTED,
      dialog.selectAllCb.selectionState
    )
    assertEquals("2 / 3", dialog.selectionBadge.text)
  }

  /**
   * Verifies that toggling selectAllCb updates all checkboxes and
   * the status badge text in both directions.
   */
  @Test
  fun testTogglingSelectAllUpdatesAllCheckboxesAndBadge() {
    val choices = arrayOf("Alpha", "Beta", "Gamma")
    val dialog = MultipleChoiceDialog("Title", "Select:", choices, false)

    dialog.selectAllCb.doClick()
    assertTrue(dialog.choicesButtons.all { it.isSelected })
    assertEquals("3 / 3", dialog.selectionBadge.text)
    assertEquals(
      TriStateCheckbox.SelectionState.SELECTED,
      dialog.selectAllCb.selectionState
    )

    dialog.selectAllCb.doClick()
    assertTrue(dialog.choicesButtons.none { it.isSelected })
    assertEquals("0 / 3", dialog.selectionBadge.text)
    assertEquals(
      TriStateCheckbox.SelectionState.NOT_SELECTED,
      dialog.selectAllCb.selectionState
    )
  }

  /**
   * Verifies card presentation geometry with an 8dp FlatLineBorder.
   */
  @Test
  fun testCardPresentationWithFlatLineBorder() {
    val choices = arrayOf("One", "Two", "Three")
    val dialog = MultipleChoiceDialog("Title", "Select:", choices, true)

    assertTrue(dialog.cardPanel.border is FlatLineBorder)
    val border = dialog.cardPanel.border as FlatLineBorder
    assertEquals(UIScaleUtils.dip(8), border.arc)
    assertEquals(1f, border.lineThickness, 0.001f)
    assertNotNull(border.lineColor)
  }

  /**
   * Verifies that when choices count exceeds 6, choices card is
   * wrapped inside a JScrollPane.
   */
  @Test
  fun testChoicesOverLimitWrapInScrollPane() {
    val choices = Array(9) { "Item #$it" }
    val dialog = MultipleChoiceDialog("Title", "Select:", choices, true)

    assertNotNull(dialog.scrollPane)
    assertEquals(dialog.cardPanel, dialog.scrollPane?.viewport?.view)
    assertTrue(dialog.cardPanel.border is FlatLineBorder)
  }

  /**
   * Verifies that when choices count is <= 6, choices are placed
   * directly in the dialog without a JScrollPane wrapper.
   */
  @Test
  fun testChoicesWithinLimitNoScrollPane() {
    val choices = arrayOf("Alpha", "Beta", "Gamma")
    val dialog = MultipleChoiceDialog("Title", "Select:", choices, false)

    assertNull(dialog.scrollPane)
    assertEquals(dialog.cardPanel, dialog.dialogPanel.getComponent(1))
  }

  /**
   * Verifies each checkbox row has hand cursor and 4dp x 6dp empty
   * border padding.
   */
  @Test
  fun testCheckBoxPaddingAndCursor() {
    val choices = arrayOf("Item 1", "Item 2")
    val dialog = MultipleChoiceDialog("Title", "Prompt", choices, false)

    val handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    dialog.choicesButtons.forEach { cb ->
      assertEquals(handCursor, cb.cursor)
      assertTrue(cb.border is EmptyBorder)
      val insets = cb.insets
      assertEquals(UIScaleUtils.dip(4), insets.top)
      assertEquals(UIScaleUtils.dip(6), insets.left)
      assertEquals(UIScaleUtils.dip(4), insets.bottom)
      assertEquals(UIScaleUtils.dip(6), insets.right)
    }
  }
}
