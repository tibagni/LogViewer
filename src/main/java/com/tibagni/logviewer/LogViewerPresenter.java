package com.tibagni.logviewer;

import com.tibagni.logviewer.filter.Filter;
import com.tibagni.logviewer.log.LogEntry;
import com.tibagni.logviewer.log.LogStream;

import java.io.File;
import java.util.List;

public interface LogViewerPresenter {

  enum UserSelection {
    CONFIRMED,
    REJECTED,
    CANCELLED
  }

  void init();
  void addFilter(String group, Filter newFilter);
  void addFilter(String group, Filter newFilter, boolean ignoreReapply);
  String addGroup(String group);
  List<String> getGroups();
  void removeFilters(String group, int[] indices);
  void moveFilters(String origGroup, String destGroup, int[] indices);
  void removeGroup(String group);
  void reorderFilters(String group, int orig, int dest);
  int getNextFilteredLogForFilter(Filter filter, int firstLogIndexSearch);
  int getPrevFilteredLogForFilter(Filter filter, int firstLogIndexSearch);
  void goToTimestamp(String timestamp);
  void saveFilters(String group);
  void loadFilters(File[] filtersFile, boolean keepCurrentFilters);
  void loadLogs(File[] logFiles);
  void refreshLogs();
  void saveFilteredLogs(File file);
  void applyFilters();
  void filterEdited(Filter filter);

  void setAllFiltersApplied(String group, boolean isApplied);
  void setAllFiltersApplied(boolean isApplied);

  void setStreamAllowed(LogStream stream, boolean allowed);
  boolean isStreamAllowed(LogStream stream);

  void ignoreLogsBefore(int index);
  void ignoreLogsAfter(int index);
  void resetIgnoredLogs(boolean resetStarting, boolean resetEnding);
  int getVisibleLogsOffset();

  LogEntry getFirstVisibleLog();
  LogEntry getLastVisibleLog();

  void addLogEntriesToMyLogs(List<LogEntry> entries);
  void removeFromMyLog(int[] indices);

  void finishing();
}
