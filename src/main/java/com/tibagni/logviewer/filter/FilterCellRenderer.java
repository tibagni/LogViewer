package com.tibagni.logviewer.filter;

import javax.swing.*;
import java.awt.*;

public class FilterCellRenderer extends JTextField implements ListCellRenderer<Filter> {
  @Override
  public Component getListCellRendererComponent(JList<? extends Filter> list, Filter value, int index,
                                                boolean isSelected, boolean cellHasFocus) {

    setText(value.getName());
    setForeground(isSelected ? list.getBackground() : value.getColor());
    setBackground(isSelected ? value.getColor() : list.getBackground());

    return this;
  }
}
