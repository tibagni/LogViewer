package com.tibagni.logviewer.util;

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import java.awt.*;

public class ProgressMonitorExt extends ProgressMonitor {

  private boolean isCancelable = true;
  private int preferredWidth;

  private boolean removedCancelButton;

  /**
   * Constructs a graphic object that shows progress, typically by filling
   * in a rectangular bar as the process nears completion.
   *
   * @param parentComponent the parent component for the dialog box
   * @param message         a descriptive message that will be shown
   *                        to the user to indicate what operation is being monitored.
   *                        This does not change as the operation progresses.
   *                        See the message parameters to methods in
   *                        {@link JOptionPane#message}
   *                        for the range of values.
   * @param note            a short note describing the state of the
   *                        operation.  As the operation progresses, you can call
   *                        setNote to change the note displayed.  This is used,
   *                        for example, in operations that iterate through a
   *                        list of files to show the name of the file being processes.
   *                        If note is initially null, there will be no note line
   *                        in the dialog box and setNote will be ineffective
   * @param min             the lower bound of the range
   * @param max             the upper bound of the range
   * @see JDialog
   * @see JOptionPane
   */
  public ProgressMonitorExt(Component parentComponent, Object message, String note, int min, int max) {
    super(parentComponent, message, note, min, max);
  }

  public boolean isCancelable() {
    return isCancelable;
  }

  public void setCancelable(boolean cancelable) {
    isCancelable = cancelable;
  }

  public int getPreferredWidth() {
    return preferredWidth;
  }

  public void setPreferredWidth(int preferredWidth) {
    this.preferredWidth = preferredWidth;
  }

  @Override
  public void setProgress(int nv) {
    super.setProgress(nv);

    if (!isCancelable() && !removedCancelButton) {
      JDialog dialog = getProgressDialog();
      if (dialog != null) {
        if (preferredWidth > 0) {
          dialog.setSize(new Dimension(preferredWidth, dialog.getHeight()));
        }

        SwingUtils.removeButtonsFromDialog(dialog);
        removedCancelButton = true;
      }
    }

    if (nv >= getMaximum()) {
      removedCancelButton = false;
    }
  }

  private JDialog getProgressDialog() {
    AccessibleContext ac = getAccessibleContext();
    return (JDialog) ac.getAccessibleParent();
  }
}
