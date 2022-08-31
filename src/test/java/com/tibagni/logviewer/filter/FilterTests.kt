package com.tibagni.logviewer.filter

import com.tibagni.logviewer.ProgressReporter
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.log.LogStream
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mock
import java.awt.Color

class FilterTests {
  @Test
  fun singleSimpleFilterTest() {
    val filter = Filter("name", "filterText", Color.WHITE, LogLevel.VERBOSE)
    val input = listOf(
      LogEntry("Log line 1", LogLevel.DEBUG, null),
      LogEntry("Log line 2", LogLevel.DEBUG, null),
      LogEntry("Log line containing filterText", LogLevel.DEBUG, null),
      LogEntry("Log line 3", LogLevel.DEBUG, null),
      LogEntry("Log line 4", LogLevel.DEBUG, null),
      LogEntry("Log line 5", LogLevel.DEBUG, null),
      LogEntry("Log line containing filterText", LogLevel.DEBUG, null),
      LogEntry("Log line containing filterText", LogLevel.DEBUG, null),
      LogEntry("Log line 6", LogLevel.DEBUG, null),
      LogEntry("Log line 7", LogLevel.DEBUG, null),
      LogEntry("Log line 8", LogLevel.DEBUG, null),
      LogEntry("Log line 9", LogLevel.DEBUG, null),
      LogEntry("Log line 10", LogLevel.DEBUG, null)
    )

    val filtered = Filters.applyMultipleFilters(input, arrayOf(filter), mock(ProgressReporter::class.java))

    assertEquals(3, filtered.size)
  }

  @Test
  fun testFilterCount() {
    val filter = Filter("name", "Log line", Color.WHITE, LogLevel.VERBOSE)
    val input = ArrayList<LogEntry>()
    for (i in 1..200) {
      input.add(LogEntry("Log line$i", LogLevel.INFO, null))
    }
    input.add(LogEntry("Last line", LogLevel.INFO, null))

    // Run this test multiple times to increase the chances we catch concurrency issues
    for (i in 0 .. 20) {
      val filtered = Filters.applyMultipleFilters(input, arrayOf(filter), mock(ProgressReporter::class.java))
      assertEquals(200, filtered.size)
      assertEquals(200, filter.temporaryInfo.totalLinesFound)
    }
  }

  @Test
  fun singleSimpleFilterWithVerbosityVerboseTest() {
    val filter = Filter("name", "Log line", Color.WHITE, LogLevel.VERBOSE)
    val input = listOf(
      LogEntry("Log line 1", LogLevel.INFO, null),
      LogEntry("Log line 2", LogLevel.DEBUG, null),
      LogEntry("Log line 3", LogLevel.WARNING, null),
      LogEntry("Log line 4", LogLevel.VERBOSE, null),
      LogEntry("Log line 5", LogLevel.ERROR, null),
      LogEntry("Log line 6", LogLevel.DEBUG, null)
    )
    val filtered = Filters.applyMultipleFilters(input, arrayOf(filter), mock(ProgressReporter::class.java))

    assertEquals(6, filtered.size)
  }

  @Test
  fun singleSimpleFilterWithVerbosityDebugTest() {
    val filter = Filter("name", "Log line", Color.WHITE, LogLevel.DEBUG)
    val input = listOf(
      LogEntry("Log line 1", LogLevel.INFO, null),
      LogEntry("Log line 2", LogLevel.DEBUG, null),
      LogEntry("Log line 3", LogLevel.WARNING, null),
      LogEntry("Log line 4", LogLevel.VERBOSE, null),
      LogEntry("Log line 5", LogLevel.ERROR, null),
      LogEntry("Log line 6", LogLevel.DEBUG, null)
    )
    val filtered = Filters.applyMultipleFilters(input, arrayOf(filter), mock(ProgressReporter::class.java))

    assertEquals(5, filtered.size)
  }

