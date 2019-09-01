package com.tibagni.logviewer

import com.tibagni.logviewer.filter.Filter
import com.tibagni.logviewer.filter.FilterException
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.log.LogStream
import com.tibagni.logviewer.log.LogTimestamp
import com.tibagni.logviewer.preferences.LogViewerPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.awt.Color
import java.io.File

class LogViewerPresenterTests {
    @Mock
    private lateinit var mockPrefs: LogViewerPreferences

    @Mock
    private lateinit var view: LogViewerView

    private lateinit var presenter: LogViewerPresenter

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        presenter = LogViewerPresenter(view, mockPrefs)
    }

    private fun createTempFiltersFile() =
            File.createTempFile(TEMP_FILE_NAME, TEMP_FILE_EXT).apply {
                writeText(TEST_SERIALIZED_FILTER)
            }

    @Test
    fun testInitNotLoadingLastFilter() {
        `when`(mockPrefs.openLastFilter).thenReturn(false)
        presenter.init()

        verify(mockPrefs, never()).lastFilterPaths
        verify<LogViewerView>(view, never()).configureFiltersList(any())
    }

    @Test
    fun testInitLoadingLastFilterNoFilterAvailable() {
        `when`(mockPrefs.openLastFilter).thenReturn(true)
        `when`(mockPrefs.lastFilterPaths).thenReturn(null)
        presenter.init()

        verify<LogViewerView>(view, never()).configureFiltersList(any())
    }

    @Test
    fun testInitLoadingLastFilter() {
        val filtersTempFile = createTempFiltersFile()
        `when`(mockPrefs.openLastFilter).thenReturn(true)
        `when`(mockPrefs.lastFilterPaths).thenReturn(arrayOf(filtersTempFile))
        presenter.init()

        // Check that correct filter was loaded
        @Suppress("UNCHECKED_CAST")
        val argument = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, List<Filter>>>
        verify<LogViewerView>(view).configureFiltersList(argument.capture())

        val loadedFilters = argument.value[filtersTempFile.name]
        assertNotNull(loadedFilters)
        assertEquals(1, loadedFilters?.size)
        assertEquals("Test", loadedFilters?.first()?.name)
        assertEquals(Color(255, 0, 0), loadedFilters?.first()?.color)

        filtersTempFile.delete()
    }

    @Test
    fun testAddFilter() {
        val testGroup = "testGroup"

        val toAdd = Filter.createFromString(TEST_SERIALIZED_FILTER)
        presenter.addFilterForTests(testGroup, toAdd)

        @Suppress("UNCHECKED_CAST")
        val argument = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, List<Filter>>>
        verify<LogViewerView>(view).configureFiltersList(argument.capture())
        verify<LogViewerView>(view).showUnsavedFilterIndication(testGroup)

        val filtersMap = argument.value
        assertEquals(1, filtersMap.size)
        assertEquals(1, filtersMap[testGroup]?.size)
    }

    @Test
    fun testRemoveOneFilter() {
        val testGroup = "testGroup"
        val toAdd = Filter.createFromString(TEST_SERIALIZED_FILTER)
        val toAdd2 = Filter.createFromString(TEST_SERIALIZED_FILTER2)

        // First we add 2 filters
        presenter.addFilterForTests(testGroup, toAdd)
        presenter.addFilterForTests(testGroup, toAdd2)

        @Suppress("UNCHECKED_CAST")
        val argument = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, List<Filter>>>
        verify<LogViewerView>(view, times(2)).configureFiltersList(argument.capture())
        verify<LogViewerView>(view, atLeastOnce()).showUnsavedFilterIndication(testGroup)

        var resultFilters = argument.value
        assertEquals(1, resultFilters.size)
        assertEquals(2, resultFilters[testGroup]?.size)

        // Now we remove the first filter
        presenter.removeFilters(testGroup, intArrayOf(0))
        // times refers to all times the method was called (2 for add + 1 for remove now)
        verify<LogViewerView>(view, times(3)).configureFiltersList(argument.capture())

        // And check the it was, in fact, removed
        resultFilters = argument.value
        assertEquals(1, resultFilters[testGroup]?.size)

        // Verify that the other filter remains
        assertEquals("Test2", resultFilters[testGroup]?.first()?.name)
    }

    @Test
    fun testRemoveTwoFilters() {
        val testGroup = "testGroup"
        val toAdd = Filter.createFromString(TEST_SERIALIZED_FILTER)
        val toAdd2 = Filter.createFromString(TEST_SERIALIZED_FILTER2)
        val toAdd3 = Filter.createFromString(TEST_SERIALIZED_FILTER3)

        // First we add 3 filters
        presenter.addFilterForTests(testGroup, toAdd)
        presenter.addFilterForTests(testGroup, toAdd2)
        presenter.addFilterForTests(testGroup, toAdd3)

        @Suppress("UNCHECKED_CAST")
        val argument = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, List<Filter>>>
        verify<LogViewerView>(view, times(3)).configureFiltersList(argument.capture())
        verify<LogViewerView>(view, atLeastOnce()).showUnsavedFilterIndication(testGroup)

        var resultFilters = argument.value
        assertEquals(3, resultFilters[testGroup]?.size)

        // Now we remove the first filter
        presenter.removeFilters(testGroup, intArrayOf(0, 1))
        // times refers to all times the method was called (3 for add + 1 for remove now)
        verify<LogViewerView>(view, times(4)).configureFiltersList(argument.capture())

        // And check that it was, in fact, removed
        resultFilters = argument.value
        assertEquals(1, resultFilters[testGroup]?.size)

        // Verify that the other filter remains
        assertEquals("Test3", resultFilters[testGroup]?.first()?.name)
    }

    @Test
    fun testReorderFilters() {
        val testGroup = "testGroup"
        val toAdd = Filter.createFromString(TEST_SERIALIZED_FILTER)
        val toAdd2 = Filter.createFromString(TEST_SERIALIZED_FILTER2)
        val toAdd3 = Filter.createFromString(TEST_SERIALIZED_FILTER3)

        // First we add 3 filters
        presenter.addFilterForTests(testGroup, toAdd)
        presenter.addFilterForTests(testGroup, toAdd2)
        presenter.addFilterForTests(testGroup, toAdd3)

        @Suppress("UNCHECKED_CAST")
        val argument = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, List<Filter>>>
        verify<LogViewerView>(view, times(3)).configureFiltersList(argument.capture())

        // Ensure the order is the added order
        var resultFilters = argument.value
        assertEquals(3, resultFilters[testGroup]?.size)
        assertEquals("Test", resultFilters[testGroup]?.get(0)?.name)
        assertEquals("Test2", resultFilters[testGroup]?.get(1)?.name)
        assertEquals("Test3", resultFilters[testGroup]?.get(2)?.name)

        // Now we exchange Test3 with Test2
        presenter.reorderFilters(testGroup, 2, 1)
        // times refers to all times the method was called (3 for add + 1 for reorder now)
        verify<LogViewerView>(view, times(4)).configureFiltersList(argument.capture())

        // And now check the new order
        resultFilters = argument.value
        assertEquals(3, resultFilters[testGroup]?.size)
        assertEquals("Test", resultFilters[testGroup]?.get(0)?.name)
        assertEquals("Test3", resultFilters[testGroup]?.get(1)?.name)
        assertEquals("Test2", resultFilters[testGroup]?.get(2)?.name)
    }

    @Test
    fun testReorderFilters2() {
        val testGroup = "testGroup"
        val toAdd = Filter.createFromString(TEST_SERIALIZED_FILTER)
        val toAdd2 = Filter.createFromString(TEST_SERIALIZED_FILTER2)
        val toAdd3 = Filter.createFromString(TEST_SERIALIZED_FILTER3)

        // First we add 3 filters
        presenter.addFilterForTests(testGroup, toAdd)
        presenter.addFilterForTests(testGroup, toAdd2)
        presenter.addFilterForTests(testGroup, toAdd3)

        @Suppress("UNCHECKED_CAST")
        val argument = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, List<Filter>>>
        verify<LogViewerView>(view, times(3)).configureFiltersList(argument.capture())

        // Ensure the order is the added order
        var resultFilters = argument.value
        assertEquals(3, resultFilters[testGroup]?.size)
        assertEquals("Test", resultFilters[testGroup]?.get(0)?.name)
        assertEquals("Test2", resultFilters[testGroup]?.get(1)?.name)
        assertEquals("Test3", resultFilters[testGroup]?.get(2)?.name)

        // Now we exchange Test3 with Test2
        presenter.reorderFilters(testGroup, 2, 0)
        // times refers to all times the method was called (3 for add + 1 for reorder now)
        verify<LogViewerView>(view, times(4)).configureFiltersList(argument.capture())

        // And now check the new order
        resultFilters = argument.value
        assertEquals(3, resultFilters[testGroup]?.size)
        assertEquals("Test3", resultFilters[testGroup]?.get(0)?.name)
        assertEquals("Test", resultFilters[testGroup]?.get(1)?.name)
        assertEquals("Test2", resultFilters[testGroup]?.get(2)?.name)
    }

    @Test
    @Throws(FilterException::class)
    fun testReorderFilters3() {
        val testGroup = "testGroup"
        val toAdd = Filter.createFromString(TEST_SERIALIZED_FILTER)
        val toAdd2 = Filter.createFromString(TEST_SERIALIZED_FILTER2)
        val toAdd3 = Filter.createFromString(TEST_SERIALIZED_FILTER3)

        // First we add 3 filters
        presenter.addFilterForTests(testGroup, toAdd)
        presenter.addFilterForTests(testGroup, toAdd2)
        presenter.addFilterForTests(testGroup, toAdd3)

        @Suppress("UNCHECKED_CAST")
        val argument = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, List<Filter>>>
        verify<LogViewerView>(view, times(3)).configureFiltersList(argument.capture())

        // Ensure the order is the added order
        var resultFilters = argument.value
        assertEquals(3, resultFilters[testGroup]?.size)
        assertEquals("Test", resultFilters[testGroup]?.get(0)?.name)
        assertEquals("Test2", resultFilters[testGroup]?.get(1)?.name)
        assertEquals("Test3", resultFilters[testGroup]?.get(2)?.name)

        // Now we exchange Test3 with Test2
        presenter.reorderFilters(testGroup, 0, 2)
        // times refers to all times the method was called (3 for add + 1 for reorder now)
        verify<LogViewerView>(view, times(4)).configureFiltersList(argument.capture())

        // And now check the new order
        resultFilters = argument.value
        assertEquals(3, resultFilters[testGroup]?.size)
        assertEquals("Test2", resultFilters[testGroup]?.get(0)?.name)
        assertEquals("Test", resultFilters[testGroup]?.get(1)?.name)
        assertEquals("Test3", resultFilters[testGroup]?.get(2)?.name)
    }

    @Test
    @Throws(FilterException::class)
    fun testReorderFilters4() {
        val testGroup = "testGroup"
        val toAdd = Filter.createFromString(TEST_SERIALIZED_FILTER)
        val toAdd2 = Filter.createFromString(TEST_SERIALIZED_FILTER2)
        val toAdd3 = Filter.createFromString(TEST_SERIALIZED_FILTER3)

        // First we add 3 filters
        presenter.addFilterForTests(testGroup, toAdd)
        presenter.addFilterForTests(testGroup, toAdd2)
        presenter.addFilterForTests(testGroup, toAdd3)

        @Suppress("UNCHECKED_CAST")
        val argument = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, List<Filter>>>
        verify<LogViewerView>(view, times(3)).configureFiltersList(argument.capture())

        // Ensure the order is the added order
        var resultFilters = argument.value
        assertEquals(3, resultFilters[testGroup]?.size)
        assertEquals("Test", resultFilters[testGroup]?.get(0)?.name)
        assertEquals("Test2", resultFilters[testGroup]?.get(1)?.name)
        assertEquals("Test3", resultFilters[testGroup]?.get(2)?.name)

        // Now we exchange Test3 with Test2
        presenter.reorderFilters(testGroup, 0, 3)
        // times refers to all times the method was called (3 for add + 1 for reorder now)
        verify<LogViewerView>(view, times(4)).configureFiltersList(argument.capture())

        // And now check the new order
        resultFilters = argument.value
        assertEquals(3, resultFilters[testGroup]?.size)
        assertEquals("Test2", resultFilters[testGroup]?.get(0)?.name)
        assertEquals("Test3", resultFilters[testGroup]?.get(1)?.name)
        assertEquals("Test", resultFilters[testGroup]?.get(2)?.name)
    }

    @Test
    fun testFinishingSaveChanges() {
        val testGroup = "testGroup"
        val filter = Filter.createFromString(TEST_SERIALIZED_FILTER)

        // Add a filter to simulate 'unsaved changes'
        presenter.addFilterForTests(testGroup, filter)

        `when`<LogViewer.UserSelection>(view.showAskToSaveFilterDialog(testGroup)).thenReturn(LogViewer.UserSelection.CONFIRMED)
        presenter.finishing()

        verify(view).showSaveFilters(testGroup)
        verify(view).finish()
    }

    @Test
    fun testFinishingDontSaveChanges() {
        val testGroup = "testGroup"
        val filter = Filter.createFromString(TEST_SERIALIZED_FILTER)

        // Add a filter to simulate 'unsaved changes'
        presenter.addFilterForTests(testGroup, filter)

        `when`<LogViewer.UserSelection>(view.showAskToSaveFilterDialog(testGroup)).thenReturn(LogViewer.UserSelection.REJECTED)
        presenter.finishing()

        verify(view, never()).showSaveFilters(testGroup)
        verify(view).finish()
    }

    @Test
    fun testFinishingCancelChanges() {
        val testGroup = "testGroup"
        val filter = Filter.createFromString(TEST_SERIALIZED_FILTER)

        // Add a filter to simulate 'unsaved changes'
        presenter.addFilterForTests(testGroup, filter)

        `when`<LogViewer.UserSelection>(view.showAskToSaveFilterDialog(testGroup)).thenReturn(LogViewer.UserSelection.CANCELLED)
        presenter.finishing()

        verify(view, never()).showSaveFilters(testGroup)
        verify(view, never()).finish()
    }

    @Test
    fun testFinishingNoChanges() {
        presenter.finishing()

        verify<LogViewerView>(view, never()).showAskToSaveFilterDialog(any())
        verify<LogViewerView>(view, never()).showSaveFilters(any())
        verify<LogViewerView>(view).finish()
    }

    @Test
    fun testNavigateNextSingleFilter() {
        val timestamp = LogTimestamp(
                10,
                12,
                22,
                32,
                50,
                264
        )

        presenter.setFilteredLogsForTesting(arrayOf(LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp)))

        val filters = listOf(Filter("name", "ABCDeF", Color.black))
        val filter = filters.first()
        presenter.setFiltersForTesting(filters)

        // Set 'unknown' stream allowed as we just want to test navigation (and default stream is unknown)
        presenter.setAvailableStreamsForTesting(setOf(LogStream.UNKNOWN))
        presenter.setStreamAllowed(LogStream.UNKNOWN, true)

        var actual = presenter.getNextFilteredLogForFilter(filter, -1)
        assertEquals(0, actual)
        verify<LogViewerView>(view, never()).showNavigationNextOver()

        actual = presenter.getNextFilteredLogForFilter(filter, 0)
        assertEquals(1, actual)
        verify<LogViewerView>(view, never()).showNavigationNextOver()

        actual = presenter.getNextFilteredLogForFilter(filter, 1)
        assertEquals(2, actual)
        verify<LogViewerView>(view, never()).showNavigationNextOver()

        actual = presenter.getNextFilteredLogForFilter(filter, 2)
        assertEquals(0, actual)
        verify<LogViewerView>(view, times(1)).showNavigationNextOver()
    }

    @Test
    fun testNavigateNextMultipleFilters() {
        val timestamp = LogTimestamp(
                10,
                12,
                22,
                32,
                50,
                264
        )

        presenter.setFilteredLogsForTesting(arrayOf(LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : AB log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABDeF log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp)))

        val filters = listOf(Filter("name", "ABCDeF", Color.black))
        val filter = filters.first()
        presenter.setFiltersForTesting(filters)

        // Set 'unknown' stream allowed as we just want to test navigation (and default stream is unknown)
        presenter.setAvailableStreamsForTesting(setOf(LogStream.UNKNOWN))
        presenter.setStreamAllowed(LogStream.UNKNOWN, true)

        var actual = presenter.getNextFilteredLogForFilter(filter, -1)
        assertEquals(0, actual)
        verify<LogViewerView>(view, never()).showNavigationNextOver()

        actual = presenter.getNextFilteredLogForFilter(filter, 0)
        assertEquals(2, actual)
        verify<LogViewerView>(view, never()).showNavigationNextOver()

        actual = presenter.getNextFilteredLogForFilter(filter, 2)
        assertEquals(3, actual)
        verify<LogViewerView>(view, never()).showNavigationNextOver()

        actual = presenter.getNextFilteredLogForFilter(filter, 3)
        assertEquals(5, actual)
        verify<LogViewerView>(view, never()).showNavigationNextOver()

        actual = presenter.getNextFilteredLogForFilter(filter, 5)
        assertEquals(0, actual)
        verify<LogViewerView>(view, times(1)).showNavigationNextOver()
    }

    @Test
    fun testNavigateNextMultipleFilters2() {
        val timestamp = LogTimestamp(
                10,
                12,
                22,
                32,
                50,
                264
        )

        presenter.setFilteredLogsForTesting(arrayOf(LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCD log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : AB log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABDeF log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp)))

        val filters = listOf(Filter("name", "ABCDeF", Color.black))
        val filter = filters.first()
        presenter.setFiltersForTesting(filters)

        // Set 'unknown' stream allowed as we just want to test navigation (and default stream is unknown)
        presenter.setAvailableStreamsForTesting(setOf(LogStream.UNKNOWN))
        presenter.setStreamAllowed(LogStream.UNKNOWN, true)

        var actual = presenter.getNextFilteredLogForFilter(filter, -1)
        assertEquals(2, actual)
        verify<LogViewerView>(view, never()).showNavigationNextOver()

        actual = presenter.getNextFilteredLogForFilter(filter, 2)
        assertEquals(3, actual)
        verify<LogViewerView>(view, never()).showNavigationNextOver()

        actual = presenter.getNextFilteredLogForFilter(filter, 3)
        assertEquals(5, actual)
        verify<LogViewerView>(view, never()).showNavigationNextOver()

        actual = presenter.getNextFilteredLogForFilter(filter, 5)
        assertEquals(2, actual)
        verify<LogViewerView>(view, times(1)).showNavigationNextOver()
    }

    @Test
    fun testNavigatePrevSingleFilter() {
        val timestamp = LogTimestamp(
                10,
                12,
                22,
                32,
                50,
                264
        )

        presenter.setFilteredLogsForTesting(arrayOf(LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp)))

        val filters = listOf(Filter("name", "ABCDeF", Color.black))
        val filter = filters.first()
        presenter.setFiltersForTesting(filters)

        // Set 'unknown' stream allowed as we just want to test navigation (and default stream is unknown)
        presenter.setAvailableStreamsForTesting(setOf(LogStream.UNKNOWN))
        presenter.setStreamAllowed(LogStream.UNKNOWN, true)

        var actual = presenter.getPrevFilteredLogForFilter(filter, -1)
        assertEquals(2, actual)
        verify<LogViewerView>(view, never()).showNavigationPrevOver()

        actual = presenter.getPrevFilteredLogForFilter(filter, 2)
        assertEquals(1, actual)
        verify<LogViewerView>(view, never()).showNavigationPrevOver()

        actual = presenter.getPrevFilteredLogForFilter(filter, 1)
        assertEquals(0, actual)
        verify<LogViewerView>(view, never()).showNavigationPrevOver()

        actual = presenter.getPrevFilteredLogForFilter(filter, 0)
        assertEquals(2, actual)
        verify<LogViewerView>(view, times(1)).showNavigationPrevOver()
    }

    @Test
    fun testNavigatePrevMultipleFilters() {
        val timestamp = LogTimestamp(
                10,
                12,
                22,
                32,
                50,
                264
        )

        presenter.setFilteredLogsForTesting(arrayOf(LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : AB log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABDeF log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp)))

        val filters = listOf(Filter("name", "ABCDeF", Color.black))
        val filter = filters.first()
        presenter.setFiltersForTesting(filters)

        // Set 'unknown' stream allowed as we just want to test navigation (and default stream is unknown)
        presenter.setAvailableStreamsForTesting(setOf(LogStream.UNKNOWN))
        presenter.setStreamAllowed(LogStream.UNKNOWN, true)

        var actual = presenter.getPrevFilteredLogForFilter(filter, -1)
        assertEquals(5, actual)
        verify<LogViewerView>(view, never()).showNavigationPrevOver()

        actual = presenter.getPrevFilteredLogForFilter(filter, 5)
        assertEquals(3, actual)
        verify<LogViewerView>(view, never()).showNavigationPrevOver()

        actual = presenter.getPrevFilteredLogForFilter(filter, 3)
        assertEquals(2, actual)
        verify<LogViewerView>(view, never()).showNavigationPrevOver()

        actual = presenter.getPrevFilteredLogForFilter(filter, 2)
        assertEquals(0, actual)
        verify<LogViewerView>(view, never()).showNavigationPrevOver()

        actual = presenter.getPrevFilteredLogForFilter(filter, 0)
        assertEquals(5, actual)
        verify<LogViewerView>(view, times(1)).showNavigationPrevOver()
    }

    @Test
    fun testNavigatePrevMultipleFilters2() {
        val timestamp = LogTimestamp(
                10,
                12,
                22,
                32,
                50,
                264
        )

        presenter.setFilteredLogsForTesting(arrayOf(LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : AB log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABDeF log Test Log", LogLevel.INFO, timestamp), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCD log Test Log", LogLevel.INFO, timestamp)))

        val filters = listOf(Filter("name", "ABCDeF", Color.black))
        val filter = filters.first()
        presenter.setFiltersForTesting(filters)

        // Set 'unknown' stream allowed as we just want to test navigation (and default stream is unknown)
        presenter.setAvailableStreamsForTesting(setOf(LogStream.UNKNOWN))
        presenter.setStreamAllowed(LogStream.UNKNOWN, true)

        var actual = presenter.getPrevFilteredLogForFilter(filter, -1)
        assertEquals(3, actual)
        verify<LogViewerView>(view, never()).showNavigationPrevOver()

        actual = presenter.getPrevFilteredLogForFilter(filter, 3)
        assertEquals(2, actual)
        verify<LogViewerView>(view, never()).showNavigationPrevOver()

        actual = presenter.getPrevFilteredLogForFilter(filter, 2)
        assertEquals(0, actual)
        verify<LogViewerView>(view, never()).showNavigationPrevOver()

        actual = presenter.getPrevFilteredLogForFilter(filter, 0)
        assertEquals(3, actual)
        verify<LogViewerView>(view, times(1)).showNavigationPrevOver()
    }

    @Test
    fun testAllowedStreamsSetNotAllowed() {
        val timestamp = LogTimestamp(
                10,
                12,
                22,
                32,
                50,
                264
        )

        presenter.setFilteredLogsForTesting(arrayOf(LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : AB log Test Log", LogLevel.INFO, timestamp, "radio"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "events"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABDeF log Test Log", LogLevel.INFO, timestamp, "bla"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCD log Test Log", LogLevel.INFO, timestamp, "main")))
        presenter.setAvailableStreamsForTesting(setOf(
                LogStream.MAIN,
                LogStream.EVENTS,
                LogStream.RADIO,
                LogStream.SYSTEM,
                LogStream.UNKNOWN
        ), true)
        presenter.setStreamAllowed(LogStream.MAIN, false)
        presenter.setStreamAllowed(LogStream.EVENTS, false)
        presenter.setStreamAllowed(LogStream.RADIO, false)
        presenter.setStreamAllowed(LogStream.UNKNOWN, false)

        // 'showFilteredLogs' should be called 4 times (one for each time we called 'setStreamAllowed')
        // So we need to validate the output of all executions
        val argument = ArgumentCaptor.forClass(Array<LogEntry>::class.java)
        verify<LogViewerView>(view, times(4)).showFilteredLogs(argument.capture())

        val allValues = argument.allValues
        var filteredLogs = allValues[0]
        assertEquals(4, filteredLogs.size)

        filteredLogs = allValues[1]
        assertEquals(3, filteredLogs.size)

        filteredLogs = allValues[2]
        assertEquals(2, filteredLogs.size)

        filteredLogs = allValues[3]
        assertEquals(1, filteredLogs.size)
    }

    @Test
    fun testAllowedStreamsSetAllowed() {
        val timestamp = LogTimestamp(
                10,
                12,
                22,
                32,
                50,
                264
        )

        presenter.setFilteredLogsForTesting(arrayOf(LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : AB log Test Log", LogLevel.INFO, timestamp, "radio"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "events"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABDeF log Test Log", LogLevel.INFO, timestamp, "bla"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCD log Test Log", LogLevel.INFO, timestamp, "main")))
        presenter.setAvailableStreamsForTesting(setOf(
                LogStream.MAIN,
                LogStream.EVENTS,
                LogStream.RADIO,
                LogStream.SYSTEM,
                LogStream.UNKNOWN)
        )
        presenter.setStreamAllowed(LogStream.SYSTEM, true)
        presenter.setStreamAllowed(LogStream.MAIN, true)

        // 'showFilteredLogs' should be called 2 times (one for each time we called 'setStreamAllowed')
        // So we need to validate the output of all executions
        val argument = ArgumentCaptor.forClass(Array<LogEntry>::class.java)
        verify<LogViewerView>(view, times(2)).showFilteredLogs(argument.capture())

        val allValues = argument.allValues
        var filteredLogs = allValues[0]
        assertEquals(1, filteredLogs.size)

        filteredLogs = allValues[1]
        assertEquals(3, filteredLogs.size)
    }

    @Test
    fun testNavigateNextMultipleFiltersWithAllowedStreams() {
        val timestamp = LogTimestamp(
                10,
                12,
                22,
                32,
                50,
                264
        )

        presenter.setFilteredLogsForTesting(arrayOf(LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "radio"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "events"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "bla"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main")))

        // This will make the filtered logs shown in UI to have only 3 entries
        // (2 for main and 1 for system)
        presenter.setAvailableStreamsForTesting(setOf(
                LogStream.MAIN,
                LogStream.EVENTS,
                LogStream.RADIO,
                LogStream.SYSTEM,
                LogStream.UNKNOWN)
        )
        presenter.setStreamAllowed(LogStream.MAIN, true)
        presenter.setStreamAllowed(LogStream.SYSTEM, true)

        val filters = listOf(Filter("name", "ABCDeF", Color.black))
        val filter = filters.first()
        presenter.setFiltersForTesting(filters)

        // Because of the allowed streams we set earlier, the filters shown on UI should be something like below:
        //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main"),
        //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"),
        //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main")

        // So we need to test the indices based on the above array, not the original one

        var actual = presenter.getNextFilteredLogForFilter(filter, -1)
        assertEquals(0, actual)

        actual = presenter.getNextFilteredLogForFilter(filter, 0)
        assertEquals(1, actual)

        actual = presenter.getNextFilteredLogForFilter(filter, 1)
        assertEquals(2, actual)
    }

    @Test
    fun testNavigateNextMultipleFiltersWithAllowedStreams2() {
        val timestamp = LogTimestamp(
                10,
                12,
                22,
                32,
                50,
                264
        )

        presenter.setFilteredLogsForTesting(arrayOf(LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABeF log Test Log", LogLevel.INFO, timestamp, "main"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "radio"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "events"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "bla"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main")))

        // This will make the filtered logs shown in UI to have only 3 entries
        // (2 for main and 1 for system)
        presenter.setAvailableStreamsForTesting(setOf(
                LogStream.MAIN,
                LogStream.EVENTS,
                LogStream.RADIO,
                LogStream.SYSTEM,
                LogStream.UNKNOWN)
        )
        presenter.setStreamAllowed(LogStream.MAIN, true)
        presenter.setStreamAllowed(LogStream.SYSTEM, true)

        val filters = listOf(Filter("name", "ABCDeF", Color.black))
        val filter = filters.first()
        presenter.setFiltersForTesting(filters)

        // Because of the allowed streams we set earlier, the filters shown on UI should be something like below:
        //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABeF log Test Log", LogLevel.INFO, timestamp, "main"),
        //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"),
        //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main")

        // So we need to test the indices based on the above array, not the original one

        var actual = presenter.getNextFilteredLogForFilter(filter, -1)
        assertEquals(1, actual)

        actual = presenter.getNextFilteredLogForFilter(filter, 1)
        assertEquals(2, actual)
    }

    @Test
    fun testNavigatePrevMultipleFiltersWithAllowedStreams() {
        val timestamp = LogTimestamp(
                10,
                12,
                22,
                32,
                50,
                264
        )

        presenter.setFilteredLogsForTesting(arrayOf(LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "radio"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "events"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "bla"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main")))

        // This will make the filtered logs shown in UI to have only 3 entries
        // (2 for main and 1 for system)
        presenter.setAvailableStreamsForTesting(setOf(
                LogStream.MAIN,
                LogStream.EVENTS,
                LogStream.RADIO,
                LogStream.SYSTEM,
                LogStream.UNKNOWN)
        )
        presenter.setStreamAllowed(LogStream.MAIN, true)
        presenter.setStreamAllowed(LogStream.SYSTEM, true)

        val filters = listOf(Filter("name", "ABCDeF", Color.black))
        val filter = filters.first()
        presenter.setFiltersForTesting(filters)

        // Because of the allowed streams we set earlier, the filters shown on UI should be something like below:
        //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main"),
        //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"),
        //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main")

        // So we need to test the indices based on the above array, not the original one

        var actual = presenter.getPrevFilteredLogForFilter(filter, -1)
        assertEquals(2, actual)

        actual = presenter.getPrevFilteredLogForFilter(filter, 2)
        assertEquals(1, actual)

        actual = presenter.getPrevFilteredLogForFilter(filter, 1)
        assertEquals(0, actual)
    }

    @Test
    fun testNavigatePrevMultipleFiltersWithAllowedStreams2() {
        val timestamp = LogTimestamp(
                10,
                12,
                22,
                32,
                50,
                264
        )

        presenter.setFilteredLogsForTesting(arrayOf(LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABeF log Test Log", LogLevel.INFO, timestamp, "main"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "radio"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "events"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "bla"), LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main")))

        // This will make the filtered logs shown in UI to have only 3 entries
        // (2 for main and 1 for system)
        presenter.setAvailableStreamsForTesting(setOf(
                LogStream.MAIN,
                LogStream.EVENTS,
                LogStream.RADIO,
                LogStream.SYSTEM,
                LogStream.UNKNOWN)
        )
        presenter.setStreamAllowed(LogStream.MAIN, true)
        presenter.setStreamAllowed(LogStream.SYSTEM, true)

        val filters = listOf(Filter("name", "ABCDeF", Color.black))
        val filter = filters.first()
        presenter.setFiltersForTesting(filters)

        // Because of the allowed streams we set earlier, the filters shown on UI should be something like below:
        //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABeF log Test Log", LogLevel.INFO, timestamp, "main"),
        //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"),
        //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main")

        // So we need to test the indices based on the above array, not the original one

        var actual = presenter.getPrevFilteredLogForFilter(filter, -1)
        assertEquals(2, actual)

        actual = presenter.getPrevFilteredLogForFilter(filter, 2)
        assertEquals(1, actual)

        actual = presenter.getPrevFilteredLogForFilter(filter, 1)
        assertEquals(2, actual)
    }

    @Test
    fun testRemoveGroup() {
        val groupToRemove = "removeGroup"
        val testGroup = "testGroup"

        presenter.setFiltersForTesting(testGroup, listOf(Filter.createFromString(TEST_SERIALIZED_FILTER)))
        presenter.setFiltersForTesting(groupToRemove, listOf(Filter.createFromString(TEST_SERIALIZED_FILTER2),
                Filter.createFromString(TEST_SERIALIZED_FILTER3)))

        presenter.removeGroup(groupToRemove)

        verify(view, never()).showAskToSaveFilterDialog(any())

        @Suppress("UNCHECKED_CAST")
        val argument = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, List<Filter>>>
        verify(view, times(1)).configureFiltersList(argument.capture())

        assertEquals(1, argument.value.keys.size)
        assertTrue(argument.value.containsKey(testGroup))
        assertFalse(argument.value.containsKey(groupToRemove))
    }

    @Test
    fun testRemoveGroupUnsaved() {
        val groupToRemove = "removeGroup"
        val testGroup = "testGroup"

        presenter.setFiltersForTesting(testGroup, listOf(Filter.createFromString(TEST_SERIALIZED_FILTER)))
        presenter.setFiltersForTesting(groupToRemove, listOf(Filter.createFromString(TEST_SERIALIZED_FILTER2),
                Filter.createFromString(TEST_SERIALIZED_FILTER3)))
        presenter.setUnsavedGroupForTesting(groupToRemove)

        presenter.removeGroup(groupToRemove)

        verify(view, times(1)).showAskToSaveFilterDialog(any())

        @Suppress("UNCHECKED_CAST")
        val argument = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, List<Filter>>>
        verify(view, times(1)).configureFiltersList(argument.capture())

        assertEquals(1, argument.value.keys.size)
        assertTrue(argument.value.containsKey(testGroup))
        assertFalse(argument.value.containsKey(groupToRemove))
    }

    @Test
    fun testRemoveGroupInvalid() {
        val testGroup = "testGroup"
        val testGroup2 = "testGroup2"

        presenter.setFiltersForTesting(testGroup, listOf(Filter.createFromString(TEST_SERIALIZED_FILTER)))
        presenter.setFiltersForTesting(testGroup2, listOf(Filter.createFromString(TEST_SERIALIZED_FILTER2),
                Filter.createFromString(TEST_SERIALIZED_FILTER3)))

        presenter.removeGroup("invalidGroup")

        verify(view, never()).showAskToSaveFilterDialog(any())
        verify(view, never()).configureFiltersList(any())
    }

    @Test
    fun testRemoveGroupEmpty() {
        val testGroup = "testGroup"
        val testGroup2 = "testGroup2"

        presenter.setFiltersForTesting(testGroup, listOf(Filter.createFromString(TEST_SERIALIZED_FILTER)))
        presenter.setFiltersForTesting(testGroup2, listOf(Filter.createFromString(TEST_SERIALIZED_FILTER2),
                Filter.createFromString(TEST_SERIALIZED_FILTER3)))

        presenter.removeGroup("")

        verify(view, never()).showAskToSaveFilterDialog(any())
        verify(view, never()).configureFiltersList(any())
    }

    @Test
    fun testRemoveGroupNull() {
        val testGroup = "testGroup"
        val testGroup2 = "testGroup2"

        presenter.setFiltersForTesting(testGroup, listOf(Filter.createFromString(TEST_SERIALIZED_FILTER)))
        presenter.setFiltersForTesting(testGroup2, listOf(Filter.createFromString(TEST_SERIALIZED_FILTER2),
                Filter.createFromString(TEST_SERIALIZED_FILTER3)))

        presenter.removeGroup(null)

        verify(view, never()).showAskToSaveFilterDialog(any())
        verify(view, never()).configureFiltersList(any())
    }

    companion object {
        private const val TEMP_FILE_NAME = "tempFilter"
        private const val TEMP_FILE_EXT = ".tmp"

        private const val TEST_SERIALIZED_FILTER = "Test,VGVzdA==,2,255:0:0"
        private const val TEST_SERIALIZED_FILTER2 = "Test2,VGVzdA==,2,255:0:0"
        private const val TEST_SERIALIZED_FILTER3 = "Test3,VGVzdA==,2,255:0:0"
    }
}
