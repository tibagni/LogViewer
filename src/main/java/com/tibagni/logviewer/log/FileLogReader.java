package com.tibagni.logviewer.log;

import com.tibagni.logviewer.util.StringUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FileLogReader implements LogReader {
  private File[] logFiles;
  private Map<String, File> logStrings;

  private boolean isClosed;

  public FileLogReader(File[] logFiles) {
    this.logFiles = logFiles;
    this.logStrings = new HashMap<>();
  }

  @Override
  public void readLogs() throws LogReaderException {
    if (isClosed) {
      throw new IllegalStateException("Reader already closed");
    }

    if (logFiles == null || logFiles.length == 0) {
      throw new LogReaderException("There are no logs to read!");
    }

    for (File logFile : logFiles) {
      logStrings.put(logFile.getPath(), logFile);
    }
  }

  private String readFile(File file) throws IOException {
    String line;
    StringBuilder builder = new StringBuilder();

    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      while ((line = reader.readLine()) != null) {
        builder.append(line);
        builder.append(StringUtils.LINE_SEPARATOR);
      }
    }

    return builder.toString();
  }

  @Override
  public int size() {
    return logStrings.size();
  }

  @Override
  public File get(String logName) {
    return logStrings.get(logName);
  }

  @Override
  public Set<String> getAvailableLogPaths() {
    return logStrings.keySet();
  }

  @Override
  public void close() {
    isClosed = true;

    logStrings.clear();
    logStrings = null;

    logFiles = null;
  }
}
