package com.tibagni.logviewer.log;

import com.tibagni.logviewer.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FileLogReader implements LogReader {
  private File[] logFiles;
  private Map<String, String> logStrings;
  private Map<String, File> logFilesMap;

  private boolean isClosed;

  public FileLogReader(File[] logFiles) {
    this.logFiles = logFiles;
    this.logStrings = new HashMap<>();
    this.logFilesMap = new HashMap<>();
    if (logFiles != null) {
      for (File file : logFiles) {
        if (file != null) {
          logFilesMap.put(file.getPath(), file);
        }
      }
    }
  }

  @Override
  public File getFile(String logName) {
    if (logFilesMap != null) {
      File f = logFilesMap.get(logName);
      if (f != null) {
        return f;
      }
    }
    return new File(logName);
  }

  @Override
  public void readLogs(Charset charset) throws LogReaderException {
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
        logStrings.put(currentFile.getPath(), readFile(currentFile, charset));
      }

    } catch (IOException e) {
      throw new LogReaderException("Error reading: " + currentFile, e);
    }
  }

  // 64 KB buffer for reading log files. The default BufferedReader buffer size is 8 KB (8192 chars).
  // A 64 KB buffer reduces the number of underlying I/O read syscalls when reading large log files
  // (which are typically tens or hundreds of megabytes), improving sequential disk read throughput.
  private static final int READ_BUFFER_SIZE_BYTES = 64 * 1024;

  private String readFile(File file, Charset charset) throws IOException {
    String line;
    // Pre-size the StringBuilder based on the file size on disk.
    // In typical Android logcat files (mostly 1-byte ASCII characters in UTF-8), file.length() closely
    // approximates the required character capacity. Pre-sizing avoids the default capacity of 16 characters
    // repeatedly doubling and copying internal char[] arrays as the file is read, eliminating significant
    // GC overhead and memory churn. Clamped between 16 and Integer.MAX_VALUE - 8 (safe JVM max array size).
    int initialCapacity = (int) Math.min(Math.max(file.length(), 16), Integer.MAX_VALUE - 8);
    StringBuilder builder = new StringBuilder(initialCapacity);

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset),
        READ_BUFFER_SIZE_BYTES)) {
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
  public Set<String> getAvailableLogPaths() {
    return logStrings.keySet();
  }

  @Override
  public void close() {
    isClosed = true;

    logStrings.clear();
    logStrings = null;

    if (logFilesMap != null) {
      logFilesMap.clear();
      logFilesMap = null;
    }

    logFiles = null;
  }
}
