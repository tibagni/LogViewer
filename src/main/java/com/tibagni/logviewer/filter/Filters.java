package com.tibagni.logviewer.filter;

import com.tibagni.logviewer.ProgressReporter;
import com.tibagni.logviewer.log.LogEntry;

import java.util.ArrayList;
import java.util.List;

public class Filters {

  public static LogEntry[] applyMultipleFilters(LogEntry[] input, Filter[] filters, ProgressReporter pr) {
    initializeContextInfo(filters);
    // This algorithm is O(n*m), but we can assume the 'filters' array will only contain a few elements
    // So, in practice, this will be much closer to O(n) than O(nˆ2)
    List<LogEntry> filtered = new ArrayList<>();
    int logsRead = 0;
    for (LogEntry entry : input) {
      pr.onProgress(logsRead++ * 100 / input.length, "Applying filters...");
      Filter appliedFilter = getAppliedFilter(entry.getLogText(), filters);
      if (appliedFilter != null) {
        entry.setFilterColor(appliedFilter.getColor());
        filtered.add(entry);
      }
    }

    pr.onProgress(100, "Done!");
    return filtered.toArray(new LogEntry[0]);
  }

  private static void initializeContextInfo(Filter[] filters) {
    for (Filter filter : filters) {
      filter.initTemporaryInfo();
    }
  }

  private static Filter getAppliedFilter(String inputLine, Filter[] filters) {
    Filter firstFound = null;
    for (Filter filter : filters) {
      if (filter.appliesTo(inputLine)) {
        if (firstFound == null) {
          firstFound = filter;
        }

        // Increment the filter's 'linesFound' so we can show to the user
        // how many times each filter has matched
        filter.getTemporaryInfo().linesFound++;
      }
    }

    return firstFound;
  }
}
