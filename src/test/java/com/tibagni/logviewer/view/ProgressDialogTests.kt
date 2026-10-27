package com.tibagni.logviewer.view

import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.ui.FlatLineBorder
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.awt.GraphicsEnvironment
import javax.swing.JFrame

/**
 * Unit tests verifying [ProgressDialog] card border styling, progress
 * bar properties, status icons, animation, and text updates with headless execution safety.
 */
class ProgressDialogTests {

  @Test
  fun testCardBorderAndComponentSetup() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val frame = JFrame()
    val dialog = ProgressDialog(frame, "Initial progress text", null, false)

    try {
      assertNotNull(dialog.contentPane)
      val border = dialog.rootPane.border
      assertTrue("RootPane border must be FlatLineBorder", border is FlatLineBorder)
      val lineBorder = border as FlatLineBorder
      assertEquals(UIScaleUtils.dip(CardBorderUtils.DEFAULT_CARD_ARC), lineBorder.arc)

      assertTrue(dialog.progressbar.isIndeterminate)
      assertTrue(dialog.progressbar.isStringPainted)
      assertEquals(
        true,
        dialog.progressbar.getClientProperty(FlatClientProperties.PROGRESS_BAR_LARGE_HEIGHT)
      )
      assertEquals(I18n.get(I18n.COMMON_LOADING), dialog.titleLabel.text)
      assertTrue(dialog.titleLabel.font.isBold)
      assertEquals("Initial progress text", dialog.progressText.text)
      assertFalse(dialog.progressText.isEditable)
      assertFalse(dialog.iconPanel.isVisible)
    } finally {
      dialog.finishProgress()
      frame.dispose()
    }
  }

  @Test
  fun testDefaultAnimatedSpinnerAndIconOption() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val frame = JFrame()
    val dialog = ProgressDialog(frame)

    try {
      assertEquals(I18n.get(I18n.COMMON_LOADING), dialog.progressText.text)
      assertNull(dialog.statusIcon.icon)
      assertFalse(dialog.statusIcon.isVisible)
      assertTrue(dialog.loadingSpinner.isVisible)
      assertTrue(dialog.iconPanel.isVisible)

      val testIcon = CommonIcons.fileDocumentIcon(24)
      dialog.updateProgressIcon(testIcon)
      assertEquals(testIcon, dialog.statusIcon.icon)
      assertTrue(dialog.statusIcon.isVisible)
      assertFalse(dialog.loadingSpinner.isVisible)
      assertTrue(dialog.iconPanel.isVisible)

      dialog.updateProgressIcon(null)
      assertNull(dialog.statusIcon.icon)
      assertFalse(dialog.statusIcon.isVisible)
      assertTrue(dialog.loadingSpinner.isVisible)
      assertTrue(dialog.iconPanel.isVisible)
    } finally {
      dialog.finishProgress()
      frame.dispose()
    }
  }

  @Test
  fun testNoAnimationOptionHidesIconPanelWhenNoIcon() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val frame = JFrame()
    val dialog = ProgressDialog(frame, "Loading...", null, false)

    try {
      assertFalse(dialog.iconPanel.isVisible)
      assertFalse(dialog.loadingSpinner.isVisible)

      val testIcon = CommonIcons.smartphoneIcon(24)
      dialog.updateProgressIcon(testIcon)
      assertTrue(dialog.iconPanel.isVisible)
      assertTrue(dialog.statusIcon.isVisible)

      dialog.updateProgressIcon(null)
      assertFalse(dialog.iconPanel.isVisible)
    } finally {
      dialog.finishProgress()
      frame.dispose()
    }
  }

  @Test
  fun testPublishProgressUpdatesProgressbarIndependently() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val frame = JFrame()
    val dialog = ProgressDialog(frame)

    try {
      assertTrue(dialog.progressbar.isIndeterminate)
      dialog.publishProgress(42)
      assertFalse(dialog.progressbar.isIndeterminate)
      assertEquals(42, dialog.progressbar.value)

      val testIcon = CommonIcons.fileDocumentIcon(24)
      dialog.updateProgressIcon(testIcon)
      assertEquals(testIcon, dialog.statusIcon.icon)
      assertTrue(dialog.iconPanel.isVisible)

      dialog.publishProgress(85)
      assertEquals(85, dialog.progressbar.value)
      assertEquals(testIcon, dialog.statusIcon.icon)
    } finally {
      dialog.finishProgress()
      frame.dispose()
    }
  }

  @Test
  fun testUpdateProgressText() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val frame = JFrame()
    val dialog = ProgressDialog(frame)

    try {
      dialog.updateProgressText("Reading log entries...")
      assertEquals("Reading log entries...", dialog.progressText.text)

      dialog.updateProgressText(null)
      assertEquals("", dialog.progressText.text)

      val longPath = "/home/user/very/long/nested/path/to/bugreport-system-2026-10-27-dump.txt"
      dialog.updateProgressText("Reading $longPath...")
      assertEquals("Reading $longPath...", dialog.progressText.text)
      assertTrue(dialog.contentPane.preferredSize.width >= UIScaleUtils.dip(460))
    } finally {
      dialog.finishProgress()
      frame.dispose()
    }
  }

  @Test
  fun testFinishProgressRestoresOwnerFrame() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val frame = JFrame()
    val dialog = ProgressDialog(frame)

    assertFalse(frame.isEnabled)
    dialog.finishProgress()
    assertTrue(frame.isEnabled)
    frame.dispose()
  }
}
