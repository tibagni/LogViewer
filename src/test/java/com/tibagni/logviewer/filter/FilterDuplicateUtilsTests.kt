package com.tibagni.logviewer.filter

import com.tibagni.logviewer.log.LogLevel
import org.junit.Assert.*
import org.junit.Test
import java.awt.Color

class FilterDuplicateUtilsTests {
    private val utils: FilterDuplicateUtils = FilterDuplicateUtilsImpl()

    @Test
    fun testIsDuplicateExact() {
        val f1 = Filter("f1", "ActivityManager", Color.RED, LogLevel.DEBUG)
        val f2 = Filter("f2", "ActivityManager", Color.BLUE, LogLevel.DEBUG)
        val f3 = Filter("f3", "ActivityManageX", Color.GREEN, LogLevel.DEBUG)
        val f4 = Filter("f4", "BatteryService", Color.YELLOW, LogLevel.DEBUG)

        assertTrue(utils.isDuplicate(f1, f2))
        assertFalse(utils.isDuplicate(f1, f3))
        assertFalse(utils.isDuplicate(f1, f4))
    }

    @Test
    fun testIsDuplicateCaseInsensitive() {
        val f1 = Filter("f1", "ActivityManager", Color.RED, LogLevel.DEBUG, false)
        val f2 = Filter("f2", "activitymanager", Color.BLUE, LogLevel.DEBUG, false)
        val f3 = Filter("f3", "activitymanager", Color.BLUE, LogLevel.DEBUG, true)

        // Both case-insensitive -> duplicate
        assertTrue(utils.isDuplicate(f1, f2))
        // One case-sensitive, one case-insensitive -> not duplicate
        assertFalse(utils.isDuplicate(f2, f3))
    }

    @Test
    fun testFindDuplicateClustersSingleGroup() {
        val f1 = Filter("f1", "WifiStateMachine", Color.RED, LogLevel.DEBUG)
        val f2 = Filter("f2", "WifiStateMachine", Color.BLUE, LogLevel.DEBUG)
        val f3 = Filter("f3", "BluetoothManager", Color.GREEN, LogLevel.DEBUG)

        val groups = mapOf("Group1" to listOf(f1, f2, f3))
        val clusters = utils.findDuplicateClusters(groups)

        assertEquals(1, clusters.size)
        val cluster = clusters[0]
        assertEquals(2, cluster.matches.size)
        assertFalse(cluster.isCrossGroup)
        assertEquals(listOf("Group1"), cluster.affectedGroups)
    }

    @Test
    fun testFindDuplicateClustersCrossGroup() {
        val f1 = Filter("f1", "AudioFlinger", Color.RED, LogLevel.DEBUG)
        val f2 = Filter("f2", "AudioFlinger", Color.BLUE, LogLevel.DEBUG)
        val f3 = Filter("f3", "CameraService", Color.GREEN, LogLevel.DEBUG)

        val groups = mapOf(
            "FileA.flt" to listOf(f1, f3),
            "FileB.flt" to listOf(f2)
        )
        val clusters = utils.findDuplicateClusters(groups)

        assertEquals(1, clusters.size)
        val cluster = clusters[0]
        assertEquals(2, cluster.matches.size)
        assertTrue(cluster.isCrossGroup)
        assertEquals(setOf("FileA.flt", "FileB.flt"), cluster.affectedGroups.toSet())
    }

    @Test
    fun testNoClustersWhenNoDuplicates() {
        val f1 = Filter("f1", "AudioFlinger", Color.RED, LogLevel.DEBUG)
        val f2 = Filter("f2", "CameraService", Color.BLUE, LogLevel.DEBUG)

        val groups = mapOf(
            "FileA.flt" to listOf(f1),
            "FileB.flt" to listOf(f2)
        )
        val clusters = utils.findDuplicateClusters(groups)

        assertTrue(clusters.isEmpty())
    }
}
