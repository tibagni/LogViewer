package com.tibagni.logviewer.preferences

import com.formdev.flatlaf.FlatClientProperties
import com.tibagni.logviewer.shortcuts.ShortcutAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import java.awt.Component

/**
 * Unit tests verifying [KeymapPreferencePanel] configuration, search field
 * properties, table copy action registration, and filtering behavior.
 */
class KeymapPreferencePanelTests {

  /**
   * Verifies that the search field installs a vector leading search icon
   * and a clear button via FlatLaf client properties.
   */
  @Test
  fun testSearchFieldLeadingIconAndClearButtonConfigured() {
    val parent = mock(Component::class.java)
    val panel = KeymapPreferencePanel(parent)

    val leadingIcon = panel.searchField.getClientProperty(
      FlatClientProperties.TEXT_FIELD_LEADING_ICON
    )
    assertNotNull("searchField must configure TEXT_FIELD_LEADING_ICON", leadingIcon)

    val showClear = panel.searchField.getClientProperty(
      FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON
    )
    assertEquals(true, showClear)
  }

  /**
   * Verifies that the shortcuts table installs a 'copy' ActionMap handler.
   */
  @Test
  fun testShortcutsTableCopyActionRegistered() {
    val parent = mock(Component::class.java)
    val panel = KeymapPreferencePanel(parent)

    val copyAction = panel.shortcutsTable.actionMap.get("copy")
    assertNotNull("shortcutsTable must register 'copy' action", copyAction)
  }

  /**
   * Verifies that shortcut filtering correctly updates visible actions and table row count.
   */
  @Test
  fun testFilterShortcutsUpdatesVisibleActions() {
    val parent = mock(Component::class.java)
    val panel = KeymapPreferencePanel(parent)

    panel.filterShortcuts("Open")
    assertTrue(panel.shortcutsTable.rowCount > 0)

    panel.filterShortcuts("nonexistent_action_query_12345")
    assertEquals(0, panel.shortcutsTable.rowCount)

    panel.filterShortcuts("")
    assertEquals(ShortcutAction.entries.size, panel.shortcutsTable.rowCount)
  }
}
