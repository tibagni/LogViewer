package com.tibagni.logviewer.view;

import javax.swing.*;
import java.awt.*;

public class ProgressDialog extends JDialog {
    private JPanel contentPane;
    private JProgressBar progressbar;
    private JLabel progressText;

    private JFrame owner;

    public ProgressDialog(JFrame owner) {
        super(owner);
        this.owner = owner;
        buildUi();
        setUndecorated(true);
        setContentPane(contentPane);
        getRootPane().setBorder(BorderFactory.createRaisedBevelBorder());
        owner.setEnabled(false); // Do this to simulate a modal dialog without blocking UI Thread
    }

    public void publishProgress(int progress) {
        if (progress > 0 && progressbar.isIndeterminate()) {
            progressbar.setIndeterminate(false);
        }
        progressbar.setValue(progress);
    }

    public void updateProgressText(String text) {
        progressText.setText(text);
    }

    public void finishProgress() {
        owner.setEnabled(true);
        dispose();
    }

    public static ProgressDialog showProgressDialog(JFrame parent) {
        ProgressDialog dialog = new ProgressDialog(parent);

        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        return dialog;
    }

    private void buildUi() {
        contentPane = new JPanel();
        contentPane.setMinimumSize(new Dimension(400, 100));
        contentPane.setPreferredSize(new Dimension(400, 100));

        progressbar = new JProgressBar(0, 100);
        progressbar.setStringPainted(true);
        contentPane.add(progressbar);
        progressbar.setIndeterminate(true);

        progressText = new JLabel();

        JOptionPane optionPane = new JOptionPane(new Object[] {"Loading...", progressText, progressbar},
                JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                new Object[]{},
                null);
        contentPane.add(optionPane);
    }
}
