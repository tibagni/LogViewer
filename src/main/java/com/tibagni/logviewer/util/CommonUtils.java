package com.tibagni.logviewer.util;

import com.tibagni.logviewer.LogViewerApplication;
import com.tibagni.logviewer.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

public class CommonUtils {

  public static void restartApplication() throws IOException, URISyntaxException {
    final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    final File currentJar = new File(LogViewerApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());

    /* is it a jar file? */
    if (!currentJar.getName().endsWith(".jar")) {
      throw new IllegalStateException("Current JAR not found");
    }

    /* Build command: java -jar application.jar */
    final ArrayList<String> command = new ArrayList<String>();
    command.add(javaBin);
    command.add("-jar");
    command.add(currentJar.getPath());

    final ProcessBuilder builder = new ProcessBuilder(command);
    builder.start();
    System.exit(0);
  }

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
