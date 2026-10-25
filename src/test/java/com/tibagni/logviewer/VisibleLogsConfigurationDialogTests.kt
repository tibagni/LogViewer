package com.tibagni.logviewer

import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.ui.FlatLineBorder
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.FlatButton
import com.tibagni.logviewer.view.StatusBadge
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.awt.Font
import java.awt.GraphicsEnvironment
import javax.swing.border.EmptyBorder

/**
 * Unit tests for [VisibleLogsConfigurationDialog] validating initial UI
 * state, clear action workflows, badge variant updates, monospace font
 * styling, FlatLaf rounded card borders, and headless environment safety.
 */
class VisibleLogsConfigurationDialogTests {

  private fun createSampleLog(text: String): LogEntry {
    return LogEntry(text, LogLevel.INFO, null)
  }

  @Test
  fun testVisibleLogConfigurationDataClass() {
    val log1 = createSampleLog("Line 1")
    val log2 = createSampleLog("Line 2")
    val config = VisibleLogConfiguration(log1, log2)

    assertEquals(log1, config.startingLog)
    assertEquals(log2, config.endingLog)

    val copyConfig = config.copy(startingLog = null)
    assertNull(copyConfig.startingLog)
    assertEquals(log2, copyConfig.endingLog)
    assertEquals(config, VisibleLogConfiguration(log1, log2))
  }

  @Test
  fun testInitialStateWithBothLogsSet() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val startLog = createSampleLog("Starting log content")
    val endLog = createSampleLog("Ending log content")
    val config = VisibleLogConfiguration(startLog, endLog)
    val dialog = VisibleLogsConfigurationDialog(null, config)

