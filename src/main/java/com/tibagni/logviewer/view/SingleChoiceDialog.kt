package com.tibagni.logviewer.view

import java.awt.Component
import javax.swing.*

class SingleChoiceDialog(
  private val title: String,
  message: String,
  choices: Array<String>,
  defaultSelectedIndex: Int
) {

  private val dialogPanel: JPanel = JPanel()
  private val choicesButtons: Array<JRadioButton>
  private var currentSelection = defaultSelectedIndex

  companion object {
    const val DIALOG_CANCELLED = -1
  }

  init {
    dialogPanel.layout = BoxLayout(dialogPanel, BoxLayout.Y_AXIS)
    dialogPanel.add(JLabel(message))
    dialogPanel.add(JLabel(" ")) // For vertical spacing

    choicesButtons = choices.map { JRadioButton(it) }.toTypedArray()
    choicesButtons[defaultSelectedIndex].isSelected = true
    choicesButtons.forEach { it.addActionListener { e -> currentSelection = choices.indexOf(e.actionCommand) } }

    val buttonGroup = ButtonGroup()
    choicesButtons.forEach { buttonGroup.add(it) }
    choicesButtons.forEach { dialogPanel.add(it) }
  }

  /**
   * Shows the dialog displaying the provided options.
   * Returns the index of the selected option or DIALOG_CANCELLED if the dialog was cancelled
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