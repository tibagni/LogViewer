package com.tibagni.logviewer.view;

import com.tibagni.logviewer.util.scaling.UIScaleUtils;

import javax.swing.*;
import java.awt.*;

public class LoadingSpinner extends JComponent {
  private final Timer timer;
  private int angle = 0;

  public LoadingSpinner() {
    int dim = Math.max(18, UIScaleUtils.dip(18));
    setPreferredSize(new Dimension(dim, dim));
    setMinimumSize(new Dimension(dim, dim));
    timer = new Timer(50, e -> {
      if (isShowing()) {
        angle = (angle + 30) % 360;
        repaint();
      }
    });
  }

  public boolean isTimerRunning() {
    return timer.isRunning();
  }

  @Override
  public void setVisible(boolean aFlag) {
    super.setVisible(aFlag);
    if (aFlag) {
      if (!timer.isRunning()) {
        timer.start();
      }
    } else {
      timer.stop();
    }
  }

  @Override
  public void removeNotify() {
    super.removeNotify();
    timer.stop();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    int strokeWidth = Math.max(2, UIScaleUtils.dip(2));
    int size = Math.min(getWidth(), getHeight()) - strokeWidth * 2;
    if (size <= 0) {
      g2.dispose();
      return;
    }
    int x = (getWidth() - size) / 2;
    int y = (getHeight() - size) / 2;
    g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    Color highlight = UIManager.getColor("ProgressBar.foreground");
    if (highlight == null) {
      highlight = UIManager.getColor("textHighlight");
    }
    if (highlight == null) {
      highlight = Color.GRAY;
    }
    g2.setColor(new Color(highlight.getRed(), highlight.getGreen(), highlight.getBlue(), 50));
    g2.drawOval(x, y, size, size);
    g2.setColor(highlight);
    g2.drawArc(x, y, size, size, angle, 90);
    g2.dispose();
  }
}
