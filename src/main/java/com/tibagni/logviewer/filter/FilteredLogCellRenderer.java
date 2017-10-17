package com.tibagni.logviewer.filter;

import com.tibagni.logviewer.LogCellRenderer;
import com.tibagni.logviewer.log.LogEntry;

import javax.swing.*;
import java.awt.*;

public class FilteredLogCellRenderer extends LogCellRenderer {
  @Override
  public Component getListCellRendererComponent(JList<? extends LogEntry> list, LogEntry value, int index,
                                                boolean isSelected, boolean cellHasFocus) {
    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

    if (!isSelected) {
      textView.setForeground(value.getFilterColor());
    }

    return this;
  }
}
