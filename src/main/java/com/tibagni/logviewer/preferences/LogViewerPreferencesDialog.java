package com.tibagni.logviewer.preferences;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.tibagni.logviewer.ServiceLocator;
import com.tibagni.logviewer.theme.LogViewerThemeManager;
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
import java.util.Map;

public class LogViewerPreferencesDialog extends JDialog implements ButtonsPane.Listener {
  private static final String FILTER_PATH_PREF_ID = "filter_path";
  private static final String LAST_FILTER_OPEN_ID = "open_last_filter";
  private static final String LOG_PATH_PREF_ID = "log_path";
  private static final String LOOK_FEEL_PREF_ID = "look_and_feel";
  private static final String APPLY_FILTER_EDIT_ID = "apply_filter_edit";
  private static final String REMEMBER_APPLIED_FILTERS_ID = "remember_applied_filters";
  private static final String PREFERRED_TEXT_EDITOR_ID = "preferred_text_editor";
  private static final String COLLAPSE_ALL_GROUPS_STARTUP_ID = "collapse_all_groups_startup";
  private static final String SHOW_LINE_NUMBERS_ID = "show_line_numbers";
  private static final String GLOBAL_CUSTOM_FONT_ID = "global_custom_font";

  private static final int[] SUPPORTED_FONT_SIZE_ARRAY = {
      8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 36, 48, 72
  };

  private ButtonsPane buttonsPane;
  private JPanel contentPane;
  private JComboBox<String> lookAndFeelCbx;
  private JComboBox<String> customFontCbx;
  private JComboBox<String> customFontSizeCbx;
  private JTextField filtersPathTxt;
  private JButton filtersPathBtn;
  private JCheckBox openLastFilterChbx;
  private JTextField logsPathTxt;
  private JButton logsPathBtn;
  private JCheckBox applyFiltersAfterEditChbx;
  private JCheckBox rememberAppliedFiltersChbx;
  private JCheckBox collapseAllGroupsStartup;
  private JCheckBox showLineNumbersChbx;
  private JTextField preferredEditorPathTxt;
  private JButton preferredEditorPathBtn;

  private JFileChooser filterFolderChooser;
  private JFileChooser logsFolderChooser;
  private JFileChooser preferredEditorFileChooser;
  private final LogViewerPreferences userPrefs;
  private final LogViewerThemeManager themeManager;

  private final Map<String, Runnable> saveActions = new HashMap<>();

  public LogViewerPreferencesDialog(JFrame owner) {
    super(owner);
    buildUi();
    setContentPane(contentPane);
    setModal(true);
    buttonsPane.setDefaultButtonOk();
    userPrefs = ServiceLocator.INSTANCE.getLogViewerPrefs();
    themeManager = ServiceLocator.INSTANCE.getThemeManager();

    initFiltersPathPreference();
    initLogsPathPreference();
    initLookAndFeelPreference();
    initCustomFontPreference();
    initPreferredEditorPathPreference();

    // Adjust the size according to the content after everything is populated
    contentPane.setPreferredSize(contentPane.getPreferredSize());
    contentPane.validate();

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

    collapseAllGroupsStartup.addActionListener(e -> onCollapseAllGroupsOnStartupChanged());
    collapseAllGroupsStartup.setSelected(userPrefs.getCollapseAllGroupsStartup());

    showLineNumbersChbx.addActionListener(e -> onShowLineNumbersChanged());
    showLineNumbersChbx.setSelected(userPrefs.getShowLineNumbers());
  }

  private void initLogsPathPreference() {
    logsFolderChooser = new JFileChooserExt(userPrefs.getDefaultLogsPath());
    logsFolderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

    logsPathBtn.addActionListener(e -> onSelectLogsPath());
    logsPathTxt.setText(userPrefs.getDefaultLogsPath().getAbsolutePath());
  }

