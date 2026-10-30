package com.tibagni.logviewer.filter;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.tibagni.logviewer.ServiceLocator;
import com.tibagni.logviewer.i18n.I18n;
import com.tibagni.logviewer.preferences.LogViewerPreferences;
import com.tibagni.logviewer.shortcuts.ShortcutAction;
import com.tibagni.logviewer.shortcuts.ShortcutManager;
import com.tibagni.logviewer.util.scaling.UIScaleUtils;
import com.tibagni.logviewer.view.CardBorderUtils;
import com.tibagni.logviewer.view.CheckBoxList;
import com.tibagni.logviewer.view.CommonIcons;
import com.tibagni.logviewer.view.FlatButton;
import com.tibagni.logviewer.view.ReorderableCheckBoxList;
import com.tibagni.logviewer.view.TriStateCheckbox;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * UI component representing a single filter group card in {@link FiltersList}.
 */
class FilterUIGroup extends JPanel {
  static final int MARGIN_VERTICAL_DP = 3;
  static final int MARGIN_HORIZONTAL_DP = 4;
  private static final int DEFAULT_CARD_ARC_DP = 8;

  private final String groupName;
  private final JButton hideGroupBtn = createToolbarComponent(new FlatButton());
  private final JButton prevBtn = createToolbarComponent(new FlatButton());
  private final JButton nextBtn = createToolbarComponent(new FlatButton());
  private final JButton addBtn = createToolbarComponent(new FlatButton());
  private final JButton saveBtn = createToolbarComponent(
      new FlatButton(I18n.get(I18n.COMMON_SAVE)));
  private final JButton closeBtn = createToolbarComponent(new FlatButton());
  private final TriStateCheckbox selectAllCb = createToolbarComponent(
      new TriStateCheckbox());
  private final JPanel optionsPane;
  private final ReorderableCheckBoxList<Filter> list;
  private final FilterCellRenderer cellRenderer;
  private final FilterListSearchHandler listSearchHandler;

  private final Supplier<FiltersList.FiltersListener> listenerSupplier;
  private final Consumer<String> focusGainedCallback;
  private final LogViewerPreferences userPrefs =
      ServiceLocator.INSTANCE.getLogViewerPrefs();
  private final ShortcutManager shortcutManager =
      ServiceLocator.INSTANCE.getShortcutManager();

