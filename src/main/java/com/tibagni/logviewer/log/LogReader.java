package com.tibagni.logviewer.log;

import java.io.File;
import java.util.Set;

public interface LogReader {
  void readLogs() throws LogReaderException;

  int size();

  File get(String logName);

  Set<String> getAvailableLogPaths();

  void close();
}
