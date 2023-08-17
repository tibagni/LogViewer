package com.tibagni.logviewer.log;

import com.tibagni.logviewer.ServiceLocator;
import com.tibagni.logviewer.filter.Filter;
import com.tibagni.logviewer.logger.Logger;
import com.tibagni.logviewer.theme.LogViewerThemeManager;
import com.tibagni.logviewer.util.StringUtils;
import com.tibagni.logviewer.util.scaling.UIScaleUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogCellRenderer extends JPanel implements TableCellRenderer {
  private final JLabel lineNumLabel;
  protected final JTextArea textView;
  private final JPanel colorIndicator;
  private final JLabel streamIndicator;
  private final LogViewerThemeManager themeManager;
  private int mHighlightLine = -1;
  private final FontRenderContext fontRenderContext = new FontRenderContext(new AffineTransform(), true, true);

  public LogCellRenderer() {
    themeManager = ServiceLocator.INSTANCE.getThemeManager();
    setLayout(new BorderLayout());

    JPanel leftPane = new JPanel(new BorderLayout());
    add(leftPane, BorderLayout.LINE_START);

    colorIndicator = new JPanel();
    leftPane.add(colorIndicator, BorderLayout.LINE_START);

    lineNumLabel = new JLabel();
    lineNumLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    lineNumLabel.setPreferredSize(new Dimension(UIScaleUtils.dip(70),
        UIScaleUtils.dip(20)));
    lineNumLabel.setBorder(new EmptyBorder(0,0,0,
        UIScaleUtils.dip(10)));
    leftPane.add(lineNumLabel);

    textView = new JTextArea();
    textView.setLineWrap(true);
    textView.setWrapStyleWord(true);
    textView.setMargin(new Insets(UIScaleUtils.dip(5),
            UIScaleUtils.dip(10),
            UIScaleUtils.dip(5),
            UIScaleUtils.dip(10)));
    int fontSize = textView.getFont().getSize();
    textView.setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));
    add(textView);

    // Create the stream indicator component but do not show initially
    // Wait until we know it is necessary to show (see showStreams(boolean))
    streamIndicator = new JLabel();
    streamIndicator.setBorder(new EmptyBorder(UIScaleUtils.dip(5),
            UIScaleUtils.dip(5),
            UIScaleUtils.dip(5),
            UIScaleUtils.dip(5)));
    fontSize = textView.getFont().getSize();
    streamIndicator.setFont(new Font(Font.MONOSPACED, Font.ITALIC, fontSize));
  }

  public void showLineNumbers(boolean showLineNumbers) {
    lineNumLabel.setVisible(showLineNumbers);
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

  public void highlightLine(int rowIndex) {
    mHighlightLine = rowIndex;
  }

  public void recalculateLineNumberPreferredSize(LogListTableModel model) {
    if (!lineNumLabel.isVisible()) return;

    int maxIndex = -1;
    for (int index = 0; index < model.getRowCount(); index++) {
      LogEntry logEntry = (LogEntry) model.getValueAt(index, 0);
      if (logEntry.getIndex() > maxIndex) {
        maxIndex = logEntry.getIndex();
      }
    }

    if (maxIndex != -1) {
      String line = String.valueOf(maxIndex + 1);
      int width = (int) getFont().getStringBounds(line, fontRenderContext).getWidth();

      // size = string width + border size
      lineNumLabel.setPreferredSize(new Dimension(width + UIScaleUtils.dip(15),
          lineNumLabel.getPreferredSize().height));
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
    // make the highlight line ui same as the select ui
    isSelected = isSelected || row == mHighlightLine;

    lineNumLabel.setText(String.valueOf(logEntry.getIndex() + 1));

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
      textView.setForeground(new Color(table.getSelectionForeground().getRGB()));
      textView.setBackground(new Color(table.getSelectionBackground().getRGB()));
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
    highlightMatchedText(logEntry.getAppliedFilter(), highlighter, logEntry, isSelected, false);
    highlightMatchedText(logEntry.getSearchFilter(), highlighter, logEntry, isSelected, true);
  }

  private void highlightMatchedText(Filter filter, Highlighter highlighter, LogEntry logEntry, boolean isSelected, boolean isForSearch) {
    String hlText = filter != null ? filter.getPatternString() : null;
    if (!StringUtils.isEmpty(hlText)) {
      try {
        int flags = filter.isCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE;
        Pattern pattern = Pattern.compile(hlText, flags);
        Matcher matcher = pattern.matcher(logEntry.getLogText());
        while (matcher.find()) {
          int start = matcher.start();
          int end = matcher.end();
          highlighter.addHighlight(start, end, new DefaultHighlighter.DefaultHighlightPainter(
              getColorForHighlightedText(isSelected, isForSearch)));
        }
      } catch (Exception e) {
        // Should not happen
        Logger.error("Failed to highlight log entry", e);
        highlighter.removeAllHighlights();
      }
    }
  }

  private Color getColorForHighlightedText(boolean isSelected, boolean isForSearch) {
    if (themeManager.isDark()) {
      return isSelected ? new Color(111, 43, 0) : isForSearch ? new Color(75,110,175) : new Color(83, 87, 10);
    } else {
      return isSelected ? new Color(234, 115, 0) : isForSearch ? new Color(38,117,191) : new Color(250, 255, 162);
    }
  }

  private Color getColorForLogLevel(LogLevel level) {
    Color logColor = Color.LIGHT_GRAY;
    switch (level) {
      case VERBOSE:
        logColor = new Color(185, 189, 186);
        break;
      case DEBUG:
        logColor = new Color(90, 153, 196);
        break;
      case INFO:
        logColor = new Color(2, 142, 2);
        break;
      case WARNING:
        logColor = new Color(214, 188, 76);
        break;
      case ERROR:
        logColor = new Color(156, 31, 2);
        break;
    }

    return logColor;
  }
}
