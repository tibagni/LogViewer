package com.tibagni.logviewer.view

import com.jgoodies.forms.builder.PanelBuilder
import com.jgoodies.forms.factories.CC
import com.jgoodies.forms.layout.FormLayout
import com.tibagni.logviewer.filter.Filter
import com.tibagni.logviewer.log.LogCellRenderer
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.util.StringUtils
import com.tibagni.logviewer.util.SwingUtils
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.apache.commons.lang3.time.StopWatch
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.event.TableModelEvent
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableColumnModel
import javax.swing.table.TableModel


class SearchableTable @JvmOverloads constructor(
  private val scope: CoroutineScope,
  dm: TableModel? = null,
  cm: TableColumnModel? = null,
  sm: ListSelectionModel? = null
) : JPanel() {

  private val searchOptionPanel = JPanel()
  private val searchText = HintTextField("Search")
  private val clearSearchText = JButton("Clear")
  private val searchLast = JButton(StringUtils.UP_ARROW_HEAD_BIG)
  private val searchNext = JButton(StringUtils.DOWN_ARROW_HEAD_BIG)
  private val searchResult = JLabel()
  private val matchCaseOption = JCheckBox("Match Case")
  private val close = FlatButton(StringUtils.DELETE)

  val table = JTable(dm, cm, sm)

  private var lastSearchJob: Deferred<List<Int>>? = null

  private val performSearchState = MutableStateFlow(Any())
  private var lastSearchGoToPos = -1

  init {
    buildUi()
    searchOptionPanel.isVisible = false

    searchLast.addActionListener {
      searchInDirection(false)
    }

    searchNext.addActionListener {
      searchInDirection(true)
    }

    matchCaseOption.addItemListener {
      matchCaseStateChanged()
    }

    clearSearchText.addActionListener { searchText.text = "" }

    table.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        when (e.keyCode) {
          KeyEvent.VK_F -> if (e.isControlDown || e.isMetaDown) showSearch()
          KeyEvent.VK_ESCAPE -> hideSearch()
        }
      }
    })

    searchText.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        if (e.keyCode == KeyEvent.VK_ESCAPE) {
          hideSearch()
        } else if (e.keyCode == KeyEvent.VK_ENTER) {
          searchInDirection(true)
        }
      }
    })

    close.addActionListener { hideSearch() }
    close.toolTipText = "Hide search bar"

    searchText.whenTextChanges { performSearchState.value = Any() }

    table.selectionModel.addListSelectionListener {
      lastSearchGoToPos = -1
      val renderer = table.getDefaultRenderer(LogEntry::class.java) as LogCellRenderer
      renderer.highlightLine(-1)
      table.revalidate()
      table.repaint()
    }
    table.model.addTableModelListener {
      // perform search if the model insert/delete items, ignore the update action
      if (searchOptionPanel.isVisible && it.type != TableModelEvent.UPDATE) {
        performSearchState.value = Any()
      }
    }

    performSearchState
      .onEach { searchContent() }
      .launchIn(scope)
  }

  private fun searchInDirection(searchDown: Boolean) {
    scope.launch {
      val matchedIndexList = lastSearchJob?.await() ?: emptyList()
      if (matchedIndexList.isEmpty()) return@launch

      val lastPos = if (lastSearchGoToPos != -1) lastSearchGoToPos else table.selectedRow
      // find the nearest matched item index
      val itemIndex = if (searchDown) {
        matchedIndexList.indexOfFirst { it > lastPos }.takeIf { it != -1 } ?: 0
      } else {
        matchedIndexList.indexOfLast { it < lastPos }.takeIf { it != -1 } ?: matchedIndexList.lastIndex
      }
      searchResult.text = " ${itemIndex + 1}/${matchedIndexList.size} "
      val targetCellPos = matchedIndexList[itemIndex]
      SwingUtils.scrollToVisible(table, targetCellPos)
      val renderer = table.getDefaultRenderer(LogEntry::class.java) as LogCellRenderer
      renderer.highlightLine(targetCellPos)
      table.revalidate()
      table.repaint()
      lastSearchGoToPos = targetCellPos
    }
  }

  private fun matchCaseStateChanged() {
    performSearchState.value = Any()
  }

  private fun searchContent() {
    lastSearchJob?.cancel()
    lastSearchGoToPos = -1
    lastSearchJob = scope.async(Dispatchers.Default) {
      val pattern = searchText.text
      val startTime = System.currentTimeMillis()
      Logger.debug("start searching for [$pattern]")
      withContext(Dispatchers.Main) {
        searchResult.text = " searching "
      }

      val filterResult = if (pattern.isNotBlank()) runCatching {
        Filter(
          "search",
          pattern,
          Color.RED,
          LogLevel.VERBOSE,
          matchCaseOption.isSelected
        )
      }.onFailure { Logger.error("create filter error", it) } else null

      var matchedEntries: List<Int> = mutableListOf()
      val rowCount = table.model.rowCount
      if (rowCount == 0) return@async matchedEntries

      val filter = filterResult?.getOrNull()
      val checkFilterTask: (Int) -> Int? = { index: Int ->
        val entry = table.model.getValueAt(index, 0) as LogEntry
        // pattern.matcher also cost so many times
        if (filter?.appliesTo(entry) == true) {
          entry.searchFilter = filter
          index
        } else {
          if (entry.searchFilter != null) {
            entry.searchFilter = null
          }
          null
        }
      }

      val parallelSearch: suspend () -> List<Int> = {
        (0 until rowCount)
          .chunked(1_0000)
          .map { async(Dispatchers.Default) { it.mapNotNull(checkFilterTask) } }
          .flatMap { it.await() }
      }

      matchedEntries = parallelSearch()
      //benchmarkOfSearch(rowCount, filter)

      Logger.debug("done for search [$pattern], time ${System.currentTimeMillis() - startTime}ms")
      withContext(Dispatchers.Main) {
        searchResult.text =
          if (filterResult?.isFailure == true) " bad pattern " else "  ${matchedEntries.size} results  "
          (table.model as AbstractTableModel).fireTableDataChanged()
      }
      matchedEntries
    }
    scope.launch(CoroutineExceptionHandler { _, throwable -> Logger.error("lastSearchJob failed", throwable) }) {
      lastSearchJob?.await()
    }
  }

  private fun showSearch() {
    if (searchOptionPanel.isVisible) {
      if (!searchText.hasFocus()) {
        searchText.requestFocus()
      }
      return
    }

    searchOptionPanel.isVisible = true
    searchText.requestFocus()
    revalidate()
  }

  private fun hideSearch() {
    if (!searchOptionPanel.isVisible) return

    searchOptionPanel.isVisible = false
    searchText.text = ""
    matchCaseOption.isSelected = false
    table.requestFocus()
    revalidate()
  }

  private fun buildUi() {
    layout = GridBagLayout()

    val layout = FormLayout(
      "200dlu, pref, pref, pref, pref, pref, pref:grow, right:pref",  // columns
      "pref"// rows
    )
    val builder = PanelBuilder(layout, searchOptionPanel)
    builder.add(searchText, CC.xy(1, 1))
    builder.add(clearSearchText, CC.xy(2, 1))
    builder.add(searchLast, CC.xy(3, 1))
    builder.add(searchNext, CC.xy(4, 1))
    builder.add(searchResult, CC.xy(5, 1))
    builder.add(matchCaseOption, CC.xy(6, 1))
    builder.add(JLabel(), CC.xy(7, 1)) // Empty space
    builder.add(close, CC.xy(8, 1))

    add(
      searchOptionPanel,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(1)
        .withWeightx(1.0)
        .withFill(GridBagConstraints.HORIZONTAL)
        .build()
    )

    add(
      JScrollPane(table),
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(2)
        .withWeightx(2.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )
  }

  private suspend fun benchmarkOfSearch(rowCount: Int,
                                        filter: Filter?) = coroutineScope {
    // Target env
    //  core processor: 8
    //  threads: 16
    // Time: average(repeat(20))
    // Log lines: 17_9406

    // single thread: 70ms

    // multi thread(0_0100/chunk): ~29.7ms
    // multi thread(0_0500/chunk): ~18.9ms
    // multi thread(0_1000/chunk): ~12.9ms
    // multi thread(0_2000/chunk): ~10.8ms
    // multi thread(0_5000/chunk): ~12.2ms
    // multi thread(1_0000/chunk): ~11.6ms
    // multi thread(5_0000/chunk): ~18.1ms
    // multi thread(10_0000/chunk): ~37.1ms
    // multi thread(15_0000/chunk): ~48.65ms
    // multi thread(20_0000/chunk): ~57.85ms
    // multi thread(25_0000/chunk): ~57.95ms
    // multi thread(30_0000/chunk): ~58.15ms
    // multi thread(40_0000/chunk): ~58.0ms
    // multi thread(50_0000/chunk): ~59.1ms
    // multi thread(100_0000/chunk): ~58.4ms

    // log lines: 282_3528
    // single thread: 2500ms
    // multi thread(0_0100/chunk): ~398.2ms
    // multi thread(0_0500/chunk): ~268.6ms
    // multi thread(0_1000/chunk): ~296.65ms
    // multi thread(0_2000/chunk): ~342.65ms
    // multi thread(0_5000/chunk): ~316.35ms
    // multi thread(1_0000/chunk): ~306.15ms
    // multi thread(5_0000/chunk): ~279.6ms
    // multi thread(10_0000/chunk): ~312.25ms
    // multi thread(15_0000/chunk): ~299.75ms
    // multi thread(20_0000/chunk): ~289.0ms
    // multi thread(25_0000/chunk): ~302.8ms
    // multi thread(30_0000/chunk): ~345.8ms
    // multi thread(40_0000/chunk): ~380.05ms
    // multi thread(50_0000/chunk): ~369.9ms
    // multi thread(100_0000/chunk): ~499.85ms

    val task: (Int) -> Unit = { index: Int ->
      val entry = table.model.getValueAt(index, 0) as LogEntry
      // this also cost many time, but for the benchmark, it a base time here
      // pattern.matcher also cost so many times
      if (filter?.appliesTo(entry) == true) {
        entry.searchFilter = filter
      } else {
        if (entry.searchFilter != null) {
          entry.searchFilter = null
        }
      }
    }

    val test: suspend (Int) -> Pair<Int, Double> = { chunk: Int ->
      val times = mutableListOf<Long>()
      repeat(20) {
        val sw = StopWatch().apply { start() }
        (0 until rowCount)
          .chunked(chunk)
          .map { async(Dispatchers.Default) { it.map(task) } }
          .flatMap { it.await() }
        sw.stop()
        times += sw.time
      }
      val average = times.average()
      Logger.debug("test with chunk $chunk cost average ~ $average")
      chunk to average
    }

    val times = mutableListOf<Pair<Int, Double>>()
    times += test(100)
    times += test(500)
    times += test(1000)
    times += test(2000)
    times += test(5000)
    times += test(1_0000)
    times += test(5_0000)
    times += test(10_0000)
    times += test(15_0000)
    times += test(20_0000)
    times += test(25_0000)
    times += test(30_0000)
    times += test(40_0000)
    times += test(50_0000)
    times += test(100_0000)

    times.sortBy { it.second }
    Logger.debug("multi-thread test with chunk top 5:\n ${times.take(5).map { "Chunk: ${it.first}, time: ${it.second}" }}")

    (0 until rowCount)
      .chunked(10_0000)
      .map { async(Dispatchers.Default) { it.map(task) } }
      .flatMap { it.await() }

    val sw = StopWatch().also { it.start() }
    for (index in 0 until rowCount) {
      task(index)
    }
    Logger.debug("single-thread test ${sw.time}")
  }
}