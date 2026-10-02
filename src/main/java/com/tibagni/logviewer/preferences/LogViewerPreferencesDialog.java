package com.tibagni.logviewer.preferences;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.tibagni.logviewer.ServiceLocator;
import com.tibagni.logviewer.i18n.I18n;
import com.tibagni.logviewer.theme.LogViewerThemeManager;
import com.tibagni.logviewer.util.scaling.UIScaleUtils;
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder;
import com.tibagni.logviewer.view.ButtonsPane;
import com.tibagni.logviewer.view.JFileChooserExt;
import com.tibagni.logviewer.view.LoadingSpinner;

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
  private static final String SHOW_LOG_DIVIDERS_ID = "show_log_dividers";
  private static final String APPLY_FILTER_CHECK_ID = "apply_filter_check";
  private static final String LOG_FONT_SIZE_PREF_ID = "log_font_size";
  private static final String LOG_FONT_BOLD_PREF_ID = "log_font_bold";

  private static final Integer[] SUPPORTED_FONT_SIZES = {
      8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 32
  };

  private ButtonsPane buttonsPane;
  private JPanel contentPane;
  private JComboBox<String> lookAndFeelCbx;
  private LoadingSpinner themeLoadingSpinner;
  private JComboBox<Integer> logFontSizeCbx;
  private JCheckBox boldLogTextChbx;
  private JTextField filtersPathTxt;
  private JButton filtersPathBtn;
  private JCheckBox openLastFilterChbx;
  private JTextField logsPathTxt;
  private JButton logsPathBtn;
  private JCheckBox applyFiltersAfterEditChbx;
  private JCheckBox rememberAppliedFiltersChbx;
  private JCheckBox collapseAllGroupsStartup;
  private JCheckBox showLineNumbersChbx;
  private JCheckBox showLogDividersChbx;
  private JTextField preferredEditorPathTxt;
  private JButton preferredEditorPathBtn;
  private JCheckBox applyFiltersOnCheckChbx;

  private JFileChooser filterFolderChooser;
  private JFileChooser logsFolderChooser;
  private JFileChooser preferredEditorFileChooser;
  private final LogViewerPreferences userPrefs;
  private final LogViewerThemeManager themeManager;

  private final Map<String, Runnable> saveActions = new HashMap<>();

  public LogViewerPreferencesDialog(JFrame owner) {
    super(owner);
    setTitle(I18n.get(I18n.PREF_DIALOG_TITLE));
    buildUi();
    setContentPane(contentPane);
    setModal(true);
    buttonsPane.setDefaultButtonOk();
    userPrefs = ServiceLocator.INSTANCE.getLogViewerPrefs();
    themeManager = ServiceLocator.INSTANCE.getThemeManager();

    initFiltersPathPreference();
    initLogsPathPreference();
    initLookAndFeelPreference();
    initLogFontPreference();
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

    showLogDividersChbx.addActionListener(e -> onShowLogDividersChanged());
    showLogDividersChbx.setSelected(userPrefs.getShowLogLineDividers());

    applyFiltersOnCheckChbx.addActionListener(e -> onApplyFiltersOnCheckChanged());
    applyFiltersOnCheckChbx.setSelected(userPrefs.getApplyFilterOnCheck());
  }

  private void initLogsPathPreference() {
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

  private void initLogFontPreference() {
    for (int size : SUPPORTED_FONT_SIZES) {
      logFontSizeCbx.addItem(size);
    }
    logFontSizeCbx.setSelectedItem(userPrefs.getLogFontSize());
    logFontSizeCbx.addActionListener(e -> {
      Integer selectedSize = (Integer) logFontSizeCbx.getSelectedItem();
      if (selectedSize != null) {
        saveActions.put(LOG_FONT_SIZE_PREF_ID, () -> userPrefs.setLogFontSize(selectedSize));
      }
    });

    boldLogTextChbx.setSelected(userPrefs.getLogFontBold());
    boldLogTextChbx.addActionListener(e -> {
      boolean isChecked = boldLogTextChbx.isSelected();
      saveActions.put(LOG_FONT_BOLD_PREF_ID, () -> userPrefs.setLogFontBold(isChecked));
    });
  }

  private void initPreferredEditorPathPreference() {
    preferredEditorPathBtn.addActionListener(e -> onSelectPreferredEditorPath());
    File editorFile = userPrefs.getPreferredTextEditor();
    String path = editorFile != null ? editorFile.getAbsolutePath() : null;
    preferredEditorPathTxt.setText(path);
  }

  @Override
  public void onOk() {
    if (saveActions.isEmpty()) {
      dispose();
      return;
    }

    // Theme change operations can take a little longer (e.g. recreating file choosers so
    // file dialogs stay instant), so we show a loading spinner and wait cursor to let the
    // user know that work is in progress.
    boolean hasThemeChange = saveActions.containsKey(LOOK_FEEL_PREF_ID);

    buttonsPane.enableOkButton(false);
    buttonsPane.enableCancelButton(false);

    if (hasThemeChange) {
      setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      if (themeLoadingSpinner != null) {
        themeLoadingSpinner.setVisible(true);
      }

      // Allow the EDT to paint the loading spinner and wait cursor before running theme change
      Timer timer = new Timer(20, e -> {
        try {
          saveActions.forEach((s, runnable) -> runnable.run());
        } finally {
          if (themeLoadingSpinner != null) {
            themeLoadingSpinner.setVisible(false);
          }
          setCursor(Cursor.getDefaultCursor());
          dispose();
        }
      });
      timer.setRepeats(false);
      timer.start();
    } else {
      saveActions.forEach((s, runnable) -> runnable.run());
      dispose();
    }
  }

  @Override
  public void onCancel() {
    dispose();
  }

  private void onSelectFilterPath() {
    if (filterFolderChooser == null) {
      filterFolderChooser = new JFileChooserExt(userPrefs.getDefaultFiltersPath());
      filterFolderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    }

    int selectedOption = filterFolderChooser.showOpenDialog(this);
    if (selectedOption == JFileChooser.APPROVE_OPTION) {
      File selectedFolder = filterFolderChooser.getSelectedFile();
      filtersPathTxt.setText(selectedFolder.getAbsolutePath());
      saveActions.put(FILTER_PATH_PREF_ID,
          () -> userPrefs.setDefaultFiltersPath(selectedFolder));
    }
  }

  private void onSelectLogsPath() {
    if (logsFolderChooser == null) {
      logsFolderChooser = new JFileChooserExt(userPrefs.getDefaultLogsPath());
      logsFolderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    }

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

  private void onShowLogDividersChanged() {
    boolean isChecked = showLogDividersChbx.getModel().isSelected();
    saveActions.put(SHOW_LOG_DIVIDERS_ID, () -> userPrefs.setShowLogLineDividers(isChecked));
  }

  private void onSelectPreferredEditorPath() {
    if (preferredEditorFileChooser == null) {
      preferredEditorFileChooser = new JFileChooserExt(userPrefs.getPreferredTextEditor());
    }

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

  /**
   * This method is called when the "Apply filters on check" checkbox is toggled.
   */
  private void onApplyFiltersOnCheckChanged() {
    boolean isChecked = applyFiltersOnCheckChbx.getModel().isSelected();
    saveActions.put(APPLY_FILTER_CHECK_ID, () -> userPrefs.setApplyFilterOnCheck(isChecked));
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
        "center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow,top:3dlu:noGrow,center:d:grow"));


    final JLabel lookNFeelLbl = new JLabel();
    lookNFeelLbl.setText(I18n.get(I18n.PREF_LOOK_AND_FEEL));
    CellConstraints cc = new CellConstraints();
    formPane.add(lookNFeelLbl, cc.xy(1, 1));
    lookAndFeelCbx = new JComboBox<>();
    lookAndFeelCbx.setMinimumSize(new Dimension());
    formPane.add(lookAndFeelCbx, cc.xy(3, 1));

    themeLoadingSpinner = new LoadingSpinner();
    themeLoadingSpinner.setVisible(false);
    formPane.add(themeLoadingSpinner, cc.xy(5, 1, CellConstraints.LEFT, CellConstraints.CENTER));

    final JSeparator sep1 = new JSeparator();
    formPane.add(sep1, cc.xyw(1, 3, 3, CellConstraints.FILL, CellConstraints.DEFAULT));

    final JLabel logFontSizeLbl = new JLabel();
    logFontSizeLbl.setText(I18n.get(I18n.PREF_LOG_FONT_SIZE));
    formPane.add(logFontSizeLbl, cc.xy(1, 5));
    logFontSizeCbx = new JComboBox<>();
    logFontSizeCbx.setMinimumSize(new Dimension());
    formPane.add(logFontSizeCbx, cc.xy(3, 5));

    final JLabel boldLogTextLbl = new JLabel();
    boldLogTextLbl.setText(I18n.get(I18n.PREF_LOG_FONT_BOLD));
    formPane.add(boldLogTextLbl, cc.xy(1, 7));
    boldLogTextChbx = new JCheckBox();
    boldLogTextChbx.setText("");
    formPane.add(boldLogTextChbx, cc.xy(3, 7));

    final JSeparator sepFont = new JSeparator();
    formPane.add(sepFont, cc.xyw(1, 9, 3, CellConstraints.FILL, CellConstraints.DEFAULT));

    final JLabel defaultLogsLbl = new JLabel();
    defaultLogsLbl.setText(I18n.get(I18n.PREF_DEFAULT_LOGS_PATH));
    formPane.add(defaultLogsLbl, cc.xy(1, 11));
    logsPathTxt = new JTextField();
    logsPathTxt.setEditable(false);
    formPane.add(logsPathTxt, cc.xy(3, 11, CellConstraints.FILL, CellConstraints.DEFAULT));
    logsPathBtn = new JButton();
    logsPathBtn.setText("...");
    formPane.add(logsPathBtn, cc.xy(5, 11));

    final JSeparator sep2 = new JSeparator();
    formPane.add(sep2, cc.xyw(1, 13, 3, CellConstraints.FILL, CellConstraints.DEFAULT));

    final JLabel defaultFiltersLbl = new JLabel();
    defaultFiltersLbl.setText(I18n.get(I18n.PREF_DEFAULT_FILTERS_PATH));
    formPane.add(defaultFiltersLbl, cc.xy(1, 15));
    filtersPathTxt = new JTextField();
    filtersPathTxt.setEditable(false);
    formPane.add(filtersPathTxt, cc.xy(3, 15, CellConstraints.FILL, CellConstraints.DEFAULT));
    filtersPathBtn = new JButton();
    filtersPathBtn.setText("...");
    formPane.add(filtersPathBtn, cc.xy(5, 15));

    final JLabel openLastLbl = new JLabel();
    openLastLbl.setText(I18n.get(I18n.PREF_OPEN_LAST_FILTER));
    formPane.add(openLastLbl, cc.xy(1, 17));
    openLastFilterChbx = new JCheckBox();
    openLastFilterChbx.setText("");
    formPane.add(openLastFilterChbx, cc.xy(3, 17));

    final JSeparator sep3 = new JSeparator();
    formPane.add(sep3, cc.xyw(1, 19, 3, CellConstraints.FILL, CellConstraints.DEFAULT));

    final JLabel applyFiltersLbl = new JLabel();
    applyFiltersLbl.setText(I18n.get(I18n.PREF_APPLY_FILTERS_AFTER_EDIT));
    formPane.add(applyFiltersLbl, cc.xy(1, 21));
    applyFiltersAfterEditChbx = new JCheckBox();
    applyFiltersAfterEditChbx.setText("");
    formPane.add(applyFiltersAfterEditChbx, cc.xy(3, 21));

    final JLabel rememberFiltersLbl = new JLabel();
    rememberFiltersLbl.setText(I18n.get(I18n.PREF_REMEMBER_APPLIED_FILTERS));
    formPane.add(rememberFiltersLbl, cc.xy(1, 23));
    rememberAppliedFiltersChbx = new JCheckBox();
    rememberAppliedFiltersChbx.setText("");
    formPane.add(rememberAppliedFiltersChbx, cc.xy(3, 23));

    final JLabel collapseOnStartLbl = new JLabel();
    collapseOnStartLbl.setText(I18n.get(I18n.PREF_COLLAPSE_ALL_GROUPS_STARTUP));
    formPane.add(collapseOnStartLbl, cc.xy(1, 25));
    collapseAllGroupsStartup = new JCheckBox();
    collapseAllGroupsStartup.setText("");
    formPane.add(collapseAllGroupsStartup, cc.xy(3, 25));

    final JLabel showLineNumberLbl = new JLabel();
    showLineNumberLbl.setText(I18n.get(I18n.PREF_SHOW_LINE_NUMBERS));
    formPane.add(showLineNumberLbl, cc.xy(1, 27));
    showLineNumbersChbx = new JCheckBox();
    showLineNumbersChbx.setText("");
    formPane.add(showLineNumbersChbx, cc.xy(3, 27));

    final JLabel showLogDividersLbl = new JLabel();
    showLogDividersLbl.setText(I18n.get(I18n.PREF_SHOW_LOG_DIVIDERS));
    formPane.add(showLogDividersLbl, cc.xy(1, 29));
    showLogDividersChbx = new JCheckBox();
    showLogDividersChbx.setText("");
    formPane.add(showLogDividersChbx, cc.xy(3, 29));

    final JLabel applyFiltersOnChangeLbl = new JLabel();
    applyFiltersOnChangeLbl.setText(I18n.get(I18n.PREF_APPLY_FILTERS_ON_CHECK));
    formPane.add(applyFiltersOnChangeLbl, cc.xy(1, 31));
    applyFiltersOnCheckChbx = new JCheckBox();
    applyFiltersOnCheckChbx.setText("");
    formPane.add(applyFiltersOnCheckChbx, cc.xy(3, 31));

    final JSeparator sep4 = new JSeparator();
    formPane.add(sep4, cc.xyw(1, 33, 3, CellConstraints.FILL, CellConstraints.DEFAULT));

    final JLabel preferredEditorLbl = new JLabel();
    preferredEditorLbl.setText(I18n.get(I18n.PREF_PREFERRED_TEXT_EDITOR));
    formPane.add(preferredEditorLbl, cc.xy(1, 35));
    preferredEditorPathTxt = new JTextField();
    preferredEditorPathTxt.setEditable(false);
    formPane.add(preferredEditorPathTxt, cc.xy(3, 35, CellConstraints.FILL, CellConstraints.DEFAULT));
    preferredEditorPathBtn = new JButton();
    preferredEditorPathBtn.setText("...");
    formPane.add(preferredEditorPathBtn, cc.xy(5, 35));

    return formPane;
  }
}