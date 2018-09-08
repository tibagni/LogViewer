package com.tibagni.logviewer;

import com.tibagni.logviewer.filter.Filter;
import com.tibagni.logviewer.log.LogEntry;

import java.io.File;

public class LogViewer {

  public interface View extends AsyncPresenter.AsyncView {
    void configureFiltersList(Filter[] filters);
    void showErrorMessage(String message);
    void showLogs(LogEntry[] logEntries);
    void showFilteredLogs(LogEntry[] logEntries);
    void showUnsavedTitle();
    void hideUnsavedTitle();
    void showAskToSaveFilterDialog();
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