  /**
   * Helper utility that assigns toolbar button FlatLaf client properties to a component.
   */
  private static <T extends JComponent> T createToolbarComponent(T component) {
    component.putClientProperty(
        FlatClientProperties.BUTTON_TYPE,
        FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    return component;
  }

  /**
   * Constructs a FilterUIGroup representing a filter category card.
   *
   * @param groupName name of this filter group.
   * @param filters initial array of filters belonging to this group.
   * @param contextTagSupplier supplier of the active context tag.
   * @param listenerSupplier supplier of the active {@link FiltersList.FiltersListener}.
   * @param focusGainedCallback callback invoked when list gains focus.
   */
  public FilterUIGroup(
      String groupName,
      Filter[] filters,
      Supplier<FilterContextTag> contextTagSupplier,
      Supplier<FiltersList.FiltersListener> listenerSupplier,
      Consumer<String> focusGainedCallback) {
    this.groupName = groupName;
    this.listenerSupplier = listenerSupplier;
    this.focusGainedCallback = focusGainedCallback;

    setLayout(new BorderLayout());
    setOpaque(false);

    cellRenderer = new FilterCellRenderer(contextTagSupplier);
    list = new ReorderableCheckBoxList<>();
    list.setOpaque(false);
    list.setCellRenderer(cellRenderer);

    optionsPane = buildGroupHeaderToolbar();

    add(optionsPane, BorderLayout.NORTH);
    add(list, BorderLayout.CENTER);

    updateCardMargin();
    updateHeaderStyle();

    listSearchHandler = new FilterListSearchHandler(list, cellRenderer);

    setListData(filters);
    configureContextActions();
    updateActionPaneButtons();
  }

  /**
   * Builds the group header toolbar containing the group toggle button
   * and action controls (select all, save, add, navigation, close).
   *
   * @return the configured toolbar {@link JPanel}.
   */
  private JPanel buildGroupHeaderToolbar() {
    prevBtn.setIcon(CommonIcons.chevronLeftIcon(14));
    nextBtn.setIcon(CommonIcons.chevronRightIcon(14));
    updateNavigationTooltips();

    addBtn.setIcon(CommonIcons.plusIcon(14));
    addBtn.setToolTipText(I18n.get(I18n.FILTERS_TOOLTIP_ADD));

    saveBtn.setToolTipText(I18n.get(I18n.FILTERS_TOOLTIP_SAVE));
    saveBtn.setVisible(false);

    closeBtn.setIcon(CommonIcons.closeIcon(14));
    closeBtn.setToolTipText(I18n.get(I18n.FILTERS_TOOLTIP_CLOSE_GROUP));

    selectAllCb.setToolTipText(I18n.get(I18n.FILTERS_TOOLTIP_SELECT_ALL));
    selectAllCb.setOpaque(false);

    configureToolbarButtonListeners();

    JPanel groupActionsPane = new JPanel(
        new FlowLayout(FlowLayout.TRAILING, UIScaleUtils.dip(2), 0));
    groupActionsPane.setOpaque(false);
    groupActionsPane.add(selectAllCb);
    groupActionsPane.add(saveBtn);
    groupActionsPane.add(addBtn);
    groupActionsPane.add(prevBtn);
    groupActionsPane.add(nextBtn);
    groupActionsPane.add(closeBtn);

    JPanel headerToolbar = new JPanel(new BorderLayout());
    headerToolbar.setOpaque(false);
    headerToolbar.add(hideGroupBtn, BorderLayout.WEST);
    headerToolbar.add(groupActionsPane, BorderLayout.EAST);
    return headerToolbar;
  }

  /**
   * Configures action listeners for header toolbar buttons and select-all checkbox.
   */
  private void configureToolbarButtonListeners() {
    hideGroupBtn.addActionListener(l -> toggleGroupVisibility());
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
    selectAllCb.addSelectionChangedListener(this::handleSelectAllChanged);
  }

  /**
   * Handles selection state changes on the select-all checkbox, applying the state
   * across all filters in this group.
   *
   * @param newSelectionState the new selection state from the checkbox.
   */
  private void handleSelectAllChanged(TriStateCheckbox.SelectionState newSelectionState) {
    FiltersList.FiltersListener listener = listenerSupplier.get();
    if (listener == null) return;

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
      updateGroupHeader();
    }
  }

  /**
   * Returns the name of this filter group.
   *
   * @return group name string.
   */
  public String getGroupName() {
    return groupName;
  }

  /**
   * Returns the list component containing the group's filters.
   *
   * @return the reorderable check box list of filters.
   */
  public ReorderableCheckBoxList<Filter> getList() {
    return list;
  }

  /**
   * Returns the tri-state checkbox used to select or deselect all filters.
   *
   * @return the tri-state select all checkbox.
   */
  public TriStateCheckbox getSelectAllCb() {
    return selectAllCb;
  }

  /**
   * Returns the header options panel for this filter group.
   *
   * @return the options pane component.
   */
  public JPanel getOptionsPane() {
    return optionsPane;
  }

  /**
   * Returns the toggle button used to expand or collapse this filter group.
   *
   * @return the hide group toggle button.
   */
  public JButton getHideGroupBtn() {
    return hideGroupBtn;
  }

  /**
   * Returns the navigate to previous match button.
   *
   * @return the previous navigation button.
   */
  public JButton getPrevBtn() {
    return prevBtn;
  }

  /**
   * Returns the navigate to next match button.
   *
   * @return the next navigation button.
   */
  public JButton getNextBtn() {
    return nextBtn;
  }

  /**
   * Returns the add filter button.
   *
   * @return the add filter button.
   */
  public JButton getAddBtn() {
    return addBtn;
  }

  /**
   * Returns the save filters button.
   *
   * @return the save filters button.
   */
  public JButton getSaveBtn() {
    return saveBtn;
  }

  /**
   * Returns the close group button.
   *
   * @return the close group button.
   */
  public JButton getCloseBtn() {
    return closeBtn;
  }

  /**
   * Cancels active inline filter searching on this group's list.
   */
  public void cancelSearch() {
    listSearchHandler.cancelSearch();
  }

  /**
   * Updates the filter list entries and refreshes the header count badge.
   *
   * @param filters the new array of filters to display.
   */
  public void setListData(Filter[] filters) {
    list.setListData(filters);
    updatePreferredSize();
    updateGroupHeader();
  }