  @Test
  fun singleSimpleFilterWithVerbosityInfoTest() {
    val filter = Filter("name", "Log line", Color.WHITE, LogLevel.INFO)
    val input = listOf(
      LogEntry("Log line 1", LogLevel.INFO, null),
      LogEntry("Log line 2", LogLevel.DEBUG, null),
      LogEntry("Log line 3", LogLevel.WARNING, null),
      LogEntry("Log line 4", LogLevel.VERBOSE, null),
      LogEntry("Log line 5", LogLevel.ERROR, null),
      LogEntry("Log line 6", LogLevel.DEBUG, null)
    )
    val filtered = Filters.applyMultipleFilters(input, arrayOf(filter), mock(ProgressReporter::class.java))

    assertEquals(3, filtered.size)
  }

  @Test
  fun singleSimpleFilterWithVerbosityWarningTest() {
    val filter = Filter("name", "Log line", Color.WHITE, LogLevel.WARNING)
    val input = listOf(
      LogEntry("Log line 1", LogLevel.INFO, null),
      LogEntry("Log line 2", LogLevel.DEBUG, null),
      LogEntry("Log line 3", LogLevel.WARNING, null),
      LogEntry("Log line 4", LogLevel.VERBOSE, null),
      LogEntry("Log line 5", LogLevel.ERROR, null),
      LogEntry("Log line 6", LogLevel.DEBUG, null)
    )
    val filtered = Filters.applyMultipleFilters(input, arrayOf(filter), mock(ProgressReporter::class.java))

    assertEquals(2, filtered.size)
  }

  @Test
  fun singleSimpleFilterWithVerbosityErrorTest() {
    val filter = Filter("name", "Log line", Color.WHITE, LogLevel.ERROR)
    val input = listOf(
      LogEntry("Log line 1", LogLevel.INFO, null),
      LogEntry("Log line 2", LogLevel.DEBUG, null),
      LogEntry("Log line 3", LogLevel.WARNING, null),
      LogEntry("Log line 4", LogLevel.VERBOSE, null),
      LogEntry("Log line 5", LogLevel.ERROR, null),
      LogEntry("Log line 6", LogLevel.DEBUG, null)
    )
    val filtered = Filters.applyMultipleFilters(input, arrayOf(filter), mock(ProgressReporter::class.java))

    assertEquals(1, filtered.size)
  }

  @Test
  fun singleSimpleFilterTestCaseInsensitive() {
    val filter = Filter("name", "filterText", Color.WHITE, LogLevel.VERBOSE)
    val input = listOf(
      LogEntry("Log line 1", LogLevel.DEBUG, null),
      LogEntry("Log line 2", LogLevel.DEBUG, null),
      LogEntry("Log line containing filterText", LogLevel.DEBUG, null),
      LogEntry("Log line 3", LogLevel.DEBUG, null),
      LogEntry("Log line 4", LogLevel.DEBUG, null),
      LogEntry("Log line 5", LogLevel.DEBUG, null),
      LogEntry("Log line containing FILTERtext", LogLevel.DEBUG, null),
      LogEntry("Log line containing filterTEXT", LogLevel.DEBUG, null),
      LogEntry("Log line 6", LogLevel.DEBUG, null),
      LogEntry("Log line 7", LogLevel.DEBUG, null),
      LogEntry("Log line 8", LogLevel.DEBUG, null),
      LogEntry("Log line 9", LogLevel.DEBUG, null),
      LogEntry("Log line 10", LogLevel.DEBUG, null)
    )

    val filtered = Filters.applyMultipleFilters(input, arrayOf(filter), mock(ProgressReporter::class.java))

    assertEquals(3, filtered.size)
  }

