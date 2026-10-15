package com.tibagni.logviewer.live

import com.tibagni.logviewer.ProgressReporter
import com.tibagni.logviewer.filter.Filter
import com.tibagni.logviewer.filter.FilterContextTag
import com.tibagni.logviewer.filter.Filters
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogStream

data class LiveLogBatch(
  val newAllLogs: List<LogEntry>,
  val newFilteredLogs: List<LogEntry>,
  val evictedAllCount: Int,
  val evictedFilteredCount: Int
)

interface LiveLogsRepository {
  val maxCapacity: Int
  val filterContextTag: FilterContextTag
  val totalLogsReceived: Long
  /**
   * The total number of logs that were evicted from the beginning of the circular buffer
   * when incoming logs exceeded [maxCapacity]. Useful for displaying roll-off statistics to the user.
   */
  val droppedLogsCount: Long

  val allLogs: List<LogEntry>
  val filteredLogs: List<LogEntry>
  val allLogsCount: Int
  val filteredLogsCount: Int
  val availableStreams: Set<LogStream>

  /**
   * Relative offset of the first entry currently retained in [allLogs] relative to the
   * total logs received (equal to [droppedLogsCount]).
   *
   * In a circular buffer of capacity 100,000 where 150,000 logs were received, entries 0..49,999
   * were evicted from the front, so table row 0 corresponds to entry.index = 50,000.
   * When a user navigates from a filtered log entry to the all-logs table, the visible table row
   * index is calculated as:
   *   tableRow = entry.index - visibleLogsOffset (e.g. 50,005 - 50,000 = row 5)
   * This matches the navigation offset formula used in LogViewerView.
   */
  val visibleLogsOffset: Int

  fun addLogEntry(entry: LogEntry): LiveLogBatch
  fun addLogEntries(entries: Collection<LogEntry>): LiveLogBatch

  fun setLiveFilters(filters: Collection<Filter>, progressReporter: ProgressReporter? = null)
  fun clear()

  fun getEntryAt(index: Int): LogEntry?
  fun getEntryByAbsoluteIndex(absoluteIndex: Int): LogEntry?
  fun getFilteredEntryAt(index: Int): LogEntry?
}

class LiveLogsRepositoryImpl(
  override val maxCapacity: Int = DEFAULT_MAX_CAPACITY,
  override val filterContextTag: FilterContextTag = FILTER_CONTEXT_TAG
) : LiveLogsRepository {

  private val lock = Any()

  private val _allLogs = ArrayDeque<LogEntry>()
  private val _filteredLogs = ArrayDeque<LogEntry>()
  private val _availableStreams = hashSetOf<LogStream>()
  private val liveFilters = mutableListOf<Filter>()

  override var totalLogsReceived: Long = 0L
    private set

  override var droppedLogsCount: Long = 0L
    private set

  override val allLogs: List<LogEntry>
    get() = synchronized(lock) { _allLogs.toList() }

  override val filteredLogs: List<LogEntry>
    get() = synchronized(lock) { _filteredLogs.toList() }

  override val allLogsCount: Int
    get() = synchronized(lock) { _allLogs.size }

  override val filteredLogsCount: Int
    get() = synchronized(lock) { _filteredLogs.size }

  override val availableStreams: Set<LogStream>
    get() = synchronized(lock) { _availableStreams.toSet() }

  override val visibleLogsOffset: Int
    get() = synchronized(lock) { droppedLogsCount.toInt() }

  override fun addLogEntry(entry: LogEntry): LiveLogBatch {
    return addLogEntries(listOf(entry))
  }

  override fun addLogEntries(entries: Collection<LogEntry>): LiveLogBatch = synchronized(lock) {
    if (entries.isEmpty()) {
      return LiveLogBatch(emptyList(), emptyList(), 0, 0)
    }

    val addedAll = ArrayList<LogEntry>(entries.size)
    val addedFiltered = ArrayList<LogEntry>()
    var evictedAll = 0
    var evictedFiltered = 0

    for (entry in entries) {
      entry.index = totalLogsReceived.toInt()
      totalLogsReceived++
      _availableStreams.add(entry.logStream)

      var firstMatchedFilter: Filter? = null
      for (filter in liveFilters) {
        if (filter.getContext(filterContextTag) == null) {
          filter.initContext(filterContextTag)
        }
        if (filter.appliesTo(entry)) {
          if (firstMatchedFilter == null) {
            firstMatchedFilter = filter
          }
          filter.getContext(filterContextTag)?.incrementLineCount(entry.logStream)
        }
      }

      if (firstMatchedFilter != null) {
        entry.appliedFilter = firstMatchedFilter
        _filteredLogs.add(entry)
        addedFiltered.add(entry)
      }

      _allLogs.add(entry)
      addedAll.add(entry)

      if (_allLogs.size > maxCapacity) {
        val removed = _allLogs.removeFirst()
        droppedLogsCount++
        evictedAll++

        for (filter in liveFilters) {
          if (filter.appliesTo(removed)) {
            filter.getContext(filterContextTag)?.decrementLineCount(removed.logStream)
          }
        }

        if (removed.appliedFilter != null && _filteredLogs.isNotEmpty() && _filteredLogs.first() == removed) {
          _filteredLogs.removeFirst()
          evictedFiltered++
        }
      }
    }

    LiveLogBatch(
      newAllLogs = addedAll,
      newFilteredLogs = addedFiltered,
      evictedAllCount = evictedAll,
      evictedFilteredCount = evictedFiltered
    )
  }

  override fun setLiveFilters(filters: Collection<Filter>, progressReporter: ProgressReporter?) = synchronized(lock) {
    for (filter in liveFilters) {
      filter.resetContext(filterContextTag)
    }
    liveFilters.clear()
    liveFilters.addAll(filters)

    for (filter in liveFilters) {
      filter.initContext(filterContextTag)
    }

    val pr = progressReporter ?: ProgressReporter { _, _ -> }

    if (_allLogs.isEmpty()) {
      _filteredLogs.clear()
      pr.onProgress(100, "")
      return@synchronized
    }

    if (liveFilters.isEmpty()) {
      for (entry in _allLogs) {
        entry.appliedFilter = null
      }
      _filteredLogs.clear()
      pr.onProgress(100, "")
      return@synchronized
    }

    val matched = Filters.applyMultipleFilters(_allLogs.toList(), liveFilters.toTypedArray(), filterContextTag, pr)
    _filteredLogs.clear()
    _filteredLogs.addAll(matched)
  }

  override fun clear() = synchronized(lock) {
    _allLogs.clear()
    _filteredLogs.clear()
    _availableStreams.clear()
    totalLogsReceived = 0L
    droppedLogsCount = 0L
    for (filter in liveFilters) {
      filter.initContext(filterContextTag)
    }
  }

  override fun getEntryAt(index: Int): LogEntry? = synchronized(lock) {
    if (index in _allLogs.indices) _allLogs[index] else null
  }

  override fun getEntryByAbsoluteIndex(absoluteIndex: Int): LogEntry? = synchronized(lock) {
    val relativeIndex = absoluteIndex - visibleLogsOffset
    if (relativeIndex in _allLogs.indices) _allLogs[relativeIndex] else null
  }

  override fun getFilteredEntryAt(index: Int): LogEntry? = synchronized(lock) {
    if (index in _filteredLogs.indices) _filteredLogs[index] else null
  }

  companion object {
    const val DEFAULT_MAX_CAPACITY = 100_000
    val FILTER_CONTEXT_TAG = FilterContextTag.register("live_logs")
  }
}
