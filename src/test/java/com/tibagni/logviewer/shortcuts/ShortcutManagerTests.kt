package com.tibagni.logviewer.shortcuts

import com.tibagni.logviewer.preferences.KeymapPreferencePanel
import com.tibagni.logviewer.preferences.LogViewerPreferences
import com.tibagni.logviewer.tools.ShortcutReadmeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.awt.Component
import java.awt.GraphicsEnvironment
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.io.File
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.KeyStroke

class ShortcutManagerTests {
  private lateinit var mockPrefs: LogViewerPreferences
  private val storedShortcuts = mutableMapOf<String, String>()

  @Before
  fun setUp() {
    storedShortcuts.clear()
    mockPrefs = mock(LogViewerPreferences::class.java)
    `when`(mockPrefs.getCustomShortcut(org.mockito.Mockito.anyString())).thenAnswer { invocation ->
      val id = invocation.getArgument<String>(0)
      storedShortcuts[id]
    }
    `when`(
      mockPrefs.setCustomShortcut(
        org.mockito.Mockito.anyString(),
        org.mockito.Mockito.nullable(String::class.java)
      )
    ).thenAnswer { invocation ->
      val id = invocation.getArgument<String>(0)
      val value = invocation.getArgument<String?>(1)
      if (value.isNullOrBlank()) {
        storedShortcuts.remove(id)
      } else {
        storedShortcuts[id] = value
      }
      Unit
    }
    `when`(mockPrefs.applyCustomShortcuts(org.mockito.Mockito.anyMap())).thenAnswer { invocation ->
      val map = invocation.getArgument<Map<String, String?>>(0)
      for ((id, value) in map) {
        if (value.isNullOrBlank()) {
          storedShortcuts.remove(id)
        } else {
          storedShortcuts[id] = value
        }
      }
      Unit
    }
  }

  @Test
  fun testAllDefaultShortcutsHaveNoConflictsOnMac() {
    val macManager = ShortcutManager(mockPrefs, isMacProvider = { true })
    val defaults = ShortcutAction.values().associateWith { macManager.getDefaultKeyStroke(it) }
    for (action in ShortcutAction.values()) {
      val ks = defaults.getValue(action)
      val conflicts = macManager.findConflicts(action, ks, defaults)
      assertTrue(
        "Default macOS shortcut for ${action.id} ($ks) conflicts with: $conflicts",
        conflicts.isEmpty()
      )
    }
  }

  @Test
  fun testAllDefaultShortcutsHaveNoConflictsOnNonMac() {
    val winManager = ShortcutManager(mockPrefs, isMacProvider = { false })
    val defaults = ShortcutAction.values().associateWith { winManager.getDefaultKeyStroke(it) }
    for (action in ShortcutAction.values()) {
      val ks = defaults.getValue(action)
      val conflicts = winManager.findConflicts(action, ks, defaults)
      assertTrue(
        "Default Windows/Linux shortcut for ${action.id} ($ks) conflicts with: $conflicts",
        conflicts.isEmpty()
      )
    }
  }

