package com.tibagni.logviewer.filter.regex;

import com.formdev.flatlaf.FlatClientProperties;
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

    regexEdit.putClientProperty(FlatClientProperties.OUTLINE,
        isRegexValid ? null : FlatClientProperties.OUTLINE_ERROR);
    regexEdit.setBackground(
        isRegexValid ? defaultRegexEditBg : errorRegexEditBg);

    if (matchStatusBadge != null) {
      if (StringUtils.isEmpty(regexPattern)) {
        matchStatusBadge.setVisible(false);
        matchStatusBadge.setToolTipText(null);
      } else if (!isRegexValid) {
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

    card.add(previewLbl,
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