  /**
   * Toggles the filter list visibility, updating header chevron and
   * notifying listeners.
   */
  public void toggleGroupVisibility() {
    boolean newVisibility = !list.isVisible();
    list.setVisible(newVisibility);
    updateGroupHeader();
    updatePreferredSize();

    FiltersList.FiltersListener listener = listenerSupplier.get();
    if (listener != null) {
      listener.onGroupVisibilityChanged(groupName);
    }
  }

  /**
   * Silently sets group visibility without notifying listener callbacks.
   *
   * @param visible true to show filter list, false to hide.
   */
  void forceGroupVisibilitySilently(boolean visible) {
    if (visible == list.isVisible()) return;
    list.setVisible(visible);
    updateGroupHeader();
    updatePreferredSize();
  }

  /**
   * Returns whether the filter group's list is currently visible.
   *
   * @return true if visible, false otherwise.
   */
  boolean isGroupVisible() {
    return list.isVisible();
  }

  /**
   * Recalculates maximum size based on preferred height to prevent unbounded vertical stretch.
   */
  private void updatePreferredSize() {
    Dimension maximumSize = getMaximumSize();
    Dimension preferredSize = getPreferredSize();
    setMaximumSize(new Dimension(maximumSize.width, preferredSize.height));
  }

  /**
   * Builds the context popup menu for the selected filter items.
   *
   * @param listener the active filters listener callback.
   * @return the configured {@link JPopupMenu} with actions for delete, edit, duplicate, and move.
   */
  private JPopupMenu buildContextMenu(FiltersList.FiltersListener listener) {
    int[] selectedIndices = list.getSelectedIndices();
    int selectedFilters = selectedIndices.length;

    JPopupMenu popup = new JPopupMenu();
    JLabel menuTitle = new JLabel(
        I18n.format(I18n.FILTERS_MENU_ITEMS_SELECTED, selectedFilters));
    menuTitle.setBorder(new EmptyBorder(0, UIScaleUtils.dip(10), 0, 0));
    popup.add(menuTitle);
    popup.add(new JPopupMenu.Separator());

    JMenuItem deleteMenuItem = popup.add(I18n.get(I18n.COMMON_DELETE));
    deleteMenuItem.addActionListener(e -> deleteSelectedFilters(listener));

    JMenuItem editMenuItem = popup.add(I18n.get(I18n.COMMON_EDIT));
    editMenuItem.setVisible(selectedFilters == 1);
    editMenuItem.addActionListener(e -> editSelectedFilter(listener));

    JMenuItem duplicateMenuItem = popup.add(I18n.get(I18n.FILTERS_MENU_DUPLICATE));
    duplicateMenuItem.setVisible(selectedFilters == 1);
    duplicateMenuItem.addActionListener(e -> duplicateSelectedFilter(listener));

    JMenuItem moveMenuItem = popup.add(I18n.get(I18n.FILTERS_MENU_MOVE));
    moveMenuItem.addActionListener(e -> moveSelectedFilters(listener));

    return popup;
  }