  private void initLookAndFeelPreference() {
    for (String theme : themeManager.getAvailableThemes()) {
      lookAndFeelCbx.addItem(theme);
    }
    lookAndFeelCbx.setSelectedItem(themeManager.getCurrentTheme());

    lookAndFeelCbx.addActionListener(l -> {
      String theme = (String) lookAndFeelCbx.getSelectedItem();
      if (theme != null) {
        saveActions.put(LOOK_FEEL_PREF_ID, () -> userPrefs.setLookAndFeel(theme));
      }
    });
  }

  private void initCustomFontPreference() {
    Runnable onFontUpdate = () -> saveActions.put(GLOBAL_CUSTOM_FONT_ID, () -> userPrefs.setGlobalCustomFont(
        new Font((String) customFontCbx.getSelectedItem(),
            Font.PLAIN,
            Integer.parseInt((String) customFontSizeCbx.getSelectedItem()))
    ));

    for (String font : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
      customFontCbx.addItem(font);
    }
    customFontCbx.setSelectedItem(userPrefs.getGlobalCustomFont().getFamily());
    customFontCbx.addActionListener(l -> onFontUpdate.run());

    for (int size : SUPPORTED_FONT_SIZE_ARRAY) {
      customFontSizeCbx.addItem(String.valueOf(size));
    }
    customFontSizeCbx.setSelectedItem(String.valueOf(userPrefs.getGlobalCustomFont().getSize()));
    customFontSizeCbx.addActionListener(l -> onFontUpdate.run());
  }

  private void initPreferredEditorPathPreference() {
    preferredEditorFileChooser = new JFileChooserExt(userPrefs.getPreferredTextEditor());

    preferredEditorPathBtn.addActionListener(e -> onSelectPreferredEditorPath());
    File editorFile = userPrefs.getPreferredTextEditor();
    String path = editorFile != null ? editorFile.getAbsolutePath() : null;
    preferredEditorPathTxt.setText(path);
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

  private void onCollapseAllGroupsOnStartupChanged() {
    boolean isChecked = collapseAllGroupsStartup.getModel().isSelected();
    saveActions.put(COLLAPSE_ALL_GROUPS_STARTUP_ID, () -> userPrefs.setCollapseAllGroupsStartup(isChecked));
  }

  private void onShowLineNumbersChanged() {
    boolean isChecked = showLineNumbersChbx.getModel().isSelected();
    saveActions.put(SHOW_LINE_NUMBERS_ID, () -> userPrefs.setShowLineNumbers(isChecked));
  }

  private void onSelectPreferredEditorPath() {
    int selectedOption = preferredEditorFileChooser.showOpenDialog(this);
    if (selectedOption == JFileChooser.APPROVE_OPTION) {
      File selectedFolder = preferredEditorFileChooser.getSelectedFile();
      preferredEditorPathTxt.setText(selectedFolder.getAbsolutePath());
      saveActions.put(PREFERRED_TEXT_EDITOR_ID,
          () -> userPrefs.setPreferredTextEditor(selectedFolder));
    }
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
        "center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow"));


    final JLabel lookNFeelLbl = new JLabel();
    lookNFeelLbl.setText("Look And Feel");
    CellConstraints cc = new CellConstraints();
    formPane.add(lookNFeelLbl, cc.xy(1, 1));
    lookAndFeelCbx = new JComboBox<>();
    lookAndFeelCbx.setMinimumSize(new Dimension());
    formPane.add(lookAndFeelCbx, cc.xy(3, 1));

    final JLabel customFontLbl = new JLabel();
    customFontLbl.setText("Custom Font");
    formPane.add(customFontLbl, cc.xy(1, 3));
    customFontCbx = new JComboBox<>();
    customFontCbx.setRenderer(new CustomFontCellRender());
    customFontCbx.setMinimumSize(new Dimension());
    formPane.add(customFontCbx, cc.xy(3, 3));
    customFontSizeCbx = new JComboBox<>();
    customFontSizeCbx.setMinimumSize(new Dimension());
    formPane.add(customFontSizeCbx, cc.xy(5, 3));

    final JSeparator sep1 = new JSeparator();
    formPane.add(sep1, cc.xyw(1, 5, 3, CellConstraints.FILL, CellConstraints.DEFAULT));

