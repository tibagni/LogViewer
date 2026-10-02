package com.tibagni.logviewer.view;

import com.tibagni.logviewer.i18n.I18n;

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
      buttonOK.setText(I18n.get(I18n.COMMON_OK));
      innerPanel.add(buttonOK);
      buttonOK.addActionListener(e -> listener.onOk());
    }

    if (mode != ButtonsMode.OK_ONLY) {
      buttonCancel = new JButton();
      buttonCancel.setText(I18n.get(I18n.COMMON_CANCEL));
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

  public void enableCancelButton(boolean enable) {
    if (buttonCancel != null) {
      buttonCancel.setEnabled(enable);
    }
  }

  public void setOkText(String text) {
    if (buttonOK != null) {
      buttonOK.setText(text);
    }
  }

  public void setCancelText(String text) {
    if (buttonCancel != null) {
      buttonCancel.setText(text);
    }
  }
}