  /**
   * Creates a mouse listener that handles context menu display on right click
   * and editing on double click.
   *
   * @return the configured {@link MouseListener}.
   */
  private MouseListener createListMouseListener() {
    return new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent me) {
        int indexClicked = list.locationToIndex(me.getPoint());

        if (SwingUtilities.isRightMouseButton(me) && !list.isSelectionEmpty()) {
          int[] selectedIndices = list.getSelectedIndices();
          if (IntStream.of(selectedIndices).anyMatch(i -> i == indexClicked)) {
            JPopupMenu popup = buildContextMenu(listenerSupplier.get());
            popup.show(list, me.getX(), me.getY());
          }
        } else if (me.getClickCount() == 2) {
          editSelectedFilter();
        }
      }
    };
  }

  /**
   * Creates a focus listener that notifies when the list gains focus and cancels
   * active search when focus is lost.
   *
   * @return the configured {@link FocusListener}.
   */
  private FocusListener createListFocusListener() {
    return new FocusAdapter() {
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
    };
  }

  /**
   * Configures mouse, keyboard, focus, and selection listeners on the filter list.
   */
  private void configureContextActions() {
    list.addMouseListener(createListMouseListener());
    list.addFocusListener(createListFocusListener());
    list.addKeyListener(new KeyAdapter() {
      @Override
      public void keyTyped(KeyEvent e) {
        listSearchHandler.handleKeyTyped(e.getKeyChar());
      }

      @Override
      public void keyPressed(KeyEvent e) {
        handleKeyShortcut(e, listenerSupplier.get());
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
      updateGroupHeader();
    });
  }

  /**
   * Handles keyboard shortcuts for filter actions such as delete, edit, navigation,
   * and inline search.
   *
   * @param e the key event triggered on the list.
   * @param listener the active filters listener callback.
   * @return true if the shortcut was recognized and handled, false otherwise.
   */
  private boolean handleKeyShortcut(KeyEvent e, FiltersList.FiltersListener listener) {
    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
      return handleEscapeKey();
    }

    if (listSearchHandler.isSearching()) {
      return false;
    }

    if (handleDeleteShortcut(e, listener)) {
      return true;
    }
    if (handleEditShortcut(e, listener)) {
      return true;
    }
    if (handleNavigationShortcut(e, listener)) {
      return true;
    }
    return handleSearchShortcut(e);
  }

  /**
   * Handles escape key by cancelling search if active, or clearing list selection.
   *
   * @return true indicating the escape key was processed.
   */
  private boolean handleEscapeKey() {
    if (listSearchHandler.isSearching()) {
      listSearchHandler.cancelSearch();
    } else {
      list.clearSelection();
    }
    return true;
  }

  /**
   * Handles delete shortcut key or backspace to delete selected filters.
   *
   * @param e the key event.
   * @param listener the active filters listener callback.
   * @return true if the delete shortcut was triggered, false otherwise.
   */
  private boolean handleDeleteShortcut(KeyEvent e, FiltersList.FiltersListener listener) {
    boolean backspace = e.getKeyCode() == KeyEvent.VK_BACK_SPACE;
    boolean matchDelete = shortcutManager.matches(e, ShortcutAction.FILTERS_DELETE_SELECTED);
    if ((matchDelete || backspace) && !list.isSelectionEmpty() && list.getModel().getSize() > 0) {
      deleteSelectedFilters(listener);
      return true;
    }
    return false;
  }

  /**
   * Handles edit shortcut key when a single filter is selected.
   *
   * @param e the key event.
   * @param listener the active filters listener callback.
   * @return true if the edit shortcut was triggered, false otherwise.
   */
  private boolean handleEditShortcut(KeyEvent e, FiltersList.FiltersListener listener) {
    if (shortcutManager.matches(e, ShortcutAction.FILTERS_EDIT_SELECTED)
        && list.getSelectedIndices().length == 1) {
      editSelectedFilter(listener);
      return true;
    }
    return false;
  }

  /**
   * Handles previous and next match navigation shortcuts.
   *
   * @param e the key event.
   * @param listener the active filters listener callback.
   * @return true if a navigation shortcut was triggered, false otherwise.
   */
  private boolean handleNavigationShortcut(KeyEvent e, FiltersList.FiltersListener listener) {
    if (shortcutManager.matches(e, ShortcutAction.FILTERS_PREV_MATCH)) {
      navigatePrev(listener);
      return true;
    }
    if (shortcutManager.matches(e, ShortcutAction.FILTERS_NEXT_MATCH)) {
      navigateNext(listener);
      return true;
    }
    return false;
  }

  /**
   * Handles inline search activation shortcut key.
   *
   * @param e the key event.
   * @return true if search shortcut was triggered, false otherwise.
   */
  private boolean handleSearchShortcut(KeyEvent e) {
    if (shortcutManager.matches(e, ShortcutAction.FILTERS_INLINE_SEARCH)
        && shortcutManager.getUnmodifiedTriggerChar(
            ShortcutAction.FILTERS_INLINE_SEARCH) == null) {
      listSearchHandler.startSearch();
      e.consume();
      return true;
    }
    return false;
  }

  /**
   * Navigates to the previous match for the currently selected filter using default listener.
   */
  private void navigatePrev() {
    navigatePrev(listenerSupplier.get());
  }

  /**
   * Navigates to the previous match for the currently selected filter.
   *
   * @param listener the active filters listener callback.
   */
  private void navigatePrev(FiltersList.FiltersListener listener) {
    if (listener != null) {
      Filter selectedFilter = list.getSelectedValue();
      if (selectedFilter != null) {
        listener.onNavigatePrevFilteredLog(selectedFilter);
      }
    }
  }

  /**
   * Navigates to the next match for the currently selected filter using default listener.
   */
  private void navigateNext() {
    navigateNext(listenerSupplier.get());
  }

  /**
   * Navigates to the next match for the currently selected filter.
   *
   * @param listener the active filters listener callback.
   */
  private void navigateNext(FiltersList.FiltersListener listener) {
    if (listener != null) {
      Filter selectedFilter = list.getSelectedValue();
      if (selectedFilter != null) {
        listener.onNavigateNextFilteredLog(selectedFilter);
      }
    }
  }

  /**
   * Updates previous and next navigation button tooltips with their key shortcuts.
   */
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

  /**
   * Updates toolbar button enabled states, navigation tooltips, and select-all
   * checkbox selection state.
   */
  public void updateActionPaneButtons() {
    if (list == null) return;
    updateNavigationTooltips();

    Filter selectedFilter = list.getSelectedValue();
    boolean enableNavigation = list.getSelectedIndices().length == 1
        && selectedFilter != null
        && selectedFilter.isApplied();
    prevBtn.setEnabled(enableNavigation);
    nextBtn.setEnabled(enableNavigation);

    selectAllCb.setSelectionState(calculateSelectionState(list.getModel()));
  }

  /**
   * Computes the tri-state checkbox selection state based on whether all, some,
   * or none of the filters in the model are applied.
   *
   * @param listModel the list model containing current filters.
   * @return the resolved {@link TriStateCheckbox.SelectionState}.
   */
  private TriStateCheckbox.SelectionState calculateSelectionState(
      ListModel<Filter> listModel) {
    if (listModel == null || listModel.getSize() == 0) {
      return TriStateCheckbox.SelectionState.NOT_SELECTED;
    }

    boolean hasAtLeastOneFilterApplied = false;
    boolean allFiltersApplied = true;
    for (int i = 0; i < listModel.getSize(); i++) {
      Filter filter = listModel.getElementAt(i);
      boolean applied = filter.isApplied();
      hasAtLeastOneFilterApplied |= applied;
      allFiltersApplied &= applied;
    }

    if (allFiltersApplied) {
      return TriStateCheckbox.SelectionState.SELECTED;
    } else if (hasAtLeastOneFilterApplied) {
      return TriStateCheckbox.SelectionState.PARTIALLY_SELECTED;
    }
    return TriStateCheckbox.SelectionState.NOT_SELECTED;
  }

  /**
   * Deletes the currently selected filters using default listener.
   */
  private void deleteSelectedFilters() {
    deleteSelectedFilters(listenerSupplier.get());
  }

  /**
   * Deletes the currently selected filters.
   *
   * @param listener the active filters listener callback.
   */
  private void deleteSelectedFilters(FiltersList.FiltersListener listener) {
    if (listener != null) {
      listener.onDeleteFilters(groupName, list.getSelectedIndices());
    }
  }

  /**
   * Opens the edit dialog for the currently selected filter using default listener.
   */
  private void editSelectedFilter() {
    editSelectedFilter(listenerSupplier.get());
  }

  /**
   * Opens the edit dialog for the currently selected filter.
   *
   * @param listener the active filters listener callback.
   */
  private void editSelectedFilter(FiltersList.FiltersListener listener) {
    if (listener != null) {
      Filter filter = list.getSelectedValue();
      listener.onEditFilter(filter);
    }
  }

  /**
   * Duplicates the currently selected filter using default listener.
   */
  private void duplicateSelectedFilter() {
    duplicateSelectedFilter(listenerSupplier.get());
  }

  /**
   * Duplicates the currently selected filter.
   *
   * @param listener the active filters listener callback.
   */
  private void duplicateSelectedFilter(FiltersList.FiltersListener listener) {
    if (listener != null) {
      Filter filter = list.getSelectedValue();
      listener.onDuplicateFilter(groupName, filter);
    }
  }

  /**
   * Moves the currently selected filters using default listener.
   */
  private void moveSelectedFilters() {
    moveSelectedFilters(listenerSupplier.get());
  }

  /**
   * Moves the currently selected filters.
   *
   * @param listener the active filters listener callback.
   */
  private void moveSelectedFilters(FiltersList.FiltersListener listener) {
    if (listener != null) {
      listener.onMoveFilters(groupName, list.getSelectedIndices());
    }
  }

  /**
   * Notifies the listener when filters are reordered in the list.
   *
   * @param orig the original index.
   * @param dest the destination index.
   */
  private void reorderFilters(int orig, int dest) {
    FiltersList.FiltersListener listener = listenerSupplier.get();
    if (listener != null) {
      listener.onReordered(groupName, orig, dest);
    }
  }

  @Override
  public void updateUI() {
    super.updateUI();
    setOpaque(false);
    if (list != null) {
      list.setOpaque(false);
      list.setBackground(resolveCardBackground());
      list.updateUI();
      list.revalidate();
      list.repaint();
    }
    if (selectAllCb != null) {
      selectAllCb.updateUI();
    }
    if (prevBtn != null) {
      prevBtn.setIcon(CommonIcons.chevronLeftIcon(14));
    }
    if (nextBtn != null) {
      nextBtn.setIcon(CommonIcons.chevronRightIcon(14));
    }
    if (addBtn != null) {
      addBtn.setIcon(CommonIcons.plusIcon(14));
    }
    if (closeBtn != null) {
      closeBtn.setIcon(CommonIcons.closeIcon(14));
    }
    updateCardMargin();
    updateHeaderStyle();
    updateGroupHeader();
    revalidate();
    repaint();
  }

  /**
   * Displays or hides the unsaved changes indicator save button.
   *
   * @param unsaved true to display the save button, false to hide.
   */
  public void showUnsavedIndication(boolean unsaved) {
    saveBtn.setVisible(unsaved);
  }

  /**
   * Updates outer margin and inner padding around the group card so child
   * components stay comfortably inside card bounds.
   */
  private void updateCardMargin() {
    int vMargin = UIScaleUtils.dip(MARGIN_VERTICAL_DP);
    int hMargin = UIScaleUtils.dip(MARGIN_HORIZONTAL_DP);
    setBorder(new EmptyBorder(vMargin, hMargin, vMargin, hMargin));
  }

  /**
   * Updates the options pane header background and border.
   */
  private void updateHeaderStyle() {
    if (optionsPane == null) return;

    optionsPane.setOpaque(false);
    optionsPane.setBackground(resolveHeaderBackground());
    optionsPane.setBorder(BorderFactory.createEmptyBorder(
        UIScaleUtils.dip(2),
        UIScaleUtils.dip(4),
        UIScaleUtils.dip(2),
        UIScaleUtils.dip(4)));
  }

  /**
   * Resolves the corner arc radius in scaled pixels, honoring the Look and
   * Feel "Component.arc" setting if present, or falling back to 8dp.
   *
   * @return the scaled corner arc in pixels.
   */
  int resolveCardArc() {
    int themeArc = UIManager.getInt("Component.arc");
    if (themeArc > 0) {
      return UIScaleUtils.dip(themeArc);
    }
    return UIScaleUtils.dip(DEFAULT_CARD_ARC_DP);
  }

  /**
   * Resolves the card background color. Returns a clean elevated surface:
   * pure white / Table.background in light mode, #26282c in dark mode.
   *
   * @return the card background {@link Color}.
   */
  Color resolveCardBackground() {
    if (isDarkTheme()) {
      return Color.decode("#26282c");
    }
    Color tableBg = UIManager.getColor("Table.background");
    return tableBg != null ? tableBg : Color.WHITE;
  }

  /**
   * Resolves the subtle header background tint: ToolBar.background or
   * #f5f6f8 in light mode, #303238 in dark mode.
   *
   * @return the header background {@link Color}.
   */
  Color resolveHeaderBackground() {
    if (isDarkTheme()) {
      return Color.decode("#303238");
    }
    Color toolBarBg = UIManager.getColor("ToolBar.background");
    return toolBarBg != null ? toolBarBg : Color.decode("#f5f6f8");
  }

  /**
   * Resolves the card boundary stroke color: Component.borderColor or
   * subtle theme-aware border.
   *
   * @return the card border {@link Color}.
   */
  Color resolveCardBorderColor() {
    return CardBorderUtils.resolveCardBorderColor();
  }

  /**
   * Returns whether the active look-and-feel theme is a dark variant.
   *
   * @return true if dark theme, false otherwise.
   */
  private boolean isDarkTheme() {
    try {
      return ServiceLocator.INSTANCE.getThemeManager().isDark();
    } catch (Throwable t) {
      return FlatLaf.isLafDark();
    }
  }

  /**
   * Computes the rounded card shape based on current component dimensions and margins.
   *
   * @return the {@link Shape} of the card, or null if width or height are non-positive.
   */
  private Shape getCardShape() {
    int vMargin = UIScaleUtils.dip(MARGIN_VERTICAL_DP);
    int hMargin = UIScaleUtils.dip(MARGIN_HORIZONTAL_DP);
    int cardX = hMargin;
    int cardY = vMargin;
    int cardW = getWidth() - (hMargin * 2);
    int cardH = getHeight() - (vMargin * 2);

    if (cardW <= 0 || cardH <= 0) {
      return null;
    }

    int arc = resolveCardArc();
    return new RoundRectangle2D.Float(
        cardX, cardY, cardW - 1, cardH - 1, arc, arc);
  }

  /**
   * Paints the rounded rectangle card surface, optional header background
   * tint and divider with antialiasing.
   *
   * @param g the {@link Graphics} context to paint with.
   */
  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(
          RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON);

      Shape cardShape = getCardShape();
      if (cardShape == null) {
        return;
      }

      int vMargin = UIScaleUtils.dip(MARGIN_VERTICAL_DP);
      int hMargin = UIScaleUtils.dip(MARGIN_HORIZONTAL_DP);
      int cardX = hMargin;
      int cardY = vMargin;
      int cardW = getWidth() - (hMargin * 2);

      // Paints rounded rectangle card background
      g2.setColor(resolveCardBackground());
      g2.fill(cardShape);

      paintHeaderBackgroundAndDivider(g2, cardShape, cardX, cardY, cardW);
    } finally {
      g2.dispose();
    }
  }

  /**
   * Paints the subtle header background tint and divider line between header and list
   * when the group is expanded.
   *
   * @param g2 the 2D graphics context.
   * @param cardShape the clipping shape of the rounded card.
   * @param cardX the card left x position.
   * @param cardY the card top y position.
   * @param cardW the card width.
   */
  private void paintHeaderBackgroundAndDivider(Graphics2D g2, Shape cardShape,
      int cardX, int cardY, int cardW) {
    if (list == null || !list.isVisible()) {
      return;
    }

    int headerHeight = optionsPane != null
        ? (optionsPane.getHeight() > 0
            ? optionsPane.getHeight()
            : optionsPane.getPreferredSize().height)
        : 0;
    if (headerHeight <= 0) {
      return;
    }

    Shape originalClip = g2.getClip();
    g2.clip(cardShape);
    g2.setColor(resolveHeaderBackground());
    g2.fillRect(cardX, cardY, cardW, headerHeight);

    int dividerY = cardY + headerHeight;
    g2.setColor(resolveCardBorderColor());
    g2.drawLine(cardX, dividerY, cardX + cardW - 1, dividerY);
    g2.setClip(originalClip);
  }

  /**
   * Paints child components clipped to the rounded card geometry so child backgrounds
   * do not bleed over the rounded bottom corners.
   *
   * @param g the {@link Graphics} context to paint with.
   */
  @Override
  protected void paintChildren(Graphics g) {
    Shape cardShape = getCardShape();
    if (cardShape != null) {
      Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.clip(cardShape);
        super.paintChildren(g2);
      } finally {
        g2.dispose();
      }
    } else {
      super.paintChildren(g);
    }
  }

  /**
   * Paints the rounded rectangle card border on top of child components with antialiasing.
   *
   * @param g the {@link Graphics} context to paint with.
   */
  @Override
  protected void paintBorder(Graphics g) {
    super.paintBorder(g);

    Shape cardShape = getCardShape();
    if (cardShape != null) {
      Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(resolveCardBorderColor());
        g2.draw(cardShape);
      } finally {
        g2.dispose();
      }
    }
  }

  /**
   * Updates the group header button text with expand/collapse chevron arrow,
   * group name, and filter count badge.
   */
  private void updateGroupHeader() {
    if (hideGroupBtn == null || list == null) return;

    ListModel<Filter> listModel = list.getModel();
    int count = 0;
    int selectedCount = 0;

    if (listModel != null) {
      count = listModel.getSize();
      for (int i = 0; i < count; i++) {
        if (listModel.getElementAt(i).isApplied()) {
          selectedCount++;
        }
      }
    }

    String filtersCount = count == 0 ? "(0)" : "(" + selectedCount + " / " + count + ")";
    hideGroupBtn.setIcon(isGroupVisible()
        ? CommonIcons.chevronDownIcon(14)
        : CommonIcons.chevronRightIcon(14));
    hideGroupBtn.setIconTextGap(UIScaleUtils.dip(4));
    hideGroupBtn.setText(groupName + " " + filtersCount);
  }
}