    final JLabel defaultLogsLbl = new JLabel();
    defaultLogsLbl.setText("Default path for log files");
    formPane.add(defaultLogsLbl, cc.xy(1, 7));
    logsPathTxt = new JTextField();
    logsPathTxt.setEditable(false);
    formPane.add(logsPathTxt, cc.xy(3, 7, CellConstraints.FILL, CellConstraints.DEFAULT));
    logsPathBtn = new JButton();
    logsPathBtn.setText("...");
    formPane.add(logsPathBtn, cc.xy(5, 7));

    final JSeparator sep2 = new JSeparator();
    formPane.add(sep2, cc.xyw(1, 8, 3, CellConstraints.FILL, CellConstraints.DEFAULT));

    final JLabel defaultFiltersLbl = new JLabel();
    defaultFiltersLbl.setText("Default path for filter files");
    formPane.add(defaultFiltersLbl, cc.xy(1, 11));
    filtersPathTxt = new JTextField();
    filtersPathTxt.setEditable(false);
    formPane.add(filtersPathTxt, cc.xy(3, 11, CellConstraints.FILL, CellConstraints.DEFAULT));
    filtersPathBtn = new JButton();
    filtersPathBtn.setText("...");
    formPane.add(filtersPathBtn, cc.xy(5, 11));

    final JLabel openLastLbl = new JLabel();
    openLastLbl.setText("Open last filters on startup");
    formPane.add(openLastLbl, cc.xy(1, 13));
    openLastFilterChbx = new JCheckBox();
    openLastFilterChbx.setText("");
    formPane.add(openLastFilterChbx, cc.xy(3, 13));

    final JSeparator sep3 = new JSeparator();
    formPane.add(sep3, cc.xyw(1, 15, 3, CellConstraints.FILL, CellConstraints.DEFAULT));

    final JLabel applyFiltersLbl = new JLabel();
    applyFiltersLbl.setText("Apply filters after edit");
    formPane.add(applyFiltersLbl, cc.xy(1, 17));
    applyFiltersAfterEditChbx = new JCheckBox();
    applyFiltersAfterEditChbx.setText("");
    formPane.add(applyFiltersAfterEditChbx, cc.xy(3, 17));

    final JLabel rememberFiltersLbl = new JLabel();
    rememberFiltersLbl.setText("Remember applied filters");
    formPane.add(rememberFiltersLbl, cc.xy(1, 19));
    rememberAppliedFiltersChbx = new JCheckBox();
    rememberAppliedFiltersChbx.setText("");
    formPane.add(rememberAppliedFiltersChbx, cc.xy(3, 19));

    final JLabel collapseOnStartLbl = new JLabel();
    collapseOnStartLbl.setText("Collapse all groups on startup");
    formPane.add(collapseOnStartLbl, cc.xy(1, 21));
    collapseAllGroupsStartup = new JCheckBox();
    collapseAllGroupsStartup.setText("");
    formPane.add(collapseAllGroupsStartup, cc.xy(3, 21));

    final JLabel showLineNumberLbl = new JLabel();
    showLineNumberLbl.setText("Show Line numbers");
    formPane.add(showLineNumberLbl, cc.xy(1, 23));
    showLineNumbersChbx = new JCheckBox();
    showLineNumbersChbx.setText("");
    formPane.add(showLineNumbersChbx, cc.xy(3, 23));

    final JSeparator sep4 = new JSeparator();
    formPane.add(sep4, cc.xyw(1, 24, 3, CellConstraints.FILL, CellConstraints.DEFAULT));

    final JLabel preferredEditorLbl = new JLabel();
    preferredEditorLbl.setText("Preferred text Editor");
    formPane.add(preferredEditorLbl, cc.xy(1, 25));
    preferredEditorPathTxt = new JTextField();
    preferredEditorPathTxt.setEditable(false);
    formPane.add(preferredEditorPathTxt, cc.xy(3, 25, CellConstraints.FILL, CellConstraints.DEFAULT));
    preferredEditorPathBtn = new JButton();
    preferredEditorPathBtn.setText("...");
    formPane.add(preferredEditorPathBtn, cc.xy(5, 25));

    return formPane;
  }
}