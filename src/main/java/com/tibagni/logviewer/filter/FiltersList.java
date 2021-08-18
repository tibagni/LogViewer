package com.tibagni.logviewer.filter;

import com.tibagni.logviewer.ServiceLocator;
import com.tibagni.logviewer.theme.LogViewerThemeManager;
import com.tibagni.logviewer.util.CommonUtils;
import com.tibagni.logviewer.util.StringUtils;
import com.tibagni.logviewer.util.SwingUtils;
import com.tibagni.logviewer.util.scaling.UIScaleUtils;
import com.tibagni.logviewer.view.CheckBoxList;
import com.tibagni.logviewer.view.FlatButton;
import com.tibagni.logviewer.view.ReorderableCheckBoxList;
import com.tibagni.logviewer.view.TriStateCheckbox;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

public class FiltersList extends JPanel {
  private FiltersListener listener;
  private Map<String, Filter[]> filters;
  private Map<String, FilterUIGroup> filterUIGroups;
  private final LogViewerThemeManager themeManager = ServiceLocator.INSTANCE.getThemeManager();

  public FiltersList(LayoutManager layout, boolean isDoubleBuffered) {
    super(layout, isDoubleBuffered);
    initialize();
  }

  public FiltersList(LayoutManager layout) {
    super(layout);
    initialize();
  }

  public FiltersList(boolean isDoubleBuffered) {
    super(isDoubleBuffered);
    initialize();
  }

  public FiltersList() {
    initialize();
  }

  @Override
  public void updateUI() {
    super.updateUI();
    if (filterUIGroups != null) {
      for (Map.Entry<String, FilterUIGroup> groupEntry : filterUIGroups.entrySet()) {
        groupEntry.getValue().updateActionPaneButtons();
      }
    }
  }

  private void initialize() {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    filterUIGroups = new HashMap<>();
    filters = new HashMap<>();
  }

  public void toggleGroupsVisibility() {
    boolean visible = !hasAtLeastOneGroupVisible();
    for (Map.Entry<String, FilterUIGroup> groupEntry : filterUIGroups.entrySet()) {
      groupEntry.getValue().forceGroupVisibilitySilently(visible);
    }

    if (listener != null) {
      listener.onGroupVisibilityChanged(null);
    }
  }

  public boolean hasAtLeastOneGroupVisible() {
    for (Map.Entry<String, FilterUIGroup> groupEntry : filterUIGroups.entrySet()) {
      if (groupEntry.getValue().isGroupVisible()) {
        return true;
      }
    }
    return false;
  }

  public boolean isEmpty() {
    return filters.isEmpty();
  }

  public void setFilters(Map<String, List<Filter>> newFilters) {
    Set<String> toAdd = new HashSet<>();
    Set<String> toRemove = new HashSet<>();
    Set<String> toUpdate = newFilters.keySet();

    // First check which filters we currently have but need to remove
    for (String group : filters.keySet()) {
      if (!newFilters.containsKey(group)) {
        toRemove.add(group);
      }
    }

    // Now check which filters we need to add
    for (String group : newFilters.keySet()) {
      if (!filters.containsKey(group)) {
        toAdd.add(group);
      }
    }

    for (String group : toRemove) {
      removeGroup(group);
      filters.remove(group);
    }

    // Sort to always display the groups in alphabetical order
    java.util.List<String> sortedToAdd = CommonUtils.asSortedList(toAdd);
    for (String group : sortedToAdd) {
      Filter[] arr = newFilters.get(group).toArray(new Filter[0]);
      addGroup(group, arr);
      filters.put(group, arr);
    }

    // There is no need to update the filters if they were just added
    toUpdate.removeAll(toAdd);
    for (String group : toUpdate) {
      Filter[] arr = newFilters.get(group).toArray(new Filter[0]);
      FilterUIGroup groupContainer = filterUIGroups.get(group);
      groupContainer.setListData(arr);
      filters.put(group, arr);
    }

    revalidate();
    repaint();
  }

  private void addGroup(String group, Filter[] filters) {
    FilterUIGroup groupContainer = new FilterUIGroup(group, filters);
    add(groupContainer);

    filterUIGroups.put(group, groupContainer);
  }

  private void removeGroup(String group) {
    FilterUIGroup groupContainer = filterUIGroups.remove(group);
    remove(groupContainer);
  }

  public void setFiltersListener(FiltersListener listener) {
    this.listener = listener;
  }

  public void showUnsavedIndication(String group, boolean unsaved) {
    FilterUIGroup groupContainer = filterUIGroups.get(group);
    if (groupContainer != null) {
      groupContainer.showUnsavedIndication(unsaved);
    }
  }

