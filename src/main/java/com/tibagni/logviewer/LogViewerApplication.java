package com.tibagni.logviewer;

import javax.swing.*;

public class LogViewerApplication {
  private static final String APPLICATION_NAME = "Log Viewer";


  public static void main(String[] args) {
    JFrame frame = new JFrame(APPLICATION_NAME);
    LogViewerView logViewer = new LogViewerView();

    frame.setContentPane(logViewer.getContentPane());
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.pack();
    frame.setVisible(true);
  }
}
