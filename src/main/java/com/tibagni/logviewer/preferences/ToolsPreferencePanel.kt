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
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel

class ToolsPreferencePanel(
  private val parent: Component,
  private val userPrefs: LogViewerPreferences
) : PreferencePanel {

  override val id = ID
  override val title: String get() = I18n.get(I18n.PREF_CATEGORY_TOOLS)

  @VisibleForTesting
  internal val preferredEditorPathTxt = createPathTextField()

  @VisibleForTesting
  internal val preferredEditorPathBtn = createBrowseButton(
    I18n.get(I18n.PREF_PREFERRED_TEXT_EDITOR),
    CommonIcons.fileDocumentIcon()
  )

  @VisibleForTesting
  internal val adbPathTxt = createPathTextField()

  @VisibleForTesting
  internal val adbPathBtn = createBrowseButton(
    I18n.get(I18n.PREF_ADB_PATH),
    CommonIcons.fileDocumentIcon()
  )

  @VisibleForTesting
  internal val adbPathClearBtn = FlatButton(
    I18n.get(I18n.COMMON_CLEAR)
  ).apply {
    toolTipText = I18n.get(I18n.COMMON_CLEAR)
  }

  private var selectedEditorPath: File? = userPrefs.preferredTextEditor
  private var selectedAdbPath: File? = userPrefs.adbPath
  private var preferredEditorFileChooser: JFileChooser? = null
  private var adbFileChooser: JFileChooser? = null

  override val panel: JComponent = buildPanel()

  init {
    updatePathText(preferredEditorPathTxt, selectedEditorPath?.absolutePath)
    updatePathText(adbPathTxt, selectedAdbPath?.absolutePath)

    preferredEditorPathBtn.addActionListener {
      val chooser = preferredEditorFileChooser
        ?: JFileChooserExt(selectedEditorPath).apply {
          dialogTitle = I18n.get(I18n.PREF_PREFERRED_TEXT_EDITOR)
          preferredEditorFileChooser = this
        }
      chooser.selectedFile = selectedEditorPath
      if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
        val file = chooser.selectedFile
        selectedEditorPath = file
        updatePathText(preferredEditorPathTxt, file.absolutePath)
      }
    }

    adbPathBtn.addActionListener {
      val chooser = adbFileChooser
        ?: JFileChooserExt(selectedAdbPath?.parentFile).apply {
          dialogTitle = I18n.get(I18n.PREF_ADB_PATH)
          adbFileChooser = this
        }
      chooser.selectedFile = selectedAdbPath
      if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
        val file = chooser.selectedFile
        selectedAdbPath = file
        updatePathText(adbPathTxt, file.absolutePath)
      }
    }

    adbPathClearBtn.addActionListener {
      selectedAdbPath = null
      updatePathText(adbPathTxt, null)
    }
  }

  override fun apply() {
    if (selectedEditorPath != userPrefs.preferredTextEditor) {
      userPrefs.preferredTextEditor = selectedEditorPath
    }
    if (selectedAdbPath != userPrefs.adbPath) {
      userPrefs.adbPath = selectedAdbPath
    }
  }

  private fun buildPanel(): JComponent {
    val cc = CellConstraints()
    val form = JPanel(
      FormLayout(
        "fill:pref,left:6dlu:noGrow,fill:default:grow",
        "center:d:noGrow,top:4dlu:noGrow,center:d:noGrow"
      )
    ).apply { isOpaque = false }

    // Row 1: Preferred text editor
    val editorPathLbl = JLabel(I18n.get(I18n.PREF_PREFERRED_TEXT_EDITOR))
    val editorPathPanel = JPanel(BorderLayout(UIScaleUtils.dip(4), 0)).apply {
      isOpaque = false
      add(preferredEditorPathTxt, BorderLayout.CENTER)
      add(preferredEditorPathBtn, BorderLayout.EAST)
    }
    form.add(editorPathLbl, cc.xy(1, 1))
    form.add(editorPathPanel, cc.xy(3, 1))

    // Row 3: ADB path
    val adbPathLbl = JLabel(I18n.get(I18n.PREF_ADB_PATH))
    val adbButtonsPane = JPanel(BorderLayout(UIScaleUtils.dip(4), 0)).apply {
      isOpaque = false
      add(adbPathBtn, BorderLayout.WEST)
      add(adbPathClearBtn, BorderLayout.EAST)
    }
    val adbPathPanel = JPanel(BorderLayout(UIScaleUtils.dip(4), 0)).apply {
      isOpaque = false
      add(adbPathTxt, BorderLayout.CENTER)
      add(adbButtonsPane, BorderLayout.EAST)
    }
    form.add(adbPathLbl, cc.xy(1, 3))
    form.add(adbPathPanel, cc.xy(3, 3))

    return form
  }

  companion object {
    const val ID = "tools"
  }
}