  private void onFilterUIGroupFocusChanged(String currentFocus) {
    // Whenever the focus is changed to a different filter group, make sure to clean the search of all others
    for (String group : filterUIGroups.keySet()) {
      if (!StringUtils.areEquals(currentFocus, group)) {
        filterUIGroups.get(group).listSearchHandler.handleKeyTyped((char) KeyEvent.VK_ESCAPE);
      }
    }
  }

  private class FilterUIGroup extends JPanel {
    private final String SHOW;
    private final String HIDE;

    String groupName;
    private JButton hideGroupBtn;
    private JButton prevBtn;
    private JButton nextBtn;
    private JButton addBtn;
    private JButton saveBtn;
    private JButton closeBtn;
    private TriStateCheckbox selectAllCb;
    private ReorderableCheckBoxList<Filter> list;
    private FilterCellRenderer cellRenderer;
    private ListSearchHandler listSearchHandler;

    private class ListSearchHandler {
      private JList targetList;
      private FilterCellRenderer filterCellRenderer;

      private JLabel searchHint;
      private Popup hintPopup;

      private boolean isSearching;
      private StringBuilder searchString = new StringBuilder();

      public ListSearchHandler(JList targetList, FilterCellRenderer filterCellRenderer) {
        this.targetList = targetList;
        this.filterCellRenderer = filterCellRenderer;
      }

      public void handleKeyTyped(char charTyped) {
        if (!isSearching) {
          // We only start searching if user pressed '/'
          if (charTyped == '/') {
            initSearch();
          }
          return;
        }

        if (charTyped == KeyEvent.VK_ESCAPE) {
          clearSearch();
          filterCellRenderer.setHighlightedText(null);
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
        if (isSearching) {
          initHintPopup();
          searchHint.setText("/" + searchString.toString());
          hintPopup.show();
        } else {
          destroyHintPopup();
        }

        targetList.invalidate();
        targetList.repaint();
      }

      private void initHintPopup() {
        if (hintPopup == null) {
          searchHint = new JLabel();
          searchHint.setBorder(BorderFactory.createLineBorder(Color.GRAY));
          searchHint.setOpaque(true);
          searchHint.setBackground(themeManager.isDark() ? Color.BLACK : Color.LIGHT_GRAY);
          searchHint.setFont(new Font(Font.MONOSPACED, Font.PLAIN, UIScaleUtils.scaleFont(24)));
          Point targetListLocation = targetList.getRootPane().getLocationOnScreen();
          int popupX = targetListLocation.x + UIScaleUtils.dip(50);
          int popupY = targetListLocation.y + UIScaleUtils.dip(50);
          hintPopup = PopupFactory.getSharedInstance().getPopup(targetList, searchHint, popupX, popupY);
        }
      }

      private void destroyHintPopup() {
        if (hintPopup != null) {
          hintPopup.hide();
          hintPopup = null;
          searchHint = null;
        }
      }
    }

    public FilterUIGroup(String groupName, Filter[] filters) {
      this.groupName = groupName;
      SHOW = StringUtils.RIGHT_ARROW_HEAD + " " + groupName;
      HIDE = StringUtils.DOWN_ARROW_HEAD + " " + groupName;

      setLayout(new BorderLayout());

      cellRenderer = new FilterCellRenderer();
      hideGroupBtn = new FlatButton(HIDE);

      prevBtn = new FlatButton(StringUtils.LEFT_BLACK_POINTER);
      prevBtn.setToolTipText("Navigate to previous filtered log");

      nextBtn = new FlatButton(StringUtils.RIGHT_BLACK_POINTER);
      nextBtn.setToolTipText("Navigate to next filtered log");

      addBtn = new FlatButton(StringUtils.PLUS);
      addBtn.setToolTipText("Add a new filter to this group");

      saveBtn = new FlatButton("save");
      saveBtn.setVisible(false);

      closeBtn = new FlatButton(StringUtils.DELETE);
      closeBtn.setToolTipText("Close group");

      selectAllCb = new TriStateCheckbox();
      selectAllCb.setToolTipText("Apply/Un-Apply all filters from this group");

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

      setListData(filters);
      configureContextActions();
      updateActionPaneButtons();
    }

    public void setListData(Filter[] filters) {
      list.setListData(filters);
      updatePreferredSize();
    }

    private void toggleGroupVisibility() {
      boolean newVisibility = !list.isVisible();
      hideGroupBtn.setText(newVisibility ? HIDE : SHOW);
      list.setVisible(newVisibility);

      updatePreferredSize();

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
      JMenuItem deleteMenuItem = popup.add("Delete");
      JMenuItem editMenuItem = popup.add("Edit");
      JMenuItem duplicateMenuItem = popup.add("Duplicate");
      JMenuItem moveMenuItem = popup.add("Move");

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
              menuTitle.setText(selectedFilters + " item(s) selected");
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

      // Add support for searching in the filters list
      listSearchHandler = new ListSearchHandler(list, cellRenderer);
      list.addKeyListener(new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent e) {
          listSearchHandler.handleKeyTyped(e.getKeyChar());
        }
      });

