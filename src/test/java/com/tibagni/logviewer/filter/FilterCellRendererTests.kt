package com.tibagni.logviewer.filter

import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.log.LogStream
import com.tibagni.logviewer.theme.LogViewerThemeManager
import org.junit.Assert.*
import org.junit.Test
import java.awt.Color
import java.awt.font.TextAttribute
import java.util.UUID
import java.util.function.Supplier
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
    assertTrue(compLight.text.contains("color=\"${LogViewerThemeManager.textForegroundHex}\""))

    LogViewerThemeManager.currentTheme = "Dark"
    val compDark = renderer.getListCellRendererComponent(list, filter, 0, false, false) as FilterCellRenderer
    assertTrue(compDark.text.contains("color=\"${LogViewerThemeManager.textForegroundHex}\""))
  }

  @Test
  fun testCountBadgeVisibleWhenContextInfoPresent() {
    val tag = FilterContextTag.register("testContext-" + UUID.randomUUID())
    val renderer = FilterCellRenderer(Supplier { tag })
    val list = JList<Filter>()
    val filter = Filter("ActivityFilter", "ActivityManager", Color.BLUE, LogLevel.INFO)
    val contextInfo = filter.initContext(tag)
    contextInfo.incrementLineCount(LogStream.MAIN)
    contextInfo.incrementLineCount(LogStream.MAIN)
    contextInfo.incrementLineCount(LogStream.SYSTEM)

    val comp = renderer.getListCellRendererComponent(list, filter, 0, false, false) as FilterCellRenderer
    assertTrue(comp.countBadge.isVisible)
    assertEquals("3", comp.countBadge.text)
  }

  @Test
  fun testCountBadgeHiddenWhenContextInfoAbsent() {
    val tag = FilterContextTag.register("testContext-" + UUID.randomUUID())
    val renderer = FilterCellRenderer(Supplier { tag })
    val list = JList<Filter>()
    val filter = Filter("ActivityFilter", "ActivityManager", Color.BLUE, LogLevel.INFO)

    val comp = renderer.getListCellRendererComponent(list, filter, 0, false, false) as FilterCellRenderer
    assertFalse(comp.countBadge.isVisible)
    assertEquals("", comp.countBadge.text)
  }

  @Test
  fun testHighlightedSearchText() {
    val renderer = FilterCellRenderer()
    renderer.highlightedText = "Activity"
    val list = JList<Filter>()
    val filter = Filter("ActivityManager", "ActivityManager", Color.BLUE, LogLevel.INFO)

    val comp = renderer.getListCellRendererComponent(list, filter, 0, false, false) as FilterCellRenderer
    assertTrue(comp.text.contains("<span style=\"background-color:yellow;color:black\">Activity</span>"))
  }

  @Test
  fun testSelectionBackground() {
    val renderer = FilterCellRenderer()
    val list = JList<Filter>()
    list.selectionBackground = Color(0, 120, 215)
    list.background = Color.WHITE
    val filter = Filter("Name", "Name", Color.BLACK, LogLevel.VERBOSE)

    val unselectedComp = renderer.getListCellRendererComponent(list, filter, 0, false, false) as FilterCellRenderer
    assertEquals(Color.WHITE, unselectedComp.background)

    val selectedComp = renderer.getListCellRendererComponent(list, filter, 0, true, false) as FilterCellRenderer
    assertNotEquals(Color.WHITE, selectedComp.background)
  }

  @Test
  fun testUnderlineForCustomFilterName() {
    val renderer = FilterCellRenderer()
    val list = JList<Filter>()
    val filterCustomName = Filter("Custom Name", "ActualPattern", Color.BLACK, LogLevel.VERBOSE)
    val compCustom = renderer.getListCellRendererComponent(list, filterCustomName, 0, false, false) as FilterCellRenderer
    assertEquals(TextAttribute.UNDERLINE_ON, compCustom.checkBox.font.attributes[TextAttribute.UNDERLINE])

    val filterSameName = Filter("SamePattern", "SamePattern", Color.BLACK, LogLevel.VERBOSE)
    val compSame = renderer.getListCellRendererComponent(list, filterSameName, 0, false, false) as FilterCellRenderer
    assertNull(compSame.checkBox.font.attributes[TextAttribute.UNDERLINE])
  }
}
