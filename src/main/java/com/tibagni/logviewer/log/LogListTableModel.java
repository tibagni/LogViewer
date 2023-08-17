package com.tibagni.logviewer.log;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
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

  public void addLog(LogEntry entry) {
    // find the nearest time pos to insert the new entry
    int possibleIndex = Collections.binarySearch(this.entries, entry);
    int targetIndex = possibleIndex;
    if (possibleIndex < 0) {
      targetIndex = Math.abs(possibleIndex + 1);
    }
    this.entries.add(targetIndex, entry);
    fireTableRowsInserted(targetIndex, targetIndex);
  }

  public void removeLog(LogEntry entry) {
    int index = this.entries.indexOf(entry);
    if (index != -1) {
      this.entries.remove(index);
      fireTableRowsDeleted(index, index);
    }
  }

  public void clearLog() {
    if (this.entries.isEmpty()) return;

    int index = this.entries.size() - 1;
    this.entries.clear();
    fireTableRowsDeleted(0, index);
  }
}
