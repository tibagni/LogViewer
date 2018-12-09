package com.tibagni.logviewer.filter.regex;

import com.tibagni.logviewer.util.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexEditorDialog extends JDialog {
  private JPanel contentPane;
  private JButton buttonOK;
  private JButton buttonCancel;
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
    setContentPane(contentPane);
    setModal(true);
    getRootPane().setDefaultButton(buttonOK);
    defaultRegexEditBg = regexEdit.getBackground();

    buttonOK.addActionListener(e -> onOK());
    buttonCancel.addActionListener(e -> onCancel());

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

  private void onOK() {
    result = new Result(regexEdit.getText(), caseSensitive.isSelected());
    dispose();
  }

  private void onCancel() {
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
    buttonOK.setEnabled(!StringUtils.isEmpty(regexPattern) && isRegexValid);
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
}
