package com.tibagni.logviewer.filter;

import com.tibagni.logviewer.util.StringUtils;

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
      text += String.format(" {%d}", tempInfo.linesFound);

      // Show the navigation shortcuts
      if (tempInfo.linesFound > 0) {
        text += String.format(" %s %s(,) / %s(.)", StringUtils.VERTICAL_SEPARATOR,
            StringUtils.LEFT_ARROW, StringUtils.RIGHT_ARROW);
      }
    }

    Component renderComponent = super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
    renderComponent.setForeground(filter.getColor());
    return renderComponent;
  }
}
