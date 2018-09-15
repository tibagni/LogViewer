package com.tibagni.logviewer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import javax.swing.*;

public class LogViewerTests {

  @Test
  public void testConvertFromSwingYes() {
    LogViewer.UserSelection result = LogViewer.convertFromSwing(JOptionPane.YES_OPTION);
    assertEquals(LogViewer.UserSelection.CONFIRMED, result);
  }

  @Test
  public void testConvertFromSwingNo() {
    LogViewer.UserSelection result = LogViewer.convertFromSwing(JOptionPane.NO_OPTION);
    assertEquals(LogViewer.UserSelection.REJECTED, result);
  }

  @Test
  public void testConvertFromSwingCancel() {
    LogViewer.UserSelection result = LogViewer.convertFromSwing(JOptionPane.CANCEL_OPTION);
    assertEquals(LogViewer.UserSelection.CANCELLED, result);
  }

  @Test
  public void testConvertFromSwingClose() {
    LogViewer.UserSelection result = LogViewer.convertFromSwing(JOptionPane.CLOSED_OPTION);
    assertEquals(LogViewer.UserSelection.CANCELLED, result);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConvertFromSwingInvalid() {
    LogViewer.convertFromSwing(999);
  }
}
