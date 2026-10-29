package com.tibagni.logviewer.filter;

import com.formdev.flatlaf.FlatClientProperties;
import com.tibagni.logviewer.ServiceLocator;
import com.tibagni.logviewer.annotation.VisibleForTesting;
import com.tibagni.logviewer.filter.regex.RegexEditorDialog;
import com.tibagni.logviewer.i18n.I18n;
import com.tibagni.logviewer.log.LogLevel;
import com.tibagni.logviewer.theme.LogViewerThemeManager;
import com.tibagni.logviewer.util.StringUtils;
import com.tibagni.logviewer.util.scaling.UIScaleUtils;
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder;
import com.tibagni.logviewer.view.ButtonsPane;
import com.tibagni.logviewer.view.CardBorderUtils;
import com.tibagni.logviewer.view.CommonIcons;
import com.tibagni.logviewer.view.FlatButton;
import com.tibagni.logviewer.view.color.ModernColorChooserPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

/**
 * Dialog for creating and editing log filters.
 *
 * Provides filter name, regular expression input with embedded editor, case sensitivity,
 * verbosity constraints, and a full-featured color chooser with an uncropped text preview.
 */
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
  private FlatButton regexEditorBtn;
  private JColorChooser colorChooser;
  private ModernColorChooserPanel modernColorChooser;

  @FunctionalInterface
  public interface DuplicateCheckCallback {
    FilterMatch check(String pattern, boolean isCaseSensitive);
  }

  private Filter filter;
  private final Filter editingFilter;
  private final DuplicateCheckCallback duplicateCheckCallback;
  private String previewText;
  private final LogViewerThemeManager themeManager;

  private final DocumentListener regexDocumentListener =
      new DocumentListener() {
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

  EditFilterDialog(Frame owner, Filter editingFilter,
      DuplicateCheckCallback duplicateCheckCallback) {
    this(owner, editingFilter, null, duplicateCheckCallback);
  }

  EditFilterDialog(Frame owner, Filter editingFilter, String preDefinedText) {
    this(owner, editingFilter, preDefinedText, null);
  }

  EditFilterDialog(Frame owner, Filter editingFilter, String preDefinedText,
      DuplicateCheckCallback duplicateCheckCallback) {
    super(owner);
    this.editingFilter = editingFilter;
    this.duplicateCheckCallback = duplicateCheckCallback;
    previewText = preDefinedText;
    themeManager = ServiceLocator.INSTANCE.getThemeManager();
    setTitle(I18n.get(editingFilter != null ?
        I18n.FILTER_DIALOG_EDIT_TITLE : I18n.FILTER_DIALOG_NEW_TITLE));
    buildUi();

    setContentPane(contentPane);
    setModal(true);
    buttonsPane.setDefaultButtonOk();

    regexEditorBtn.addActionListener(e -> onEditRegex());

    Color initialColor = getInitialColor();
    colorChooser.setColor(initialColor);

    // call onCancel() when cross is clicked
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        onCancel();
      }
    });

    // call onCancel() on ESCAPE
    contentPane.registerKeyboardAction(e -> onCancel(),
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
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
            regexTxt.getDocument()
                .removeDocumentListener(regexDocumentListener);
            nameTxt.setEnabled(true);
            nameTxt.requestFocus();
            nameTxt.selectAll();
          }
        }
      });
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
    Color selectedColor = colorChooser.getColor();
    String name = nameTxt.getText();
    String pattern = regexTxt.getText();
    boolean caseSensitive = caseSensitiveCbx.isSelected();
    LogLevel verbosity = (LogLevel) verbosityCombo.getSelectedItem();

    FilterMatch duplicateMatch =
        checkForDuplicateFilter(pattern, caseSensitive);
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
          new Object[]{I18n.get(I18n.FILTER_DUPLICATE_ADD_ANYWAY),
              I18n.get(I18n.COMMON_CANCEL)},
          I18n.get(I18n.COMMON_CANCEL)
      );
      if (choice != JOptionPane.YES_OPTION) {
        return;
      }
    }

    try {
      if (filter == null) {
        filter = new Filter(name, pattern, selectedColor,
            verbosity, caseSensitive);
      } else {
        filter.updateFilter(name, pattern, selectedColor,
            verbosity, caseSensitive);
      }
    } catch (FilterException e) {
      JOptionPane.showConfirmDialog(this, e.getMessage(),
          I18n.get(I18n.COMMON_ERROR),
          JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
      return;
    }

    dispose();
  }

  FilterMatch checkForDuplicateFilter(String newPattern,
      boolean caseSensitive) {
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
    RegexEditorDialog.Result edited =
        RegexEditorDialog.showEditRegexDialog(this, this,
            regexTxt.getText(), previewText, caseSensitiveCbx.isSelected());

    if (edited != null) {
      regexTxt.setText(edited.pattern);
      caseSensitiveCbx.setSelected(edited.caseSensitive);
    }
  }

  public static Color getRandomColor(boolean isDark) {
    Random r = new Random();
    Color[] colors = isDark ? INITIAL_COLORS_DARK : INITIAL_COLORS_LIGHT;
    return colors[r.nextInt(colors.length)];
  }

  private Color getInitialColor() {
    return getRandomColor(themeManager.isDark());
  }

  public static Filter showEditFilterDialog(Frame parent,
      Filter editingFilter, DuplicateCheckCallback duplicateCheckCallback) {
    EditFilterDialog dialog =
        new EditFilterDialog(parent, editingFilter, duplicateCheckCallback);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setVisible(true);

    return dialog.filter;
  }

  public static Filter showEditFilterDialog(Frame parent,
      Filter editingFilter) {
    return showEditFilterDialog(parent, editingFilter, null);
  }

  public static Filter showEditFilterDialog(Frame parent,
      DuplicateCheckCallback duplicateCheckCallback) {
    return showEditFilterDialog(parent, null, duplicateCheckCallback);
  }

  public static Filter showEditFilterDialog(Frame parent) {
    return showEditFilterDialog(parent, null, null);
  }

  // This is used to create a Filter from an existing predefined String
  // It will open the Edit Dialog directly on the RegEx Editor
  public static Filter showEditFilterDialogWithText(Frame parent,
      String preDefinedText, DuplicateCheckCallback duplicateCheckCallback) {
    EditFilterDialog dialog = new EditFilterDialog(parent, null,
        preDefinedText, duplicateCheckCallback);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setVisible(true);

    return dialog.filter;
  }

  public static Filter showEditFilterDialogWithText(Frame parent,
      String preDefinedText) {
    return showEditFilterDialogWithText(parent, preDefinedText, null);
  }

  private void buildUi() {
    contentPane = new JPanel();
    contentPane.setLayout(new GridBagLayout());
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

    contentPane.add(buildEditPane(),
        new GBConstraintsBuilder()
            .withGridx(0)
            .withGridy(0)
            .withWeightx(1.0)
            .withWeighty(1.0)
            .withFill(GridBagConstraints.BOTH)
            .build());
  }

  /**
   * Constructs the main editing panel containing the rules and color cards.
   *
   * @return the editing container panel
   */
  private JPanel buildEditPane() {
    final JPanel editPane = new JPanel(new GridBagLayout());
    editPane.setOpaque(false);

    editPane.add(buildFilterRulesCard(),
        new GBConstraintsBuilder()
            .withGridx(0).withGridy(0)
            .withWeightx(1.0)
            .withFill(GridBagConstraints.HORIZONTAL)
            .withInsets(new Insets(0, 0, UIScaleUtils.dip(8), 0))
            .build());

    editPane.add(buildColorCard(),
        new GBConstraintsBuilder()
            .withGridx(0).withGridy(1)
            .withWeightx(1.0).withWeighty(1.0)
            .withFill(GridBagConstraints.BOTH)
            .build());

    return editPane;
  }

  /**
   * Constructs the filter rules card with name, regex, and options inputs.
   *
   * @return the filter rules card panel
   */
  private JPanel buildFilterRulesCard() {
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

    nameLbl = new JLabel(I18n.get(I18n.FILTER_LABEL_NAME));
    nameLbl.setToolTipText(I18n.get(I18n.FILTER_TOOLTIP_NAME));
    nameLbl.setForeground(secondaryColor);

    nameTxt = new JTextField();
    nameTxt.setEnabled(true);
    nameTxt.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);

    regexLbl = new JLabel(I18n.get(I18n.FILTER_LABEL_REGEX));
    regexLbl.setToolTipText(I18n.get(I18n.FILTER_TOOLTIP_REGEX));
    regexLbl.setForeground(secondaryColor);

    regexTxt = new JTextField();
    regexTxt.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
    regexTxt.putClientProperty(
        FlatClientProperties.TEXT_FIELD_LEADING_ICON, CommonIcons.searchIcon(14));
    regexTxt.setFont(new Font(Font.MONOSPACED, Font.PLAIN, regexTxt.getFont().getSize()));

    regexEditorBtn = new FlatButton();
    regexEditorBtn.setText(I18n.get(I18n.FILTER_BTN_REGEX_EDITOR));
    regexEditorBtn.setIcon(CommonIcons.chevronRightIcon(14));
    regexEditorBtn.setIconTextGap(UIScaleUtils.dip(4));
    regexEditorBtn.setToolTipText(I18n.get(I18n.FILTER_TOOLTIP_REGEX_EDITOR));

    caseSensitiveLbl = new JLabel(I18n.get(I18n.FILTER_LABEL_CASE_SENSITIVE));
    caseSensitiveLbl.setForeground(secondaryColor);

    caseSensitiveCbx = new JCheckBox(I18n.get(I18n.FILTER_LABEL_ENABLE_CASE_SENSITIVE));
    caseSensitiveCbx.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    verbosityLbl = new JLabel(I18n.get(I18n.FILTER_LABEL_VERBOSITY));
    verbosityLbl.setForeground(secondaryColor);

    verbosityCombo = new JComboBox<>();
    for (LogLevel level : LogLevel.values()) {
      verbosityCombo.addItem(level);
    }

    // Row 0: Name
    card.add(nameLbl,
        new GBConstraintsBuilder().withGridx(0).withGridy(0)
            .withAnchor(GridBagConstraints.WEST)
            .withInsets(new Insets(0, 0, UIScaleUtils.dip(6), UIScaleUtils.dip(8)))
            .build());
    card.add(nameTxt,
        new GBConstraintsBuilder().withGridx(1).withGridy(0)
            .withGridWidth(2)
            .withWeightx(1.0).withFill(GridBagConstraints.HORIZONTAL)
            .withInsets(new Insets(0, 0, UIScaleUtils.dip(6), 0))
            .build());

    // Row 1: RegEx
    card.add(regexLbl,
        new GBConstraintsBuilder().withGridx(0).withGridy(1)
            .withAnchor(GridBagConstraints.WEST)
            .withInsets(new Insets(0, 0, UIScaleUtils.dip(6), UIScaleUtils.dip(8)))
            .build());
    card.add(regexTxt,
        new GBConstraintsBuilder().withGridx(1).withGridy(1)
            .withWeightx(1.0).withFill(GridBagConstraints.HORIZONTAL)
            .withInsets(new Insets(0, 0, UIScaleUtils.dip(6), UIScaleUtils.dip(6)))
            .build());
    card.add(regexEditorBtn,
        new GBConstraintsBuilder().withGridx(2).withGridy(1)
            .withInsets(new Insets(0, 0, UIScaleUtils.dip(6), 0))
            .build());

    // Row 2: Options (Case Sensitive & Verbosity)
    card.add(caseSensitiveLbl,
        new GBConstraintsBuilder().withGridx(0).withGridy(2)
            .withAnchor(GridBagConstraints.WEST)
            .withInsets(new Insets(0, 0, 0, UIScaleUtils.dip(8)))
            .build());

    JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    optionsPanel.setOpaque(false);
    optionsPanel.add(caseSensitiveCbx);
    optionsPanel.add(Box.createHorizontalStrut(UIScaleUtils.dip(16)));
    optionsPanel.add(verbosityLbl);
    optionsPanel.add(Box.createHorizontalStrut(UIScaleUtils.dip(6)));
    optionsPanel.add(verbosityCombo);

    card.add(optionsPanel,
        new GBConstraintsBuilder().withGridx(1).withGridy(2)
            .withGridWidth(2)
            .withWeightx(1.0).withFill(GridBagConstraints.HORIZONTAL)
            .build());

    return card;
  }

  /**
   * Constructs the color chooser card with modern and legacy panels and preview.
   *
   * @return the color selection card panel
   */
  private JPanel buildColorCard() {
    final JPanel card = new JPanel(new BorderLayout(0, UIScaleUtils.dip(6)));
    card.setOpaque(false);
    card.setBorder(BorderFactory.createCompoundBorder(
        CardBorderUtils.createCardBorder(),
        new EmptyBorder(
            UIScaleUtils.dip(8),
            UIScaleUtils.dip(10),
            UIScaleUtils.dip(8),
            UIScaleUtils.dip(10))));

    colorLbl = new JLabel(I18n.get(I18n.FILTER_LABEL_COLOR));
    colorLbl.setToolTipText(I18n.get(I18n.FILTER_TOOLTIP_COLOR));
    colorLbl.setForeground(CardBorderUtils.resolveSecondaryLabelColor());
    colorLbl.setFont(colorLbl.getFont().deriveFont(Font.BOLD));
    card.add(colorLbl, BorderLayout.NORTH);

    colorChooser = new JColorChooser();
    modernColorChooser = new ModernColorChooserPanel(getInitialColor(), themeManager, false);

    // Keep all legacy chooser panels (Swatches, HSV, HSL, RGB, CMYK) and prepend Modern panel
    AbstractColorChooserPanel[] existingPanels = colorChooser.getChooserPanels();
    AbstractColorChooserPanel[] allPanels =
        new AbstractColorChooserPanel[existingPanels.length + 1];
    allPanels[0] = modernColorChooser;
    System.arraycopy(existingPanels, 0, allPanels, 1, existingPanels.length);
    colorChooser.setChooserPanels(allPanels);

    // Show an uncropped text label for preview across all panels
    JLabel preview = new JLabel(I18n.get(I18n.FILTER_PREVIEW_COLOR), SwingConstants.CENTER) {
      @Override
      public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, Math.max(d.height, UIScaleUtils.dip(42)));
      }
    };
    preview.setFont(preview.getFont().deriveFont(Font.BOLD, UIScaleUtils.dipf(13f)));
    preview.setBorder(BorderFactory.createCompoundBorder(
        CardBorderUtils.createCardBorder(),
        new EmptyBorder(
            UIScaleUtils.dip(10),
            UIScaleUtils.dip(18),
            UIScaleUtils.dip(10),
            UIScaleUtils.dip(18))));
    preview.setForeground(getInitialColor());
    colorChooser.setPreviewPanel(preview);
    colorChooser.getSelectionModel().addChangeListener(e ->
        preview.setForeground(colorChooser.getColor()));

    card.add(colorChooser, BorderLayout.CENTER);
    return card;
  }

  /**
   * Returns the modern color chooser component.
   *
   * @return the modern color chooser
   */
  public ModernColorChooserPanel getModernColorChooser() {
    return modernColorChooser;
  }

  /**
   * Returns the color chooser component.
   *
   * @return the color chooser
   */
  public JColorChooser getColorChooser() {
    return colorChooser;
  }

  /**
   * Returns the regex editor flat button.
   *
   * @return the regex editor button
   */
  public FlatButton getRegexEditorBtn() {
    return regexEditorBtn;
  }

  /**
   * Returns the regex pattern input field.
   *
   * @return the regex text field
   */
  public JTextField getRegexTxt() {
    return regexTxt;
  }

  /**
   * Returns the filter name input field.
   *
   * @return the name text field
   */
  public JTextField getNameTxt() {
    return nameTxt;
  }

  /**
   * Returns the name label.
   *
   * @return the name label
   */
  public JLabel getNameLbl() {
    return nameLbl;
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
   * Returns the case sensitive label.
   *
   * @return the case sensitive label
   */
  public JLabel getCaseSensitiveLbl() {
    return caseSensitiveLbl;
  }

  /**
   * Returns the verbosity label.
   *
   * @return the verbosity label
   */
  public JLabel getVerbosityLbl() {
    return verbosityLbl;
  }

  /**
   * Returns the color label.
   *
   * @return the color label
   */
  public JLabel getColorLbl() {
    return colorLbl;
  }

  /**
   * Returns the case sensitive checkbox.
   *
   * @return the case sensitive checkbox
   */
  @VisibleForTesting
  public JCheckBox getCaseSensitiveCbx() {
    return caseSensitiveCbx;
  }

  /**
   * Returns the verbosity combobox.
   *
   * @return the verbosity combobox
   */
  @VisibleForTesting
  public JComboBox<LogLevel> getVerbosityCombo() {
    return verbosityCombo;
  }
}
