package com.tibagni.logviewer.view

import java.awt.Component
import javax.swing.BorderFactory
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

class PaddingListCellRenderer(
  private val top: Int,
  private val left: Int,
  private val bottom: Int,
  private val right: Int
) : DefaultListCellRenderer() {
  constructor(padding: Int) : this(padding, padding, padding, padding)

  override fun getListCellRendererComponent(
    list: JList<*>?,
    value: Any?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).also {
      border = BorderFactory.createEmptyBorder(top, left, bottom, right)
    }
  }
}