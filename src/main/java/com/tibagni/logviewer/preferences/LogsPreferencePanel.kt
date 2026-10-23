package com.tibagni.logviewer.preferences

import com.jgoodies.forms.layout.CellConstraints
import com.jgoodies.forms.layout.FormLayout
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.FlatButton
import com.tibagni.logviewer.view.JFileChooserExt
import java.awt.BorderLayout
import java.awt.Component
import java.io.File
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel

class LogsPreferencePanel(
  private val parent: Component,
  private val userPrefs: LogViewerPreferences
) : PreferencePanel {

  override val id = ID
  override val title: String get() = I18n.get(I18n.PREF_CATEGORY_LOGS)

  private val logsPathTxt = createPathTextField()
  private val logsPathBtn = FlatButton("...").apply {
    toolTipText = I18n.get(I18n.PREF_DEFAULT_LOGS_PATH)
  }
  private val showLineNumbersChbx = JCheckBox()
  private val showLogDividersChbx = JCheckBox()

  private var selectedLogsPath: File = userPrefs.defaultLogsPath
  private var logsFolderChooser: JFileChooser? = null

  override val panel: JComponent = buildPanel()

  init {
    updatePathText(logsPathTxt, selectedLogsPath.absolutePath)
    showLineNumbersChbx.isSelected = userPrefs.showLineNumbers
    showLogDividersChbx.isSelected = userPrefs.showLogLineDividers

    logsPathBtn.addActionListener {
      val chooser = logsFolderChooser ?: JFileChooserExt(selectedLogsPath).apply {
        dialogTitle = I18n.get(I18n.PREF_DEFAULT_LOGS_PATH)
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        logsFolderChooser = this
      }
      chooser.selectedFile = selectedLogsPath
      if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
        val file = chooser.selectedFile
        selectedLogsPath = file
        updatePathText(logsPathTxt, file.absolutePath)
      }
    }
  }

  override fun apply() {
    if (selectedLogsPath != userPrefs.defaultLogsPath) {
      userPrefs.defaultLogsPath = selectedLogsPath
    }
    if (showLineNumbersChbx.isSelected != userPrefs.showLineNumbers) {
      userPrefs.showLineNumbers = showLineNumbersChbx.isSelected
    }
    if (showLogDividersChbx.isSelected != userPrefs.showLogLineDividers) {
      userPrefs.showLogLineDividers = showLogDividersChbx.isSelected
    }
  }

  private fun buildPanel(): JComponent {
    val cc = CellConstraints()
    val form = JPanel(
      FormLayout(
        "fill:pref,left:6dlu:noGrow,fill:default:grow",
        "center:d:noGrow,top:4dlu:noGrow,center:d:noGrow,top:4dlu:noGrow,center:d:noGrow"
      )
    ).apply { isOpaque = false }

    // Row 1: Default logs path
    val logsPathLbl = JLabel(I18n.get(I18n.PREF_DEFAULT_LOGS_PATH))
    val logsPathPanel = JPanel(BorderLayout(UIScaleUtils.dip(4), 0)).apply {
      isOpaque = false
      add(logsPathTxt, BorderLayout.CENTER)
      add(logsPathBtn, BorderLayout.EAST)
    }
    form.add(logsPathLbl, cc.xy(1, 1))
    form.add(logsPathPanel, cc.xy(3, 1))

    // Row 3: Show line numbers
    val lineNumbersLbl = JLabel(I18n.get(I18n.PREF_SHOW_LINE_NUMBERS))
    bindLabelToCheckbox(lineNumbersLbl, showLineNumbersChbx)
    form.add(lineNumbersLbl, cc.xy(1, 3))
    form.add(showLineNumbersChbx, cc.xy(3, 3))

    // Row 5: Show log line dividers
    val dividersLbl = JLabel(I18n.get(I18n.PREF_SHOW_LOG_DIVIDERS))
    bindLabelToCheckbox(dividersLbl, showLogDividersChbx)
    form.add(dividersLbl, cc.xy(1, 5))
    form.add(showLogDividersChbx, cc.xy(3, 5))

    return form
  }

  companion object {
    const val ID = "logs"
  }
}
