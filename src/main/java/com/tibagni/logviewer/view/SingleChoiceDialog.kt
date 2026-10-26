package com.tibagni.logviewer.view

import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.Insets
import java.awt.event.ItemEvent
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JScrollPane
import javax.swing.UIManager
import javax.swing.border.EmptyBorder

/**
 * Dialog displaying a list of options allowing the user to select
 * exactly one choice via radio buttons styled inside a rounded card.
 *
 * @param title title displayed in the dialog title bar.
 * @param message header prompt text displayed above the choices card.
 * @param choices array of option label strings to present.
 * @param defaultSelectedIndex initial selection index, or negative
 *        value if no option should be selected by default.
 */
class SingleChoiceDialog(
  private val title: String,
  message: String,
  choices: Array<String>,
  defaultSelectedIndex: Int
) {

  companion object {
    /**
     * Value returned when the user dismisses or cancels the dialog.
     */
    const val DIALOG_CANCELLED = -1

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
    border = EmptyBorder(0, 0, UIScaleUtils.dip(8), 0)
  }

  @VisibleForTesting
  internal val cardPanel: JPanel = JPanel()

  @VisibleForTesting
  internal val choicesButtons: Array<JRadioButton>

  @VisibleForTesting
  internal val scrollPane: JScrollPane?

  @VisibleForTesting
  internal var currentSelection = defaultSelectedIndex

  init {
    dialogPanel.layout = BorderLayout(0, UIScaleUtils.dip(6))
    dialogPanel.border = EmptyBorder(
      UIScaleUtils.dip(4),
      UIScaleUtils.dip(4),
      UIScaleUtils.dip(4),
      UIScaleUtils.dip(4)
    )
    dialogPanel.add(messageLabel, BorderLayout.NORTH)

    cardPanel.layout = BoxLayout(cardPanel, BoxLayout.Y_AXIS)
    cardPanel.isOpaque = false
    val pad = UIScaleUtils.dip(4)
    cardPanel.border = CardBorderUtils.createCardBorder(
      insets = Insets(pad, pad, pad, pad)
    )

    choicesButtons = choices.map { choiceText ->
      JRadioButton(choiceText).apply {
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

    if (defaultSelectedIndex in choices.indices) {
      choicesButtons[defaultSelectedIndex].isSelected = true
    }

    val buttonGroup = ButtonGroup()
    choicesButtons.forEachIndexed { index, button ->
      buttonGroup.add(button)
      cardPanel.add(button)
      button.addActionListener { currentSelection = index }
      button.addItemListener { e ->
        if (e.stateChange == ItemEvent.SELECTED) {
          currentSelection = index
        }
      }
    }

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
   * @return selected option index, or [DIALOG_CANCELLED] if dismissed.
   */
  fun show(parentComponent: Component): Int {
    val closeOption = JOptionPane.showOptionDialog(
      parentComponent,
      dialogPanel,
      title,
      JOptionPane.OK_CANCEL_OPTION,
      JOptionPane.QUESTION_MESSAGE,
      null, null, null
    )

    if (closeOption == JOptionPane.OK_OPTION) {
      return currentSelection
    }

    return DIALOG_CANCELLED
  }
}
