package com.tibagni.logviewer.util;

import com.tibagni.logviewer.logger.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CommonUtils {

  public static void sleepSilently(long duration) {
    try {
      Thread.sleep(duration);
    } catch (InterruptedException e) {
      Logger.error("Thread.sleep was interrupted", e);
    }
  }

  public static <E> Set setOf(E... args) {
    Set<E> s = new HashSet<>();
    for (E e : args) {
      s.add(e);
    }

    return s;
  }

  public static int[] toIntArray(Collection<Integer> collection) {
    int[] returnArray = new int[collection.size()];
    int i = 0;
    for (Integer element : collection) {
      returnArray[i++] = element;
    }

    return returnArray;
  }
}
