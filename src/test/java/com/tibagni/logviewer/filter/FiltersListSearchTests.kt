package com.tibagni.logviewer.filter

import com.tibagni.logviewer.log.LogLevel
import org.junit.Assert.*
import org.junit.Test
import java.awt.Color

class FiltersListSearchTests {

    @Test
    fun testMatchesByPattern() {
        val f = Filter("Rule1", "WifiStateMachine", Color.RED, LogLevel.DEBUG)
        assertTrue(FiltersSearchLogic.matches(f, "wifi"))
        assertTrue(FiltersSearchLogic.matches(f, "STATEMACHINE"))
        assertFalse(FiltersSearchLogic.matches(f, "bluetooth"))
    }

    @Test
    fun testMatchesByName() {
        val f = Filter("MySpecialFilter", "12345", Color.RED, LogLevel.DEBUG)
        assertTrue(FiltersSearchLogic.matches(f, "special"))
        assertTrue(FiltersSearchLogic.matches(f, "MYSPECIAL"))
        assertTrue(FiltersSearchLogic.matches(f, "12345"))
        assertFalse(FiltersSearchLogic.matches(f, "other"))
    }

    @Test
    fun testEmptyQueryMatchesAll() {
        val f = Filter("Rule", "Pattern", Color.RED, LogLevel.DEBUG)
        assertTrue(FiltersSearchLogic.matches(f, ""))
    }

    @Test
    fun testFilterGroup() {
        val f1 = Filter("WifiRule", "WifiStateMachine", Color.RED, LogLevel.DEBUG)
        val f2 = Filter("AudioRule", "AudioFlinger", Color.BLUE, LogLevel.DEBUG)
        val f3 = Filter("BluetoothRule", "bt_stack", Color.GREEN, LogLevel.DEBUG)
        val group = listOf(f1, f2, f3)

        val matchesWifi = FiltersSearchLogic.filterGroup(group, "wifi")
        assertEquals(listOf(f1), matchesWifi)

        val matchesRule = FiltersSearchLogic.filterGroup(group, "rule")
        assertEquals(3, matchesRule.size)

        val matchesNone = FiltersSearchLogic.filterGroup(group, "xyz_not_found")
        assertTrue(matchesNone.isEmpty())

        val matchesEmpty = FiltersSearchLogic.filterGroup(group, "")
        assertEquals(3, matchesEmpty.size)
    }

    @Test
    fun testFilterAllGroupsAndCount() {
        val f1 = Filter("WifiRule", "WifiStateMachine", Color.RED, LogLevel.DEBUG)
        val f2 = Filter("AudioRule", "AudioFlinger", Color.BLUE, LogLevel.DEBUG)
        val f3 = Filter("BluetoothRule", "bt_stack", Color.GREEN, LogLevel.DEBUG)

        val groups = mapOf(
            "Network" to listOf(f1, f3),
            "Media" to listOf(f2)
        )

        val filtered = FiltersSearchLogic.filterAllGroups(groups, "rule")
        assertEquals(2, filtered["Network"]?.size)
        assertEquals(1, filtered["Media"]?.size)
        assertEquals(3, FiltersSearchLogic.countTotalMatches(filtered))

        val filteredWifi = FiltersSearchLogic.filterAllGroups(groups, "wifi")
        assertEquals(1, filteredWifi["Network"]?.size)
        assertEquals(0, filteredWifi["Media"]?.size)
        assertEquals(1, FiltersSearchLogic.countTotalMatches(filteredWifi))
    }
}
