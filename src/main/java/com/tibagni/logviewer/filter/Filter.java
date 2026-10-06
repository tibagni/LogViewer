package com.tibagni.logviewer.filter;

import com.tibagni.logviewer.i18n.I18n;
import com.tibagni.logviewer.log.LogEntry;
import com.tibagni.logviewer.log.LogLevel;
import com.tibagni.logviewer.log.LogStream;
import com.tibagni.logviewer.util.StringUtils;

import java.awt.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Filter {
  public static final String FILE_EXTENSION = "filter";

  private boolean applied;
  private String name;
  private Color color;
  private LogLevel verbosity = LogLevel.VERBOSE;
  private Pattern pattern;
  private int flags = Pattern.CASE_INSENSITIVE;
  private ContextInfo temporaryInfo;
  private boolean isSimpleFilter;

  // Cache the pattern string and the case sensitive flag
  // here for better performance during comparisons
  private String patternString;
  private boolean caseSensitive;

  public boolean wasLoadedFromLegacyFile = false;

  // We intentionally don't copy the temporary info as it is temporary
  // We intentionally don't copy 'wasLoadedFromLegacyFile' as the copied filter would not have been loaded from a file
  @SuppressWarnings("CopyConstructorMissesField")
  public Filter(Filter from) throws FilterException {
    name = from.name;
    color = new Color(from.color.getRGB());
    flags = from.flags;
    applied = from.isApplied();
    pattern = getPattern(from.pattern.pattern());
    verbosity = from.verbosity;
    isSimpleFilter = from.isSimpleFilter;

    patternString = from.patternString;
    caseSensitive = from.caseSensitive;
  }

  public Filter(String name, String pattern, Color color, LogLevel verbosity) throws FilterException {
    this(name, pattern, color, verbosity, false);
  }

  public Filter(String name, String pattern, Color color, LogLevel verbosity, boolean caseSensitive)
      throws FilterException {
    updateFilter(name, pattern, color, verbosity, caseSensitive);
  }

  boolean nameIsPattern() {
    return StringUtils.areEquals(getName(), getPatternString());
  }

  public void updateFilter(String name, String pattern, Color color, LogLevel verbosity, boolean caseSensitive)
      throws FilterException {

    if (StringUtils.isEmpty(name) || StringUtils.isEmpty(pattern) || color == null) {
      throw new FilterException(I18n.get(I18n.FILTER_ERROR_EMPTY_FIELDS));
    }

    if (caseSensitive) {
      flags &= ~Pattern.CASE_INSENSITIVE;
    } else {
      flags |= Pattern.CASE_INSENSITIVE;
    }

    this.name = name;
    this.color = color;
    this.pattern = getPattern(pattern);
    this.verbosity = verbosity;
    this.isSimpleFilter = !StringUtils.isPotentialRegex(pattern);

    this.patternString = pattern;
    this.caseSensitive = caseSensitive;
  }

  public static Filter createFromString(String filterString) throws FilterException {
    // See format in 'serializeFilter'
    try {
      String[] params = filterString.split(",");
      if (params.length < 4) {
        throw new IllegalArgumentException();
      }

      String[] rgb = params[3].split(":");
      if (rgb.length != 3) {
        throw new IllegalArgumentException(I18n.get(I18n.FILTER_ERROR_WRONG_COLOR_FORMAT));
      }

      boolean isLegacy = params.length == 4;

      String name = params[0];
      String pattern = StringUtils.decodeBase64(params[1]);
      Color color = new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
      LogLevel verbosity = isLegacy ? LogLevel.VERBOSE : LogLevel.valueOf(params[4]);
      int flags = Integer.parseInt(params[2]);
      boolean isCaseSensitive = (flags & Pattern.CASE_INSENSITIVE) == 0;

      Filter filter = new Filter(name, pattern, color, verbosity, isCaseSensitive);
      filter.wasLoadedFromLegacyFile = isLegacy;
      return filter;
    } catch (Exception e) {
      throw new FilterException(I18n.format(I18n.FILTER_ERROR_WRONG_FORMAT, filterString), e);
    }
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

  public LogLevel getVerbosity() {
    return verbosity;
  }

  public Color getColor() {
    return color;
  }

  public String getPatternString() {
    return patternString;
  }

  public Pattern getPattern() {
    return pattern;
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
    return caseSensitive;
  }

  /**
   * Take a single String and return whether it appliesTo this filter or not
   *
   * @param entry A single log line entry
   * @return true if this filter is applicable to the input line. False otherwise
   */
  public boolean appliesTo(LogEntry entry) {
    if (verbosity.ordinal() > entry.logLevel.ordinal()) {
      return false;
    }

    String inputLine = entry.getLogText();
    return isSimpleFilter ? simpleMatch(inputLine) : regexMatch(inputLine);
  }

  private boolean simpleMatch(String inputLine) {
    if (caseSensitive) {
      return inputLine.contains(patternString);
    }
    return StringUtils.containsIgnoreCase(inputLine, patternString);
  }

  private boolean regexMatch(String inputLine) {
    return pattern.matcher(inputLine).find();
  }

  private Pattern getPattern(String pattern) throws FilterException {
    try {
      return Pattern.compile(pattern, flags);
    } catch (PatternSyntaxException e) {
      throw new FilterException(I18n.format(I18n.FILTER_ERROR_INVALID_PATTERN, pattern), e);
    }
  }

  @Override
  public String toString() {
    return String.format("Filter: [Name=%s, pattern=%s, regexFlags=%d, color=%s, verbosity=%s, applied=%b]",
        name, pattern, flags, color, verbosity, applied);
  }

  public String serializeFilter() {
    return String.format("%s,%s,%d,%d:%d:%d,%s",
        name.replaceAll(",", " "),
        StringUtils.encodeBase64(getPatternString()),
        flags,
        color.getRed(),
        color.getGreen(),
        color.getBlue(),
        verbosity);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Filter filter = (Filter) o;
    return flags == filter.flags &&
        Objects.equals(name, filter.name) &&
        Objects.equals(color, filter.color) &&
        Objects.equals(getPatternString(), filter.getPatternString()) &&
        Objects.equals(temporaryInfo, filter.temporaryInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, color, pattern, flags, temporaryInfo);
  }
}
