package com.tibagni.logviewer.util

import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.logger.Logger
import java.awt.Component
import java.awt.Desktop
import java.awt.GraphicsEnvironment
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JOptionPane

object EditorLauncher {

  /**
   * Opens the specified file using the configured preferred text editor, or falls
   * back to the system default application / prompts the user to select an editor.
   */
  fun openInEditor(parent: Component?, file: File, @Suppress("UNUSED_PARAMETER") line: Int = -1) {
    val preferredEditor = ServiceLocator.logViewerPrefs.preferredTextEditor
    if (preferredEditor != null && preferredEditor.exists()) {
      openWithPreferredEditor(parent, file, preferredEditor)
    } else {
      openWithFallbackEditor(parent, file)
    }
  }

  private fun openWithPreferredEditor(parent: Component?, file: File, preferredEditor: File) {
    try {
      val process = ProcessBuilder(preferredEditor.absolutePath, file.absolutePath).start()
      Logger.debug("Opened $file using $preferredEditor: $process")
    } catch (ex: Exception) {
      Logger.error("Failed to open file using editor $preferredEditor", ex)
      if (!GraphicsEnvironment.isHeadless()) {
        JOptionPane.showMessageDialog(
          parent,
          I18n.format(I18n.LOG_DETAILS_OPEN_EDITOR_ERROR, preferredEditor.absolutePath),
          I18n.get(I18n.COMMON_ERROR),
          JOptionPane.ERROR_MESSAGE
        )
      }
    }
  }

  private fun openWithFallbackEditor(parent: Component?, file: File) {
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
      try {
        Desktop.getDesktop().open(file)
        return
      } catch (ex: Exception) {
        Logger.warning("Desktop.open failed for $file", ex)
      }
    }

    if (!GraphicsEnvironment.isHeadless()) {
      promptForEditor(parent, file)
    }
  }

  private fun promptForEditor(parent: Component?, file: File) {
    val input = JOptionPane.showConfirmDialog(
      parent,
      I18n.get(I18n.LOG_DETAILS_NO_EDITOR_CONFIGURED_MSG),
      I18n.get(I18n.PREF_PREFERRED_TEXT_EDITOR),
      JOptionPane.YES_NO_OPTION
    )

    if (input == JOptionPane.YES_OPTION) {
      val fileChooser = JFileChooser()
      val result = fileChooser.showOpenDialog(parent)
      if (result == JFileChooser.APPROVE_OPTION) {
        val newPreferredEditor = fileChooser.selectedFile
        ServiceLocator.logViewerPrefs.preferredTextEditor = newPreferredEditor
        try {
          ProcessBuilder(newPreferredEditor.absolutePath, file.absolutePath).start()
        } catch (ex: Exception) {
          Logger.error("Failed to open file using newly selected editor $newPreferredEditor", ex)
        }
      }
    }
  }
}
