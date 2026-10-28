package com.tibagni.logviewer.bugreport.section.ui

import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.bugreport.section.PlainTextSection
import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.preferences.LogViewerPreferences
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.CardBorderUtils
import com.tibagni.logviewer.view.CommonIcons
import com.tibagni.logviewer.view.FlatButton
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.Insets
import java.io.File
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * Panel displaying plain-text bugreport information with an option to open the
 * raw bugreport in an external text editor.
 *
 * @param plainTextSection plain text section model containing file path.
 */
class PlainTextBugreportPanel(private val plainTextSection: PlainTextSection) :
  SectionPanel(plainTextSection.sectionName) {

  private var textEditorPath: File? = null
  internal lateinit var openBugreportBtn: FlatButton
  internal lateinit var disclaimerTxt: JTextArea

  private val prefsListener = object : LogViewerPreferences.Adapter() {
    override fun onPreferredTextEditorChanged() {
      updateTextEditorPath()
    }
  }

  init {
    buildUi()

    openBugreportBtn.addActionListener { openBugReport() }
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
        val process = Runtime.getRuntime().exec(
          editorFile.absolutePath + " " + bugreportFile.absoluteFile
        )
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
    disclaimerTxt.text = "LogViewer can parse bugreports and present it here in a " +
        "more user-friendly manner. However, there are only a few sections presented " +
        "this way at this time, and sometimes reading the full bugreport is the best choice.\n\n" +
        "If what you are looking for is not in any of the sections available, you can open " +
        "the bugreport file located in \"${plainTextSection.bugReportPath}\" " +
        "with $textEditorName by clicking the button below"
  }

  private fun buildUi() {
    openBugreportBtn = FlatButton("").apply {
      icon = CommonIcons.fileDocumentIcon(UIScaleUtils.dip(16))
    }

    disclaimerTxt = JTextArea().apply {
      lineWrap = true
      wrapStyleWord = true
      isEditable = false
      isOpaque = false
      foreground = CardBorderUtils.resolveSecondaryLabelColor()
      border = EmptyBorder(0, 0, UIScaleUtils.dip(12), 0)
    }

    val cardPanel = JPanel(BorderLayout()).apply {
      border = CardBorderUtils.createCardBorder(
        arc = CardBorderUtils.DEFAULT_CARD_ARC,
        insets = Insets(
          UIScaleUtils.dip(16),
          UIScaleUtils.dip(18),
          UIScaleUtils.dip(16),
          UIScaleUtils.dip(18)
        )
      )
      add(disclaimerTxt, BorderLayout.CENTER)
      val btnPanel = JPanel(FlowLayout(FlowLayout.LEADING, 0, 0)).apply {
        isOpaque = false
        add(openBugreportBtn)
      }
      add(btnPanel, BorderLayout.SOUTH)
    }

    add(
      cardPanel,
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(1)
        .withWeightx(1.0)
        .withAnchor(GridBagConstraints.NORTHWEST)
        .withFill(GridBagConstraints.HORIZONTAL)
        .build()
    )

    add(
      JPanel().apply { isOpaque = false },
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(2)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )
  }

  override fun addNotify() {
    super.addNotify()
    ServiceLocator.logViewerPrefs.addPreferenceListener(prefsListener)
    updateTextEditorPath()
  }

  override fun removeNotify() {
    super.removeNotify()
    ServiceLocator.logViewerPrefs.removePreferenceListener(prefsListener)
  }
}
