package com.tibagni.logviewer.filter

import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.view.TriStateCheckbox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.awt.Color
import java.awt.GraphicsEnvironment
import javax.swing.SwingUtilities

/**
 * Unit tests for [FiltersList] managing filter groups and visibility.
 */
class FiltersListTests {
  private lateinit var filtersList: FiltersList
  private lateinit var listener: FiltersList.FiltersListener

  private fun onEdt(block: () -> Unit) {
    if (SwingUtilities.isEventDispatchThread()) {
      block()
    } else {
      SwingUtilities.invokeAndWait(block)
    }
  }

  @Before
  fun setUp() {
    assumeFalse(GraphicsEnvironment.isHeadless())
    onEdt {
      filtersList = FiltersList()
      listener = mock(FiltersList.FiltersListener::class.java)
      filtersList.setFiltersListener(listener)
    }
  }

  @Test
  fun testInitialStateIsEmpty() {
    onEdt {
      assertTrue(filtersList.isEmpty)
      assertFalse(filtersList.hasAtLeastOneGroupVisible())
    }
  }

  @Test
  fun testSetFiltersAddsAndSortsGroups() {
    val filterA = Filter("FilterA", "patternA", Color.RED, LogLevel.DEBUG, false)
    val filterB = Filter("FilterB", "patternB", Color.BLUE, LogLevel.INFO, false)

    val map = mapOf(
      "Zebra" to listOf(filterB),
      "Alpha" to listOf(filterA)
    )

    onEdt {
      filtersList.setFilters(map)
      assertFalse(filtersList.isEmpty)
      assertTrue(filtersList.hasAtLeastOneGroupVisible())
      assertEquals(2, filtersList.componentCount)
    }
  }

  @Test
  fun testSetFiltersRemovesOldGroups() {
    val filter1 = Filter("Filter1", "pattern1", Color.RED, LogLevel.DEBUG, false)
    val filter2 = Filter("Filter2", "pattern2", Color.BLUE, LogLevel.INFO, false)

    onEdt {
      filtersList.setFilters(mapOf("Group1" to listOf(filter1), "Group2" to listOf(filter2)))
      assertEquals(2, filtersList.componentCount)

      filtersList.setFilters(mapOf("Group1" to listOf(filter1)))
      assertEquals(1, filtersList.componentCount)
    }
  }

  @Test
  fun testToggleGroupsVisibility() {
    val filter = Filter("Filter1", "pattern1", Color.RED, LogLevel.DEBUG, false)
    onEdt {
      filtersList.setFilters(mapOf("Group1" to listOf(filter)))

      assertTrue(filtersList.hasAtLeastOneGroupVisible())

      filtersList.toggleGroupsVisibility()
      assertFalse(filtersList.hasAtLeastOneGroupVisible())
    }
    verify(listener).onGroupVisibilityChanged(null)

    onEdt {
      filtersList.toggleGroupsVisibility()
      assertTrue(filtersList.hasAtLeastOneGroupVisible())
    }
  }

  @Test
  fun testSelectFilterExpandsGroup() {
    val filter = Filter("Filter1", "pattern1", Color.RED, LogLevel.DEBUG, false)
    onEdt {
      filtersList.setFilters(mapOf("Group1" to listOf(filter)))

      filtersList.setAllGroupsVisibility(false)
      assertFalse(filtersList.hasAtLeastOneGroupVisible())

      filtersList.selectFilter("Group1", filter)
      assertTrue(filtersList.hasAtLeastOneGroupVisible())
    }
  }

  @Test
  fun testEmptyGroupSelectAllState() {
    onEdt {
      filtersList.setFilters(mapOf("EmptyGroup" to emptyList()))
      assertEquals(1, filtersList.componentCount)
      val group = filtersList.getComponent(0) as FilterUIGroup
      assertEquals(0, group.list.model.size)
      // Checkbox should be NOT_SELECTED when group has 0 filters
      assertEquals(TriStateCheckbox.SelectionState.NOT_SELECTED, group.selectAllCb.selectionState)
    }
  }
}
