package com.tibagni.logviewer;

import com.tibagni.logviewer.filter.ContextInfo;
import com.tibagni.logviewer.filter.Filter;
import com.tibagni.logviewer.filter.FilterContextTag;
import com.tibagni.logviewer.filter.Filters;
import com.tibagni.logviewer.log.LogEntry;
import com.tibagni.logviewer.log.LogStream;
import com.tibagni.logviewer.log.LogTimestamp;
import com.tibagni.logviewer.logger.Logger;
import com.tibagni.logviewer.preferences.LogViewerPreferences;
import com.tibagni.logviewer.util.StringUtils;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import com.tibagni.logviewer.log.LogLineInfo;
import com.tibagni.logviewer.log.LogLineParser;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.tibagni.logviewer.logger.ProfilerKt.wrapProfiler;

public class LogViewerPresenterImpl extends AsyncPresenter implements LogViewerPresenter, FiltersRepository.Listener {
  public static final FilterContextTag FILTER_CONTEXT_TAG = FilterContextTag.register("logs");

  private final LogViewerPresenterView view;
  private final List<LogEntry> filteredLogs;
  private final List<LogEntry> cachedAllowedFilteredLogs;

  private final Map<LogStream, Boolean> allowedStreamsMap;
  private final LogViewerPreferences userPrefs;

  private final LogsRepository logsRepository;
  private final MyLogsRepository myLogsRepository;
  private final FiltersRepository filtersRepository;

  LogViewerPresenterImpl(LogViewerPresenterView view,
                         LogViewerPreferences userPrefs,
                         LogsRepository logsRepository,
                         MyLogsRepository myLogsRepository,
                         FiltersRepository filtersRepository) {
    super(view);
    this.view = view;
    this.userPrefs = userPrefs;
    this.logsRepository = logsRepository;
    this.myLogsRepository = myLogsRepository;
    this.filtersRepository = filtersRepository;

    cachedAllowedFilteredLogs = new ArrayList<>();
    filteredLogs = new ArrayList<>();
    allowedStreamsMap = new HashMap<>();
  }

  @Override
  public FilterContextTag getFilterContextTag() {
    return FILTER_CONTEXT_TAG;
  }

  @Override
  public void init() {
    filtersRepository.addListener(this);
  }

