package com.tibagni.logviewer.filter;

import com.tibagni.logviewer.log.LogStream;
import com.tibagni.logviewer.util.StringUtils;

import java.awt.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Filter {
  public static final String FILE_EXTENSION = "filter";

  private boolean applied;
  private String name;
  private Color color;

  private Pattern pattern;
  private int flags = Pattern.CASE_INSENSITIVE;

  private ContextInfo temporaryInfo;

  private Filter() { }

  public Filter(Filter from) throws FilterException {
    name = from.name;
    color = new Color(from.color.getRGB());
    flags = from.flags;
    applied = from.isApplied();
    pattern = getPattern(from.pattern.pattern());
    if (temporaryInfo != null) {
      temporaryInfo = new ContextInfo(from.temporaryInfo);
    }
  }

  public Filter(String name, String pattern, Color color) throws FilterException {
    this(name, pattern, color, false);
  }

  public Filter(String name, String pattern, Color color, boolean caseSensitive)
      throws FilterException {
    updateFilter(name, pattern, color, caseSensitive);
  }

  public void updateFilter(String name, String pattern, Color color, boolean caseSensitive)
      throws FilterException {

    if (StringUtils.isEmpty(name) || StringUtils.isEmpty(pattern) || color == null) {
      throw new FilterException("You must provide a name, a regex pattern and a color for the filter");
    }

    if (caseSensitive) {
      flags &= ~Pattern.CASE_INSENSITIVE;
    } else {
      flags |= Pattern.CASE_INSENSITIVE;
    }

    this.name = name;
    this.color = color;
    this.pattern = getPattern(pattern);
  }

  public boolean isApplied() {
    return applied;
  }

  public void setApplied(boolean applied) {
    this.applied = applied;
  }

  public String getName() {
    return name;
  }

  public Color getColor() {
    return color;
  }

  public String getPatternString() {
    return pattern.toString();
  }

  public ContextInfo getTemporaryInfo() {
    return temporaryInfo;
  }

  public void resetTemporaryInfo() {
    this.temporaryInfo = null;
  }

  void initTemporaryInfo() {
    temporaryInfo = new ContextInfo();
  }

  public boolean isCaseSensitive() {
    // Check if the CASE_INSENSITIVE is OFF!!
    return (flags & Pattern.CASE_INSENSITIVE) == 0;
  }

  /**
   * Take a single String and return whether the it appliesTo this filter or not
   *
   * @param inputLine A single log line
   * @return true if this filter is applicable to the input line. False otherwise
   */
  public boolean appliesTo(String inputLine) {
    return pattern.matcher(inputLine).find();
  }

  private Pattern getPattern(String pattern) throws FilterException {
    try {
      return Pattern.compile(pattern, flags);
    } catch (PatternSyntaxException e) {
      throw new FilterException("Invalid pattern: " + pattern, e);
    }
  }

  @Override
  public String toString() {
    return String.format("Filter: [Name=%s, pattern=%s, regexFlags=%d, color=%s, applied=%b]",
        name, pattern, flags, color, applied);
  }

  public String serializeFilter() {
    return String.format("%s,%s,%d,%d:%d:%d",
        name.replaceAll(",", " "),
        StringUtils.encodeBase64(getPatternString()),
        flags,
        color.getRed(),
        color.getGreen(),
        color.getBlue());
  }

  public static Filter createFromString(String filterString) throws FilterException {
    // See format in 'serializeFilter'
    try {
      String[] params = filterString.split(",");
      if (params.length != 4) {
        throw new IllegalArgumentException();
      }

      Filter filter = new Filter();
      filter.name = params[0];
      filter.flags = Integer.parseInt(params[2]);
      filter.pattern = filter.getPattern(StringUtils.decodeBase64(params[1]));

      String[] rgb = params[3].split(":");
      if (rgb.length != 3) {
        throw new IllegalArgumentException("Wrong color format");
      }

      filter.color = new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));

      return filter;
    } catch (Exception e) {
      throw new FilterException("Wrong filter format: " + filterString, e);
    }
  }

  public static class ContextInfo {
    private final Map<LogStream, Integer> linesFound;
    private Set<LogStream> allowedStreams;


    private ContextInfo(ContextInfo from) {
      linesFound = new HashMap<>(from.linesFound);
      if (from.allowedStreams != null) {
        allowedStreams = new HashSet<>(from.allowedStreams);
      }
    }

    private ContextInfo() {
      linesFound = new HashMap<>();
    }

    public void setAllowedStreams(Set<LogStream> allowedStreams) {
      this.allowedStreams = allowedStreams;
    }

    public int getTotalLinesFound() {
      int totalLinesFound = 0;
      for (Map.Entry<LogStream, Integer> entry : linesFound.entrySet()) {
        if (allowedStreams == null || allowedStreams.contains(entry.getKey())) {
          totalLinesFound += entry.getValue();
        }
      }

      return totalLinesFound;
    }

    public void incrementLineCount(LogStream stream) {
      int currentCount = 0;
      if (linesFound.containsKey(stream)) {
        currentCount = linesFound.get(stream);
      }

      linesFound.put(stream, currentCount + 1);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Filter filter = (Filter) o;
    return flags == filter.flags &&
        Objects.equals(name, filter.name) &&
        Objects.equals(color, filter.color) &&
        Objects.equals(pattern, filter.pattern) &&
        Objects.equals(temporaryInfo, filter.temporaryInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, color, pattern, flags, temporaryInfo);
  }
}