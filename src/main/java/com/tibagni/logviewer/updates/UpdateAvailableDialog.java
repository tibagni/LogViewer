package com.tibagni.logviewer.updates;

import com.tibagni.logviewer.logger.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class UpdateAvailableDialog extends JDialog {
  private JPanel contentPane;
  private JButton buttonOK;
  private JButton buttonCancel;
  private JTextArea releaseInfo;

  public UpdateAvailableDialog(ReleaseInfo latestInfo) {
    setContentPane(contentPane);
    setModal(true);
    getRootPane().setDefaultButton(buttonOK);

    buttonOK.addActionListener(e -> onDownload(latestInfo.getReleaseUrl()));

    buttonCancel.addActionListener(e -> onCancel());

    // call onCancel() when cross is clicked
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        onCancel();
      }
    });

    // call onCancel() on ESCAPE
    contentPane.registerKeyboardAction(e -> onCancel(),
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
        JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

    releaseInfo.setText(getLatestReleaseText(latestInfo));
  }

  private String getLatestReleaseText(ReleaseInfo releaseInfo) {
    return "New Version: " + releaseInfo.getVersionName() +
        "\n --------------------------------------- \n" +
        "Release Notes: \n" +
        releaseInfo.getReleaseNotes();
  }

  private void onDownload(String url) {
    try {
      Desktop.getDesktop().browse(new URL(url).toURI());
    } catch (Exception e) {
      Logger.error("Failed to download new Version from URL: " + url, e);
      dispose();
    }
  }

  private void onCancel() {
    dispose();
  }


  public static void showUpdateAvailableDialog(ReleaseInfo latest) {
    UpdateAvailableDialog dialog = new UpdateAvailableDialog(latest);
    dialog.pack();
    dialog.setLocationRelativeTo(null);
    dialog.setVisible(true);
  }
}
