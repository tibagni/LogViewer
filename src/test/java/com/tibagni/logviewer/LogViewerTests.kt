package com.tibagni.logviewer

import org.junit.Assert.assertEquals
import org.junit.Test
import javax.swing.JOptionPane

class LogViewerTests {
    @Test
    fun testConvertFromSwingYes() {
        val result = LogViewer.convertFromSwing(JOptionPane.YES_OPTION)
        assertEquals(LogViewer.UserSelection.CONFIRMED, result)
    }

    @Test
    fun testConvertFromSwingNo() {
        val result = LogViewer.convertFromSwing(JOptionPane.NO_OPTION)
        assertEquals(LogViewer.UserSelection.REJECTED, result)
    }

    @Test
    fun testConvertFromSwingCancel() {
        val result = LogViewer.convertFromSwing(JOptionPane.CANCEL_OPTION)
        assertEquals(LogViewer.UserSelection.CANCELLED, result)
    }

    @Test
    fun testConvertFromSwingClose() {
        val result = LogViewer.convertFromSwing(JOptionPane.CLOSED_OPTION)
        assertEquals(LogViewer.UserSelection.CANCELLED, result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testConvertFromSwingInvalid() {
        LogViewer.convertFromSwing(999)
    }
}
