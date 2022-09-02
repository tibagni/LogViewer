package com.tibagni.logviewer.view

import java.awt.Component
import javax.swing.*

class MultipleChoiceDialog(
  private val title: String,
  message: String,
  choices: Array<String>,
  allSelectedByDefault: Boolean
) {

  private val dialogPanel: JPanel = JPanel()
  private val choicesButtons: Array<JCheckBox>
  private val selectAllCb: TriStateCheckbox

  init {
    dialogPanel.layout = BoxLayout(dialogPanel, BoxLayout.Y_AXIS)
    dialogPanel.add(JLabel(message))
    dialogPanel.add(JLabel(" ")) // For vertical spacing

    selectAllCb = TriStateCheckbox("Select/Unselect all")
    dialogPanel.add(selectAllCb)
    dialogPanel.add(JLabel(" ")) // For vertical spacing

    choicesButtons = choices.map { JCheckBox(it) }.toTypedArray()
    if (allSelectedByDefault) {
      selectAllCb.selectionState = TriStateCheckbox.SelectionState.SELECTED
      choicesButtons.forEach { it.isSelected = true }
    }

    choicesButtons.forEach { dialogPanel.add(it) }
    choicesButtons.forEach { it.addActionListener { evaluateSelectAllCbState() } }

    selectAllCb.addSelectionChangedListener(object : TriStateCheckbox.SelectionChangedListener {
      override fun onSelectionChanged(newSelectionState: TriStateCheckbox.SelectionState) {
        choicesButtons.forEach { it.isSelected = newSelectionState == TriStateCheckbox.SelectionState.SELECTED }
      }
    })
  }

  private fun evaluateSelectAllCbState() {
    val hasAtLeastOneSelected = choicesButtons.any { it.isSelected }
    val areAllSelected = choicesButtons.all { it.isSelected }
    if (areAllSelected) {
      selectAllCb.selectionState = TriStateCheckbox.SelectionState.SELECTED
    } else if (hasAtLeastOneSelected) {
      selectAllCb.selectionState = TriStateCheckbox.SelectionState.PARTIALLY_SELECTED
    } else {
      selectAllCb.selectionState = TriStateCheckbox.SelectionState.NOT_SELECTED
    }
    selectAllCb.updateUI()
  }

  /**
   * Shows the dialog displaying the provided options.
   * Returns an array indicating the selection of each position or null if the dialog was cancelled
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
