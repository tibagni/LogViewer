package com.tibagni.logviewer.filter;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.tibagni.logviewer.ServiceLocator;
import com.tibagni.logviewer.filter.regex.RegexEditorDialog;
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
  private JButton regexEditorBtn;
  private JColorChooser colorChooser;

  private Filter filter;
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

  private EditFilterDialog(Frame owner, Filter editingFilter) {
    this(owner, editingFilter, null);
  }

  private EditFilterDialog(Frame owner, Filter editingFilter, String preDefinedText) {
    super(owner);
    previewText = preDefinedText;
    themeManager = ServiceLocator.INSTANCE.getThemeManager();
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

  public static Filter showEditFilterDialog(Frame parent, Filter editingFilter) {
    EditFilterDialog dialog = new EditFilterDialog(parent, editingFilter);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setVisible(true);

    return dialog.filter;
  }

  public static Filter showEditFilterDialog(Frame parent) {
    return showEditFilterDialog(parent, null);
  }

  // This is used to create a Filter from an existing predefined String
  // It will open the Edit Dialog directly on the RegEx Editor
  public static Filter showEditFilterDialogWithText(Frame parent, String preDefinedText) {
    EditFilterDialog dialog = new EditFilterDialog(parent, null, preDefinedText);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setVisible(true);

    return dialog.filter;
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
        "center:d:noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow,top:3dlu:noGrow,center:max(d;4px):noGrow"));

    nameLbl = new JLabel();
    nameLbl.setText("Filter name:");
    nameLbl.setToolTipText("Give a name to your filter to appear on the filters list");
    CellConstraints cc = new CellConstraints();
    editPane.add(nameLbl, cc.xy(1, 1));
    nameTxt = new JTextField();
    nameTxt.setEnabled(true);
    editPane.add(nameTxt, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

    regexLbl = new JLabel();
    regexLbl.setText("Regex:");
    regexLbl.setToolTipText("The regular expression of your filter");
    editPane.add(regexLbl, cc.xy(1, 3));
    regexTxt = new JTextField();
    editPane.add(regexTxt, cc.xy(3, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
    regexEditorBtn = new JButton();
    regexEditorBtn.setText("Editor");
    regexEditorBtn.setToolTipText("Open the regex editor window");
    editPane.add(regexEditorBtn, cc.xy(5, 3));

    caseSensitiveLbl = new JLabel();
    caseSensitiveLbl.setText("Case sensitive:");
    editPane.add(caseSensitiveLbl, cc.xy(1, 5));
    caseSensitiveCbx = new JCheckBox();
    caseSensitiveCbx.setText("Enable case sensitive for this filter");
    editPane.add(caseSensitiveCbx, cc.xy(3, 5));

    colorLbl = new JLabel();
    colorLbl.setText("Color:");
    colorLbl.setToolTipText("Choose a color to differentiate your filter");
    editPane.add(colorLbl, cc.xy(1, 7));
    colorChooser = new JColorChooser();

    // Show a simple text field for preview
    JTextField preview = new JTextField("Filtered text color preview");
    preview.setBorder(new EmptyBorder(UIScaleUtils.dip(5),
            UIScaleUtils.dip(15),
            UIScaleUtils.dip(5),
            UIScaleUtils.dip(15)));
    colorChooser.setPreviewPanel(preview);
    editPane.add(colorChooser, cc.xy(3, 7));

    return editPane;
  }
}
