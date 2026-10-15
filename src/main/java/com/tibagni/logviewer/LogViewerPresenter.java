package com.tibagni.logviewer;

import com.tibagni.logviewer.filter.Filter;
import com.tibagni.logviewer.log.LogEntry;
import com.tibagni.logviewer.log.LogLineInfo;
import com.tibagni.logviewer.log.LogStream;

import com.tibagni.logviewer.filter.FilterContextTag;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

public interface LogViewerPresenter {

  void init();
  FilterContextTag getFilterContextTag();
  void loadLogs(File[] logFiles);
  void loadLogs(File[] logFiles, Charset charset);
  void refreshLogs();
  void refreshLogsWithDifferentCharset(Charset charset);
  void saveFilteredLogs(File file);

  int getNextFilteredLogForFilter(Filter filter, int firstLogIndexSearch);
  int getPrevFilteredLogForFilter(Filter filter, int firstLogIndexSearch);
  void goToTimestamp(String timestamp);

  void setStreamAllowed(LogStream stream, boolean allowed);
  boolean isStreamAllowed(LogStream stream);

  void ignoreLogsBefore(int index);
  void ignoreLogsAfter(int index);
  void resetIgnoredLogs(boolean resetStarting, boolean resetEnding);
  int getVisibleLogsOffset();

  LogEntry getFirstVisibleLog();
  LogEntry getLastVisibleLog();

  void addLogEntriesToMyLogs(List<LogEntry> entries);
  void removeFromMyLog(int[] indices);

  LogLineInfo getLogLineInfo(LogEntry entry);

  void finishing();
}
