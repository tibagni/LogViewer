package com.tibagni.logviewer;

import com.tibagni.logviewer.preferences.LogViewerPreferences;
import com.tibagni.logviewer.util.PropertiesWrapper;
import com.tibagni.logviewer.util.StringUtils;
import com.tibagni.logviewer.util.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class LogViewerApplication {
  private static final String APPLICATION_NAME = "Log Viewer";

  private static final String APP_PROPERTIES_FILE = "properties/app.properties";
  private static final String VERSION_KEY = "version";

  private LogViewerApplication() { }

  public static void main(String[] args) {
    LogViewerApplication application = new LogViewerApplication();
    application.start();
  }

  private void start() {
    initLookAndFeel();
    JFrame frame = new JFrame(getApplicationTitle());
    watchLookAndFeelUpdates(frame);
    LogViewerView logViewer = new LogViewerView(frame);

    frame.setContentPane(logViewer.getContentPane());
    frame.pack();
    frame.setVisible(true);
  }

  private void initLookAndFeel() {
    String lookAndFeel = LogViewerPreferences.getInstance().getLookAndFeel();
    if (!StringUtils.isEmpty(lookAndFeel)) {
        SwingUtils.setLookAndFeel(lookAndFeel);
    }
  }

  private String getApplicationTitle() {
    return APPLICATION_NAME + " v" + getCurrentVersion();
  }

  private String getCurrentVersion() {
    String currentVersion = "unknown";
    try {
      PropertiesWrapper appProperties = new PropertiesWrapper(APP_PROPERTIES_FILE);
      currentVersion = appProperties.get(VERSION_KEY);
    } catch (IOException e) {}

    return currentVersion;
  }

  private void watchLookAndFeelUpdates(Frame frame) {
    LogViewerPreferences prefs = LogViewerPreferences.getInstance();
    prefs.addPreferenceListener(new LogViewerPreferences.Adapter() {
      @Override
      public void onLookAndFeelChanged() {
        String lookAndFeel = prefs.getLookAndFeel();
        if (!StringUtils.isEmpty(lookAndFeel)) {
          SwingUtils.updateLookAndFeelAfterStart(lookAndFeel, frame);
        }
      }
    });
  }
}
