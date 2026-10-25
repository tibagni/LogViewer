package com.tibagni.logviewer.about

import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.ui.FlatLineBorder
import com.tibagni.logviewer.AppInfo
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.updates.ReleaseInfo
import com.tibagni.logviewer.updates.UpdateManager
import com.tibagni.logviewer.updates.UpdateManager.UpdateListener
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.StatusBadge
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.awt.Cursor
import java.awt.GraphicsEnvironment

/**
 * Unit tests verifying [AboutDialog] presentation, state transitions,
 * status badge styling, and headless execution safety.
 */
class AboutDialogTests {

  /**
   * Test double for [UpdateManager] that suppresses network requests.
   *
   * @param listener the update callback listener.
   */
  private class FakeUpdateManager(
    listener: UpdateListener
  ) : UpdateManager(listener) {
    override fun checkForUpdates() {
      // No-op in tests to prevent background network operations
    }
  }

  /**
   * Helper creating a test [ReleaseInfo] payload.
   *
   * @param tag the version tag name.
   * @return the constructed [ReleaseInfo].
   */
  private fun createReleaseInfo(tag: String = "v3.0.0"): ReleaseInfo {
    val json = JSONObject().apply {
      put("tag_name", tag)
      put("html_url", "https://github.com/tibagni/LogViewer/releases/$tag")
      put("body", "Release notes for $tag")
    }
    return ReleaseInfo(json)
  }

  /**
   * Verifies initial dialog state including application branding,
   * GitHub repository link, progress bar visibility, and hidden update button.
   */
  @Test
  fun testInitialStateAndBranding() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    var capturedListener: UpdateListener? = null
    val dialog = AboutDialog(null) { listener ->
      capturedListener = listener
      FakeUpdateManager(listener)
    }

