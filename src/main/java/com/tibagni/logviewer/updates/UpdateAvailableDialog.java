package com.tibagni.logviewer.updates;

import com.tibagni.logviewer.logger.Logger;
import com.tibagni.logviewer.util.layout.FontBuilder;
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder;
import com.tibagni.logviewer.view.ButtonsPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;

public class UpdateAvailableDialog extends JDialog implements ButtonsPane.Listener {
  private ButtonsPane buttonsPane;
  private JPanel contentPane;
  private JTextArea releaseInfo;
  private ReleaseInfo latestInfo;

  public UpdateAvailableDialog(ReleaseInfo latestInfo) {
    this.latestInfo = latestInfo;
    buildUi();

    setContentPane(contentPane);
    setModal(true);
    buttonsPane.setDefaultButtonOk();

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

  @Override
  public void onOk() {
    onDownload(latestInfo.getReleaseUrl());
  }

  @Override
  public void onCancel() {
    dispose();
  }

  public static void showUpdateAvailableDialog(ReleaseInfo latest) {
    showUpdateAvailableDialog(null, latest);
  }

  public static void showUpdateAvailableDialog(Component relativeTo, ReleaseInfo latest) {
    UpdateAvailableDialog dialog = new UpdateAvailableDialog(latest);
    dialog.pack();
    dialog.setLocationRelativeTo(relativeTo);
    dialog.setVisible(true);
  }

  private void buildUi() {
    contentPane = new JPanel();
    contentPane.setLayout(new GridBagLayout());
    contentPane.setMinimumSize(new Dimension(550, 250));
    contentPane.setPreferredSize(new Dimension(550, 250));
    contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    buttonsPane = new ButtonsPane(ButtonsPane.ButtonsMode.OK_CANCEL, this);
    contentPane.add(buttonsPane,
        new GBConstraintsBuilder()
            .withGridx(0)
            .withGridy(1)
            .withWeightx(1.0)
            .withFill(GridBagConstraints.BOTH)
            .build());

    contentPane.add(buildReleaseInfoPane(),
        new GBConstraintsBuilder()
            .withGridx(0)
            .withGridy(0)
            .withWeightx(1.0)
            .withWeighty(1.0)
            .withFill(GridBagConstraints.BOTH)
            .build());
  }

  private JPanel buildReleaseInfoPane() {
    final JPanel releaseInfoPane = new JPanel();
    releaseInfoPane.setLayout(new GridBagLayout());
    releaseInfoPane.setAutoscrolls(false);
    final JLabel title = new JLabel();
    title.setText("There is a new version of LogViewer available for Download!");
    title.setFont(new FontBuilder(title).withStyle(Font.BOLD).build());
    releaseInfoPane.add(title,
        new GBConstraintsBuilder()
            .withGridx(0)
            .withGridy(0)
            .withWeightx(1.0)
            .withFill(GridBagConstraints.HORIZONTAL)
            .build());

    releaseInfo = new JTextArea();
    releaseInfo.setEditable(false);
    releaseInfoPane.add(new JScrollPane(releaseInfo),
        new GBConstraintsBuilder()
            .withGridx(0)
            .withGridy(1)
            .withWeightx(1.0)
            .withWeighty(1.0)
            .withFill(GridBagConstraints.BOTH)
            .build());

    return releaseInfoPane;
  }
}
