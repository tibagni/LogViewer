package com.tibagni.logviewer;

import com.tibagni.logviewer.filter.Filter;
import com.tibagni.logviewer.filter.FilterException;
import com.tibagni.logviewer.filter.Filters;
import com.tibagni.logviewer.log.FileLogReader;
import com.tibagni.logviewer.log.LogEntry;
import com.tibagni.logviewer.log.LogReaderException;
import com.tibagni.logviewer.log.parser.LogParser;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LogViewerPresenter extends AsyncPresenter implements LogViewer.Presenter {
  private LogViewer.View view;

  private List<Filter> filters;
  private LogEntry[] allLogs;
  private LogEntry[] filteredLogs;
  private LogParser logParser;

  public LogViewerPresenter(LogViewer.View view) {
    super(view);
    this.view = view;
    filters = new ArrayList<>();
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
  }

  @Override
  public void saveFilters(File filterFile) {
    try {
      boolean firstLoop = true;
      BufferedWriter fileWriter = new BufferedWriter(new FileWriter(filterFile));
      for (Filter f : filters) {
        if (firstLoop) {
          firstLoop = false;
        } else {
          fileWriter.newLine();
        }

        fileWriter.write(f.serializeFilter());
      }
      fileWriter.close();
    } catch (IOException e) {
      view.showErrorMessage(e.getMessage());
    }
  }

  @Override
  public void loadFilters(File filtersFile) {
    BufferedReader bufferedReader = null;
    try {
      bufferedReader = new BufferedReader(new FileReader(filtersFile));
      String line;
      List<Filter> filtersFromFile = new ArrayList<>();
      while ((line = bufferedReader.readLine()) != null && line.trim().length() > 0) {
        filtersFromFile.add(Filter.createFromString(line));
      }

      setFilters(filtersFromFile);
    } catch (FilterException | IOException e) {
      view.showErrorMessage(e.getMessage());
    } finally {
      if (bufferedReader != null) {
        try {
          bufferedReader.close();
        } catch (IOException ignore) {
        }
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
          } else {
            view.showErrorMessage("No logs found");
          }
        });
      } catch (LogReaderException e) {
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
}
