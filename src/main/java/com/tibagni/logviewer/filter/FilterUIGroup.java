package com.tibagni.logviewer.filter;

import com.tibagni.logviewer.ServiceLocator;
import com.tibagni.logviewer.i18n.I18n;
import com.tibagni.logviewer.preferences.LogViewerPreferences;
import com.tibagni.logviewer.shortcuts.ShortcutAction;
import com.tibagni.logviewer.shortcuts.ShortcutManager;
import com.tibagni.logviewer.util.StringUtils;
import com.tibagni.logviewer.util.scaling.UIScaleUtils;
import com.tibagni.logviewer.view.CheckBoxList;
import com.tibagni.logviewer.view.FlatButton;
import com.tibagni.logviewer.view.ReorderableCheckBoxList;
import com.tibagni.logviewer.view.TriStateCheckbox;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * UI component representing a single filter group card in {@link FiltersList}.
 */
class FilterUIGroup extends JPanel {
  private final String SHOW;
  private final String HIDE;

  private final String groupName;
  private final JButton hideGroupBtn;
  private final JButton prevBtn;
  private final JButton nextBtn;
  private final JButton addBtn;
  private final JButton saveBtn;
  private final JButton closeBtn;
  private final TriStateCheckbox selectAllCb;
  private final ReorderableCheckBoxList<Filter> list;
  private final FilterCellRenderer cellRenderer;
  private final FilterListSearchHandler listSearchHandler;

  private final Supplier<FiltersList.FiltersListener> listenerSupplier;
  private final Consumer<String> focusGainedCallback;
  private final LogViewerPreferences userPrefs = ServiceLocator.INSTANCE.getLogViewerPrefs();
  private final ShortcutManager shortcutManager = ServiceLocator.INSTANCE.getShortcutManager();

  public FilterUIGroup(
      String groupName,
      Filter[] filters,
      Supplier<FilterContextTag> contextTagSupplier,
      Supplier<FiltersList.FiltersListener> listenerSupplier,
      Consumer<String> focusGainedCallback) {
    this.groupName = groupName;
    this.listenerSupplier = listenerSupplier;
    this.focusGainedCallback = focusGainedCallback;

    SHOW = StringUtils.RIGHT_ARROW_HEAD + " " + groupName;
    HIDE = StringUtils.DOWN_ARROW_HEAD + " " + groupName;

    setLayout(new BorderLayout());

    cellRenderer = new FilterCellRenderer(contextTagSupplier);
    hideGroupBtn = new FlatButton(HIDE);

    prevBtn = new FlatButton(StringUtils.LEFT_BLACK_POINTER);
    nextBtn = new FlatButton(StringUtils.RIGHT_BLACK_POINTER);
    updateNavigationTooltips();

    addBtn = new FlatButton(StringUtils.PLUS);
    addBtn.setToolTipText(I18n.get(I18n.FILTERS_TOOLTIP_ADD));

    saveBtn = new FlatButton(I18n.get(I18n.COMMON_SAVE));
    saveBtn.setToolTipText(I18n.get(I18n.FILTERS_TOOLTIP_SAVE));
    saveBtn.setVisible(false);

    closeBtn = new FlatButton(StringUtils.DELETE);
    closeBtn.setToolTipText(I18n.get(I18n.FILTERS_TOOLTIP_CLOSE_GROUP));

    selectAllCb = new TriStateCheckbox();
    selectAllCb.setToolTipText(I18n.get(I18n.FILTERS_TOOLTIP_SELECT_ALL));

    list = new ReorderableCheckBoxList<>();
    list.setCellRenderer(cellRenderer);

    JPanel optionsPane = new JPanel(new BorderLayout());
    optionsPane.add(hideGroupBtn, BorderLayout.WEST);
    JPanel groupActionsPane = new JPanel(new FlowLayout(FlowLayout.TRAILING));
    groupActionsPane.add(selectAllCb);
    groupActionsPane.add(saveBtn);
    groupActionsPane.add(addBtn);
    groupActionsPane.add(prevBtn);
    groupActionsPane.add(nextBtn);
    groupActionsPane.add(closeBtn);
    optionsPane.add(groupActionsPane, BorderLayout.EAST);
    add(optionsPane, BorderLayout.NORTH);
    add(list);

    hideGroupBtn.addActionListener(l -> toggleGroupVisibility());

    listSearchHandler = new FilterListSearchHandler(list, cellRenderer);

    setListData(filters);
    configureContextActions();
    updateActionPaneButtons();
  }

