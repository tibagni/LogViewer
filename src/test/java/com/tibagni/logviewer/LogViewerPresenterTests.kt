package com.tibagni.logviewer

import com.tibagni.logviewer.filter.Filter
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.log.LogStream
import com.tibagni.logviewer.log.LogTimestamp
import com.tibagni.logviewer.preferences.LogViewerPreferences
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.awt.Color
import java.io.File

class LogViewerPresenterTests {
  @Mock
  private lateinit var mockPrefs: LogViewerPreferences

  @Mock
  private lateinit var view: LogViewerView

  @Mock
  private lateinit var mockLogsRepository: LogsRepository

  @Mock
  private lateinit var mockFiltersRepository: FiltersRepository

  private lateinit var presenter: LogViewerPresenter
  private var tempFilterFile: File? = null
  private var tempLogFile: File? = null

  /**
   * Returns Mockito.any() as nullable type to avoid java.lang.IllegalStateException when
   * null is returned.
   */
  private fun <T> anyOrNull(): T = Mockito.any<T>()
  private fun <T> eqOrNull(value: T): T = eq<T>(value)

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    presenter = LogViewerPresenter(view, mockPrefs, mockLogsRepository, mockFiltersRepository)
    presenter.setBgExecutorService(MockExecutorService())
    presenter.setUiExecutor { it.run() }
  }

  @After
  fun tearDown() {
    tempFilterFile?.delete()
    tempLogFile?.delete()
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
    `when`(mockPrefs.lastFilterPaths).thenReturn(arrayOf())
    presenter.init()

    verify<LogViewerView>(view, never()).configureFiltersList(any())
  }

  @Test
  fun testInitLoadingLastFilter() {
    val inputFile = File("mock")
    `when`(mockPrefs.openLastFilter).thenReturn(true)
    `when`(mockPrefs.lastFilterPaths).thenReturn(arrayOf(inputFile))
    presenter.init()

    verify(mockFiltersRepository).openFilterFiles(arrayOf(inputFile))
    verify(view).configureFiltersList(any())
  }

  @Test
  fun testAddFilterNoApplyOnAdd() {
    val toAdd = Filter.createFromString(TEST_SERIALIZED_FILTER)
    `when`(mockPrefs.reapplyFiltersAfterEdit).thenReturn(false)

    presenter.addFilter("testGroup", toAdd)

    verify(mockFiltersRepository, times(1)).addFilter("testGroup", toAdd)
    verify(view, times(1)).configureFiltersList(any())
    verify(view, never()).showFilteredLogs(any())
  }

  @Test
  fun testAddFilterApplyOnAdd() {
    val toAdd = Filter.createFromString(TEST_SERIALIZED_FILTER)
    `when`(mockPrefs.reapplyFiltersAfterEdit).thenReturn(true)
    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(LogEntry("Log line 1", LogLevel.DEBUG, null))
    )

    // Ensure filter is not marked as applied before it is added
    assertFalse(toAdd.isApplied)

    presenter.addFilter("testGroup", toAdd)

    verify(mockFiltersRepository, times(1)).addFilter("testGroup", toAdd)
    verify(view).configureFiltersList(any())
    verify(view).showFilteredLogs(any())
    assertTrue(toAdd.isApplied)
  }

  @Test
  fun testRemoveOneNotAppliedFilter() {
    val remainingFilters = mapOf("testGroup" to listOf(Filter.createFromString(TEST_SERIALIZED_FILTER2)))
    val removedFilter = Filter.createFromString(TEST_SERIALIZED_FILTER)

    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(LogEntry("Log line 1", LogLevel.DEBUG, null))
    )
    `when`(mockFiltersRepository.deleteFilters("testGroup", intArrayOf(0))).thenReturn(listOf(removedFilter))
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(remainingFilters)
    `when`(mockFiltersRepository.getChangedGroupsSinceLastOpened()).thenReturn(listOf("testGroup"))

    presenter.removeFilters("testGroup", intArrayOf(0))

    verify(view, times(1)).configureFiltersList(remainingFilters)
    verify(view, never()).showFilteredLogs(any())
  }

  @Test
  fun testRemoveOneAppliedFilter() {
    val remainingFilters = mapOf("testGroup" to listOf(Filter.createFromString(TEST_SERIALIZED_FILTER2)))
    val removedFilter = Filter.createFromString(TEST_SERIALIZED_FILTER)
    removedFilter.isApplied = true

    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(LogEntry("Log line 1", LogLevel.DEBUG, null))
    )
    `when`(mockFiltersRepository.deleteFilters("testGroup", intArrayOf(0))).thenReturn(listOf(removedFilter))
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(remainingFilters)
    `when`(mockFiltersRepository.getChangedGroupsSinceLastOpened()).thenReturn(listOf("testGroup"))

    presenter.removeFilters("testGroup", intArrayOf(0))

    verify(view, times(1)).configureFiltersList(remainingFilters)
    verify(view).showFilteredLogs(any())
  }

  @Test
  fun testRemoveTwoNotAppliedFilters() {
    val remainingFilters = mapOf("testGroup" to listOf(Filter.createFromString(TEST_SERIALIZED_FILTER3)))
    val removedFilters =
      listOf(Filter.createFromString(TEST_SERIALIZED_FILTER), Filter.createFromString(TEST_SERIALIZED_FILTER2))

    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(LogEntry("Log line 1", LogLevel.DEBUG, null))
    )
    `when`(mockFiltersRepository.deleteFilters("testGroup", intArrayOf(0))).thenReturn(removedFilters)
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(remainingFilters)
    `when`(mockFiltersRepository.getChangedGroupsSinceLastOpened()).thenReturn(listOf("testGroup"))

    presenter.removeFilters("testGroup", intArrayOf(0))

    verify(view, times(1)).configureFiltersList(remainingFilters)
    verify(view, never()).showFilteredLogs(any())
  }

  @Test
  fun testRemoveTwoFiltersOneApplied() {
    val remainingFilters = mapOf("testGroup" to listOf(Filter.createFromString(TEST_SERIALIZED_FILTER3)))
    val appliedFilter = Filter.createFromString(TEST_SERIALIZED_FILTER2)
    appliedFilter.isApplied = true
    val removedFilters = listOf(appliedFilter, Filter.createFromString(TEST_SERIALIZED_FILTER))

    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(LogEntry("Log line 1", LogLevel.DEBUG, null))
    )
    `when`(mockFiltersRepository.deleteFilters("testGroup", intArrayOf(0))).thenReturn(removedFilters)
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(remainingFilters)
    `when`(mockFiltersRepository.getChangedGroupsSinceLastOpened()).thenReturn(listOf("testGroup"))

    presenter.removeFilters("testGroup", intArrayOf(0))

    verify(view, times(1)).configureFiltersList(remainingFilters)
    verify(view).showFilteredLogs(any())
  }

  @Test
  fun testReorderFilters() {
    presenter.reorderFilters("testGroup", 2, 1)

    verify(mockFiltersRepository).reorderFilters("testGroup", 2, 1)
    verify(view).configureFiltersList(any())
  }

  @Test
  fun testReorderFiltersSameOrder() {
    presenter.reorderFilters("testGroup", 1, 1)

    verify(mockFiltersRepository, never()).reorderFilters("testGroup", 2, 1)
    verify(view, never()).configureFiltersList(any())
  }

  @Test
  fun testFinishingSaveChangesGroupHasFile() {
    val testGroup = "testGroup"
    val testFile = File("testFile")
    presenter.setUnsavedGroupForTesting(testGroup)
    `when`(view.showAskToSaveFilterDialog(testGroup)).thenReturn(LogViewer.UserSelection.CONFIRMED)
    `when`(mockFiltersRepository.currentlyOpenedFilterFiles).thenReturn(mapOf(testGroup to testFile))

    presenter.finishing()

    verify(view, never()).showSaveFilters(testGroup)
    verify(mockFiltersRepository).persistGroup(testFile, testGroup)
    verify(view).finish()
  }

  @Test
  fun testFinishingSaveChangesGroupHasNoFileConfirmSave() {
    val testGroup = "testGroup"
    val testFile = File("testFile")
    presenter.setUnsavedGroupForTesting(testGroup)
    `when`(view.showAskToSaveFilterDialog(testGroup)).thenReturn(LogViewer.UserSelection.CONFIRMED)
    `when`(view.showSaveFilters(testGroup)).thenReturn(testFile)
    `when`(mockFiltersRepository.currentlyOpenedFilterFiles).thenReturn(mapOf())

    presenter.finishing()

    verify(view).showSaveFilters(testGroup)
    verify(mockFiltersRepository).persistGroup(testFile, testGroup)
    verify(view).finish()
  }

  @Test
  fun testFinishingSaveChangesGroupHasNoFileNoSave() {
    val testGroup = "testGroup"
    presenter.setUnsavedGroupForTesting(testGroup)
    `when`(view.showAskToSaveFilterDialog(testGroup)).thenReturn(LogViewer.UserSelection.CONFIRMED)
    `when`(view.showSaveFilters(testGroup)).thenReturn(null)
    `when`(mockFiltersRepository.currentlyOpenedFilterFiles).thenReturn(mapOf())

    presenter.finishing()

    verify(view).showSaveFilters(testGroup)
    verify(mockFiltersRepository, never()).persistGroup(anyOrNull(), anyString())
    verify(view).finish()
  }

  @Test
  fun testFinishingDontSaveChanges() {
    val testGroup = "testGroup"
    presenter.setUnsavedGroupForTesting(testGroup)
    `when`(view.showAskToSaveFilterDialog(testGroup)).thenReturn(LogViewer.UserSelection.REJECTED)

    presenter.finishing()

    verify(view, never()).showSaveFilters(testGroup)
    verify(mockFiltersRepository, never()).persistGroup(anyOrNull(), anyString())
    verify(view).finish()
  }

  @Test
  fun testFinishingCancelChanges() {
    val testGroup = "testGroup"
    presenter.setUnsavedGroupForTesting(testGroup)
    `when`(view.showAskToSaveFilterDialog(testGroup)).thenReturn(LogViewer.UserSelection.CANCELLED)

    presenter.finishing()

    verify(view, never()).showSaveFilters(testGroup)
    verify(mockFiltersRepository, never()).persistGroup(anyOrNull(), anyString())
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

    presenter.setFilteredLogsForTesting(
      arrayOf(
        LogEntry(
          "10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log",
          LogLevel.INFO,
          timestamp
        ),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp)
      )
    )

    val filters = listOf(Filter("name", "ABCDeF", Color.black))
    val filter = filters.first()
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(mapOf("Test" to filters))

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

    presenter.setFilteredLogsForTesting(
      arrayOf(
        LogEntry(
          "10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log",
          LogLevel.INFO,
          timestamp
        ),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : AB log Test Log", LogLevel.INFO, timestamp),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABDeF log Test Log", LogLevel.INFO, timestamp),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp)
      )
    )

    val filters = listOf(Filter("name", "ABCDeF", Color.black))
    val filter = filters.first()
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(mapOf("Test" to filters))

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

    presenter.setFilteredLogsForTesting(
      arrayOf(
        LogEntry(
          "10-12 22:32:50.264  2646  2664 I test  : ABCD log Test Log",
          LogLevel.INFO,
          timestamp
        ),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : AB log Test Log", LogLevel.INFO, timestamp),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABDeF log Test Log", LogLevel.INFO, timestamp),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp)
      )
    )

    val filters = listOf(Filter("name", "ABCDeF", Color.black))
    val filter = filters.first()
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(mapOf("Test" to filters))

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

    presenter.setFilteredLogsForTesting(
      arrayOf(
        LogEntry(
          "10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log",
          LogLevel.INFO,
          timestamp
        ),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp)
      )
    )

    val filters = listOf(Filter("name", "ABCDeF", Color.black))
    val filter = filters.first()
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(mapOf("Test" to filters))

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

    presenter.setFilteredLogsForTesting(
      arrayOf(
        LogEntry(
          "10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log",
          LogLevel.INFO,
          timestamp
        ),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : AB log Test Log", LogLevel.INFO, timestamp),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABDeF log Test Log", LogLevel.INFO, timestamp),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp)
      )
    )

    val filters = listOf(Filter("name", "ABCDeF", Color.black))
    val filter = filters.first()
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(mapOf("Test" to filters))

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

    presenter.setFilteredLogsForTesting(
      arrayOf(
        LogEntry(
          "10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log",
          LogLevel.INFO,
          timestamp
        ),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : AB log Test Log", LogLevel.INFO, timestamp),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABDeF log Test Log", LogLevel.INFO, timestamp),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCD log Test Log", LogLevel.INFO, timestamp)
      )
    )

    val filters = listOf(Filter("name", "ABCDeF", Color.black))
    val filter = filters.first()
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(mapOf("Test" to filters))

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

    presenter.setFilteredLogsForTesting(
      arrayOf(
        LogEntry(
          "10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log",
          LogLevel.INFO,
          timestamp,
          "main"
        ),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : AB log Test Log", LogLevel.INFO, timestamp, "radio"),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "events"),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABDeF log Test Log", LogLevel.INFO, timestamp, "bla"),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCD log Test Log", LogLevel.INFO, timestamp, "main")
      )
    )
    presenter.setAvailableStreamsForTesting(
      setOf(
        LogStream.MAIN,
        LogStream.EVENTS,
        LogStream.RADIO,
        LogStream.SYSTEM,
        LogStream.UNKNOWN
      ), true
    )

    // ArgumentCaptor catches the arguments by reference. Since the argument of showFilteredLogs
    // is mutable and changed internally in the presenter, we need to make copies of the arguments
    // at the time they are called in order to correctly compare them
    val filteredLogsArguments = mutableListOf<List<LogEntry>>()
    @Suppress("UNCHECKED_CAST")
    `when`(view.showFilteredLogs(any())).thenAnswer { filteredLogsArguments.add(ArrayList(it.arguments[0] as List<LogEntry>)) }

    presenter.setStreamAllowed(LogStream.MAIN, false)
    presenter.setStreamAllowed(LogStream.EVENTS, false)
    presenter.setStreamAllowed(LogStream.RADIO, false)
    presenter.setStreamAllowed(LogStream.UNKNOWN, false)

    // 'showFilteredLogs' should be called 4 times (one for each time we called 'setStreamAllowed')
    // So we need to validate the output of all executions
    verify(view, times(4)).showFilteredLogs(anyOrNull())
    assertEquals(4, filteredLogsArguments[0].size)
    assertEquals(3, filteredLogsArguments[1].size)
    assertEquals(2, filteredLogsArguments[2].size)
    assertEquals(1, filteredLogsArguments[3].size)
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

    presenter.setFilteredLogsForTesting(
      arrayOf(
        LogEntry(
          "10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log",
          LogLevel.INFO,
          timestamp,
          "main"
        ),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : AB log Test Log", LogLevel.INFO, timestamp, "radio"),
        LogEntry(
          "10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log",
          LogLevel.INFO,
          timestamp,
          "system"
        ),
        LogEntry(
          "10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log",
          LogLevel.INFO,
          timestamp,
          "events"
        ),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABDeF log Test Log", LogLevel.INFO, timestamp, "bla"),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCD log Test Log", LogLevel.INFO, timestamp, "main")
      )
    )
    presenter.setAvailableStreamsForTesting(
      setOf(
        LogStream.MAIN,
        LogStream.EVENTS,
        LogStream.RADIO,
        LogStream.SYSTEM,
        LogStream.UNKNOWN
      )
    )

    // ArgumentCaptor catches the arguments by reference. Since the argument of showFilteredLogs
    // is mutable and changed internally in the presenter, we need to make copies of the arguments
    // at the time they are called in order to correctly compare them
    val filteredLogsArguments = mutableListOf<List<LogEntry>>()
    @Suppress("UNCHECKED_CAST")
    `when`(view.showFilteredLogs(any())).thenAnswer { filteredLogsArguments.add(ArrayList(it.arguments[0] as List<LogEntry>)) }

    presenter.setStreamAllowed(LogStream.SYSTEM, true)
    presenter.setStreamAllowed(LogStream.MAIN, true)

    // 'showFilteredLogs' should be called 2 times (one for each time we called 'setStreamAllowed')
    // So we need to validate the output of all executions
    verify<LogViewerView>(view, times(2)).showFilteredLogs(anyOrNull())

    assertEquals(1, filteredLogsArguments[0].size)
    assertEquals(3, filteredLogsArguments[1].size)
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

    presenter.setFilteredLogsForTesting(
      arrayOf(
        LogEntry(
          "10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log",
          LogLevel.INFO,
          timestamp,
          "main"
        ),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "radio"),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "events"),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "bla"),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main")
      )
    )

    // This will make the filtered logs shown in UI to have only 3 entries
    // (2 for main and 1 for system)
    presenter.setAvailableStreamsForTesting(
      setOf(
        LogStream.MAIN,
        LogStream.EVENTS,
        LogStream.RADIO,
        LogStream.SYSTEM,
        LogStream.UNKNOWN
      )
    )
    presenter.setStreamAllowed(LogStream.MAIN, true)
    presenter.setStreamAllowed(LogStream.SYSTEM, true)

    val filters = listOf(Filter("name", "ABCDeF", Color.black))
    val filter = filters.first()
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(mapOf("Test" to filters))

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

    presenter.setFilteredLogsForTesting(
      arrayOf(
        LogEntry(
          "10-12 22:32:50.264  2646  2664 I test  : ABeF log Test Log",
          LogLevel.INFO,
          timestamp,
          "main"
        ),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "radio"),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "events"),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "bla"),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main")
      )
    )

    // This will make the filtered logs shown in UI to have only 3 entries
    // (2 for main and 1 for system)
    presenter.setAvailableStreamsForTesting(
      setOf(
        LogStream.MAIN,
        LogStream.EVENTS,
        LogStream.RADIO,
        LogStream.SYSTEM,
        LogStream.UNKNOWN
      )
    )
    presenter.setStreamAllowed(LogStream.MAIN, true)
    presenter.setStreamAllowed(LogStream.SYSTEM, true)

    val filters = listOf(Filter("name", "ABCDeF", Color.black))
    val filter = filters.first()
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(mapOf("Test" to filters))

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

    presenter.setFilteredLogsForTesting(
      arrayOf(
        LogEntry(
          "10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log",
          LogLevel.INFO,
          timestamp,
          "main"
        ),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "radio"),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "events"),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "bla"),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main")
      )
    )

    // This will make the filtered logs shown in UI to have only 3 entries
    // (2 for main and 1 for system)
    presenter.setAvailableStreamsForTesting(
      setOf(
        LogStream.MAIN,
        LogStream.EVENTS,
        LogStream.RADIO,
        LogStream.SYSTEM,
        LogStream.UNKNOWN
      )
    )
    presenter.setStreamAllowed(LogStream.MAIN, true)
    presenter.setStreamAllowed(LogStream.SYSTEM, true)

    val filters = listOf(Filter("name", "ABCDeF", Color.black))
    val filter = filters.first()
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(mapOf("Test" to filters))

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

    presenter.setFilteredLogsForTesting(
      arrayOf(
        LogEntry(
          "10-12 22:32:50.264  2646  2664 I test  : ABeF log Test Log",
          LogLevel.INFO,
          timestamp,
          "main"
        ),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "radio"),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "events"),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "bla"),
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main")
      )
    )

    // This will make the filtered logs shown in UI to have only 3 entries
    // (2 for main and 1 for system)
    presenter.setAvailableStreamsForTesting(
      setOf(
        LogStream.MAIN,
        LogStream.EVENTS,
        LogStream.RADIO,
        LogStream.SYSTEM,
        LogStream.UNKNOWN
      )
    )
    presenter.setStreamAllowed(LogStream.MAIN, true)
    presenter.setStreamAllowed(LogStream.SYSTEM, true)

    val filters = listOf(Filter("name", "ABCDeF", Color.black))
    val filter = filters.first()
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(mapOf("Test" to filters))

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
  fun testRemoveGroupNoFiltersApplied() { // TODO validate more conditions
    val groupToRemove = "removeGroup"
    val testGroup = "testGroup"

    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(LogEntry("Log line 1", LogLevel.DEBUG, null))
    )
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(
      mapOf(
        testGroup to listOf(Filter.createFromString(TEST_SERIALIZED_FILTER)),
        groupToRemove to listOf(
          Filter.createFromString(TEST_SERIALIZED_FILTER2),
          Filter.createFromString(TEST_SERIALIZED_FILTER3)
        )
      )
    )

    presenter.removeGroup(groupToRemove)

    verify(view, never()).showAskToSaveFilterDialog(anyOrNull())
    verify(view).configureFiltersList(any())
    verify(mockFiltersRepository).deleteGroup(groupToRemove)
    verify(view).configureFiltersList(anyOrNull())

    // Verify filter was re-applied after group is removed
    assertEquals(0, presenter.testStats.applyFiltersCallCount)
    verify(view, never()).showFilteredLogs(any())
  }

  @Test
  fun testRemoveGroupOneFilterApplied() { // TODO validate more conditions
    val groupToRemove = "removeGroup"
    val testGroup = "testGroup"
    val appliedFilter = Filter.createFromString(TEST_SERIALIZED_FILTER2)
    appliedFilter.isApplied = true

    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(LogEntry("Log line 1", LogLevel.DEBUG, null))
    )
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(
      mapOf(
        testGroup to listOf(Filter.createFromString(TEST_SERIALIZED_FILTER)),
        groupToRemove to listOf(
          appliedFilter,
          Filter.createFromString(TEST_SERIALIZED_FILTER3)
        )
      )
    )

    presenter.removeGroup(groupToRemove)

    verify(view, never()).showAskToSaveFilterDialog(anyOrNull())
    verify(view).configureFiltersList(any())
    verify(mockFiltersRepository).deleteGroup(groupToRemove)
    verify(view).configureFiltersList(anyOrNull())

    // Verify filter was re-applied after group is removed
    assertEquals(1, presenter.testStats.applyFiltersCallCount)
    verify(view).showFilteredLogs(any())
  }

  @Test
  fun testRemoveGroupUnsaved() { // TODO validate more conditions
    val groupToRemove = "removeGroup"
    val testGroup = "testGroup"

    `when`(view.showAskToSaveFilterDialog(groupToRemove)).thenReturn(LogViewer.UserSelection.CONFIRMED)
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(
      mapOf(
        testGroup to listOf(Filter.createFromString(TEST_SERIALIZED_FILTER)),
        groupToRemove to listOf(
          Filter.createFromString(TEST_SERIALIZED_FILTER2), Filter.createFromString(
            TEST_SERIALIZED_FILTER3
          )
        )
      )
    )
    presenter.setUnsavedGroupForTesting(groupToRemove)

    presenter.removeGroup(groupToRemove)

    verify(view).showAskToSaveFilterDialog(groupToRemove)
    verify(view).configureFiltersList(anyOrNull())
  }

  @Test
  fun testRemoveGroupUnsavedCancelled() { // TODO validate more conditions
    val groupToRemove = "removeGroup"
    val testGroup = "testGroup"

    `when`(view.showAskToSaveFilterDialog(groupToRemove)).thenReturn(LogViewer.UserSelection.CANCELLED)
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(
      mapOf(
        testGroup to listOf(Filter.createFromString(TEST_SERIALIZED_FILTER)),
        groupToRemove to listOf(
          Filter.createFromString(TEST_SERIALIZED_FILTER2), Filter.createFromString(
            TEST_SERIALIZED_FILTER3
          )
        )
      )
    )
    presenter.setUnsavedGroupForTesting(groupToRemove)

    presenter.removeGroup(groupToRemove)

    verify(view).showAskToSaveFilterDialog(groupToRemove)
    verify(view, never()).configureFiltersList(anyOrNull())
  }

  @Test
  fun testRemoveGroupInvalid() {
    val testGroup = "testGroup"
    val testGroup2 = "testGroup2"

    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(
      mapOf(
        testGroup to listOf(Filter.createFromString(TEST_SERIALIZED_FILTER)),
        testGroup2 to listOf(
          Filter.createFromString(TEST_SERIALIZED_FILTER2), Filter.createFromString(
            TEST_SERIALIZED_FILTER3
          )
        )
      )
    )

    presenter.removeGroup("invalidGroup")

    verify(view, never()).showAskToSaveFilterDialog(any())
    verify(view, never()).configureFiltersList(any())
  }

  @Test
  fun testRemoveGroupEmpty() {
    val testGroup = "testGroup"
    val testGroup2 = "testGroup2"

    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(
      mapOf(
        testGroup to listOf(Filter.createFromString(TEST_SERIALIZED_FILTER)),
        testGroup2 to listOf(
          Filter.createFromString(TEST_SERIALIZED_FILTER2), Filter.createFromString(
            TEST_SERIALIZED_FILTER3
          )
        )
      )
    )

    presenter.removeGroup("")

    verify(view, never()).showAskToSaveFilterDialog(any())
    verify(view, never()).configureFiltersList(any())
  }

  @Test
  fun testRemoveGroupNull() {
    val testGroup = "testGroup"
    val testGroup2 = "testGroup2"

    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(
      mapOf(
        testGroup to listOf(Filter.createFromString(TEST_SERIALIZED_FILTER)),
        testGroup2 to listOf(
          Filter.createFromString(TEST_SERIALIZED_FILTER2), Filter.createFromString(
            TEST_SERIALIZED_FILTER3
          )
        )
      )
    )

    presenter.removeGroup(null)

    verify(view, never()).showAskToSaveFilterDialog(any())
    verify(view, never()).configureFiltersList(any())
  }

  @Test
  fun testFilterEditedNoReapply() {
    val filter = Filter.createFromString(TEST_SERIALIZED_FILTER)
    `when`(mockPrefs.reapplyFiltersAfterEdit).thenReturn(false)
    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(LogEntry("Log line 1", LogLevel.DEBUG, null))
    )

    presenter.filterEdited(filter)

    assertFalse(filter.isApplied)
    assertEquals(0, presenter.testStats.applyFiltersCallCount)
    verify(view, never()).showFilteredLogs(any())
  }

  @Test
  fun testFilterEditedReapply() {
    val filter = Filter.createFromString(TEST_SERIALIZED_FILTER)
    `when`(mockPrefs.reapplyFiltersAfterEdit).thenReturn(true)
    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(LogEntry("Log line 1", LogLevel.DEBUG, null))
    )

    presenter.filterEdited(filter)

    assertTrue(filter.isApplied)
    assertEquals(1, presenter.testStats.applyFiltersCallCount)
    verify(view).showFilteredLogs(any())
  }

  @Test
  fun testLoadLogs() {
    val inputLogFiles = arrayOf(File("test"))

    `when`(mockLogsRepository.currentlyOpenedLogFiles).thenReturn(inputLogFiles.toList())
    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(LogEntry("Log line 1", LogLevel.DEBUG, null))
    )

    presenter.loadLogs(inputLogFiles)

    verify(mockLogsRepository).openLogFiles(eqOrNull(inputLogFiles), anyOrNull())
    verify(view).showFilteredLogs(any())
    verify(view).showLogs(any())
    verify(view).showAvailableLogStreams(any())
    verify(view).showCurrentLogsLocation(notNull())
    verify(view, never()).showErrorMessage(any())
    assertEquals(0, presenter.testStats.applyFiltersCallCount)
  }

  @Test
  fun testLoadLogsNoFile() {
    presenter.loadLogs(emptyArray())

    verify(view).showFilteredLogs(emptyList())
    verify(view).showLogs(emptyList())
    verify(view).showAvailableLogStreams(emptySet())
    verify(view).showCurrentLogsLocation(isNull())
    verify(view).showErrorMessage("No logs found")

    assertEquals(0, presenter.testStats.applyFiltersCallCount)
  }

  @Test
  fun testLoadLogsInvalidFile() {
    `when`(mockLogsRepository.openLogFiles(anyOrNull(), anyOrNull())).thenThrow(
      OpenLogsException(
        "test invalid file message",
        Exception()
      )
    )
    presenter.loadLogs(arrayOf(File("invalid_path")))

    verify(view, never()).showFilteredLogs(any())
    verify(view, never()).showLogs(any())
    verify(view, never()).showAvailableLogStreams(any())
    verify(view, never()).showCurrentLogsLocation(notNull())
    verify(view).showErrorMessage("test invalid file message")

    assertEquals(0, presenter.testStats.applyFiltersCallCount)
  }

  @Test
  fun testRefreshLogs() {
    `when`(mockLogsRepository.currentlyOpenedLogFiles).thenReturn(listOf(File("test")))
    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(LogEntry("Log line 1", LogLevel.DEBUG, null))
    )
    presenter.refreshLogs()

    verify(view).showLogs(any())
    verify(view, never()).showErrorMessage(any())
  }

  @Test
  fun testRefreshLogsNoLogsLoaded() {
    presenter.refreshLogs()

    verify(view, never()).showLogs(any())
    verify(view).showErrorMessage("No logs to be refreshed")
  }

  @Test
  fun testSaveFiltersNonExistentGroupNoSave() {
    `when`(view.showSaveFilters(anyString())).thenReturn(null)
    presenter.saveFilters("newGroup")

    verify(view).showSaveFilters("newGroup")
    verify(mockFiltersRepository, never()).persistGroup(anyOrNull(), anyOrNull())
  }

  @Test
  fun testSaveFiltersNonExistentGroupSave() {
    val testFile = File("test")
    `when`(view.showSaveFilters(anyString())).thenReturn(testFile)
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(
      mapOf("newGroup" to listOf(Filter.createFromString(TEST_SERIALIZED_FILTER)))
    )
    `when`(mockFiltersRepository.getChangedGroupsSinceLastOpened()).thenReturn(emptyList())
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(mapOf("newGroup" to emptyList()))

    presenter.saveFilters("newGroup")

    verify(view).showSaveFilters("newGroup")
    verify(mockFiltersRepository).persistGroup(testFile, "newGroup")
    verify(view).hideUnsavedFilterIndication("newGroup")
    verify(view, never()).showErrorMessage(any())
  }

  @Test
  fun testSaveFiltersFail() {
    val tempFile = File("\u0000")

    `when`(
      mockFiltersRepository.persistGroup(
        anyOrNull(),
        anyString()
      )
    ).thenThrow(PersistFiltersException("test error message", Exception()))
    `when`(view.showSaveFilters(anyString())).thenReturn(tempFile)
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(
      mapOf("newGroup" to listOf(Filter.createFromString(TEST_SERIALIZED_FILTER)))
    )
    presenter.saveFilters("newGroup")

    verify(view).showSaveFilters("newGroup")
    verify(view, never()).hideUnsavedFilterIndication("newGroup")
    verify(view).showErrorMessage("test error message")
  }

  @Test
  fun testAddEmptyGroup() {
    val addedGroup = presenter.addGroup("")

    assertNull(addedGroup)
    verify(view, never()).configureFiltersList(anyOrNull())
    verify(mockFiltersRepository, never()).addGroup(anyString())
  }

  @Test
  fun testAddNullGroup() {
    val addedGroup = presenter.addGroup(null)

    assertNull(addedGroup)
    verify(view, never()).configureFiltersList(anyOrNull())
    verify(mockFiltersRepository, never()).addGroup(anyString())
  }

  @Test
  fun testAddNewGroup() {
    `when`(mockFiltersRepository.addGroup("newGroup")).thenReturn("newGroup")
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(mapOf("newGroup" to emptyList()))

    val addedGroup = presenter.addGroup("newGroup")

    assertEquals("newGroup", addedGroup)
    verify(view).configureFiltersList(any())
    verify(mockFiltersRepository).addGroup("newGroup")
  }

  @Test
  fun testAddExistingGroup() {
    `when`(mockFiltersRepository.addGroup("existingGroup")).thenReturn("existingGroup1")
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(
      mapOf("existingGroup" to listOf())
    )

    val addedGroup = presenter.addGroup("existingGroup")

    assertEquals("existingGroup1", addedGroup)
    verify(view).configureFiltersList(any())
    verify(mockFiltersRepository).addGroup("existingGroup")
  }

  @Test
  fun testAddExistingGroup2() {
    `when`(mockFiltersRepository.addGroup("existingGroup")).thenReturn("existingGroup2")
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(
      mapOf(
        "existingGroup" to listOf(),
        "existingGroup1" to listOf()
      )
    )

    val addedGroup = presenter.addGroup("existingGroup")

    assertEquals("existingGroup2", addedGroup)
    verify(view).configureFiltersList(any())
    verify(mockFiltersRepository).addGroup("existingGroup")
  }

  @Test
  fun testSaveFilteredLogs() {
    val tempFile = File.createTempFile("temp", ".tmp")
    presenter.setFilteredLogsForTesting(arrayOf(LogEntry("Log line 1", LogLevel.DEBUG, null)))

    presenter.saveFilteredLogs(tempFile)

    assertTrue(tempFile.readBytes().isNotEmpty())
    tempFile.delete()
  }

  @Test
  fun testSaveFilteredLogsNoFilteredLogs() {
    val tempFile = File.createTempFile("temp", ".tmp")

    presenter.saveFilteredLogs(tempFile)

    assertTrue(tempFile.readBytes().isEmpty())
    tempFile.delete()
  }

  @Test
  fun testSaveFilteredLogsFail() {
    // Create an invalid path name to force an exception
    val mockFile = File("\u0000")
    presenter.setFilteredLogsForTesting(arrayOf(LogEntry("Log line 1", LogLevel.DEBUG, null)))

    presenter.saveFilteredLogs(mockFile)
    verify(view).showErrorMessage(any())
  }

  companion object {
    private const val TEST_SERIALIZED_FILTER = "Test,VGVzdA==,2,255:0:0"
    private const val TEST_SERIALIZED_FILTER2 = "Test2,VGVzdA==,2,255:0:0"
    private const val TEST_SERIALIZED_FILTER3 = "Test3,VGVzdA==,2,255:0:0"
  }
}
