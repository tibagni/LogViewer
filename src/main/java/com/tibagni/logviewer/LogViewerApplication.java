package com.tibagni.logviewer;

import com.tibagni.logviewer.logger.Logger;
import com.tibagni.logviewer.lookandfeel.LookNFeel;
import com.tibagni.logviewer.lookandfeel.LookNFeelProvider;
import com.tibagni.logviewer.preferences.LogViewerPreferences;
import com.tibagni.logviewer.updates.ReleaseInfo;
import com.tibagni.logviewer.updates.UpdateAvailableDialog;
import com.tibagni.logviewer.updates.UpdateManager;
import com.tibagni.logviewer.util.StringUtils;
import com.tibagni.logviewer.util.SwingUtils;

import javax.swing.*;

public class LogViewerApplication implements UpdateManager.UpdateListener {
  private LogViewerApplication() { }

  public static void main(String[] args) {
    LogViewerApplication application = new LogViewerApplication();
    application.start();
  }

  private void start() {
    startCheckingForUpdates();
    initLookAndFeel();

    newLogViewerWindow();
  }

  private void initLookAndFeel() {
    LookNFeelProvider lookNFeelProvider = LookNFeelProvider.getInstance();
    String lookAndFeel = LogViewerPreferences.getInstance().getLookAndFeel();
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

  void newLogViewerWindow() {
    JFrame frame = new JFrame(getApplicationTitle());
    LogViewerView logViewer = new LogViewerView(frame, this);

    frame.setContentPane(logViewer.getContentPane());
    frame.pack();
    frame.setVisible(true);
  }

  private String getApplicationTitle() {
    return AppInfo.APPLICATION_NAME + " v" + AppInfo.getCurrentVersion();
  }

  private void watchLookAndFeelUpdates() {
    LogViewerPreferences prefs = LogViewerPreferences.getInstance();
    prefs.addPreferenceListener(new LogViewerPreferences.Adapter() {
      @Override
      public void onLookAndFeelChanged() {
        String lookAndFeel = prefs.getLookAndFeel();
        if (!StringUtils.isEmpty(lookAndFeel)) {
          LookNFeelProvider lookNFeelProvider = LookNFeelProvider.getInstance();
          LookNFeel lnf = lookNFeelProvider.getByClass(lookAndFeel);
          lookNFeelProvider.applyTheme(lnf);
          SwingUtils.updateLookAndFeelAfterStart(lookAndFeel);
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
