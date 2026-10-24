package com.tibagni.logviewer.filter.regex;

import com.formdev.flatlaf.FlatClientProperties;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.tibagni.logviewer.ServiceLocator;
import com.tibagni.logviewer.i18n.I18n;
import com.tibagni.logviewer.theme.LogViewerThemeManager;
import com.tibagni.logviewer.util.StringUtils;
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder;
import com.tibagni.logviewer.util.scaling.UIScaleUtils;
import com.tibagni.logviewer.view.ButtonsPane;
import com.tibagni.logviewer.view.CardBorderUtils;
import com.tibagni.logviewer.view.StatusBadge;

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
  private JCheckBox caseSensitive;
  private JLabel flagsLbl;
  private JLabel regexLbl;
  private JLabel previewLbl;
  private StatusBadge matchStatusBadge;

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
      }
    }

    regexEdit.putClientProperty(FlatClientProperties.OUTLINE,
        isRegexValid ? null : FlatClientProperties.OUTLINE_ERROR);
    regexEdit.setBackground(
        isRegexValid ? defaultRegexEditBg : errorRegexEditBg);

    if (matchStatusBadge != null) {
      if (StringUtils.isEmpty(regexPattern)) {
        matchStatusBadge.setVisible(false);
      } else if (!isRegexValid) {
        matchStatusBadge.updateResult(0, 0, StatusBadge.Variant.NO_RESULTS,
            I18n.get(I18n.REGEX_EDITOR_INVALID_REGEX));
      } else if (count > 0) {
        matchStatusBadge.updateResult(count, count, StatusBadge.Variant.NORMAL,
            I18n.format(I18n.REGEX_EDITOR_MATCHES_COUNT, count));
      } else {
        matchStatusBadge.updateResult(0, 0, StatusBadge.Variant.NO_RESULTS,
            I18n.get(I18n.REGEX_EDITOR_NO_MATCHES));
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

  private void buildUi() {
    contentPane = new JPanel();
    contentPane.setLayout(new GridBagLayout());
    contentPane.setBorder(BorderFactory.createEmptyBorder(
        UIScaleUtils.dip(10),
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

    contentPane.add(buildRegexPane(),
        new GBConstraintsBuilder()
            .withGridx(0)
            .withGridy(0)
            .withWeightx(1.0)
            .withWeighty(1.0)
            .withFill(GridBagConstraints.BOTH)
            .build());
  }

  private JPanel buildRegexPane() {
    final JPanel regexPane = new JPanel();
    regexPane.setLayout(new FormLayout(
        "fill:d:noGrow,left:4dlu:noGrow,fill:d:grow",
        "center:max(d;4px):noGrow,top:3dlu:noGrow,center:d:noGrow,"
            + "top:3dlu:noGrow,center:max(d;4px):noGrow"));

    flagsLbl = new JLabel();
    flagsLbl.setText(I18n.get(I18n.REGEX_EDITOR_LABEL_FLAGS));
    CellConstraints cc = new CellConstraints();
    regexPane.add(flagsLbl, cc.xy(1, 1));

    caseSensitive = new JCheckBox();
    caseSensitive.setText(I18n.get(I18n.REGEX_EDITOR_CASE_SENSITIVE));
    regexPane.add(caseSensitive, cc.xy(3, 1));

    regexLbl = new JLabel();
    regexLbl.setText(I18n.get(I18n.REGEX_EDITOR_LABEL_REGEX));
    regexPane.add(regexLbl, cc.xy(1, 3));

    regexEdit = new JTextArea(3, 50);
    regexEdit.setLineWrap(true);
    regexEdit.setRequestFocusEnabled(true);
    regexEdit.setFont(new Font(Font.MONOSPACED, Font.PLAIN,
        regexEdit.getFont().getSize()));
    regexEdit.setBorder(new EmptyBorder(
        UIScaleUtils.dip(4), UIScaleUtils.dip(6),
        UIScaleUtils.dip(4), UIScaleUtils.dip(6)));

    matchStatusBadge = new StatusBadge();

    JPanel regexContainer =
        new JPanel(new BorderLayout(0, UIScaleUtils.dip(4)));
    regexContainer.setOpaque(false);
    regexContainer.add(new JScrollPane(regexEdit), BorderLayout.CENTER);

    JPanel badgePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    badgePanel.setOpaque(false);
    badgePanel.add(matchStatusBadge);
    regexContainer.add(badgePanel, BorderLayout.SOUTH);

    regexPane.add(regexContainer,
        cc.xy(3, 3, CellConstraints.FILL, CellConstraints.FILL));

    previewLbl = new JLabel();
    previewLbl.setText(I18n.get(I18n.REGEX_EDITOR_LABEL_PREVIEW));
    regexPane.add(previewLbl, cc.xy(1, 5));

    regexPreview = new JTextArea(10, 50);
    regexPreview.setLineWrap(true);
    regexPreview.setText(I18n.get(I18n.REGEX_EDITOR_SAMPLE_TEXT));
    regexPreview.setWrapStyleWord(true);
    regexPreview.setFont(new Font(Font.MONOSPACED, Font.PLAIN,
        regexPreview.getFont().getSize()));
    regexPreview.setBorder(new EmptyBorder(
        UIScaleUtils.dip(4), UIScaleUtils.dip(6),
        UIScaleUtils.dip(4), UIScaleUtils.dip(6)));
    regexPane.add(new JScrollPane(regexPreview),
        cc.xy(3, 5, CellConstraints.FILL, CellConstraints.FILL));

    Color secondaryColor = CardBorderUtils.resolveSecondaryLabelColor();
    flagsLbl.setForeground(secondaryColor);
    regexLbl.setForeground(secondaryColor);
    previewLbl.setForeground(secondaryColor);

    return regexPane;
  }
}