  public String getGroupName() {
    return groupName;
  }

  public ReorderableCheckBoxList<Filter> getList() {
    return list;
  }

  public TriStateCheckbox getSelectAllCb() {
    return selectAllCb;
  }

  public void cancelSearch() {
    listSearchHandler.cancelSearch();
  }

  public void setListData(Filter[] filters) {
    list.setListData(filters);
    updatePreferredSize();
  }

  public void toggleGroupVisibility() {
    boolean newVisibility = !list.isVisible();
    hideGroupBtn.setText(newVisibility ? HIDE : SHOW);
    list.setVisible(newVisibility);

    updatePreferredSize();

    FiltersList.FiltersListener listener = listenerSupplier.get();
    if (listener != null) {
      listener.onGroupVisibilityChanged(groupName);
    }
  }

  void forceGroupVisibilitySilently(boolean visible) {
    if (visible == list.isVisible()) return;
    hideGroupBtn.setText(visible ? HIDE : SHOW);
    list.setVisible(visible);

    updatePreferredSize();
  }

  boolean isGroupVisible() {
    return list.isVisible();
  }

  private void updatePreferredSize() {
    Dimension maximumSize = getMaximumSize();
    Dimension preferredSize = getPreferredSize();
    setMaximumSize(new Dimension(maximumSize.width, preferredSize.height));
  }

