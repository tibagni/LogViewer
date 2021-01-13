package com.tibagni.logviewer.log;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class LogListTableModel extends AbstractTableModel {
  private final List<LogEntry> entries = new ArrayList<>();
  private final String title;

  public LogListTableModel(String title) {
    this.title = title;
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    // We only have one column
    return LogEntry.class;
  }

  @Override
  public String getColumnName(int column) {
    return title;
  }

  @Override
  public int getRowCount() {
    return entries.size();
  }

  @Override
  public int getColumnCount() {
    return 1;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    return entries.get(rowIndex);
  }

  public void setLogs(List<LogEntry> entries) {
    this.entries.clear();
    this.entries.addAll(entries);
    fireTableRowsInserted(0, this.entries.size() - 1);
  }
}
