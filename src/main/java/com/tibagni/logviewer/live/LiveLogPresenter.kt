package com.tibagni.logviewer.live

import com.tibagni.logviewer.ProgressReporter
import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.adb.AdbDevice
import com.tibagni.logviewer.adb.AdbLogStreamer
import com.tibagni.logviewer.adb.AdbManager
import com.tibagni.logviewer.filter.Filter
import com.tibagni.logviewer.filter.FilterContextTag
import com.tibagni.logviewer.FiltersRepository
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.log.LogEntry
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

interface LiveLogPresenterView {
  fun showStreamingStatus(statusText: String, isStreaming: Boolean)
  fun showStats(totalReceived: Long, droppedCount: Long, allCount: Int, filteredCount: Int)
  fun showLogsBatch(batch: LiveLogBatch)
  fun showAllLogs(allLogs: List<LogEntry>, filteredLogs: List<LogEntry>)
  fun showFilteringProgress(progress: Int)
  fun finishFilteringProgress()

  fun showDeviceScanning()
  fun showNoDevices()
  fun showDeviceList(devices: List<AdbDevice>)
  fun showAdbNotAvailable()
  fun showRestartingServer(isRestarting: Boolean)
  fun showRestartServerResult(success: Boolean)
  fun showOpenTerminalFailed()

  fun showLogsView(device: AdbDevice)
}

interface LiveLogPresenter {
  val filterContextTag: FilterContextTag
  val isStreaming: Boolean
  val connectedDevice: AdbDevice?
  val visibleLogsOffset: Int
  val availableBuffers: List<String>
  val defaultBuffers: List<String>

  fun init()
  fun loadDevices()
  fun startStreaming(device: AdbDevice, buffers: List<String>? = null)
  fun stopStreaming()
  fun clearLogs()
  fun openDeviceTerminal(device: AdbDevice)
  fun restartAdbServer()
  fun disconnectDevice()
  fun finishing()
}

// Capacity of the buffered Channel connecting ADB stream producer to batch consumer.
// Provides a lightweight (~1 MB) shock absorber for rapid bursts of ADB logs without
// suspending the reader, while acting as a safety backpressure ceiling to prevent OOM.
private const val LOG_CHANNEL_CAPACITY = 10_000

// Maximum number of log entries to batch before dispatching to the UI table models.
// Kept around 200 to balance high throughput with minimal EDT rendering time (~2-4ms).
private const val MAX_BATCH_SIZE = 200

// Delay between consecutive batch dispatches to the Event Dispatch Thread (EDT).
// Caps update frequency at ~50 FPS (1000ms / 20ms), ensuring smooth painting and giving the EDT
// breathing room to handle user interactions without stutter.
private val BATCH_DISPATCH_DELAY: Duration = 20.milliseconds