  private void configureContextActions() {
    JPopupMenu popup = new JPopupMenu();
    final JLabel menuTitle = new JLabel();
    menuTitle.setBorder(new EmptyBorder(0, UIScaleUtils.dip(10), 0, 0));
    popup.add(menuTitle);
    popup.add(new JPopupMenu.Separator());
    JMenuItem deleteMenuItem = popup.add(I18n.get(I18n.COMMON_DELETE));
    JMenuItem editMenuItem = popup.add(I18n.get(I18n.COMMON_EDIT));
    JMenuItem duplicateMenuItem = popup.add(I18n.get(I18n.FILTERS_MENU_DUPLICATE));
    JMenuItem moveMenuItem = popup.add(I18n.get(I18n.FILTERS_MENU_MOVE));

    deleteMenuItem.addActionListener(e -> deleteSelectedFilters());
    editMenuItem.addActionListener(e -> editSelectedFilter());
    duplicateMenuItem.addActionListener(e -> duplicateSelectedFilter());
    moveMenuItem.addActionListener(e -> moveSelectedFilters());

    list.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent me) {
        int indexClicked = list.locationToIndex(me.getPoint());

        if (SwingUtilities.isRightMouseButton(me) && !list.isSelectionEmpty()) {
          int[] selectedIndices = list.getSelectedIndices();
          if (IntStream.of(selectedIndices).anyMatch(i -> i == indexClicked)) {
            int selectedFilters = selectedIndices.length;
            menuTitle.setText(I18n.format(I18n.FILTERS_MENU_ITEMS_SELECTED, selectedFilters));
            editMenuItem.setVisible(selectedFilters == 1);
            duplicateMenuItem.setVisible(selectedFilters == 1);

            popup.show(list, me.getX(), me.getY());
          }
        } else if (me.getClickCount() == 2) {
          // Edit filter on double click
          editSelectedFilter();
        }
      }
    });

    list.addKeyListener(new KeyAdapter() {
      @Override
      public void keyTyped(KeyEvent e) {
        listSearchHandler.handleKeyTyped(e.getKeyChar());
      }
    });

    list.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        if (focusGainedCallback != null) {
          focusGainedCallback.accept(groupName);
        }
      }

      @Override
      public void focusLost(FocusEvent e) {
        listSearchHandler.cancelSearch();
      }
    });

    // Add the shortcuts for the context menu
    list.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
          if (listSearchHandler.isSearching()) {
            listSearchHandler.cancelSearch();
          } else {
            list.clearSelection();
          }
          return;
        }

        if (listSearchHandler.isSearching()) {
          return;
        }

        boolean backspace = e.getKeyCode() == KeyEvent.VK_BACK_SPACE;

        if (!list.isSelectionEmpty() && list.getModel().getSize() > 0 &&
            (shortcutManager.matches(e, ShortcutAction.FILTERS_DELETE_SELECTED) || backspace)) {
          deleteSelectedFilters();
        } else if (shortcutManager.matches(e, ShortcutAction.FILTERS_EDIT_SELECTED) &&
            list.getSelectedIndices().length == 1) {
          editSelectedFilter();
        } else if (shortcutManager.matches(e, ShortcutAction.FILTERS_PREV_MATCH)) {
          navigatePrev();
        } else if (shortcutManager.matches(e, ShortcutAction.FILTERS_NEXT_MATCH)) {
          navigateNext();
        } else if (shortcutManager.matches(e, ShortcutAction.FILTERS_INLINE_SEARCH) &&
            shortcutManager.getUnmodifiedTriggerChar(ShortcutAction.FILTERS_INLINE_SEARCH) == null) {
          listSearchHandler.startSearch();
          e.consume();
        }
      }
    });

    prevBtn.addActionListener(l -> navigatePrev());
    nextBtn.addActionListener(l -> navigateNext());
    addBtn.addActionListener(l -> {
      FiltersList.FiltersListener listener = listenerSupplier.get();
      if (listener != null) {
        listener.onAddFilter(groupName);
      }
    });
    saveBtn.addActionListener(l -> {
      FiltersList.FiltersListener listener = listenerSupplier.get();
      if (listener != null) {
        listener.onSaveFilters(groupName);
      }
    });
    closeBtn.addActionListener(l -> {
      FiltersList.FiltersListener listener = listenerSupplier.get();
      if (listener != null) {
        listener.onCloseGroup(groupName);
      }
    });
    selectAllCb.addSelectionChangedListener(newSelectionState -> {
      FiltersList.FiltersListener listener = listenerSupplier.get();
      if (listener != null) {
        Boolean applied = null;
        if (newSelectionState == TriStateCheckbox.SelectionState.SELECTED) {
          applied = true;
        } else if (newSelectionState == TriStateCheckbox.SelectionState.NOT_SELECTED) {
          applied = false;
        }

        if (applied != null) {
          ListModel<Filter> listModel = list.getModel();
          for (int i = 0; i < listModel.getSize(); i++) {
            listModel.getElementAt(i).setApplied(applied);
          }
          list.updateUI();
          listener.onFiltersApplied();
        }
      }
    });

    list.addListSelectionListener(e -> updateActionPaneButtons());
    list.setReorderedListener(this::reorderFilters);
    list.setItemsCheckListener((CheckBoxList.ItemsCheckListener<Filter>) elements -> {
      elements.forEach(f -> f.setApplied(!f.isApplied()));
      FiltersList.FiltersListener listener = listenerSupplier.get();
      if (userPrefs.getApplyFilterOnCheck() && listener != null) {
        listener.onFiltersApplied();
      }
      updateActionPaneButtons();
    });
  }

  private void navigatePrev() {
    FiltersList.FiltersListener listener = listenerSupplier.get();
    if (listener != null) {
      Filter selectedFilter = list.getSelectedValue();
      if (selectedFilter != null) {
        listener.onNavigatePrevFilteredLog(selectedFilter);
      }
    }
  }

  private void navigateNext() {
    FiltersList.FiltersListener listener = listenerSupplier.get();
    if (listener != null) {
      Filter selectedFilter = list.getSelectedValue();
      if (selectedFilter != null) {
        listener.onNavigateNextFilteredLog(selectedFilter);
      }
    }
  }

  private void updateNavigationTooltips() {
    String prevShortcut = ShortcutManager.formatKeyStroke(
        shortcutManager.getKeyStroke(ShortcutAction.FILTERS_PREV_MATCH));
    String nextShortcut = ShortcutManager.formatKeyStroke(
        shortcutManager.getKeyStroke(ShortcutAction.FILTERS_NEXT_MATCH));
    prevBtn.setToolTipText(prevShortcut.isEmpty()
        ? I18n.get(I18n.FILTERS_TOOLTIP_PREV)
        : I18n.format(I18n.FILTERS_TOOLTIP_PREV_FORMAT, prevShortcut));
    nextBtn.setToolTipText(nextShortcut.isEmpty()
        ? I18n.get(I18n.FILTERS_TOOLTIP_NEXT)
        : I18n.format(I18n.FILTERS_TOOLTIP_NEXT_FORMAT, nextShortcut));
  }

  public void updateActionPaneButtons() {
    if (list == null) return;
    updateNavigationTooltips();

    Filter selectedFilter = list.getSelectedValue();
    boolean enableNavigation = list.getSelectedIndices().length == 1 &&
        selectedFilter != null &&
        selectedFilter.isApplied();
    prevBtn.setEnabled(enableNavigation);
    nextBtn.setEnabled(enableNavigation);

    boolean hasAtLeastOneFilterApplied = false;
    ListModel<Filter> listModel = list.getModel();
    boolean hasItems = listModel.getSize() > 0;
    boolean allFiltersApplied = hasItems;
    for (int i = 0; i < listModel.getSize(); i++) {
      Filter filter = listModel.getElementAt(i);
      hasAtLeastOneFilterApplied |= filter.isApplied();
      allFiltersApplied &= filter.isApplied();
    }

    TriStateCheckbox.SelectionState selectionState = TriStateCheckbox.SelectionState.NOT_SELECTED;
    if (allFiltersApplied) {
      selectionState = TriStateCheckbox.SelectionState.SELECTED;
    } else if (hasAtLeastOneFilterApplied) {
      selectionState = TriStateCheckbox.SelectionState.PARTIALLY_SELECTED;
    }
    selectAllCb.setSelectionState(selectionState);
    selectAllCb.updateUI();
  }

  private void deleteSelectedFilters() {
    FiltersList.FiltersListener listener = listenerSupplier.get();
    if (listener != null) {
      listener.onDeleteFilters(groupName, list.getSelectedIndices());
    }
  }

  private void editSelectedFilter() {
    FiltersList.FiltersListener listener = listenerSupplier.get();
    if (listener != null) {
      Filter filter = list.getSelectedValue();
      listener.onEditFilter(filter);
    }
  }

  private void duplicateSelectedFilter() {
    FiltersList.FiltersListener listener = listenerSupplier.get();
    if (listener != null) {
      Filter filter = list.getSelectedValue();
      listener.onDuplicateFilter(groupName, filter);
    }
  }

  private void moveSelectedFilters() {
    FiltersList.FiltersListener listener = listenerSupplier.get();
    if (listener != null) {
      listener.onMoveFilters(groupName, list.getSelectedIndices());
    }
  }

  private void reorderFilters(int orig, int dest) {
    FiltersList.FiltersListener listener = listenerSupplier.get();
    if (listener != null) {
      listener.onReordered(groupName, orig, dest);
    }
  }

  @Override
  public void updateUI() {
    super.updateUI();
    if (list != null) {
      list.updateUI();
      list.revalidate();
      list.repaint();
    }
    if (selectAllCb != null) {
      selectAllCb.updateUI();
    }
  }

  public void showUnsavedIndication(boolean unsaved) {
    saveBtn.setVisible(unsaved);
  }
}
