package com.tibagni.logviewer.view

import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.FlatLaf
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.event.ActionEvent
import javax.swing.Icon
import javax.swing.JCheckBox
import javax.swing.UIManager

class TriStateCheckbox(text: String? = null) : JCheckBox(text) {
  enum class SelectionState {
    SELECTED, PARTIALLY_SELECTED, NOT_SELECTED
  }

  interface SelectionChangedListener {
    fun onSelectionChanged(newSelectionState: SelectionState)
  }

  private val selectionChangedListeners = mutableSetOf<SelectionChangedListener>()

  private var _selectionState = SelectionState.NOT_SELECTED
  var selectionState: SelectionState
    get() = _selectionState
    set(value) {
      isSelected = value == SelectionState.SELECTED
      _selectionState = value
      updateIndeterminateState()
      repaint()
    }

  init {
    icon = TriStateCheckIcon(this)
    updateIndeterminateState()
    addActionListener(this::actionPerformed)
  }

  private fun updateIndeterminateState() {
    if (_selectionState == SelectionState.PARTIALLY_SELECTED) {
      putClientProperty(FlatClientProperties.SELECTED_STATE, FlatClientProperties.SELECTED_STATE_INDETERMINATE)
    } else {
      putClientProperty(FlatClientProperties.SELECTED_STATE, null)
    }
  }

  override fun updateUI() {
    super.updateUI()
    icon = TriStateCheckIcon(this)
    updateIndeterminateState()
  }

  fun addSelectionChangedListener(listener: SelectionChangedListener) {
    selectionChangedListeners.add(listener)
  }

  private fun actionPerformed(e: ActionEvent) {
    selectionState = if (selectionState != SelectionState.SELECTED) {
      SelectionState.SELECTED
    } else {
      SelectionState.NOT_SELECTED
    }

    selectionChangedListeners.forEach { it.onSelectionChanged(_selectionState) }
  }
}

private class TriStateCheckIcon(private val checkBox: TriStateCheckbox) : Icon {
  private val checkIcon: Icon?
    get() = UIManager.getIcon("CheckBox.icon")
  private val checkColor: Color
    get() = UIManager.getColor("CheckBox.icon.checkmarkColor")
      ?: UIManager.getColor("CheckBox.foreground")
      ?: Color.GRAY

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    checkIcon?.paintIcon(c, g, x, y)
    if (checkBox.selectionState != TriStateCheckbox.SelectionState.PARTIALLY_SELECTED) return

    // If FlatLaf is active, FlatCheckBoxIcon already painted the native vector indeterminate dash
    if (UIManager.getLookAndFeel() is FlatLaf) return

    // Fallback for non-FlatLaf Look and Feels: draw a square to represent the partially selected state
    g.color = checkColor
    g.fillRect(
      x + iconWidth / 2 - iconWidth / 4,
      y + iconHeight / 2 - iconHeight / 4,
      iconWidth / 2,
      iconHeight / 2
    )
  }

  override fun getIconWidth() = checkIcon?.iconWidth ?: 0
  override fun getIconHeight() = checkIcon?.iconHeight ?: 0
}