class LiveLogPresenterImpl(
  private val view: LiveLogPresenterView,
  private val liveLogsRepository: LiveLogsRepository,
  private val adbLogStreamer: AdbLogStreamer,
  private val adbManager: AdbManager = ServiceLocator.adbManager,
  private val filtersRepository: FiltersRepository = ServiceLocator.filtersRepository,
  private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
  private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LiveLogPresenter {

  private var streamingJob: Job? = null
  private var filteringJob: Job? = null
  private var deviceScanJob: Job? = null

  override val availableBuffers: List<String> = AdbLogStreamer.ALL_BUFFERS
  override val defaultBuffers: List<String> = AdbLogStreamer.DEFAULT_BUFFERS
  private var lastStreamedBuffers: List<String> = defaultBuffers

  override val filterContextTag: FilterContextTag
    get() = liveLogsRepository.filterContextTag

  private var _connectedDevice: AdbDevice? = null
  override val connectedDevice: AdbDevice?
    get() = _connectedDevice

  private var _isStreaming: Boolean = false
  override val isStreaming: Boolean
    get() = _isStreaming

  override val visibleLogsOffset: Int
    get() = liveLogsRepository.visibleLogsOffset

  private val filtersListener = object : FiltersRepository.Listener {
    override fun onFiltersApplied(appliedFilters: List<Filter>) {
      applyFiltersAsync(appliedFilters)
    }
  }

  private fun applyFiltersAsync(appliedFilters: List<Filter>) {
    filteringJob?.cancel()
    filteringJob = coroutineScope.launch {
      val hasLogs = liveLogsRepository.allLogsCount > 0
      if (hasLogs) {
        withContext(mainDispatcher) {
          view.showFilteringProgress(0)
        }
      }

      try {
        withContext(ioDispatcher) {
          liveLogsRepository.setLiveFilters(appliedFilters, ProgressReporter { progress, _ ->
            coroutineScope.launch(mainDispatcher) {
              view.showFilteringProgress(progress)
            }
          })
        }
      } finally {
        if (!isActive) {
          withContext(NonCancellable) {
            withContext(mainDispatcher) {
              view.finishFilteringProgress()
            }
          }
        }
      }

      withContext(mainDispatcher) {
        view.finishFilteringProgress()
        view.showAllLogs(liveLogsRepository.allLogs, liveLogsRepository.filteredLogs)
        view.showStats(
          totalReceived = liveLogsRepository.totalLogsReceived,
          droppedCount = liveLogsRepository.droppedLogsCount,
          allCount = liveLogsRepository.allLogsCount,
          filteredCount = liveLogsRepository.filteredLogsCount
        )
      }
    }
  }

  override fun init() {
    filtersRepository.addListener(filtersListener)
    liveLogsRepository.setLiveFilters(filtersRepository.appliedFilters)
    view.showStreamingStatus(
      statusText = I18n.get(I18n.LIVE_LOGS_STATUS_STOPPED),
      isStreaming = false
    )
    view.showStats(
      totalReceived = liveLogsRepository.totalLogsReceived,
      droppedCount = liveLogsRepository.droppedLogsCount,
      allCount = liveLogsRepository.allLogsCount,
      filteredCount = liveLogsRepository.filteredLogsCount
    )
    val hasLogs = liveLogsRepository.allLogsCount > 0
    val device = _connectedDevice
    if (hasLogs && device != null) {
      view.showLogsView(device)
    } else {
      loadDevices()
    }
  }

  override fun loadDevices() {
    deviceScanJob?.cancel()
    deviceScanJob = coroutineScope.launch {
      view.showDeviceScanning()
      val isAvailable = withContext(ioDispatcher) { adbManager.isAdbAvailable }
      if (!isAvailable) {
        view.showAdbNotAvailable()
        return@launch
      }

      val devices = withContext(ioDispatcher) { adbManager.getConnectedDevices() }
      if (devices.isEmpty()) {
        view.showNoDevices()
      } else {
        view.showDeviceList(devices)
      }
    }
  }

  override fun startStreaming(device: AdbDevice, buffers: List<String>?) {
    val activeBuffers = buffers ?: lastStreamedBuffers
    if (activeBuffers.isEmpty()) return
    lastStreamedBuffers = activeBuffers

    if (_isStreaming) {
      stopStreaming()
    }

    _connectedDevice = device
    _isStreaming = true

    view.showLogsView(device)
    view.showStreamingStatus(
      statusText = I18n.get(I18n.LIVE_LOGS_STATUS_STREAMING),
      isStreaming = true
    )

    val channel = Channel<LogEntry>(capacity = LOG_CHANNEL_CAPACITY)

    val producerJob = coroutineScope.launch(ioDispatcher) {
      try {
        adbLogStreamer.startStream(device.serialNumber, activeBuffers).collect { entry ->
          channel.send(entry)
        }
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        withContext(mainDispatcher) {
          _isStreaming = false
          view.showStreamingStatus(
            statusText = I18n.get(I18n.LIVE_LOGS_STATUS_STOPPED),
            isStreaming = false
          )
        }
      } finally {
        channel.close()
      }
    }

    val consumerJob = coroutineScope.launch(ioDispatcher) {
      val batch = ArrayList<LogEntry>(MAX_BATCH_SIZE)
      try {
        while (isActive) {
          val first = channel.receiveCatching().getOrNull() ?: break
          batch.add(first)

          while (batch.size < MAX_BATCH_SIZE) {
            val next = channel.tryReceive().getOrNull() ?: break
            batch.add(next)
          }

          val resultBatch = liveLogsRepository.addLogEntries(batch)
          batch.clear()

          withContext(mainDispatcher) {
            view.showLogsBatch(resultBatch)
            view.showStats(
              totalReceived = liveLogsRepository.totalLogsReceived,
              droppedCount = liveLogsRepository.droppedLogsCount,
              allCount = liveLogsRepository.allLogsCount,
              filteredCount = liveLogsRepository.filteredLogsCount
            )
          }

          delay(BATCH_DISPATCH_DELAY)
        }
      } finally {
        if (batch.isNotEmpty()) {
          val resultBatch = liveLogsRepository.addLogEntries(batch)
          withContext(mainDispatcher) {
            view.showLogsBatch(resultBatch)
            view.showStats(
              totalReceived = liveLogsRepository.totalLogsReceived,
              droppedCount = liveLogsRepository.droppedLogsCount,
              allCount = liveLogsRepository.allLogsCount,
              filteredCount = liveLogsRepository.filteredLogsCount
            )
          }
        }
      }
    }

    streamingJob = coroutineScope.launch {
      producerJob.join()
      consumerJob.join()
      if (_isStreaming) {
        _isStreaming = false
        withContext(mainDispatcher) {
          view.showStreamingStatus(
            statusText = I18n.get(I18n.LIVE_LOGS_STATUS_STOPPED),
            isStreaming = false
          )
        }
      }
    }
  }

  override fun stopStreaming() {
    if (!_isStreaming) return
    _isStreaming = false
    streamingJob?.cancel()
    streamingJob = null
    adbLogStreamer.stopStream()
    view.showStreamingStatus(
      statusText = I18n.get(I18n.LIVE_LOGS_STATUS_STOPPED),
      isStreaming = false
    )
  }

  override fun clearLogs() {
    filteringJob?.cancel()
    filteringJob = null
    liveLogsRepository.clear()
    view.finishFilteringProgress()
    view.showAllLogs(emptyList(), emptyList())
    view.showStats(
      totalReceived = 0,
      droppedCount = 0,
      allCount = 0,
      filteredCount = 0
    )
    if (!_isStreaming) {
      _connectedDevice = null
      loadDevices()
    }
  }

  override fun disconnectDevice() {
    stopStreaming()
    _connectedDevice = null
    clearLogs()
  }

  override fun openDeviceTerminal(device: AdbDevice) {
    coroutineScope.launch {
      val launched = withContext(ioDispatcher) {
        adbManager.launchDeviceTerminal(device)
      }
      if (!launched) {
        withContext(mainDispatcher) {
          view.showOpenTerminalFailed()
        }
      }
    }
  }

  override fun restartAdbServer() {
    coroutineScope.launch {
      withContext(mainDispatcher) {
        view.showRestartingServer(true)
      }
      val success = withContext(ioDispatcher) {
        adbManager.restartServer()
      }
      withContext(mainDispatcher) {
        view.showRestartingServer(false)
        view.showRestartServerResult(success)
      }
      loadDevices()
    }
  }

  override fun finishing() {
    stopStreaming()
    deviceScanJob?.cancel()
    filteringJob?.cancel()
    filtersRepository.removeListener(filtersListener)
    coroutineScope.cancel()
  }
}
