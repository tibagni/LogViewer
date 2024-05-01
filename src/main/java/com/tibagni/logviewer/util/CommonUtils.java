package com.tibagni.logviewer.util;

import com.tibagni.logviewer.logger.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CommonUtils {

  public static void sleepSilently(long duration) {
    try {
      Thread.sleep(duration);
    } catch (InterruptedException e) {
      Logger.error("Thread.sleep was interrupted", e);
    }
  }

  @SafeVarargs
  public static <E> List<E> listOf(E... elements) {
    List<E> l = new ArrayList<>();
    Collections.addAll(l, elements);

    return l;
  }

  public static
  <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
    List<T> list = new ArrayList<>(c);
    Collections.sort(list);
    return list;
  }

  public static String calculateStackTraceHash(Throwable throwable) {
    StringBuilder stackTrace = new StringBuilder();
    for (StackTraceElement element : throwable.getStackTrace()) {
      stackTrace.append(element.toString()).append("\n");
    }

    byte[] bytes = stackTrace.toString().getBytes();

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(bytes);
      StringBuilder hexString = new StringBuilder();
      for (byte b : hashBytes) {
        hexString.append(String.format("%02x", b));
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException ignored) { }

    return "empty";
  }
}
