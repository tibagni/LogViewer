package com.tibagni.logviewer.bugreport.section.ui

import com.tibagni.logviewer.ServiceLocator
import java.awt.Color
import java.awt.Component
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

class PropTableModel(data: Array<Array<String>>, cols: Array<String>) : DefaultTableModel(data, cols) {
  override fun isCellEditable(row: Int, column: Int) = false
}

class PropTableRenderer : DefaultTableCellRenderer() {
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
