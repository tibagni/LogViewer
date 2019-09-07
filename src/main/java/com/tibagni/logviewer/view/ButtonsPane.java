package com.tibagni.logviewer.view;

import javax.swing.*;
import java.awt.*;

public class ButtonsPane extends JPanel {
  private JButton buttonOK;
  private JButton buttonCancel;

  public enum ButtonsMode {
    OK_CANCEL,
    OK_ONLY,
    CANCEL_ONLY
  }

  public interface Listener {
    void onOk();

    void onCancel();
  }

  public ButtonsPane(ButtonsMode mode, Listener listener) {
    setLayout(new FlowLayout(FlowLayout.RIGHT));
    JPanel innerPanel = new JPanel();

    if (mode != ButtonsMode.CANCEL_ONLY) {
      buttonOK = new JButton();
      buttonOK.setText("OK");
      innerPanel.add(buttonOK);
      buttonOK.addActionListener(e -> listener.onOk());
    }

    if (mode != ButtonsMode.OK_ONLY) {
      buttonCancel = new JButton();
      buttonCancel.setText("Cancel");
      innerPanel.add(buttonCancel);
      buttonCancel.addActionListener(e -> listener.onCancel());
    }

    add(innerPanel);
  }

  public void setDefaultButtonOk() {
    getRootPane().setDefaultButton(buttonOK);
  }

  public void setDefaultButtonCancel() {
    getRootPane().setDefaultButton(buttonCancel);
  }

  public void enableOkButton(boolean enable) {
    buttonOK.setEnabled(enable);
  }
}
