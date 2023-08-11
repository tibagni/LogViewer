package com.tibagni.logviewer.util;

import org.apache.commons.lang3.StringEscapeUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

public class StringUtils {
  public static final String LINE_SEPARATOR = System.lineSeparator();
  public static final String RIGHT_ARROW = "\u2192";
  public static final String LEFT_ARROW = "\u2190";
  public static final String VERTICAL_SEPARATOR = "\u2759";

  public static final String LEFT_ARROW_WITH_HOOK = "\u21A9";
  public static final String RIGHT_ARROW_WITH_HOOK = "\u21AA";

  public static final String LEFT_BLACK_POINTER = "\u25c0";
  public static final String RIGHT_BLACK_POINTER = "\u25b6";

  public static final String RIGHT_ARROW_HEAD = "\u25B8";
  public static final String DOWN_ARROW_HEAD = "\u25BE";
  public static final String UP_ARROW_HEAD = "\u25B4";

  public static final String DOWN_ARROW_HEAD_BIG = "\u25BC";
  public static final String UP_ARROW_HEAD_BIG = "\u25B2";

  public static final String THREE_LINES = "\u2630";

  public static final String DELETE = "\u2715";
  public static final String PLUS = "\uFF0B";

  public static boolean isEmpty(String str) {
    return str == null || str.trim().length() == 0;
  }

  public static boolean isNotEmpty(String str) {
    return !isEmpty(str);
  }

  public static String encodeBase64(String str) {
    byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
    return Base64.getEncoder().encodeToString(bytes);
  }

  public static String decodeBase64(String base64) {
    byte[] bytes = Base64.getDecoder().decode(base64);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  public static boolean areEquals(String str1, String str2) {
    return (Objects.equals(str1, str2));
  }

  public static String htmlHighlightAndEscape(String text, int start, int end) {
    String prefix = escape(text.substring(0, start));
    String highlightedPart = escape(text.substring(start, end));
    String suffix = escape(text.substring(end));
    return prefix + highlight(highlightedPart) + suffix;
  }

  public static String htmlEscape(String text) {
    return escape(text);
  }

  private static String highlight(String text) {
    return "<span style=\"background-color:yellow;color:black\">" + text + "</span>";
  }

  public static boolean endsWithOneOf(String text, String[] suffixes) {
    return Arrays.stream(suffixes).anyMatch(text::endsWith);
  }

  private static String escape(String text) {
    return StringEscapeUtils.escapeHtml4(text);
  }

  public static String wrapHtml(String text) {
    return "<html>" + text + "</html>";
  }
}
