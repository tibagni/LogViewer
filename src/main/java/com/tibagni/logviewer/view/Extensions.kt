package com.tibagni.logviewer.view

import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.TableCellRenderer
import kotlin.math.max

fun JTextField.whenTextChanges(callback: (newText: String) -> Unit) {
  document.addDocumentListener(object : DocumentListener {
    override fun insertUpdate(e: DocumentEvent?) {
      callback(text)
    }

    override fun removeUpdate(e: DocumentEvent?) {
      callback(text)
    }

    override fun changedUpdate(e: DocumentEvent?) {
      callback(text)
    }
  })
}

fun JTable.maxWidthOfColumn(col: Int): Int {
  var width = -1
  for (row in 0 until rowCount) {
    val renderer: TableCellRenderer = getCellRenderer(row, col)
    val comp = prepareRenderer(renderer, row, col)
    width = max(comp.preferredSize.width + 1, width)
  }
  return width
}