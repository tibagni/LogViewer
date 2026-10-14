package com.tibagni.logviewer.view

import com.tibagni.logviewer.util.scaling.UIScaleUtils
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.border.Border

class ToggleSideButton(
  imageIcon: ImageIcon,
  tooltip: String = "",
  initialActive: Boolean = false,
  textLabel: String = "",
  val listener: (Boolean) -> Unit
) : SideButton(imageIcon, tooltip, textLabel) {

  var isActive: Boolean = initialActive
    set(value) {
      if (field != value) {
        field = value
        updateUiState()
      }
    }

  private lateinit var selectedBorder: Border
  private lateinit var normalBorder: Border

  init {
    isBorderPainted = true
    addActionListener {
      toggle()
    }
    updateBorders()
    updateUiState()
  }

  override fun updateUI() {
    super.updateUI()
    @Suppress("SENSELESS_COMPARISON")
    if (rawIcon != null) {
      updateBorders()
      updateUiState()
    }
  }

  private fun updateBorders() {
    normalBorder = BorderFactory.createEmptyBorder(
      UIScaleUtils.dip(5),
      UIScaleUtils.dip(5),
      UIScaleUtils.dip(5),
      UIScaleUtils.dip(5)
    )
    selectedBorder = BorderFactory.createCompoundBorder(
      BorderFactory.createMatteBorder(0, 0, 0, 2, rolloverColor),
      BorderFactory.createEmptyBorder(
        UIScaleUtils.dip(5),
        UIScaleUtils.dip(5),
        UIScaleUtils.dip(5),
        UIScaleUtils.dip(3)
      )
    )
  }

  fun toggle() {
    isActive = !isActive
    listener(isActive)
  }

  private fun updateUiState() {
    if (isActive) {
      border = selectedBorder
      foreground = rolloverColor
      icon = rolloverIconImg
    } else {
      border = normalBorder
      foreground = normalColor
      icon = normalIcon
    }
  }

  override fun onMouseEntered() {
    if (!isActive) {
      super.onMouseEntered()
    }
  }

  override fun onMouseExited() {
    if (!isActive) {
      super.onMouseExited()
    }
  }
}
