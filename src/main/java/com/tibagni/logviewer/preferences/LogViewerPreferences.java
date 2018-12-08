package com.tibagni.logviewer.preferences;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

public class LogViewerPreferences {
  private static LogViewerPreferences instance;
  private Preferences preferences;

  private Set<Listener> listeners = new HashSet<>();

  private static final String FILTERS_PATH = "filters_path";
  private static final String LAST_FILTER_PATH = "last_filter_path";
  private static final String OPEN_LAST_FILTER = "open_last_filter";
  private static final String LOGS_PATH = "logs_path";
  private static final String LOOK_AND_FEEL = "look_and_feel";
  private static final String REAPPLY_FILTERS_AFTER_EDIT = "reapply_filters_after_edit";
  private static final String REMEMBER_APPLIED_FILTERS = "remember_applied_filters";
  private static final String REMEMBER_APPLIED_FILTERS_PREFIX = "applied_for_group_";

  private LogViewerPreferences() {
    preferences = Preferences.userRoot().node(getClass().getName());
  }

  public static synchronized LogViewerPreferences getInstance() {
    if (instance == null) {
      instance = new LogViewerPreferences();
    }

    return instance;
  }

  public void addPreferenceListener(Listener listener) {
    listeners.add(listener);
  }

  public void removePreferenceListener(Listener listener) {
    listeners.remove(listener);
  }

  public File getDefaultFiltersPath() {
    String path = preferences.get(FILTERS_PATH, null);

    return path != null ? new File(path) :
        FileSystemView.getFileSystemView().getHomeDirectory();
  }

  public void setDefaultFiltersPath(File defaultFolder) {
    preferences.put(FILTERS_PATH, defaultFolder.getAbsolutePath());
    listeners.forEach(l -> l.onDefaultFiltersPathChanged());
  }

  public File[] getLastFilterPaths() {
    String pathStrings = preferences.get(LAST_FILTER_PATH, null);
    String[] paths = pathStrings.split("\\$");

    if (paths != null && paths.length > 0) {
      File[] files = new File[paths.length];
      for (int i = 0; i < paths.length; i++) {
        files[i] = new File(paths[i]);
      }

      return files;
    }
    return null;
  }

  public void setLastFilterPaths(List<File> filterPaths) {
    StringBuilder pathsString = new StringBuilder();
    boolean addSeparator = false;
    for (File file : filterPaths) {
      if (addSeparator) {
        pathsString.append("$");
      } else {
        addSeparator = true;
      }

      pathsString.append(file.getAbsolutePath());
    }

    preferences.put(LAST_FILTER_PATH, pathsString.toString());
    listeners.forEach(l -> l.onLastFilterPathChanged());
  }

  public void setOpenLastFilter(boolean openLastFilter) {
    preferences.putBoolean(OPEN_LAST_FILTER, openLastFilter);
    listeners.forEach(l -> l.onOpenLastFilterChanged());
  }

  public File getDefaultLogsPath() {
    String path = preferences.get(LOGS_PATH, null);

    return path != null ? new File(path) :
        FileSystemView.getFileSystemView().getHomeDirectory();
  }

  public void setDefaultLogsPath(File defaultFolder) {
    preferences.put(LOGS_PATH, defaultFolder.getAbsolutePath());
    listeners.forEach(l -> l.onDefaultLogsPathChanged());
  }

  public boolean shouldOpenLastFilter() {
    return preferences.getBoolean(OPEN_LAST_FILTER, false);
  }

  public String getLookAndFeel() {
    return preferences.get(LOOK_AND_FEEL, "");
  }

  public void setLookAndFeel(String lookAndFeel) {
    preferences.put(LOOK_AND_FEEL, lookAndFeel);
    listeners.forEach(l -> l.onLookAndFeelChanged());
  }

  public void setReapplyFiltersAfterEdit(boolean reApply) {
    preferences.putBoolean(REAPPLY_FILTERS_AFTER_EDIT, reApply);
    listeners.forEach(l -> l.onReapplyFiltersConfigChanged());
  }

  public boolean shouldReapplyFiltersAfterEdit() {
    return preferences.getBoolean(REAPPLY_FILTERS_AFTER_EDIT, true);
  }

  public void setRememberAppliedFilters(boolean remember) {
    preferences.putBoolean(REMEMBER_APPLIED_FILTERS, remember);
    listeners.forEach(l -> l.onRememberAppliedFiltersConfigChanged());
  }

  public boolean shouldRememberReappliedFilters() {
    return preferences.getBoolean(REMEMBER_APPLIED_FILTERS, true);
  }

  public void setAppliedFiltersIndices(String group, List<Integer> indices) {
    StringBuilder saveData = new StringBuilder();
    boolean isFirst = true;
    for (int index : indices) {
      if (isFirst) {
        isFirst = false;
      } else {
        saveData.append(",");
      }

      saveData.append(index);
    }

    preferences.put(REMEMBER_APPLIED_FILTERS_PREFIX + group, saveData.toString());
  }

  public List<Integer> getAppliedFiltersIndices(String group) {
    String savedData = preferences.get(REMEMBER_APPLIED_FILTERS_PREFIX + group, "");
    String[] indicesString = savedData.split(",");
    List<Integer> indices = new ArrayList<>();
    for (String strIndex : indicesString) {
      try {
        indices.add(Integer.parseInt(strIndex));
      } catch (NumberFormatException nfe) {}
    }

    return indices;
  }

  public interface Listener {
    void onLookAndFeelChanged();
    void onDefaultFiltersPathChanged();
    void onLastFilterPathChanged();
    void onOpenLastFilterChanged();
    void onDefaultLogsPathChanged();
    void onReapplyFiltersConfigChanged();
    void onRememberAppliedFiltersConfigChanged();
  }

  public static abstract class Adapter implements Listener {
    @Override public void onLookAndFeelChanged() { }
    @Override public void onDefaultFiltersPathChanged() { }
    @Override public void onLastFilterPathChanged() { }
    @Override public void onOpenLastFilterChanged() { }
    @Override public void onDefaultLogsPathChanged() { }
    @Override public void onReapplyFiltersConfigChanged() { }
    @Override public void onRememberAppliedFiltersConfigChanged() { }
  }
}
