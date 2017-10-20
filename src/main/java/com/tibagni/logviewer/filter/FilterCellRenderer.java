package com.tibagni.logviewer.filter;

import javax.swing.*;
import java.awt.*;

public class FilterCellRenderer extends DefaultListCellRenderer {
  @Override
  public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                boolean cellHasFocus) {
    Filter filter = (Filter) value;
    String text = filter.getName();
    Filter.ContextInfo tempInfo = filter.getTemporaryInfo();
    if (tempInfo != null) {
      text += String.format(" (%d)", tempInfo.linesFound);
    }
    setText(text);

    Component renderComponent = super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
    renderComponent.setForeground(filter.getColor());
    return renderComponent;
  }
}
