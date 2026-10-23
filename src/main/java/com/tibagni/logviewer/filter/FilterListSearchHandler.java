package com.tibagni.logviewer.filter;

import com.tibagni.logviewer.ServiceLocator;
import com.tibagni.logviewer.shortcuts.ShortcutAction;
import com.tibagni.logviewer.theme.LogViewerThemeManager;
import com.tibagni.logviewer.util.LogFontManager;
import com.tibagni.logviewer.util.scaling.UIScaleUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Handles inline keyboard search within a filter {@link JList} component, displaying a popup hint.
 */
class FilterListSearchHandler {
  private final JList<?> targetList;
  private final FilterCellRenderer filterCellRenderer;
  private final LogViewerThemeManager themeManager = ServiceLocator.INSTANCE.getThemeManager();

  private JLabel searchHint;
  private Popup hintPopup;

  private boolean isSearching;
  private final StringBuilder searchString = new StringBuilder();

  public FilterListSearchHandler(JList<?> targetList, FilterCellRenderer filterCellRenderer) {
    this.targetList = targetList;
    this.filterCellRenderer = filterCellRenderer;
  }

  public boolean isSearching() {
    return isSearching;
  }

  public void startSearch() {
    if (!isSearching) {
      initSearch();
    }
  }

  public void handleKeyTyped(char charTyped) {
    if (!isSearching) {
      Character triggerChar = ServiceLocator.INSTANCE.getShortcutManager()
          .getUnmodifiedTriggerChar(ShortcutAction.FILTERS_INLINE_SEARCH);
      if (triggerChar != null && Character.toLowerCase(charTyped) == Character.toLowerCase(triggerChar)) {
        initSearch();
      }
      return;
    }

    if (charTyped == KeyEvent.VK_ESCAPE) {
      cancelSearch();
      return;
    }

    if (charTyped == KeyEvent.VK_BACK_SPACE) {
      if (searchString.length() > 0) {
        searchString.deleteCharAt(searchString.length() - 1);
      }
    } else {
      searchString.append(charTyped);
    }

    filterCellRenderer.setHighlightedText(searchString.toString());
    updateSearchHint();
  }

  public void cancelSearch() {
    filterCellRenderer.setHighlightedText(null);
    clearSearch();
  }

  private void initSearch() {
    isSearching = true;
    updateSearchHint();
  }

  private void clearSearch() {
    searchString.setLength(0);
    isSearching = false;
    updateSearchHint();
  }

  private void updateSearchHint() {
    destroyHintPopup();
    if (isSearching) {
      initHintPopup();
      if (hintPopup != null) {
        hintPopup.show();
      }
    }

    targetList.invalidate();
    targetList.repaint();
  }

  private void initHintPopup() {
    JRootPane rootPane = targetList.getRootPane();
    if (rootPane == null || !rootPane.isShowing()) {
      return;
    }

    searchHint = new JLabel("/" + searchString); // i18n:ignore
    searchHint.setBorder(BorderFactory.createLineBorder(Color.GRAY));
    searchHint.setOpaque(true);
    searchHint.setBackground(themeManager.isDark() ? Color.BLACK : Color.LIGHT_GRAY);
    searchHint.setFont(LogFontManager.getFont(
        ServiceLocator.INSTANCE.getLogViewerPrefs().getLogFontFamily(),
        Font.PLAIN, UIScaleUtils.scaleFont(24)));
    Point targetListLocation = rootPane.getLocationOnScreen();
    int popupX = targetListLocation.x + UIScaleUtils.dip(50);
    int popupY = targetListLocation.y + UIScaleUtils.dip(50);
    hintPopup = PopupFactory.getSharedInstance().getPopup(targetList, searchHint, popupX, popupY);
  }

  private void destroyHintPopup() {
    if (hintPopup != null) {
      hintPopup.hide();
      hintPopup = null;
      searchHint = null;
    }
  }
}
