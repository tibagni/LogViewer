package com.tibagni.logviewer.view

import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.UIManager
import javax.swing.border.EmptyBorder

/**
 * Dialog displaying a list of options allowing the user to select
 * multiple choices via checkboxes inside a rounded card, with a
 * select-all toggle and a live status counter badge.
 *
 * @param title title displayed in the dialog title bar.
 * @param message header prompt text displayed above the choices card.
 * @param choices array of option label strings to present.
 * @param allSelectedByDefault whether all options are selected initially.
 */
class MultipleChoiceDialog(
  private val title: String,
  message: String,
  choices: Array<String>,
  allSelectedByDefault: Boolean
) {

  companion object {
    /**
     * Maximum visible choice rows before wrapping in a scroll pane.
     */
    private const val MAX_VISIBLE_CHOICES = 6
  }

  @VisibleForTesting
  internal val dialogPanel: JPanel = JPanel()

  @VisibleForTesting
  internal val messageLabel: JLabel = JLabel(message).apply {
    font = font.deriveFont(Font.BOLD)
    foreground = UIManager.getColor("Label.foreground")
  }

  @VisibleForTesting
  internal val selectionBadge: StatusBadge =
    StatusBadge(StatusBadge.Variant.NORMAL)

  @VisibleForTesting
  internal val selectAllCb: TriStateCheckbox =
    TriStateCheckbox(I18n.get(I18n.COMMON_SELECT_UNSELECT_ALL)).apply {
      cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    }

  @VisibleForTesting
  internal val cardPanel: JPanel = JPanel()

  @VisibleForTesting
  internal val choicesButtons: Array<JCheckBox>

  @VisibleForTesting
  internal val scrollPane: JScrollPane?

  private var isUpdatingFromSelectAll = false

  init {
    dialogPanel.layout = BorderLayout(0, UIScaleUtils.dip(6))
    dialogPanel.border = EmptyBorder(
      UIScaleUtils.dip(4),
      UIScaleUtils.dip(4),
      UIScaleUtils.dip(4),
      UIScaleUtils.dip(4)
    )

    val headerRow = JPanel(BorderLayout(UIScaleUtils.dip(8), 0)).apply {
      isOpaque = false
      border = EmptyBorder(0, 0, UIScaleUtils.dip(4), 0)
      add(messageLabel, BorderLayout.WEST)
      add(selectionBadge, BorderLayout.EAST)
    }

    val selectAllRow = JPanel(BorderLayout()).apply {
      isOpaque = false
      border = EmptyBorder(
        UIScaleUtils.dip(2),
        0,
        UIScaleUtils.dip(6),
        0
      )
      add(selectAllCb, BorderLayout.WEST)
    }

    val topPanel = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      isOpaque = false
      headerRow.alignmentX = Component.LEFT_ALIGNMENT
      selectAllRow.alignmentX = Component.LEFT_ALIGNMENT
      add(headerRow)
      add(selectAllRow)
    }
    dialogPanel.add(topPanel, BorderLayout.NORTH)

    cardPanel.layout = BoxLayout(cardPanel, BoxLayout.Y_AXIS)
    cardPanel.isOpaque = false
    val pad = UIScaleUtils.dip(4)
    cardPanel.border = CardBorderUtils.createCardBorder(
      insets = Insets(pad, pad, pad, pad)
    )

    choicesButtons = choices.map { choiceText ->
      JCheckBox(choiceText).apply {
        border = EmptyBorder(
          UIScaleUtils.dip(4),
          UIScaleUtils.dip(6),
          UIScaleUtils.dip(4),
          UIScaleUtils.dip(6)
        )
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        alignmentX = Component.LEFT_ALIGNMENT
      }
    }.toTypedArray()

    choicesButtons.forEach { cardPanel.add(it) }

    if (allSelectedByDefault) {
      selectAllCb.selectionState = TriStateCheckbox.SelectionState.SELECTED
      choicesButtons.forEach { it.isSelected = true }
    } else {
      selectAllCb.selectionState = TriStateCheckbox.SelectionState.NOT_SELECTED
      choicesButtons.forEach { it.isSelected = false }
    }
    updateSelectionBadge()

    choicesButtons.forEach { button ->
      button.addItemListener {
        if (!isUpdatingFromSelectAll) {
          evaluateSelectAllCbState()
        }
      }
    }

    selectAllCb.addSelectionChangedListener(
      object : TriStateCheckbox.SelectionChangedListener {
        override fun onSelectionChanged(
          newSelectionState: TriStateCheckbox.SelectionState
        ) {
          isUpdatingFromSelectAll = true
          try {
            val selectAll =
              newSelectionState == TriStateCheckbox.SelectionState.SELECTED
            choicesButtons.forEach { it.isSelected = selectAll }
          } finally {
            isUpdatingFromSelectAll = false
          }
          updateSelectionBadge()
        }
      }
    )

    if (choices.size > MAX_VISIBLE_CHOICES) {
      val sp = JScrollPane(cardPanel).apply {
        border = BorderFactory.createEmptyBorder()
        viewport.isOpaque = false
        isOpaque = false
        verticalScrollBar.unitIncrement = UIScaleUtils.dip(16)
        val prefWidth = maxOf(
          cardPanel.preferredSize.width + UIScaleUtils.dip(24),
          UIScaleUtils.dip(260)
        )
        preferredSize = Dimension(prefWidth, UIScaleUtils.dip(220))
      }
      scrollPane = sp
      dialogPanel.add(sp, BorderLayout.CENTER)
    } else {
      scrollPane = null
      dialogPanel.add(cardPanel, BorderLayout.CENTER)
    }
  }

  /**
   * Evaluates the selection state of all choice checkboxes and updates
   * [selectAllCb] and [selectionBadge] accordingly.
   */
  @VisibleForTesting
  internal fun evaluateSelectAllCbState() {
    val hasAtLeastOneSelected = choicesButtons.any { it.isSelected }
    val areAllSelected =
      choicesButtons.isNotEmpty() && choicesButtons.all { it.isSelected }
    if (areAllSelected) {
      selectAllCb.selectionState = TriStateCheckbox.SelectionState.SELECTED
    } else if (hasAtLeastOneSelected) {
      selectAllCb.selectionState =
        TriStateCheckbox.SelectionState.PARTIALLY_SELECTED
    } else {
      selectAllCb.selectionState = TriStateCheckbox.SelectionState.NOT_SELECTED
    }
    updateSelectionBadge()
  }

  /**
   * Refreshes the [selectionBadge] counter text to reflect the current
   * number of selected choices out of the total count.
   */
  private fun updateSelectionBadge() {
    val selectedCount = choicesButtons.count { it.isSelected }
    val totalCount = choicesButtons.size
    selectionBadge.text = "$selectedCount / $totalCount" // i18n:ignore
    selectionBadge.isVisible = true
    selectionBadge.repaint()
  }

  /**
   * Resolves the theme-adaptive border stroke color with dark and
   * light fallback values.
   */
  @VisibleForTesting
  internal fun resolveBorderColor(): Color =
    CardBorderUtils.resolveCardBorderColor()

  /**
   * Shows the dialog displaying the provided options.
   *
   * @param parentComponent parent swing component for dialog positioning.
   * @return array indicating selection state of each option, or null
   *         if the dialog was cancelled.
   */
  fun show(parentComponent: Component): Array<Boolean>? {
    val closeOption = JOptionPane.showOptionDialog(
      parentComponent,
      dialogPanel,
      title,
      JOptionPane.OK_CANCEL_OPTION,
      JOptionPane.QUESTION_MESSAGE,
      null, null, null
    )

    if (closeOption == JOptionPane.OK_OPTION) {
      return choicesButtons.map { it.isSelected }.toTypedArray()
    }

    return null
  }
}
