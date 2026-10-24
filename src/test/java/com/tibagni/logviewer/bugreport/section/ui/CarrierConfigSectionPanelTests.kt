package com.tibagni.logviewer.bugreport.section.ui

import com.formdev.flatlaf.FlatClientProperties
import com.tibagni.logviewer.bugreport.section.CarrierConfigSection
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.CommonIcons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.MouseEvent
import javax.swing.border.MatteBorder

/**
 * Unit tests for [CarrierConfigSectionPanel] verifying vector chevron icons,
 * toolbar button styling, collapsible behavior, table styling, and search
 * filtering.
 */
class CarrierConfigSectionPanelTests {

  @Test
  fun testInitialCollapsedStateWithChevronRight() {
    val configs = mapOf(
      "Default Values" to mapOf(
        "config1" to "val1",
        "config2" to "val2"
      )
    )
    val section = CarrierConfigSection(configs, "sample logs")
    val panel = CarrierConfigSectionPanel(section)

    assertEquals(1, panel.configTables.size)
    val table = panel.configTables.first()
    assertFalse("Table should start collapsed", table.isVisible)

    assertEquals(1, panel.collapseButtons.size)
    val button = panel.collapseButtons.first()
    assertNotNull("Collapse button should have an icon", button.icon)
    assertEquals(14, button.icon.iconWidth)
    assertEquals(
      "ChevronRightIcon",
      button.icon.javaClass.simpleName
    )
    assertTrue(
      "Collapse button text should be empty",
      button.text.isNullOrEmpty()
    )

    assertEquals(1, panel.titleLabels.size)
    val label = panel.titleLabels.first()
    assertEquals("Default Values (2)", label.text)
    assertTrue("Title label font should be bold", label.font.isBold)
    assertEquals(
      Cursor.HAND_CURSOR,
      label.cursor.type
    )
  }

  @Test
  fun testToggleTableVisibilityUpdatesChevronIcon() {
    val configs = mapOf(
      "Carrier Config" to mapOf("keyA" to "valA")
    )
    val section = CarrierConfigSection(configs, "logs")
    val panel = CarrierConfigSectionPanel(section)

    val table = panel.configTables.first()
    val button = panel.collapseButtons.first()

    assertFalse(table.isVisible)
    assertEquals(
      "ChevronRightIcon",
      button.icon.javaClass.simpleName
    )

    // First click expands table and changes chevron to down
    button.doClick()
    assertTrue("Table should become visible", table.isVisible)
    assertEquals(
      "ChevronDownIcon",
      button.icon.javaClass.simpleName
    )

    // Second click collapses table and changes chevron back to right
    button.doClick()
    assertFalse("Table should become hidden", table.isVisible)
    assertEquals(
      "ChevronRightIcon",
      button.icon.javaClass.simpleName
    )
  }

  @Test
  fun testTitleLabelClickTogglesTable() {
    val configs = mapOf(
      "Carrier Config" to mapOf("keyA" to "valA")
    )
    val section = CarrierConfigSection(configs, "logs")
    val panel = CarrierConfigSectionPanel(section)

    val table = panel.configTables.first()
    val button = panel.collapseButtons.first()
    val label = panel.titleLabels.first()

    assertFalse(table.isVisible)
    assertEquals(
      "ChevronRightIcon",
      button.icon.javaClass.simpleName
    )

    // Click title label to expand
    val clickEvent = MouseEvent(
      label,
      MouseEvent.MOUSE_CLICKED,
      System.currentTimeMillis(),
      0,
      0,
      0,
      1,
      false
    )
    label.mouseListeners.forEach { it.mouseClicked(clickEvent) }

    assertTrue("Table should be visible after label click", table.isVisible)
    assertEquals(
      "ChevronDownIcon",
      button.icon.javaClass.simpleName
    )

    // Click title label again to collapse
    label.mouseListeners.forEach { it.mouseClicked(clickEvent) }
    assertFalse("Table should be hidden after second click", table.isVisible)
    assertEquals(
      "ChevronRightIcon",
      button.icon.javaClass.simpleName
    )
  }

  @Test
  fun testSearchFiltersCarrierConfigRows() {
    val configs = mapOf(
      "Source 1" to mapOf(
        "carrier_volte_enabled" to "true",
        "carrier_wfc_mode" to "1",
        "signal_strength_threshold" to "4"
      )
    )
    val section = CarrierConfigSection(configs, "logs")
    val panel = CarrierConfigSectionPanel(section)

    val table = panel.configTables.first()
    assertEquals(3, table.rowCount)

    // Filter matching two rows
    panel.onSearch("carrier")
    assertEquals(2, table.rowCount)
    assertEquals("carrier_volte_enabled", table.getValueAt(0, 0))
    assertEquals("carrier_wfc_mode", table.getValueAt(1, 0))

    // Filter matching single row
    panel.onSearch("volte")
    assertEquals(1, table.rowCount)
    assertEquals("carrier_volte_enabled", table.getValueAt(0, 0))

    // Filter matching zero rows
    panel.onSearch("nonexistent_key")
    assertEquals(0, table.rowCount)

    // Reset filter
    panel.onSearch("")
    assertEquals(3, table.rowCount)
  }

  @Test
  fun testTableStylingProperties() {
    val configs = mapOf(
      "Source" to mapOf("key1" to "val1")
    )
    val section = CarrierConfigSection(configs, "logs")
    val panel = CarrierConfigSectionPanel(section)

    val table = panel.configTables.first()
    assertEquals(UIScaleUtils.dip(22), table.rowHeight)
    assertTrue(table.showHorizontalLines)
    assertFalse(table.showVerticalLines)
    assertTrue(table.fillsViewportHeight)
    assertEquals(Dimension(0, 1), table.intercellSpacing)

    val renderer = table.getDefaultRenderer(Any::class.java)
    assertTrue(
      "Table default renderer should be PropTableRenderer",
      renderer is PropTableRenderer
    )
  }

  @Test
  fun testFlatButtonAndMatteBorderStyling() {
    val configs = mapOf(
      "Source" to mapOf("key1" to "val1")
    )
    val section = CarrierConfigSection(configs, "logs")
    val panel = CarrierConfigSectionPanel(section)

    val button = panel.collapseButtons.first()
    assertEquals(
      FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON,
      button.getClientProperty(FlatClientProperties.BUTTON_TYPE)
    )

    val configPanel = panel.configPanels.first()
    val border = configPanel.border as? MatteBorder
    assertNotNull("Config panel should have a MatteBorder", border)
    assertEquals(0, border!!.borderInsets.top)
    assertEquals(0, border.borderInsets.left)
    assertEquals(1, border.borderInsets.bottom)
    assertEquals(0, border.borderInsets.right)
  }

  @Test
  fun testMultipleCarrierConfigSources() {
    val configs = mapOf(
      "SIM 0" to mapOf("key0" to "val0"),
      "SIM 1" to mapOf("key1" to "val1")
    )
    val section = CarrierConfigSection(configs, "logs")
    val panel = CarrierConfigSectionPanel(section)

    assertEquals(2, panel.configTables.size)
    assertEquals(2, panel.collapseButtons.size)
    assertEquals(2, panel.titleLabels.size)
    assertEquals(2, panel.configPanels.size)

    assertEquals("SIM 0 (1)", panel.titleLabels[0].text)
    assertEquals("SIM 1 (1)", panel.titleLabels[1].text)
  }
}
