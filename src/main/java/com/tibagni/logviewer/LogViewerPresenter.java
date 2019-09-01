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
import com.tibagni.logviewer.preferences.LogViewerPreferencesImpl;
import com.tibagni.logviewer.util.StringUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class LogViewerPresenter extends AsyncPresenter implements LogViewer.Presenter {
  private final LogViewer.View view;

  private File[] currentlyOpenedLogFiles;
  private final Map<String, List<Filter>> filters;
  private final Map<String, File> filtersFilesMap;
  private final Map<String, List<String>> currentlyOpenedFilters;
  private LogEntry[] allLogs;
  private LogEntry[] filteredLogs;
  private LogEntry[] cachedAllowedFilteredLogs;
  private LogParser logParser;
  private final List<String> unsavedFilterGroups;
  private Map<LogStream, Boolean> availableStreams = new HashMap<>();
  private final LogViewerPreferences userPrefs;

  public LogViewerPresenter(LogViewer.View view) {
    this(view, LogViewerPreferencesImpl.INSTANCE);
  }

  // Visible for testing
  LogViewerPresenter(LogViewer.View view, LogViewerPreferences userPrefs) {
    super(view);
    this.view = view;
    filters = new HashMap<>();
    filtersFilesMap = new HashMap<>();
    currentlyOpenedFilters = new HashMap<>();
    unsavedFilterGroups = new ArrayList<>();

    this.userPrefs = userPrefs;
  }

  @Override
  public void init() {
    // Check if we need to open the last opened filter
    if (userPrefs.getOpenLastFilter()) {
      File[] lastFilters = userPrefs.getLastFilterPaths();
      if (lastFilters != null && lastFilters.length > 0) {
        loadFilters(lastFilters);
      }
    }
  }

  private void setFilters(Map<String, List<Filter>> newFilters) {
    filters.clear();
    filters.putAll(newFilters);
    view.configureFiltersList(filters);
  }

  @Override
  public void addFilter(String group, Filter newFilter) {
    if (!StringUtils.isEmpty(group) && newFilter != null) {
      if (filters.containsKey(group)) {
        List<Filter> filtersFromGroup = filters.get(group);
        filtersFromGroup.add(newFilter);
      } else {
        List<Filter> filtersFromGroup = new ArrayList<>();
        filtersFromGroup.add(newFilter);
        filters.put(group, filtersFromGroup);
      }

      view.configureFiltersList(filters);
      checkForUnsavedChanges();

      if (userPrefs.getReapplyFiltersAfterEdit()) {
        // Make sure to add the new filter to the 'applied' list
        // so it gets applied now (We always add to the end, so
        // just add the last index as well)
        newFilter.setApplied(true);
        applyFilters();
      }
    }
  }

  @Override
  public String addGroup(String group) {
    if (!StringUtils.isEmpty(group)) {
      int n = 1;
      String groupName = group;
      while(filters.containsKey(groupName)) {
        groupName = group + n++;
      }

      filters.put(groupName, new ArrayList<>());
      view.configureFiltersList(filters);
      return groupName;
    }

    return null;
  }

  @Override
  public List<String> getGroups() {
    return new ArrayList<>(filters.keySet());
  }

  @Override
  public void removeFilters(String group, int[] indices) {
    boolean shouldReapply = false;

    // Iterate backwards otherwise the indices will change
    // and we will end up deleting wrong items
    List<Filter> filtersFromGroup = filters.get(group);
    for (int i = indices.length - 1; i >= 0; i--) {
      Filter removedFilter = filtersFromGroup.remove(indices[i]);
      shouldReapply |= removedFilter.isApplied();
    }

    view.configureFiltersList(filters);

    // Do not mark as unsaved changes if all filters were removed.
    // User will not save an empty filter set
    if (filters.size() > 0) {
      checkForUnsavedChanges();
    }

    // Only re-apply the filters if the at least one of the removed filters
    // were applied
    if (shouldReapply) {
      applyFilters();
    }
  }

  @Override
  public void removeGroup(String group) {
    if (!StringUtils.isEmpty(group)) {
      boolean unsavedChange = unsavedFilterGroups.contains(group);

      if (unsavedChange) {
        LogViewer.UserSelection userSelection = view.showAskToSaveFilterDialog(group);
        if (userSelection == LogViewer.UserSelection.CONFIRMED) {
          saveFilters(group);
        }
      }

      List<Filter> removed = filters.remove(group);
      if (removed != null) {
        view.configureFiltersList(filters);
        checkForUnsavedChanges();

        boolean shouldReapply = removed.stream().anyMatch(f -> f.isApplied());
        if (shouldReapply) {
          applyFilters();
        }
      }
    }
  }

  @Override
  public void reorderFilters(String group, int orig, int dest) {
    if (orig == dest) return;

    int destIndex = dest > orig ? (dest - 1) : dest;
    List<Filter> filtersFromGroup = filters.get(group);
    Filter filter = filtersFromGroup.remove(orig);
    filtersFromGroup.add(destIndex, filter);
    view.configureFiltersList(filters);
    checkForUnsavedChanges();
  }

  @Override
  public int getNextFilteredLogForFilter(Filter filter, int firstLogIndexSearch) {
    // we need to navigate on the logs that are being shown on the UI,
    // so use 'cachedAllowedFilteredLogs' here
    if (cachedAllowedFilteredLogs == null || cachedAllowedFilteredLogs.length == 0) {
      return -1;
    }

    if (firstLogIndexSearch < 0) {
      firstLogIndexSearch = -1;
    }

    if (firstLogIndexSearch >= cachedAllowedFilteredLogs.length) {
      firstLogIndexSearch = cachedAllowedFilteredLogs.length - 1;
    }

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
  public int getPrevFilteredLogForFilter(Filter filter, int firstLogIndexSearch) {
    // we need to navigate on the logs that are being shown on the UI,
    // so use 'cachedAllowedFilteredLogs' here
    if (cachedAllowedFilteredLogs == null || cachedAllowedFilteredLogs.length == 0) {
      return -1;
    }

    if (firstLogIndexSearch < 0) {
      firstLogIndexSearch = -1;
    }

    if (firstLogIndexSearch >= cachedAllowedFilteredLogs.length) {
      firstLogIndexSearch = cachedAllowedFilteredLogs.length - 1;
    }

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
  public void saveFilters(String group) {
    File saveFile = filtersFilesMap.get(group);
    if (saveFile == null) {
      saveFile = view.showSaveFilters(group);
    }

    if (saveFile != null) {
      saveFilters(saveFile, group);
    }
  }

  public void saveFilters(File filterFile, String group) {
    try {
      boolean firstLoop = true;
      BufferedWriter fileWriter = new BufferedWriter(new FileWriter(filterFile));
      List<String> serializedFilters = getSerializedFilters(group);
      for (String serializedFilter : serializedFilters) {
        if (firstLoop) {
          firstLoop = false;
        } else {
          fileWriter.newLine();
        }

        fileWriter.write(serializedFilter);
      }
      fileWriter.close();

      filtersFilesMap.put(group, filterFile);
      // Now that we saved the filters, make it the currently opened filters
      currentlyOpenedFilters.remove(group);
      currentlyOpenedFilters.put(group, serializedFilters);

      // Call checkForUnsavedChanges to clear the 'unsaved changes' state
      checkForUnsavedChanges();
    } catch (IOException e) {
      view.showErrorMessage(e.getMessage());
    }
  }

  private List<String> getSerializedFilters(String group) {
    List<Filter> filtersFromGroup = filters.get(group);
    if (filtersFromGroup == null) {
      return Collections.emptyList();
    }

    return filtersFromGroup.stream().map(Filter::serializeFilter).
        collect(Collectors.toList());
  }

  @Override
  public void loadFilters(File[] filtersFiles) {
    List<File> successfulOpenedFiles = new ArrayList<>();
    Map<String, List<Filter>> filtersFromFiles = new HashMap<>();

    if (userPrefs.getRememberAppliedFilters()) {
      // First remember which filters are applied for the current files
      // So the next time these files are opened, we can re-apply the same filters
      rememberAppliedFilters();
    }

    currentlyOpenedFilters.clear();
    filtersFilesMap.clear();
    for (File file : filtersFiles) {
      BufferedReader bufferedReader = null;
      String group = file.getName();

      try {
        bufferedReader = new BufferedReader(new FileReader(file));
        String line;
        List<Filter> filters = new ArrayList<>();
        List<String> serializedFilters = new ArrayList<>();
        while ((line = bufferedReader.readLine()) != null && line.trim().length() > 0) {
          filters.add(Filter.createFromString(line));
          serializedFilters.add(line);
        }
        currentlyOpenedFilters.put(group, serializedFilters);
        filtersFilesMap.put(group, file);

        filtersFromFiles.put(group, filters);
        successfulOpenedFiles.add(file);
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
    setFilters(filtersFromFiles);
    // Call checkForUnsavedChanges to clear the 'unsaved changes' state
    checkForUnsavedChanges();

    // Set this as the last filter opened
    userPrefs.setLastFilterPaths(successfulOpenedFiles.toArray(new File[0]));
    if (userPrefs.getRememberAppliedFilters()) {
      reapplyRememberedFilters();
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

            long appliedFiltersCount = getFiltersThat(filter -> filter.isApplied()).size();
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
    testStats.applyFiltersCallCount++;

    if (allLogs == null || allLogs.length == 0) {
      //view.showErrorMessage("There are no logs to filter...");
      return;
    }

    // Before applying a new filter, make sure the last one is cleaned up
    // (if there is an existing one)
    cleanUpFilteredColors();
    cleanUpFilterTempInfo();

    List<Filter> toApply = getFiltersThat(filter -> filter.isApplied());
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

    if (userPrefs.getReapplyFiltersAfterEdit() &&
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

    forEachFilter(filter -> {
      Filter.ContextInfo filterTemporaryInfo = filter.getTemporaryInfo();
      if (filterTemporaryInfo != null) {
        filterTemporaryInfo.setAllowedStreams(allowedStreams);
      }
    });
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
    // 'unsavedFilterGroups' can be changed while we are iterating over it
    // create an array with its elements to be safe instead
    String[] unsavedGroups = unsavedFilterGroups.toArray(new String[0]);

    for (String unsavedGroup : unsavedGroups) {
      LogViewer.UserSelection userSelection = view.showAskToSaveFilterDialog(unsavedGroup);
      if (userSelection == LogViewer.UserSelection.CONFIRMED) {
        saveFilters(unsavedGroup);
      } else if (userSelection == LogViewer.UserSelection.CANCELLED) {
        // Cancel all
        shouldFinish = false;
        break;
      }
    }

    if (shouldFinish) {
      if (userPrefs.getRememberAppliedFilters()) {
        // Remember which filters are applied for the current files, so the next
        // time these files are opened, we can re-apply the same filters
        rememberAppliedFilters();
      }

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
    forEachFilter(filter -> filter.resetTemporaryInfo());
  }

  private void checkForUnsavedChanges() {
    unsavedFilterGroups.clear();
    for (String group : filters.keySet()) {
      if (haveFiltersChanged(group)) {
        view.showUnsavedFilterIndication(group);
        unsavedFilterGroups.add(group);
      } else {
        view.hideUnsavedFilterIndication(group);
      }
    }
  }

  private boolean haveFiltersChanged(String group) {
    // Here we check if the current filters are different from
    // the filters saved in disk (Which we have cached in memory,
    // stored in 'currentlyOpenedFilters' to make this method efficient)
    List<String> serializedFilters = getSerializedFilters(group);
    List<String> currentFilters = currentlyOpenedFilters.get(group);
    if (serializedFilters.size() > 0 && currentFilters == null) {
      return true;
    }

    if (serializedFilters.size() > 0 &&
        serializedFilters.size() != currentFilters.size()) {
      return true;
    }

    for (int i = 0; i < serializedFilters.size(); i++) {
      if (!StringUtils.areEquals(serializedFilters.get(i),
          currentFilters.get(i))) {
        return true;
      }
    }

    return false;
  }

  private void forEachFilter(Consumer<Filter> consumer) {
    for (Map.Entry<String, List<Filter>> entry : filters.entrySet()) {
      List<Filter> filtersFromGroup = entry.getValue();
      for (Filter f : filtersFromGroup) {
        consumer.accept(f);
      }
    }
  }

  private List<Filter> getFiltersThat(Predicate<Filter> condition) {
    List<Filter> resultFilters = new ArrayList<>();
    for (Map.Entry<String, List<Filter>> entry : filters.entrySet()) {
      List<Filter> filtersFromGroup = entry.getValue();
      for (Filter f : filtersFromGroup) {
        if (condition.test(f)) {
          resultFilters.add(f);
        }
      }
    }

    return resultFilters;
  }

  private void rememberAppliedFilters() {
    for (Map.Entry<String, List<Filter>> entry : filters.entrySet()) {
      List<Filter> filtersFromGroup = entry.getValue();
      List<Integer> appliedIndices = new ArrayList<>();
      for (int i = 0; i < filtersFromGroup.size(); i++) {
        if (filtersFromGroup.get(i).isApplied()) {
          appliedIndices.add(i);
        }
        userPrefs.setAppliedFiltersIndices(entry.getKey(), appliedIndices);
      }
    }
  }

  private void reapplyRememberedFilters() {
    for (Map.Entry<String, List<Filter>> entry : filters.entrySet()) {
      List<Integer> appliedIndices = userPrefs.getAppliedFiltersIndices(entry.getKey());

      if (!appliedIndices.isEmpty()) {
        List<Filter> filtersFromGroup = entry.getValue();
        for (int i = 0; i < filtersFromGroup.size(); i++) {
          if (appliedIndices.contains(i)) {
            filtersFromGroup.get(i).setApplied(true);
          }
        }
      }
    }

    applyFilters();
  }

  // Test helpers
  static class Stats {
    int applyFiltersCallCount;
  }
  private Stats testStats = new Stats();
  Stats getTestStats() {
    return testStats;
  }


  void setFilteredLogsForTesting(LogEntry[] filteredLogs) {
    this.filteredLogs = filteredLogs;
  }
  void setFiltersForTesting(List<Filter> filters) {
    this.filters.put("Test", filters);
  }
  void setFiltersForTesting(String group, List<Filter> filters) {
    this.filters.put(group, filters);
  }
  void setAvailableStreamsForTesting(Set<LogStream> streams, boolean initiallyAllowed) {
    for (LogStream stream : streams) {
      availableStreams.put(stream, initiallyAllowed);
    }
  }
  void setAvailableStreamsForTesting(Set<LogStream> streams) {
    setAvailableStreamsForTesting(streams, false);
  }
  void addFilterForTests(String group, Filter newFilter) {
    addFilter(group, newFilter);
  }
  void setUnsavedGroupForTesting(String group) {
    unsavedFilterGroups.add(group);
  }
}
