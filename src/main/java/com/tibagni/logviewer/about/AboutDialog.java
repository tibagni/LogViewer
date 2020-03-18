package com.tibagni.logviewer.about;

import com.tibagni.logviewer.AppInfo;
import com.tibagni.logviewer.logger.Logger;
import com.tibagni.logviewer.updates.ReleaseInfo;
import com.tibagni.logviewer.updates.UpdateAvailableDialog;
import com.tibagni.logviewer.updates.UpdateManager;
import com.tibagni.logviewer.util.layout.FontBuilder;
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder;
import com.tibagni.logviewer.util.scaling.UIScaleUtils;
import com.tibagni.logviewer.view.ButtonsPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class AboutDialog extends JDialog implements ButtonsPane.Listener {
  private ButtonsPane buttonsPane;
  private JPanel contentPane;
  private JLabel applicationName;
  private JLabel versionStatus;
  private JProgressBar updateStatusProgress;
  private JLabel openSourceInfo;
  private JButton updateBtn;

  private UpdateManager updateManager;

  public AboutDialog(JFrame owner) {
    super(owner);
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

  @Override
  public void onOk() {
    dispose();
  }

  @Override
  public void onCancel() {
    dispose();
  }

  private void onUpdate(ReleaseInfo newRelease) {
    dispose();
    UpdateAvailableDialog.showUpdateAvailableDialog(getParent(), newRelease);
  }

  public static void showAboutDialog(JFrame parent) {
    AboutDialog dialog = new AboutDialog(parent);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setVisible(true);
  }

  private void buildUi() {
    contentPane = new JPanel();
    contentPane.setLayout(new GridBagLayout());
    contentPane.setBorder(BorderFactory.createEmptyBorder(UIScaleUtils.dip(10), UIScaleUtils.dip(10),
            UIScaleUtils.dip(10),
            UIScaleUtils.dip(10)));

    buttonsPane = new ButtonsPane(ButtonsPane.ButtonsMode.OK_ONLY, this);
    contentPane.add(buttonsPane,
        new GBConstraintsBuilder()
            .withGridx(0)
            .withGridy(1)
            .withWeightx(1.0)
            .withFill(GridBagConstraints.BOTH)
            .build());

    contentPane.add(buildInfoPane(),
        new GBConstraintsBuilder()
            .withGridx(0)
            .withGridy(0)
            .withWeightx(1.0)
            .withWeighty(1.0)
            .withFill(GridBagConstraints.BOTH)
            .build());
  }

  private JPanel buildInfoPane() {
    final JPanel infoPane = new JPanel();
    infoPane.setLayout(new GridBagLayout());

    applicationName = new JLabel();
    applicationName.setFont(new FontBuilder(applicationName)
        .withStyle(Font.BOLD)
        .withSize(UIScaleUtils.scaleFont(20))
        .build());
    applicationName.setText("");
    infoPane.add(applicationName,
        new GBConstraintsBuilder()
            .withGridx(0)
            .withGridy(0)
            .withGridWidth(3)
            .withWeightx(1.0)
            .withFill(GridBagConstraints.HORIZONTAL)
            .build());


    openSourceInfo = new JLabel();
    openSourceInfo.setText("Open Source Software available on");
    infoPane.add(openSourceInfo,
        new GBConstraintsBuilder()
            .withGridx(0)
            .withGridy(1)
            .withGridWidth(2)
            .withWeightx(1.0)
            .withFill(GridBagConstraints.HORIZONTAL)
            .build());

    versionStatus = new JLabel();
    versionStatus.setText("");
    infoPane.add(versionStatus,
        new GBConstraintsBuilder()
            .withGridx(0)
            .withGridy(2)
            .withGridWidth(2)
            .withWeightx(1.0)
            .withFill(GridBagConstraints.HORIZONTAL)
            .build());

    updateStatusProgress = new JProgressBar();
    updateStatusProgress.setIndeterminate(true);
    infoPane.add(updateStatusProgress,
        new GBConstraintsBuilder()
            .withGridx(0)
            .withGridy(4)
            .withWeightx(1.0)
            .withFill(GridBagConstraints.HORIZONTAL)
            .build());

    final JLabel aboutImage = new JLabel();
    aboutImage.setIcon(new ImageIcon(getClass().getResource("/Images/about.png")));
    aboutImage.setText("");
    infoPane.add(aboutImage,
        new GBConstraintsBuilder()
            .withGridx(0)
            .withGridy(5)
            .withWeightx(1.0)
            .withWeighty(1.0)
            .withFill(GridBagConstraints.HORIZONTAL)
            .build());

    updateBtn = new JButton();
    updateBtn.setText("Update");
    GridBagConstraints constraints = new GBConstraintsBuilder()
            .withGridx(0)
            .withGridy(3)
            .withWeightx(1.0)
            .withFill(GridBagConstraints.HORIZONTAL)
            .build();
    constraints.insets = new Insets(UIScaleUtils.dip(5),0, UIScaleUtils.dip(5),0);
    infoPane.add(updateBtn, constraints);

    return infoPane;
  }
}
