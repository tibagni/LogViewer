package com.tibagni.logviewer.bugreport.section.ui

import com.tibagni.logviewer.bugreport.section.SystemPropertiesSection
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import java.awt.GridBagConstraints
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.SwingUtilities
import javax.swing.table.DefaultTableModel

/**
 * Panel displaying system properties key-value pairs parsed from bugreports.
 *
 * @param section system properties section model containing property entries.
 */
class SystemPropertiesSectionPanel(
  private val section: SystemPropertiesSection
) : SectionPanel(section.sectionName, true) {
  private val headerTitles = arrayOf("Property", "Value")
  private lateinit var tableModel: SysPropTableModel

  init {
    buildUi()
  }

  public override fun onSearch(searchText: String) {
    val filteredRows = section.configs
      .filter { (key, value) ->
        key.contains(searchText, ignoreCase = true) ||
            value.contains(searchText, ignoreCase = true)
      }
      .map { arrayOf(it.key, it.value) }
      .toTypedArray()
    if (SwingUtilities.isEventDispatchThread()) {
      tableModel.setDataVector(filteredRows, headerTitles)
    } else {
      SwingUtilities.invokeLater {
        tableModel.setDataVector(filteredRows, headerTitles)
      }
    }
  }

  private fun buildUi() {
    val rows = section.configs.entries
      .map { arrayOf(it.key, it.value) }
      .toTypedArray()

    val table = JTable().apply {
      model = SysPropTableModel(rows, headerTitles).also { tableModel = it }
      tableHeader.resizingAllowed = true
      fillsViewportHeight = true
    }

    add(
      JScrollPane(table).apply {
        verticalScrollBar.unitIncrement = 16
      },
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

private class SysPropTableModel(
  data: Array<Array<String>>,
  cols: Array<String>
) : DefaultTableModel(data, cols) {
  override fun isCellEditable(row: Int, column: Int) = false
}
