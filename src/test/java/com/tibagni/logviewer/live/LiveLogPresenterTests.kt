package com.tibagni.logviewer.live

import com.tibagni.logviewer.ProgressReporter
import com.tibagni.logviewer.adb.AdbDevice
import com.tibagni.logviewer.adb.AdbLogStreamer
import com.tibagni.logviewer.adb.AdbManager
import com.tibagni.logviewer.adb.DeviceState
import com.tibagni.logviewer.filter.Filter
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.log.LogStream
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.awt.Color

class LiveLogPresenterTests {

  /**
   * Mockito's [ArgumentCaptor.capture] is written in Java and returns null at runtime.
   * In Kotlin, passing null to a method expecting a non-nullable parameter triggers
   * Kotlin's intrinsic null-check, resulting in an NPE ("must not be null").
   *
   * To satisfy Kotlin's runtime null-safety while registering the argument matcher
   * on Mockito's internal verification stack, this helper calls [captor.capture()]
   * and returns a non-null [fallback] instance. Mockito discards this return value
   * and matches against the real captured object, which can then be retrieved via
   * [ArgumentCaptor.getValue].
   */
  private fun <T : Any> captureNonNull(captor: ArgumentCaptor<T>, fallback: T): T {
    captor.capture()
    return fallback
  }

  private val dummyFilterListener = object : com.tibagni.logviewer.FiltersRepository.Listener {
    override fun onFiltersApplied(appliedFilters: List<Filter>) {}
  }

  @Mock
  private lateinit var mockView: LiveLogPresenterView

  @Mock
  private lateinit var mockRepo: LiveLogsRepository

  @Mock
  private lateinit var mockStreamer: AdbLogStreamer

  @Mock
  private lateinit var mockFiltersRepo: com.tibagni.logviewer.FiltersRepository

  @Mock
  private lateinit var mockAdbManager: AdbManager

