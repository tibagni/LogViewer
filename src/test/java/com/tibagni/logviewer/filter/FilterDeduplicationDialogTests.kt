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
}
