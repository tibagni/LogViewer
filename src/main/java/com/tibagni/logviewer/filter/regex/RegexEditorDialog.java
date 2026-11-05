package com.tibagni.logviewer.filter.regex;

import org.jetbrains.annotations.Nullable;
import com.formdev.flatlaf.FlatClientProperties;
import com.tibagni.logviewer.ServiceLocator;
import com.tibagni.logviewer.i18n.I18n;
import com.tibagni.logviewer.theme.LogViewerThemeManager;
import com.tibagni.logviewer.util.StringUtils;
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder;
import com.tibagni.logviewer.util.scaling.UIScaleUtils;
import com.tibagni.logviewer.view.ButtonsPane;
import com.tibagni.logviewer.view.CardBorderUtils;
import com.tibagni.logviewer.view.CommonIcons;
import com.tibagni.logviewer.view.FlatButton;
import com.tibagni.logviewer.view.StatusBadge;
import com.tibagni.logviewer.view.Toast;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexEditorDialog extends JDialog
    implements ButtonsPane.Listener {
  private ButtonsPane buttonsPane;
  private JPanel contentPane;
  private JTextArea regexEdit;
  private JTextArea regexPreview;
  public static final String SAMPLE_LOGCAT =
      "10-27 12:34:56.789  1000  1024 D ActivityManager: Start proc " +
      "1234:com.android.settings/u0a50 for activity\n" +
      "10-27 12:34:57.102  1000  1024 I ActivityTaskManager: Displayed " +
      "com.android.settings/.SubSettings: +312ms\n" +
      "10-27 12:34:58.450  1000  1100 W WindowManager: Failed to deliver event to window";

  public static final String SAMPLE_EXCEPTION =
      "java.lang.NullPointerException: Attempt to invoke virtual method on a null " +
      "object reference\n" +
      "    at com.example.app.MainActivity.onCreate(MainActivity.kt:42)\n" +
      "    at android.app.Activity.performCreate(Activity.java:8000)\n" +
      "    at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1309)";

  public static final String SAMPLE_SYSTEM =
      "05-14 08:15:22.100  1000  1200 I WindowManager: Screen frozen for " +
      "+150ms due to Window{abcd u0 StatusBar}\n" +
      "05-14 08:15:23.010  1000  1250 D BatteryService: Processing new " +
      "battery values: status=2, health=2, level=85\n" +
      "05-14 08:15:24.320  1000  1300 E DropBoxManagerService: Can't write to " +
      "/data/system/dropbox";

  private JCheckBox caseSensitive;
  private JLabel flagsLbl;
  private JLabel regexLbl;
  private JLabel previewLbl;
  private StatusBadge matchStatusBadge;
  private FlatButton resetSampleBtn;
  private FlatButton presetsBtn;

  private final Color defaultRegexEditBg;
  private final Color errorRegexEditBg;
  private final Color highlightColor;

  boolean isRegexValid;
  Result result;

  /**
   * Constructs a new {@code RegexEditorDialog} with the specified owner dialog.
   *
   * @param owner the {@link Dialog} from which the dialog is displayed,
   *              or {@code null}
   */
  RegexEditorDialog(Dialog owner) {
    super(owner);
    LogViewerThemeManager themeManager =
        ServiceLocator.INSTANCE.getThemeManager();
    highlightColor = themeManager.isDark() ?
        new Color(101, 70, 0) : new Color(255, 233, 152);
    errorRegexEditBg = themeManager.isDark() ?
        new Color(88, 12, 25) : new Color(235, 77, 103);

    setTitle(I18n.get(I18n.REGEX_EDITOR_DIALOG_TITLE));
    buildUi();

    setContentPane(contentPane);
    setModal(true);
    buttonsPane.setDefaultButtonOk();
    defaultRegexEditBg = regexEdit.getBackground();

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

    final DocumentListener regexApplier = new DocumentListener() {
      @Override public void insertUpdate(DocumentEvent e) {
        onDocumentChanged();
      }
      @Override public void removeUpdate(DocumentEvent e) {
        onDocumentChanged();
      }
      @Override public void changedUpdate(DocumentEvent e) {
        onDocumentChanged();
      }
    };

    regexEdit.getDocument().addDocumentListener(regexApplier);
    regexPreview.getDocument().addDocumentListener(regexApplier);
    caseSensitive.addActionListener(e -> onDocumentChanged());

    setupCopyAction(regexEdit);
    setupCopyAction(regexPreview);
  }

  /**
   * Installs a copy action wrapper on the specified text area to provide
   * transient toast feedback when text is copied to the clipboard.
   *
   * @param textArea the text area to configure with copy action feedback
   */
  private void setupCopyAction(JTextArea textArea) {
    Action origCopy = textArea.getActionMap().get("copy");
    textArea.getActionMap().put("copy", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (origCopy != null) {
          origCopy.actionPerformed(e);
        }
        String selected = textArea.getSelectedText();
        if (selected != null && !selected.isEmpty()) {
          Toast.showToast(
              RegexEditorDialog.this,
              I18n.get(I18n.COMMON_COPIED_TO_CLIPBOARD),
              Toast.LENGTH_SHORT);
        }
      }
    });
  }

  private void onDocumentChanged() {
    applyRegexToPreview();
    updateOkButton();
  }

  @Override
  public void onOk() {
    result = new Result(regexEdit.getText(), caseSensitive.isSelected());
    dispose();
  }

  @Override
  public void onCancel() {
    dispose();
  }

  /**
   * Applies the current regular expression in {@link #regexEdit} to the
   * preview text, highlighting matching occurrences in {@link #regexPreview}.
   * Updates the outline property and background color of {@link #regexEdit}
   * and configures {@link #matchStatusBadge} with match count feedback.
   */
  void applyRegexToPreview() {
    String regexPattern = regexEdit.getText();
    String previewText = regexPreview.getText();
    Highlighter highlighter = regexPreview.getHighlighter();
    highlighter.removeAllHighlights();

    isRegexValid = true;
    int count = 0;
    String errorMessage = null;
    if (!StringUtils.isEmpty(regexPattern)) {
      try {
        int flags = caseSensitive.isSelected() ? 0 : Pattern.CASE_INSENSITIVE;
        Pattern pattern = Pattern.compile(regexPattern, flags);
        Matcher matcher = pattern.matcher(previewText);
        while (matcher.find()) {
          int start = matcher.start();
          int end = matcher.end();
          highlighter.addHighlight(start, end,
              new DefaultHighlighter.DefaultHighlightPainter(highlightColor));
          count++;
        }
        isRegexValid = true;
      } catch (Exception e) {
        isRegexValid = false;
        errorMessage = e.getMessage();
      }
    }

    updateValidationFeedback(isRegexValid, count, errorMessage, regexPattern);
  }

  /**
   * Updates UI feedback components including text area outline, background color,
   * and match status badge based on regex validation state and match count.
   * Configures error outline and tooltip on invalid syntax, normal badge on
   * successful matches, no-results badge on zero matches, or hides the badge when empty.
   *
   * @param isValid whether the regex syntax is valid
   * @param count number of matches found in preview text
   * @param errorMessage error message if regex syntax is invalid, or {@code null}
   * @param regexPattern the regex pattern string entered by the user
   */
  private void updateValidationFeedback(boolean isValid, int count,
                                        @Nullable String errorMessage,
                                        String regexPattern) {
    regexEdit.putClientProperty(FlatClientProperties.OUTLINE,
        isValid ? null : FlatClientProperties.OUTLINE_ERROR);
    regexEdit.setBackground(
        isValid ? defaultRegexEditBg : errorRegexEditBg);

    if (matchStatusBadge != null) {
      if (StringUtils.isEmpty(regexPattern)) {
        matchStatusBadge.setVisible(false);
        matchStatusBadge.setToolTipText(null);
      } else if (!isValid) {
        matchStatusBadge.updateResult(0, 0, StatusBadge.Variant.NO_RESULTS,
            I18n.get(I18n.REGEX_EDITOR_INVALID_REGEX));
        matchStatusBadge.setToolTipText(errorMessage);
      } else if (count > 0) {
        matchStatusBadge.updateResult(count, count, StatusBadge.Variant.NORMAL,
            I18n.format(I18n.REGEX_EDITOR_MATCHES_COUNT, count));
        matchStatusBadge.setToolTipText(null);
      } else {
        matchStatusBadge.updateResult(0, 0, StatusBadge.Variant.NO_RESULTS,
            I18n.get(I18n.REGEX_EDITOR_NO_MATCHES));
        matchStatusBadge.setToolTipText(null);
      }
    }
  }

  /**
   * Returns the regex pattern input text area.
   *
   * @return the regex input {@link JTextArea}
   */
  public JTextArea getRegexEdit() {
    return regexEdit;
  }

  /**
   * Returns the preview text area.
   *
   * @return the preview {@link JTextArea}
   */
  public JTextArea getRegexPreview() {
    return regexPreview;
  }

  /**
   * Returns the case sensitive checkbox.
   *
   * @return the case sensitive {@link JCheckBox}
   */
  public JCheckBox getCaseSensitive() {
    return caseSensitive;
  }

  /**
   * Returns the status badge showing regex match feedback.
   *
   * @return the match status badge
   */
  public StatusBadge getMatchStatusBadge() {
    return matchStatusBadge;
  }

  /**
   * Returns the flags label.
   *
   * @return the flags label
   */
  public JLabel getFlagsLbl() {
    return flagsLbl;
  }

  /**
   * Returns the regex label.
   *
   * @return the regex label
   */
  public JLabel getRegexLbl() {
    return regexLbl;
  }

  /**
   * Returns the preview label.
   *
   * @return the preview label
   */
  public JLabel getPreviewLbl() {
    return previewLbl;
  }

  /**
   * Returns the reset sample text button.
   *
   * @return the reset sample button
   */
  public FlatButton getResetSampleBtn() {
    return resetSampleBtn;
  }

  /**
   * Returns the presets button for loading sample log scenarios.
   *
   * @return the presets {@link FlatButton}
   */
  public FlatButton getPresetsBtn() {
    return presetsBtn;
  }

  /**
   * Displays the sample text presets menu below the presets button.
   */
  private void showPresetsMenu() {
    JPopupMenu popup = new JPopupMenu();

    JMenuItem defaultItem = new JMenuItem(I18n.get(I18n.REGEX_EDITOR_PRESET_DEFAULT));
    defaultItem.setIcon(CommonIcons.fileDocumentIcon(UIScaleUtils.dip(14)));
    defaultItem.addActionListener(e -> loadSamplePreset(
        I18n.get(I18n.REGEX_EDITOR_SAMPLE_TEXT),
        I18n.get(I18n.REGEX_EDITOR_PRESET_DEFAULT)));

    JMenuItem logcatItem = new JMenuItem(I18n.get(I18n.REGEX_EDITOR_PRESET_LOGCAT));
    logcatItem.setIcon(CommonIcons.smartphoneIcon(UIScaleUtils.dip(14)));
    logcatItem.addActionListener(e -> loadSamplePreset(
        SAMPLE_LOGCAT,
        I18n.get(I18n.REGEX_EDITOR_PRESET_LOGCAT)));

    JMenuItem exceptionItem = new JMenuItem(I18n.get(I18n.REGEX_EDITOR_PRESET_EXCEPTION));
    exceptionItem.setIcon(CommonIcons.infoIcon(UIScaleUtils.dip(14)));
    exceptionItem.addActionListener(e -> loadSamplePreset(
        SAMPLE_EXCEPTION,
        I18n.get(I18n.REGEX_EDITOR_PRESET_EXCEPTION)));

    JMenuItem systemItem = new JMenuItem(I18n.get(I18n.REGEX_EDITOR_PRESET_SYSTEM));
    systemItem.setIcon(CommonIcons.terminalIcon(UIScaleUtils.dip(14)));
    systemItem.addActionListener(e -> loadSamplePreset(
        SAMPLE_SYSTEM,
        I18n.get(I18n.REGEX_EDITOR_PRESET_SYSTEM)));

    popup.add(defaultItem);
    popup.addSeparator();
    popup.add(logcatItem);
    popup.add(exceptionItem);
    popup.add(systemItem);

    popup.show(presetsBtn, 0, presetsBtn.getHeight());
  }

  void loadSamplePreset(String text, String presetName) {
    regexPreview.setText(text);
    regexPreview.requestFocus();
    Toast.showToast(this, presetName, Toast.LENGTH_SHORT);
  }

  private void updateOkButton() {
    String regexPattern = regexEdit.getText();
    buttonsPane.enableOkButton(
        !StringUtils.isEmpty(regexPattern) && isRegexValid);
  }

  public static class Result {
    public final String pattern;
    public final boolean caseSensitive;

    public Result(String pattern, boolean caseSensitive) {
      this.pattern = pattern;
      this.caseSensitive = caseSensitive;
    }
  }

  public static Result showEditRegexDialog(Dialog parent,
                                           Component relativeTo,
                                           String pattern,
                                           String previewText,
                                           boolean caseSensitive) {
    RegexEditorDialog dialog = new RegexEditorDialog(parent);
    dialog.caseSensitive.setSelected(caseSensitive);
    dialog.regexEdit.setText(pattern);
    if (!StringUtils.isEmpty(previewText)) {
      dialog.regexPreview.setText(previewText);
    }
    dialog.applyRegexToPreview();
    dialog.updateOkButton();

    dialog.pack();
    dialog.setLocationRelativeTo(relativeTo);
    dialog.setVisible(true);

    return dialog.result;
  }

  /**
   * Constructs and lays out the dialog interface.
   */
  private void buildUi() {
    contentPane = new JPanel(new GridBagLayout());
    contentPane.setBorder(BorderFactory.createEmptyBorder(
        UIScaleUtils.dip(12),
        UIScaleUtils.dip(14),
        UIScaleUtils.dip(12),
        UIScaleUtils.dip(14)));

    buttonsPane = new ButtonsPane(ButtonsPane.ButtonsMode.OK_CANCEL, this);
    contentPane.add(buttonsPane,
        new GBConstraintsBuilder()
            .withGridx(0)
            .withGridy(1)
            .withWeightx(1.0)
            .withFill(GridBagConstraints.HORIZONTAL)
            .withInsets(new Insets(UIScaleUtils.dip(10), 0, 0, 0))
            .build());

    contentPane.add(buildRegexPane(),
        new GBConstraintsBuilder()
            .withGridx(0)
            .withGridy(0)
            .withWeightx(1.0)
            .withWeighty(1.0)
            .withFill(GridBagConstraints.BOTH)
            .build());
  }

  /**
   * Constructs the main panel composed of the pattern card and the preview card.
   *
   * @return the container panel holding both editor cards
   */
  private JPanel buildRegexPane() {
    final JPanel regexPane = new JPanel(new GridBagLayout());
    regexPane.setOpaque(false);

    regexPane.add(buildPatternCard(),
        new GBConstraintsBuilder()
            .withGridx(0).withGridy(0)
            .withWeightx(1.0)
            .withFill(GridBagConstraints.HORIZONTAL)
            .withInsets(new Insets(0, 0, UIScaleUtils.dip(8), 0))
            .build());

    regexPane.add(buildPreviewCard(),
        new GBConstraintsBuilder()
            .withGridx(0).withGridy(1)
            .withWeightx(1.0).withWeighty(1.0)
            .withFill(GridBagConstraints.BOTH)
            .build());

    return regexPane;
  }

  /**
   * Constructs the regular expression input card with flags and match status badge.
   *
   * @return the pattern card panel
   */
  private JPanel buildPatternCard() {
    final JPanel card = new JPanel(new GridBagLayout());
    card.setOpaque(false);
    card.setBorder(BorderFactory.createCompoundBorder(
        CardBorderUtils.createCardBorder(),
        new EmptyBorder(
            UIScaleUtils.dip(10),
            UIScaleUtils.dip(12),
            UIScaleUtils.dip(10),
            UIScaleUtils.dip(12))));

    Color secondaryColor = CardBorderUtils.resolveSecondaryLabelColor();

    regexLbl = new JLabel(I18n.get(I18n.REGEX_EDITOR_LABEL_REGEX));
    regexLbl.setForeground(secondaryColor);
    regexLbl.setFont(regexLbl.getFont().deriveFont(Font.BOLD));

    flagsLbl = new JLabel(I18n.get(I18n.REGEX_EDITOR_LABEL_FLAGS));
    flagsLbl.setForeground(secondaryColor);

    caseSensitive = new JCheckBox(I18n.get(I18n.REGEX_EDITOR_CASE_SENSITIVE));
    caseSensitive.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    matchStatusBadge = new StatusBadge();

    regexEdit = new JTextArea(3, 50);
    regexEdit.setLineWrap(true);
    regexEdit.setRequestFocusEnabled(true);
    regexEdit.setFont(new Font(Font.MONOSPACED, Font.PLAIN, regexEdit.getFont().getSize()));
    regexEdit.setBorder(new EmptyBorder(
        UIScaleUtils.dip(4), UIScaleUtils.dip(6),
        UIScaleUtils.dip(4), UIScaleUtils.dip(6)));

    JPanel headerPanel = new JPanel(new BorderLayout());
    headerPanel.setOpaque(false);
    headerPanel.add(regexLbl, BorderLayout.WEST);

    JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    rightHeader.setOpaque(false);
    rightHeader.add(flagsLbl);
    rightHeader.add(Box.createHorizontalStrut(UIScaleUtils.dip(4)));
    rightHeader.add(caseSensitive);
    rightHeader.add(Box.createHorizontalStrut(UIScaleUtils.dip(8)));
    rightHeader.add(matchStatusBadge);
    headerPanel.add(rightHeader, BorderLayout.EAST);

    card.add(headerPanel,
        new GBConstraintsBuilder()
            .withGridx(0).withGridy(0)
            .withWeightx(1.0)
            .withFill(GridBagConstraints.HORIZONTAL)
            .withInsets(new Insets(0, 0, UIScaleUtils.dip(6), 0))
            .build());

    JScrollPane scrollPane = new JScrollPane(regexEdit);
    scrollPane.setBorder(CardBorderUtils.createCardBorder());
    card.add(scrollPane,
        new GBConstraintsBuilder()
            .withGridx(0).withGridy(1)
            .withWeightx(1.0).withWeighty(1.0)
            .withFill(GridBagConstraints.BOTH)
            .build());

    return card;
  }

  /**
   * Constructs the sample text preview card with interactive highlight feedback.
   *
   * @return the preview card panel
   */
  private JPanel buildPreviewCard() {
    final JPanel card = new JPanel(new GridBagLayout());
    card.setOpaque(false);
    card.setBorder(BorderFactory.createCompoundBorder(
        CardBorderUtils.createCardBorder(),
        new EmptyBorder(
            UIScaleUtils.dip(10),
            UIScaleUtils.dip(12),
            UIScaleUtils.dip(10),
            UIScaleUtils.dip(12))));

    Color secondaryColor = CardBorderUtils.resolveSecondaryLabelColor();

    previewLbl = new JLabel(I18n.get(I18n.REGEX_EDITOR_LABEL_PREVIEW));
    previewLbl.setForeground(secondaryColor);
    previewLbl.setFont(previewLbl.getFont().deriveFont(Font.BOLD));

    regexPreview = new JTextArea(10, 50);
    regexPreview.setLineWrap(true);
    regexPreview.setText(I18n.get(I18n.REGEX_EDITOR_SAMPLE_TEXT));
    regexPreview.setWrapStyleWord(true);
    regexPreview.setFont(new Font(Font.MONOSPACED, Font.PLAIN, regexPreview.getFont().getSize()));
    regexPreview.setBorder(new EmptyBorder(
        UIScaleUtils.dip(4), UIScaleUtils.dip(6),
        UIScaleUtils.dip(4), UIScaleUtils.dip(6)));

    JPanel headerPanel = new JPanel(new BorderLayout());
    headerPanel.setOpaque(false);
    headerPanel.add(previewLbl, BorderLayout.WEST);

    presetsBtn = new FlatButton();
    presetsBtn.setIcon(CommonIcons.viewListIcon(UIScaleUtils.dip(12)));
    presetsBtn.setText(I18n.get(I18n.REGEX_EDITOR_BTN_PRESETS));
    presetsBtn.setIconTextGap(UIScaleUtils.dip(4));
    presetsBtn.setToolTipText(I18n.get(I18n.REGEX_EDITOR_BTN_PRESETS));
    presetsBtn.addActionListener(e -> showPresetsMenu());

    resetSampleBtn = new FlatButton();
    resetSampleBtn.setIcon(CommonIcons.refreshIcon(UIScaleUtils.dip(12)));
    resetSampleBtn.setToolTipText(I18n.get(I18n.REGEX_EDITOR_RESET_SAMPLE_TOOLTIP));
    resetSampleBtn.addActionListener(e -> {
      regexPreview.setText(I18n.get(I18n.REGEX_EDITOR_SAMPLE_TEXT));
      regexPreview.requestFocus();
    });

    JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIScaleUtils.dip(4), 0));
    actionsPanel.setOpaque(false);
    actionsPanel.add(presetsBtn);
    actionsPanel.add(resetSampleBtn);
    headerPanel.add(actionsPanel, BorderLayout.EAST);

    card.add(headerPanel,
        new GBConstraintsBuilder()
            .withGridx(0).withGridy(0)
            .withWeightx(1.0)
            .withFill(GridBagConstraints.HORIZONTAL)
            .withInsets(new Insets(0, 0, UIScaleUtils.dip(6), 0))
            .build());

    JScrollPane scrollPane = new JScrollPane(regexPreview);
    scrollPane.setBorder(CardBorderUtils.createCardBorder());
    card.add(scrollPane,
        new GBConstraintsBuilder()
            .withGridx(0).withGridy(1)
            .withWeightx(1.0).withWeighty(1.0)
            .withFill(GridBagConstraints.BOTH)
            .build());

    return card;
  }
}
