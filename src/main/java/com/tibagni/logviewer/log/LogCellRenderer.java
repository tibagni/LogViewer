package com.tibagni.logviewer.log;

import com.tibagni.logviewer.logger.Logger;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class LogCellRenderer extends JPanel implements TableCellRenderer {
  protected final JTextArea textView;
  private final JPanel colorIndicator;

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
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                 boolean hasFocus, int row, int column) {
    LogEntry logEntry = (LogEntry) value;
    if (logEntry == null) {
      Logger.debug("getTableCellRenderedComponent passed a null value at position (" + row + ", " + column + ")");
      logEntry = (LogEntry) table.getModel().getValueAt(row, column);
      if (logEntry == null) {
        Logger.error("TableModel has a null value at position (" + row + ", " + column + ")");
        return null;
      }
    }

    textView.setText(logEntry.getLogText());
    textView.setWrapStyleWord(true);
    textView.setLineWrap(true);

    colorIndicator.setBackground(getColorForLogLevel(logEntry.getLogLevel()));

    setSize(table.getColumnModel().getColumn(column).getWidth(), getPreferredSize().height);
    if (table.getRowHeight(row) != getPreferredSize().height) {
      table.setRowHeight(row, getPreferredSize().height);
    }

    if (isSelected) {
      textView.setForeground(table.getSelectionForeground());
      textView.setBackground(table.getSelectionBackground());
    } else {
      Color background = table.getBackground();
      if (background == null || background instanceof javax.swing.plaf.UIResource) {
        Color alternateColor = UIManager.getColor("Table.alternateRowColor");
        if (alternateColor != null && row % 2 != 0) {
          background = alternateColor;
        }
      }

      textView.setForeground(table.getForeground());
      textView.setBackground(background);
    }

    Color filteredColor = logEntry.getFilterColor();
    if (!isSelected && filteredColor != null) {
      textView.setForeground(filteredColor);
    }

    return this;
  }


  private Color getColorForLogLevel(LogLevel level) {
    Color logColor = Color.LIGHT_GRAY;
    switch (level) {
      case VERBOSE:
        logColor = new Color(165, 255, 183);
        break;
      case DEBUG:
        logColor = new Color(147, 178, 191);
        break;
      case INFO:
        logColor = new Color(124, 124, 124);
        break;
      case WARNING:
        logColor = new Color(217, 204, 135);
        break;
      case ERROR:
        logColor = new Color(208, 108, 89);
        break;
    }

    return logColor;
  }

}
