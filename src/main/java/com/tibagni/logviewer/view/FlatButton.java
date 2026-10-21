package com.tibagni.logviewer.view;

import com.formdev.flatlaf.FlatClientProperties;
import com.tibagni.logviewer.util.scaling.UIScaleUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * A borderless flat button styled as a toolbar button.
 *
 * Configures FlatLaf toolbar button client properties and handles mouse rollover
 * highlights while maintaining consistent padding and cursor styles.
 */
public class FlatButton extends JButton {
  protected Color normalColor;
  protected Color rolloverColor;

  public FlatButton() {
    initialize();
  }

  public FlatButton(Icon icon) {
    super(icon);
    initialize();
  }

  public FlatButton(String text) {
    super(text);
    initialize();
  }

  public FlatButton(Action a) {
    super(a);
    initialize();
  }

  public FlatButton(String text, Icon icon) {
    super(text, icon);
    initialize();
  }

  /**
   * Initializes the flat button properties, configuring the toolbar button client property,
   * focus, border, cursor, and initial foreground colors.
   */
  private void initialize() {
    putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    setFocusPainted(false);
    setBorderPainted(false);
    setContentAreaFilled(false);
    setOpaque(false);
    setBorder(BorderFactory.createEmptyBorder(UIScaleUtils.dip(2),
            UIScaleUtils.dip(2),
            UIScaleUtils.dip(2),
            UIScaleUtils.dip(2)));
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    updateColors();
  }

  @Override
  public void updateUI() {
    super.updateUI();
    updateColors();
  }

  private void updateColors() {
    Color btnForeground = UIManager.getColor("Button.foreground");
    if (btnForeground != null) {
      normalColor = new Color(btnForeground.getRGB());
    }
    Color highlight = UIManager.getColor("textHighlight");
    if (highlight != null) {
      rolloverColor = new Color(highlight.getRGB());
    }
    if (normalColor != null) {
      setForeground(normalColor);
    }
  }

  @Override
  protected void processMouseEvent(MouseEvent e) {
    if (isEnabled()) {
      if (e.getID() == MouseEvent.MOUSE_ENTERED) {
        onMouseEntered();
      } else if (e.getID() == MouseEvent.MOUSE_EXITED) {
        onMouseExited();
      }
    }

    super.processMouseEvent(e);
  }

  protected void onMouseEntered() {
    setForeground(rolloverColor);
  }

  protected void onMouseExited() {
    setForeground(normalColor);
  }
}
