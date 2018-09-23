package com.tibagni.logviewer.log;

import javax.swing.table.AbstractTableModel;

public class LogListTableModel extends AbstractTableModel {
  private LogEntry[] entries;
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
    if (entries == null) {
      return 0;
    }
    return entries.length;
  }

  @Override
  public int getColumnCount() {
    return 1;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    return entries[rowIndex];
  }

  public void setLogs(LogEntry[] entries) {
    this.entries = entries;
    fireTableRowsInserted(0, entries.length - 1);
  }
}
