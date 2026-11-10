package com.tibagni.logviewer.preferences

import com.jgoodies.forms.layout.CellConstraints
import com.jgoodies.forms.layout.FormLayout
import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.CommonIcons
import com.tibagni.logviewer.view.FlatButton
import com.tibagni.logviewer.view.JFileChooserExt
import com.tibagni.logviewer.view.ToolbarDivider
import java.awt.BorderLayout
import java.awt.Component
import java.io.File
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.Icon
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
  override val icon: Icon get() = CommonIcons.toolsIcon(UIScaleUtils.dip(16))

  @VisibleForTesting
  internal val preferredEditorPathTxt = createPathTextField()

  @VisibleForTesting
  internal val preferredEditorPathBtn = createBrowseButton(
    I18n.get(I18n.PREF_PREFERRED_TEXT_EDITOR),
    CommonIcons.fileDocumentIcon(UIScaleUtils.dip(16))
  )

  @VisibleForTesting
  internal val preferredEditorPathClearBtn = FlatButton(
    I18n.get(I18n.COMMON_CLEAR)
  ).apply {
    icon = CommonIcons.clearIcon(UIScaleUtils.dip(14))
    iconTextGap = UIScaleUtils.dip(4)
    toolTipText = I18n.get(I18n.COMMON_CLEAR)
  }

  @VisibleForTesting
  internal val adbPathTxt = createPathTextField()

  @VisibleForTesting
  internal val adbPathBtn = createBrowseButton(
    I18n.get(I18n.PREF_ADB_PATH),
    CommonIcons.fileDocumentIcon(UIScaleUtils.dip(16))
  )

  @VisibleForTesting
  internal val adbPathClearBtn = FlatButton(
    I18n.get(I18n.COMMON_CLEAR)
  ).apply {
    icon = CommonIcons.clearIcon(UIScaleUtils.dip(14))
    iconTextGap = UIScaleUtils.dip(4)
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

    preferredEditorPathClearBtn.addActionListener {
      selectedEditorPath = null
      updatePathText(preferredEditorPathTxt, null)
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

    // Card 1: External Text Editor Card
    val editorForm = JPanel(
      FormLayout(
        "fill:pref,left:6dlu:noGrow,fill:default:grow",
        "center:d:noGrow"
      )
    ).apply { isOpaque = false }

    val editorPathLbl = JLabel(I18n.get(I18n.PREF_PREFERRED_TEXT_EDITOR))
    val editorButtonsPane = JPanel(BorderLayout(UIScaleUtils.dip(4), 0)).apply {
      isOpaque = false
      add(preferredEditorPathBtn, BorderLayout.WEST)
      add(ToolbarDivider(), BorderLayout.CENTER)
      add(preferredEditorPathClearBtn, BorderLayout.EAST)
    }
    val editorPathPanel = JPanel(BorderLayout(UIScaleUtils.dip(4), 0)).apply {
      isOpaque = false
      add(preferredEditorPathTxt, BorderLayout.CENTER)
      add(editorButtonsPane, BorderLayout.EAST)
    }
    editorForm.add(editorPathLbl, cc.xy(1, 1))
    editorForm.add(editorPathPanel, cc.xy(3, 1))

    val editorCard = createPreferenceCard(
      title = I18n.get(I18n.PREF_SECTION_TOOLS_EDITOR),
      icon = CommonIcons.fileDocumentIcon(UIScaleUtils.dip(16)),
      content = editorForm
    )

    // Card 2: Android Debug Bridge (ADB) Card
    val adbForm = JPanel(
      FormLayout(
        "fill:pref,left:6dlu:noGrow,fill:default:grow",
        "center:d:noGrow"
      )
    ).apply { isOpaque = false }

    val adbPathLbl = JLabel(I18n.get(I18n.PREF_ADB_PATH))
    val adbButtonsPane = JPanel(BorderLayout(UIScaleUtils.dip(4), 0)).apply {
      isOpaque = false
      add(adbPathBtn, BorderLayout.WEST)
      add(ToolbarDivider(), BorderLayout.CENTER)
      add(adbPathClearBtn, BorderLayout.EAST)
    }
    val adbPathPanel = JPanel(BorderLayout(UIScaleUtils.dip(4), 0)).apply {
      isOpaque = false
      add(adbPathTxt, BorderLayout.CENTER)
      add(adbButtonsPane, BorderLayout.EAST)
    }
    adbForm.add(adbPathLbl, cc.xy(1, 1))
    adbForm.add(adbPathPanel, cc.xy(3, 1))

    val adbCard = createPreferenceCard(
      title = I18n.get(I18n.PREF_SECTION_TOOLS_ADB),
      icon = CommonIcons.smartphoneIcon(UIScaleUtils.dip(16)),
      content = adbForm
    )

    return JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      isOpaque = false
      add(editorCard)
      add(Box.createVerticalStrut(UIScaleUtils.dip(12)))
      add(adbCard)
    }
  }

  companion object {
    const val ID = "tools"
  }
}
