package com.tibagni.logviewer.preferences

import java.awt.Component
import java.awt.Font
import javax.swing.JList
import javax.swing.plaf.FontUIResource
import javax.swing.plaf.basic.BasicComboBoxRenderer

class CustomFontCellRender : BasicComboBoxRenderer() {
  override fun getListCellRendererComponent(
    list: JList<*>,
    value: Any,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)

    font = FontUIResource(value as String, Font.PLAIN, 14)
    return this
  }
}