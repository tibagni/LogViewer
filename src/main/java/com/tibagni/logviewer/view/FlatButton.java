package com.tibagni.logviewer.view;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.event.MouseEvent;

public class FlatButton extends JButton {
  private static final Border NORMAL_BORDER = new EmptyBorder(5, 5, 5, 5);
  private static final Border HOVER_BORDER = new EmptyBorder(6, 5, 4, 5);

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

  private void initialize() {
    setFocusPainted(false);
    setBorderPainted(false);
    setContentAreaFilled(false);
    setBorder(NORMAL_BORDER);
  }

  @Override
  protected void processMouseEvent(MouseEvent e) {
    if (isEnabled()) {
      if (e.getID() == MouseEvent.MOUSE_ENTERED) {
        setBorder(HOVER_BORDER);
        setForeground(UIManager.getColor("Button.shadow"));
      } else if (e.getID() == MouseEvent.MOUSE_EXITED) {
        setBorder(NORMAL_BORDER);
        setForeground(UIManager.getColor("Button.foreground"));
      }
    }

    super.processMouseEvent(e);
  }
}
