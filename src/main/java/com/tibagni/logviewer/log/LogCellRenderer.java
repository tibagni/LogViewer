package com.tibagni.logviewer.log;

import com.tibagni.logviewer.filter.Filter;
import com.tibagni.logviewer.logger.Logger;
import com.tibagni.logviewer.util.StringUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogCellRenderer extends JPanel implements TableCellRenderer {
  protected final JTextArea textView;
  private final JPanel colorIndicator;
  private final JLabel streamIndicator;

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

    // Create the stream indicator component but do not show initially
    // Wait until we know it is necessary to show (see showStreams(boolean))
    streamIndicator = new JLabel();
    streamIndicator.setBorder(new EmptyBorder(5, 5, 5, 5));
    streamIndicator.setFont(new Font(Font.MONOSPACED, Font.ITALIC, 12));
  }

  public void showStreams(boolean showStreams) {
    if (showStreams) {
      // Add the component if not already added
      if (streamIndicator.getParent() == null) {
        add(streamIndicator, BorderLayout.LINE_END);
      }
    } else {
      // Remove if the component is added
      if (streamIndicator.getParent() != null) {
        remove(streamIndicator);
      }
    }
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

    Highlighter highlighter = textView.getHighlighter();
    highlighter.removeAllHighlights();

    colorIndicator.setBackground(getColorForLogLevel(logEntry.getLogLevel()));
    streamIndicator.setText(logEntry.getStream().getSymbol());

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

    Filter appliedFilter = logEntry.getAppliedFilter();
    Color filteredColor = appliedFilter != null ? appliedFilter.getColor() : null;
    if (!isSelected && filteredColor != null) {
      textView.setForeground(filteredColor);
    }
    // Apply highlighting if needed
    highlightMatchedText(highlighter, logEntry, isSelected);

    return this;
  }

  private void highlightMatchedText(Highlighter highlighter, LogEntry logEntry, boolean isSelected) {
    Filter appliedFilter = logEntry.getAppliedFilter();
    String hlText = appliedFilter != null ? appliedFilter.getPatternString() : null;
    if (!StringUtils.isEmpty(hlText)) {
      try {
        int flags = appliedFilter.isCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE;
        Pattern pattern = Pattern.compile(hlText, flags);
        Matcher matcher = pattern.matcher(logEntry.getLogText());
        while (matcher.find()) {
          int start = matcher.start();
          int end = matcher.end();
          highlighter.addHighlight(start, end, new DefaultHighlighter.DefaultHighlightPainter(
              getColorForHighlightedText(isSelected)));
        }
      } catch (Exception e) {
        // Should not happen
        Logger.error("Failed to highlight log entry", e);
        highlighter.removeAllHighlights();
      }
    }
  }

  private Color getColorForHighlightedText(boolean isSelected) {
    return isSelected ? new Color(234, 115, 0) : new Color(250, 255, 162);
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
