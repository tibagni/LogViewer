package com.tibagni.logviewer.view;

import com.tibagni.logviewer.util.CommonUtils;
import com.tibagni.logviewer.util.scaling.UIScaleUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class Toast extends JFrame {
  private static Toast currentToast;

  private static final int INNER_MARGIN = UIScaleUtils.dip(10);
  private static final int BOTTOM_MARGIN = UIScaleUtils.dip(50);

  public static final int LENGTH_LONG = 3500;
  public static final int LENGTH_SHORT = 2000;

  private final Component parent;
  private final JLabel label;
  private Popup popup;

  private final Color foreground = Color.white;
  private final Color background = Color.darkGray;

  private Toast(Component parent, String message) {
    this.parent = parent;
    this.label = createLabel(message);
  }

  private JLabel createLabel(String text) {
    JLabel label = new JLabel(text);
    label.setOpaque(true);

    Border border = label.getBorder();
    Border margin = new EmptyBorder(INNER_MARGIN, INNER_MARGIN, INNER_MARGIN, INNER_MARGIN);
    label.setBorder(new CompoundBorder(border, margin));

    return label;
  }

  private void show(long duration) {
    label.setForeground(foreground);
    label.setBackground(background);

    Point toastLocation = calculateLocation();
    popup = PopupFactory.getSharedInstance().getPopup(
        parent,
        label,
        toastLocation.x,
        toastLocation.y);

    new Thread(
        () -> {
          popup.show();
          CommonUtils.sleepSilently(duration);
          popup.hide();
        }
    ).start();
  }

  private Point calculateLocation() {
    Point locationOnScreen = parent.getLocationOnScreen();
    Point toastLocation = new Point();
    toastLocation.x = (parent.getWidth() / 2) + locationOnScreen.x;
    toastLocation.y = parent.getHeight() + locationOnScreen.y - BOTTOM_MARGIN;

    return toastLocation;
  }

  public static void showToast(Component parent, String message, int duration) {
    if (currentToast != null) {
      currentToast.popup.hide();
      currentToast = null;
    }

    currentToast = new Toast(parent, message);
    currentToast.show(duration);
  }
}
