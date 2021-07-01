package com.tibagni.logviewer.filter;

import com.tibagni.logviewer.util.StringUtils;
import com.tibagni.logviewer.util.SwingUtils;
import com.tibagni.logviewer.util.scaling.UIScaleUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class FilterCellRenderer extends JCheckBox implements ListCellRenderer<Object>, Serializable {

  private static final Border DEFAULT_NO_FOCUS_BORDER = new EmptyBorder(UIScaleUtils.dip(1),
          UIScaleUtils.dip(1),
          UIScaleUtils.dip(1),
          UIScaleUtils.dip(1));
  private String highlightedText;

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

    boolean useHtml = false;
    if (!StringUtils.isEmpty(highlightedText)) {
      int hlStart = text.toUpperCase().indexOf(highlightedText.toUpperCase());
      if (hlStart >= 0) {
        int hlEnd = hlStart + highlightedText.length();
        text = StringUtils.htmlHighlightAndEscape(text, hlStart, hlEnd);
        useHtml = true;
      }
    }

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

    if (useHtml) {
      text = StringUtils.wrapHtml(text);
    }

    // Underline filters that have a different name so it is obvious just by looking at the list
    Font font = list.getFont();
    if (!filter.nameIsPattern()) {
      Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
      attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
      font = font.deriveFont(attributes);
    }

    setText(text);
    setEnabled(list.isEnabled());
    setFont(font);
    setToolTipText("search {" + filter.getPatternString() + "}");

    return this;
  }

  public void setHighlightedText(String hlText) {
    highlightedText = hlText;
  }
}