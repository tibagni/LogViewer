package com.tibagni.logviewer.preferences

import com.jgoodies.forms.layout.CellConstraints
import com.jgoodies.forms.layout.FormLayout
import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.CommonIcons
import com.tibagni.logviewer.view.FlatButton
import com.tibagni.logviewer.view.JFileChooserExt
import java.awt.BorderLayout
import java.awt.Component
import java.io.File
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.Icon
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
  override val icon: Icon get() = CommonIcons.logsTabIcon(UIScaleUtils.dip(16))

  @VisibleForTesting
  internal val logsPathTxt = createPathTextField()

  @VisibleForTesting
  internal val logsPathBtn = createBrowseButton(
    I18n.get(I18n.PREF_DEFAULT_LOGS_PATH),
    CommonIcons.folderIcon()
  )
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

    // Card 1: Storage Card
    val storageForm = JPanel(
      FormLayout(
        "fill:pref,left:6dlu:noGrow,fill:default:grow",
        "center:d:noGrow"
      )
    ).apply { isOpaque = false }

    val logsPathLbl = JLabel(I18n.get(I18n.PREF_DEFAULT_LOGS_PATH))
    val logsPathPanel = JPanel(BorderLayout(UIScaleUtils.dip(4), 0)).apply {
      isOpaque = false
      add(logsPathTxt, BorderLayout.CENTER)
      add(logsPathBtn, BorderLayout.EAST)
    }
    storageForm.add(logsPathLbl, cc.xy(1, 1))
    storageForm.add(logsPathPanel, cc.xy(3, 1))

    val storageCard = createPreferenceCard(
      title = I18n.get(I18n.PREF_SECTION_LOGS_STORAGE),
      icon = CommonIcons.folderIcon(UIScaleUtils.dip(16)),
      content = storageForm
    )

    // Card 2: Display Card
    val displayForm = JPanel(
      FormLayout(
        "fill:pref,left:6dlu:noGrow,fill:default:grow",
        "center:d:noGrow,top:4dlu:noGrow,center:d:noGrow"
      )
    ).apply { isOpaque = false }

    val lineNumbersLbl = JLabel(I18n.get(I18n.PREF_SHOW_LINE_NUMBERS))
    bindLabelToCheckbox(lineNumbersLbl, showLineNumbersChbx)
    displayForm.add(lineNumbersLbl, cc.xy(1, 1))
    displayForm.add(showLineNumbersChbx, cc.xy(3, 1))

    val dividersLbl = JLabel(I18n.get(I18n.PREF_SHOW_LOG_DIVIDERS))
    bindLabelToCheckbox(dividersLbl, showLogDividersChbx)
    displayForm.add(dividersLbl, cc.xy(1, 3))
    displayForm.add(showLogDividersChbx, cc.xy(3, 3))

    val displayCard = createPreferenceCard(
      title = I18n.get(I18n.PREF_SECTION_LOGS_DISPLAY),
      icon = CommonIcons.fileDocumentIcon(UIScaleUtils.dip(16)),
      content = displayForm
    )

    return JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      isOpaque = false
      add(storageCard)
      add(Box.createVerticalStrut(UIScaleUtils.dip(12)))
      add(displayCard)
    }
  }

  companion object {
    const val ID = "logs"
  }
}
