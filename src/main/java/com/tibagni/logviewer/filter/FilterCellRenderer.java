package com.tibagni.logviewer.filter;

import com.tibagni.logviewer.util.StringUtils;
import com.tibagni.logviewer.util.SwingUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.Serializable;

public class FilterCellRenderer extends JCheckBox implements ListCellRenderer<Object>, Serializable {

  private static final Border DEFAULT_NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);

  public FilterCellRenderer() {
    super();
    setOpaque(true);
    setBorder(DEFAULT_NO_FOCUS_BORDER);
    setName("List.cellRenderer");
  }

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
    }
    setComponentOrientation(list.getComponentOrientation());
    setSelected(filter.isApplied());

    JList.DropLocation dropLocation = list.getDropLocation();
    if (dropLocation != null
        && !dropLocation.isInsert()
        && dropLocation.getIndex() == index) {

      isSelected = true;
    }

    if (isSelected) {
      // Since our filters have their own colors as foreground, add a little alpha to selection background
      // so filter text is still easy to visualize
      Color selectedBgColor = SwingUtils.changeColorAlpha(list.getSelectionBackground(), 25);
      setBackground(selectedBgColor);
    } else {
      setBackground(list.getBackground());
    }
    setForeground(filter.getColor());
    setText(text);

    setEnabled(list.isEnabled());
    setFont(list.getFont());

    return this;
  }
}