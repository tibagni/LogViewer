package com.tibagni.logviewer.preferences;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;

public class LogViewerPreferences {
    private static LogViewerPreferences instance;
    private Preferences preferences;

    private Set<Listener> listeners = new HashSet<>();

    private static final String FILTERS_PATH = "filters_path";
    private static final String LOOK_AND_FEEL = "look_and_feel";

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

    public String getLookAndFeel() {
        return preferences.get(LOOK_AND_FEEL, "");
    }

    public void setLookAndFeel(String lookAndFeel) {
        preferences.put(LOOK_AND_FEEL, lookAndFeel);
        listeners.forEach(l -> l.onLookAndFeelChanged());
    }

    public interface Listener {
        void onLookAndFeelChanged();
        void onDefaultFiltersPathChanged();
    }

    public static abstract class Adapter implements Listener {
        @Override
        public void onLookAndFeelChanged() {

        }

        @Override
        public void onDefaultFiltersPathChanged() {

        }
    }
}
