package com.tibagni.logviewer.filter;

import javax.swing.table.AbstractTableModel;

public class FiltersListTableModel extends AbstractTableModel {
  private Filter[] filters;
  private String groupName;

  public FiltersListTableModel(String group) {
    groupName = group;
  }

  @Override
  public int getRowCount() {
    if (filters == null) {
      return 0;
    }

    return filters.length;
  }

  @Override
  public int getColumnCount() {
    return 1;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    return filters[rowIndex];
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    return Filter.class;
  }

  @Override
  public String getColumnName(int column) {
    return groupName;
  }

  public void setFilters(Filter[] filters) {
    this.filters = filters;
    fireTableRowsInserted(0, filters.length - 1);
  }
}
