package com.tibagni.logviewer;

import com.tibagni.logviewer.filter.Filter;
import com.tibagni.logviewer.filter.FilterException;
import com.tibagni.logviewer.filter.Filters;
import com.tibagni.logviewer.log.FileLogReader;
import com.tibagni.logviewer.log.LogEntry;
import com.tibagni.logviewer.log.LogReaderException;
import com.tibagni.logviewer.log.parser.LogParser;
import com.tibagni.logviewer.log.parser.LogParserException;
import com.tibagni.logviewer.preferences.LogViewerPreferences;
import com.tibagni.logviewer.util.StringUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LogViewerPresenter extends AsyncPresenter implements LogViewer.Presenter {
  private final LogViewer.View view;

  private final List<Filter> filters;
  private LogEntry[] allLogs;
  private LogEntry[] filteredLogs;
  private LogParser logParser;

  private boolean hasUnsavedFilterChanges;
  private List<String> currentlyOpenedFilters;

  private final LogViewerPreferences userPrefs;

  public LogViewerPresenter(LogViewer.View view) {
    this(view, LogViewerPreferences.getInstance());
  }

  // Visible for testing
  LogViewerPresenter(LogViewer.View view, LogViewerPreferences userPrefs) {
    super(view);
    this.view = view;
    filters = new ArrayList<>();
    currentlyOpenedFilters = new ArrayList<>();

    this.userPrefs = userPrefs;
  }

  @Override
  public void init() {
    // Check if we need to open the last opened filter
    if (userPrefs.shouldOpenLastFilter()) {
      File lastFilter = userPrefs.getLastFilterPath();
      if (lastFilter != null) {
        loadFilters(lastFilter);
      }
    }
  }

  private void setFilters(List<Filter> newFilters) {
    filters.clear();
    filters.addAll(newFilters);
    view.configureFiltersList(filters.toArray(new Filter[0]));
  }

  @Override
  public void addFilter(Filter newFilter) {
    if (newFilter != null) {
      filters.add(newFilter);
      view.configureFiltersList(filters.toArray(new Filter[0]));
      checkForUnsavedChanges();
    }
  }

  @Override
  public void removeFilters(int[] indices) {
    // Iterate backwards otherwise the indices will change
    // and we will end up deleting wrong items
    for (int i = indices.length - 1; i >= 0; i--) {
      filters.remove(indices[i]);
    }

    view.configureFiltersList(filters.toArray(new Filter[0]));

    // Do not mark as unsaved changes if all filters were removed.
    // User will not save an empty filter set
    if (filters.size() > 0) {
      checkForUnsavedChanges();
    }
  }

  @Override
  public void reorderFilters(int orig, int dest) {
    if (orig == dest) return;

    int destIndex = dest > orig ? (dest - 1) : dest;
    Filter filter = filters.remove(orig);
    filters.add(destIndex, filter);
    view.configureFiltersList(filters.toArray(new Filter[0]));
    checkForUnsavedChanges();
  }

  @Override
  public int getNextFilteredLogForFilter(int filterIndex, int firstLogIndexSearch) {
    if (filterIndex < 0) {
      return -1;
    }

    if (firstLogIndexSearch < 0) {
      firstLogIndexSearch = -1;
    }

    if (firstLogIndexSearch >= filteredLogs.length) {
      firstLogIndexSearch = filteredLogs.length - 1;
    }

    Filter filter = filters.get(filterIndex);
    for (int i = firstLogIndexSearch + 1; i < filteredLogs.length; i++) {
      if (filter.appliesTo(filteredLogs[i].getLogText())) {
        return i;
      }
    }

    return -1;
  }

  @Override
  public int getPrevFilteredLogForFilter(int filterIndex, int firstLogIndexSearch) {
    if (filterIndex < 0) {
      return -1;
    }

    if (firstLogIndexSearch < 0) {
      firstLogIndexSearch = -1;
    }

    if (firstLogIndexSearch >= filteredLogs.length) {
      firstLogIndexSearch = filteredLogs.length - 1;
    }

    Filter filter = filters.get(filterIndex);
    for (int i = firstLogIndexSearch - 1; i >= 0; i--) {
      if (filter.appliesTo(filteredLogs[i].getLogText())) {
        return i;
      }
    }

    return -1;
  }

  @Override
  public void saveFilters(File filterFile) {
    try {
      boolean firstLoop = true;
      BufferedWriter fileWriter = new BufferedWriter(new FileWriter(filterFile));
      List<String> serializedFilters = getSerializedFilters();
      for (String serializedFilter : serializedFilters) {
        if (firstLoop) {
          firstLoop = false;
        } else {
          fileWriter.newLine();
        }

        fileWriter.write(serializedFilter);
      }
      fileWriter.close();

      // Now that we saved the filters, make it the currently opened filters
      currentlyOpenedFilters.clear();
      currentlyOpenedFilters.addAll(serializedFilters);

      // Call checkForUnsavedChanges to clear the 'unsaved changes' state
      checkForUnsavedChanges();
    } catch (IOException e) {
      view.showErrorMessage(e.getMessage());
    }
  }

  private List<String> getSerializedFilters() {
    return filters.stream().map(Filter::serializeFilter).
        collect(Collectors.toList());
  }

  @Override
  public void loadFilters(File filtersFile) {
    BufferedReader bufferedReader = null;
    currentlyOpenedFilters.clear();
    try {
      bufferedReader = new BufferedReader(new FileReader(filtersFile));
      String line;
      List<Filter> filtersFromFile = new ArrayList<>();
      while ((line = bufferedReader.readLine()) != null && line.trim().length() > 0) {
        filtersFromFile.add(Filter.createFromString(line));
        currentlyOpenedFilters.add(line);
      }
      setFilters(filtersFromFile);

      // Call checkForUnsavedChanges to clear the 'unsaved changes' state
      checkForUnsavedChanges();

      // Set this as the last filter opened
      userPrefs.setLastFilterPath(filtersFile);
    } catch (FilterException | IOException e) {
      view.showErrorMessage(e.getMessage());
    } finally {
      if (bufferedReader != null) {
        try {
          bufferedReader.close();
        } catch (IOException ignore) { }
      }
    }
  }

  @Override
  public void loadLogs(File[] logFiles) {
    logParser = new LogParser(new FileLogReader(logFiles), this::updateAsyncProgress);

    doAsync(() -> {
      try {
        // After loading new logs, clear the filtered logs as well as it is no longer valid
        allLogs = logParser.parseLogs();
        filteredLogs = new LogEntry[0];

        logParser.release();
        logParser = null;

        doOnUiThread(() -> {
          if (allLogs.length > 0) {
            view.showFilteredLogs(filteredLogs);
            view.showLogs(allLogs);

            String logsPath = FilenameUtils.getPath(logFiles[0].getPath());
            view.showCurrentLogsLocation(logsPath);
          } else {
            view.showErrorMessage("No logs found");
          }
        });
      } catch (LogReaderException | LogParserException e) {
        doOnUiThread(() -> view.showErrorMessage(e.getMessage()));
      }
    });

    // Clean up the filters info as it does not apply anymore
    cleanUpFilterTempInfo();
  }

  @Override
  public void applyFilters(int[] filterIndices) {
    if (allLogs == null || allLogs.length == 0) {
      view.showErrorMessage("There are no logs to filter...");
      return;
    }

    // Before applying a new filter, make sure the last one is cleaned up
    // (if there is an existing one)
    cleanUpFilteredColors();
    cleanUpFilterTempInfo();

    Filter[] filters = new Filter[filterIndices.length];
    int i = 0;
    for (int filterIndex : filterIndices) {
      filters[i++] = this.filters.get(filterIndex);
    }

    doAsync(() -> {
      filteredLogs = Filters.applyMultipleFilters(allLogs, filters, this::updateAsyncProgress);
      doOnUiThread(() -> view.showFilteredLogs(filteredLogs));
    });

  }

  @Override
  public void filterEdited() {
    checkForUnsavedChanges();
  }

  @Override
  public void finishing() {
    boolean shouldFinish = true;
    if (hasUnsavedFilterChanges) {
      LogViewer.UserSelection userSelection = view.showAskToSaveFilterDialog();

      if (userSelection == LogViewer.UserSelection.CONFIRMED) {
        view.showSaveFilter();
      }

      // Do not close window if user has cancelled or closed the
      // finishing dialog
      shouldFinish = (userSelection !=
          LogViewer.UserSelection.CANCELLED);
    }

    if (shouldFinish) {
      view.finish();
      release();
    }
  }

  private void cleanUpFilteredColors() {
    if (filteredLogs != null) {
      for (LogEntry entry : filteredLogs) {
        entry.setFilterColor(null);
      }
    }
  }

  private void cleanUpFilterTempInfo() {
    for (Filter filter : filters) {
      filter.resetTemporaryInfo();
    }
  }

  private void checkForUnsavedChanges() {
    boolean filtersChanged = haveFiltersChanged();

    if (hasUnsavedFilterChanges != filtersChanged) {
      hasUnsavedFilterChanges = filtersChanged;

      if (hasUnsavedFilterChanges) {
        view.showUnsavedTitle();
      } else {
        view.hideUnsavedTitle();
      }
    }
  }

  private boolean haveFiltersChanged() {
    // Here we check if the current filters are different from
    // the filters saved in disk (Which we have cached in memory,
    // stored in 'currentlyOpenedFilters' to make this method efficient)
    List<String> serializedFilters = getSerializedFilters();
    if (serializedFilters.size() > 0 &&
        serializedFilters.size() != currentlyOpenedFilters.size()) {
      return true;
    }

    for (int i = 0; i < serializedFilters.size(); i++) {
      if (!StringUtils.areEquals(serializedFilters.get(i),
          currentlyOpenedFilters.get(i))) {
        return true;
      }
    }

    return false;
  }
}
