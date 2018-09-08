package com.tibagni.logviewer.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class StringUtils {
  public static final String LINE_SEPARATOR = System.lineSeparator();
  public static final String RIGHT_ARROW = "\u2192";
  public static final String LEFT_ARROW = "\u2190";
  public static final String VERTICAL_SEPARATOR = "\u2759";

  public static boolean isEmpty(String str) {
    return str == null || str.trim().length() == 0;
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
    return (str1 == null ? str2 == null : str1.equals(str2));
  }
}
