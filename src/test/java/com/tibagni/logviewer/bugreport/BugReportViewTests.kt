package com.tibagni.logviewer.bugreport

import com.formdev.flatlaf.FlatClientProperties
import com.tibagni.logviewer.MainView
import com.tibagni.logviewer.bugreport.section.BugReportSection
import com.tibagni.logviewer.bugreport.section.PlainTextSection
import com.tibagni.logviewer.bugreport.section.SystemPropertiesSection
import com.tibagni.logviewer.bugreport.section.ui.PlainTextBugreportPanel
import com.tibagni.logviewer.bugreport.section.ui.SectionPanel
import com.tibagni.logviewer.bugreport.section.ui.SystemPropertiesSectionPanel
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.view.FlatButton
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*
import javax.swing.JScrollPane
import javax.swing.JTable

/**
 * Unit tests verifying [BugReportViewImpl], modern sidebar navigation, empty state handling,
 * selection lifecycle, theme change updates, and section panel decorations.
 */
class BugReportViewTests {

  @Test
  fun testInitialUiStructure() {
    val mockMain = mock(MainView::class.java)
    val view = BugReportViewImpl(mockMain)

    assertNotNull(view.sidebarPanel)
    assertNotNull(view.mainSplitPane)
    assertNotNull(view.sectionsList)
    assertNotNull(view.sectionsListModel)
    assertNotNull(view.emptyPane)

    assertEquals(view.sidebarPanel, view.mainSplitPane.leftComponent)
    assertEquals(view.emptyPane, view.mainSplitPane.rightComponent)
    assertEquals(I18n.get(I18n.BUGREPORT_SECTIONS_TITLE), view.sidebarHeaderLabel.text)
  }

  @Test
  fun testShowBugReportPopulatesSections() {
    val mockMain = mock(MainView::class.java)
    val view = BugReportViewImpl(mockMain)

    val s1 = object : BugReportSection("Section Alpha") {}
    val s2 = object : BugReportSection("Section Beta") {}
    val report = BugReport("dummy raw", listOf(s1, s2))

    view.showBugReport(report)

    assertEquals(2, view.sectionsListModel.size())
    assertEquals("Section Alpha", view.sectionsListModel[0])
    assertEquals("Section Beta", view.sectionsListModel[1])
    assertEquals(0, view.sectionsList.selectedIndex)
    assertNotEquals(view.emptyPane, view.mainSplitPane.rightComponent)
  }

  @Test
  fun testSelectDifferentSectionSwapsPanel() {
    val mockMain = mock(MainView::class.java)
    val view = BugReportViewImpl(mockMain)

    val s1 = object : BugReportSection("Section One") {}
    val s2 = object : BugReportSection("Section Two") {}
    val report = BugReport("dummy raw", listOf(s1, s2))

    view.showBugReport(report)
    val firstPanel = view.mainSplitPane.rightComponent

    view.sectionsList.selectedIndex = 1
    val secondPanel = view.mainSplitPane.rightComponent

    assertNotEquals(firstPanel, secondPanel)
  }

  @Test
  fun testClearSelectionAndReselectRestoresPanel() {
    val mockMain = mock(MainView::class.java)
    val view = BugReportViewImpl(mockMain)

    val s1 = object : BugReportSection("Section One") {}
    val s2 = object : BugReportSection("Section Two") {}
    val report = BugReport("dummy raw", listOf(s1, s2))

    view.showBugReport(report)
    assertNotEquals(view.emptyPane, view.mainSplitPane.rightComponent)

    view.sectionsList.clearSelection()
    assertEquals(view.emptyPane, view.mainSplitPane.rightComponent)

    view.sectionsList.selectedIndex = 0
    assertNotEquals(view.emptyPane, view.mainSplitPane.rightComponent)
  }

  @Test
  fun testOnBugReportClosedClearsModelAndRestoresEmptyPane() {
    val mockMain = mock(MainView::class.java)
    val view = BugReportViewImpl(mockMain)

    val s1 = object : BugReportSection("Section A") {}
    val report = BugReport("dummy raw", listOf(s1))

    view.showBugReport(report)
    assertEquals(1, view.sectionsListModel.size())
    assertNotEquals(view.emptyPane, view.mainSplitPane.rightComponent)

    view.onBugReportClosed()
    assertEquals(0, view.sectionsListModel.size())
    assertEquals(view.emptyPane, view.mainSplitPane.rightComponent)
  }

  @Test
  fun testOnThemeChangedUpdatesIcons() {
    val mockMain = mock(MainView::class.java)
    val view = BugReportViewImpl(mockMain)

    view.onThemeChanged()
    assertNotNull(view.sidebarHeaderLabel.icon)
    assertNotNull(view.sectionsList.cellRenderer)
  }

  @Test
  fun testSectionPanelLiveSearchDecorations() {
    val panel = object : SectionPanel("Test Section", isSearchable = true) {}

    assertNotNull(panel.searchField)
    assertEquals(
      true,
      panel.searchField?.getClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON)
    )
    assertNotNull(
      panel.searchField?.getClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON)
    )
    assertEquals("Test Section", panel.titleLabel.text)
  }

  @Test
  fun testSystemPropertiesSectionPanel() {
    val configs = mapOf("ro.build.version.release" to "14", "ro.product.model" to "Pixel 8")
    val section = SystemPropertiesSection(configs)
    val panel = SystemPropertiesSectionPanel(section)

    val scrollPane = panel.components.filterIsInstance<JScrollPane>().firstOrNull()
    assertNotNull(scrollPane)
    val table = scrollPane?.viewport?.view as? JTable
    assertNotNull(table)
    assertTrue(table!!.fillsViewportHeight)
    assertEquals(2, table.rowCount)

    panel.onSearch("pixel")
    javax.swing.SwingUtilities.invokeAndWait {}
    assertEquals(1, table.rowCount)
  }

  @Test
  fun testPlainTextBugreportPanel() {
    val section = PlainTextSection("/tmp/bugreport.txt")
    val panel = PlainTextBugreportPanel(section)

    assertTrue(panel.openBugreportBtn is FlatButton)
    assertNotNull(panel.openBugreportBtn.icon)
    assertFalse(panel.disclaimerTxt.isEditable)
    assertTrue(panel.disclaimerTxt.lineWrap)

    // Test notify lifecycle
    panel.addNotify()
    panel.removeNotify()
  }
}
