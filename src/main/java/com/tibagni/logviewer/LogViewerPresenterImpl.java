package com.tibagni.logviewer;

import com.tibagni.logviewer.filter.Filter;
import com.tibagni.logviewer.filter.Filters;
import com.tibagni.logviewer.log.LogEntry;
import com.tibagni.logviewer.log.LogStream;
import com.tibagni.logviewer.log.LogTimestamp;
import com.tibagni.logviewer.logger.Logger;
import com.tibagni.logviewer.preferences.LogViewerPreferences;
import com.tibagni.logviewer.util.StringUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.tibagni.logviewer.logger.ProfilerKt.wrapProfiler;

public class LogViewerPresenterImpl extends AsyncPresenter implements LogViewerPresenter {
  private final LogViewerPresenterView view;

  private final List<LogEntry> filteredLogs;
  private final List<LogEntry> cachedAllowedFilteredLogs;

  private final List<String> unsavedFilterGroups;
  private final Map<LogStream, Boolean> allowedStreamsMap;
  private final LogViewerPreferences userPrefs;

  private final LogsRepository logsRepository;
  private final FiltersRepository filtersRepository;

  LogViewerPresenterImpl(LogViewerPresenterView view,
                         LogViewerPreferences userPrefs,
                         LogsRepository logsRepository,
                         FiltersRepository filtersRepository) {
    super(view);
    this.view = view;
    this.userPrefs = userPrefs;
    this.logsRepository = logsRepository;
    this.filtersRepository = filtersRepository;

    unsavedFilterGroups = new ArrayList<>();
    cachedAllowedFilteredLogs = new ArrayList<>();
    filteredLogs = new ArrayList<>();
    allowedStreamsMap = new HashMap<>();
  }

  @Override
  public void init() {
    // Check if we need to open the last opened filter
    if (userPrefs.getOpenLastFilter()) {
      File[] lastFilters = userPrefs.getLastFilterPaths();
      if (lastFilters.length > 0) {
        loadFilters(lastFilters, false);
      }
    }

    // Check if we need to collapse all groups on startup
    if (userPrefs.getCollapseAllGroupsStartup()) {
      view.collapseAllGroups();
    }
  }

  @Override
  public void addFilter(String group, Filter newFilter) {
    addFilter(group, newFilter, false);
  }

