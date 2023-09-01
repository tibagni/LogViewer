package com.tibagni.logviewer.log;

import org.jetbrains.annotations.Nullable;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

  public @Nullable LogEntry getMatchingLogEntry(LogEntry entry) {
    // Here we want to check ig the given log entry exists anywhere in the list, not necessarily in the same index,
    // And we also want to make sure the text is the same. So, use a different comparator here that only considers
    // the timestamp for comparison and also checks if the log text is the same
    Comparator<LogEntry> cmp = Comparator
        .comparing((LogEntry o) -> o.timestamp);

    int indexFound = Collections.binarySearch(entries, entry, cmp);
    if (indexFound >= 0) {
      // We found one index for a possible entry. But there might be multiple log entries for the same timestamp
      // so, iterate until we find the exact line we are looking for.
      // First we want to find the first log in this timestamp
      int i = indexFound;
      while ( i >= 0 && entries.get(i).timestamp.equals(entry.timestamp)) {
        i--;
      }

      // Now that we are in the beginning of the timestamp, look for the entry
      while (!entries.get(i).logText.equals(entry.logText) &&
          entries.get(i).timestamp.compareTo(entry.timestamp) <= 0) {
        i++;
      }

      // We either found or finished search. check which one
      if (entries.get(i).logText.equals(entry.logText)) {
        return entries.get(i);
      }
    }

    // Not found
    return null;
  }
}
