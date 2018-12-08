package com.tibagni.logviewer.filter;

import com.tibagni.logviewer.filter.regex.RegexEditorDialog;
import com.tibagni.logviewer.util.StringUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class EditFilterDialog extends JDialog {
  private static final Color[] INITIAL_COLORS = new Color[]{
      Color.black,
      Color.blue,
      Color.darkGray,
      Color.red,
      Color.yellow,
      Color.cyan,
      Color.green,
      Color.magenta,
      Color.orange,
      Color.pink,
      new Color(102, 102, 0),
      new Color(0, 102, 153),
      new Color(0, 102, 102),
      new Color(102, 0, 0),
      new Color(51, 204, 255),
      new Color(255, 6, 250),
      new Color(255, 113, 41)
  };

  private JPanel contentPane;
  private JButton buttonOK;
  private JButton buttonCancel;
  private JLabel nameLbl;
  private JTextField nameTxt;
  private JLabel regexLbl;
  private JTextField regexTxt;
  private JLabel colorLbl;
  private JLabel caseSensitiveLbl;
  private JCheckBox caseSensitiveCbx;
  private JButton regexEditorBtn;
  private JColorChooser colorChooser;

  private Filter filter;

  private final DocumentListener regexDocumentListener = new DocumentListener() {
    @Override
    public void insertUpdate(DocumentEvent e) {
      nameTxt.setText(regexTxt.getText());
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
      nameTxt.setText(regexTxt.getText());
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
      nameTxt.setText(regexTxt.getText());
    }
  };

  private EditFilterDialog(Frame owner, Filter editingFilter) {
    super(owner);

    setContentPane(contentPane);
    setModal(true);
    getRootPane().setDefaultButton(buttonOK);

    buttonOK.addActionListener(e -> onOK());
    buttonCancel.addActionListener(e -> onCancel());
    regexEditorBtn.addActionListener(e -> onEditRegex());

    colorChooser.setColor(getInitialColor());

    // call onCancel() when cross is clicked
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        onCancel();
      }
    });

    // call onCancel() on ESCAPE
    contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
        JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

    boolean nameIsPattern = true;
    if (editingFilter != null) {
      filter = editingFilter;
      nameTxt.setText(filter.getName());
      regexTxt.setText(filter.getPatternString());
      colorChooser.setColor(filter.getColor());
      caseSensitiveCbx.setSelected(filter.isCaseSensitive());
      nameIsPattern = StringUtils.areEquals(filter.getName(), filter.getPatternString());
    }

    if (nameIsPattern) {
      regexTxt.getDocument().addDocumentListener(regexDocumentListener);
      nameTxt.setEnabled(false);
      nameTxt.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          if (!nameTxt.isEnabled()) {
            regexTxt.getDocument().removeDocumentListener(regexDocumentListener);
            nameTxt.setEnabled(true);
            nameTxt.requestFocus();
            nameTxt.selectAll();
          }
        }
      });
    }

    SwingUtilities.invokeLater(() -> regexTxt.requestFocus());
  }

  private void onOK() {
    // add your code here
    Color selectedColor = colorChooser.getColor();
    String name = nameTxt.getText();
    String pattern = regexTxt.getText();
    boolean caseSensitive = caseSensitiveCbx.isSelected();

    try {
      if (filter == null) {
        filter = new Filter(name, pattern, selectedColor, caseSensitive);
      } else {
        filter.updateFilter(name, pattern, selectedColor, caseSensitive);
      }
    } catch (FilterException e) {
      JOptionPane.showConfirmDialog(this, e.getMessage(), "Error...",
          JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
      return;
    }

    dispose();
  }

  private void onCancel() {
    filter = null;
    dispose();
  }

  private void onEditRegex() {
    RegexEditorDialog.Result edited = RegexEditorDialog.showEditRegexDialog(this, this,
        regexTxt.getText(), caseSensitiveCbx.isSelected());

    if (edited != null) {
      regexTxt.setText(edited.pattern);
      caseSensitiveCbx.setSelected(edited.caseSensitive);
    }
  }

  public static Filter showEditFilterDialog(Frame parent, Component relativeTo, Filter editingFilter) {
    EditFilterDialog dialog = new EditFilterDialog(parent, editingFilter);
    dialog.pack();
    dialog.setLocationRelativeTo(relativeTo);
    dialog.setVisible(true);

    return dialog.filter;
  }

  public static Filter showEditFilterDialog(Frame parent, Component relativeTo) {
    return showEditFilterDialog(parent, relativeTo, null);
  }

  private void createUIComponents() {
    colorChooser = new JColorChooser();
    AbstractColorChooserPanel swatchPanel = getSwatchPanel(colorChooser.getChooserPanels());

    // Keep only the swatch panel
    colorChooser.setChooserPanels(new AbstractColorChooserPanel[]{swatchPanel});

    // Show a simple text field for preview
    JTextField preview = new JTextField("Filtered text color preview");
    preview.setBorder(new EmptyBorder(5, 15, 5, 15));
    colorChooser.setPreviewPanel(preview);
  }

  private AbstractColorChooserPanel getSwatchPanel(AbstractColorChooserPanel[] panels) {
    for (AbstractColorChooserPanel colorPanel : panels) {
      if (colorPanel.getClass().getName().contains("DefaultSwatchChooserPanel")) {
        return colorPanel;
      }
    }

    return null;
  }

  private Color getInitialColor() {
    // Set a random color for the filter initially
    final Random r = new Random();
    return INITIAL_COLORS[r.nextInt(INITIAL_COLORS.length)];
  }
}
