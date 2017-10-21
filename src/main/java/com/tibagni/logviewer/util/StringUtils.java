package com.tibagni.logviewer.util;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;

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
    return DatatypeConverter.printBase64Binary(bytes);
  }

  public static String decodeBase64(String base64) {
    byte[] bytes = DatatypeConverter.parseBase64Binary(base64);
    return new String(bytes, StandardCharsets.UTF_8);
  }
}
