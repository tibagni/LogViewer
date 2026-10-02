package com.tibagni.logviewer.filter;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.tibagni.logviewer.ServiceLocator;
import com.tibagni.logviewer.filter.regex.RegexEditorDialog;
import com.tibagni.logviewer.i18n.I18n;
import com.tibagni.logviewer.log.LogLevel;
import com.tibagni.logviewer.theme.LogViewerThemeManager;
import com.tibagni.logviewer.util.StringUtils;
import com.tibagni.logviewer.util.scaling.UIScaleUtils;
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder;
import com.tibagni.logviewer.view.ButtonsPane;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class EditFilterDialog extends JDialog implements ButtonsPane.Listener {
  private static final Color[] INITIAL_COLORS_LIGHT = new Color[]{
      Color.darkGray,
      new Color(102, 102, 0),
      new Color(0, 102, 153),
      new Color(0, 102, 102),
      new Color(102, 0, 0),
      new Color(26, 69, 73),
      new Color(88, 24, 88),
      new Color(97, 49, 2)
  };

  private static final Color[] INITIAL_COLORS_DARK = new Color[]{
      Color.blue,
      Color.red,
      Color.yellow,
      Color.cyan,
      Color.green,
      Color.pink,
      new Color(208, 208, 119),
      new Color(83, 199, 246),
      new Color(8, 248, 248),
      new Color(222, 143, 143),
      new Color(111, 210, 255),
      new Color(255, 6, 250),
      new Color(252, 156, 106)
  };

  private ButtonsPane buttonsPane;
  private JPanel contentPane;
  private JLabel nameLbl;
  private JTextField nameTxt;
  private JLabel regexLbl;
  private JTextField regexTxt;
  private JLabel colorLbl;
  private JLabel caseSensitiveLbl;
  private JCheckBox caseSensitiveCbx;
  private JLabel verbosityLbl;
  private JComboBox<LogLevel> verbosityCombo;
  private JButton regexEditorBtn;
  private JColorChooser colorChooser;

  @FunctionalInterface
  public interface DuplicateCheckCallback {
    FilterMatch check(String pattern, boolean isCaseSensitive);
  }

  private Filter filter;
  private final Filter editingFilter;
  private final DuplicateCheckCallback duplicateCheckCallback;
  private String previewText;
  private final LogViewerThemeManager themeManager;

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

  EditFilterDialog(Frame owner, Filter editingFilter) {
    this(owner, editingFilter, null, null);
  }

  EditFilterDialog(Frame owner, Filter editingFilter, DuplicateCheckCallback duplicateCheckCallback) {
    this(owner, editingFilter, null, duplicateCheckCallback);
  }

  EditFilterDialog(Frame owner, Filter editingFilter, String preDefinedText) {
    this(owner, editingFilter, preDefinedText, null);
  }

  EditFilterDialog(Frame owner, Filter editingFilter, String preDefinedText, DuplicateCheckCallback duplicateCheckCallback) {
    super(owner);
    this.editingFilter = editingFilter;
    this.duplicateCheckCallback = duplicateCheckCallback;
    previewText = preDefinedText;
    themeManager = ServiceLocator.INSTANCE.getThemeManager();
    setTitle(I18n.get(editingFilter != null ? I18n.FILTER_DIALOG_EDIT_TITLE : I18n.FILTER_DIALOG_NEW_TITLE));
    buildUi();

    setContentPane(contentPane);
    setModal(true);
    buttonsPane.setDefaultButtonOk();

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
      regexTxt.selectAll();
      colorChooser.setColor(filter.getColor());
      caseSensitiveCbx.setSelected(filter.isCaseSensitive());
      verbosityCombo.setSelectedItem(filter.getVerbosity());
      nameIsPattern = filter.nameIsPattern();
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

      // Adjust the size according to the content after everything is populated
      contentPane.setPreferredSize(contentPane.getPreferredSize());
      contentPane.validate();
    }

    SwingUtilities.invokeLater(() -> regexTxt.requestFocus());

    if (!StringUtils.isEmpty(preDefinedText)) {
      regexTxt.setText(preDefinedText);
      addWindowListener(new WindowAdapter() {
        @Override
        public void windowOpened(WindowEvent e) {
          regexEditorBtn.doClick();
        }
      });
    }
  }

  @Override
  public void onOk() {
    // add your code here
    Color selectedColor = colorChooser.getColor();
    String name = nameTxt.getText();
    String pattern = regexTxt.getText();
    boolean caseSensitive = caseSensitiveCbx.isSelected();
    LogLevel verbosity = (LogLevel) verbosityCombo.getSelectedItem();

    FilterMatch duplicateMatch = checkForDuplicateFilter(pattern, caseSensitive);
    if (duplicateMatch != null) {
      String message = I18n.format(
          I18n.FILTER_DUPLICATE_WARNING_MSG,
          duplicateMatch.getFilter().getPatternString(),
          duplicateMatch.getGroup()
      );
      int choice = JOptionPane.showOptionDialog(
          this,
          message,
          I18n.get(I18n.FILTER_DUPLICATE_WARNING_TITLE),
          JOptionPane.YES_NO_OPTION,
          JOptionPane.WARNING_MESSAGE,
          null,
          new Object[]{I18n.get(I18n.FILTER_DUPLICATE_ADD_ANYWAY), I18n.get(I18n.COMMON_CANCEL)},
          I18n.get(I18n.COMMON_CANCEL)
      );
      if (choice != JOptionPane.YES_OPTION) {
        return;
      }
    }

    try {
      if (filter == null) {
        filter = new Filter(name, pattern, selectedColor, verbosity, caseSensitive);
      } else {
        filter.updateFilter(name, pattern, selectedColor, verbosity, caseSensitive);
      }
    } catch (FilterException e) {
      JOptionPane.showConfirmDialog(this, e.getMessage(), I18n.get(I18n.COMMON_ERROR),
          JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
      return;
    }

    dispose();
  }

  FilterMatch checkForDuplicateFilter(String newPattern, boolean caseSensitive) {
    if (duplicateCheckCallback != null) {
      return duplicateCheckCallback.check(newPattern, caseSensitive);
    }
    return null;
  }

  @Override
  public void onCancel() {
    filter = null;
    dispose();
  }

  private void onEditRegex() {
    RegexEditorDialog.Result edited = RegexEditorDialog.showEditRegexDialog(this, this,
        regexTxt.getText(), previewText, caseSensitiveCbx.isSelected());

    if (edited != null) {
      regexTxt.setText(edited.pattern);
      caseSensitiveCbx.setSelected(edited.caseSensitive);
    }
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
    Color[] colors = themeManager.isDark() ? INITIAL_COLORS_DARK : INITIAL_COLORS_LIGHT;
    return colors[r.nextInt(colors.length)];
  }

  public static Filter showEditFilterDialog(Frame parent, Filter editingFilter, DuplicateCheckCallback duplicateCheckCallback) {
    EditFilterDialog dialog = new EditFilterDialog(parent, editingFilter, duplicateCheckCallback);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setVisible(true);

    return dialog.filter;
  }

  public static Filter showEditFilterDialog(Frame parent, Filter editingFilter) {
    return showEditFilterDialog(parent, editingFilter, null);
  }

  public static Filter showEditFilterDialog(Frame parent, DuplicateCheckCallback duplicateCheckCallback) {
    return showEditFilterDialog(parent, null, duplicateCheckCallback);
  }

  public static Filter showEditFilterDialog(Frame parent) {
    return showEditFilterDialog(parent, null, null);
  }

  // This is used to create a Filter from an existing predefined String
  // It will open the Edit Dialog directly on the RegEx Editor
  public static Filter showEditFilterDialogWithText(Frame parent, String preDefinedText, DuplicateCheckCallback duplicateCheckCallback) {
    EditFilterDialog dialog = new EditFilterDialog(parent, null, preDefinedText, duplicateCheckCallback);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setVisible(true);

    return dialog.filter;
  }

  public static Filter showEditFilterDialogWithText(Frame parent, String preDefinedText) {
    return showEditFilterDialogWithText(parent, preDefinedText, null);
  }

  private void buildUi() {
    contentPane = new JPanel();
    contentPane.setLayout(new GridBagLayout());
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

    contentPane.add(buildEditPane(),
        new GBConstraintsBuilder()
            .withGridx(0)
            .withGridy(0)
            .withWeightx(1.0)
            .withWeighty(1.0)
            .withFill(GridBagConstraints.BOTH)
            .build());
  }

  private JPanel buildEditPane() {
    final JPanel editPane = new JPanel();
    editPane.setLayout(new FormLayout(
        "fill:d:noGrow,left:4dlu:noGrow,fill:d:grow,left:4dlu:noGrow,fill:max(d;4px):noGrow",
        "center:d:noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow"));

    nameLbl = new JLabel();
    nameLbl.setText(I18n.get(I18n.FILTER_LABEL_NAME));
    nameLbl.setToolTipText(I18n.get(I18n.FILTER_TOOLTIP_NAME));
    CellConstraints cc = new CellConstraints();
    editPane.add(nameLbl, cc.xy(1, 1));
    nameTxt = new JTextField();
    nameTxt.setEnabled(true);
    editPane.add(nameTxt, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

    regexLbl = new JLabel();
    regexLbl.setText(I18n.get(I18n.FILTER_LABEL_REGEX));
    regexLbl.setToolTipText(I18n.get(I18n.FILTER_TOOLTIP_REGEX));
    editPane.add(regexLbl, cc.xy(1, 3));
    regexTxt = new JTextField();
    editPane.add(regexTxt, cc.xy(3, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
    regexEditorBtn = new JButton();
    regexEditorBtn.setText(I18n.get(I18n.FILTER_BTN_REGEX_EDITOR));
    regexEditorBtn.setToolTipText(I18n.get(I18n.FILTER_TOOLTIP_REGEX_EDITOR));
    editPane.add(regexEditorBtn, cc.xy(5, 3));

    caseSensitiveLbl = new JLabel();
    caseSensitiveLbl.setText(I18n.get(I18n.FILTER_LABEL_CASE_SENSITIVE));
    editPane.add(caseSensitiveLbl, cc.xy(1, 5));
    caseSensitiveCbx = new JCheckBox();
    caseSensitiveCbx.setText(I18n.get(I18n.FILTER_LABEL_ENABLE_CASE_SENSITIVE));
    editPane.add(caseSensitiveCbx, cc.xy(3, 5));

    verbosityLbl = new JLabel();
    verbosityLbl.setText(I18n.get(I18n.FILTER_LABEL_VERBOSITY));
    editPane.add(verbosityLbl, cc.xy(1, 7));
    verbosityCombo = new JComboBox<>();
    for (LogLevel level : LogLevel.values()) {
      verbosityCombo.addItem(level);
    }
    editPane.add(verbosityCombo, cc.xy(3, 7));

    colorLbl = new JLabel();
    colorLbl.setText(I18n.get(I18n.FILTER_LABEL_COLOR));
    colorLbl.setToolTipText(I18n.get(I18n.FILTER_TOOLTIP_COLOR));
    editPane.add(colorLbl, cc.xy(1, 9));
    colorChooser = new JColorChooser();

    // Show a simple text field for preview
    JTextField preview = new JTextField(I18n.get(I18n.FILTER_PREVIEW_COLOR));
    preview.setBorder(new EmptyBorder(UIScaleUtils.dip(5),
            UIScaleUtils.dip(15),
            UIScaleUtils.dip(5),
            UIScaleUtils.dip(15)));
    colorChooser.setPreviewPanel(preview);
    editPane.add(colorChooser, cc.xy(3, 9));

    return editPane;
  }
}
