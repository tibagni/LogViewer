package com.tibagni.logviewer

import com.formdev.flatlaf.FlatClientProperties
import com.tibagni.logviewer.preferences.LogViewerPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.Mockito.mock
import javax.swing.JFrame

import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.SwingUtils
import java.awt.event.KeyEvent
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.KeyStroke

import org.mockito.ArgumentCaptor
import org.mockito.Mockito.verify

/**
 * Unit tests for [MainView] and [MainViewImpl] verifying UI setup and client property configurations.
 */
class MainViewTests {

  /**
   * Verifies that the tabbed pane in [MainViewImpl] has the expected FlatLaf client properties configured:
   * [FlatClientProperties.TABBED_PANE_TAB_TYPE] set to underlined,
   * [FlatClientProperties.TABBED_PANE_SHOW_TAB_SEPARATORS] set to true, and
   * [FlatClientProperties.TABBED_PANE_HAS_FULL_BORDER] set to false.
   */
  @Test
  fun testMainViewTabbedPaneClientProperties() {
    val mockPrefs = mock(LogViewerPreferences::class.java)
    val frame = mock(JFrame::class.java)
    val mainView = MainViewImpl(frame, mockPrefs, emptySet())
    val tabbedPane = mainView.mainTabbedPane

    assertNotNull(tabbedPane)
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
  }

  @Test
  fun testLogsMenuAccelerators() {
    val mockPrefs = mock(LogViewerPreferences::class.java)
    val frame = mock(JFrame::class.java)
    MainViewImpl(frame, mockPrefs, emptySet())

    val menuBarCaptor = ArgumentCaptor.forClass(JMenuBar::class.java)
    verify(frame).jMenuBar = menuBarCaptor.capture()
    val menuBar = menuBarCaptor.value
    assertNotNull(menuBar)

    var logsMenu: JMenu? = null
    for (i in 0 until menuBar.menuCount) {
      val menu = menuBar.getMenu(i)
      if (menu != null && menu.text == I18n.get(I18n.MENU_LOGS)) {
        logsMenu = menu
        break
      }
    }
    assertNotNull(logsMenu)

    val menuShortcutMask = SwingUtils.getMenuShortcutKeyMask()

    var openLogsItem: JMenuItem? = null
    var closeLogsItem: JMenuItem? = null
    for (i in 0 until logsMenu!!.itemCount) {
      val item = logsMenu.getItem(i)
      if (item != null && item.text == I18n.get(I18n.MENU_ITEM_OPEN_LOGS)) {
        openLogsItem = item
      }
      if (item != null && item.text == I18n.get(I18n.MENU_ITEM_CLOSE_LOGS)) {
        closeLogsItem = item
      }
    }

    assertNotNull(openLogsItem)
    assertEquals(
      KeyStroke.getKeyStroke(KeyEvent.VK_O, menuShortcutMask),
      openLogsItem?.accelerator
    )

    assertNotNull(closeLogsItem)
    assertEquals(
      KeyStroke.getKeyStroke(KeyEvent.VK_W, menuShortcutMask),
      closeLogsItem?.accelerator
    )
  }
}
