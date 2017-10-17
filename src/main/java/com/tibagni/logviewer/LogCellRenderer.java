package com.tibagni.logviewer;

import com.tibagni.logviewer.log.LogEntry;
import com.tibagni.logviewer.log.LogLevel;

import javax.swing.*;
import java.awt.*;

public class LogCellRenderer extends JPanel implements ListCellRenderer<LogEntry> {

  protected JTextArea textView;
  private JPanel colorIndicator;

  public LogCellRenderer() {
    setLayout(new BorderLayout());

    colorIndicator = new JPanel();
    add(colorIndicator, BorderLayout.LINE_START);

    textView = new JTextArea();
    textView.setLineWrap(true);
    textView.setWrapStyleWord(true);
    textView.setMargin(new Insets(5, 10,5 ,10));
    textView.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

    add(textView);
  }

  @Override
  public Component getListCellRendererComponent(JList<? extends LogEntry> list, LogEntry value,
                                                int index, boolean isSelected, boolean cellHasFocus) {

    Color normalBgColor = list.getBackground();
    normalBgColor = (index % 2 == 0) ? normalBgColor : darker(normalBgColor);

    Color selectedBgColor = list.getSelectionBackground();
    Color normalFgColor = list.getForeground();
    Color selectedFgColor = list.getSelectionForeground();

    textView.setText(value.getLogText());
    colorIndicator.setBackground(getColorForLogLevel(value.getLogLevel()));

    textView.setBackground(isSelected ? selectedBgColor : normalBgColor);
    textView.setForeground(isSelected ? selectedFgColor : normalFgColor);

    Color filteredColor = value.getFilterColor();
    if (!isSelected && filteredColor != null) {
      textView.setForeground(filteredColor);
    }

    return this;
  }

  private Color darker(Color color) {
    final double FACTOR = 0.97;
    return new Color(Math.max((int)(color.getRed()  *FACTOR), 0),
        Math.max((int)(color.getGreen()*FACTOR), 0),
        Math.max((int)(color.getBlue() *FACTOR), 0),
        color.getAlpha());
  }

  private Color getColorForLogLevel(LogLevel level) {
    Color logColor = Color.LIGHT_GRAY;
    switch (level) {
      case VERBOSE:
        logColor = new Color(255, 255, 255);
        break;
      case DEBUG:
        logColor = new Color(76, 191, 50);
        break;
      case INFO:
        logColor = new Color(45, 36, 197);
        break;
      case WARNING:
        logColor = new Color(255, 204, 0);
        break;
      case ERROR:
        logColor = new Color(208, 0, 0);
        break;
    }

    return logColor;
  }
}
