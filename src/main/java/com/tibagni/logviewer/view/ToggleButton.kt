package com.tibagni.logviewer.view

import com.tibagni.logviewer.util.SwingUtils
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.border.Border

class ToggleButton(imageIcon: ImageIcon, val listener: (Boolean) -> Unit) : FlatButton() {
  private val originalIcon: ImageIcon = SwingUtils.resizeImage(imageIcon, 25, 25)
  private lateinit var selectedIcon: ImageIcon
  private lateinit var normalIcon: ImageIcon
  private var _isActive = false
  val isActive: Boolean
    get() = _isActive

  private lateinit var selectedBorder: Border
  private val normalBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  init {
    addActionListener {
      toggle()
    }

    isBorderPainted = true
    updateIconsAndBorders()
    updateUiState()
  }

  override fun updateUI() {
    super.updateUI()
    @Suppress("SENSELESS_COMPARISON")
    if (originalIcon != null) {
      updateIconsAndBorders()
      updateUiState()
    }
  }

  private fun updateIconsAndBorders() {
    normalIcon = SwingUtils.tintImage(originalIcon, normalColor)
    selectedIcon = SwingUtils.tintImage(originalIcon, rolloverColor)
    selectedBorder = BorderFactory.createCompoundBorder(
      BorderFactory.createMatteBorder(0, 0, 0, 2, rolloverColor),
      BorderFactory.createEmptyBorder(5, 5, 5, 3)
    )
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
      icon = normalIcon
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