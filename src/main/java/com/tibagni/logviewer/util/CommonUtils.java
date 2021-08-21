package com.tibagni.logviewer.util;

import com.tibagni.logviewer.logger.Logger;

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
}
