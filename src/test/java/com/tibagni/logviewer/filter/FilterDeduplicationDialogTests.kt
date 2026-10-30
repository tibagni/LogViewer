package com.tibagni.logviewer.filter

import com.formdev.flatlaf.ui.FlatLineBorder
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.StatusBadge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.awt.Color
import java.awt.GraphicsEnvironment

class FilterDeduplicationDialogTests {

  @Test
  fun testCalculateRemovalsSingleGroup() {
    val f1 = Filter("f1", "WifiManager", Color.RED, LogLevel.DEBUG)
    val f2 = Filter("f2", "WifiManager", Color.BLUE, LogLevel.DEBUG)
    val cluster = DuplicateCluster(
      f1,
      listOf(FilterMatch("Group1", f1, 0), FilterMatch("Group1", f2, 1))
    )

    val removals = FilterDeduplicationLogic.calculateRemovals(
      listOf(cluster),
      mapOf(0 to Pair(true, "Group1"))
    )

    assertEquals(1, removals.size)
    assertEquals(listOf(f2), removals["Group1"])
  }

  @Test
  fun testCalculateRemovalsCrossGroup() {
    val f1 = Filter("f1", "AudioFlinger", Color.RED, LogLevel.DEBUG)
    val f2 = Filter("f2", "AudioFlinger", Color.BLUE, LogLevel.DEBUG)
    val cluster = DuplicateCluster(
      f1,
      listOf(FilterMatch("GroupA", f1, 0), FilterMatch("GroupB", f2, 0))
    )

    // Case 1: Keep in GroupA -> GroupB filter removed
    val removalsKeepA = FilterDeduplicationLogic.calculateRemovals(
      listOf(cluster),
      mapOf(0 to Pair(true, "GroupA"))
    )
    assertEquals(1, removalsKeepA.size)
    assertEquals(listOf(f2), removalsKeepA["GroupB"])
    assertNull(removalsKeepA["GroupA"])

    // Case 2: Keep in GroupB -> GroupA filter removed
    val removalsKeepB = FilterDeduplicationLogic.calculateRemovals(
      listOf(cluster),
      mapOf(0 to Pair(true, "GroupB"))
    )
    assertEquals(1, removalsKeepB.size)
    assertEquals(listOf(f1), removalsKeepB["GroupA"])
    assertNull(removalsKeepB["GroupB"])
  }

  @Test
  fun testCalculateRemovalsDisabledCluster() {
    val f1 = Filter("f1", "AudioFlinger", Color.RED, LogLevel.DEBUG)
    val f2 = Filter("f2", "AudioFlinger", Color.BLUE, LogLevel.DEBUG)
    val cluster = DuplicateCluster(
      f1,
      listOf(FilterMatch("GroupA", f1, 0), FilterMatch("GroupB", f2, 0))
    )

    val removals = FilterDeduplicationLogic.calculateRemovals(
      listOf(cluster),
      mapOf(0 to Pair(false, "GroupA"))
    )

    assertTrue(removals.isEmpty())
  }

  @Test
  fun testCalculateRemovalsCrossGroupToggleDisabled() {
    val f1 = Filter("f1", "AudioFlinger", Color.RED, LogLevel.DEBUG)
    val f2 = Filter("f2", "AudioFlinger", Color.BLUE, LogLevel.DEBUG)
    val cluster = DuplicateCluster(
      f1,
      listOf(FilterMatch("GroupA", f1, 0), FilterMatch("GroupB", f2, 0))
    )

    // When includeCrossFileDuplicates is false, cross-group clusters
    // are excluded from removals
    val removals = FilterDeduplicationLogic.calculateRemovals(
      listOf(cluster),
      mapOf(0 to Pair(true, "GroupA")),
      includeCrossFileDuplicates = false
    )

    assertTrue(removals.isEmpty())
  }

  @Test
  fun testDialogHierarchyAndCardBorders() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val f1 = Filter("f1", "WifiManager", Color.RED, LogLevel.DEBUG)
    val f2 = Filter("f2", "WifiManager", Color.BLUE, LogLevel.DEBUG)
    val cluster = DuplicateCluster(
      f1,
      listOf(FilterMatch("Group1", f1, 0), FilterMatch("Group1", f2, 1))
    )

