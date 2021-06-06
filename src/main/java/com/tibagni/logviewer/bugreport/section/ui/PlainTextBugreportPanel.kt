package com.tibagni.logviewer.bugreport.section.ui

import com.tibagni.logviewer.bugreport.section.PlainTextSection
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import java.awt.Component
import java.awt.Font
import java.awt.GridBagConstraints
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableCellRenderer


class PlainTextBugreportPanel(private val plainTextSection: PlainTextSection) :
  SectionPanel(plainTextSection.sectionName) {

  init {
    // TODO implement search, load in background
    buildUi()
  }

  private fun buildUi() {
    val bugreportContent = JTable(BugReportTableModel(plainTextSection.bugreportLines))
    bugreportContent.setDefaultRenderer(String::class.java, BugReportTableRenderer())
    bugreportContent.autoscrolls = true
    add(
      JScrollPane(bugreportContent),
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

private class BugReportTableModel(private val lines: List<String>) : AbstractTableModel() {
  override fun getColumnClass(columnIndex: Int) = String::class.java
  override fun getColumnName(column: Int) = "Bug Report"
  override fun getRowCount() = lines.size
  override fun getColumnCount() = 1
  override fun getValueAt(rowIndex: Int, columnIndex: Int) = lines[rowIndex]
}

private class BugReportTableRenderer : JTextArea(), TableCellRenderer {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    font = Font("monospaced", Font.PLAIN, font.size)
    text = value as String
    wrapStyleWord = true
    lineWrap = true
    foreground = if (isSelected) table.selectionForeground else table.foreground
    background = if (isSelected) table.selectionBackground else table.background

    setSize(table.columnModel.getColumn(column).width, preferredSize.height)
    if (table.getRowHeight(row) != preferredSize.height) {
      table.setRowHeight(row, preferredSize.height)
    }

    return this
  }
}