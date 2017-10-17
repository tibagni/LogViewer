package com.tibagni.logviewer.util;

import javax.swing.*;
import java.io.File;

public class JFileChooserExt extends JFileChooser {

  private String saveExtension;

  public JFileChooserExt(File currentDirectory) {
    super(currentDirectory);
  }

  /**
   * Set the extension to be added to a file (if not already) when saving
   *
   * @param saveExtension
   */
  public void setSaveExtension(String saveExtension) {
    this.saveExtension = saveExtension;
  }

  @Override
  public void approveSelection() {
    if (getDialogType() == SAVE_DIALOG) {
      setExtensionToSavingFile();

      File savingFile = getSelectedFile();
      if (savingFile != null && savingFile.exists()) {
        int result = JOptionPane.showConfirmDialog(this,
            String.format("The file %s already exists, overwrite?", savingFile.getName()),
            "File already exists!",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);

        switch (result) {
          case JOptionPane.YES_OPTION:
            super.approveSelection();
            break;
          case JOptionPane.CANCEL_OPTION:
            cancelSelection();
            break;
        }

        return;
      }
    }
    super.approveSelection();
  }

  private void setExtensionToSavingFile() {
    File f = getSelectedFile();
    if (f != null && !StringUtils.isEmpty(saveExtension)) {
      if (!f.getName().endsWith(saveExtension)) {
        setSelectedFile(new File(f.getPath() + "." + saveExtension));
      }
    }
  }
}
