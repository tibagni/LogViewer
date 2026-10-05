package com.tibagni.logviewer.filter;

import com.tibagni.logviewer.log.LogStream;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Encapsulates contextual information and statistics for a single Filter.
 * Tracking the number of matches found across specific streams.
 */
public class ContextInfo {
  private final ConcurrentHashMap<LogStream, AtomicInteger> linesFound;
  private Set<LogStream> allowedStreams;

  public ContextInfo() {
    linesFound = new ConcurrentHashMap<>();
    for (LogStream stream : LogStream.values()) {
      linesFound.put(stream, new AtomicInteger(0));
    }
  }

  public void setAllowedStreams(Set<LogStream> allowedStreams) {
    this.allowedStreams = allowedStreams;
  }

  public int getTotalLinesFound() {
    int totalLinesFound = 0;
    for (Map.Entry<LogStream, AtomicInteger> entry : linesFound.entrySet()) {
      if (allowedStreams == null || allowedStreams.contains(entry.getKey())) {
        totalLinesFound += entry.getValue().get();
      }
    }
    return totalLinesFound;
  }

  public void incrementLineCount(LogStream stream) {
    linesFound.get(stream).incrementAndGet();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ContextInfo that = (ContextInfo) o;
    if (!Objects.equals(allowedStreams, that.allowedStreams)) {
      return false;
    }
    for (LogStream stream : LogStream.values()) {
      int thisCount = this.linesFound.get(stream).get();
      int thatCount = that.linesFound.get(stream).get();
      if (thisCount != thatCount) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    int countsHash = 0;
    for (LogStream stream : LogStream.values()) {
      countsHash = 31 * countsHash + linesFound.get(stream).get();
    }
    return Objects.hash(countsHash, allowedStreams);
  }
}
