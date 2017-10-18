package com.tibagni.logviewer.filter;

import javax.swing.*;
import java.awt.*;

public class FilterCellRenderer extends JTextField implements ListCellRenderer<Filter> {
  @Override
  public Component getListCellRendererComponent(JList<? extends Filter> list, Filter value, int index,
                                                boolean isSelected, boolean cellHasFocus) {

    String text = value.getName();
    Filter.ContextInfo tempInfo = value.getTemporaryInfo();
    if (tempInfo != null) {
      text += String.format(" (%d)", tempInfo.linesFound);
    }

    setText(text);
    setForeground(isSelected ? list.getBackground() : value.getColor());
    setBackground(isSelected ? value.getColor() : list.getBackground());

    return this;
  }
}
