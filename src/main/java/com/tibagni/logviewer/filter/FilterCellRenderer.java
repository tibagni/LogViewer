package com.tibagni.logviewer.filter;

import com.tibagni.logviewer.ServiceLocator;
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
    String text =  filter.getName();

    int hlStart = -1;
    if (!StringUtils.isEmpty(highlightedText)) {
      hlStart = text.toUpperCase().indexOf(highlightedText.toUpperCase());
    }

    // If we have a matching search, highlight the text, otherwise just escape
    if (hlStart >= 0) {
      int hlEnd = hlStart + highlightedText.length();
      text = StringUtils.htmlHighlightAndEscape(text, hlStart, hlEnd);
    } else {
      text = StringUtils.htmlEscape(text);
    }

    // Add a marker indicating the verbosity of the filter
    String verbosityMarkColor = ServiceLocator.INSTANCE.getThemeManager().isDark() ? "#FFF" : "#000";
    text = "<small color=" + verbosityMarkColor + ">" + filter.getVerbosity().toString().charAt(0) + "</small> " + text;

    Filter.ContextInfo tempInfo = filter.getTemporaryInfo();
    if (tempInfo != null) {
      int totalLinesFound = tempInfo.getTotalLinesFound();
      text += String.format(" {%d}", totalLinesFound);
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
    text = StringUtils.wrapHtml(text);

    // Underline filters that have a different name, so it is obvious just by looking at the list
    Font font = list.getFont();
    if (!filter.nameIsPattern()) {
      Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
      attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
      font = font.deriveFont(attributes);
    }

    setText(text);
    setEnabled(list.isEnabled());
    setFont(font);
    setToolTipText(StringUtils.wrapHtml(filter.getPatternString() + "<br>" + filter.getVerbosity()));

    return this;
  }

  public void setHighlightedText(String hlText) {
    highlightedText = hlText;
  }
}