package com.tibagni.logviewer.updates

import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.ui.FlatLineBorder
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.StatusBadge
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.awt.GraphicsEnvironment
import javax.swing.JPanel
import javax.swing.JScrollPane

/**
 * Unit tests verifying [UpdateAvailableDialog] presentation, styling,
 * URL launcher delegation, and card border metrics.
 */
class UpdateAvailableDialogTests {

  companion object {
    private const val TEST_JSON = """{
      "tag_name": "3.0.0",
      "html_url": "https://github.com/tibagni/LogViewer/releases/tag/3.0.0",
      "body": "Bug fixes and Material 3 improvements"
    }"""
  }

  private fun createSampleReleaseInfo(): ReleaseInfo {
    return ReleaseInfo(JSONObject(TEST_JSON))
  }

  @Test
  fun testInitialState() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val release = createSampleReleaseInfo()
    val dialog = UpdateAvailableDialog(release)
    try {
      assertEquals(
        I18n.get(I18n.UPDATES_TITLE),
        dialog.title
      )
      assertEquals("3.0.0", dialog.versionBadge.text)
      assertEquals(StatusBadge.Variant.SURFACE, dialog.versionBadge.variant)
      assertTrue(dialog.versionBadge.isVisible)
      assertEquals(
        "Bug fixes and Material 3 improvements",
        dialog.releaseInfo.text
      )
      assertFalse(dialog.releaseInfo.isEditable)
      assertTrue(dialog.releaseInfo.lineWrap)
      assertTrue(dialog.releaseInfo.wrapStyleWord)

      val buttonOk = dialog.buttonsPane.buttonOk
      assertNotNull(buttonOk)
      assertEquals(
        I18n.get(I18n.UPDATES_BTN_DOWNLOAD),
        buttonOk?.text
      )
      assertEquals(
        FlatClientProperties.BUTTON_TYPE_ROUND_RECT,
        buttonOk?.getClientProperty(FlatClientProperties.BUTTON_TYPE)
      )
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testOnOkInvokesCustomUrlLauncher() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val release = createSampleReleaseInfo()
    var launchedUrl: String? = null
    val dialog = UpdateAvailableDialog(release) { url ->
      launchedUrl = url
    }

    try {
      dialog.onOk()
      assertEquals(release.releaseUrl, launchedUrl)
      assertFalse(dialog.isDisplayable)
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testOnCancelDisposesDialogWithoutInvokingLauncher() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val release = createSampleReleaseInfo()
    var launchedUrl: String? = null
    val dialog = UpdateAvailableDialog(release) { url ->
      launchedUrl = url
    }

    try {
      dialog.onCancel()
      assertEquals(null, launchedUrl)
      assertFalse(dialog.isDisplayable)
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testCardPresentationAndBorderMetrics() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val release = createSampleReleaseInfo()
    val dialog = UpdateAvailableDialog(release)
    try {
      val cardBorder = dialog.createCardBorder()
      assertTrue(cardBorder is FlatLineBorder)
      val flatLineBorder = cardBorder as FlatLineBorder
      assertEquals(UIScaleUtils.dip(8), flatLineBorder.arc)
      assertEquals(1f, flatLineBorder.lineThickness, 0.001f)
      assertNotNull(flatLineBorder.lineColor)

      assertNotNull(dialog.releaseInfoPane)
      val card = findCardPanel(dialog.releaseInfoPane)
      assertNotNull(card)
      assertTrue(card?.border is FlatLineBorder)

      val scrollPane = findScrollPane(card!!)
      assertNotNull(scrollPane)
      assertTrue(scrollPane?.border is FlatLineBorder)
      val scrollBorder = scrollPane?.border as FlatLineBorder
      assertEquals(UIScaleUtils.dip(4), scrollBorder.arc)
    } finally {
      dialog.dispose()
    }
  }

  private fun findCardPanel(parent: JPanel): JPanel? {
    for (i in 0 until parent.componentCount) {
      val comp = parent.getComponent(i)
      if (comp is JPanel && comp.border is FlatLineBorder) {
        return comp
      }
    }
    return null
  }

  private fun findScrollPane(parent: JPanel): JScrollPane? {
    for (i in 0 until parent.componentCount) {
      val comp = parent.getComponent(i)
      if (comp is JScrollPane) {
        return comp
      }
    }
    return null
  }
}
