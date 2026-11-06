package com.tibagni.logviewer.filter

import com.formdev.flatlaf.FlatClientProperties
import com.tibagni.logviewer.FiltersRepository
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.StatusBadge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.awt.Color
import java.awt.Dimension
import java.awt.GraphicsEnvironment

/**
 * Unit tests for [SearchFiltersDialog] verifying UI component modernization,
 * [StatusBadge] match reporting, FlatLaf properties, button states,
 * and filtering behavior.
 */
class SearchFiltersDialogTests {

  companion object {
    private val TAG_A = FilterContextTag.register("search_dialog_tag_a")
    private val TAG_B = FilterContextTag.register("search_dialog_tag_b")
  }

  private lateinit var mockPresenter: FiltersPresenter
  private lateinit var mockFiltersRepo: FiltersRepository
  private lateinit var filter1: Filter
  private lateinit var filter2: Filter
  private lateinit var filter3: Filter

  /**
   * Sets up test fixtures and mocks prior to each test run.
   */
  @Before
  fun setUp() {
    mockPresenter = mock(FiltersPresenter::class.java)
    mockFiltersRepo = mock(FiltersRepository::class.java)

    filter1 = Filter(
      "AlphaFilter",
      "alpha.*pattern",
      Color.RED,
      LogLevel.DEBUG
    )
    filter2 = Filter(
      "BetaFilter",
      "beta.*pattern",
      Color.BLUE,
      LogLevel.INFO
    )
    filter3 = Filter(
      "GammaFilter",
      "gamma.*pattern",
      Color.GREEN,
      LogLevel.WARNING
    )

    val filtersMap = mapOf(
      "GroupOne" to listOf(filter1, filter2),
      "GroupTwo" to listOf(filter3)
    )
    `when`(mockFiltersRepo.currentlyOpenedFilters).thenReturn(filtersMap)
  }