  @Override
  public void onFiltersApplied(@NotNull List<Filter> appliedFilters) {
    applyFilters();
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
  public void  loadLogs(File[] logFiles) {
    loadLogs(logFiles, StandardCharsets.UTF_8);
  }

  @Override
  public void  loadLogs(File[] logFiles, Charset charset) {
    // Clean up the filters info as it does not apply anymore
    cleanUpFilterTempInfo();
    doAsync(() -> {
      try {
        logsRepository.openLogFiles(logFiles, charset, this::updateAsyncProgress);
        rebuildLogStreamsMap(logsRepository.getAvailableStreams());
        filteredLogs.clear();
        cachedAllowedFilteredLogs.clear();
        cachedAllowedFilteredLogs.addAll(excludeNonAllowedStreams(filteredLogs));

        List<String> skippedLogs = logsRepository.getLastSkippedLogFiles();
        Map<String, String> bugReports = logsRepository.getPotentialBugReports();
        final boolean myLogsChanged = updateMyLogs();
        doOnUiThread(() -> {
          view.showFilteredLogs(cachedAllowedFilteredLogs);
          view.showLogs(logsRepository.getCurrentlyOpenedLogs());
          view.showAvailableLogStreams(allowedStreamsMap.keySet());
          // In case something changed with 'My Logs', make sure to update the UI as well
          if (myLogsChanged) {
            view.showMyLogs(myLogsRepository.getLogs());
          }

          if (logsRepository.getCurrentlyOpenedLogs().size() > 0) {
            String logsPath = FilenameUtils.getFullPath(logFiles[0].getAbsolutePath());
            view.showCurrentLogsLocation(logsPath);
            long appliedFiltersCount = filtersRepository.getAppliedFilters().size();
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
      }
    });
  }

  @Override
  public void refreshLogsWithDifferentCharset(Charset charset) {
    if (logsRepository.getCurrentlyOpenedLogFiles().isEmpty()) {
      view.showErrorMessage("No logs currently open");
    } else {
      loadLogs(logsRepository.getCurrentlyOpenedLogFiles().toArray(new File[0]), charset);
    }
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
  public void closeLogs() {
    if (logsRepository.getCurrentlyOpenedLogs().isEmpty()) {
      return;
    }

    cleanUpFilterTempInfo();
    cleanUpFilterInfoFromLogEntries();
    logsRepository.closeLogs();
    allowedStreamsMap.clear();
    filteredLogs.clear();
    cachedAllowedFilteredLogs.clear();
    myLogsRepository.reset(Collections.emptyList());

    view.showFilteredLogs(cachedAllowedFilteredLogs);
    view.showLogs(logsRepository.getCurrentlyOpenedLogs());
    view.showAvailableLogStreams(allowedStreamsMap.keySet());
    view.showMyLogs(myLogsRepository.getLogs());
    view.showCurrentLogsLocation(null);
    view.closeCurrentlyOpenedBugReports();
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

  private void applyFilters() {
    testStats.applyFiltersCallCount++;
    if (logsRepository.getCurrentlyOpenedLogs().isEmpty()) {
      doOnUiThread(() -> view.showFilteredLogs(cachedAllowedFilteredLogs));
      return;
    }

    // Before applying a new filter, make sure the last one is cleaned up
    // (if there is an existing one)
    cleanUpFilterInfoFromLogEntries();
    cleanUpFilterTempInfo();

    List<Filter> toApply = filtersRepository.getAppliedFilters();
    doAsync(() -> {
      filteredLogs.clear();
      filteredLogs.addAll(Filters.applyMultipleFilters(
          logsRepository.getCurrentlyOpenedLogs(), toApply.toArray(new Filter[0]), FILTER_CONTEXT_TAG, this::updateAsyncProgress));
      cachedAllowedFilteredLogs.clear();
      cachedAllowedFilteredLogs.addAll(excludeNonAllowedStreams(filteredLogs));
      updateFiltersContextInfo();
      doOnUiThread(() -> view.showFilteredLogs(cachedAllowedFilteredLogs));
    });
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
      ContextInfo filterTemporaryInfo = filter.getContext(FILTER_CONTEXT_TAG);
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

  @Override
  public void addLogEntriesToMyLogs(List<LogEntry> entries) {
    myLogsRepository.addLogEntries(entries);
    view.showMyLogs(myLogsRepository.getLogs());
  }

  @Override
  public void removeFromMyLog(int[] indices) {
    List<LogEntry> toRemove = new ArrayList<>();
    for (int i : indices) {
      int myLogsSize = myLogsRepository.getLogs().size();
      if (i < 0 || i >= myLogsSize) {
        Logger.warning("Trying to remove invalid index " + i + " from MyLogs. Current size: " + myLogsSize);
      } else {
        toRemove.add(myLogsRepository.getLogs().get(i));
      }
    }
    myLogsRepository.removeLogEntries(toRemove);
    view.showMyLogs(myLogsRepository.getLogs());
  }

  boolean updateMyLogs() {
    // We only want to update logs in 'My Logs' if the new logs that are being loaded are different
    // So we check if the logs under 'My Logs' are still matching the logs that are open. If not, clear
    // This is to avoid clearing 'My Logs' when the user is simply refreshing the logs (F5)
    if (myLogsRepository.getLogs().isEmpty()) {
      return false;
    }

    boolean mismatch = false;
    for (LogEntry myLogEntry : myLogsRepository.getLogs()) {
      // If myLog's index is greater than the number of new logs, it is a mismatch
      if (myLogEntry.getIndex() > logsRepository.getLastVisibleLogIndex()) {
        mismatch = true;
        break;
      }

      // If the myLog no longer matches the same index in the new logs, it is a mismatch
      List<LogEntry> currentLogs = logsRepository.getCurrentlyOpenedLogs();
      if (myLogEntry.getIndex() >= 0 && myLogEntry.getIndex() < currentLogs.size() &&
          !myLogEntry.equals(currentLogs.get(myLogEntry.getIndex()))) {
        mismatch = true;
        break;
      }
    }

    if (mismatch) {
      // It is a mismatch, the logs under 'My Logs' could still match the opened logs in a different position.
      // For example if the user opened the same log file again, but in addition with some other logs.
      // To cover this case, make sure all logs in 'MyLogs' have a match in the opened logs. And if so, update the
      // indices of the 'My Logs'
      List<LogEntry> matchedLogs = new ArrayList<>();

      for (LogEntry myLogEntry : myLogsRepository.getLogs()) {
        LogEntry matchedLogEntry = logsRepository.getMatchingLogEntry(myLogEntry);

        if (matchedLogEntry != null) {
          // Match found, update the 'My Logs' entry
          matchedLogs.add(matchedLogEntry);
        }
      }

      // Now, we always want to update the log entries in 'MyLogs' to have the correct indices so, remove all and
      // reinsert the ones that have a match
      myLogsRepository.reset(matchedLogs);
      return true;
    }

    return false;
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
    view.finish();
    release();
  }

  private void cleanUpFilterInfoFromLogEntries() {
    if (filteredLogs != null) {
      for (LogEntry entry : filteredLogs) {
        entry.setAppliedFilter(null);
      }
    }
  }

  private void cleanUpFilterTempInfo() {
    forEachFilter(filter -> filter.resetContext(FILTER_CONTEXT_TAG));
  }

  private void forEachFilter(Consumer<Filter> consumer) {
    for (Map.Entry<String, List<Filter>> entry : filtersRepository.getCurrentlyOpenedFilters().entrySet()) {
      List<Filter> filtersFromGroup = entry.getValue();
      for (Filter f : filtersFromGroup) {
        consumer.accept(f);
      }
    }
  }

  @Override
  public LogLineInfo getLogLineInfo(LogEntry entry) {
    return LogLineParser.parseLineInfo(entry);
  }

  // Test helpers
  static class Stats {
    int applyFiltersCallCount;
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
}
