package com.tibagni.logviewer.view;

import javax.swing.*;

public class ProgressDialog extends JDialog {
    private JPanel contentPane;
    private JProgressBar progressbar;
    private JTextArea progressText;
    private final JFrame owner;
    private final String initialText;

    public ProgressDialog(JFrame owner) {
       this(owner, "Loading...");
    }

    public ProgressDialog(JFrame owner, String initialText) {
        super(owner);
        this.owner = owner;
        this.initialText = initialText;
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
        progressbar = new JProgressBar(0, 100);
        progressbar.setStringPainted(true);
        contentPane.add(progressbar);
        progressbar.setIndeterminate(true);

        progressText = new JTextArea(4, 50);
        progressText.setLineWrap(true);
        progressText.setWrapStyleWord(true);
        progressText.setEditable(false);
        progressText.setText(initialText);

        JOptionPane optionPane = new JOptionPane(new Object[] {progressText, progressbar},
                JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                new Object[]{},
                null);
        contentPane.add(optionPane);
    }
}
