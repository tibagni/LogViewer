package com.tibagni.logviewer.filter;

import com.tibagni.logviewer.ProgressReporter;
import com.tibagni.logviewer.log.LogEntry;

import java.util.*;

public class Filters {

  private static class Progress {
    public final long totalLogs;
    public final long publishThreshold;
    public long logsRead;
    public long logsReadOnProgressPublish;

    public Progress(long totalLogs) {
      this.totalLogs = totalLogs;
      this.publishThreshold = totalLogs / 10;
    }
  }

  public static LogEntry[] applyMultipleFilters(LogEntry[] input, Filter[] filters, ProgressReporter pr) {
    initializeContextInfo(filters);
    // This algorithm is O(n*m), but we can assume the 'filters' array will only contain a few elements
    // So, in practice, this will be much closer to O(n) than O(nˆ2)
    List<LogEntry> filtered = new Vector<>();
    final Progress progress = new Progress(input.length);
    Arrays.stream(input).parallel().forEach(entry -> {
      Filter appliedFilter = getAppliedFilter(entry, filters);
      if (appliedFilter != null) {
        entry.setFilterColor(appliedFilter.getColor());
        entry.setMatchedText(appliedFilter.getPatternString());
        filtered.add(entry);
      }

      // This is called A LOT of times, so we try to publish progress update only after a given threshold
      // to not impact on performance. We don't care about thread synchronization either as it is not
      // that important that the progress is completely accurate (since there will be so many iterations
      // here it will not actually make a difference and the progress will be accurate). We only care about
      // impacting the least possible in performance here
      progress.logsRead++;
      if (progress.logsRead > (progress.logsReadOnProgressPublish + progress.publishThreshold)
              || progress.logsRead >= progress.totalLogs ) {
        progress.logsReadOnProgressPublish = progress.logsRead;
        pr.onProgress((int)progress.logsRead * 100 / input.length, "Applying filters...");
      }
    });
    Collections.sort(filtered);

    pr.onProgress(100, "Done!");
    return filtered.toArray(new LogEntry[0]);
  }

  private static void initializeContextInfo(Filter[] filters) {
    for (Filter filter : filters) {
      filter.initTemporaryInfo();
    }
  }

  private static Filter getAppliedFilter(LogEntry entry, Filter[] filters) {
    String inputLine = entry.getLogText();
    Filter firstFound = null;
    for (Filter filter : filters) {
      if (filter.appliesTo(inputLine)) {
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
