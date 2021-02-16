package com.tibagni.logviewer.bugreport.section.ui

import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.bugreport.section.PropertiesSection
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.view.maxWidthOfColumn
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.GridBagConstraints
import javax.swing.BorderFactory
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

import javax.swing.table.DefaultTableCellRenderer


class PropertiesSectionPanel(private val section: PropertiesSection) : SectionPanel(section.sectionName) {
  init {
    buildUi()
  }

  private fun buildUi() {
    val data = arrayOf(
      arrayOf("Build", section.build),
      arrayOf("Fingerprint", section.fingerprint),
      arrayOf("Bootloader", section.bootloader),
      arrayOf("Kernel", section.kernel),
      arrayOf("Uptime", section.uptime)
    )
    val propTable = JTable()
    propTable.model = PropTableModel(data, arrayOf("Property", "Value"))
    propTable.setDefaultRenderer(Object::class.java, PropTableRenderer())

    // Resize the first column according to its maximum width
    val propColWidth = propTable.maxWidthOfColumn(0)
    propTable.columnModel.getColumn(0).minWidth = propColWidth * 1.5.toInt()
    propTable.columnModel.getColumn(0).maxWidth = propColWidth * 3
    propTable.columnModel.getColumn(0).preferredWidth = propColWidth * 2

    propTable.tableHeader.resizingAllowed = true
    add(
      JScrollPane(propTable),
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(1)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )
  }
}

private class PropTableModel(data: Array<Array<String>>, cols: Array<String>) : DefaultTableModel(data, cols) {
  override fun isCellEditable(row: Int, column: Int) = false
}

private class PropTableRenderer : DefaultTableCellRenderer() {
  private val borderColor = if (ServiceLocator.themeManager.isDark) Color.GRAY else Color.DARK_GRAY
  override fun getTableCellRendererComponent(
    table: JTable?,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
    border = BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor)
    val fontStyle = if (column == 0) Font.BOLD else Font.ITALIC
    font = Font(font.name, fontStyle, font.size)
    return this
  }
}