  @Test
  fun singleSimpleFilterTestCaseSensitive() {
    val filter = Filter("name", "filterText", Color.WHITE, LogLevel.VERBOSE, true)
    val input = listOf(
      LogEntry("Log line 1", LogLevel.DEBUG, null),
      LogEntry("Log line 2", LogLevel.DEBUG, null),
      LogEntry("Log line containing filterText", LogLevel.DEBUG, null),
      LogEntry("Log line 3", LogLevel.DEBUG, null),
      LogEntry("Log line 4", LogLevel.DEBUG, null),
      LogEntry("Log line 5", LogLevel.DEBUG, null),
      LogEntry("Log line containing FILTERtext", LogLevel.DEBUG, null),
      LogEntry("Log line containing filterTEXT", LogLevel.DEBUG, null),
      LogEntry("Log line 6", LogLevel.DEBUG, null),
      LogEntry("Log line 7", LogLevel.DEBUG, null),
      LogEntry("Log line 8", LogLevel.DEBUG, null),
      LogEntry("Log line 9", LogLevel.DEBUG, null),
      LogEntry("Log line 10", LogLevel.DEBUG, null)
    )

    val filtered = Filters.applyMultipleFilters(input, arrayOf(filter), mock(ProgressReporter::class.java))

    assertEquals(1, filtered.size)
  }

  @Test
  fun singleRegexFilterTest() {
    val filter = Filter("name", "[\\w\\d]+@[\\w\\d]+\\.\\w+", Color.WHITE, LogLevel.VERBOSE)
    val input = listOf(
      LogEntry("Log line 1", LogLevel.DEBUG, null),
      LogEntry("Log line 2", LogLevel.DEBUG, null),
      LogEntry("Log line containing eMail@bla.com", LogLevel.DEBUG, null),
      LogEntry("Log line 3", LogLevel.DEBUG, null),
      LogEntry("Log line 4", LogLevel.DEBUG, null),
      LogEntry("Log line 5", LogLevel.DEBUG, null),
      LogEntry("email@email.com", LogLevel.DEBUG, null),
      LogEntry("Log otheremail@other.co Log", LogLevel.DEBUG, null),
      LogEntry("Log line 6", LogLevel.DEBUG, null),
      LogEntry("Log line 7", LogLevel.DEBUG, null),
      LogEntry("Log line 8", LogLevel.DEBUG, null),
      LogEntry("Log line 9", LogLevel.DEBUG, null),
      LogEntry("Log line 10", LogLevel.DEBUG, null)
    )

    val filtered = Filters.applyMultipleFilters(input, arrayOf(filter), mock(ProgressReporter::class.java))

    assertEquals(3, filtered.size)
  }

  @Test
  fun multipleFilterTest() {
    val filters = arrayOf(
      Filter("name", "[\\w\\d]+@[\\w\\d]+\\.\\w+", Color.WHITE, LogLevel.VERBOSE),
      Filter("name", "caseSensitiveText", Color.WHITE, LogLevel.VERBOSE, true),
      Filter("name", "CaSeInSeNsitiveTeXT", Color.WHITE, LogLevel.VERBOSE)
    )

    val input = listOf(
      LogEntry("Log line containing caseinsensitivetext", LogLevel.DEBUG, null),
      LogEntry("Log line containing caseInsensitiveText", LogLevel.DEBUG, null),
      LogEntry("Log line containing CASEINSENSITIVETEXT", LogLevel.DEBUG, null),
      LogEntry("Log line 2", LogLevel.DEBUG, null),
      LogEntry("Log line containing eMail@bla.com", LogLevel.DEBUG, null),
      LogEntry("Log line 3", LogLevel.DEBUG, null),
      LogEntry("Log line 4", LogLevel.DEBUG, null),
      LogEntry("Log line 5", LogLevel.DEBUG, null),
      LogEntry("email@email.com", LogLevel.DEBUG, null),
      LogEntry("Log otheremail@other.co Log", LogLevel.DEBUG, null),
      LogEntry("Log line 6", LogLevel.DEBUG, null),
      LogEntry("Log line 7", LogLevel.DEBUG, null),
      LogEntry("Log line containing caseSensitiveText", LogLevel.DEBUG, null),
      LogEntry("Log line containing casesensitivetext", LogLevel.DEBUG, null),
      LogEntry("Log line containing CASESENSITIVETEXT", LogLevel.DEBUG, null)
    )

    val filtered = Filters.applyMultipleFilters(input, filters, mock(ProgressReporter::class.java))

    assertEquals(7, filtered.size)
  }

