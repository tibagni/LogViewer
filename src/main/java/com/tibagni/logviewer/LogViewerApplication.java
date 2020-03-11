package com.tibagni.logviewer;

import com.tibagni.logviewer.logger.Logger;
import com.tibagni.logviewer.lookandfeel.LookNFeel;
import com.tibagni.logviewer.lookandfeel.LookNFeelProvider;
import com.tibagni.logviewer.preferences.LogViewerPreferences;
import com.tibagni.logviewer.preferences.LogViewerPreferencesImpl;
import com.tibagni.logviewer.updates.ReleaseInfo;
import com.tibagni.logviewer.updates.UpdateAvailableDialog;
import com.tibagni.logviewer.updates.UpdateManager;
import com.tibagni.logviewer.util.CommonUtils;
import com.tibagni.logviewer.util.StringUtils;
import com.tibagni.logviewer.util.SwingUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class LogViewerApplication implements UpdateManager.UpdateListener {
  private LogViewerApplication() { }

  public static void main(String[] args) {
    Set<File> initialLogFiles = Arrays
            .stream(args)
            .map(fileName -> new File(fileName))
            .filter(f -> f.exists() && f.isFile())
            .collect(Collectors.toSet());

    LogViewerApplication application = new LogViewerApplication();
    application.start(initialLogFiles);
  }

  private void start(Set<File> initialLogFiles) {
    startCheckingForUpdates();
    initLookAndFeel();

    newLogViewerWindow(initialLogFiles);
  }

  private void initLookAndFeel() {
    LookNFeelProvider lookNFeelProvider = LookNFeelProvider.getInstance();
    LogViewerPreferences prefs = LogViewerPreferencesImpl.INSTANCE;
    String lookAndFeel = prefs.getLookAndFeel();
    if (!StringUtils.isEmpty(lookAndFeel) &&
        lookNFeelProvider.getByClass(lookAndFeel) == null) {
      Logger.debug(lookAndFeel + " not found. Fallback to default...");
      lookAndFeel = lookNFeelProvider.getDefaultLookNFeel().getCls();
    }

    if (!StringUtils.isEmpty(lookAndFeel)) {
      LookNFeel lnf = lookNFeelProvider.getByClass(lookAndFeel);
      lookNFeelProvider.applyTheme(lnf);

      SwingUtils.setLookAndFeel(lookAndFeel);
    }
    watchLookAndFeelUpdates();
  }

  void newLogViewerWindow(Set<File> initialLogFiles) {
    JFrame frame = new JFrame(getApplicationTitle());
    LogViewerView logViewer = new LogViewerView(frame, this, initialLogFiles);

    frame.setContentPane(logViewer.getContentPane());
    frame.pack();
    frame.setVisible(true);
  }

  private String getApplicationTitle() {
    return AppInfo.APPLICATION_NAME + " v" + AppInfo.getCurrentVersion();
  }

  private void watchLookAndFeelUpdates() {
    LogViewerPreferences prefs = LogViewerPreferencesImpl.INSTANCE;
    prefs.addPreferenceListener(new LogViewerPreferencesImpl.Adapter() {
      @Override
      public void onLookAndFeelChanged() {
        String lookAndFeel = prefs.getLookAndFeel();
        if (!StringUtils.isEmpty(lookAndFeel)) {
          int userChoice = JOptionPane.showConfirmDialog(
                  javax.swing.FocusManager.getCurrentManager().getActiveWindow(),
                  "Do you want to restart LogViewer to apply the new Look and Feel?",
                  "Changing theme",
                  JOptionPane.YES_NO_OPTION,
                  JOptionPane.WARNING_MESSAGE);

          if (userChoice == JOptionPane.YES_NO_OPTION) {
            try {
              CommonUtils.restartApplication();
            } catch (Exception e) {
              Logger.error("Failed to restart", e);
            }
          }
        }
      }
    });
  }

  private void startCheckingForUpdates() {
    UpdateManager updateManager = new UpdateManager(this);
    updateManager.checkForUpdates();
  }

  @Override
  public void onUpdateFound(ReleaseInfo newRelease) {
    UpdateAvailableDialog.showUpdateAvailableDialog(newRelease);
  }

  @Override
  public void onUpToDate() {
    // Do Nothing
  }

  @Override
  public void onFailedToCheckForUpdate(Throwable tr) {
    // Do nothing
  }
}
