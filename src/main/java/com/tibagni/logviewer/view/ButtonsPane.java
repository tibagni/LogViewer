package com.tibagni.logviewer.view;

import com.tibagni.logviewer.util.layout.GBConstraintsBuilder;

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
    setLayout(new GridBagLayout());

    if (mode != ButtonsMode.CANCEL_ONLY) {
      buttonOK = new JButton();
      buttonOK.setText("OK");
      add(buttonOK,
          new GBConstraintsBuilder()
              .withGridx(0)
              .withGridy(0)
              .build());
      buttonOK.addActionListener(e -> listener.onOk());
    }

    if (mode != ButtonsMode.OK_ONLY) {
      buttonCancel = new JButton();
      buttonCancel.setText("Cancel");
      add(buttonCancel,
          new GBConstraintsBuilder()
              .withGridx(1)
              .withGridy(0)
              .build());
      buttonCancel.addActionListener(e -> listener.onCancel());
    }
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