  @Test
  fun testPlatformSpecificDefaults() {
    val macManager = ShortcutManager(mockPrefs, isMacProvider = { true })
    val winManager = ShortcutManager(mockPrefs, isMacProvider = { false })

    // LOGS_REFRESH is Cmd+R on macOS and F5 on Windows/Linux
    assertEquals(
      KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.META_DOWN_MASK),
      macManager.getDefaultKeyStroke(ShortcutAction.LOGS_REFRESH)
    )
    assertEquals(
      KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0),
      winManager.getDefaultKeyStroke(ShortcutAction.LOGS_REFRESH)
    )

    // FILTERS_OPEN is Cmd+Shift+O on macOS and Ctrl+Shift+O on Windows/Linux
    assertEquals(
      KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.META_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK),
      macManager.getDefaultKeyStroke(ShortcutAction.FILTERS_OPEN)
    )
    assertEquals(
      KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK),
      winManager.getDefaultKeyStroke(ShortcutAction.FILTERS_OPEN)
    )
  }

  @Test
  fun testCustomKeyStrokePersistenceAndFallback() {
    val manager = ShortcutManager(mockPrefs, isMacProvider = { false })
    val customKs = KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK or InputEvent.ALT_DOWN_MASK)

    assertFalse(manager.isCustomized(ShortcutAction.LOGS_OPEN))
    assertEquals(
      KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK),
      manager.getKeyStroke(ShortcutAction.LOGS_OPEN)
    )

    manager.setCustomKeyStroke(ShortcutAction.LOGS_OPEN, customKs)
    assertTrue(manager.isCustomized(ShortcutAction.LOGS_OPEN))
    assertEquals(customKs, manager.getKeyStroke(ShortcutAction.LOGS_OPEN))

    // Resetting back to default clears custom preference
    manager.setCustomKeyStroke(ShortcutAction.LOGS_OPEN, manager.getDefaultKeyStroke(ShortcutAction.LOGS_OPEN))
    assertFalse(manager.isCustomized(ShortcutAction.LOGS_OPEN))
    assertNull(storedShortcuts[ShortcutAction.LOGS_OPEN.id])
  }

  @Test
  fun testReadOnlyActionIgnoresCustomKeyStroke() {
    val manager = ShortcutManager(mockPrefs, isMacProvider = { false })
    val customKs = KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK)

    assertFalse(ShortcutAction.FILTERS_TOGGLE_APPLIED.isRebindable)
    manager.setCustomKeyStroke(ShortcutAction.FILTERS_TOGGLE_APPLIED, customKs)
    assertFalse(manager.isCustomized(ShortcutAction.FILTERS_TOGGLE_APPLIED))
    assertEquals(
      KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
      manager.getKeyStroke(ShortcutAction.FILTERS_TOGGLE_APPLIED)
    )
  }

  @Test
  fun testSerializeAndDeserializeKeyStroke() {
    val ks = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)
    val serialized = ShortcutManager.serializeKeyStroke(ks)
    val deserialized = ShortcutManager.deserializeKeyStroke(serialized)
    assertEquals(ks, deserialized)

    assertNull(ShortcutManager.deserializeKeyStroke(null))
    assertNull(ShortcutManager.deserializeKeyStroke(""))
    assertNull(ShortcutManager.deserializeKeyStroke("invalid:data"))
    assertNull(ShortcutManager.deserializeKeyStroke("-1:0"))
  }

  @Test
  fun testFormatKeyStrokeMacAndNonMac() {
    val macKs = KeyStroke.getKeyStroke(
      KeyEvent.VK_F,
      InputEvent.META_DOWN_MASK or InputEvent.ALT_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK
    )
    assertEquals("Cmd+Opt+Shift+F", ShortcutManager.formatKeyStroke(macKs, isMac = true))

    val winKs = KeyStroke.getKeyStroke(
      KeyEvent.VK_F,
      InputEvent.CTRL_DOWN_MASK or InputEvent.ALT_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK
    )
    assertEquals("Ctrl+Alt+Shift+F", ShortcutManager.formatKeyStroke(winKs, isMac = false))

    assertEquals(",", ShortcutManager.formatKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, 0)))
    assertEquals(".", ShortcutManager.formatKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, 0)))
    assertEquals("/", ShortcutManager.formatKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, 0)))
    assertEquals("Space", ShortcutManager.formatKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0)))
    assertEquals("Enter", ShortcutManager.formatKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)))
    assertEquals("Delete", ShortcutManager.formatKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)))
  }

  @Test
  fun testConflictDetectionRespectsScopes() {
    val manager = ShortcutManager(mockPrefs, isMacProvider = { false })

    // Assigning Ctrl+O (LOGS_OPEN, GLOBAL) to LOGS_CLOSE (GLOBAL) conflicts with LOGS_OPEN
    val ctrlO = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK)
    val globalConflicts = manager.findConflicts(ShortcutAction.LOGS_CLOSE, ctrlO)
    assertEquals(listOf(ShortcutAction.LOGS_OPEN), globalConflicts)

    // Assigning Delete to LOGS_REMOVE_MY_LOG (LOGS_TABLE) does NOT conflict with FILTERS_DELETE_SELECTED (FILTERS_LIST)
    val deleteKs = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)
    val scopedConflicts = manager.findConflicts(ShortcutAction.LOGS_REMOVE_MY_LOG, deleteKs)
    assertTrue(scopedConflicts.isEmpty())

    // Assigning a GLOBAL shortcut to Delete conflicts with both contextual Delete actions
    val globalDeleteConflicts = manager.findConflicts(ShortcutAction.LOGS_CLOSE, deleteKs)
    assertTrue(globalDeleteConflicts.contains(ShortcutAction.LOGS_REMOVE_MY_LOG))
    assertTrue(globalDeleteConflicts.contains(ShortcutAction.FILTERS_DELETE_SELECTED))
  }

  @Test
  fun testMatchesKeyEventsAndUnmodifiedTriggerChar() {
    val manager = ShortcutManager(mockPrefs, isMacProvider = { false })
    val dummySource: Component = JLabel()

    // Unmodified '/' trigger char
    assertEquals('/', manager.getUnmodifiedTriggerChar(ShortcutAction.FILTERS_INLINE_SEARCH))
    // Modified Ctrl+F has null unmodified trigger char
    assertNull(manager.getUnmodifiedTriggerChar(ShortcutAction.SEARCH_FIND_IN_TABLE))

    val keyPressedCtrlF = KeyEvent(
      dummySource,
      KeyEvent.KEY_PRESSED,
      0L,
      InputEvent.CTRL_DOWN_MASK,
      KeyEvent.VK_F,
      'f'
    )
    assertTrue(manager.matches(keyPressedCtrlF, ShortcutAction.SEARCH_FIND_IN_TABLE))
    assertFalse(manager.matches(keyPressedCtrlF, ShortcutAction.LOGS_OPEN))

    // International keyboard layout where '/' is typed with Shift+7 (VK_7, keyChar = '/')
    val keyPressedIntlSlash = KeyEvent(
      dummySource,
      KeyEvent.KEY_PRESSED,
      0L,
      InputEvent.SHIFT_DOWN_MASK,
      KeyEvent.VK_7,
      '/'
    )
    assertTrue(manager.matches(keyPressedIntlSlash, ShortcutAction.FILTERS_INLINE_SEARCH))
  }

  @Test
  fun testBindToMenuItemSetsAccelerator() {
    if (GraphicsEnvironment.isHeadless()) return
    val manager = ShortcutManager(mockPrefs, isMacProvider = { false })
    val item = JMenuItem("Open")
    manager.bindToMenuItem(item, ShortcutAction.LOGS_OPEN)
    assertEquals(
      KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK),
      item.accelerator
    )
  }

  @Test
  fun testKeymapPreferencePanelFilteringAndReset() {
    if (GraphicsEnvironment.isHeadless()) return
    val manager = ShortcutManager(mockPrefs, isMacProvider = { false })
    val panel = KeymapPreferencePanel(JLabel(), manager)

    assertEquals(ShortcutAction.values().size, panel.shortcutsTable.rowCount)

    // Filter by query
    panel.filterShortcuts("Timestamp")
    assertEquals(1, panel.shortcutsTable.rowCount)
    assertEquals(ShortcutAction.LOGS_GO_TO_TIMESTAMP, panel.getSelectedAction())

    // Assign non-conflicting custom shortcut
    val newKs = KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK or InputEvent.ALT_DOWN_MASK)
    assertTrue(panel.assignShortcut(ShortcutAction.LOGS_GO_TO_TIMESTAMP, newKs))
    panel.apply()
    assertEquals(newKs, manager.getKeyStroke(ShortcutAction.LOGS_GO_TO_TIMESTAMP))

    // Reset all to defaults
    panel.resetAllToDefaults()
    panel.apply()
    assertFalse(manager.isCustomized(ShortcutAction.LOGS_GO_TO_TIMESTAMP))
  }

  @Test
  fun testReadmeShortcutsTableIsUpToDate() {
    val readmeFile = File("README.md")
    assertTrue("README.md must exist at project root", readmeFile.exists())
    val readmeContent = readmeFile.readText()
    val actualTable = ShortcutReadmeGenerator.extractTableFromReadme(readmeContent)
    assertNotNull(
      "Could not find '## ⌨ Keyboard Shortcuts' table in README.md",
      actualTable
    )
    val expectedTable = ShortcutReadmeGenerator.generateMarkdownTable()
    assertEquals(
      "The Keyboard Shortcuts table in README.md is out of date with ShortcutAction definitions! " +
        "Run './gradlew updateReadmeShortcuts' to regenerate it.",
      expectedTable.trim(),
      actualTable!!.trim()
    )
  }
}
