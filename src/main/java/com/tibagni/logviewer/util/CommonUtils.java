package com.tibagni.logviewer.util;

import com.tibagni.logviewer.logger.Logger;

import java.util.*;

public class CommonUtils {

  public static void sleepSilently(long duration) {
    try {
      Thread.sleep(duration);
    } catch (InterruptedException e) {
      Logger.error("Thread.sleep was interrupted", e);
    }
  }

  public static <E> List<E> listOf(E... elements) {
    List<E> l = new ArrayList<>();
    for (E e : elements) {
      l.add(e);
    }

    return l;
  }

  public static int[] toIntArray(Collection<Integer> collection) {
    int[] returnArray = new int[collection.size()];
    int i = 0;
    for (Integer element : collection) {
      returnArray[i++] = element;
    }

    return returnArray;
  }

  public static
  <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
    List<T> list = new ArrayList<>(c);
    Collections.sort(list);
    return list;
  }
}
