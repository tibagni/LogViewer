package com.tibagni.logviewer.preferences;

import com.tibagni.logviewer.util.JFileChooserExt;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LogViewerPreferencesDialog extends JDialog {
  private static final String FILTER_PATH_PREF_ID = "filter_path";
  private static final String LAST_FILTER_OPEN_ID = "open_last_filter";
  private static final String LOOK_FEEL_PREF_ID = "look_and_feel";

  private JPanel contentPane;
  private JButton buttonOK;
  private JButton buttonCancel;
  private JComboBox lookAndFeelCbx;
  private JTextField filtersPathTxt;
  private JButton filtersPathBtn;
  private JCheckBox openLastFilterChbx;

  private JFileChooser folderChooser;
  private final LogViewerPreferences userPrefs;

  private Map<String, Runnable> saveActions = new HashMap<>();

  public LogViewerPreferencesDialog() {
    setContentPane(contentPane);
    setModal(true);
    getRootPane().setDefaultButton(buttonOK);
    userPrefs = LogViewerPreferences.getInstance();

    buttonOK.addActionListener(e -> onOK());
    buttonCancel.addActionListener(e -> onCancel());

    initFiltersPathPreference();
    initLookAndFeelPreference();

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
  }

  private void initFiltersPathPreference() {
    folderChooser = new JFileChooserExt(userPrefs.getDefaultFiltersPath());
    folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

    filtersPathBtn.addActionListener(e -> onSelectFilterPath());
    filtersPathTxt.setText(userPrefs.getDefaultFiltersPath().getAbsolutePath());

    openLastFilterChbx.addActionListener(e -> onOpenLastFilterChanged());
    openLastFilterChbx.setSelected(userPrefs.shouldOpenLastFilter());
  }

  private void initLookAndFeelPreference() {
    String currLnf = UIManager.getLookAndFeel().getName();
    LookAndFeel selectedItem = null;
    for (UIManager.LookAndFeelInfo lnf : UIManager.getInstalledLookAndFeels()) {
      LookAndFeel item = new LookAndFeel(lnf);
      lookAndFeelCbx.addItem(item);

      if (currLnf != null && currLnf.equals(lnf.getName())) {
        selectedItem = item;
      }
    }
    if (selectedItem != null) {
      lookAndFeelCbx.setSelectedItem(selectedItem);
    }

    lookAndFeelCbx.addActionListener(l -> {
      LookAndFeel lookAndFeel = (LookAndFeel) lookAndFeelCbx.getSelectedItem();
      saveActions.put(LOOK_FEEL_PREF_ID, () -> {
        String lnfClass = lookAndFeel.lnfInfo.getClassName();
        userPrefs.setLookAndFeel(lnfClass);
      });
    });
  }

  private void onOK() {
    saveActions.forEach((s, runnable) -> runnable.run());
    dispose();
  }

  private void onCancel() {
    dispose();
  }

  private void onSelectFilterPath() {
    int selectedOption = folderChooser.showOpenDialog(this);
    if (selectedOption == JFileChooser.APPROVE_OPTION) {
      File selectedFolder = folderChooser.getSelectedFile();
      filtersPathTxt.setText(selectedFolder.getAbsolutePath());
      saveActions.put(FILTER_PATH_PREF_ID,
          () -> userPrefs.setDefaultFiltersPath(selectedFolder));
    }
  }

  private void onOpenLastFilterChanged() {
    boolean isChecked = openLastFilterChbx.getModel().isSelected();
    saveActions.put(LAST_FILTER_OPEN_ID, () -> userPrefs.setOpenLastFilter(isChecked));
  }

  private class LookAndFeel {
    UIManager.LookAndFeelInfo lnfInfo;

    LookAndFeel(UIManager.LookAndFeelInfo lnfInfo) {
      this.lnfInfo = lnfInfo;
    }

    @Override
    public String toString() {
      return lnfInfo.getName();
    }
  }

  public static void showPreferencesDialog(Component relativeTo) {
    LogViewerPreferencesDialog dialog = new LogViewerPreferencesDialog();

    dialog.pack();
    dialog.setLocationRelativeTo(relativeTo);
    dialog.setVisible(true);
  }
}
