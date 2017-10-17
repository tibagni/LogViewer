package com.tibagni.logviewer.filter;

import com.tibagni.logviewer.log.LogEntry;
import com.tibagni.logviewer.util.StringUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Filter {
  public static final String FILE_EXTENSION = "filter";

  private String name;
  private Color color;

  private Pattern pattern;
  private int flags = Pattern.CASE_INSENSITIVE;

  private Filter() { }

  public Filter(String name, String pattern, Color color) throws FilterException {
    this(name, pattern, color, false);
  }

  public Filter(String name, String pattern, Color color, boolean caseSensitive)
      throws FilterException {
    updateFilter(name, pattern, color, caseSensitive);
  }

  public void updateFilter(String name, String pattern, Color color) throws FilterException {
    updateFilter(name, pattern, color, ((flags & Pattern.CASE_INSENSITIVE) == 0));
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

  public String getName() {
    return name;
  }

  public Color getColor() {
    return color;
  }

  String getPatternString() {
    return pattern.toString();
  }

  boolean isCaseSensitive() {
    // Check if the CASE_INSENSITIVE is OFF!!
    return (flags & Pattern.CASE_INSENSITIVE) == 0;
  }

  /**
   * Apply this filter to the given input
   *
   * @param input on which the filter should be applied
   * @return a new String array containing the filtered output
   */
  public LogEntry[] apply(LogEntry[] input) {
    List<LogEntry> filtered = new ArrayList<>();

    for (LogEntry entry : input) {
      if (appliesTo(entry.getLogText())) {
        entry.setFilterColor(color);
        filtered.add(entry);
      }
    }

    return filtered.toArray(new LogEntry[0]);
  }

  /**
   * Take a single String and return whether the it appliesTo this filter or not
   *
   * @param inputLine
   * @return
   */
  boolean appliesTo(String inputLine) {
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
    return String.format("Filter: [Name=%s, pattern=%s, regexFlags=%d, color=%s]", name, pattern, flags, color);
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
      String params[] = filterString.split(",");
      if (params.length != 4) {
        throw new IllegalArgumentException();
      }

      Filter filter = new Filter();
      filter.name = params[0];
      filter.flags = Integer.parseInt(params[2]);
      filter.pattern = filter.getPattern(StringUtils.decodeBase64(params[1]));

      String rgb[] = params[3].split(":");
      if (rgb.length != 3) {
        throw new IllegalArgumentException("Wrong color format");
      }

      filter.color = new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));

      return filter;
    } catch (Exception e) {
      throw new FilterException("Wrong filter format: " + filterString, e);
    }
  }
}