package com.tibagni.logviewer.filter;

import com.tibagni.logviewer.util.StringUtils;
import com.tibagni.logviewer.util.SwingUtils;

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
      int totalLinesFound = tempInfo.getTotalLinesFound();
      text += String.format(" {%d}", totalLinesFound);

      // Show the navigation shortcuts
      if (totalLinesFound > 0) {
        text += String.format(" %s %s(,) / %s(.)", StringUtils.VERTICAL_SEPARATOR,
            StringUtils.LEFT_ARROW, StringUtils.RIGHT_ARROW);
      }

      text = String.format("%s %s", StringUtils.CHECK_SYMBOL, text);
    }

    Component renderComponent = super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
    renderComponent.setForeground(filter.getColor());

    // Since our filters have their own colors as foreground, add a little alpha to selection background
    // so filter text is still easy to visualize
    if (isSelected) {
      Color selectedBgColor = SwingUtils.changeColorAlpha(list.getSelectionBackground(), 60);
      renderComponent.setBackground(selectedBgColor);
    }

    return renderComponent;
  }
}