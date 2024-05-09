package com.tibagni.logviewer.bugreport.section.ui

import com.tibagni.logviewer.bugreport.section.SystemPropertiesSection
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import java.awt.GridBagConstraints
import javax.swing.*
import javax.swing.table.DefaultTableModel

class SystemPropertiesSectionPanel(private val section: SystemPropertiesSection) : SectionPanel(section.sectionName, true) {
  private val headerTitles = arrayOf("Property", "Value")
  private lateinit var tableModel: SysPropTableModel

  init {
    buildUi()
  }

  override fun onSearch(searchText: String) {
    val filteredRows =
      section.configs.filterKeys { it.contains(searchText) }.map { arrayOf(it.key, it.value) }
        .toTypedArray()
    tableModel.setDataVector(filteredRows, arrayOf("Property", "Value"))
  }

  private fun buildUi() {
    val container = JPanel()
    container.layout = BoxLayout(container, BoxLayout.Y_AXIS)

    val rows = section.configs.entries.map { arrayOf(it.key, it.value) }.toTypedArray()

    val table = JTable()
    tableModel = SysPropTableModel(rows, headerTitles)
    table.model = tableModel
    table.tableHeader.resizingAllowed = true

    container.add(table)

    add(
      JScrollPane(container).also { it.verticalScrollBar.unitIncrement = 16 },
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(2)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )
  }
}

private class SysPropTableModel(data: Array<Array<String>>, cols: Array<String>) : DefaultTableModel(data, cols) {
  override fun isCellEditable(row: Int, column: Int) = false
}