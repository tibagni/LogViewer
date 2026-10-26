package com.tibagni.logviewer.filter;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.tibagni.logviewer.ServiceLocator;
import com.tibagni.logviewer.i18n.I18n;
import com.tibagni.logviewer.preferences.LogViewerPreferences;
import com.tibagni.logviewer.shortcuts.ShortcutAction;
import com.tibagni.logviewer.shortcuts.ShortcutManager;
import com.tibagni.logviewer.util.StringUtils;
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
  static final int MARGIN_VERTICAL_DP = 3;
  static final int MARGIN_HORIZONTAL_DP = 4;
  private static final int DEFAULT_CARD_ARC_DP = 8;

  private final String groupName;
  private final JButton hideGroupBtn;
  private final JButton prevBtn;
  private final JButton nextBtn;
  private final JButton addBtn;
  private final JButton saveBtn;
  private final JButton closeBtn;
  private final TriStateCheckbox selectAllCb;
  private final JPanel optionsPane;
  private final JPanel groupActionsPane;
  private final ReorderableCheckBoxList<Filter> list;
  private final FilterCellRenderer cellRenderer;
  private final FilterListSearchHandler listSearchHandler;

  private final Supplier<FiltersList.FiltersListener> listenerSupplier;
  private final Consumer<String> focusGainedCallback;
  private final LogViewerPreferences userPrefs = ServiceLocator.INSTANCE.getLogViewerPrefs();
  private final ShortcutManager shortcutManager = ServiceLocator.INSTANCE.getShortcutManager();

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
   * @param listenerSupplier supplier of the active [FiltersList.FiltersListener].
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

    hideGroupBtn = createToolbarComponent(new FlatButton());

    prevBtn = createToolbarComponent(new FlatButton());
    prevBtn.setIcon(CommonIcons.chevronLeftIcon(14));

    nextBtn = createToolbarComponent(new FlatButton());
    nextBtn.setIcon(CommonIcons.chevronRightIcon(14));
    updateNavigationTooltips();

    addBtn = createToolbarComponent(new FlatButton());
    addBtn.setIcon(CommonIcons.plusIcon(14));
    addBtn.setToolTipText(I18n.get(I18n.FILTERS_TOOLTIP_ADD));

    saveBtn = createToolbarComponent(new FlatButton(I18n.get(I18n.COMMON_SAVE)));
    saveBtn.setToolTipText(I18n.get(I18n.FILTERS_TOOLTIP_SAVE));
    saveBtn.setVisible(false);

    closeBtn = createToolbarComponent(new FlatButton());
    closeBtn.setIcon(CommonIcons.closeIcon(14));
    closeBtn.setToolTipText(I18n.get(I18n.FILTERS_TOOLTIP_CLOSE_GROUP));

    selectAllCb = createToolbarComponent(new TriStateCheckbox());
    selectAllCb.setToolTipText(I18n.get(I18n.FILTERS_TOOLTIP_SELECT_ALL));
    selectAllCb.setOpaque(false);

    list = new ReorderableCheckBoxList<>();
    list.setOpaque(false);
    list.setCellRenderer(cellRenderer);

    optionsPane = new JPanel(new BorderLayout());
    optionsPane.setOpaque(false);
    optionsPane.add(hideGroupBtn, BorderLayout.WEST);

    groupActionsPane = new JPanel(new FlowLayout(FlowLayout.TRAILING, UIScaleUtils.dip(2), 0));
    groupActionsPane.setOpaque(false);
    groupActionsPane.add(selectAllCb);
    groupActionsPane.add(saveBtn);
    groupActionsPane.add(addBtn);
    groupActionsPane.add(prevBtn);
    groupActionsPane.add(nextBtn);
    groupActionsPane.add(closeBtn);
    optionsPane.add(groupActionsPane, BorderLayout.EAST);

    add(optionsPane, BorderLayout.NORTH);
    add(list, BorderLayout.CENTER);

    updateCardMargin();
    updateHeaderStyle();

    hideGroupBtn.addActionListener(l -> toggleGroupVisibility());

    listSearchHandler = new FilterListSearchHandler(list, cellRenderer);

    setListData(filters);
    configureContextActions();
    updateActionPaneButtons();
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
   * @return the card background [Color].
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
   * @return the header background [Color].
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
   * @return the card border [Color].
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
   * @return the [Shape] of the card, or null if width or height are non-positive.
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
   * @param g the [Graphics] context to paint with.
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

      // When expanded (list.isVisible()): paints subtle header background
      // tint clipped to the rounded card shape across optionsPane.getHeight(),
      // and a hairline divider line between header and list.
      if (list != null && list.isVisible()) {
        int headerHeight = optionsPane != null
            ? (optionsPane.getHeight() > 0
                ? optionsPane.getHeight()
                : optionsPane.getPreferredSize().height)
            : 0;
        if (headerHeight > 0) {
          Shape originalClip = g2.getClip();
          g2.clip(cardShape);
          g2.setColor(resolveHeaderBackground());
          g2.fillRect(cardX, cardY, cardW, headerHeight);

          int dividerY = cardY + headerHeight;
          g2.setColor(resolveCardBorderColor());
          g2.drawLine(cardX, dividerY, cardX + cardW - 1, dividerY);
          g2.setClip(originalClip);
        }
      }
    } finally {
      g2.dispose();
    }
  }

  /**
   * Paints child components clipped to the rounded card geometry so child backgrounds
   * do not bleed over the rounded bottom corners.
   *
   * @param g the [Graphics] context to paint with.
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
   * @param g the [Graphics] context to paint with.
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

    int count = list.getModel() != null ? list.getModel().getSize() : 0;
    hideGroupBtn.setIcon(isGroupVisible()
        ? CommonIcons.chevronDownIcon(14)
        : CommonIcons.chevronRightIcon(14));
    hideGroupBtn.setIconTextGap(UIScaleUtils.dip(4));
    hideGroupBtn.setText(groupName + " (" + count + ")");
  }
}
