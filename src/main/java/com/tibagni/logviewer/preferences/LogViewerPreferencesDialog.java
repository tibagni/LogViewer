package com.tibagni.logviewer.preferences;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.tibagni.logviewer.lookandfeel.LookNFeelProvider;
import com.tibagni.logviewer.lookandfeel.LookNFeel;
import com.tibagni.logviewer.util.scaling.UIScaleUtils;
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder;
import com.tibagni.logviewer.view.ButtonsPane;
import com.tibagni.logviewer.view.JFileChooserExt;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogViewerPreferencesDialog extends JDialog implements ButtonsPane.Listener {
  private static final String FILTER_PATH_PREF_ID = "filter_path";
  private static final String LAST_FILTER_OPEN_ID = "open_last_filter";
  private static final String LOG_PATH_PREF_ID = "log_path";
  private static final String LOOK_FEEL_PREF_ID = "look_and_feel";
  private static final String APPLY_FILTER_EDIT_ID = "apply_filter_edit";
  private static final String REMEMBER_APPLIED_FILTERS_ID = "remember_applied_filters";

  private ButtonsPane buttonsPane;
  private JPanel contentPane;
  private JComboBox lookAndFeelCbx;
  private JTextField filtersPathTxt;
  private JButton filtersPathBtn;
  private JCheckBox openLastFilterChbx;
  private JTextField logsPathTxt;
  private JButton logsPathBtn;
  private JCheckBox applyFiltersAfterEditChbx;
  private JCheckBox rememberAppliedFiltersChbx;

  private JFileChooser filterFolderChooser;
  private JFileChooser logsFolderChooser;
  private final LogViewerPreferences userPrefs;

  private Map<String, Runnable> saveActions = new HashMap<>();

  public LogViewerPreferencesDialog(JFrame owner) {
    super(owner);
    buildUi();
    setContentPane(contentPane);
    setModal(true);
    buttonsPane.setDefaultButtonOk();
    userPrefs = LogViewerPreferencesImpl.INSTANCE;

    initFiltersPathPreference();
    initLogsPathPreference();
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
    filterFolderChooser = new JFileChooserExt(userPrefs.getDefaultFiltersPath());
    filterFolderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

    filtersPathBtn.addActionListener(e -> onSelectFilterPath());
    filtersPathTxt.setText(userPrefs.getDefaultFiltersPath().getAbsolutePath());

    openLastFilterChbx.addActionListener(e -> onOpenLastFilterChanged());
    openLastFilterChbx.setSelected(userPrefs.getOpenLastFilter());

    applyFiltersAfterEditChbx.addActionListener(e -> onApplyFiltersAfterEditChanged());
    applyFiltersAfterEditChbx.setSelected(userPrefs.getReapplyFiltersAfterEdit());

    rememberAppliedFiltersChbx.addActionListener(e -> onRememberAppliedFiltersChanged());
    rememberAppliedFiltersChbx.setSelected(userPrefs.getRememberAppliedFilters());
  }

  private void initLogsPathPreference() {
    logsFolderChooser = new JFileChooserExt(userPrefs.getDefaultLogsPath());
    logsFolderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

    logsPathBtn.addActionListener(e -> onSelectLogsPath());
    logsPathTxt.setText(userPrefs.getDefaultLogsPath().getAbsolutePath());
  }

  private void initLookAndFeelPreference() {
    LookNFeelProvider lnfProvider = LookNFeelProvider.getInstance();
    List<LookNFeel> lookAndFeels = lnfProvider.getAvailableLookNFeels();

    String currLnf = UIManager.getLookAndFeel().getClass().getName();
    LookNFeel selectedItem = lnfProvider.getByClass(currLnf);
    for (LookNFeel lnf : lookAndFeels) {
      lookAndFeelCbx.addItem(lnf);
    }

    if (selectedItem != null) {
      lookAndFeelCbx.setSelectedItem(selectedItem);
    }

    lookAndFeelCbx.addActionListener(l -> {
      LookNFeel lookNFeel = (LookNFeel) lookAndFeelCbx.getSelectedItem();
      saveActions.put(LOOK_FEEL_PREF_ID, () -> {
        String lnfClass = lookNFeel.getCls();
        userPrefs.setLookAndFeel(lnfClass);
      });
    });
  }

  @Override
  public void onOk() {
    saveActions.forEach((s, runnable) -> runnable.run());
    dispose();
  }

  @Override
  public void onCancel() {
    dispose();
  }

  private void onSelectFilterPath() {
    int selectedOption = filterFolderChooser.showOpenDialog(this);
    if (selectedOption == JFileChooser.APPROVE_OPTION) {
      File selectedFolder = filterFolderChooser.getSelectedFile();
      filtersPathTxt.setText(selectedFolder.getAbsolutePath());
      saveActions.put(FILTER_PATH_PREF_ID,
          () -> userPrefs.setDefaultFiltersPath(selectedFolder));
    }
  }

  private void onSelectLogsPath() {
    int selectedOption = logsFolderChooser.showOpenDialog(this);
    if (selectedOption == JFileChooser.APPROVE_OPTION) {
      File selectedFolder = logsFolderChooser.getSelectedFile();
      logsPathTxt.setText(selectedFolder.getAbsolutePath());
      saveActions.put(LOG_PATH_PREF_ID,
          () -> userPrefs.setDefaultLogsPath(selectedFolder));
    }
  }

  private void onOpenLastFilterChanged() {
    boolean isChecked = openLastFilterChbx.getModel().isSelected();
    saveActions.put(LAST_FILTER_OPEN_ID, () -> userPrefs.setOpenLastFilter(isChecked));
  }

  private void onApplyFiltersAfterEditChanged() {
    boolean isChecked = applyFiltersAfterEditChbx.getModel().isSelected();
    saveActions.put(APPLY_FILTER_EDIT_ID, () -> userPrefs.setReapplyFiltersAfterEdit(isChecked));
  }

  private void onRememberAppliedFiltersChanged() {
    boolean isChecked = rememberAppliedFiltersChbx.getModel().isSelected();
    saveActions.put(REMEMBER_APPLIED_FILTERS_ID, () -> userPrefs.setRememberAppliedFilters(isChecked));
  }

  public static void showPreferencesDialog(JFrame parent) {
    LogViewerPreferencesDialog dialog = new LogViewerPreferencesDialog(parent);

    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setVisible(true);
  }

  private void buildUi() {
    contentPane = new JPanel();
    contentPane.setLayout(new GridBagLayout());
    contentPane.setMinimumSize(new Dimension(UIScaleUtils.dip(400), UIScaleUtils.dip(250)));
    contentPane.setPreferredSize(new Dimension(UIScaleUtils.dip(600), UIScaleUtils.dip(350)));
    contentPane.setRequestFocusEnabled(true);
    contentPane.setBorder(BorderFactory.createEmptyBorder(UIScaleUtils.dip(10),
            UIScaleUtils.dip(10),
            UIScaleUtils.dip(10),
            UIScaleUtils.dip(10)));

    buttonsPane = new ButtonsPane(ButtonsPane.ButtonsMode.OK_CANCEL, this);
    contentPane.add(buttonsPane,
        new GBConstraintsBuilder()
            .withGridx(0)
            .withGridy(1)
            .withWeightx(1.0)
            .withFill(GridBagConstraints.BOTH)
            .build());


    contentPane.add(buildFormPane(),
        new GBConstraintsBuilder()
            .withGridx(0)
            .withGridy(0)
            .withWeightx(1.0)
            .withWeighty(1.0)
            .withFill(GridBagConstraints.BOTH)
            .build());
  }

  private JPanel buildFormPane() {
    final JPanel formPane = new JPanel();
    formPane.setLayout(new FormLayout(
        "fill:d:grow,left:4dlu:noGrow,fill:d:grow,left:4dlu:noGrow,fill:d:grow",
        "center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow"));


    final JLabel lookNFeelLbl = new JLabel();
    lookNFeelLbl.setText("Look And Feel");
    CellConstraints cc = new CellConstraints();
    formPane.add(lookNFeelLbl, cc.xy(1, 1));
    lookAndFeelCbx = new JComboBox();
    formPane.add(lookAndFeelCbx, cc.xy(3, 1));

    final JSeparator sep1 = new JSeparator();
    formPane.add(sep1, cc.xyw(1, 3, 3, CellConstraints.FILL, CellConstraints.DEFAULT));

    final JLabel defaultLogsLbl = new JLabel();
    defaultLogsLbl.setText("Default path for log files");
    formPane.add(defaultLogsLbl, cc.xy(1, 5));
    logsPathTxt = new JTextField();
    logsPathTxt.setEditable(false);
    formPane.add(logsPathTxt, cc.xy(3, 5, CellConstraints.FILL, CellConstraints.DEFAULT));
    logsPathBtn = new JButton();
    logsPathBtn.setText("...");
    formPane.add(logsPathBtn, cc.xy(5, 5));

    final JSeparator sep2 = new JSeparator();
    formPane.add(sep2, cc.xyw(1, 7, 3, CellConstraints.FILL, CellConstraints.DEFAULT));

    final JLabel defaultFiltersLbl = new JLabel();
    defaultFiltersLbl.setText("Default path for filter files");
    formPane.add(defaultFiltersLbl, cc.xy(1, 9));
    filtersPathTxt = new JTextField();
    filtersPathTxt.setEditable(false);
    formPane.add(filtersPathTxt, cc.xy(3, 9, CellConstraints.FILL, CellConstraints.DEFAULT));
    filtersPathBtn = new JButton();
    filtersPathBtn.setText("...");
    formPane.add(filtersPathBtn, cc.xy(5, 9));

    final JLabel openLastLbl = new JLabel();
    openLastLbl.setText("Open last filters on startup");
    formPane.add(openLastLbl, cc.xy(1, 11));
    openLastFilterChbx = new JCheckBox();
    openLastFilterChbx.setText("");
    formPane.add(openLastFilterChbx, cc.xy(3, 11));

    final JSeparator sep3 = new JSeparator();
    formPane.add(sep3, cc.xyw(1, 13, 3, CellConstraints.FILL, CellConstraints.DEFAULT));

    final JLabel applyFiltersLbl = new JLabel();
    applyFiltersLbl.setText("Apply filters after edit");
    formPane.add(applyFiltersLbl, cc.xy(1, 15));
    applyFiltersAfterEditChbx = new JCheckBox();
    applyFiltersAfterEditChbx.setText("");
    formPane.add(applyFiltersAfterEditChbx, cc.xy(3, 15));

    final JLabel rememberFiltersLbl = new JLabel();
    rememberFiltersLbl.setText("Remember applied filters");
    formPane.add(rememberFiltersLbl, cc.xy(1, 17));
    rememberAppliedFiltersChbx = new JCheckBox();
    rememberAppliedFiltersChbx.setText("");
    formPane.add(rememberAppliedFiltersChbx, cc.xy(3, 17));

    return formPane;
  }
}