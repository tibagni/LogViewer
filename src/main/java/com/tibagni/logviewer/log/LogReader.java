package com.tibagni.logviewer.log;

import java.nio.charset.Charset;
import java.util.Set;

public interface LogReader {
  void readLogs(Charset charset) throws LogReaderException;

  int size();

  String get(String logName);

  Set<String> getAvailableLogPaths();

  void close();
}