    try {
      assertNotNull(capturedListener)
      val expectedTitle = I18n.format(
        I18n.ABOUT_VERSION_FORMAT,
        AppInfo.APPLICATION_NAME,
        AppInfo.currentVersion
      )
      assertEquals(expectedTitle, dialog.applicationName.text)
      assertTrue(dialog.openSourceInfo.text.contains("github"))
      assertEquals(
        Cursor.HAND_CURSOR,
        dialog.openSourceInfo.cursor.type
      )
      assertEquals(AppInfo.GITHUB_URL, dialog.openSourceInfo.toolTipText)

      assertTrue(dialog.updateStatusProgress.isVisible)
      assertTrue(dialog.updateStatusProgress.isIndeterminate)
      assertEquals(
        true,
        dialog.updateStatusProgress.getClientProperty(
          FlatClientProperties.PROGRESS_BAR_LARGE_HEIGHT
        )
      )
      assertEquals(
        false,
        dialog.updateStatusProgress.getClientProperty(
          FlatClientProperties.PROGRESS_BAR_SQUARE
        )
      )

      assertFalse(dialog.updateBtn.isVisible)
      assertEquals(
        FlatClientProperties.BUTTON_TYPE_ROUND_RECT,
        dialog.updateBtn.getClientProperty(FlatClientProperties.BUTTON_TYPE)
      )
      assertEquals(
        "accent",
        dialog.updateBtn.getClientProperty(FlatClientProperties.STYLE_CLASS)
      )

      assertTrue(dialog.versionStatusBadge.isVisible)
      assertEquals(
        StatusBadge.Variant.NORMAL,
        dialog.versionStatusBadge.variant
      )
      assertEquals(
        I18n.get(I18n.ABOUT_CHECKING_UPDATES),
        dialog.versionStatusBadge.text
      )
    } finally {
      dialog.dispose()
    }
  }

  /**
   * Verifies UI transitions when [UpdateListener.onUpToDate] is invoked.
   */
  @Test
  fun testOnUpToDateTransition() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    var capturedListener: UpdateListener? = null
    val dialog = AboutDialog(null) { listener ->
      capturedListener = listener
      FakeUpdateManager(listener)
    }

    try {
      assertNotNull(capturedListener)
      capturedListener?.onUpToDate()

      assertFalse(dialog.updateStatusProgress.isVisible)
      assertFalse(dialog.updateBtn.isVisible)
      assertTrue(dialog.versionStatusBadge.isVisible)
      assertEquals(
        StatusBadge.Variant.NORMAL,
        dialog.versionStatusBadge.variant
      )
      assertEquals(
        I18n.get(I18n.ABOUT_UP_TO_DATE),
        dialog.versionStatusBadge.text
      )
    } finally {
      dialog.dispose()
    }
  }

  /**
   * Verifies UI transitions when [UpdateListener.onUpdateFound] is invoked.
   */
  @Test
  fun testOnUpdateFoundTransition() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    var capturedListener: UpdateListener? = null
    val dialog = AboutDialog(null) { listener ->
      capturedListener = listener
      FakeUpdateManager(listener)
    }

    try {
      assertNotNull(capturedListener)
      val release = createReleaseInfo("v2.5.0")
      capturedListener?.onUpdateFound(release)

      assertFalse(dialog.updateStatusProgress.isVisible)
      assertTrue(dialog.updateBtn.isVisible)
      val expectedBtnText = I18n.format(
        I18n.ABOUT_UPDATE_TO,
        release.versionName
      )
      assertEquals(expectedBtnText, dialog.updateBtn.text)
      assertTrue(dialog.versionStatusBadge.isVisible)
      assertEquals(
        StatusBadge.Variant.SURFACE,
        dialog.versionStatusBadge.variant
      )
      assertEquals(
        I18n.get(I18n.ABOUT_UPDATE_AVAILABLE),
        dialog.versionStatusBadge.text
      )
    } finally {
      dialog.dispose()
    }
  }

  /**
   * Verifies UI transitions when update check fails via
   * [UpdateListener.onFailedToCheckForUpdate].
   */
  @Test
  fun testOnFailedToCheckForUpdateTransition() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    var capturedListener: UpdateListener? = null
    val dialog = AboutDialog(null) { listener ->
      capturedListener = listener
      FakeUpdateManager(listener)
    }

    try {
      assertNotNull(capturedListener)
      val failure = RuntimeException("Connection timed out")
      capturedListener?.onFailedToCheckForUpdate(failure)

      assertFalse(dialog.updateStatusProgress.isVisible)
      assertFalse(dialog.updateBtn.isVisible)
      assertTrue(dialog.versionStatusBadge.isVisible)
      assertEquals(
        StatusBadge.Variant.NO_RESULTS,
        dialog.versionStatusBadge.variant
      )
      assertEquals(
        I18n.get(I18n.ABOUT_CHECK_UPDATE_FAILED),
        dialog.versionStatusBadge.text
      )
    } finally {
      dialog.dispose()
    }
  }

  /**
   * Verifies that [AboutDialog.onOk] disposes the dialog.
   */
  @Test
  fun testDialogDisposeOnOk() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val dialog = AboutDialog(null) { listener ->
      FakeUpdateManager(listener)
    }

    try {
      dialog.onOk()
      assertFalse(dialog.isDisplayable)
    } finally {
      dialog.dispose()
    }
  }

  /**
   * Verifies that [AboutDialog.onCancel] disposes the dialog.
   */
  @Test
  fun testDialogDisposeOnCancel() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val dialog = AboutDialog(null) { listener ->
      FakeUpdateManager(listener)
    }

    try {
      dialog.onCancel()
      assertFalse(dialog.isDisplayable)
    } finally {
      dialog.dispose()
    }
  }

  /**
   * Verifies card presentation and border styling on the info pane.
   */
  @Test
  fun testCardPresentationAndBorder() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val dialog = AboutDialog(null) { listener ->
      FakeUpdateManager(listener)
    }

    try {
      val border = dialog.infoPane.border
      assertTrue(border is FlatLineBorder)
      val flatBorder = border as FlatLineBorder
      assertEquals(UIScaleUtils.dip(8), flatBorder.arc)
      assertEquals(1f, flatBorder.lineThickness, 0.001f)
      assertNotNull(flatBorder.lineColor)
      assertNotNull(dialog.resolveCardBorderColor())
    } finally {
      dialog.dispose()
    }
  }

  /**
   * Verifies that the secondary constructor creates a valid dialog instance
   * using the default [UpdateManager] wiring.
   */
  @Test
  fun testDefaultConstructor() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val dialog = AboutDialog(null)
    try {
      assertNotNull(dialog)
      assertEquals(
        I18n.get(I18n.ABOUT_DIALOG_TITLE),
        dialog.title
      )
    } finally {
      dialog.dispose()
    }
  }
}
