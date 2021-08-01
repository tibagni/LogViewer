package com.tibagni.logviewer.bugreport.section.ui

import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.bugreport.section.PlainTextSection
import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.preferences.LogViewerPreferences
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.io.File
import javax.swing.*


class PlainTextBugreportPanel(private val plainTextSection: PlainTextSection) :
  SectionPanel(plainTextSection.sectionName) {

  private var textEditorPath: File? = null
  private lateinit var openBugreportBtn: JButton
  private lateinit var disclaimerTxt: JTextArea

  init {
    buildUi()

    openBugreportBtn.addActionListener { openBugReport() }
    ServiceLocator.logViewerPrefs.addPreferenceListener(object: LogViewerPreferences.Adapter() {
      override fun onPreferredTextEditorChanged() {
        updateTextEditorPath()
      }
    })

    updateTextEditorPath()
  }

  private fun updateTextEditorPath() {
    textEditorPath = findTextEditor()
    updateUiText()
  }

  private fun openBugReport() {
    val bugreportFile = File(plainTextSection.bugReportPath)
    val editorFile = textEditorPath

    if (editorFile != null) {
      try {
        val process = Runtime.getRuntime().exec(editorFile.absolutePath + " " + bugreportFile.absoluteFile)
        Logger.debug("Opened $bugreportFile using $editorFile: $process")
      } catch (ex: Exception) {
        JOptionPane.showMessageDialog(
          this,
          "It was not possible to open the bugreport using ${editorFile.absolutePath}",
          "Error",
          JOptionPane.ERROR_MESSAGE
        )
      }
    } else {
      val input = JOptionPane.showConfirmDialog(
        this,
        "You don't have a preferred text editor, do you want to configure one now",
        "Preferred text editor",
        JOptionPane.YES_NO_OPTION
      )

      if (input == JOptionPane.YES_OPTION) {
        val fileChooser = JFileChooser()
        val result = fileChooser.showOpenDialog(this)
        if (result == JFileChooser.APPROVE_OPTION) {
          val newPreferredEditor = fileChooser.selectedFile
          Logger.debug("New preferred editor selected: $newPreferredEditor")

          ServiceLocator.logViewerPrefs.preferredTextEditor = newPreferredEditor
        }
      }
    }
  }

  private fun findTextEditor(): File? {
    val preferredTextEditor = ServiceLocator.logViewerPrefs.preferredTextEditor
    Logger.debug("Preferred text editor: $preferredTextEditor")

    return preferredTextEditor
  }

  private fun updateUiText() {
    val textEditorName = textEditorPath?.name ?: "a text editor of your preference"
    openBugreportBtn.text = "Open using $textEditorName"
    disclaimerTxt.text = "LogViewer can parse bugreports and present it here in a more user-friendly manner. " +
        "However, there are only a few sections presented this way at this time, and sometimes reading the full " +
        "bugreport is the best choice.\n\n" +

        "If what you are looking for is not in any of the sections available, you can open " +
        "the bugreport file located in \"${plainTextSection.bugReportPath}\" with $textEditorName " +
        "by clicking the button below"
  }

  private fun buildUi() {
    openBugreportBtn = JButton()
    // Use text area here because we want line wrap
    disclaimerTxt = JTextArea()
    disclaimerTxt.lineWrap = true
    disclaimerTxt.wrapStyleWord = true
    disclaimerTxt.isEditable = false

    val panel = JPanel(BorderLayout())
    panel.add(disclaimerTxt, BorderLayout.PAGE_START)
    panel.add(JPanel().also { it.add(openBugreportBtn) }, BorderLayout.CENTER)
    add(
      panel,
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(1)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )
  }
}