    try {
      assertEquals(startLog, dialog.startingLog)
      assertEquals(endLog, dialog.endingLog)
      assertEquals("Starting log content", dialog.startingPointLogText.text)
      assertEquals("Ending log content", dialog.endingPointLogText.text)

      assertTrue(dialog.clearStartingPointBtn.isEnabled)
      assertTrue(dialog.clearEndingPointBtn.isEnabled)

      assertEquals(
        StatusBadge.Variant.NORMAL,
        dialog.startingBadge.variant
      )
      assertEquals(
        I18n.get(I18n.VISIBLE_LOGS_STATUS_ACTIVE),
        dialog.startingBadge.text
      )
      assertTrue(dialog.startingBadge.isVisible)

      assertEquals(
        StatusBadge.Variant.NORMAL,
        dialog.endingBadge.variant
      )
      assertEquals(
        I18n.get(I18n.VISIBLE_LOGS_STATUS_ACTIVE),
        dialog.endingBadge.text
      )
      assertTrue(dialog.endingBadge.isVisible)

      assertEquals(0, dialog.startingPointLogText.caretPosition)
      assertEquals(0, dialog.endingPointLogText.caretPosition)
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testInitialStateWithOnlyStartingLogSet() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val startLog = createSampleLog("First line")
    val config = VisibleLogConfiguration(startLog, null)
    val dialog = VisibleLogsConfigurationDialog(null, config)

    try {
      assertEquals(startLog, dialog.startingLog)
      assertNull(dialog.endingLog)

      assertEquals("First line", dialog.startingPointLogText.text)
      assertEquals(
        I18n.get(I18n.VISIBLE_LOGS_NO_ENDING_POINT),
        dialog.endingPointLogText.text
      )

      assertTrue(dialog.clearStartingPointBtn.isEnabled)
      assertFalse(dialog.clearEndingPointBtn.isEnabled)

      assertEquals(
        StatusBadge.Variant.NORMAL,
        dialog.startingBadge.variant
      )
      assertEquals(
        StatusBadge.Variant.NO_RESULTS,
        dialog.endingBadge.variant
      )
      assertEquals(
        I18n.get(I18n.VISIBLE_LOGS_STATUS_NOT_SET),
        dialog.endingBadge.text
      )
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testInitialStateWithOnlyEndingLogSet() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val endLog = createSampleLog("Last line")
    val config = VisibleLogConfiguration(null, endLog)
    val dialog = VisibleLogsConfigurationDialog(null, config)

    try {
      assertNull(dialog.startingLog)
      assertEquals(endLog, dialog.endingLog)

      assertEquals(
        I18n.get(I18n.VISIBLE_LOGS_NO_STARTING_POINT),
        dialog.startingPointLogText.text
      )
      assertEquals("Last line", dialog.endingPointLogText.text)

      assertFalse(dialog.clearStartingPointBtn.isEnabled)
      assertTrue(dialog.clearEndingPointBtn.isEnabled)

      assertEquals(
        StatusBadge.Variant.NO_RESULTS,
        dialog.startingBadge.variant
      )
      assertEquals(
        I18n.get(I18n.VISIBLE_LOGS_STATUS_NOT_SET),
        dialog.startingBadge.text
      )
      assertEquals(
        StatusBadge.Variant.NORMAL,
        dialog.endingBadge.variant
      )
      assertEquals(
        I18n.get(I18n.VISIBLE_LOGS_STATUS_ACTIVE),
        dialog.endingBadge.text
      )
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testInitialStateWithNeitherLogSet() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val config = VisibleLogConfiguration(null, null)
    val dialog = VisibleLogsConfigurationDialog(null, config)

    try {
      assertNull(dialog.startingLog)
      assertNull(dialog.endingLog)

      assertEquals(
        I18n.get(I18n.VISIBLE_LOGS_NO_STARTING_POINT),
        dialog.startingPointLogText.text
      )
      assertEquals(
        I18n.get(I18n.VISIBLE_LOGS_NO_ENDING_POINT),
        dialog.endingPointLogText.text
      )

      assertFalse(dialog.clearStartingPointBtn.isEnabled)
      assertFalse(dialog.clearEndingPointBtn.isEnabled)

      assertEquals(
        StatusBadge.Variant.NO_RESULTS,
        dialog.startingBadge.variant
      )
      assertEquals(
        StatusBadge.Variant.NO_RESULTS,
        dialog.endingBadge.variant
      )
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testClearStartingPointUpdatesState() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val startLog = createSampleLog("Start")
    val endLog = createSampleLog("End")
    val config = VisibleLogConfiguration(startLog, endLog)
    val dialog = VisibleLogsConfigurationDialog(null, config)

    try {
      dialog.clearStartingPointBtn.doClick()

      assertNull(dialog.startingLog)
      assertEquals(endLog, dialog.endingLog)

      assertEquals(
        I18n.get(I18n.VISIBLE_LOGS_NO_STARTING_POINT),
        dialog.startingPointLogText.text
      )
      assertFalse(dialog.clearStartingPointBtn.isEnabled)

      assertEquals(
        StatusBadge.Variant.NO_RESULTS,
        dialog.startingBadge.variant
      )
      assertEquals(
        I18n.get(I18n.VISIBLE_LOGS_STATUS_NOT_SET),
        dialog.startingBadge.text
      )

      assertEquals("End", dialog.endingPointLogText.text)
      assertTrue(dialog.clearEndingPointBtn.isEnabled)
      assertEquals(
        StatusBadge.Variant.NORMAL,
        dialog.endingBadge.variant
      )
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testClearEndingPointUpdatesState() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val startLog = createSampleLog("Start")
    val endLog = createSampleLog("End")
    val config = VisibleLogConfiguration(startLog, endLog)
    val dialog = VisibleLogsConfigurationDialog(null, config)

    try {
      dialog.clearEndingPointBtn.doClick()

      assertEquals(startLog, dialog.startingLog)
      assertNull(dialog.endingLog)

      assertEquals(
        I18n.get(I18n.VISIBLE_LOGS_NO_ENDING_POINT),
        dialog.endingPointLogText.text
      )
      assertFalse(dialog.clearEndingPointBtn.isEnabled)

      assertEquals(
        StatusBadge.Variant.NO_RESULTS,
        dialog.endingBadge.variant
      )
      assertEquals(
        I18n.get(I18n.VISIBLE_LOGS_STATUS_NOT_SET),
        dialog.endingBadge.text
      )

      assertEquals("Start", dialog.startingPointLogText.text)
      assertTrue(dialog.clearStartingPointBtn.isEnabled)
      assertEquals(
        StatusBadge.Variant.NORMAL,
        dialog.startingBadge.variant
      )
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testOnOkSetsReturnConfigurationAndCloses() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val startLog = createSampleLog("Start")
    val endLog = createSampleLog("End")
    val config = VisibleLogConfiguration(startLog, endLog)
    val dialog = VisibleLogsConfigurationDialog(null, config)

    try {
      dialog.clearStartingPointBtn.doClick()
      dialog.onOk()

      val result = dialog.returnConfiguration
      assertNotNull(result)
      assertNull(result?.startingLog)
      assertEquals(endLog, result?.endingLog)
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testOnCancelSetsNullReturnConfiguration() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val startLog = createSampleLog("Start")
    val endLog = createSampleLog("End")
    val config = VisibleLogConfiguration(startLog, endLog)
    val dialog = VisibleLogsConfigurationDialog(null, config)

    try {
      dialog.onCancel()
      assertNull(dialog.returnConfiguration)
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testMonospaceFontAndBorderStyling() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val config = VisibleLogConfiguration(null, null)
    val dialog = VisibleLogsConfigurationDialog(null, config)

    try {
      assertEquals(
        Font.MONOSPACED,
        dialog.startingPointLogText.font.name
      )
      assertEquals(
        Font.MONOSPACED,
        dialog.endingPointLogText.font.name
      )
      assertFalse(dialog.startingPointLogText.isEditable)
      assertFalse(dialog.endingPointLogText.isEditable)
      assertTrue(dialog.startingPointLogText.lineWrap)
      assertTrue(dialog.endingPointLogText.lineWrap)

      assertTrue(dialog.startingPointLogText.border is EmptyBorder)
      val insets = (
        dialog.startingPointLogText.border as EmptyBorder
      ).borderInsets
      assertEquals(UIScaleUtils.dip(6), insets.top)
      assertEquals(UIScaleUtils.dip(8), insets.left)
      assertEquals(UIScaleUtils.dip(6), insets.bottom)
      assertEquals(UIScaleUtils.dip(8), insets.right)

      assertTrue(dialog.startingCard.border is FlatLineBorder)
      val cardBorder = dialog.startingCard.border as FlatLineBorder
      assertEquals(UIScaleUtils.dip(8), cardBorder.arc)
      assertEquals(1f, cardBorder.lineThickness, 0.001f)
      assertNotNull(cardBorder.lineColor)

      assertTrue(dialog.startingScrollPane.border is FlatLineBorder)
      val scrollBorder = (
        dialog.startingScrollPane.border as FlatLineBorder
      )
      assertEquals(UIScaleUtils.dip(4), scrollBorder.arc)
      assertEquals(1f, scrollBorder.lineThickness, 0.001f)
      assertNotNull(scrollBorder.lineColor)
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testClearButtonsAreFlatButtons() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val config = VisibleLogConfiguration(null, null)
    val dialog = VisibleLogsConfigurationDialog(null, config)

    try {
      assertEquals(
        FlatButton::class.java,
        dialog.clearStartingPointBtn.javaClass
      )
      assertEquals(
        FlatButton::class.java,
        dialog.clearEndingPointBtn.javaClass
      )
      assertEquals(
        FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON,
        dialog.clearStartingPointBtn.getClientProperty(
          FlatClientProperties.BUTTON_TYPE
        )
      )
      assertEquals(
        FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON,
        dialog.clearEndingPointBtn.getClientProperty(
          FlatClientProperties.BUTTON_TYPE
        )
      )
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testColorAndForegroundResolutions() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val config = VisibleLogConfiguration(null, null)
    val dialog = VisibleLogsConfigurationDialog(null, config)

    try {
      assertNotNull(dialog.resolveBorderColor())
      assertNotNull(dialog.resolveDisabledForeground())
      assertNotNull(dialog.resolveDefaultTextForeground())
    } finally {
      dialog.dispose()
    }
  }
}
