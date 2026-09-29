package com.tibagni.logviewer.filter

import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.log.LogLevel
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.awt.Color

class EditFilterDialogTests {
    private val filtersRepo = ServiceLocator.filtersRepository

    @Before
    fun setUp() {
        filtersRepo.closeAllFilters()
    }

    @After
    fun tearDown() {
        filtersRepo.closeAllFilters()
    }

    @Test
    fun testDetectsExactDuplicate() {
        val existing = Filter("f1", "ActivityManager", Color.RED, LogLevel.DEBUG)
        filtersRepo.addFilters("GroupA", listOf(existing))

        val match = FilterSimilarityUtils.findFirstDuplicateOrSimilar(
            "ActivityManager",
            false,
            null,
            filtersRepo.currentlyOpenedFilters
        )

        assertNotNull(match)
        assertEquals("GroupA", match?.group)
        assertEquals(existing, match?.filter)
    }

    @Test
    fun testDetectsSimilarFilter() {
        val existing = Filter("f1", "ActivityManager", Color.RED, LogLevel.DEBUG)
        filtersRepo.addFilters("GroupA", listOf(existing))

        // 14/15 = 93.3% similar
        val match = FilterSimilarityUtils.findFirstDuplicateOrSimilar(
            "ActivityManageX",
            false,
            null,
            filtersRepo.currentlyOpenedFilters
        )

        assertNotNull(match)
        assertEquals("GroupA", match?.group)
        assertEquals(existing, match?.filter)
    }

    @Test
    fun testIgnoresDifferentFilter() {
        val existing = Filter("f1", "ActivityManager", Color.RED, LogLevel.DEBUG)
        filtersRepo.addFilters("GroupA", listOf(existing))

        val match = FilterSimilarityUtils.findFirstDuplicateOrSimilar(
            "PackageManager",
            false,
            null,
            filtersRepo.currentlyOpenedFilters
        )

        assertNull(match)
    }

    @Test
    fun testIgnoresSelfWhenEditing() {
        val existing = Filter("f1", "ActivityManager", Color.RED, LogLevel.DEBUG)
        filtersRepo.addFilters("GroupA", listOf(existing))

        // Editing existing filter
        val match = FilterSimilarityUtils.findFirstDuplicateOrSimilar(
            "ActivityManager",
            false,
            existing,
            filtersRepo.currentlyOpenedFilters
        )

        assertNull(match)
    }
}
