package com.tibagni.logviewer.view;

import com.tibagni.logviewer.i18n.I18n;

import javax.swing.*;
import java.awt.*;

/**
 * Standard button panel containing OK and Cancel action buttons for dialogs
 * and input views.
 */
public class ButtonsPane extends JPanel {
  private JButton buttonOK;
  private JButton buttonCancel;

  /**
   * Display mode determining which buttons are present in the panel.
   */
  public enum ButtonsMode {
    OK_CANCEL,
    OK_ONLY,
    CANCEL_ONLY
  }

  /**
   * Listener interface for button action callbacks.
   */
  public interface Listener {
    /**
     * Invoked when the OK button is clicked.
     */
    void onOk();

    /**
     * Invoked when the Cancel button is clicked.
     */
    void onCancel();
  }

  /**
   * Constructs a new {@code ButtonsPane} with the specified mode and listener.
   *
   * @param mode the button mode defining which buttons to display
   * @param listener the callback listener receiving button click events
   */
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

  /**
   * Returns the OK button instance.
   *
   * @return the {@link JButton} instance for OK, or null if not created
   */
  public JButton getButtonOk() {
    return buttonOK;
  }

  /**
   * Returns the Cancel button instance.
   *
   * @return the {@link JButton} instance for Cancel, or null if not created
   */
  public JButton getButtonCancel() {
    return buttonCancel;
  }

  /**
   * Sets the OK button as the default button for the parent root pane.
   */
  public void setDefaultButtonOk() {
    JRootPane root = getRootPane();
    if (root != null) {
      root.setDefaultButton(buttonOK);
    }
  }

  /**
   * Sets the Cancel button as the default button for the parent root pane.
   */
  public void setDefaultButtonCancel() {
    JRootPane root = getRootPane();
    if (root != null) {
      root.setDefaultButton(buttonCancel);
    }
  }

  /**
   * Enables or disables the OK button.
   *
   * @param enable true to enable the button, false to disable
   */
  public void enableOkButton(boolean enable) {
    if (buttonOK != null) {
      buttonOK.setEnabled(enable);
    }
  }

  /**
   * Enables or disables the Cancel button.
   *
   * @param enable true to enable the button, false to disable
   */
  public void enableCancelButton(boolean enable) {
    if (buttonCancel != null) {
      buttonCancel.setEnabled(enable);
    }
  }

  /**
   * Updates the display text of the OK button.
   *
   * @param text the new button label text
   */
  public void setOkText(String text) {
    if (buttonOK != null) {
      buttonOK.setText(text);
    }
  }

  /**
   * Updates the display text of the Cancel button.
   *
   * @param text the new button label text
   */
  public void setCancelText(String text) {
    if (buttonCancel != null) {
      buttonCancel.setText(text);
    }
  }
}
