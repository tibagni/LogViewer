package com.tibagni.logviewer.view

import com.tibagni.logviewer.util.SwingUtils
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.SwingConstants
import javax.swing.UIManager

open class SideButton(
  initialIcon: ImageIcon,
  tooltip: String,
  var textLabel: String = ""
) : FlatButton() {
  protected var rawIcon: ImageIcon = initialIcon
  protected lateinit var normalIcon: ImageIcon
  protected lateinit var rolloverIconImg: ImageIcon
  var isExpanded: Boolean = false
    set(value) {
      if (field != value) {
        field = value
        if (value) {
          text = textLabel
          horizontalAlignment = SwingConstants.LEFT
          iconTextGap = UIScaleUtils.dip(8)
        } else {
          text = ""
          horizontalAlignment = SwingConstants.CENTER
        }
        updateDimensions()
        revalidate()
        repaint()
      }
    }

  init {
    toolTipText = tooltip
    alignmentX = Component.CENTER_ALIGNMENT
    horizontalAlignment = SwingConstants.CENTER
    border = BorderFactory.createEmptyBorder(
      UIScaleUtils.dip(5),
      UIScaleUtils.dip(5),
      UIScaleUtils.dip(5),
      UIScaleUtils.dip(5)
    )
    updateDimensions()
    updateIcons()
  }

  fun setIconAndTooltip(newIcon: ImageIcon, newTooltip: String, newTextLabel: String = newTooltip) {
    this.rawIcon = newIcon
    this.toolTipText = newTooltip
    this.textLabel = newTextLabel
    if (isExpanded) {
      this.text = newTextLabel
    }
    updateIcons()
    repaint()
  }

  override fun updateUI() {
    super.updateUI()
    @Suppress("SENSELESS_COMPARISON")
    if (rawIcon != null) {
      updateIcons()
      updateDimensions()
    }
  }

  protected fun updateDimensions() {
    val width = if (isExpanded) UIScaleUtils.dip(EXPANDED_WIDTH) else UIScaleUtils.dip(COLLAPSED_WIDTH)
    val height = UIScaleUtils.dip(36)
    maximumSize = Dimension(width, height)
    preferredSize = Dimension(width, height)
    minimumSize = Dimension(width, height)
  }

  protected open fun updateIcons() {
    val resized = SwingUtils.resizeImage(rawIcon, 25, 25)
    normalIcon = SwingUtils.tintImage(resized, normalColor)
    rolloverIconImg = SwingUtils.tintImage(resized, rolloverColor)
    val tintedDisabled = SwingUtils.tintImage(
      resized,
      UIManager.getColor("Label.disabledForeground") ?: Color(160, 160, 160)
    )
    icon = normalIcon
    rolloverIcon = rolloverIconImg
    disabledIcon = tintedDisabled
  }

  override fun onMouseEntered() {
    super.onMouseEntered()
    if (isEnabled) {
      icon = rolloverIconImg
    }
  }

  override fun onMouseExited() {
    super.onMouseExited()
    if (isEnabled) {
      icon = normalIcon
    }
  }

  companion object {
    internal const val COLLAPSED_WIDTH = 36
    internal const val EXPANDED_WIDTH = 165
  }
}
