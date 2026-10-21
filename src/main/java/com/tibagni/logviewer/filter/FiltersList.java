package com.tibagni.logviewer.filter;

import com.tibagni.logviewer.util.CommonUtils;
import com.tibagni.logviewer.util.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Container panel displaying groups of filters with actions to add, remove, and reorder filters.
 */
public class FiltersList extends JPanel {
  private FiltersListener listener;
  private Map<String, Filter[]> filters;
  private Map<String, FilterUIGroup> filterUIGroups;
  private FilterContextTag activeFilterContextTag;

  public void setActiveFilterContextTag(FilterContextTag activeFilterContextTag) {
    this.activeFilterContextTag = activeFilterContextTag;
  }

  public FilterContextTag getActiveFilterContextTag() {
    return activeFilterContextTag;
  }

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
        groupEntry.getValue().updateUI();
        groupEntry.getValue().updateActionPaneButtons();
      }
    }
  }

  public void updateActionPaneButtons() {
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
    setAllGroupsVisibility(visible);
  }

  public void setAllGroupsVisibility(boolean visible) {
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
    Set<String> toUpdate = new HashSet<>(newFilters.keySet());

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
    FilterUIGroup groupContainer = new FilterUIGroup(
        group,
        filters,
        this::getActiveFilterContextTag,
        () -> listener,
        this::onFilterUIGroupFocusChanged);
    add(groupContainer);

    filterUIGroups.put(group, groupContainer);
  }

  private void removeGroup(String group) {
    FilterUIGroup groupContainer = filterUIGroups.remove(group);
    if (groupContainer != null) {
      groupContainer.cancelSearch();
      remove(groupContainer);
    }
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

  public void selectFilter(String group, Filter filter) {
    FilterUIGroup groupContainer = filterUIGroups.get(group);
    if (groupContainer == null) return;

    if (!groupContainer.isGroupVisible()) {
      groupContainer.toggleGroupVisibility();
    }

    groupContainer.getList().setSelectedValue(filter, true);
    groupContainer.getList().requestFocusInWindow();

    int index = groupContainer.getList().getSelectedIndex();
    if (index >= 0) {
      Rectangle cellBounds = groupContainer.getList().getCellBounds(index, index);
      if (cellBounds != null) {
        Rectangle groupBounds = groupContainer.getBounds();
        scrollRectToVisible(new Rectangle(
            groupBounds.x,
            groupBounds.y + cellBounds.y,
            groupBounds.width,
            cellBounds.height));
      }
    }
  }

  private void onFilterUIGroupFocusChanged(String currentFocus) {
    // Whenever the focus is changed to a different filter group, make sure to clean the search of all others
    for (Map.Entry<String, FilterUIGroup> entry : filterUIGroups.entrySet()) {
      if (!StringUtils.areEquals(currentFocus, entry.getKey())) {
        entry.getValue().cancelSearch();
      }
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
