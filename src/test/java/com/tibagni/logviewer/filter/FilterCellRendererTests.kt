package com.tibagni.logviewer.filter

import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.theme.LogViewerThemeManager
import org.junit.Assert.*
import org.junit.Test
import java.awt.Color
import javax.swing.DefaultListModel
import javax.swing.JList

class FilterCellRendererTests {

  @Test
  fun testBorderPreservedOnUpdateUI() {
    val renderer = FilterCellRenderer()
    val initialBorder = renderer.border
    assertNotNull(initialBorder)

    renderer.updateUI()
    assertEquals(initialBorder, renderer.border)
  }

  @Test
  fun testRendererComponentOnThemeChange() {
    LogViewerThemeManager.currentTheme = "Light"
    val renderer = FilterCellRenderer()
    val list = JList<Filter>()
    val model = DefaultListModel<Filter>()
    val filter = Filter("TestFilter", "pattern", Color.RED, LogLevel.DEBUG)
    model.addElement(filter)
    list.model = model

    val compLight = renderer.getListCellRendererComponent(list, filter, 0, false, false) as FilterCellRenderer
    assertTrue(compLight.text.contains("TestFilter"))
    assertTrue(compLight.text.contains("color=#000")) // dark text for light theme

    LogViewerThemeManager.currentTheme = "Dark"
    val compDark = renderer.getListCellRendererComponent(list, filter, 0, false, false) as FilterCellRenderer
    assertTrue(compDark.text.contains("color=#FFF")) // light text for dark theme
  }
}
