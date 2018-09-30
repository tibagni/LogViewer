package com.tibagni.logviewer.about;

import com.tibagni.logviewer.AppInfo;
import com.tibagni.logviewer.logger.Logger;
import com.tibagni.logviewer.updates.ReleaseInfo;
import com.tibagni.logviewer.updates.UpdateAvailableDialog;
import com.tibagni.logviewer.updates.UpdateManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class AboutDialog extends JDialog {
  private JPanel contentPane;
  private JButton buttonOK;
  private JLabel applicationName;
  private JLabel versionStatus;
  private JProgressBar updateStatusProgress;
  private JLabel openSourceInfo;
  private JButton updateBtn;

  private UpdateManager updateManager;

  public AboutDialog() {
    setContentPane(contentPane);
    setModal(true);
    getRootPane().setDefaultButton(buttonOK);

    buttonOK.addActionListener(e -> onOK());

    // call onCancel() when cross is clicked
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        onOK();
      }
    });

    // call onCancel() on ESCAPE
    contentPane.registerKeyboardAction(e -> onOK(),
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
        JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

    applicationName.setText(AppInfo.APPLICATION_NAME + " - Version: " + AppInfo.getCurrentVersion());
    openSourceInfo.setText("<html>Open Source Software available on " +
        "<font color=\"#000099\"><u>github</u></font></html>");
    versionStatus.setText("Checking for updates...");
    updateStatusProgress.setVisible(true);
    updateBtn.setVisible(false);

    openSourceInfo.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent event) {
        try {
          Desktop.getDesktop().browse(new URL(AppInfo.GITHUB_URL).toURI());
        } catch (IOException | URISyntaxException e) {
          Logger.error("Failed to open github link", e);
        }
      }
    });

    updateManager = new UpdateManager(new UpdateManager.UpdateListener() {
      @Override
      public void onUpdateFound(ReleaseInfo newRelease) {
        versionStatus.setText("There is a new version of Log Viewer available!");
        updateStatusProgress.setVisible(false);
        updateBtn.setVisible(true);
        updateBtn.setText("Update to " + newRelease.getVersionName());
        updateBtn.addActionListener(l -> onUpdate(newRelease));
      }

      @Override
      public void onUpToDate() {
        versionStatus.setText("Log Viewer is already up to date!");
        updateStatusProgress.setVisible(false);
        updateBtn.setVisible(false);
      }

      @Override
      public void onFailedToCheckForUpdate(Throwable tr) {
        versionStatus.setText("Not possible to check for updates this time");
        updateStatusProgress.setVisible(false);
        updateBtn.setVisible(false);
      }
    });

    updateManager.checkForUpdates();
  }

  private void onOK() {
    dispose();
  }

  private void onUpdate(ReleaseInfo newRelease) {
    dispose();
    UpdateAvailableDialog.showUpdateAvailableDialog(newRelease);
  }

  public static void showAboutDialog() {
    AboutDialog dialog = new AboutDialog();
    dialog.pack();
    dialog.setLocationRelativeTo(null);
    dialog.setVisible(true);
  }
}
