package com.tibagni.logviewer.filter.regex;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.tibagni.logviewer.util.StringUtils;
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder;
import com.tibagni.logviewer.view.ButtonsPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexEditorDialog extends JDialog implements ButtonsPane.Listener {
  private ButtonsPane buttonsPane;
  private JPanel contentPane;
  private JTextArea regexEdit;
  private JTextArea regexPreview;
  private JCheckBox caseSensitive;

  private final Color defaultRegexEditBg;
  private final Color errorRegexEditBg = new Color(235, 77, 103);
  private final Color highlightColor = new Color(255, 233, 152);

  boolean isRegexValid;
  Result result;

  private RegexEditorDialog(Dialog owner) {
    super(owner);
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
      @Override public void insertUpdate(DocumentEvent e) { onDocumentChanged(); }
      @Override public void removeUpdate(DocumentEvent e) { onDocumentChanged(); }
      @Override public void changedUpdate(DocumentEvent e) { onDocumentChanged(); }
    };

    regexEdit.getDocument().addDocumentListener(regexApplier);
    regexPreview.getDocument().addDocumentListener(regexApplier);
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

  private void applyRegexToPreview() {
    String regexPattern = regexEdit.getText();
    String previewText = regexPreview.getText();
    Highlighter highlighter = regexPreview.getHighlighter();
    highlighter.removeAllHighlights();

    isRegexValid = true;
    if (!StringUtils.isEmpty(regexPattern)) {
      try {
        int flags = caseSensitive.isSelected() ? 0 : Pattern.CASE_INSENSITIVE;
        Pattern pattern = Pattern.compile(regexPattern, flags);
        Matcher matcher = pattern.matcher(previewText);
        while (matcher.find()) {
          int start = matcher.start();
          int end = matcher.end();
          highlighter.addHighlight(start, end, new DefaultHighlighter.DefaultHighlightPainter(highlightColor));
        }
        isRegexValid = true;
      } catch (Exception e) {
        isRegexValid = false;
      }
    }

    regexEdit.setBackground(isRegexValid ? defaultRegexEditBg : errorRegexEditBg);
  }

  private void updateOkButton() {
    String regexPattern = regexEdit.getText();
    buttonsPane.enableOkButton(!StringUtils.isEmpty(regexPattern) && isRegexValid);
  }

  public static class Result {
    public final String pattern;
    public final boolean caseSensitive;

    private Result(String pattern, boolean caseSensitive) {
      this.pattern = pattern;
      this.caseSensitive = caseSensitive;
    }
  }

  public static Result showEditRegexDialog(Dialog parent, Component relativeTo,
                                           String pattern, String previewText,
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
    contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

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
        "center:max(d;4px):noGrow,top:3dlu:noGrow,center:d:noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow"));

    final JLabel regexLbl = new JLabel();
    regexLbl.setText("Regex:");
    CellConstraints cc = new CellConstraints();
    regexPane.add(regexLbl, cc.xy(1, 3));

    final JLabel previewLbl = new JLabel();
    previewLbl.setText("Preview:");
    regexPane.add(previewLbl, cc.xy(1, 5));

    final JLabel flagsLbl = new JLabel();
    flagsLbl.setText("Flags:");
    regexPane.add(flagsLbl, cc.xy(1, 1));

    caseSensitive = new JCheckBox();
    caseSensitive.setText("Case sensitive");
    regexPane.add(caseSensitive, cc.xy(3, 1));

    regexEdit = new JTextArea(3, 50);
    regexEdit.setLineWrap(true);
    regexEdit.setRequestFocusEnabled(true);
    regexPane.add(new JScrollPane(regexEdit),
        cc.xy(3, 3, CellConstraints.FILL, CellConstraints.FILL));

    regexPreview = new JTextArea(10, 50);
    regexPreview.setLineWrap(true);
    regexPreview.setText("Sample text for testing: \n" +
        "abcdefghijklmnopqrstuvwxyz \n" +
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ \n" +
        "0123456789 _+-.,!@#$%^&*();\\/|<>\"'\n" +
        "12345 -98.7 3.141 .6180 9,000 \n" +
        "+42 555.123.4567 +1-(800)-555-2468 \n" +
        "foo@demo.net bar.ba@test.co.uk \n" +
        "www.demo.com http://foo.co.uk/");
    regexPreview.setWrapStyleWord(true);
    regexPane.add(new JScrollPane(regexPreview),
        cc.xy(3, 5, CellConstraints.FILL, CellConstraints.FILL));

    return regexPane;
  }
}
