package com.tibagni.logviewer;

import com.tibagni.logviewer.filter.Filter;
import com.tibagni.logviewer.log.LogEntry;
import com.tibagni.logviewer.log.LogStream;

import javax.swing.*;
import java.io.File;
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
    void configureFiltersList(Filter[] filters);
    void showErrorMessage(String message);
    void showLogs(LogEntry[] logEntries);
    void showCurrentLogsLocation(String logsPath);
    void showFilteredLogs(LogEntry[] logEntries);
    void showAvailableLogStreams(Set<LogStream> logStreams);
    void showUnsavedTitle();
    void hideUnsavedTitle();
    UserSelection showAskToSaveFilterDialog();
    void showSaveFilter();
    void finish();
    void showNavigationNextOver();
    void showNavigationPrevOver();
  }

  public interface Presenter {
    void init();
    void addFilter(Filter newFilter);
    void removeFilters(int[] indices);
    void reorderFilters(int orig, int dest);
    int getNextFilteredLogForFilter(int filterIndex, int firstLogIndexSearch);
    int getPrevFilteredLogForFilter(int filterIndex, int firstLogIndexSearch);
    void saveFilters(File filterFile);
    void loadFilters(File filtersFile);
    void loadLogs(File[] logFiles);
    void applyFilters(int[] filterIndices);
    void filterEdited();

    void finishing();
  }
}