  @Test
  fun testSerializeSimpleFilter() {
    val filter1 = Filter("Filter Name", "Filter Query", Color(0, 0, 0), LogLevel.VERBOSE)
    val filter2 = Filter("Filter Name", "F", Color(255, 255, 255), LogLevel.VERBOSE)
    val filter3 = Filter("filter", "filter", Color(0, 255, 0), LogLevel.VERBOSE, true)
    val filter4 = Filter("(){}filter", "filter", Color(255, 0, 0), LogLevel.VERBOSE)
    val filter5 = Filter("./\\()*&ˆˆ", "Filter Query", Color(0, 0, 255), LogLevel.VERBOSE)

    val serialized1 = filter1.serializeFilter()
    val serialized2 = filter2.serializeFilter()
    val serialized3 = filter3.serializeFilter()
    val serialized4 = filter4.serializeFilter()
    val serialized5 = filter5.serializeFilter()

    assertEquals("Filter Name,RmlsdGVyIFF1ZXJ5,2,0:0:0,VERBOSE", serialized1)
    assertEquals("Filter Name,Rg==,2,255:255:255,VERBOSE", serialized2)
    assertEquals("filter,ZmlsdGVy,0,0:255:0,VERBOSE", serialized3)
    assertEquals("(){}filter,ZmlsdGVy,2,255:0:0,VERBOSE", serialized4)
    assertEquals("./\\()*&ˆˆ,RmlsdGVyIFF1ZXJ5,2,0:0:255,VERBOSE", serialized5)
  }

  @Test
  fun testSerializeRegexFilter() {
    val filter1 = Filter(
      "Filter Name", "\\w+@\\w+\\.(net|com)(\\.br){0,1}",
      Color(0, 0, 0), LogLevel.VERBOSE
    )
    val filter2 = Filter(
      "Filter Name", "\\+\\d-\\(\\d{3}\\)-\\d{3}-\\d{4}",
      Color(255, 255, 255), LogLevel.VERBOSE, true
    )

    val serialized1 = filter1.serializeFilter()
    val serialized2 = filter2.serializeFilter()

    assertEquals("Filter Name,XHcrQFx3K1wuKG5ldHxjb20pKFwuYnIpezAsMX0=,2,0:0:0,VERBOSE", serialized1)
    assertEquals("Filter Name,XCtcZC1cKFxkezN9XCktXGR7M30tXGR7NH0=,0,255:255:255,VERBOSE", serialized2)
  }

  @Test
  fun testDeSerializeSimpleFilter() {
    val serialized1 = "Filter Name,RmlsdGVyIFF1ZXJ5,2,0:0:0,VERBOSE"
    val serialized2 = "Filter Name,Rg==,2,255:255:255,DEBUG"
    val serialized3 = "filter,ZmlsdGVy,0,0:255:0,INFO"
    val serialized4 = "(){}filter,ZmlsdGVy,2,255:0:0,WARNING"
    val serialized5 = "./\\()*&ˆˆ,RmlsdGVyIFF1ZXJ5,2,0:0:255,ERROR"

    val filter1 = Filter.createFromString(serialized1)
    val filter2 = Filter.createFromString(serialized2)
    val filter3 = Filter.createFromString(serialized3)
    val filter4 = Filter.createFromString(serialized4)
    val filter5 = Filter.createFromString(serialized5)

    assertEquals("Filter Name", filter1.name)
    assertEquals("Filter Query", filter1.patternString)
    assertFalse(filter1.isCaseSensitive)
    assertEquals(Color(0, 0, 0), filter1.color)
    assertEquals(LogLevel.VERBOSE, filter1.verbosity)

    assertEquals("Filter Name", filter2.name)
    assertEquals("F", filter2.patternString)
    assertFalse(filter2.isCaseSensitive)
    assertEquals(Color(255, 255, 255), filter2.color)
    assertEquals(LogLevel.DEBUG, filter2.verbosity)

    assertEquals("filter", filter3.name)
    assertEquals("filter", filter3.patternString)
    assertTrue(filter3.isCaseSensitive)
    assertEquals(Color(0, 255, 0), filter3.color)
    assertEquals(LogLevel.INFO, filter3.verbosity)

    assertEquals("(){}filter", filter4.name)
    assertEquals("filter", filter4.patternString)
    assertFalse(filter4.isCaseSensitive)
    assertEquals(Color(255, 0, 0), filter4.color)
    assertEquals(LogLevel.WARNING, filter4.verbosity)

    assertEquals("./\\()*&ˆˆ", filter5.name)
    assertEquals("Filter Query", filter5.patternString)
    assertFalse(filter5.isCaseSensitive)
    assertEquals(Color(0, 0, 255), filter5.color)
    assertEquals(LogLevel.ERROR, filter5.verbosity)
  }

