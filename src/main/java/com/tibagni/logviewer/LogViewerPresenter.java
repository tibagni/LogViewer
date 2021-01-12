package com.tibagni.logviewer;

import com.tibagni.logviewer.filter.Filter;
import com.tibagni.logviewer.log.LogEntry;
import com.tibagni.logviewer.log.LogStream;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface LogViewerPresenter {

  enum UserSelection {
    CONFIRMED,
    REJECTED,
    CANCELLED
  }

  void init();
  void addFilter(String group, Filter newFilter);
  String addGroup(String group);
  List<String> getGroups();
  void removeFilters(String group, int[] indices);
  void removeGroup(String group);
  void reorderFilters(String group, int orig, int dest);
  int getNextFilteredLogForFilter(Filter filter, int firstLogIndexSearch);
  int getPrevFilteredLogForFilter(Filter filter, int firstLogIndexSearch);
  void saveFilters(String group);
  void loadFilters(File[] filtersFile, boolean keepCurrentFilters);
  void loadLogs(File[] logFiles);
  void refreshLogs();
  void saveFilteredLogs(File file);
  void applyFilters();
  void filterEdited(Filter filter);

  void setStreamAllowed(LogStream stream, boolean allowed);
  boolean isStreamAllowed(LogStream stream);

  void finishing();
}
