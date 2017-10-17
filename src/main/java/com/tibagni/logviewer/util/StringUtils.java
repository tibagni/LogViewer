package com.tibagni.logviewer.util;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;

public class StringUtils {
  public static final String LINE_SEPARATOR = System.lineSeparator();

  public static boolean isEmpty(String str) {
    if (str == null || str.trim().length() == 0) {
      return true;
    }
    return false;
  }

  public static String encodeBase64(String str) {
    byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
    String encodedStr = DatatypeConverter.printBase64Binary(bytes);

    return encodedStr;
  }

  public static String decodeBase64(String base64) {
    byte[] bytes = DatatypeConverter.parseBase64Binary(base64);
    String str = new String(bytes, StandardCharsets.UTF_8);

    return str;
  }
}
