package com.tibagni.logviewer.filter

import org.junit.Assert.*
import org.junit.Test

class FiltersSearchPanelTests {

    @Test
    fun testUpdateMatchesCount() {
        var queryReceived = ""
        var closeCalled = false
        val panel = FiltersSearchPanel(
            onSearchQueryChanged = { queryReceived = it },
            onCloseRequested = { closeCalled = true }
        )

        panel.updateMatchesCount(2, 5, "test")
        assertTrue(panel.matchesLabel.isVisible)
        assertEquals("2 of 5 filter(s) match", panel.matchesLabel.text)

        panel.updateMatchesCount(0, 5, "")
        assertFalse(panel.matchesLabel.isVisible)

        panel.closeSearch()
        assertTrue(closeCalled)
        assertEquals("", panel.currentQuery)
    }
}
