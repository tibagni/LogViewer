package com.tibagni.logviewer.filter

import com.tibagni.logviewer.log.LogLevel
import org.junit.Assert.*
import org.junit.Test
import java.awt.Color

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

        // When includeCrossFileDuplicates is false, cross-group clusters are excluded from removals
        val removals = FilterDeduplicationLogic.calculateRemovals(
            listOf(cluster),
            mapOf(0 to Pair(true, "GroupA")),
            includeCrossFileDuplicates = false
        )

        assertTrue(removals.isEmpty())
    }
}
