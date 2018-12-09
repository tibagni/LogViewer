package com.tibagni.logviewer;

import com.tibagni.logviewer.filter.Filter;
import com.tibagni.logviewer.log.LogEntry;
import com.tibagni.logviewer.log.LogStream;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LogViewer {

  public enum UserSelection {
    CONFIRMED,
    REJECTED,
    CANCELLED
  }

  static UserSelection convertFromSwing(int swingOption) {
    switch (swingOption) {
      case JOptionPane.YES_OPTION: return UserSelection.CONFIRMED;
      case JOptionPane.NO_OPTION: return UserSelection.REJECTED;
      case JOptionPane.CANCEL_OPTION:
      case JOptionPane.CLOSED_OPTION: return UserSelection.CANCELLED;
    }

    throw new IllegalArgumentException("Invalid option: " + swingOption);
  }

  public interface View extends AsyncPresenter.AsyncView {
    void configureFiltersList(Map<String, List<Filter>> filters);
    void showErrorMessage(String message);
    void showLogs(LogEntry[] logEntries);
    void showCurrentLogsLocation(String logsPath);
    void showFilteredLogs(LogEntry[] logEntries);
    void showAvailableLogStreams(Set<LogStream> logStreams);
    void showUnsavedFilterIndication(String group);
    void hideUnsavedFilterIndication(String group);
    UserSelection showAskToSaveFilterDialog(String group);
    File showSaveFilters(String group);
    void finish();
    void showNavigationNextOver();
    void showNavigationPrevOver();
  }

  public interface Presenter {
    void init();
    void addFilter(String group, Filter newFilter);
    String addGroup(String group);
    List<String> getGroups();
    void removeFilters(String group, int[] indices);
    void reorderFilters(String group, int orig, int dest);
    int getNextFilteredLogForFilter(Filter filter, int firstLogIndexSearch);
    int getPrevFilteredLogForFilter(Filter filter, int firstLogIndexSearch);
    void saveFilters(String group);
    void loadFilters(File[] filtersFile);
    void loadLogs(File[] logFiles);
    void refreshLogs();
    void applyFilters();
    void filterEdited(Filter filter);

    void setStreamAllowed(LogStream stream, boolean allowed);
    boolean isStreamAllowed(LogStream stream);

    void finishing();
  }
}
