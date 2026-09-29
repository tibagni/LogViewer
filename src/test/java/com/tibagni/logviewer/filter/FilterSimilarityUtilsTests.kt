package com.tibagni.logviewer.filter

import com.tibagni.logviewer.log.LogLevel
import org.junit.Assert.*
import org.junit.Test
import java.awt.Color

class FilterSimilarityUtilsTests {

    @Test
    fun testLevenshteinDistance() {
        assertEquals(0, FilterSimilarityUtils.levenshteinDistance("", ""))
        assertEquals(3, FilterSimilarityUtils.levenshteinDistance("abc", ""))
        assertEquals(3, FilterSimilarityUtils.levenshteinDistance("", "xyz"))
        assertEquals(0, FilterSimilarityUtils.levenshteinDistance("test", "test"))
        assertEquals(1, FilterSimilarityUtils.levenshteinDistance("test", "tests"))
        assertEquals(1, FilterSimilarityUtils.levenshteinDistance("test", "best"))
        assertEquals(3, FilterSimilarityUtils.levenshteinDistance("kitten", "sitting"))
    }

    @Test
    fun testCalculateSimilarity() {
        assertEquals(1.0, FilterSimilarityUtils.calculateSimilarity("ActivityManager", "ActivityManager"), 0.001)
        assertEquals(1.0, FilterSimilarityUtils.calculateSimilarity("  ActivityManager  ", "ActivityManager"), 0.001)
        
        // 1 difference in 10 characters = 9/10 = 0.90 (90%)
        assertEquals(0.90, FilterSimilarityUtils.calculateSimilarity("1234567890", "123456789X"), 0.001)
        
        // 1 difference in 5 characters = 4/5 = 0.80 (80%)
        assertEquals(0.80, FilterSimilarityUtils.calculateSimilarity("abcde", "abcdf"), 0.001)
        
        // 2 differences in 5 characters = 3/5 = 0.60 (60%)
        assertEquals(0.60, FilterSimilarityUtils.calculateSimilarity("abcde", "abczz"), 0.001)
    }

    @Test
    fun testIsDuplicateOrSimilar() {
        val f1 = Filter("f1", "ActivityManager", Color.RED, LogLevel.DEBUG)
        val f2 = Filter("f2", "ActivityManager", Color.BLUE, LogLevel.DEBUG)
        val f3 = Filter("f3", "ActivityManageX", Color.GREEN, LogLevel.DEBUG) // 14/15 = 93.3% similar
        val f4 = Filter("f4", "BatteryService", Color.YELLOW, LogLevel.DEBUG)

        assertTrue(FilterSimilarityUtils.isDuplicateOrSimilar(f1, f2))
        assertTrue(FilterSimilarityUtils.isDuplicateOrSimilar(f1, f3))
        assertFalse(FilterSimilarityUtils.isDuplicateOrSimilar(f1, f4))
    }

    @Test
    fun testIsPatternDuplicateOrSimilar() {
        assertTrue(FilterSimilarityUtils.isPatternDuplicateOrSimilar("ActivityManager", "ActivityManager"))
        assertTrue(FilterSimilarityUtils.isPatternDuplicateOrSimilar("ActivityManager", "ActivityManageX")) // 14/15 = 93.3% -> >= 90%
        assertFalse(FilterSimilarityUtils.isPatternDuplicateOrSimilar("12345", "1234X")) // 4/5 = 80% -> < 90%
        assertFalse(FilterSimilarityUtils.isPatternDuplicateOrSimilar("ActivityManager", "PackageManager")) // < 90%
    }

    @Test
    fun testFindDuplicateClustersSingleGroup() {
        val f1 = Filter("f1", "WifiStateMachine", Color.RED, LogLevel.DEBUG)
        val f2 = Filter("f2", "WifiStateMachine", Color.BLUE, LogLevel.DEBUG)
        val f3 = Filter("f3", "BluetoothManager", Color.GREEN, LogLevel.DEBUG)

        val groups = mapOf("Group1" to listOf(f1, f2, f3))
        val clusters = FilterSimilarityUtils.findDuplicateClusters(groups)

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
        val clusters = FilterSimilarityUtils.findDuplicateClusters(groups)

        assertEquals(1, clusters.size)
        val cluster = clusters[0]
        assertEquals(2, cluster.matches.size)
        assertTrue(cluster.isCrossGroup)
        assertEquals(setOf("FileA.flt", "FileB.flt"), cluster.affectedGroups.toSet())
    }
}
