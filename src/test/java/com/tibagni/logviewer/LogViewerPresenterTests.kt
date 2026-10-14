package com.tibagni.logviewer

import com.tibagni.logviewer.filter.DuplicateCluster
import com.tibagni.logviewer.filter.Filter
import com.tibagni.logviewer.filter.FilterDuplicateUtils
import com.tibagni.logviewer.filter.FilterMatch
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
import java.nio.charset.StandardCharsets

class LogViewerPresenterTests {
  @Mock
  private lateinit var mockPrefs: LogViewerPreferences

  @Mock
  private lateinit var view: LogViewerPresenterView

  @Mock
  private lateinit var mockLogsRepository: LogsRepository

  @Mock
  private lateinit var mockFiltersRepository: FiltersRepository

  @Mock
  private lateinit var mockMyLogsRepository: MyLogsRepository

  private lateinit var presenter: LogViewerPresenterImpl
  private var tempFilterFile: File? = null
  private var tempLogFile: File? = null

  /**
   * Returns Mockito.any() as nullable type to avoid java.lang.IllegalStateException when
   * null is returned.
   */
  private fun <T> anyOrNull(): T = Mockito.any<T>()
  private fun <T> eqOrNull(value: T): T = eq<T>(value)
  private fun <T : Any> anyNonNull(default: T): T {
    Mockito.any<T>()
    return default
  }
  private fun <T : Any> eqNonNull(value: T): T {
    Mockito.eq(value)
    return value
  }
  private fun <T : Any> captureNonNull(captor: org.mockito.ArgumentCaptor<T>, default: T): T {
    captor.capture()
    return default
  }

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    `when`(mockFiltersRepository.appliedFilters).thenAnswer {
      mockFiltersRepository.currentlyOpenedFilters?.values?.flatten()?.filter { it.isApplied } ?: emptyList<Filter>()
    }
    presenter = LogViewerPresenterImpl(
      view,
      mockPrefs,
      mockLogsRepository,
      mockMyLogsRepository,
      mockFiltersRepository
    )
    presenter.setBgExecutorService(MockExecutorService())
    presenter.setUiExecutor { it.run() }
  }

  @After
  fun tearDown() {
    tempFilterFile?.delete()
    tempLogFile?.delete()
  }

  @Test
  fun testFinishing() {
    presenter.finishing()
    verify(view).finish()
  }

  @Test
  fun testInitRegistersFiltersListener() {
    presenter.init()
    verify(mockFiltersRepository).addListener(presenter)
  }

  @Test
  fun testOnFiltersAppliedTriggersApplyFilters() {
    presenter.onFiltersApplied(emptyList())
    assertEquals(1, presenter.testStats.applyFiltersCallCount)
  }

  @Test
  fun testNavigateNextNoLogs() {
    val i = presenter.getNextFilteredLogForFilter(Filter.createFromString(TEST_SERIALIZED_FILTER), 0)

    assertEquals(-1, i)
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

    val filters = listOf(Filter("name", "ABCDeF", Color.black, LogLevel.VERBOSE))
    val filter = filters.first()
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(mapOf("Test" to filters))

    // Set 'unknown' stream allowed as we just want to test navigation (and default stream is unknown)
    presenter.setAvailableStreamsForTesting(setOf(LogStream.UNKNOWN))
    presenter.setStreamAllowed(LogStream.UNKNOWN, true)

    var actual = presenter.getNextFilteredLogForFilter(filter, -1)
    assertEquals(0, actual)
    verify(view, never()).showNavigationNextOver()

    actual = presenter.getNextFilteredLogForFilter(filter, 0)
    assertEquals(1, actual)
    verify(view, never()).showNavigationNextOver()

    actual = presenter.getNextFilteredLogForFilter(filter, 1)
    assertEquals(2, actual)
    verify(view, never()).showNavigationNextOver()

    actual = presenter.getNextFilteredLogForFilter(filter, 2)
    assertEquals(0, actual)
    verify(view, times(1)).showNavigationNextOver()
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

    val filters = listOf(Filter("name", "ABCDeF", Color.black, LogLevel.VERBOSE))
    val filter = filters.first()
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(mapOf("Test" to filters))

    // Set 'unknown' stream allowed as we just want to test navigation (and default stream is unknown)
    presenter.setAvailableStreamsForTesting(setOf(LogStream.UNKNOWN))
    presenter.setStreamAllowed(LogStream.UNKNOWN, true)

    var actual = presenter.getNextFilteredLogForFilter(filter, -1)
    assertEquals(0, actual)
    verify(view, never()).showNavigationNextOver()

    actual = presenter.getNextFilteredLogForFilter(filter, 0)
    assertEquals(2, actual)
    verify(view, never()).showNavigationNextOver()

    actual = presenter.getNextFilteredLogForFilter(filter, 2)
    assertEquals(3, actual)
    verify(view, never()).showNavigationNextOver()

    actual = presenter.getNextFilteredLogForFilter(filter, 3)
    assertEquals(5, actual)
    verify(view, never()).showNavigationNextOver()

    actual = presenter.getNextFilteredLogForFilter(filter, 5)
    assertEquals(0, actual)
    verify(view, times(1)).showNavigationNextOver()
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

    val filters = listOf(Filter("name", "ABCDeF", Color.black, LogLevel.VERBOSE))
    val filter = filters.first()
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(mapOf("Test" to filters))

    // Set 'unknown' stream allowed as we just want to test navigation (and default stream is unknown)
    presenter.setAvailableStreamsForTesting(setOf(LogStream.UNKNOWN))
    presenter.setStreamAllowed(LogStream.UNKNOWN, true)

    var actual = presenter.getNextFilteredLogForFilter(filter, -1)
    assertEquals(2, actual)
    verify(view, never()).showNavigationNextOver()

    actual = presenter.getNextFilteredLogForFilter(filter, 2)
    assertEquals(3, actual)
    verify(view, never()).showNavigationNextOver()

    actual = presenter.getNextFilteredLogForFilter(filter, 3)
    assertEquals(5, actual)
    verify(view, never()).showNavigationNextOver()

    actual = presenter.getNextFilteredLogForFilter(filter, 5)
    assertEquals(2, actual)
    verify(view, times(1)).showNavigationNextOver()
  }

  @Test
  fun testNavigatePrevNoLogs() {
    val i = presenter.getPrevFilteredLogForFilter(Filter.createFromString(TEST_SERIALIZED_FILTER), 0)

    assertEquals(-1, i)
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

    val filters = listOf(Filter("name", "ABCDeF", Color.black, LogLevel.VERBOSE))
    val filter = filters.first()
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(mapOf("Test" to filters))

    // Set 'unknown' stream allowed as we just want to test navigation (and default stream is unknown)
    presenter.setAvailableStreamsForTesting(setOf(LogStream.UNKNOWN))
    presenter.setStreamAllowed(LogStream.UNKNOWN, true)

    var actual = presenter.getPrevFilteredLogForFilter(filter, -1)
    assertEquals(2, actual)
    verify(view, never()).showNavigationPrevOver()

    actual = presenter.getPrevFilteredLogForFilter(filter, 2)
    assertEquals(1, actual)
    verify(view, never()).showNavigationPrevOver()

    actual = presenter.getPrevFilteredLogForFilter(filter, 1)
    assertEquals(0, actual)
    verify(view, never()).showNavigationPrevOver()

    actual = presenter.getPrevFilteredLogForFilter(filter, 0)
    assertEquals(2, actual)
    verify(view, times(1)).showNavigationPrevOver()
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

    val filters = listOf(Filter("name", "ABCDeF", Color.black, LogLevel.VERBOSE))
    val filter = filters.first()
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(mapOf("Test" to filters))

    // Set 'unknown' stream allowed as we just want to test navigation (and default stream is unknown)
    presenter.setAvailableStreamsForTesting(setOf(LogStream.UNKNOWN))
    presenter.setStreamAllowed(LogStream.UNKNOWN, true)

    var actual = presenter.getPrevFilteredLogForFilter(filter, -1)
    assertEquals(5, actual)
    verify(view, never()).showNavigationPrevOver()

    actual = presenter.getPrevFilteredLogForFilter(filter, 5)
    assertEquals(3, actual)
    verify(view, never()).showNavigationPrevOver()

    actual = presenter.getPrevFilteredLogForFilter(filter, 3)
    assertEquals(2, actual)
    verify(view, never()).showNavigationPrevOver()

    actual = presenter.getPrevFilteredLogForFilter(filter, 2)
    assertEquals(0, actual)
    verify(view, never()).showNavigationPrevOver()

    actual = presenter.getPrevFilteredLogForFilter(filter, 0)
    assertEquals(5, actual)
    verify(view, times(1)).showNavigationPrevOver()
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

    val filters = listOf(Filter("name", "ABCDeF", Color.black, LogLevel.VERBOSE))
    val filter = filters.first()
    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(mapOf("Test" to filters))

    // Set 'unknown' stream allowed as we just want to test navigation (and default stream is unknown)
    presenter.setAvailableStreamsForTesting(setOf(LogStream.UNKNOWN))
    presenter.setStreamAllowed(LogStream.UNKNOWN, true)

    var actual = presenter.getPrevFilteredLogForFilter(filter, -1)
    assertEquals(3, actual)
    verify(view, never()).showNavigationPrevOver()

    actual = presenter.getPrevFilteredLogForFilter(filter, 3)
    assertEquals(2, actual)
    verify(view, never()).showNavigationPrevOver()

    actual = presenter.getPrevFilteredLogForFilter(filter, 2)
    assertEquals(0, actual)
    verify(view, never()).showNavigationPrevOver()

    actual = presenter.getPrevFilteredLogForFilter(filter, 0)
    assertEquals(3, actual)
    verify(view, times(1)).showNavigationPrevOver()
  }

  @Test
  fun testGotoTimestampInvalidTimestamp() {
    presenter.goToTimestamp("invalid")

    verify(view).showInvalidTimestampSearchError("invalid")
    verify(view, never()).showLogLocationAtSearchedTimestamp(anyInt(), anyInt())
  }

  @Test
  fun testGotoTimestampInvalidTimestamp2() {
    presenter.goToTimestamp("12-3-3")

    verify(view).showInvalidTimestampSearchError("12-3-3")
    verify(view, never()).showLogLocationAtSearchedTimestamp(anyInt(), anyInt())
  }

  @Test
  fun testGotoTimestampInvalidTimestamp3() {
    presenter.goToTimestamp("12-")

    verify(view).showInvalidTimestampSearchError("12-")
    verify(view, never()).showLogLocationAtSearchedTimestamp(anyInt(), anyInt())
  }

  @Test
  fun testGotoTimestampInvalidTimestamp4() {
    presenter.goToTimestamp("12")

    verify(view).showInvalidTimestampSearchError("12")
    verify(view, never()).showLogLocationAtSearchedTimestamp(anyInt(), anyInt())
  }

  @Test
  fun testGotoTimestampExactTimestamp() {
    val timestamps = listOf(
      LogTimestamp(10, 12, 22, 32, 50, 264),
      LogTimestamp(10, 12, 22, 32, 50, 321),
      LogTimestamp(10, 12, 22, 32, 50, 472),
      LogTimestamp(10, 12, 22, 32, 50, 801),
      LogTimestamp(10, 12, 22, 32, 51, 32),
      LogTimestamp(10, 12, 22, 32, 52, 1),
      LogTimestamp(10, 12, 22, 32, 52, 120),
      LogTimestamp(10, 12, 22, 32, 52, 200),
      LogTimestamp(10, 12, 22, 32, 53, 0)
    )

    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : Log1", LogLevel.INFO, timestamps[0]),
        LogEntry("10-12 22:32:50.321  2646  2664 I test  : Log2", LogLevel.INFO, timestamps[1]),
        LogEntry("10-12 22:32:50.472  2646  2664 I test  : Log3", LogLevel.INFO, timestamps[2]),
        LogEntry("10-12 22:32:50.801  2646  2664 I test  : Log4", LogLevel.INFO, timestamps[3]),
        LogEntry("10-12 22:32:51.032  2646  2664 I test  : Log5", LogLevel.INFO, timestamps[4]),
        LogEntry("10-12 22:32:52.001  2646  2664 I test  : Log6", LogLevel.INFO, timestamps[5]),
        LogEntry("10-12 22:32:52.120  2646  2664 I test  : Log7", LogLevel.INFO, timestamps[6]),
        LogEntry("10-12 22:32:52.200  2646  2664 I test  : Log8", LogLevel.INFO, timestamps[7]),
        LogEntry("10-12 22:32:53.000  2646  2664 I test  : Log9", LogLevel.INFO, timestamps[8])
      )
    )

    presenter.setFilteredLogsForTesting(
      arrayOf(
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : Log1", LogLevel.INFO, timestamps[0]),
        LogEntry("10-12 22:32:50.321  2646  2664 I test  : Log2", LogLevel.INFO, timestamps[1]),
        LogEntry("10-12 22:32:50.472  2646  2664 I test  : Log3", LogLevel.INFO, timestamps[2]),
        LogEntry("10-12 22:32:51.032  2646  2664 I test  : Log5", LogLevel.INFO, timestamps[4]),
        LogEntry("10-12 22:32:53.000  2646  2664 I test  : Log9", LogLevel.INFO, timestamps[8])
      ),
      true // set cached filtered logs as well
    )

    presenter.goToTimestamp("10-12 22:32:51.032")

    verify(view, never()).showInvalidTimestampSearchError(anyString())
    verify(view).showLogLocationAtSearchedTimestamp(4, 3)
  }

  @Test
  fun testGotoTimestampInexactTimestamp() {
    val timestamps = listOf(
      LogTimestamp(10, 12, 22, 32, 50, 264),
      LogTimestamp(10, 12, 22, 32, 50, 321),
      LogTimestamp(10, 12, 22, 32, 50, 472),
      LogTimestamp(10, 12, 22, 32, 50, 801),
      LogTimestamp(10, 12, 22, 32, 51, 32),
      LogTimestamp(10, 12, 22, 32, 52, 1),
      LogTimestamp(10, 12, 22, 32, 52, 120),
      LogTimestamp(10, 12, 22, 32, 52, 200),
      LogTimestamp(10, 12, 22, 32, 53, 0)
    )

    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : Log1", LogLevel.INFO, timestamps[0]),
        LogEntry("10-12 22:32:50.321  2646  2664 I test  : Log2", LogLevel.INFO, timestamps[1]),
        LogEntry("10-12 22:32:50.472  2646  2664 I test  : Log3", LogLevel.INFO, timestamps[2]),
        LogEntry("10-12 22:32:50.801  2646  2664 I test  : Log4", LogLevel.INFO, timestamps[3]),
        LogEntry("10-12 22:32:51.032  2646  2664 I test  : Log5", LogLevel.INFO, timestamps[4]),
        LogEntry("10-12 22:32:52.001  2646  2664 I test  : Log6", LogLevel.INFO, timestamps[5]),
        LogEntry("10-12 22:32:52.120  2646  2664 I test  : Log7", LogLevel.INFO, timestamps[6]),
        LogEntry("10-12 22:32:52.200  2646  2664 I test  : Log8", LogLevel.INFO, timestamps[7]),
        LogEntry("10-12 22:32:53.000  2646  2664 I test  : Log9", LogLevel.INFO, timestamps[8])
      )
    )

    presenter.setFilteredLogsForTesting(
      arrayOf(
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : Log1", LogLevel.INFO, timestamps[0]),
        LogEntry("10-12 22:32:50.321  2646  2664 I test  : Log2", LogLevel.INFO, timestamps[1]),
        LogEntry("10-12 22:32:50.472  2646  2664 I test  : Log3", LogLevel.INFO, timestamps[2]),
        LogEntry("10-12 22:32:51.032  2646  2664 I test  : Log5", LogLevel.INFO, timestamps[4]),
        LogEntry("10-12 22:32:53.000  2646  2664 I test  : Log9", LogLevel.INFO, timestamps[8])
      ),
      true // set cached filtered logs as well
    )

    presenter.goToTimestamp("10-12 22:32:51.132")

    verify(view, never()).showInvalidTimestampSearchError(anyString())
    verify(view).showLogLocationAtSearchedTimestamp(4, 3)
  }

  @Test
  fun testGotoTimestampInexactTimestampBeforeAll() {
    val timestamps = listOf(
      LogTimestamp(10, 12, 22, 32, 50, 264),
      LogTimestamp(10, 12, 22, 32, 50, 321),
      LogTimestamp(10, 12, 22, 32, 50, 472),
      LogTimestamp(10, 12, 22, 32, 50, 801),
      LogTimestamp(10, 12, 22, 32, 51, 32),
      LogTimestamp(10, 12, 22, 32, 52, 1),
      LogTimestamp(10, 12, 22, 32, 52, 120),
      LogTimestamp(10, 12, 22, 32, 52, 200),
      LogTimestamp(10, 12, 22, 32, 53, 0)
    )

    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : Log1", LogLevel.INFO, timestamps[0]),
        LogEntry("10-12 22:32:50.321  2646  2664 I test  : Log2", LogLevel.INFO, timestamps[1]),
        LogEntry("10-12 22:32:50.472  2646  2664 I test  : Log3", LogLevel.INFO, timestamps[2]),
        LogEntry("10-12 22:32:50.801  2646  2664 I test  : Log4", LogLevel.INFO, timestamps[3]),
        LogEntry("10-12 22:32:51.032  2646  2664 I test  : Log5", LogLevel.INFO, timestamps[4]),
        LogEntry("10-12 22:32:52.001  2646  2664 I test  : Log6", LogLevel.INFO, timestamps[5]),
        LogEntry("10-12 22:32:52.120  2646  2664 I test  : Log7", LogLevel.INFO, timestamps[6]),
        LogEntry("10-12 22:32:52.200  2646  2664 I test  : Log8", LogLevel.INFO, timestamps[7]),
        LogEntry("10-12 22:32:53.000  2646  2664 I test  : Log9", LogLevel.INFO, timestamps[8])
      )
    )

    presenter.setFilteredLogsForTesting(
      arrayOf(
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : Log1", LogLevel.INFO, timestamps[0]),
        LogEntry("10-12 22:32:50.321  2646  2664 I test  : Log2", LogLevel.INFO, timestamps[1]),
        LogEntry("10-12 22:32:50.472  2646  2664 I test  : Log3", LogLevel.INFO, timestamps[2]),
        LogEntry("10-12 22:32:51.032  2646  2664 I test  : Log5", LogLevel.INFO, timestamps[4]),
        LogEntry("10-12 22:32:53.000  2646  2664 I test  : Log9", LogLevel.INFO, timestamps[8])
      ),
      true // set cached filtered logs as well
    )

    presenter.goToTimestamp("10-12 22:32:49.132")

    verify(view, never()).showInvalidTimestampSearchError(anyString())
    verify(view).showLogLocationAtSearchedTimestamp(0, 0)
  }

  @Test
  fun testGotoTimestampInexactTimestampAfterAll() {
    val timestamps = listOf(
      LogTimestamp(10, 12, 22, 32, 50, 264),
      LogTimestamp(10, 12, 22, 32, 50, 321),
      LogTimestamp(10, 12, 22, 32, 50, 472),
      LogTimestamp(10, 12, 22, 32, 50, 801),
      LogTimestamp(10, 12, 22, 32, 51, 32),
      LogTimestamp(10, 12, 22, 32, 52, 1),
      LogTimestamp(10, 12, 22, 32, 52, 120),
      LogTimestamp(10, 12, 22, 32, 52, 200),
      LogTimestamp(10, 12, 22, 32, 53, 0)
    )

    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : Log1", LogLevel.INFO, timestamps[0]),
        LogEntry("10-12 22:32:50.321  2646  2664 I test  : Log2", LogLevel.INFO, timestamps[1]),
        LogEntry("10-12 22:32:50.472  2646  2664 I test  : Log3", LogLevel.INFO, timestamps[2]),
        LogEntry("10-12 22:32:50.801  2646  2664 I test  : Log4", LogLevel.INFO, timestamps[3]),
        LogEntry("10-12 22:32:51.032  2646  2664 I test  : Log5", LogLevel.INFO, timestamps[4]),
        LogEntry("10-12 22:32:52.001  2646  2664 I test  : Log6", LogLevel.INFO, timestamps[5]),
        LogEntry("10-12 22:32:52.120  2646  2664 I test  : Log7", LogLevel.INFO, timestamps[6]),
        LogEntry("10-12 22:32:52.200  2646  2664 I test  : Log8", LogLevel.INFO, timestamps[7]),
        LogEntry("10-12 22:32:53.000  2646  2664 I test  : Log9", LogLevel.INFO, timestamps[8])
      )
    )

    presenter.setFilteredLogsForTesting(
      arrayOf(
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : Log1", LogLevel.INFO, timestamps[0]),
        LogEntry("10-12 22:32:50.321  2646  2664 I test  : Log2", LogLevel.INFO, timestamps[1]),
        LogEntry("10-12 22:32:50.472  2646  2664 I test  : Log3", LogLevel.INFO, timestamps[2]),
        LogEntry("10-12 22:32:51.032  2646  2664 I test  : Log5", LogLevel.INFO, timestamps[4]),
        LogEntry("10-12 22:32:53.000  2646  2664 I test  : Log9", LogLevel.INFO, timestamps[8])
      ),
      true // set cached filtered logs as well
    )

    presenter.goToTimestamp("10-12 22:32:59.132")

    verify(view, never()).showInvalidTimestampSearchError(anyString())
    verify(view).showLogLocationAtSearchedTimestamp(8, 4)
  }

  @Test
  fun testGotoTimestampNoFiltered() {
    val timestamps = listOf(
      LogTimestamp(10, 12, 22, 32, 50, 264),
      LogTimestamp(10, 12, 22, 32, 50, 321),
      LogTimestamp(10, 12, 22, 32, 50, 472),
      LogTimestamp(10, 12, 22, 32, 50, 801),
      LogTimestamp(10, 12, 22, 32, 51, 32),
      LogTimestamp(10, 12, 22, 32, 52, 1),
      LogTimestamp(10, 12, 22, 32, 52, 120),
      LogTimestamp(10, 12, 22, 32, 52, 200),
      LogTimestamp(10, 12, 22, 32, 53, 0)
    )

    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(
        LogEntry("10-12 22:32:50.264  2646  2664 I test  : Log1", LogLevel.INFO, timestamps[0]),
        LogEntry("10-12 22:32:50.321  2646  2664 I test  : Log2", LogLevel.INFO, timestamps[1]),
        LogEntry("10-12 22:32:50.472  2646  2664 I test  : Log3", LogLevel.INFO, timestamps[2]),
        LogEntry("10-12 22:32:50.801  2646  2664 I test  : Log4", LogLevel.INFO, timestamps[3]),
        LogEntry("10-12 22:32:51.032  2646  2664 I test  : Log5", LogLevel.INFO, timestamps[4]),
        LogEntry("10-12 22:32:52.001  2646  2664 I test  : Log6", LogLevel.INFO, timestamps[5]),
        LogEntry("10-12 22:32:52.120  2646  2664 I test  : Log7", LogLevel.INFO, timestamps[6]),
        LogEntry("10-12 22:32:52.200  2646  2664 I test  : Log8", LogLevel.INFO, timestamps[7]),
        LogEntry("10-12 22:32:53.000  2646  2664 I test  : Log9", LogLevel.INFO, timestamps[8])
      )
    )

    presenter.goToTimestamp("10-12 22:32:51.032")

    verify(view, never()).showInvalidTimestampSearchError(anyString())
    verify(view).showLogLocationAtSearchedTimestamp(4, -1)
  }

  @Test
  fun testGotoTimestampNoLogs() {
    presenter.goToTimestamp("10-12 22:32:51.032")

    verify(view, never()).showInvalidTimestampSearchError(anyString())
    verify(view).showLogLocationAtSearchedTimestamp(-1, -1)
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
    verify(view, times(2)).showFilteredLogs(anyOrNull())

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

    val filters = listOf(Filter("name", "ABCDeF", Color.black, LogLevel.VERBOSE))
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

    val filters = listOf(Filter("name", "ABCDeF", Color.black, LogLevel.VERBOSE))
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

    val filters = listOf(Filter("name", "ABCDeF", Color.black, LogLevel.VERBOSE))
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

    val filters = listOf(Filter("name", "ABCDeF", Color.black, LogLevel.VERBOSE))
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
  fun testLoadLogs() {
    val inputLogFiles = arrayOf(File("test"))

    `when`(mockLogsRepository.currentlyOpenedLogFiles).thenReturn(inputLogFiles.toList())
    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(LogEntry("Log line 1", LogLevel.DEBUG, null))
    )

    presenter.loadLogs(inputLogFiles)

    verify(mockLogsRepository).openLogFiles(eqOrNull(inputLogFiles), anyOrNull(), anyOrNull())
    verify(view).showFilteredLogs(any())
    verify(view).showLogs(any())
    verify(view, never()).showMyLogs(any())
    verify(view).showAvailableLogStreams(any())
    verify(view).showCurrentLogsLocation(notNull())
    verify(view, never()).showErrorMessage(any())
    verify(view, never()).showSkippedLogsMessage(anyOrNull())
    assertEquals(0, presenter.testStats.applyFiltersCallCount)
  }

  @Test
  fun testLoadLogsChangeMyLogs() {
    val inputLogFiles = arrayOf(File("test"))

    `when`(mockLogsRepository.currentlyOpenedLogFiles).thenReturn(inputLogFiles.toList())
    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(LogEntry("Log line 1", LogLevel.DEBUG, null))
    )

    `when`(mockMyLogsRepository.logs).thenReturn(
      listOf(LogEntry("Log line 1", LogLevel.DEBUG, null).also { it.index = 10 })
    )
    `when`(mockLogsRepository.lastVisibleLogIndex).thenReturn(1)
    `when`(mockLogsRepository.getMatchingLogEntry(anyOrNull())).thenReturn(
      LogEntry("Matching Log", LogLevel.DEBUG, null)
    )

    presenter.loadLogs(inputLogFiles)

    verify(mockLogsRepository).openLogFiles(eqOrNull(inputLogFiles), anyOrNull(), anyOrNull())
    verify(view).showFilteredLogs(any())
    verify(view).showLogs(any())
    verify(view).showMyLogs(any())
    verify(view).showAvailableLogStreams(any())
    verify(view).showCurrentLogsLocation(notNull())
    verify(view, never()).showErrorMessage(any())
    verify(view, never()).showSkippedLogsMessage(anyOrNull())
    assertEquals(0, presenter.testStats.applyFiltersCallCount)
  }

  @Test
  fun testLoadLogsNotApplyFilters() {
    val inputLogFiles = arrayOf(File("test"))
    val currentFilters = mapOf("testGroup" to listOf(Filter.createFromString(TEST_SERIALIZED_FILTER)))

    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(currentFilters)
    `when`(mockLogsRepository.currentlyOpenedLogFiles).thenReturn(inputLogFiles.toList())
    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(LogEntry("Log line 1", LogLevel.DEBUG, null))
    )

    presenter.loadLogs(inputLogFiles)

    verify(mockLogsRepository).openLogFiles(eqOrNull(inputLogFiles), anyOrNull(), anyOrNull())
    verify(view).showFilteredLogs(any())
    verify(view).showLogs(any())
    verify(view).showAvailableLogStreams(any())
    verify(view).showCurrentLogsLocation(notNull())
    verify(view, never()).showErrorMessage(any())
    verify(view, never()).showSkippedLogsMessage(anyOrNull())
    assertEquals(0, presenter.testStats.applyFiltersCallCount)
  }

  @Test
  fun testLoadLogsApplyFilters() {
    val inputLogFiles = arrayOf(File("test"))
    val appliedFilter = Filter.createFromString(TEST_SERIALIZED_FILTER)
    appliedFilter.isApplied = true
    val currentFilters = mapOf("testGroup" to listOf(appliedFilter))

    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(currentFilters)
    `when`(mockLogsRepository.currentlyOpenedLogFiles).thenReturn(inputLogFiles.toList())
    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(LogEntry("Log line 1", LogLevel.DEBUG, null))
    )

    presenter.loadLogs(inputLogFiles)

    verify(mockLogsRepository).openLogFiles(eqOrNull(inputLogFiles), anyOrNull(), anyOrNull())

    // It should be called twice: first when the logs are loaded and then when filters are applied
    verify(view, times(2)).showFilteredLogs(any())

    verify(view).showLogs(any())
    verify(view).showAvailableLogStreams(any())
    verify(view).showCurrentLogsLocation(notNull())
    verify(view, never()).showErrorMessage(any())
    verify(view, never()).showSkippedLogsMessage(anyOrNull())
    assertEquals(1, presenter.testStats.applyFiltersCallCount)
  }

  @Test
  fun testLoadLogsApplyFilters2() {
    val inputLogFiles = arrayOf(File("test"))
    val appliedFilter = Filter.createFromString(TEST_SERIALIZED_FILTER)
    appliedFilter.isApplied = true
    val currentFilters = mapOf("testGroup" to listOf(appliedFilter, Filter.createFromString(TEST_SERIALIZED_FILTER2)))

    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(currentFilters)
    `when`(mockLogsRepository.currentlyOpenedLogFiles).thenReturn(inputLogFiles.toList())
    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(LogEntry("Log line 1", LogLevel.DEBUG, null))
    )

    presenter.loadLogs(inputLogFiles)

    verify(mockLogsRepository).openLogFiles(eqOrNull(inputLogFiles), anyOrNull(), anyOrNull())

    // It should be called twice: first when the logs are loaded and then when filters are applied
    verify(view, times(2)).showFilteredLogs(any())

    verify(view).showLogs(any())
    verify(view).showAvailableLogStreams(any())
    verify(view).showCurrentLogsLocation(notNull())
    verify(view, never()).showErrorMessage(any())
    verify(view, never()).showSkippedLogsMessage(anyOrNull())
    assertEquals(1, presenter.testStats.applyFiltersCallCount)
  }

  @Test
  fun testLoadLogsNoFile() {
    presenter.loadLogs(emptyArray())

    verify(view).showFilteredLogs(emptyList())
    verify(view).showLogs(emptyList())
    verify(view).showAvailableLogStreams(emptySet())
    verify(view).showCurrentLogsLocation(isNull())
    verify(view).showErrorMessage("No logs found")
    verify(view, never()).showSkippedLogsMessage(anyOrNull())

    assertEquals(0, presenter.testStats.applyFiltersCallCount)
  }

  @Test
  fun testLoadLogsInvalidFile() {
    `when`(mockLogsRepository.openLogFiles(anyOrNull(), anyOrNull(), anyOrNull())).thenThrow(
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
  fun testLoadLogsValidAndInvalid() {
    /*
     * This test ensures that the "Skipped" files dialog is displayed to the user
     * when one of the log files being loaded is invalid
     */
    val inputLogFiles = arrayOf(File("test"), File("invalid"))

    `when`(mockLogsRepository.currentlyOpenedLogFiles).thenReturn(listOf(inputLogFiles[0]))
    `when`(mockLogsRepository.lastSkippedLogFiles).thenReturn(listOf(inputLogFiles[1].name))
    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(LogEntry("Log line 1", LogLevel.DEBUG, null))
    )

    presenter.loadLogs(inputLogFiles)

    verify(mockLogsRepository).openLogFiles(eqOrNull(inputLogFiles), anyOrNull(), anyOrNull())
    verify(view).showFilteredLogs(any())
    verify(view).showLogs(any())
    verify(view).showAvailableLogStreams(any())
    verify(view).showCurrentLogsLocation(notNull())
    verify(view, never()).showErrorMessage(any())
    verify(view).showSkippedLogsMessage(anyOrNull())
  }

  @Test
  fun testLoadLogsOnlyInvalid() {
    /*
     * This test ensures that the "Skipped" files dialog is NOT displayed to the user
     * when the single loaded file is invalid, and ensures that the "No logs found" dialog is displayed
     * There is no need to display the skipped logs file name when nothing else was loaded
     */
    val inputLogFiles = arrayOf(File("invalid"))

    `when`(mockLogsRepository.currentlyOpenedLogFiles).thenReturn(listOf())
    `when`(mockLogsRepository.lastSkippedLogFiles).thenReturn(listOf(inputLogFiles[0].name))
    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(listOf())

    presenter.loadLogs(inputLogFiles)

    verify(mockLogsRepository).openLogFiles(eqOrNull(inputLogFiles), anyOrNull(), anyOrNull())
    verify(view).showFilteredLogs(any())
    verify(view).showLogs(any())
    verify(view).showAvailableLogStreams(any())
    verify(view, never()).showCurrentLogsLocation(notNull())
    verify(view).showErrorMessage(any())
    verify(view, never()).showSkippedLogsMessage(anyOrNull())
  }

  @Test
  fun testLoadLogsWithBugreport() {
    val inputLogFiles = arrayOf(File("log"), File("bugreport"))

    `when`(mockLogsRepository.currentlyOpenedLogFiles).thenReturn(inputLogFiles.toList())
    `when`(mockLogsRepository.lastSkippedLogFiles).thenReturn(listOf())
    `when`(mockLogsRepository.potentialBugReports).thenReturn(mapOf("bugreport" to "test text"))
    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(LogEntry("Log line 1", LogLevel.DEBUG, null))
    )

    presenter.loadLogs(inputLogFiles)

    verify(mockLogsRepository).openLogFiles(eqOrNull(inputLogFiles), anyOrNull(), anyOrNull())
    verify(view).showFilteredLogs(any())
    verify(view).showLogs(any())
    verify(view).showAvailableLogStreams(any())
    verify(view).showCurrentLogsLocation(notNull())
    verify(view, never()).showErrorMessage(any())
    verify(view, never()).showSkippedLogsMessage(anyOrNull())
    verify(view).showOpenPotentialBugReport("bugreport", "test text")
    verify(view, never()).closeCurrentlyOpenedBugReports()
  }

  @Test
  fun testLoadLogsWithoutBugreport() {
    val inputLogFiles = arrayOf(File("log"))

    `when`(mockLogsRepository.currentlyOpenedLogFiles).thenReturn(inputLogFiles.toList())
    `when`(mockLogsRepository.lastSkippedLogFiles).thenReturn(listOf())
    `when`(mockLogsRepository.potentialBugReports).thenReturn(mapOf())
    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(
      listOf(LogEntry("Log line 1", LogLevel.DEBUG, null))
    )

    presenter.loadLogs(inputLogFiles)

    verify(mockLogsRepository).openLogFiles(eqOrNull(inputLogFiles), anyOrNull(), anyOrNull())
    verify(view).showFilteredLogs(any())
    verify(view).showLogs(any())
    verify(view).showAvailableLogStreams(any())
    verify(view).showCurrentLogsLocation(notNull())
    verify(view, never()).showErrorMessage(any())
    verify(view, never()).showSkippedLogsMessage(anyOrNull())
    verify(view, never()).showOpenPotentialBugReport(anyString(), anyString())
    verify(view).closeCurrentlyOpenedBugReports()
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

  @Test
  fun testIgnoreLogsBefore() {
    `when`(mockLogsRepository.firstVisibleLogIndex).thenReturn(0)
    `when`(mockLogsRepository.lastVisibleLogIndex).thenReturn(100)

    presenter.ignoreLogsBefore(50)

    verify(mockLogsRepository).firstVisibleLogIndex = 50
    verify(view, never()).showErrorMessage(anyString())
    verify(view).showLogs(any())
  }

  @Test
  fun testIgnoreLogsBeforeInvalidIndex() {
    `when`(mockLogsRepository.firstVisibleLogIndex).thenReturn(0)
    `when`(mockLogsRepository.lastVisibleLogIndex).thenReturn(100)

    presenter.ignoreLogsBefore(101)

    verify(mockLogsRepository, never()).firstVisibleLogIndex = anyInt()
    verify(view).showErrorMessage(anyString())
    verify(view, never()).showLogs(any())
  }

  @Test
  fun testIgnoreLogsBeforeSameIndex() {
    `when`(mockLogsRepository.firstVisibleLogIndex).thenReturn(10)
    `when`(mockLogsRepository.lastVisibleLogIndex).thenReturn(100)

    presenter.ignoreLogsBefore(10)

    verify(mockLogsRepository, never()).firstVisibleLogIndex = anyInt()
    verify(view, never()).showErrorMessage(anyString())
    verify(view, never()).showLogs(any())
  }

  @Test
  fun testIgnoreLogsAfter() {
    `when`(mockLogsRepository.firstVisibleLogIndex).thenReturn(0)
    `when`(mockLogsRepository.lastVisibleLogIndex).thenReturn(100)

    presenter.ignoreLogsAfter(50)

    verify(mockLogsRepository).lastVisibleLogIndex = 50
    verify(view, never()).showErrorMessage(anyString())
    verify(view).showLogs(any())
  }

  @Test
  fun testIgnoreLogsAfterInvalidIndex() {
    `when`(mockLogsRepository.firstVisibleLogIndex).thenReturn(10)
    `when`(mockLogsRepository.lastVisibleLogIndex).thenReturn(100)

    presenter.ignoreLogsAfter(9)

    verify(mockLogsRepository, never()).lastVisibleLogIndex = anyInt()
    verify(view).showErrorMessage(anyString())
    verify(view, never()).showLogs(any())
  }

  @Test
  fun testIgnoreLogsAfterSameIndex() {
    `when`(mockLogsRepository.firstVisibleLogIndex).thenReturn(10)
    `when`(mockLogsRepository.lastVisibleLogIndex).thenReturn(100)

    presenter.ignoreLogsAfter(100)

    verify(mockLogsRepository, never()).lastVisibleLogIndex = anyInt()
    verify(view, never()).showErrorMessage(anyString())
    verify(view, never()).showLogs(any())
  }

  @Test
  fun testIgnoreLogsAfterApplyFilters() {
    `when`(mockLogsRepository.firstVisibleLogIndex).thenReturn(10)
    `when`(mockLogsRepository.lastVisibleLogIndex).thenReturn(100)

    presenter.ignoreLogsAfter(100)

    verify(mockLogsRepository, never()).lastVisibleLogIndex = anyInt()
    verify(view, never()).showErrorMessage(anyString())
    verify(view, never()).showLogs(any())
  }

  @Test
  fun testResetIgnoredLogsBoth() {
    presenter.resetIgnoredLogs(true, true)

    verify(mockLogsRepository).firstVisibleLogIndex = -1
    verify(mockLogsRepository).lastVisibleLogIndex = -1
  }

  @Test
  fun testResetIgnoredLogsOnlyBefore() {
    presenter.resetIgnoredLogs(true, false)

    verify(mockLogsRepository).firstVisibleLogIndex = -1
    verify(mockLogsRepository, never()).lastVisibleLogIndex = anyInt()
  }

  @Test
  fun testResetIgnoredLogsOnlyAfter() {
    presenter.resetIgnoredLogs(false, true)

    verify(mockLogsRepository, never()).firstVisibleLogIndex = anyInt()
    verify(mockLogsRepository).lastVisibleLogIndex = -1
  }

  @Test
  fun testResetIgnoredLogsNeither() {
    presenter.resetIgnoredLogs(false, false)

    verify(mockLogsRepository, never()).firstVisibleLogIndex = anyInt()
    verify(mockLogsRepository, never()).lastVisibleLogIndex = anyInt()
  }

  @Test
  fun testVisibleLogsOffset() {
    `when`(mockLogsRepository.firstVisibleLogIndex).thenReturn(30)
    val offset = presenter.visibleLogsOffset

    assertEquals(30, offset)
  }

  @Test
  fun testLastVisibleLog() {
    // This is always the visible logs sublist already, so the return must always be the last log of this list
    val logs = listOf(
      LogEntry("Log line 1", LogLevel.DEBUG, null),
      LogEntry("Log line 2", LogLevel.DEBUG, null),
      LogEntry("Log line 3", LogLevel.DEBUG, null)
    )

    `when`(mockLogsRepository.lastVisibleLogIndex).thenReturn(2)
    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(logs)
    val lastLog = presenter.lastVisibleLog

    assertEquals(logs[2], lastLog)
  }

  @Test
  fun testLastVisibleLogNotSet() {
    val logs = listOf(
      LogEntry("Log line 1", LogLevel.DEBUG, null),
      LogEntry("Log line 2", LogLevel.DEBUG, null),
      LogEntry("Log line 3", LogLevel.DEBUG, null)
    )

    `when`(mockLogsRepository.lastVisibleLogIndex).thenReturn(2)
    `when`(mockLogsRepository.allLogsSize).thenReturn(3)
    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(logs)
    val lastLog = presenter.lastVisibleLog

    assertNull(lastLog)
  }

  @Test
  fun testFirstVisibleLog() {
    // This is always the visible logs sublist already, so the return must always be the first log of this list
    val logs = listOf(
      LogEntry("Log line 1", LogLevel.DEBUG, null),
      LogEntry("Log line 2", LogLevel.DEBUG, null),
      LogEntry("Log line 3", LogLevel.DEBUG, null)
    )

    `when`(mockLogsRepository.firstVisibleLogIndex).thenReturn(1)
    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(logs)
    val firstLog = presenter.firstVisibleLog

    assertEquals(logs[0], firstLog)
  }

  @Test
  fun testFirstVisibleLogNotSet() {
    val logs = listOf(
      LogEntry("Log line 1", LogLevel.DEBUG, null),
      LogEntry("Log line 2", LogLevel.DEBUG, null),
      LogEntry("Log line 3", LogLevel.DEBUG, null)
    )

    `when`(mockLogsRepository.firstVisibleLogIndex).thenReturn(0)
    `when`(mockLogsRepository.allLogsSize).thenReturn(3)
    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(logs)
    val firstLog = presenter.firstVisibleLog

    assertNull(firstLog)
  }

  @Test
  fun testAddToMyLogs() {
    val logs = listOf(
      LogEntry("Log line 1", LogLevel.DEBUG, null),
      LogEntry("Log line 2", LogLevel.DEBUG, null),
      LogEntry("Log line 3", LogLevel.DEBUG, null)
    )
    `when`(mockMyLogsRepository.logs).thenReturn(logs)

    presenter.addLogEntriesToMyLogs(logs)

    verify(mockMyLogsRepository).addLogEntries(logs)
    verify(view).showMyLogs(logs)
    verify(view, never()).showLogs(any())
  }

  @Test
  fun testRemoveOneFromMyLogs() {
    val log1 = LogEntry("Log line 1", LogLevel.DEBUG, null)
    val log2 = LogEntry("Log line 2", LogLevel.DEBUG, null)
    val log3 = LogEntry("Log line 3", LogLevel.DEBUG, null)
    val logs = listOf(log1, log2, log3)

    `when`(mockMyLogsRepository.logs).thenReturn(logs)

    presenter.removeFromMyLog(intArrayOf(0))

    verify(mockMyLogsRepository).removeLogEntries(listOf(log1))
    verify(view).showMyLogs(anyList())
    verify(view, never()).showLogs(any())
  }

  @Test
  fun testRemoveMultipleFromMyLogs() {
    val log1 = LogEntry("Log line 1", LogLevel.DEBUG, null)
    val log2 = LogEntry("Log line 2", LogLevel.DEBUG, null)
    val log3 = LogEntry("Log line 3", LogLevel.DEBUG, null)
    val logs = listOf(log1, log2, log3)

    `when`(mockMyLogsRepository.logs).thenReturn(logs)

    presenter.removeFromMyLog(intArrayOf(0, 2))

    verify(mockMyLogsRepository).removeLogEntries(listOf(log1, log3))
    verify(view).showMyLogs(anyList())
    verify(view, never()).showLogs(any())
  }

  @Test
  fun testRemoveInvalidFromMyLogs() {
    val log1 = LogEntry("Log line 1", LogLevel.DEBUG, null)
    val log2 = LogEntry("Log line 2", LogLevel.DEBUG, null)
    val log3 = LogEntry("Log line 3", LogLevel.DEBUG, null)
    val logs = listOf(log1, log2, log3)

    `when`(mockMyLogsRepository.logs).thenReturn(logs)

    presenter.removeFromMyLog(intArrayOf(4))

    verify(mockMyLogsRepository).removeLogEntries(emptyList())
    verify(view).showMyLogs(any())
    verify(view, never()).showLogs(any())
  }

  @Test
  fun testRemoveInvalidAndValidFromMyLogs() {
    val log1 = LogEntry("Log line 1", LogLevel.DEBUG, null)
    val log2 = LogEntry("Log line 2", LogLevel.DEBUG, null)
    val log3 = LogEntry("Log line 3", LogLevel.DEBUG, null)
    val logs = listOf(log1, log2, log3)

    `when`(mockMyLogsRepository.logs).thenReturn(logs)

    presenter.removeFromMyLog(intArrayOf(1, 4))

    verify(mockMyLogsRepository).removeLogEntries(listOf(log2))
    verify(view).showMyLogs(any())
    verify(view, never()).showLogs(any())
  }

  @Test
  fun testUpdateMyLogsNoChangeEmptyMyLogs() {
    `when`(mockMyLogsRepository.logs).thenReturn(emptyList())
    val ret = presenter.updateMyLogs()
    verify(mockMyLogsRepository, never()).reset(anyList())
    assertFalse(ret)
  }

  @Test
  fun testUpdateMyLogsNoChange() {
    val log1 = LogEntry("Log line 1", LogLevel.DEBUG, null).also { it.index = 0 }
    val log2 = LogEntry("Log line 2", LogLevel.DEBUG, null).also { it.index = 1 }
    val log3 = LogEntry("Log line 3", LogLevel.DEBUG, null).also { it.index = 2 }
    val log4 = LogEntry("Log line 4", LogLevel.DEBUG, null).also { it.index = 3 }
    val log5 = LogEntry("Log line 5", LogLevel.DEBUG, null).also { it.index = 4 }
    val logs = listOf(log1, log2, log3, log4, log5)
    val myLogs = listOf(log3, log5)

    `when`(mockMyLogsRepository.logs).thenReturn(myLogs)
    `when`(mockLogsRepository.lastVisibleLogIndex).thenReturn(4)
    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(logs)

    val ret = presenter.updateMyLogs()
    verify(mockMyLogsRepository, never()).reset(anyList())
    assertFalse(ret)
  }

  @Test
  fun testUpdateMyLogsMismatch() {
    val log1 = LogEntry("Log line 1", LogLevel.DEBUG, null).also { it.index = 0 }
    val log2 = LogEntry("Log line 2", LogLevel.DEBUG, null).also { it.index = 1 }
    val log3 = LogEntry("Log line 3", LogLevel.DEBUG, null).also { it.index = 2 }
    val log4 = LogEntry("Log line 4", LogLevel.DEBUG, null).also { it.index = 3 }
    val log5 = LogEntry("Log line 5", LogLevel.DEBUG, null).also { it.index = 4 }

    val myLog4 = LogEntry("Log line 4", LogLevel.DEBUG, null).also { it.index = 0 }
    val myLog5 = LogEntry("Log line 5", LogLevel.DEBUG, null).also { it.index = 1 }
    val logs = listOf(log1, log2, log3, log4, log5)
    val myLogs = listOf(myLog4, myLog5)

    val matchingLog = LogEntry("Matching Log", LogLevel.DEBUG, null)

    `when`(mockMyLogsRepository.logs).thenReturn(myLogs)
    `when`(mockLogsRepository.lastVisibleLogIndex).thenReturn(4)
    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(logs)
    `when`(mockLogsRepository.getMatchingLogEntry(anyOrNull())).thenReturn(matchingLog)

    val ret = presenter.updateMyLogs()
    verify(mockMyLogsRepository).reset(anyList())
    assertTrue(ret)
  }

  @Test
  fun testUpdateMyLogsMismatchGreaterIndex() {
    val log1 = LogEntry("Log line 1", LogLevel.DEBUG, null).also { it.index = 0 }
    val log2 = LogEntry("Log line 2", LogLevel.DEBUG, null).also { it.index = 1 }
    val log3 = LogEntry("Log line 3", LogLevel.DEBUG, null).also { it.index = 2 }
    val log4 = LogEntry("Log line 4", LogLevel.DEBUG, null).also { it.index = 3 }
    val log5 = LogEntry("Log line 5", LogLevel.DEBUG, null).also { it.index = 4 }

    val myLog4 = LogEntry("Log line 4", LogLevel.DEBUG, null).also { it.index = 5 }
    val logs = listOf(log1, log2, log3, log4, log5)
    val myLogs = listOf(myLog4)

    val matchingLog = LogEntry("Matching Log", LogLevel.DEBUG, null)

    `when`(mockMyLogsRepository.logs).thenReturn(myLogs)
    `when`(mockLogsRepository.lastVisibleLogIndex).thenReturn(4)
    `when`(mockLogsRepository.currentlyOpenedLogs).thenReturn(logs)
    `when`(mockLogsRepository.getMatchingLogEntry(anyOrNull())).thenReturn(matchingLog)

    val ret = presenter.updateMyLogs()
    verify(mockMyLogsRepository).reset(anyList())
    assertTrue(ret)
  }

  @Test
  fun testGetLogLineInfo() {
    val entry = LogEntry(
      "10-12 22:33:46.839  3172  3172 V KeyguardStatusView: refresh statusview showing:true",
      LogLevel.VERBOSE,
      null
    )
    val info = presenter.getLogLineInfo(entry)
    assertNotNull(info)
    assertEquals(3172, info?.pid)
    assertEquals(3172, info?.tid)
    assertEquals("KeyguardStatusView", info?.tag)
    assertEquals("refresh statusview showing:true", info?.message)
  }

  companion object {
    private const val TEST_SERIALIZED_FILTER = "Test,VGVzdA==,2,255:0:0"
    private const val TEST_SERIALIZED_FILTER2 = "Test2,VGVzdA==,2,255:0:0"
    private const val TEST_SERIALIZED_FILTER3 = "Test3,VGVzdA==,2,255:0:0"
  }
}