  @Test
  fun testCompatibilityWithOlderFilterFormat() {
    // By default, older filters that do not contain "verbosity" info, should be created with
    // the maximum verbosity allowed (VERBOSE)
    val serialized1 = "Filter Name,RmlsdGVyIFF1ZXJ5,2,0:0:0"
    val filter1 = Filter.createFromString(serialized1)

    assertEquals(LogLevel.VERBOSE, filter1.verbosity)
  }

  @Test
  fun testDeSerializeRegexFilter() {
    val serialized1 = "Filter Name,XHcrQFx3K1wuKG5ldHxjb20pKFwuYnIpezAsMX0=,2,0:0:0,VERBOSE"
    val serialized2 = "Filter Name,XCtcZC1cKFxkezN9XCktXGR7M30tXGR7NH0=,0,255:255:255,DEBUG"

    val filter1 = Filter.createFromString(serialized1)
    val filter2 = Filter.createFromString(serialized2)

    assertEquals("Filter Name", filter1.name)
    assertEquals("\\w+@\\w+\\.(net|com)(\\.br){0,1}", filter1.patternString)
    assertFalse(filter1.isCaseSensitive)
    assertEquals(Color(0, 0, 0), filter1.color)
    assertEquals(LogLevel.VERBOSE, filter1.verbosity)

    assertEquals("Filter Name", filter2.name)
    assertEquals("\\+\\d-\\(\\d{3}\\)-\\d{3}-\\d{4}", filter2.patternString)
    assertTrue(filter2.isCaseSensitive)
    assertEquals(Color(255, 255, 255), filter2.color)
    assertEquals(LogLevel.DEBUG, filter2.verbosity)
  }

  @Test
  fun testFilterEquals() {
    val serialized1 = "Filter Name,XHcrQFx3K1wuKG5ldHxjb20pKFwuYnIpezAsMX0=,2,0:0:0"
    val serialized2 = "Filter Name,XCtcZC1cKFxkezN9XCktXGR7M30tXGR7NH0=,0,255:255:255"

    val filter1 = Filter.createFromString(serialized1)
    val filter2 = Filter.createFromString(serialized2)
    val filter3 = Filter.createFromString(serialized1)

    assertFalse(filter1.equals(filter2))
    assertTrue(filter1.equals(filter3))
  }

  @Test
  fun testContextInfo() {
    val filter = Filter.createFromString("Filter Name,XHcrQFx3K1wuKG5ldHxjb20pKFwuYnIpezAsMX0=,2,0:0:0")
    filter.initTemporaryInfo()
    val contextInfo = filter.temporaryInfo

    contextInfo.setAllowedStreams(setOf(LogStream.EVENTS, LogStream.MAIN, LogStream.SYSTEM))
    for (i in 1..10) contextInfo.incrementLineCount(LogStream.SYSTEM)
    for (i in 1..15) contextInfo.incrementLineCount(LogStream.MAIN)
    for (i in 1..8) contextInfo.incrementLineCount(LogStream.EVENTS)

    assertEquals(33, contextInfo.totalLinesFound)

    contextInfo.setAllowedStreams(setOf(LogStream.MAIN))

    assertEquals(15, contextInfo.totalLinesFound)

    contextInfo.setAllowedStreams(setOf(LogStream.SYSTEM))

    assertEquals(10, contextInfo.totalLinesFound)

    contextInfo.setAllowedStreams(setOf(LogStream.EVENTS))

    assertEquals(8, contextInfo.totalLinesFound)
  }
}
