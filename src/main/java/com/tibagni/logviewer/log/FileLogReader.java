package com.tibagni.logviewer.log;

import com.tibagni.logviewer.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FileLogReader implements LogReader {
  private File[] logFiles;
  private Map<String, String> logStrings;

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

    File currentFile = null;
    try {
      for (File logFile : logFiles) {
        currentFile = logFile;
        logStrings.put(currentFile.getName(), readFile(currentFile));
      }

    } catch (IOException e) {
      throw new LogReaderException("Error reading: " + currentFile, e);
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
  public String get(String logName) {
    return logStrings.get(logName);
  }

  @Override
  public Set<String> getAvailableLogsNames() {
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
