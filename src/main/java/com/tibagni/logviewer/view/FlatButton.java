package com.tibagni.logviewer.view;

import com.tibagni.logviewer.util.scaling.UIScaleUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.awt.event.MouseEvent;

public class FlatButton extends JButton {
  private Color normalColor;
  private Color rolloverColor;

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
    setOpaque(false);
    setBorder(BorderFactory.createEmptyBorder(UIScaleUtils.dip(2),
            UIScaleUtils.dip(2),
            UIScaleUtils.dip(2),
            UIScaleUtils.dip(2)));
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    normalColor = new Color(UIManager.getColor("Button.foreground").getRGB());
    rolloverColor = new Color(UIManager.getColor("textHighlight").getRGB());
  }

  @Override
  protected void processMouseEvent(MouseEvent e) {
    if (isEnabled()) {
      if (e.getID() == MouseEvent.MOUSE_ENTERED) {
        setForeground(rolloverColor);
      } else if (e.getID() == MouseEvent.MOUSE_EXITED) {
        setForeground(normalColor);
      }
    }

    super.processMouseEvent(e);
  }
}
