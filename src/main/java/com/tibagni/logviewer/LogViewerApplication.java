package com.tibagni.logviewer;

import com.tibagni.logviewer.preferences.LogViewerPreferences;
import com.tibagni.logviewer.util.StringUtils;
import com.tibagni.logviewer.util.SwingUtils;

import javax.swing.*;
import java.awt.*;

public class LogViewerApplication {
  private static final String APPLICATION_NAME = "Log Viewer";


  public static void main(String[] args) {
    initLookAndFeel();
    JFrame frame = new JFrame(APPLICATION_NAME);
    watchLookAndFeelUpdates(frame);
    LogViewerView logViewer = new LogViewerView(frame);

    frame.setContentPane(logViewer.getContentPane());
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.pack();
    frame.setVisible(true);
  }

  private static void initLookAndFeel() {
    String lookAndFeel = LogViewerPreferences.getInstance().getLookAndFeel();
    if (!StringUtils.isEmpty(lookAndFeel)) {
        SwingUtils.setLookAndFeel(lookAndFeel);
    }
  }

  private static void watchLookAndFeelUpdates(Frame frame) {
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