  private val testDevice = AdbDevice(
    serialNumber = "device_123",
    state = DeviceState.ONLINE,
    product = "pixel",
    model = "Pixel_7",
    device = "panther",
    transportId = "1"
  )

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    `when`(mockFiltersRepo.appliedFilters).thenAnswer {
      mockFiltersRepo.currentlyOpenedFilters?.values?.flatten()?.filter { it.isApplied } ?: emptyList<Filter>()
    }
    `when`(mockAdbManager.isAdbAvailable).thenReturn(true)
    runBlocking {
      `when`(mockAdbManager.getConnectedDevices()).thenReturn(emptyList())
    }
  }

  private fun createPresenter(scope: CoroutineScope): LiveLogPresenterImpl {
    return LiveLogPresenterImpl(
      view = mockView,
      liveLogsRepository = mockRepo,
      adbLogStreamer = mockStreamer,
      filtersRepository = mockFiltersRepo,
      adbManager = mockAdbManager,
      coroutineScope = scope,
      ioDispatcher = Dispatchers.Unconfined,
      mainDispatcher = Dispatchers.Unconfined
    )
  }

  @Test
  fun testInitUpdatesViewWithInitialState() {
    runBlocking {
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
      val presenter = createPresenter(scope)
      presenter.init()

      verify(mockView).showStreamingStatus(I18n.get(I18n.LIVE_LOGS_STATUS_STOPPED), false)
      verify(mockView).showStats(0L, 0L, 0, 0)
      assertFalse(presenter.isStreaming)
      assertNull(presenter.connectedDevice)
      presenter.finishing()
    }
  }

  @Test
  fun testStartStreamingBatchesAndDispatchesLogs() {
    runBlocking {
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
      val entry1 = LogEntry("Entry 1", LogLevel.INFO, null, LogStream.MAIN)
      val entry2 = LogEntry("Entry 2", LogLevel.DEBUG, null, LogStream.MAIN)
      val streamDeferred = CompletableDeferred<Unit>()
      `when`(mockStreamer.startStream(testDevice.serialNumber, AdbLogStreamer.DEFAULT_BUFFERS)).thenReturn(flow {
        emit(entry1)
        emit(entry2)
        streamDeferred.await()
      })

      val batchResult = LiveLogBatch(
        newAllLogs = listOf(entry1, entry2),
        evictedAllCount = 0,
        newFilteredLogs = emptyList(),
        evictedFilteredCount = 0
      )
      `when`(mockRepo.addLogEntries(anyList())).thenReturn(batchResult)
      `when`(mockRepo.totalLogsReceived).thenReturn(2L)
      `when`(mockRepo.droppedLogsCount).thenReturn(0L)
      `when`(mockRepo.allLogsCount).thenReturn(2)
      `when`(mockRepo.filteredLogsCount).thenReturn(0)

      val presenter = createPresenter(scope)
      presenter.startStreaming(testDevice)

      delay(50) // Yield to allow batch consumer loop to process channel

      assertTrue(presenter.isStreaming)
      assertEquals(testDevice, presenter.connectedDevice)

      verify(mockStreamer).startStream(testDevice.serialNumber, AdbLogStreamer.DEFAULT_BUFFERS)
      verify(mockView).showStreamingStatus(I18n.get(I18n.LIVE_LOGS_STATUS_STREAMING), true)
      verify(mockRepo, atLeastOnce()).addLogEntries(anyList())
      verify(mockView, atLeastOnce()).showLogsBatch(batchResult)
      verify(mockView, atLeastOnce()).showStats(2L, 0L, 2, 0)

      streamDeferred.complete(Unit)
      presenter.finishing()
    }
  }

  @Test
  fun testStopStreaming() {
    runBlocking {
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
      `when`(mockStreamer.startStream(testDevice.serialNumber, AdbLogStreamer.DEFAULT_BUFFERS)).thenReturn(flow {
        CompletableDeferred<Unit>().await()
      })
      val presenter = createPresenter(scope)
      presenter.startStreaming(testDevice)

      presenter.stopStreaming()
      assertFalse(presenter.isStreaming)
      assertEquals(testDevice, presenter.connectedDevice)

      verify(mockStreamer).stopStream()
      verify(mockView, atLeastOnce()).showStreamingStatus(I18n.get(I18n.LIVE_LOGS_STATUS_STOPPED), false)

      // When clearing logs after stopping, connectedDevice is reset
      presenter.clearLogs()
      assertNull(presenter.connectedDevice)

      presenter.finishing()
    }
  }

  @Test
  fun testClearLogs() {
    runBlocking {
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
      val presenter = createPresenter(scope)
      presenter.clearLogs()

      verify(mockRepo).clear()
      verify(mockView).showAllLogs(emptyList(), emptyList())
      verify(mockView).showStats(0L, 0L, 0, 0)
      presenter.finishing()
    }
  }

  @Test
  fun testInitSetsLiveFilters() {
    runBlocking {
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
      val activeFilter = Filter("Test", "regex.*", Color.RED, LogLevel.VERBOSE).apply { isApplied = true }
      val inactiveFilter = Filter("Inactive", "foo", Color.BLUE, LogLevel.INFO).apply { isApplied = false }
      `when`(mockFiltersRepo.currentlyOpenedFilters).thenReturn(mapOf("group1" to listOf(activeFilter, inactiveFilter)))

      val presenter = createPresenter(scope)
      presenter.init()

      val captor = ArgumentCaptor.forClass(com.tibagni.logviewer.FiltersRepository.Listener::class.java)
      verify(mockFiltersRepo).addListener(captureNonNull(captor, dummyFilterListener))
      verify(mockRepo).setLiveFilters(listOf(activeFilter))
      presenter.finishing()
    }
  }

  @Test
  fun testVisibleLogsOffsetDelegatesToRepo() {
    runBlocking {
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
      `when`(mockRepo.visibleLogsOffset).thenReturn(42)
      val presenter = createPresenter(scope)

      assertEquals(42, presenter.visibleLogsOffset)
      verify(mockRepo).visibleLogsOffset
      presenter.finishing()
    }
  }

  @Test
  fun testFiltersRepositoryListenerRegisteredAndNotified() {
    runBlocking {
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
      val captor = ArgumentCaptor.forClass(com.tibagni.logviewer.FiltersRepository.Listener::class.java)

      val entry = LogEntry("matched", LogLevel.INFO, null, LogStream.MAIN)
      `when`(mockRepo.allLogs).thenReturn(listOf(entry))
      `when`(mockRepo.filteredLogs).thenReturn(listOf(entry))
      `when`(mockRepo.totalLogsReceived).thenReturn(1L)
      `when`(mockRepo.droppedLogsCount).thenReturn(0L)
      `when`(mockRepo.allLogsCount).thenReturn(1)
      `when`(mockRepo.filteredLogsCount).thenReturn(1)

      val presenter = createPresenter(scope)
      presenter.init()
      verify(mockFiltersRepo).addListener(captureNonNull(captor, dummyFilterListener))
      clearInvocations(mockRepo)

      val listener = captor.value
      assertNotNull(listener)
      val filter = Filter("F1", "matched", Color.RED, LogLevel.INFO).apply { isApplied = true }
      listener.onFiltersApplied(listOf(filter))

      @Suppress("UNCHECKED_CAST")
      val filtersCaptor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<Filter>>
      val prCaptor: ArgumentCaptor<ProgressReporter> = ArgumentCaptor.forClass(ProgressReporter::class.java)
      verify(mockRepo).setLiveFilters(captureNonNull(filtersCaptor, emptyList()), prCaptor.capture())
      assertEquals(listOf(filter), filtersCaptor.value)

      prCaptor.value.onProgress(50, "")
      verify(mockView).showFilteringProgress(50)

      verify(mockView).showFilteringProgress(0)
      verify(mockView).finishFilteringProgress()
      verify(mockView).showAllLogs(listOf(entry), listOf(entry))

      presenter.finishing()
    }
  }

  @Test
  fun testLoadDevicesWhenAdbNotAvailable() {
    runBlocking {
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
      `when`(mockAdbManager.isAdbAvailable).thenReturn(false)
      val presenter = createPresenter(scope)

      presenter.loadDevices()

      verify(mockView).showDeviceScanning()
      verify(mockView).showAdbNotAvailable()
      presenter.finishing()
    }
  }

  @Test
  fun testLoadDevicesWithNoDevices() {
    runBlocking {
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
      `when`(mockAdbManager.isAdbAvailable).thenReturn(true)
      `when`(mockAdbManager.getConnectedDevices()).thenReturn(emptyList())
      val presenter = createPresenter(scope)

      presenter.loadDevices()

      verify(mockView).showDeviceScanning()
      verify(mockView).showNoDevices()
      presenter.finishing()
    }
  }

  @Test
  fun testLoadDevicesWithConnectedDevices() {
    runBlocking {
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
      `when`(mockAdbManager.isAdbAvailable).thenReturn(true)
      val devices = listOf(testDevice)
      `when`(mockAdbManager.getConnectedDevices()).thenReturn(devices)
      val presenter = createPresenter(scope)

      presenter.loadDevices()

      verify(mockView).showDeviceScanning()
      verify(mockView).showDeviceList(devices)
      presenter.finishing()
    }
  }

  @Test
  fun testOpenDeviceTerminalSuccess() {
    runBlocking {
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
      `when`(mockAdbManager.launchDeviceTerminal(testDevice)).thenReturn(true)
      val presenter = createPresenter(scope)

      presenter.openDeviceTerminal(testDevice)

      verify(mockAdbManager).launchDeviceTerminal(testDevice)
      verify(mockView, never()).showOpenTerminalFailed()
      presenter.finishing()
    }
  }

  @Test
  fun testOpenDeviceTerminalFailed() {
    runBlocking {
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
      `when`(mockAdbManager.launchDeviceTerminal(testDevice)).thenReturn(false)
      val presenter = createPresenter(scope)

      presenter.openDeviceTerminal(testDevice)

      verify(mockAdbManager).launchDeviceTerminal(testDevice)
      verify(mockView).showOpenTerminalFailed()
      presenter.finishing()
    }
  }

  @Test
  fun testRestartAdbServerSuccess() {
    runBlocking {
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
      `when`(mockAdbManager.isAdbAvailable).thenReturn(true)
      `when`(mockAdbManager.restartServer()).thenReturn(true)
      val presenter = createPresenter(scope)

      presenter.restartAdbServer()

      verify(mockView).showRestartingServer(true)
      verify(mockAdbManager).restartServer()
      verify(mockView).showRestartingServer(false)
      verify(mockView).showRestartServerResult(true)
      presenter.finishing()
    }
  }

  @Test
  fun testRestartAdbServerFailed() {
    runBlocking {
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
      `when`(mockAdbManager.isAdbAvailable).thenReturn(true)
      `when`(mockAdbManager.restartServer()).thenReturn(false)
      val presenter = createPresenter(scope)

      presenter.restartAdbServer()

      verify(mockView).showRestartingServer(true)
      verify(mockAdbManager).restartServer()
      verify(mockView).showRestartingServer(false)
      verify(mockView).showRestartServerResult(false)
      presenter.finishing()
    }
  }

  @Test
  fun testDisconnectDeviceReturnsToHub() {
    runBlocking {
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
      `when`(mockStreamer.startStream(testDevice.serialNumber, AdbLogStreamer.DEFAULT_BUFFERS)).thenReturn(flow {
        CompletableDeferred<Unit>().await()
      })
      val presenter = createPresenter(scope)
      presenter.startStreaming(testDevice)
      assertTrue(presenter.isStreaming)
      assertEquals(testDevice, presenter.connectedDevice)

      presenter.disconnectDevice()

      assertFalse(presenter.isStreaming)
      assertNull(presenter.connectedDevice)
      verify(mockStreamer).stopStream()
      verify(mockView).showDeviceScanning()
      presenter.finishing()
    }
  }

  @Test
  fun testAvailableAndDefaultBuffers() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    val presenter = createPresenter(scope)

    assertEquals(AdbLogStreamer.ALL_BUFFERS, presenter.availableBuffers)
    assertEquals(AdbLogStreamer.DEFAULT_BUFFERS, presenter.defaultBuffers)
  }

  @Test
  fun testStartStreamingWithCustomBuffers() {
    runBlocking {
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
      val customBuffers = listOf("system", "events")
      `when`(mockStreamer.startStream(testDevice.serialNumber, customBuffers)).thenReturn(flow {
        CompletableDeferred<Unit>().await()
      })

      val presenter = createPresenter(scope)
      presenter.startStreaming(testDevice, customBuffers)

      assertTrue(presenter.isStreaming)
      verify(mockStreamer).startStream(testDevice.serialNumber, customBuffers)
      presenter.finishing()
    }
  }

  @Test
  fun testResumeStreamingUsesLastStreamedBuffers() {
    runBlocking {
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
      val customBuffers = listOf("system", "events")
      `when`(mockStreamer.startStream(testDevice.serialNumber, customBuffers)).thenReturn(flow {
        CompletableDeferred<Unit>().await()
      })

      val presenter = createPresenter(scope)
      presenter.startStreaming(testDevice, customBuffers)
      presenter.stopStreaming()

      presenter.startStreaming(testDevice)

      assertTrue(presenter.isStreaming)
      verify(mockStreamer, times(2)).startStream(testDevice.serialNumber, customBuffers)
      presenter.finishing()
    }
  }

  @Test
  fun testStartStreamingWithEmptyBuffersDoesNothing() {
    runBlocking {
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
      val presenter = createPresenter(scope)

      presenter.startStreaming(testDevice, emptyList())

      assertFalse(presenter.isStreaming)
      verifyNoInteractions(mockStreamer)
      presenter.finishing()
    }
  }
}