  /**
   * Verifies dialog component initialization, FlatLaf client properties,
   * table geometry, and initial [StatusBadge] match counting showing all filters.
   */
  @Test
  fun testDialogInstantiationAndComponents() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val dialog = SearchFiltersDialog(
      owner = null,
      presenter = mockPresenter,
      filtersRepository = mockFiltersRepo,
      onFilterSelected = { _, _ -> }
    )
    try {
      assertNotNull(dialog.matchesBadge)
      assertEquals(
        StatusBadge.Variant.NORMAL,
        dialog.matchesBadge.variant
      )
      assertEquals(
        I18n.format(I18n.SEARCH_FILTERS_SHOWING_ALL, 3),
        dialog.matchesBadge.text
      )
      assertEquals(3, dialog.resultsTable.rowCount)

      assertNotNull(dialog.jumpBtn)
      assertNotNull(dialog.editBtn)
      assertNotNull(dialog.deleteBtn)
      assertNotNull(dialog.closeBtn)

      val showClear = dialog.searchField.getClientProperty(
        FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON
      )
      assertEquals(true, showClear)

      assertTrue(dialog.resultsTable.showHorizontalLines)
      assertFalse(dialog.resultsTable.showVerticalLines)
      assertEquals(Dimension(0, 1), dialog.resultsTable.intercellSpacing)
      assertTrue(dialog.resultsTable.tableHeader.resizingAllowed)
    } finally {
      dialog.dispose()
    }
  }

  /**
   * Verifies that searching updates [StatusBadge] variant and text:
   * [StatusBadge.Variant.NORMAL] for matching query and
   * [StatusBadge.Variant.NO_RESULTS] for non-matching query.
   */
  @Test
  fun testSearchFilteringUpdatesMatchesBadgeAndTable() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val dialog = SearchFiltersDialog(
      owner = null,
      presenter = mockPresenter,
      filtersRepository = mockFiltersRepo,
      onFilterSelected = { _, _ -> }
    )
    try {
      // Matching query with single result
      dialog.performSearch("Alpha")
      assertEquals(
        StatusBadge.Variant.NORMAL,
        dialog.matchesBadge.variant
      )
      assertEquals(
        I18n.format(I18n.SEARCH_FILTERS_SHOWING_MATCHES, 1, 3),
        dialog.matchesBadge.text
      )
      assertEquals(1, dialog.resultsTable.rowCount)

      // Matching query with multiple results
      dialog.performSearch("Filter")
      assertEquals(
        StatusBadge.Variant.NORMAL,
        dialog.matchesBadge.variant
      )
      assertEquals(
        I18n.format(I18n.SEARCH_FILTERS_SHOWING_MATCHES, 3, 3),
        dialog.matchesBadge.text
      )
      assertEquals(3, dialog.resultsTable.rowCount)

      // Non-matching query
      dialog.performSearch("NonExistentPattern12345")
      assertEquals(
        StatusBadge.Variant.NO_RESULTS,
        dialog.matchesBadge.variant
      )
      assertEquals(
        I18n.format(I18n.SEARCH_FILTERS_SHOWING_MATCHES, 0, 3),
        dialog.matchesBadge.text
      )
      assertEquals(0, dialog.resultsTable.rowCount)

      // Blank query resets to showing all
      dialog.performSearch("")
      assertEquals(
        StatusBadge.Variant.NORMAL,
        dialog.matchesBadge.variant
      )
      assertEquals(
        I18n.format(I18n.SEARCH_FILTERS_SHOWING_ALL, 3),
        dialog.matchesBadge.text
      )
      assertEquals(3, dialog.resultsTable.rowCount)
    } finally {
      dialog.dispose()
    }
  }

  /**
   * Verifies that jump, edit, and delete action button enabled states
   * depend strictly on whether a table row is selected.
   */
  @Test
  fun testButtonStatesDependOnSelection() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val dialog = SearchFiltersDialog(
      owner = null,
      presenter = mockPresenter,
      filtersRepository = mockFiltersRepo,
      onFilterSelected = { _, _ -> }
    )
    try {
      // By default with matches, first row is selected
      assertTrue(dialog.resultsTable.selectedRow >= 0)
      assertTrue(dialog.jumpBtn.isEnabled)
      assertTrue(dialog.editBtn.isEnabled)
      assertTrue(dialog.deleteBtn.isEnabled)

      // Clear selection - buttons should be disabled
      dialog.resultsTable.clearSelection()
      assertEquals(-1, dialog.resultsTable.selectedRow)
      assertFalse(dialog.jumpBtn.isEnabled)
      assertFalse(dialog.editBtn.isEnabled)
      assertFalse(dialog.deleteBtn.isEnabled)

      // Select a row again - buttons should be enabled
      dialog.resultsTable.setRowSelectionInterval(0, 0)
      assertTrue(dialog.jumpBtn.isEnabled)
      assertTrue(dialog.editBtn.isEnabled)
      assertTrue(dialog.deleteBtn.isEnabled)

      // When searching with no matches, no row can be selected
      dialog.performSearch("NoSuchFilter")
      assertEquals(-1, dialog.resultsTable.selectedRow)
      assertFalse(dialog.jumpBtn.isEnabled)
      assertFalse(dialog.editBtn.isEnabled)
      assertFalse(dialog.deleteBtn.isEnabled)
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testEmptyStateCardAndClearSearchAction() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val dialog = SearchFiltersDialog(
      owner = null,
      presenter = mockPresenter,
      filtersRepository = mockFiltersRepo,
      onFilterSelected = { _, _ -> }
    )
    try {
      assertNotNull(dialog.emptyStateCard)
      assertNotNull(dialog.tableScrollPane)
      assertTrue(dialog.tableScrollPane.border is com.formdev.flatlaf.ui.FlatLineBorder)

      // Initial search has 3 results -> table card showing
      assertEquals(3, dialog.resultsTable.rowCount)

      // Search non-existent -> empty state card
      dialog.searchField.text = "UnknownFilterXYZ"
      dialog.performSearch("UnknownFilterXYZ")
      assertEquals(0, dialog.resultsTable.rowCount)
      assertEquals(StatusBadge.Variant.NO_RESULTS, dialog.matchesBadge.variant)

      // Clicking clear button resets search field and restores all filters
      dialog.clearSearchBtn.doClick()
      assertEquals("", dialog.searchField.text)
      assertEquals(3, dialog.resultsTable.rowCount)
      assertEquals(StatusBadge.Variant.NORMAL, dialog.matchesBadge.variant)
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testSearchFieldLeadingIconConfigured() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val dialog = SearchFiltersDialog(
      owner = null,
      presenter = mockPresenter,
      filtersRepository = mockFiltersRepo,
      onFilterSelected = { _, _ -> }
    )
    try {
      assertNotNull(
        dialog.searchField.getClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON)
      )
      assertEquals(
        true,
        dialog.searchField.getClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON)
      )
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testActionButtonsHaveVectorIcons() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val dialog = SearchFiltersDialog(
      owner = null,
      presenter = mockPresenter,
      filtersRepository = mockFiltersRepo,
      onFilterSelected = { _, _ -> }
    )
    try {
      assertNotNull("jumpBtn should have an icon", dialog.jumpBtn.icon)
      assertEquals(UIScaleUtils.dip(4), dialog.jumpBtn.iconTextGap)
      assertNotNull("editBtn should have an icon", dialog.editBtn.icon)
      assertEquals(UIScaleUtils.dip(4), dialog.editBtn.iconTextGap)
      assertNotNull("deleteBtn should have an icon", dialog.deleteBtn.icon)
      assertEquals(UIScaleUtils.dip(4), dialog.deleteBtn.iconTextGap)
      assertNotNull("closeBtn should have an icon", dialog.closeBtn.icon)
      assertEquals(UIScaleUtils.dip(4), dialog.closeBtn.iconTextGap)
      assertNotNull("clearSearchBtn should have an icon", dialog.clearSearchBtn.icon)
      assertEquals(UIScaleUtils.dip(4), dialog.clearSearchBtn.iconTextGap)
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testSearchFiltersTableModelAppliedStateScopedToContextTag() {
    val modelA = SearchFiltersTableModel(mockPresenter, TAG_A)
    val modelB = SearchFiltersTableModel(mockPresenter, TAG_B)
    val items = listOf(FilterSearchResult("GroupOne", filter1))
    modelA.setItems(items, "")
    modelB.setItems(items, "")

    assertEquals(false, modelA.getValueAt(0, 0))
    assertEquals(false, modelB.getValueAt(0, 0))

    modelA.setValueAt(true, 0, 0)
    assertEquals(true, modelA.getValueAt(0, 0))
    assertEquals(false, modelB.getValueAt(0, 0))
    assertTrue(filter1.isApplied(TAG_A))
    assertFalse(filter1.isApplied(TAG_B))

    modelB.setValueAt(true, 0, 0)
    assertEquals(true, modelA.getValueAt(0, 0))
    assertEquals(true, modelB.getValueAt(0, 0))

    modelA.setValueAt(false, 0, 0)
    assertEquals(false, modelA.getValueAt(0, 0))
    assertEquals(true, modelB.getValueAt(0, 0))
  }
}
