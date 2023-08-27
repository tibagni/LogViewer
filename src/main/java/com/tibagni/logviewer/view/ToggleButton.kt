package com.tibagni.logviewer.view

import com.tibagni.logviewer.util.SwingUtils
import javax.swing.BorderFactory
import javax.swing.ImageIcon

class ToggleButton(imageIcon: ImageIcon, val listener: (Boolean) -> Unit) : FlatButton() {
  private val originalIcon: ImageIcon
  private val selectedIcon: ImageIcon
  private val normalIcon: ImageIcon
  private var _isActive = false
  val isActive: Boolean
    get() = _isActive

  private val selectedBorder = BorderFactory.createCompoundBorder(
      BorderFactory.createMatteBorder(0, 0, 0, 2, rolloverColor),
      BorderFactory.createEmptyBorder(5, 5, 5, 3))

  private val normalBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  init {
    addActionListener {
      toggle()
    }

    isBorderPainted = true
    originalIcon = SwingUtils.resizeImage(imageIcon, 25, 25)
    normalIcon = SwingUtils.tintImage(originalIcon, normalColor)
    selectedIcon = SwingUtils.tintImage(originalIcon, rolloverColor)
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
      foreground = rolloverColor
      icon = selectedIcon
    } else {
      border = normalBorder
      foreground = normalColor
      icon = originalIcon
    }
  }

  override fun onMouseEntered() {
    if (!_isActive) {
      super.onMouseEntered()
      icon = selectedIcon
    }
  }

  override fun onMouseExited() {
    if (!_isActive) {
      super.onMouseExited()
      icon = normalIcon
    }
  }
}