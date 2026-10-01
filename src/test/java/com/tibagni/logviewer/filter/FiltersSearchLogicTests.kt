package com.tibagni.logviewer.filter

import com.tibagni.logviewer.log.LogLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Color

class FiltersSearchLogicTests {

  private val filter1 = Filter("RadioState", "setRadioState:.*", Color.RED, LogLevel.VERBOSE)
  private val filter2 = Filter("WifiConnect", "connectToNetwork", Color.BLUE, LogLevel.DEBUG)
  private val filter3 = Filter("AudioTrack", "startAudioStreaming", Color.GREEN, LogLevel.INFO)

  @Test
  fun testMatchesEmptyQuery() {
    assertTrue(FiltersSearchLogic.matches(filter1, ""))
    assertTrue(FiltersSearchLogic.matches(filter1, "   "))
  }

  @Test
  fun testMatchesByName() {
    assertTrue(FiltersSearchLogic.matches(filter1, "radio"))
    assertTrue(FiltersSearchLogic.matches(filter1, "STATE"))
    assertFalse(FiltersSearchLogic.matches(filter1, "bluetooth"))
  }

  @Test
  fun testMatchesByPattern() {
    assertTrue(FiltersSearchLogic.matches(filter1, "setRadioState"))
    assertTrue(FiltersSearchLogic.matches(filter2, "connectToNetwork"))
    assertTrue(FiltersSearchLogic.matches(filter3, "streaming"))
  }

  @Test
  fun testSearchMultipleGroups() {
    val openedFilters = mapOf(
      "Telephony.filters" to listOf(filter1),
      "Connectivity.filters" to listOf(filter2),
      "Multimedia.filters" to listOf(filter3)
    )

    val resultsAll = FiltersSearchLogic.search(openedFilters, "")
    assertEquals(3, resultsAll.size)

    val resultsRadio = FiltersSearchLogic.search(openedFilters, "radio")
    assertEquals(1, resultsRadio.size)
    assertEquals("Telephony.filters", resultsRadio[0].group)
    assertEquals("RadioState", resultsRadio[0].filter.name)

    val resultsStreaming = FiltersSearchLogic.search(openedFilters, "streaming")
    assertEquals(1, resultsStreaming.size)
    assertEquals("Multimedia.filters", resultsStreaming[0].group)
    assertEquals("AudioTrack", resultsStreaming[0].filter.name)

    val resultsNone = FiltersSearchLogic.search(openedFilters, "non_existent_token")
    assertTrue(resultsNone.isEmpty())
  }

  @Test
  fun testTruncateAndHighlightShortText() {
    val text = "ShortFilterName"
    val result = FiltersSearchLogic.truncateAndHighlight(text, "Filter", 30)
    assertTrue(result.contains("Filter"))
    assertFalse(result.endsWith("..."))
  }

  @Test
  fun testTruncateAndHighlightLongTextNoMatch() {
    val text = "This is a very very long filter name that exceeds the maximum length"
    val result = FiltersSearchLogic.truncateAndHighlight(text, "", 30)
    assertTrue(result.endsWith("..."))
    assertTrue(result.length <= 40) // truncated plus ellipsis
  }

  @Test
  fun testTruncateAndHighlightMatchNearBeginning() {
    val text = "ActivityTaskManager: START u0 {act=android.intent.action.MAIN}"
    val result = FiltersSearchLogic.truncateAndHighlight(text, "Activity", 30)
    assertTrue(result.contains("Activity"))
    assertTrue(result.endsWith("..."))
  }

  @Test
  fun testTruncateAndHighlightMatchDeepInText() {
    val text = "ActivityTaskManager: START u0 {act=android.intent.action.MAIN cmp=com.google.android.apps.nexuslauncher/.NexusLauncherActivity}"
    val result = FiltersSearchLogic.truncateAndHighlight(text, "NexusLauncher", 50)
    assertTrue(result.contains("NexusLauncher"))
    assertTrue(result.startsWith("..."))
  }
}