  @Override
  public void addFilter(String group, Filter newFilter, boolean ignoreReapply) {
    if (!StringUtils.isEmpty(group) && newFilter != null) {
      filtersRepository.addFilter(group, newFilter);
      view.configureFiltersList(filtersRepository.getCurrentlyOpenedFilters());
      checkForUnsavedChanges();

      if (!ignoreReapply && userPrefs.getReapplyFiltersAfterEdit()) {
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
      String addedGroup = filtersRepository.addGroup(group);
      view.configureFiltersList(filtersRepository.getCurrentlyOpenedFilters());
      return addedGroup;
    }

    return null;
  }

  @Override
  public List<String> getGroups() {
    return new ArrayList<>(filtersRepository.getCurrentlyOpenedFilters().keySet());
  }

  @Override
  public void removeFilters(String group, int[] indices) {
    List<Filter> deletedFilters = filtersRepository.deleteFilters(group, indices);
    boolean shouldReapply = deletedFilters.stream().anyMatch(Filter::isApplied);
    view.configureFiltersList(filtersRepository.getCurrentlyOpenedFilters());

    // Do not mark as unsaved changes if all filters were removed.
    // User will not save an empty filter set
    if (!filtersRepository.getCurrentlyOpenedFilters().isEmpty()) {
      checkForUnsavedChanges();
    }

    // Only re-apply the filters if the at least one of the removed filters was applied
    if (shouldReapply) {
      applyFilters();
    }
  }

  @Override
  public void moveFilters(String origGroup, String destGroup, int[] indices) {
    if (StringUtils.areEquals(origGroup, destGroup)) {
      return;
    }

    List<Filter> movingFilters = filtersRepository.deleteFilters(origGroup, indices);
    filtersRepository.addFilters(destGroup, movingFilters);

    boolean shouldReapply = movingFilters.stream().anyMatch(Filter::isApplied);
    view.configureFiltersList(filtersRepository.getCurrentlyOpenedFilters());
    checkForUnsavedChanges();

    // Only re-apply the filters if the at least one of the moved filters was applied
    if (shouldReapply) {
      applyFilters();
    }
  }

  @Override
  public void removeGroup(String group) {
    if (!StringUtils.isEmpty(group)) {
      boolean unsavedChange = unsavedFilterGroups.contains(group);

      if (unsavedChange) {
        UserSelection userSelection = view.showAskToSaveFilterDialog(group);
        if (userSelection == UserSelection.CONFIRMED) {
          saveFilters(group);
        } else if (userSelection == UserSelection.CANCELLED) {
          // User canceled. Abort!
          return;
        }
      }

      List<Filter> filtersFromGroup = filtersRepository.getCurrentlyOpenedFilters().get(group);
      File groupFile = filtersRepository.getCurrentlyOpenedFilterFiles().get(group);
      if (filtersFromGroup != null) {
        filtersRepository.deleteGroup(group);
        view.configureFiltersList(filtersRepository.getCurrentlyOpenedFilters());
        checkForUnsavedChanges();

        boolean shouldReapply = filtersFromGroup.stream().anyMatch(Filter::isApplied);
        if (shouldReapply) {
          applyFilters();
        }
      }

      // Remove this group from the last filters path config
      if (groupFile != null) {
        File[] currentSavedPaths = userPrefs.getLastFilterPaths();
        if (currentSavedPaths.length > 0) {
          int i = ArrayUtils.indexOf(currentSavedPaths, groupFile);
          if (i >= 0) {
            currentSavedPaths = ArrayUtils.remove(currentSavedPaths, i);
            userPrefs.setLastFilterPaths(currentSavedPaths);
          }
        }
      }
    }
  }

  @Override
  public void reorderFilters(String group, int orig, int dest) {
    if (orig == dest) return;

    filtersRepository.reorderFilters(group, orig, dest);
    view.configureFiltersList(filtersRepository.getCurrentlyOpenedFilters());
    checkForUnsavedChanges();
  }

  @Override
  public int getNextFilteredLogForFilter(Filter filter, int firstLogIndexSearch) {
    // we need to navigate on the logs that are being shown on the UI,
    // so use 'cachedAllowedFilteredLogs' here
    if (cachedAllowedFilteredLogs.isEmpty()) {
      return -1;
    }

    if (firstLogIndexSearch < 0) {
      firstLogIndexSearch = -1;
    }

    if (firstLogIndexSearch >= cachedAllowedFilteredLogs.size()) {
      firstLogIndexSearch = cachedAllowedFilteredLogs.size() - 1;
    }

    int startSearch = firstLogIndexSearch + 1;
    int endSearch = startSearch + cachedAllowedFilteredLogs.size();

    for (int i = startSearch; i <= endSearch; i++) {
      int index = i % cachedAllowedFilteredLogs.size();
      if (filter.appliesTo(cachedAllowedFilteredLogs.get(index))) {
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
    if (cachedAllowedFilteredLogs.isEmpty()) {
      return -1;
    }

    if (firstLogIndexSearch < 0) {
      firstLogIndexSearch = -1;
    }

    if (firstLogIndexSearch >= cachedAllowedFilteredLogs.size()) {
      firstLogIndexSearch = cachedAllowedFilteredLogs.size() - 1;
    }

    int startSearch = firstLogIndexSearch < 0 ? firstLogIndexSearch : firstLogIndexSearch - 1;
    int endSearch = startSearch - cachedAllowedFilteredLogs.size();

    for (int i = startSearch; i >= endSearch; i--) {
      int index = i >= 0 ? i : (cachedAllowedFilteredLogs.size() + i);
      if (filter.appliesTo(cachedAllowedFilteredLogs.get(index))) {
        if (index > firstLogIndexSearch && firstLogIndexSearch >= 0) {
          view.showNavigationPrevOver();
        }
        return index;
      }
    }

    return -1;
  }

  @Override
  public void goToTimestamp(String timestamp) {
    try {
      String[] timestampParts = timestamp.split(" ");
      String[] date = timestampParts[0].split("-");
      String[] time = timestampParts.length > 1 ?
          // Allow the user to use ':' or '.' for time portion. In case it is a copy of what is in the logcat
          timestampParts[1].replaceAll("\\.", ":").split(":") :
          new String[]{};

      if (date.length != 2) throw new IllegalArgumentException("Invalid date!");

      int month = Integer.parseInt(date[0]);
      int day = Integer.parseInt(date[1]);

      // It does not matter if the timestamp is not complete as it should work with
      // approximate values. So, don't enforce it.
      int hour = (time.length > 0) ? Integer.parseInt(time[0]) : 0;
      int min = (time.length > 1) ? Integer.parseInt(time[1]) : 0;
      int sec = (time.length > 2) ? Integer.parseInt(time[2]) : 0;
      int hund = (time.length > 3) ? Integer.parseInt(time[3]) : 0;

      LogTimestamp searchTimestamp = new LogTimestamp(month, day, hour, min, sec, hund);
      Logger.info("Going to timestamp: " + searchTimestamp);

      int unfilteredLogIndex = wrapProfiler(
          "findClosestLogIndexByTimestamp-AllLogs",
          () -> findClosestLogIndexByTimestamp(searchTimestamp, logsRepository.getCurrentlyOpenedLogs())
      );
      int filteredLogIndex = wrapProfiler(
          "findClosestLogIndexByTimestamp-FilteredLogs",
          () -> findClosestLogIndexByTimestamp(searchTimestamp, cachedAllowedFilteredLogs)
      );

      view.showLogLocationAtSearchedTimestamp(unfilteredLogIndex, filteredLogIndex);
    } catch (Exception e) {
      Logger.error("Failed to parse timestamp: " + timestamp, e);
      view.showInvalidTimestampSearchError(timestamp);
    }
  }

  private int findClosestLogIndexByTimestamp(@NotNull LogTimestamp timestamp, List<LogEntry> logList) {
    if (logList == null || logList.isEmpty()) {
      return -1;
    }

    int index = -1;
    for (LogEntry entry : logList) {
      index++;
      if (timestamp.compareTo(entry.timestamp) == 0) {
        break;
      } else if (timestamp.compareTo(entry.timestamp) < 0) {
        // We want the log line before
        if (index > 0) index--;
        break;
      }
    }

    return index;
  }

  @Override
  public void saveFilters(String group) {
    File saveFile = filtersRepository.getCurrentlyOpenedFilterFiles().get(group);
    if (saveFile == null) {
      saveFile = view.showSaveFilters(group);
    }

    if (saveFile != null) {
      saveFilters(saveFile, group);
    }
  }

  private void saveFilters(File filterFile, String group) {
    try {
      filtersRepository.persistGroup(filterFile, group);
      // Call checkForUnsavedChanges to clear the 'unsaved changes' state
      checkForUnsavedChanges();
    } catch (PersistFiltersException e) {
      view.showErrorMessage(e.getMessage());
    }
  }

  @Override
  public void loadFilters(File[] filtersFiles, boolean keepCurrentFilters) {
    if (userPrefs.getRememberAppliedFilters()) {
      // First remember which filters are applied for the current files
      // So the next time these files are opened, we can re-apply the same filters
      rememberAppliedFilters();
    }

    if (!keepCurrentFilters) {
      // Do not keep current filters, clear everything before loading the new ones
      if (!filtersRepository.getCurrentlyOpenedFilters().isEmpty()) {
        boolean shouldAbort = !requestSaveUnsavedGroups();
        if (shouldAbort) {
          return;
        }
      }
      filtersRepository.closeAllFilters();
    }

    try {
      filtersRepository.openFilterFiles(filtersFiles);
    } catch (OpenFiltersException e) {
      view.showErrorMessage(e.getMessage());
    }

    Map<String, List<Filter>> currentlyOpenedFilters = filtersRepository.getCurrentlyOpenedFilters();
    // Check if we need to re-save the recently opened filters (in case they were converted to the new format)
    for (File filterFile : filtersFiles) {
      String group = filterFile.getName();
      List<Filter> filters = currentlyOpenedFilters.getOrDefault(group, new ArrayList<>());
      boolean isLegacy = filters.stream().anyMatch(filter -> filter.wasLoadedFromLegacyFile);
      if (isLegacy) {
        Logger.info("Filter Group: " + group + " is using old file format. Re-save it");
        // Now that we checked, clear the legacy flag
        filters.stream().forEach(filter -> filter.wasLoadedFromLegacyFile = false);
        // ... and save it
        saveFilters(group);
      }
    }

    view.configureFiltersList(currentlyOpenedFilters);
    // Call checkForUnsavedChanges to clear the 'unsaved changes' state
    checkForUnsavedChanges();

    // Set all the currently opened filters as the latest
    File[] lastFilterPaths = filtersRepository.getCurrentlyOpenedFilterFiles().values().toArray(new File[0]);
    userPrefs.setLastFilterPaths(lastFilterPaths);
    if (userPrefs.getRememberAppliedFilters()) {
      reapplyRememberedFilters();
      view.onAppliedFiltersRemembered();
    }
  }

  @Override
  public void loadLogs(File[] logFiles) {
    // Clean up the filters info as it does not apply anymore
    cleanUpFilterTempInfo();
    doAsync(() -> {
      try {
        logsRepository.openLogFiles(logFiles, this::updateAsyncProgress);
        rebuildLogStreamsMap(logsRepository.getAvailableStreams());
        filteredLogs.clear();
        cachedAllowedFilteredLogs.clear();
        cachedAllowedFilteredLogs.addAll(excludeNonAllowedStreams(filteredLogs));

        List<String> skippedLogs = logsRepository.getLastSkippedLogFiles();
        Map<String, String> bugReports = logsRepository.getPotentialBugReports();
        doOnUiThread(() -> {
          view.showFilteredLogs(cachedAllowedFilteredLogs);
          view.showLogs(logsRepository.getCurrentlyOpenedLogs());
          view.showAvailableLogStreams(allowedStreamsMap.keySet());

          if (!logsRepository.getCurrentlyOpenedLogs().isEmpty()) {
            String logsPath = FilenameUtils.getFullPath(logFiles[0].getAbsolutePath());
            view.showCurrentLogsLocation(logsPath);
            long appliedFiltersCount = getFiltersThat(Filter::isApplied).size();
            if (appliedFiltersCount > 0) {
              applyFilters();
            }

            // Only show this skipped logs if there are other logs loaded
            // otherwise the "No logs found" is enough
            if (!skippedLogs.isEmpty()) {
              // Some logs were not parsed, let the UI know which
              view.showSkippedLogsMessage(skippedLogs);
            }

            if (bugReports.isEmpty()) {
              view.closeCurrentlyOpenedBugReports();
            } else {
              // We only support opening one bugreport for now
              Map.Entry<String, String> entry = bugReports.entrySet().iterator().next();
              view.showOpenPotentialBugReport(entry.getKey(), entry.getValue());
            }
          } else {
            view.showCurrentLogsLocation(null);
            view.showErrorMessage("No logs found");
          }
        });
      } catch (OpenLogsException e) {
        doOnUiThread(() -> view.showErrorMessage(e.getMessage()));
      } finally {
        System.gc();
        System.runFinalization();
      }
    });
  }

  @Override
  public void refreshLogs() {
    if (logsRepository.getCurrentlyOpenedLogFiles().isEmpty()) {
      view.showErrorMessage("No logs to be refreshed");
    } else {
      loadLogs(logsRepository.getCurrentlyOpenedLogFiles().toArray(new File[0]));
    }
  }

  @Override
  public void saveFilteredLogs(File file) {
    if (filteredLogs.isEmpty()) {
      return;
    }

    try {
      BufferedWriter fileWriter = new BufferedWriter(new FileWriter(file));
      for (LogEntry entry : filteredLogs) {
        fileWriter.write(entry.toString());
        fileWriter.newLine();
      }
      fileWriter.close();
    } catch (IOException e) {
      view.showErrorMessage(e.getMessage());
    }
  }

  private void rebuildLogStreamsMap(Set<LogStream> availableStreams) {
    this.allowedStreamsMap.clear();
    for (LogStream s : availableStreams) {
      this.allowedStreamsMap.put(s, true);
    }
  }

  @Override
  public void applyFilters() {
    testStats.applyFiltersCallCount++;
    if (logsRepository.getCurrentlyOpenedLogs().isEmpty()) {
      doOnUiThread(() -> view.showFilteredLogs(cachedAllowedFilteredLogs));
      return;
    }

    // Before applying a new filter, make sure the last one is cleaned up
    // (if there is an existing one)
    cleanUpFilterInfoFromLogEntries();
    cleanUpFilterTempInfo();

    List<Filter> toApply = getFiltersThat(Filter::isApplied);
    doAsync(() -> {
      filteredLogs.clear();
      filteredLogs.addAll(Filters.applyMultipleFilters(
          logsRepository.getCurrentlyOpenedLogs(), toApply.toArray(new Filter[0]), this::updateAsyncProgress));
      cachedAllowedFilteredLogs.clear();
      cachedAllowedFilteredLogs.addAll(excludeNonAllowedStreams(filteredLogs));
      updateFiltersContextInfo();
      doOnUiThread(() -> view.showFilteredLogs(cachedAllowedFilteredLogs));
    });
  }

  @Override
  public void filterEdited(Filter filter) {
    checkForUnsavedChanges();

    if (userPrefs.getReapplyFiltersAfterEdit()) {
      // Make sure the edited filter will also be re-applied.
      // If it was not previously applied, apply now
      filter.setApplied(true);
      applyFilters();
    }
  }

  @Override
  public void setAllFiltersApplied(String group, boolean isApplied) {
    forEachFilterInGroup(group, filter -> filter.setApplied(isApplied));
    applyFilters();
  }

  @Override
  public void setAllFiltersApplied(boolean isApplied) {
    forEachFilter(filter -> filter.setApplied(isApplied));
    applyFilters();
  }

  @Override
  public void setStreamAllowed(LogStream stream, boolean allowed) {
    if (allowedStreamsMap.isEmpty() || !allowedStreamsMap.containsKey(stream)) {
      throw new IllegalStateException("Stream " + stream + " is not available");
    }

    allowedStreamsMap.put(stream, allowed);
    updateFiltersContextInfo();
    cachedAllowedFilteredLogs.clear();
    cachedAllowedFilteredLogs.addAll(excludeNonAllowedStreams(filteredLogs));
    view.showFilteredLogs(cachedAllowedFilteredLogs);
  }

  private void updateFiltersContextInfo() {
    Set<LogStream> allowedStreams = new HashSet<>();
    for (Map.Entry<LogStream, Boolean> entry : allowedStreamsMap.entrySet()) {
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
    if (allowedStreamsMap.isEmpty()) {
      throw new IllegalStateException("There are no streams available");
    }

    if (!allowedStreamsMap.containsKey(stream)) {
      throw new IllegalStateException("Stream " + stream + " is not available");
    }

    return allowedStreamsMap.get(stream);
  }

  @Override
  public void ignoreLogsBefore(int index) {
    if (index == logsRepository.getFirstVisibleLogIndex()) {
      // Nothing to do, return early
      return;
    }

    if (index >= logsRepository.getLastVisibleLogIndex()) {
      // This would ignore all logs
      view.showErrorMessage("Cannot set first visible log after last visible log");
      return;
    }

    logsRepository.setFirstVisibleLogIndex(index);
    view.showLogs(logsRepository.getCurrentlyOpenedLogs());
    applyFilters();
  }

  @Override
  public void ignoreLogsAfter(int index) {
    if (index == logsRepository.getLastVisibleLogIndex()) {
      // Nothing to do, return early
      return;
    }

    if (index <= logsRepository.getFirstVisibleLogIndex()) {
      // This would ignore all logs
      view.showErrorMessage("Cannot set last visible log before first visible log");
      return;
    }

    logsRepository.setLastVisibleLogIndex(index);
    view.showLogs(logsRepository.getCurrentlyOpenedLogs());
    applyFilters();
  }

  @Override
  public void resetIgnoredLogs(boolean resetStarting, boolean resetEnding) {
    if (resetStarting) {
      logsRepository.setFirstVisibleLogIndex(-1);
    }
    if (resetEnding) {
      logsRepository.setLastVisibleLogIndex(-1);
    }

    view.showLogs(logsRepository.getCurrentlyOpenedLogs());
    applyFilters();
  }

  @Override
  public int getVisibleLogsOffset() {
    return logsRepository.getFirstVisibleLogIndex();
  }

  @Override
  public LogEntry getFirstVisibleLog() {
    int index = logsRepository.getFirstVisibleLogIndex();
    if (index <= 0) {
      // If the first visible log index is the first index, then there are no ignored logs,
      // so, just return null
      return null;
    }

    // "currently opened logs" already represents the visible logs, so just return the first one
    return logsRepository.getCurrentlyOpenedLogs().get(0);
  }

  @Override
  public LogEntry getLastVisibleLog() {
    int index = logsRepository.getLastVisibleLogIndex();
    int lastVisibleIndex = logsRepository.getCurrentlyOpenedLogs().size() - 1;
    if (index == (logsRepository.getAllLogsSize() - 1)) {
      // If the last visible log index is the last index, then there are no ignored logs,
      // so, just return null
      return null;
    }

    // "currently opened logs" already represents the visible logs, so just return the last one
    return logsRepository.getCurrentlyOpenedLogs().get(lastVisibleIndex);
  }

  private List<LogEntry> excludeNonAllowedStreams(List<LogEntry> entries) {
    if (allowedStreamsMap.isEmpty()) {
      // If there is no stream restriction just work with all entries
      return entries;
    }

    ArrayList<LogEntry> result = new ArrayList<>();
    Set<LogStream> allowedStreams = new HashSet<>();
    for (Map.Entry<LogStream, Boolean> entry : allowedStreamsMap.entrySet()) {
      if (entry.getValue()) {
        allowedStreams.add(entry.getKey());
      }
    }

    for (LogEntry entry : entries) {
      if (allowedStreams.contains(entry.getStream())) {
        result.add(entry);
      }
    }

    return result;
  }

  @Override
  public void finishing() {
    boolean shouldFinish = requestSaveUnsavedGroups();
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

  private boolean requestSaveUnsavedGroups() {
    // 'unsavedFilterGroups' can be changed while we are iterating over it
    // create an array with its elements to be safe instead
    String[] unsavedGroups = unsavedFilterGroups.toArray(new String[0]);
    if (unsavedGroups.length == 0) {
      // There is no unsaved groups, return early. don't bother showing anything to the user
      return true;
    }

    Boolean[] groupsSelection = view.showAskToSaveMultipleFiltersDialog(unsavedGroups);
    if (groupsSelection != null) {
      for (int i = 0; i < unsavedGroups.length; i++) {
        // Check each group if user selected to save
        if (groupsSelection[i]) {
          saveFilters(unsavedGroups[i]);
        }
      }
      return true;
    }

    return false;
  }

  private void cleanUpFilterInfoFromLogEntries() {
    if (filteredLogs != null) {
      for (LogEntry entry : filteredLogs) {
        entry.setAppliedFilter(null);
      }
    }
  }

  private void cleanUpFilterTempInfo() {
    forEachFilter(Filter::resetTemporaryInfo);
  }

  private void checkForUnsavedChanges() {
    unsavedFilterGroups.clear();
    List<String> changedGroups = filtersRepository.getChangedGroupsSinceLastOpened();
    Set<String> allGroups = filtersRepository.getCurrentlyOpenedFilters().keySet();

    for (String group : allGroups) {
      if (changedGroups.contains(group)) {
        view.showUnsavedFilterIndication(group);
        unsavedFilterGroups.add(group);
      } else {
        view.hideUnsavedFilterIndication(group);
      }
    }
  }

  private void forEachFilterInGroup(String group, Consumer<Filter> consumer) {
    List<Filter> filtersFromGroup = filtersRepository.getCurrentlyOpenedFilters().get(group);
    if (filtersFromGroup != null) {
      for (Filter f : filtersFromGroup) {
        consumer.accept(f);
      }
    }
  }

  private void forEachFilter(Consumer<Filter> consumer) {
    for (Map.Entry<String, List<Filter>> entry : filtersRepository.getCurrentlyOpenedFilters().entrySet()) {
      List<Filter> filtersFromGroup = entry.getValue();
      for (Filter f : filtersFromGroup) {
        consumer.accept(f);
      }
    }
  }

  private List<Filter> getFiltersThat(Predicate<Filter> condition) {
    List<Filter> resultFilters = new ArrayList<>();
    for (Map.Entry<String, List<Filter>> entry : filtersRepository.getCurrentlyOpenedFilters().entrySet()) {
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
    testStats.rememberAppliedFiltersCallCount++;
    for (Map.Entry<String, List<Filter>> entry : filtersRepository.getCurrentlyOpenedFilters().entrySet()) {
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
    testStats.reapplyRememberedFiltersCallCount++;
    for (Map.Entry<String, List<Filter>> entry : filtersRepository.getCurrentlyOpenedFilters().entrySet()) {
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
    int rememberAppliedFiltersCallCount;
    int reapplyRememberedFiltersCallCount;
  }

  private final Stats testStats = new Stats();

  Stats getTestStats() {
    return testStats;
  }

  void setFilteredLogsForTesting(LogEntry[] filteredLogs) {
    setFilteredLogsForTesting(filteredLogs, false);
  }

  void setFilteredLogsForTesting(LogEntry[] filteredLogs, boolean setCached) {
    this.filteredLogs.clear();
    this.filteredLogs.addAll(Arrays.asList(filteredLogs));
    if (setCached) {
      this.cachedAllowedFilteredLogs.clear();
      this.cachedAllowedFilteredLogs.addAll(Arrays.asList(filteredLogs));
    }
  }

  void setAvailableStreamsForTesting(Set<LogStream> streams, boolean initiallyAllowed) {
    for (LogStream stream : streams) {
      allowedStreamsMap.put(stream, initiallyAllowed);
    }
  }

  void setAvailableStreamsForTesting(Set<LogStream> streams) {
    setAvailableStreamsForTesting(streams, false);
  }

  void setUnsavedGroupForTesting(String group) {
    unsavedFilterGroups.add(group);
  }
}
