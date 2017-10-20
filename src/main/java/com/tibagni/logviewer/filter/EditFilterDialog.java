package com.tibagni.logviewer.filter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class EditFilterDialog extends JDialog {
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
  private JButton chooseColorButton;
  private JPanel colorPreview;
  private Color selectedColor;

  private Filter filter;

  private EditFilterDialog(Filter editingFilter) {
    setSelectedColor(Color.RED); // Set RED by default

    setContentPane(contentPane);
    setModal(true);
    getRootPane().setDefaultButton(buttonOK);

    buttonOK.addActionListener(e -> onOK());
    buttonCancel.addActionListener(e -> onCancel());
    chooseColorButton.addActionListener(e -> onSelectColor());

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

    if (editingFilter != null) {
      filter = editingFilter;
      nameTxt.setText(filter.getName());
      regexTxt.setText(filter.getPatternString());
      setSelectedColor(filter.getColor());
      caseSensitiveCbx.setSelected(filter.isCaseSensitive());
    }
  }

  private void onOK() {
    // add your code here
    Color selectedColor = colorPreview.getBackground();
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
    dispose();
  }

  private void onSelectColor() {
    Color color = JColorChooser.showDialog(this, "Select Color...", selectedColor);
    if (color != null) {
      setSelectedColor(color);
    }
  }

  private void setSelectedColor(Color color) {
    selectedColor = color;
    colorPreview.setBackground(selectedColor);
    colorPreview.invalidate();
  }

  public static Filter showEditFilterDialog(Component relativeTo, Filter editingFilter) {
    EditFilterDialog dialog = new EditFilterDialog(editingFilter);
    dialog.setLocationRelativeTo(relativeTo);
    dialog.pack();
    dialog.setVisible(true);

    return dialog.filter;
  }

  public static Filter showEditFilterDialog(Component relativeTo) {
    return showEditFilterDialog(relativeTo, null);
  }
}
