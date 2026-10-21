package com.tibagni.logviewer

import com.formdev.flatlaf.FlatClientProperties
import com.tibagni.logviewer.preferences.LogViewerPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.Mockito.mock
import javax.swing.JFrame

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
}
