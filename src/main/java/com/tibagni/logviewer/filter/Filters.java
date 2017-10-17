package com.tibagni.logviewer.filter;

import com.tibagni.logviewer.log.LogEntry;

import java.util.ArrayList;
import java.util.List;

public class Filters {

  public static LogEntry[] applyMultipleFilters(LogEntry[] input, Filter[] filters) {
    // This algorithm is O(n*m), but we can assume the 'filters' array will only contain a few elements
    // So, in practice, this will be much closer to O(n) than O(nˆ2)
    List<LogEntry> filtered = new ArrayList<>();
    for (LogEntry entry : input) {
      Filter appliedFilter = getAppliedFilter(entry.getLogText(), filters);
      if (appliedFilter != null) {
        entry.setFilterColor(appliedFilter.getColor());
        filtered.add(entry);
      }
    }

    return filtered.toArray(new LogEntry[0]);
  }

  private static Filter getAppliedFilter(String inputLine, Filter[] filters) {
    for (Filter filter : filters) {
      if (filter.appliesTo(inputLine)) {
        return filter;
      }
    }

    return null;
  }
}
