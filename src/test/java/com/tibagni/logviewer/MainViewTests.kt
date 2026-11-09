package com.tibagni.logviewer

import com.formdev.flatlaf.FlatClientProperties
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.preferences.LogViewerPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.mockito.Mockito.mock
import java.awt.GraphicsEnvironment
import java.awt.Insets
import javax.swing.JFrame

/**
 * Unit tests for [MainViewImpl] verifying primary tab navigation, icon assignments,
 * FlatLaf client properties, and menu accelerators.
 */
class MainViewTests {

  /**
   * Verifies that the primary tabbed pane has FlatLaf underlined tab properties,
   * comfortable tab insets, and no full border.
   */
  @Test
  fun testMainViewTabbedPaneClientProperties() {
    assumeFalse(GraphicsEnvironment.isHeadless())
    val parent = JFrame()
    try {
      val prefs = mock(LogViewerPreferences::class.java)
      val mainView = MainViewImpl(parent, prefs, emptySet())
      val tabbedPane = mainView.mainTabbedPane

      assertEquals(
        FlatClientProperties.TABBED_PANE_TAB_TYPE_UNDERLINED,
        tabbedPane.getClientProperty(FlatClientProperties.TABBED_PANE_TAB_TYPE)
      )
      assertEquals(
        true,
        tabbedPane.getClientProperty(FlatClientProperties.TABBED_PANE_SHOW_TAB_SEPARATORS)
      )
      assertEquals(
        false,
        tabbedPane.getClientProperty(FlatClientProperties.TABBED_PANE_HAS_FULL_BORDER)
      )
      assertNotNull(
        tabbedPane.getClientProperty(FlatClientProperties.TABBED_PANE_TAB_INSETS)
      )
      assertNotNull(
        tabbedPane.getClientProperty(FlatClientProperties.TABBED_PANE_TAB_AREA_INSETS)
      )
    } finally {
      parent.dispose()
    }
  }

  /**
   * Verifies that each primary tab (Logs, Bug Report, Live Logs) has its title and
   * dynamic vector icon attached.
   */
  @Test
  fun testMainViewTabIconsAndTitles() {
    assumeFalse(GraphicsEnvironment.isHeadless())
    val parent = JFrame()
    try {
      val prefs = mock(LogViewerPreferences::class.java)
      val mainView = MainViewImpl(parent, prefs, emptySet())
      val tabbedPane = mainView.mainTabbedPane

      assertEquals(3, tabbedPane.tabCount)

      assertEquals(I18n.get(I18n.MAIN_TAB_LOGS), tabbedPane.getTitleAt(0))
      assertNotNull(tabbedPane.getIconAt(0))
      assertEquals(16, tabbedPane.getIconAt(0).iconWidth)

      assertEquals(I18n.get(I18n.MAIN_TAB_BUG_REPORT), tabbedPane.getTitleAt(1))
      assertNotNull(tabbedPane.getIconAt(1))
      assertEquals(16, tabbedPane.getIconAt(1).iconWidth)

      assertEquals(I18n.get(I18n.MAIN_TAB_LIVE_LOGS), tabbedPane.getTitleAt(2))
      assertNotNull(tabbedPane.getIconAt(2))
      assertEquals(16, tabbedPane.getIconAt(2).iconWidth)
    } finally {
      parent.dispose()
    }
  }

  /**
   * Verifies that main menu items have theme-aware vector icons attached.
   */
  @Test
  fun testMenuBarItemIconography() {
    assumeFalse(GraphicsEnvironment.isHeadless())
    val parent = JFrame()
    try {
      val prefs = mock(LogViewerPreferences::class.java)
      MainViewImpl(parent, prefs, emptySet())
      val menuBar = parent.jMenuBar
      assertNotNull(menuBar)

      // Find menu items across all menus and verify icons
      val menus = (0 until menuBar.menuCount).mapNotNull { menuBar.getMenu(it) }
      val menuItems = menus.flatMap { menu ->
        (0 until menu.itemCount).mapNotNull { menu.getItem(it) }
      }

      // Check specific items have non-null icons
      val settingsItem = menuItems.find { it.text == I18n.get(I18n.MENU_ITEM_SETTINGS) }
      assertNotNull(settingsItem)
      assertNotNull(settingsItem?.icon)

      val shortcutsItem = menuItems.find {
        it.text == I18n.get(I18n.MENU_ITEM_KEYBOARD_SHORTCUTS)
      }
      assertNotNull(shortcutsItem)
      assertNotNull(shortcutsItem?.icon)

      val aboutItem = menuItems.find { it.text == I18n.get(I18n.MENU_ITEM_ABOUT) }
      assertNotNull(aboutItem)
      assertNotNull(aboutItem?.icon)

      val helpItem = menuItems.find { it.text == I18n.get(I18n.MENU_ITEM_USER_GUIDE) }
      assertNotNull(helpItem)
      assertNotNull(helpItem?.icon)

      val gotoTsItem = menuItems.find {
        it.text == I18n.get(I18n.MENU_ITEM_GO_TO_TIMESTAMP)
      }
      assertNotNull(gotoTsItem)
      assertNotNull(gotoTsItem?.icon)

      val visibleLogsItem = menuItems.find {
        it.text == I18n.get(I18n.MENU_ITEM_VISIBLE_LOGS)
      }
      assertNotNull(visibleLogsItem)
      assertNotNull(visibleLogsItem?.icon)
    } finally {
      parent.dispose()
    }
  }
}