    val dialog = FilterDeduplicationDialog(null, listOf(cluster))
    try {
      assertNotNull(dialog.mainContentPane)
      assertEquals(1, dialog.clusterBoxes.size)
      val card = dialog.clusterBoxes[0]
      assertNotNull(card)
      assertTrue(card!!.border is FlatLineBorder)
      val border = card.border as FlatLineBorder
      assertEquals(UIScaleUtils.dip(8), border.arc)
      assertNotNull(border.lineColor)
      assertEquals(1f, border.lineThickness, 0.001f)
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testClusterBadges() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val f1 = Filter("f1", "AudioFlinger", Color.RED, LogLevel.DEBUG)
    val f2 = Filter("f2", "AudioFlinger", Color.BLUE, LogLevel.DEBUG)
    val crossCluster = DuplicateCluster(
      f1,
      listOf(FilterMatch("GroupA", f1, 0), FilterMatch("GroupB", f2, 0))
    )

    val f3 = Filter("f3", "WifiManager", Color.GREEN, LogLevel.INFO)
    val f4 = Filter("f4", "WifiManager", Color.YELLOW, LogLevel.INFO)
    val sameCluster = DuplicateCluster(
      f3,
      listOf(FilterMatch("GroupC", f3, 0), FilterMatch("GroupC", f4, 1))
    )

    val dialog = FilterDeduplicationDialog(
      null,
      listOf(crossCluster, sameCluster)
    )
    try {
      assertEquals(2, dialog.clusterBadges.size)

      val crossBadge = dialog.clusterBadges[0]
      assertNotNull(crossBadge)
      assertEquals(
        StatusBadge.Variant.SURFACE,
        crossBadge!!.variant
      )
      assertEquals(
        I18n.get(I18n.FILTER_DEDUP_BADGE_CROSS_GROUP),
        crossBadge.text
      )

      val sameBadge = dialog.clusterBadges[1]
      assertNotNull(sameBadge)
      assertEquals(StatusBadge.Variant.NORMAL, sameBadge!!.variant)
      assertEquals(
        I18n.get(I18n.FILTER_DEDUP_BADGE_SAME_GROUP),
        sameBadge.text
      )
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testMergeCrossFileToggleUpdatesVisibility() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val f1 = Filter("f1", "AudioFlinger", Color.RED, LogLevel.DEBUG)
    val f2 = Filter("f2", "AudioFlinger", Color.BLUE, LogLevel.DEBUG)
    val crossCluster = DuplicateCluster(
      f1,
      listOf(FilterMatch("GroupA", f1, 0), FilterMatch("GroupB", f2, 0))
    )

    val f3 = Filter("f3", "WifiManager", Color.GREEN, LogLevel.INFO)
    val f4 = Filter("f4", "WifiManager", Color.YELLOW, LogLevel.INFO)
    val sameCluster = DuplicateCluster(
      f3,
      listOf(FilterMatch("GroupC", f3, 0), FilterMatch("GroupC", f4, 1))
    )

    val dialog = FilterDeduplicationDialog(
      null,
      listOf(crossCluster, sameCluster)
    )
    try {
      assertTrue(dialog.mergeCrossFileCb.isSelected)
      assertTrue(dialog.clusterBoxes[0]!!.isVisible)
      assertTrue(dialog.clusterSelections[0]!!.first.isSelected)
      assertTrue(dialog.clusterBoxes[1]!!.isVisible)
      assertTrue(dialog.clusterSelections[1]!!.first.isSelected)

      // Toggle off cross-file duplicates
      dialog.mergeCrossFileCb.doClick()
      assertFalse(dialog.mergeCrossFileCb.isSelected)
      assertFalse(dialog.clusterBoxes[0]!!.isVisible)
      assertFalse(dialog.clusterSelections[0]!!.first.isSelected)
      // Same-group cluster should remain visible and selected
      assertTrue(dialog.clusterBoxes[1]!!.isVisible)
      assertTrue(dialog.clusterSelections[1]!!.first.isSelected)

      // Toggle back on
      dialog.mergeCrossFileCb.doClick()
      assertTrue(dialog.mergeCrossFileCb.isSelected)
      assertTrue(dialog.clusterBoxes[0]!!.isVisible)
      assertTrue(dialog.clusterSelections[0]!!.first.isSelected)
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testOkAndCancelState() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val dialog = FilterDeduplicationDialog(null, emptyList())
    try {
      assertFalse(dialog.isApplied)
      dialog.onOk()
      assertTrue(dialog.isApplied)
      dialog.onCancel()
      assertFalse(dialog.isApplied)
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testColorSwatchDotPainting() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val dot = FilterDeduplicationDialog.ColorSwatchDot(Color.RED)
    assertNotNull(dot)
    val img = java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB)
    val g = img.createGraphics()
    try {
      dot.size = java.awt.Dimension(16, 16)
      dot.paint(g)
      val hasPixels = (0 until img.width).any { x ->
        (0 until img.height).any { y ->
          (img.getRGB(x, y) ushr 24 and 0xFF) > 0
        }
      }
      assertTrue("ColorSwatchDot should render non-empty pixels", hasPixels)
    } finally {
      g.dispose()
    }
  }

  @Test
  fun testCrossGroupResolutionChipsDynamicallyUpdate() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val f1 = Filter("f1", "AudioFlinger", Color.RED, LogLevel.DEBUG)
    val f2 = Filter("f2", "AudioFlinger", Color.BLUE, LogLevel.DEBUG)
    val crossCluster = DuplicateCluster(
      f1,
      listOf(FilterMatch("GroupA", f1, 0), FilterMatch("GroupB", f2, 0))
    )

    val dialog = FilterDeduplicationDialog(null, listOf(crossCluster))
    try {
      val (enableCb, groupCombo) = dialog.clusterSelections[0]!!
      assertNotNull(groupCombo)

      val badges = dialog.resolutionBadges[0]
      assertNotNull(badges)
      assertEquals(2, badges!!.size)

      // Initial selection is GroupA: GroupA keeps, GroupB removes
      assertEquals("GroupA", groupCombo!!.selectedItem)
      assertEquals(I18n.get(I18n.FILTER_DEDUP_ACTION_KEEP), badges[0].text)
      assertEquals(StatusBadge.Variant.NORMAL, badges[0].variant)
      assertEquals(I18n.get(I18n.COMMON_REMOVE), badges[1].text)
      assertEquals(StatusBadge.Variant.NO_RESULTS, badges[1].variant)

      // Switch selection to GroupB: GroupA removes, GroupB keeps
      groupCombo.selectedItem = "GroupB"
      val removals = dialog.removalsByGroup
      assertEquals(1, removals.size)
      assertEquals(listOf(f1), removals["GroupA"])
      assertNull(removals["GroupB"])

      assertEquals(I18n.get(I18n.COMMON_REMOVE), badges[0].text)
      assertEquals(StatusBadge.Variant.NO_RESULTS, badges[0].variant)
      assertEquals(I18n.get(I18n.FILTER_DEDUP_ACTION_KEEP), badges[1].text)
      assertEquals(StatusBadge.Variant.NORMAL, badges[1].variant)

      // Disable cluster: both badges show '-' with SURFACE variant
      enableCb.doClick()
      assertFalse(enableCb.isSelected)
      assertEquals("-", badges[0].text)
      assertEquals(StatusBadge.Variant.SURFACE, badges[0].variant)
      assertEquals("-", badges[1].text)
      assertEquals(StatusBadge.Variant.SURFACE, badges[1].variant)

      // Re-enable cluster: resolution chips are restored
      enableCb.doClick()
      assertTrue(enableCb.isSelected)
      assertEquals(I18n.get(I18n.COMMON_REMOVE), badges[0].text)
      assertEquals(StatusBadge.Variant.NO_RESULTS, badges[0].variant)
      assertEquals(I18n.get(I18n.FILTER_DEDUP_ACTION_KEEP), badges[1].text)
      assertEquals(StatusBadge.Variant.NORMAL, badges[1].variant)
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testCrossGroupWithIntraGroupDuplicatesResolutionChips() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val f1 = Filter("f1", "AudioFlinger", Color.RED, LogLevel.DEBUG)
    val f2 = Filter("f2", "AudioFlinger", Color.BLUE, LogLevel.DEBUG)
    val f3 = Filter("f3", "AudioFlinger", Color.GREEN, LogLevel.DEBUG)
    val cluster = DuplicateCluster(
      f1,
      listOf(
        FilterMatch("GroupA", f1, 0),
        FilterMatch("GroupA", f2, 1),
        FilterMatch("GroupB", f3, 0)
      )
    )

    val dialog = FilterDeduplicationDialog(null, listOf(cluster))
    try {
      val (_, groupCombo) = dialog.clusterSelections[0]!!
      val badges = dialog.resolutionBadges[0]!!
      assertEquals(3, badges.size)

      // GroupA is retaining: first match in GroupA is Keep, second is Remove, GroupB is Remove
      assertEquals(I18n.get(I18n.FILTER_DEDUP_ACTION_KEEP), badges[0].text)
      assertEquals(StatusBadge.Variant.NORMAL, badges[0].variant)
      assertEquals(I18n.get(I18n.COMMON_REMOVE), badges[1].text)
      assertEquals(StatusBadge.Variant.NO_RESULTS, badges[1].variant)
      assertEquals(I18n.get(I18n.COMMON_REMOVE), badges[2].text)
      assertEquals(StatusBadge.Variant.NO_RESULTS, badges[2].variant)

      // Switch retaining group to GroupB: both GroupA matches become Remove, GroupB is Keep
      groupCombo!!.selectedItem = "GroupB"
      assertEquals(I18n.get(I18n.COMMON_REMOVE), badges[0].text)
      assertEquals(StatusBadge.Variant.NO_RESULTS, badges[0].variant)
      assertEquals(I18n.get(I18n.COMMON_REMOVE), badges[1].text)
      assertEquals(StatusBadge.Variant.NO_RESULTS, badges[1].variant)
      assertEquals(I18n.get(I18n.FILTER_DEDUP_ACTION_KEEP), badges[2].text)
      assertEquals(StatusBadge.Variant.NORMAL, badges[2].variant)
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testSameGroupResolutionChipsAndDisableToggle() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val f1 = Filter("f1", "CameraService", Color.RED, LogLevel.DEBUG)
    val f2 = Filter("f2", "CameraService", Color.BLUE, LogLevel.DEBUG)
    val sameGroupCluster = DuplicateCluster(
      f1,
      listOf(FilterMatch("Group1", f1, 0), FilterMatch("Group1", f2, 1))
    )

    val dialog = FilterDeduplicationDialog(null, listOf(sameGroupCluster))
    try {
      val (enableCb, _) = dialog.clusterSelections[0]!!
      val badges = dialog.resolutionBadges[0]!!
      assertEquals(2, badges.size)

      // First match is Keep, second is Remove
      assertEquals(I18n.get(I18n.FILTER_DEDUP_ACTION_KEEP), badges[0].text)
      assertEquals(StatusBadge.Variant.NORMAL, badges[0].variant)
      assertEquals(I18n.get(I18n.COMMON_REMOVE), badges[1].text)
      assertEquals(StatusBadge.Variant.NO_RESULTS, badges[1].variant)

      // Disable cluster: both badges show '-' with SURFACE variant
      enableCb.doClick()
      assertEquals("-", badges[0].text)
      assertEquals(StatusBadge.Variant.SURFACE, badges[0].variant)
      assertEquals("-", badges[1].text)
      assertEquals(StatusBadge.Variant.SURFACE, badges[1].variant)

      // Re-enable
      enableCb.doClick()
      assertEquals(I18n.get(I18n.FILTER_DEDUP_ACTION_KEEP), badges[0].text)
      assertEquals(StatusBadge.Variant.NORMAL, badges[0].variant)
      assertEquals(I18n.get(I18n.COMMON_REMOVE), badges[1].text)
      assertEquals(StatusBadge.Variant.NO_RESULTS, badges[1].variant)
    } finally {
      dialog.dispose()
    }
  }

  /**
   * Tests modular sub-panel builders extracted from monolithic buildUi, verifying
   * header, footer, cluster card, and resolution panel generation.
   */
  @Test
  fun testBuildSubPanelsModularStructure() {
    if (GraphicsEnvironment.isHeadless()) return

    val f1 = Filter("f1", "AudioFlinger", Color.RED, LogLevel.DEBUG)
    val f2 = Filter("f2", "AudioFlinger", Color.BLUE, LogLevel.DEBUG)
    val cluster = DuplicateCluster(
      f1,
      listOf(FilterMatch("GroupA", f1, 0), FilterMatch("GroupB", f2, 0))
    )

    val dialog = FilterDeduplicationDialog(null, listOf(cluster))
    try {
      val header = dialog.buildHeaderPanel()
      assertNotNull(header)

      val footer = dialog.buildFooterActionsPanel()
      assertNotNull(footer)

      val card = dialog.buildClusterCard(0, cluster)
      assertNotNull(card)

      val resolution = dialog.buildResolutionPanel(0, cluster)
      assertNotNull(resolution)

      val sameGroupCluster = DuplicateCluster(
        f1,
        listOf(FilterMatch("GroupA", f1, 0), FilterMatch("GroupA", f2, 1))
      )
      val sameGroupResolution = dialog.buildResolutionPanel(0, sameGroupCluster)
      assertNotNull(sameGroupResolution)
    } finally {
      dialog.dispose()
    }
  }
}
