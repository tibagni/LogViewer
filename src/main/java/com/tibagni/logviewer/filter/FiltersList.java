package com.tibagni.logviewer.filter;

import com.tibagni.logviewer.util.CommonUtils;
import com.tibagni.logviewer.util.StringUtils;
import com.tibagni.logviewer.view.CheckBoxList;
import com.tibagni.logviewer.view.FlatButton;
import com.tibagni.logviewer.view.ReorderableCheckBoxList;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class FiltersList extends JPanel {
  private FiltersListener listener;
  private Map<String, Filter[]> filters;
  private Map<String, FilterUIGroup> filterUIGroups;

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

  private void initialize() {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    filterUIGroups = new HashMap<>();
    filters = new HashMap<>();
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

  private class FilterUIGroup extends JPanel {
    private final String SHOW;
    private final String HIDE;

    String groupName;
    private JButton hideGroupBtn;
    private JButton prevBtn;
    private JButton nextBtn;
    private JButton addBtn;
    private JButton saveBtn;
    private JButton deleteBtn;
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
          searchHint.setOpaque(true);
          searchHint.setBackground(Color.LIGHT_GRAY);
          searchHint.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 24));
          Point targetListLocation = targetList.getLocationOnScreen();
          int popupX = targetListLocation.x + 10;
          int popupY = targetListLocation.y - 40;
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
      nextBtn = new FlatButton(StringUtils.RIGHT_BLACK_POINTER);
      addBtn = new FlatButton(StringUtils.PLUS);
      saveBtn = new FlatButton("save");
      saveBtn.setVisible(false);
      deleteBtn = new FlatButton(StringUtils.DELETE);

      list = new ReorderableCheckBoxList<>();
      list.setCellRenderer(cellRenderer);

      JPanel optionsPane = new JPanel(new BorderLayout());
      optionsPane.add(hideGroupBtn, BorderLayout.WEST);
      JPanel prevNextPane = new JPanel(new FlowLayout(FlowLayout.TRAILING));
      prevNextPane.add(saveBtn);
      prevNextPane.add(addBtn);
      prevNextPane.add(prevBtn);
      prevNextPane.add(nextBtn);
      prevNextPane.add(deleteBtn);
      optionsPane.add(prevNextPane, BorderLayout.EAST);
      add(optionsPane, BorderLayout.NORTH);
      add(list);

      hideGroupBtn.addActionListener(l -> toggleGroupVisibility());

      updateNavigationButtons();
      setListData(filters);
      configureContextActions();
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
    }

    private void updatePreferredSize() {
      Dimension maximumSize = getMaximumSize();
      Dimension preferredSize = getPreferredSize();
      setMaximumSize(new Dimension(maximumSize.width, preferredSize.height));
    }

    private void configureContextActions() {
      JPopupMenu popup = new JPopupMenu();
      final JLabel menuTitle = new JLabel();
      menuTitle.setBorder(new EmptyBorder(0, 10, 0, 0));
      popup.add(menuTitle);
      popup.add(new JPopupMenu.Separator());
      JMenuItem deleteMenuItem = popup.add("Delete");
      JMenuItem editMenuItem = popup.add("Edit");

      deleteMenuItem.addActionListener(e -> deleteSelectedFilters());
      editMenuItem.addActionListener(e -> editSelectedFilter());

      list.addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent me) {
          int indexClicked = list.locationToIndex(me.getPoint());

          if (SwingUtilities.isRightMouseButton(me) && !list.isSelectionEmpty()) {
            int[] selectedIndices = list.getSelectedIndices();
            if (IntStream.of(selectedIndices).anyMatch(i -> i == indexClicked)) {
              int selectedFilters = selectedIndices.length;
              menuTitle.setText(selectedFilters + " item(s) selected");
              editMenuItem.setVisible(selectedFilters == 1);

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
        public void focusLost(FocusEvent e) {
          // Send an "escape" key to search handler to cancel it when list loses focus
          listSearchHandler.handleKeyTyped((char) KeyEvent.VK_ESCAPE);
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
      deleteBtn.addActionListener(l -> {
        if(listener != null){
          listener.onDeleteGroup(groupName);
        }
      });

      list.addListSelectionListener(e -> updateNavigationButtons());
      list.setReorderedListener(this::reorderFilters);
      list.setItemsCheckListener((CheckBoxList.ItemsCheckListener<Filter>) elements -> {
        elements.forEach(f -> f.setApplied(!f.isApplied()));
        if (listener != null) {
          listener.onFiltersApplied();
        }
        updateNavigationButtons();
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

    private void updateNavigationButtons() {
      boolean enable = list.getSelectedIndices().length == 1 && list.getSelectedValue().isApplied();
      prevBtn.setEnabled(enable);
      nextBtn.setEnabled(enable);
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
    void onDeleteFilters(String group, int[] indices);
    void onDeleteGroup(String group);
    void onNavigateNextFilteredLog(Filter filter);
    void onNavigatePrevFilteredLog(Filter filter);
    void onAddFilter(String group);
    void onSaveFilters(String group);
  }
}
