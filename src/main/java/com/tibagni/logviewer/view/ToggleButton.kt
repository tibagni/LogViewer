package com.tibagni.logviewer.view

import javax.swing.BorderFactory
import javax.swing.JLabel

class ToggleButton(unicodeIcon: String) : FlatButton() {
  private val iconLabel = JLabel(unicodeIcon)
  private var _isActive = false
  val isActive: Boolean
    get() = _isActive
  var listener: (Boolean) -> Unit = {}

  private val selectedBorder = BorderFactory.createCompoundBorder(
    BorderFactory.createMatteBorder(0, 0, 0, 2, rolloverColor),
    BorderFactory.createEmptyBorder(5, 5, 5, 3)
  )
  private val normalBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  init {
    add(iconLabel)
    iconLabel.font = font.deriveFont(25f)
    iconLabel.foreground = normalColor
    addActionListener { toggle() }
    isBorderPainted = true
    updateUiState()
  }

  fun toggle() {
    _isActive = !_isActive
    updateUiState()
    listener(_isActive)
  }

  private fun updateUiState() {
    if (_isActive) {
      border = selectedBorder
      iconLabel.foreground = rolloverColor
    } else {
      border = normalBorder
      iconLabel.foreground = normalColor
    }
  }

  override fun onMouseEntered() {
    if (!_isActive) {
      super.onMouseEntered()
      iconLabel.foreground = rolloverColor
    }
  }

  override fun onMouseExited() {
    if (!_isActive) {
      super.onMouseExited()
      iconLabel.foreground = normalColor
    }
  }

  fun setActive(active: Boolean) {
    if (_isActive != active) {
      _isActive = active
      updateUiState()
    }
  }
}