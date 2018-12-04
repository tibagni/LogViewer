package com.tibagni.logviewer;

import com.tibagni.logviewer.filter.Filter;
import com.tibagni.logviewer.filter.FilterException;
import com.tibagni.logviewer.filter.Filters;
import com.tibagni.logviewer.log.FileLogReader;
import com.tibagni.logviewer.log.LogEntry;
import com.tibagni.logviewer.log.LogReaderException;
import com.tibagni.logviewer.log.LogStream;
import com.tibagni.logviewer.log.parser.LogParser;
import com.tibagni.logviewer.log.parser.LogParserException;
import com.tibagni.logviewer.preferences.LogViewerPreferences;
import com.tibagni.logviewer.util.StringUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class LogViewerPresenter extends AsyncPresenter implements LogViewer.Presenter {
  private final LogViewer.View view;

  private File[] currentlyOpenedLogFiles;
  private final List<Filter> filters;
  private LogEntry[] allLogs;
  private LogEntry[] filteredLogs;
  private LogEntry[] cachedAllowedFilteredLogs;
  private LogParser logParser;

  private boolean hasUnsavedFilterChanges;
  private List<String> currentlyOpenedFilters;

  private Map<LogStream, Boolean> availableStreams = new HashMap<>();

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

      if (userPrefs.shouldReapplyFiltersAfterEdit() &&
          allLogs != null && allLogs.length > 0) {
        // Make sure to add the new filter to the 'applied' list
        // so it gets applied now (We always add to the end, so
        // just add the last index as well)
        newFilter.setApplied(true);
        applyFilters();
      }
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
    // we need to navigate on the logs that are being shown on the UI,
    // so use 'cachedAllowedFilteredLogs' here
    if (filterIndex < 0 || cachedAllowedFilteredLogs.length == 0) {
      return -1;
    }

    if (firstLogIndexSearch < 0) {
      firstLogIndexSearch = -1;
    }

    if (firstLogIndexSearch >= cachedAllowedFilteredLogs.length) {
      firstLogIndexSearch = cachedAllowedFilteredLogs.length - 1;
    }

    Filter filter = filters.get(filterIndex);
    int startSearch = firstLogIndexSearch + 1;
    int endSearch = startSearch + cachedAllowedFilteredLogs.length;

    for (int i = startSearch; i <= endSearch; i++) {
      int index = i % cachedAllowedFilteredLogs.length;
      if (filter.appliesTo(cachedAllowedFilteredLogs[index].getLogText())) {
        if (index < firstLogIndexSearch) {
          view.showNavigationNextOver();
        }
        return index;
      }
    }

    return -1;
  }

  @Override
  public int getPrevFilteredLogForFilter(int filterIndex, int firstLogIndexSearch) {
    // we need to navigate on the logs that are being shown on the UI,
    // so use 'cachedAllowedFilteredLogs' here
    if (filterIndex < 0 || cachedAllowedFilteredLogs.length == 0) {
      return -1;
    }

    if (firstLogIndexSearch < 0) {
      firstLogIndexSearch = -1;
    }

    if (firstLogIndexSearch >= cachedAllowedFilteredLogs.length) {
      firstLogIndexSearch = cachedAllowedFilteredLogs.length - 1;
    }

    Filter filter = filters.get(filterIndex);
    int startSearch = firstLogIndexSearch < 0 ? firstLogIndexSearch : firstLogIndexSearch - 1;
    int endSearch = startSearch - cachedAllowedFilteredLogs.length;

    for (int i = startSearch; i >= endSearch; i--) {
      int index = i >= 0 ? i : (cachedAllowedFilteredLogs.length + i);
      if (filter.appliesTo(cachedAllowedFilteredLogs[index].getLogText())) {
        if (index > firstLogIndexSearch && firstLogIndexSearch >= 0) {
          view.showNavigationPrevOver();
        }
        return index;
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

    // Clean up the filters info as it does not apply anymore
    cleanUpFilterTempInfo();
    doAsync(() -> {
      try {
        // After loading new logs, clear the filtered logs as well as it is no longer valid
        allLogs = logParser.parseLogs();
        availableStreams = buildLogStreamsMap(logParser.getAvailableStreams());
        filteredLogs = new LogEntry[0];
        cachedAllowedFilteredLogs = excludeNonAllowedStreams(filteredLogs);

        logParser.release();
        logParser = null;

        doOnUiThread(() -> {
          view.showFilteredLogs(cachedAllowedFilteredLogs);
          view.showLogs(allLogs);
          view.showAvailableLogStreams(availableStreams.keySet());

          if (allLogs.length > 0) {
            String logsPath = FilenameUtils.getFullPath(logFiles[0].getPath());
            view.showCurrentLogsLocation(logsPath);

            long appliedFiltersCount = filters.stream().filter(f -> f.isApplied()).count();
            if (appliedFiltersCount > 0) {
              applyFilters();
            }

            currentlyOpenedLogFiles = Arrays.copyOf(logFiles, logFiles.length);
          } else {
            view.showCurrentLogsLocation(null);
            view.showErrorMessage("No logs found");
            currentlyOpenedLogFiles = null;
          }
        });
      } catch (LogReaderException | LogParserException e) {
        doOnUiThread(() -> view.showErrorMessage(e.getMessage()));
        currentlyOpenedLogFiles = null;
      }
    });
  }

  @Override
  public void refreshLogs() {
    if (currentlyOpenedLogFiles != null && currentlyOpenedLogFiles.length > 0) {
      loadLogs(currentlyOpenedLogFiles);
    } else {
      view.showErrorMessage("No logs to be refreshed");
    }
  }

  private Map<LogStream, Boolean> buildLogStreamsMap(Set<LogStream> availableStreams) {
    Map<LogStream, Boolean> streamsMap = new HashMap<>();
    for (LogStream s : availableStreams) {
      streamsMap.put(s, true);
    }

    return streamsMap;
  }

  @Override
  public void applyFilters() {
    if (allLogs == null || allLogs.length == 0) {
      //view.showErrorMessage("There are no logs to filter...");
      return;
    }

    // Before applying a new filter, make sure the last one is cleaned up
    // (if there is an existing one)
    cleanUpFilteredColors();
    cleanUpFilterTempInfo();

    List<Filter> toApply = filters.stream().filter(f -> f.isApplied()).collect(Collectors.toList());

    doAsync(() -> {
      filteredLogs = Filters.applyMultipleFilters(allLogs, toApply.toArray(new Filter[0]), this::updateAsyncProgress);
      cachedAllowedFilteredLogs = excludeNonAllowedStreams(filteredLogs);
      updateFiltersContextInfo();
      doOnUiThread(() -> view.showFilteredLogs(cachedAllowedFilteredLogs));
    });
  }

  @Override
  public void filterEdited(Filter filter) {
    checkForUnsavedChanges();

    if (userPrefs.shouldReapplyFiltersAfterEdit() &&
        allLogs != null && allLogs.length > 0) {
      // Make sure the edited filter will also be re-applied.
      // If it was not previously applied, apply now
      filter.setApplied(true);
      applyFilters();
    }
  }

  @Override
  public void setStreamAllowed(LogStream stream, boolean allowed) {
    if (availableStreams == null || availableStreams.isEmpty() ||
        !availableStreams.containsKey(stream)) {
      throw new IllegalStateException("Stream " + stream + " is not available");
    }

    availableStreams.put(stream, allowed);
    updateFiltersContextInfo();
    cachedAllowedFilteredLogs = excludeNonAllowedStreams(filteredLogs);
    view.showFilteredLogs(cachedAllowedFilteredLogs);
  }

  private void updateFiltersContextInfo() {
    Set<LogStream> allowedStreams = new HashSet<>();
    for (Map.Entry<LogStream, Boolean> entry : availableStreams.entrySet()) {
      if (entry.getValue()) {
        allowedStreams.add(entry.getKey());
      }
    }

    for (Filter filter : filters) {
      Filter.ContextInfo filterTemporaryInfo = filter.getTemporaryInfo();
      if (filterTemporaryInfo != null) {
        filterTemporaryInfo.setAllowedStreams(allowedStreams);
      }
    }
  }

  @Override
  public boolean isStreamAllowed(LogStream stream) {
    if (availableStreams == null || availableStreams.isEmpty()) {
      throw new IllegalStateException("There are no streams available");
    }

    if (!availableStreams.containsKey(stream)) {
      throw new IllegalStateException("Stream " + stream + " is not available");
    }

    return availableStreams.get(stream);
  }

  private LogEntry[] excludeNonAllowedStreams(LogEntry[] entries) {
    if (availableStreams == null || availableStreams.isEmpty()) {
      // If there is no stream restriction just work with all entries
      return entries;
    }

    ArrayList<LogEntry> result = new ArrayList<>();
    Set<LogStream> allowedStreams = new HashSet<>();
    for (Map.Entry<LogStream, Boolean> entry : availableStreams.entrySet()) {
      if (entry.getValue()) {
        allowedStreams.add(entry.getKey());
      }
    }

    for (LogEntry entry : entries) {
      if (allowedStreams.contains(entry.getStream())) {
        result.add(entry);
      }
    }

    return result.toArray(new LogEntry[0]);
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

  // Test helpers
  void setFilteredLogsForTesting(LogEntry[] filteredLogs) {
    this.filteredLogs = filteredLogs;
  }
  void setFiltersForTesting(List<Filter> filters) {
    this.filters.addAll(filters);
  }
  void setAvailableStreamsForTesting(Set<LogStream> streams, boolean initiallyAllowed) {
    for (LogStream stream : streams) {
      availableStreams.put(stream, initiallyAllowed);
    }
  }
  void setAvailableStreamsForTesting(Set<LogStream> streams) {
    setAvailableStreamsForTesting(streams, false);
  }
}
