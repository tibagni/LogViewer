package com.tibagni.logviewer.log;

import org.jetbrains.annotations.Nullable;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class LogListTableModel extends AbstractTableModel {
  protected final List<LogEntry> entries = new ArrayList<>();
  protected final String title;

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

  public @Nullable LogEntry getLastEntry() {
    if (entries.isEmpty()) {
      return null;
    }

    return entries.get(entries.size() - 1);
  }

  public void setLogs(List<LogEntry> entries) {
    this.entries.clear();
    this.entries.addAll(entries);
    fireTableRowsInserted(0, this.entries.size() - 1);
  }

  public void clear() {
    if (entries.isEmpty()) return;

    int index = entries.size() - 1;
    entries.clear();
    fireTableRowsDeleted(0, index);
  }
}
