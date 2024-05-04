package com.tibagni.logviewer;

import com.formdev.flatlaf.util.UIScale;
import com.tibagni.logviewer.logger.Logger;
import com.tibagni.logviewer.preferences.LogViewerPreferences;
import com.tibagni.logviewer.rc.CrashReportConfig;
import com.tibagni.logviewer.rc.LogLevelConfig;
import com.tibagni.logviewer.rc.RuntimeConfiguration;
import com.tibagni.logviewer.rc.UIScaleConfig;
import com.tibagni.logviewer.theme.LogViewerThemeManager;
import com.tibagni.logviewer.updates.ReleaseInfo;
import com.tibagni.logviewer.updates.UpdateAvailableDialog;
import com.tibagni.logviewer.updates.UpdateManager;
import com.tibagni.logviewer.util.CommonUtils;
import com.tibagni.logviewer.util.StringUtils;
import com.tibagni.logviewer.util.scaling.UIScaleUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

public class LogViewerApplication implements UpdateManager.UpdateListener {
  private MainViewImpl mainView;

  private LogViewerApplication() { }

  public static void main(String[] args) {
    RuntimeConfiguration.initialize();
    UIScaleUtils.initialize(RuntimeConfiguration.getConfig(RuntimeConfiguration.UI_SCALE, UIScaleConfig.class));
    Logger.initialize(RuntimeConfiguration.getConfig(RuntimeConfiguration.LOG_LEVEL, LogLevelConfig.class));

    CrashReportConfig crashReportConfig = RuntimeConfiguration.getConfig(RuntimeConfiguration.CRASH_REPORT,
        CrashReportConfig.class);
    if (crashReportConfig != null && crashReportConfig.getConfigValue()) {
      Logger.info("Enabling crash report...");
      configureUncaughtExceptionHandler();
    }

    Set<File> initialLogFiles = Arrays
            .stream(args)
            .map(File::new)
            .filter(f -> f.exists() && f.isFile())
            .collect(Collectors.toSet());

    LogViewerApplication application = new LogViewerApplication();
    application.start(initialLogFiles);
  }

  private static void configureUncaughtExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
      String fileName = "CRASH-logviewer_" + CommonUtils.calculateStackTraceHash(e) + ".txt";
      Path filePath = Paths.get(System.getProperty("user.home"), fileName);
      SimpleDateFormat formatter = new SimpleDateFormat("dd-MM HH:mm:ss.SSS");
      try (PrintWriter pw = new PrintWriter(new FileWriter(filePath.toFile()))) {
        pw.println("Exception happened on thread " + t.getId() + " (" + t.getName() + ") at "
            + formatter.format(new Date()));
        e.printStackTrace(pw);
        pw.println("\n---------------- Previous logs before the exception:");
        Logger.dump(pw);
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    });
  }

  private void start(Set<File> initialLogFiles) {
    startCheckingForUpdates();
    initLookAndFeel();

    // Update ScaleFactor based on theme
    UIScaleUtils.updateScaleFactor((int) UIScale.getUserScaleFactor());
    newLogViewerWindow(initialLogFiles);
  }

  private void initLookAndFeel() {
    LogViewerPreferences prefs = ServiceLocator.INSTANCE.getLogViewerPrefs();
    LogViewerThemeManager themeManager = ServiceLocator.INSTANCE.getThemeManager();
    String lookAndFeel = prefs.getLookAndFeel();
    Logger.debug("Installing theme: " + lookAndFeel);
    themeManager.setCurrentTheme(lookAndFeel);

//    UIScaleUtils.updateDefaultSizes();
    watchLookAndFeelUpdates();
  }

  void newLogViewerWindow(Set<File> initialLogFiles) {
    JFrame frame = new JFrame(getApplicationTitle());

    mainView = new MainViewImpl(frame, ServiceLocator.INSTANCE.getLogViewerPrefs(), initialLogFiles);
    frame.setContentPane(mainView.getContentPane());
    frame.pack();
    frame.setVisible(true);
  }

  private String getApplicationTitle() {
    return AppInfo.APPLICATION_NAME + " v" + AppInfo.INSTANCE.getCurrentVersion();
  }

  private void watchLookAndFeelUpdates() {
    LogViewerPreferences prefs = ServiceLocator.INSTANCE.getLogViewerPrefs();
    prefs.addPreferenceListener(new LogViewerPreferences.Adapter() {
      @Override
      public void onLookAndFeelChanged() {
        String lookAndFeel = prefs.getLookAndFeel();
        if (!StringUtils.isEmpty(lookAndFeel)) {
          Logger.debug("Installing theme: " + lookAndFeel);
          ServiceLocator.INSTANCE.getThemeManager().setCurrentTheme(lookAndFeel);
          mainView.themeChanged();
        }
      }
    });
  }

  private void startCheckingForUpdates() {
    UpdateManager updateManager = new UpdateManager(this);
    updateManager.checkForUpdates();
  }

  @Override
  public void onUpdateFound(@NotNull ReleaseInfo newRelease) {
    UpdateAvailableDialog.Companion.showUpdateAvailableDialog(null, newRelease);
  }

  @Override
  public void onUpToDate() {
    // Do Nothing
  }

  @Override
  public void onFailedToCheckForUpdate(@NotNull Throwable tr) {
    // Do nothing
  }
}