      list.addFocusListener(new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
          onFilterUIGroupFocusChanged(groupName);
        }
      });

      // Add the shortcuts for the context menu
      list.addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          // We only consider backspace if user is not searching
          boolean backspace = e.getKeyCode() == KeyEvent.VK_BACK_SPACE && !listSearchHandler.isSearching;

          if (!list.isSelectionEmpty() && list.getModel().getSize() > 0 &&
              (e.getKeyCode() == KeyEvent.VK_DELETE || backspace)) {
            deleteSelectedFilters();
          } else if (e.getKeyCode() == KeyEvent.VK_ENTER && list.getSelectedIndices().length == 1) {
            editSelectedFilter();
          } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            list.clearSelection();
          } else if (e.getKeyChar() == ',' || e.getKeyChar() == '.') {
            if (e.getKeyChar() == ',') {
              navigatePrev();
            } else {
              navigateNext();
            }
          }
        }
      });

      prevBtn.addActionListener(l -> navigatePrev());
      nextBtn.addActionListener(l -> navigateNext());
      addBtn.addActionListener(l -> {
        if (listener != null) {
          listener.onAddFilter(groupName);
        }
      });
      saveBtn.addActionListener(l -> {
        if (listener != null) {
          listener.onSaveFilters(groupName);
        }
      });
      closeBtn.addActionListener(l -> {
        if (listener != null) {
          listener.onCloseGroup(groupName);
        }
      });
      selectAllCb.addSelectionChangedListener(newSelectionState -> {
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
            // Update list UI to render the correct state for the checkboxes even if the filters are not really applied
            // (If there are no logs open, the filters will not actually get applied)
            list.updateUI();
            listener.onFiltersApplied();
          }
        }
      });

      list.addListSelectionListener(e -> updateActionPaneButtons());
      list.setReorderedListener(this::reorderFilters);
      list.setItemsCheckListener((CheckBoxList.ItemsCheckListener<Filter>) elements -> {
        elements.forEach(f -> f.setApplied(!f.isApplied()));
        if (listener != null) {
          listener.onFiltersApplied();
        }
        updateActionPaneButtons();
      });
    }

    private void navigatePrev() {
      if (listener != null) {
        Filter selectedFilter = list.getSelectedValue();
        if (selectedFilter != null) {
          listener.onNavigatePrevFilteredLog(selectedFilter);
        }
      }
    }

    private void navigateNext() {
      if (listener != null) {
        Filter selectedFilter = list.getSelectedValue();
        if (selectedFilter != null) {
          listener.onNavigateNextFilteredLog(selectedFilter);
        }
      }
    }

    private void updateActionPaneButtons() {
      if (list == null) return;

      boolean enableNavigation = list.getSelectedIndices().length == 1 && list.getSelectedValue().isApplied();
      prevBtn.setEnabled(enableNavigation);
      nextBtn.setEnabled(enableNavigation);

      boolean hasAtLeastOneFilterApplied = false;
      boolean allFiltersApplied = true;
      ListModel<Filter> listModel = list.getModel();
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
      if (listener != null) {
        listener.onDeleteFilters(groupName, list.getSelectedIndices());
      }
    }

    private void editSelectedFilter() {
      if (listener != null) {
        Filter filter = list.getSelectedValue();
        listener.onEditFilter(filter);
      }
    }

    private void duplicateSelectedFilter() {
      if (listener != null) {
        Filter filter = list.getSelectedValue();
        listener.onDuplicateFilter(groupName, filter);
      }
    }

    private void moveSelectedFilters() {
      if (listener != null) {
        listener.onMoveFilters(groupName, list.getSelectedIndices());
      }
    }

    private void reorderFilters(int orig, int dest) {
      if (listener != null) {
        listener.onReordered(groupName, orig, dest);
      }
    }

    @Override
    public void updateUI() {
      super.updateUI();
      if (list != null) {
        list.updateUI();
      }
    }

    public void showUnsavedIndication(boolean unsaved) {
      saveBtn.setVisible(unsaved);
    }
  }

  public interface FiltersListener {
    void onReordered(String group, int orig, int dest);
    void onFiltersApplied();
    void onEditFilter(Filter filter);
    void onDuplicateFilter(String group, Filter filter);
    void onDeleteFilters(String group, int[] indices);
    void onMoveFilters(String group, int[] indices);
    void onCloseGroup(String group);
    void onNavigateNextFilteredLog(Filter filter);
    void onNavigatePrevFilteredLog(Filter filter);
    void onAddFilter(String group);
    void onSaveFilters(String group);
    void onGroupVisibilityChanged(String group);
  }
}
