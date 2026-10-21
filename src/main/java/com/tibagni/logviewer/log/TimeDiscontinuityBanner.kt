package com.tibagni.logviewer.log

import com.formdev.flatlaf.FlatLaf
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.CommonIcons
import com.tibagni.logviewer.view.FlatButton
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Frame
import javax.swing.*

class TimeDiscontinuityBanner(
  private val parentFrameProvider: () -> Frame?
) : JPanel(BorderLayout(UIScaleUtils.dip(10), 0)) {

  private val messageLabel = JLabel().apply {
    icon = CommonIcons.warningIcon(UIScaleUtils.dip(18))
    iconTextGap = UIScaleUtils.dip(8)
  }

  private val viewDetailsBtn = JButton(I18n.get(I18n.TIME_DISCONTINUITY_VIEW_DETAILS)).apply {
    isFocusable = false
    cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
  }

  private val dismissBtn = FlatButton(CommonIcons.closeIcon(UIScaleUtils.dip(14))).apply {
    toolTipText = I18n.get(I18n.TIME_DISCONTINUITY_DISMISS)
    isFocusable = false
  }

  var currentDiscontinuities: List<TimeDiscontinuity> = emptyList()
    private set
  private var onNavigateToLine: ((TimeDiscontinuity) -> Unit)? = null

  private var initialized = false

  init {
    isVisible = false
    buildUi()
    setupActions()
    updateThemeColors()
    initialized = true
  }

  private fun buildUi() {
    add(messageLabel, BorderLayout.CENTER)

    val rightActionsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, UIScaleUtils.dip(6), 0)).apply {
      isOpaque = false
      add(viewDetailsBtn)
      add(dismissBtn)
    }
    add(rightActionsPanel, BorderLayout.EAST)
  }

  private fun setupActions() {
    dismissBtn.addActionListener { dismiss() }

    viewDetailsBtn.addActionListener {
      TimeDiscontinuitiesDialog.show(
        parentFrameProvider(),
        currentDiscontinuities,
        onNavigateToLine
      )
    }
  }

  fun showDiscontinuities(
    discontinuities: List<TimeDiscontinuity>,
    onNavigate: ((TimeDiscontinuity) -> Unit)? = null
  ) {
    if (discontinuities.isEmpty()) {
      dismiss()
      return
    }

    currentDiscontinuities = discontinuities
    this.onNavigateToLine = onNavigate

    val messageText = if (discontinuities.size == 1) {
      val d = discontinuities[0]
      I18n.format(
        I18n.TIME_DISCONTINUITY_SINGLE,
        d.sourceFileName,
        d.lineNumber.toString(),
        d.formatPreviousTime(),
        d.formatCurrentTime()
      )
    } else {
      I18n.format(I18n.TIME_DISCONTINUITY_MULTIPLE, discontinuities.size.toString())
    }

    messageLabel.text = messageText
    isVisible = true
    revalidate()
    repaint()
  }

  fun dismiss() {
    currentDiscontinuities = emptyList()
    onNavigateToLine = null
    isVisible = false
    revalidate()
    repaint()
  }

  override fun updateUI() {
    super.updateUI()
    if (initialized) {
      updateThemeColors()
    }
  }

  private fun updateThemeColors() {
    val isDark = FlatLaf.isLafDark()
    val bgColor = if (isDark) Color(0x3D, 0x33, 0x1A) else Color(0xFE, 0xF9, 0xE7)
    val borderColor = if (isDark) Color(0x66, 0x54, 0x22) else Color(0xF6, 0xE0, 0xA4)
    val textColor = if (isDark) Color(0xFA, 0xDF, 0x8C) else Color(0x73, 0x4B, 0x08)

    background = bgColor
    border = BorderFactory.createCompoundBorder(
      BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor),
      BorderFactory.createEmptyBorder(
        UIScaleUtils.dip(6),
        UIScaleUtils.dip(12),
        UIScaleUtils.dip(6),
        UIScaleUtils.dip(12)
      )
    )
    messageLabel.foreground = textColor
  }
}
