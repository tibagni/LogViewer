package com.tibagni.logviewer.filter;

import com.tibagni.logviewer.ProgressReporter;
import com.tibagni.logviewer.i18n.I18n;
import com.tibagni.logviewer.log.LogEntry;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Filters {

  public static List<LogEntry> applyMultipleFilters(List<LogEntry> input, Filter[] filters, ProgressReporter pr) {
    initializeContextInfo(filters);
    final int totalLogs = input.size();
    if (totalLogs == 0 || filters.length == 0) {
      pr.onProgress(100, I18n.get(I18n.COMMON_DONE));
      return Collections.emptyList();
    }

    final int numCores = Math.max(1, Runtime.getRuntime().availableProcessors());
    final int targetChunkSize = Math.max(50, Math.min(10_000, totalLogs / (numCores * 2)));

    List<List<LogEntry>> chunks = new ArrayList<>();
    for (int i = 0; i < totalLogs; i += targetChunkSize) {
      chunks.add(input.subList(i, Math.min(i + targetChunkSize, totalLogs)));
    }

    final AtomicInteger completedLogs = new AtomicInteger(0);
    final int publishThreshold = Math.max(1, totalLogs / 10);
    final AtomicInteger lastReported = new AtomicInteger(0);

    List<List<LogEntry>> chunkResults = chunks.parallelStream().map(chunk ->
      applyFiltersToChunk(chunk, filters, completedLogs, lastReported, publishThreshold, totalLogs, pr)
    ).collect(Collectors.toList());

    int totalFilteredSize = 0;
    for (List<LogEntry> chunkResult : chunkResults) {
      totalFilteredSize += chunkResult.size();
    }

    // The input list is already sorted chronologically. Since we partitioned the input into
    // contiguous ordered chunks, each chunk maintained its internal order, and
    // parallelStream().map().collect() preserves encounter order, chunkResults is an ordered
    // list of ordered slices. Concatenating them sequentially produces a 100% sorted result
    // in O(N) time with fast System.arraycopy, completely eliminating Collections.sort().
    List<LogEntry> filtered = new ArrayList<>(totalFilteredSize);
    for (List<LogEntry> chunkResult : chunkResults) {
      filtered.addAll(chunkResult);
    }

    pr.onProgress(100, I18n.get(I18n.COMMON_DONE));
    return filtered;
  }

  /**
   * Processes a chunk of log entries to determine which match the provided filters.
   *
   * @param chunk            the chunk of log entries to process.
   * @param filters          the array of filters to apply.
   * @param completedLogs    Atomic counter for tracking total processed logs.
   * @param lastReported     Atomic counter for tracking the last progress report threshold.
   * @param publishThreshold the threshold for reporting progress.
   * @param totalLogs        the total count of logs for progress calculation.
   * @param pr               the progress reporter.
   * @return a list of filtered entries from the chunk.
   */
  private static List<LogEntry> applyFiltersToChunk(
      List<LogEntry> chunk,
      Filter[] filters,
      AtomicInteger completedLogs,
      AtomicInteger lastReported,
      int publishThreshold,
      int totalLogs,
      ProgressReporter pr) {
    List<LogEntry> chunkResult = new ArrayList<>();
    for (LogEntry entry : chunk) {
      Filter appliedFilter = getAppliedFilter(entry, filters);
      if (appliedFilter != null) {
        entry.setAppliedFilter(appliedFilter);
        chunkResult.add(entry);
      }
    }

    // We publish progress updates only after a given threshold to not impact performance.
    // Progress is tracked per chunk rather than per log line to minimize synchronization overhead.
    int completed = completedLogs.addAndGet(chunk.size());
    int last = lastReported.get();
    if (completed - last >= publishThreshold || completed >= totalLogs) {
      if (lastReported.compareAndSet(last, completed)) {
        pr.onProgress(completed * 100 / totalLogs, I18n.get(I18n.FILTERS_PROGRESS_APPLYING));
      }
    }

    return chunkResult;
  }

  private static void initializeContextInfo(Filter[] filters) {
    for (Filter filter : filters) {
      filter.initTemporaryInfo();
    }
  }

  /**
   * Identifies the primary matching filter for the given log entry and increments line counters
   * across all matching filters.
   *
   * @param entry   the log entry being evaluated.
   * @param filters the array of candidate filters.
   * @return the first filter that matched the log entry, or null if no filters matched.
   */
  private static Filter getAppliedFilter(LogEntry entry, Filter[] filters) {
    Filter firstFound = null;
    for (Filter filter : filters) {
      if (filter.appliesTo(entry)) {
        if (firstFound == null) {
          firstFound = filter;
        }

        // Increment the filter's 'linesFound' so we can show to the user
        // how many times each filter has matched
        filter.getTemporaryInfo().incrementLineCount(entry.getStream());
      }
    }

    return firstFound;
  }
}
