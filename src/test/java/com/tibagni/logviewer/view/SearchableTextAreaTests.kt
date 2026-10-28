package com.tibagni.logviewer.view

import com.formdev.flatlaf.FlatClientProperties
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Cursor
import javax.swing.border.CompoundBorder

/**
 * Unit tests verifying [SearchableTextArea] modern search controls,
 * highlight indexing, navigation, regex safety, and theme updates.
 */
class SearchableTextAreaTests {

  @Test
  fun testInitialComponentsState() {
    val comp = SearchableTextArea()

    assertFalse("Search panel should initially be hidden", comp.searchOptionPanel.isVisible)
    assertEquals(
      true,
      comp.searchText.getClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON)
    )
    assertEquals(Cursor.HAND_CURSOR, comp.matchCaseOption.cursor.type)
    assertNotNull(comp.searchLast.icon)
    assertNotNull(comp.searchNext.icon)
    assertNotNull(comp.close.icon)
  }

  @Test
  fun testShowAndHideSearch() {
    val comp = SearchableTextArea()

    comp.showSearch()
    assertTrue(comp.searchOptionPanel.isVisible)

    comp.searchText.text = "test query"
    comp.hideSearch()

    assertFalse(comp.searchOptionPanel.isVisible)
    assertEquals("", comp.searchText.text)
    assertEquals(-1, comp.currentMatchIndex)
    assertTrue(comp.currentMatches.isEmpty())
  }

  @Test
  fun testSearchHighlightsAndNavigation() {
    val comp = SearchableTextArea()
    comp.text = "apple banana apple cherry apple"
    comp.showSearch()

    comp.searchText.text = "apple"
    assertEquals(3, comp.currentMatches.size)
    assertEquals(0, comp.currentMatchIndex)
    assertEquals("1/3", comp.searchResult.text)

    comp.searchNext.doClick()
    assertEquals(1, comp.currentMatchIndex)
    assertEquals("2/3", comp.searchResult.text)

    comp.searchNext.doClick()
    assertEquals(2, comp.currentMatchIndex)
    assertEquals("3/3", comp.searchResult.text)

    // Wrap-around forward
    comp.searchNext.doClick()
    assertEquals(0, comp.currentMatchIndex)
    assertEquals("1/3", comp.searchResult.text)

    // Wrap-around backward
    comp.searchLast.doClick()
    assertEquals(2, comp.currentMatchIndex)
    assertEquals("3/3", comp.searchResult.text)
  }

  @Test
  fun testRegexSpecialCharactersDoNotThrow() {
    val comp = SearchableTextArea()
    comp.text = "text with (parentheses) [brackets] and *star"
    comp.showSearch()

    comp.searchText.text = "("
    assertEquals(1, comp.currentMatches.size)

    comp.searchText.text = "["
    assertEquals(1, comp.currentMatches.size)

    comp.searchText.text = "*"
    assertEquals(1, comp.currentMatches.size)
  }

  @Test
  fun testMatchCaseOptionTogglesHighlights() {
    val comp = SearchableTextArea()
    comp.text = "Android android ANDROID"
    comp.showSearch()

    comp.searchText.text = "Android"
    assertEquals(3, comp.currentMatches.size)

    comp.matchCaseOption.isSelected = true
    assertEquals(1, comp.currentMatches.size)
    assertEquals("1/1", comp.searchResult.text)

    comp.matchCaseOption.isSelected = false
    assertEquals(3, comp.currentMatches.size)
  }

  @Test
  fun testOnThemeChangedUpdatesIconsAndBorders() {
    val comp = SearchableTextArea()
    comp.onThemeChanged()

    assertNotNull(comp.searchLast.icon)
    assertNotNull(comp.searchNext.icon)
    assertNotNull(comp.close.icon)

    val border = comp.searchOptionPanel.border
    assertTrue(border is CompoundBorder)
  }

  @Test
  fun testNoMatchesUpdatesBadge() {
    val comp = SearchableTextArea()
    comp.text = "Sample log text"
    comp.showSearch()

    comp.searchText.text = "nonexistent"
    assertEquals(0, comp.currentMatches.size)
    assertEquals("0 results", comp.searchResult.text)

    comp.searchText.text = ""
    assertEquals(0, comp.currentMatches.size)
    assertEquals("", comp.searchResult.text)
  }
}
