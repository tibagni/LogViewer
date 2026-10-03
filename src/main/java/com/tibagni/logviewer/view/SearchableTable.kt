package com.tibagni.logviewer.view

import com.jgoodies.forms.builder.PanelBuilder
import com.jgoodies.forms.factories.CC
import com.jgoodies.forms.layout.FormLayout
import com.tibagni.logviewer.filter.Filter
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.log.LogCellRenderer
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.util.StringUtils
import com.tibagni.logviewer.util.SwingUtils
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.util.Collections
import javax.swing.*
import javax.swing.event.TableModelEvent
import javax.swing.table.TableColumnModel
import javax.swing.table.TableModel

@OptIn(FlowPreview::class)
class SearchableTable @JvmOverloads constructor(
  dm: TableModel? = null,
  cm: TableColumnModel? = null,
  sm: ListSelectionModel? = null
) : JPanel() {

  private val searchOptionPanel = JPanel()
  private val searchText = HintTextField(I18n.get(I18n.SEARCH_HINT))
  private val clearSearchText = JButton(I18n.get(I18n.COMMON_CLEAR))
  private val searchLast = JButton(StringUtils.UP_ARROW_HEAD_BIG)
  private val searchNext = JButton(StringUtils.DOWN_ARROW_HEAD_BIG)
  private val searchResult = JLabel()
  private val matchCaseOption = JCheckBox(I18n.get(I18n.SEARCH_MATCH_CASE))
  private val close = FlatButton(StringUtils.DELETE)

  val table = JTable(dm, cm, sm)

  companion object {
    private const val SEARCH_DEBOUNCE_MS = 250L
  }

  internal data class SearchRequest(
    val text: String = "",
    val matchCase: Boolean = false,
    val revision: Long = 0L
  )

  private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
  private var lastSearchJob: Deferred<List<Int>>? = null

  private val searchRequest = MutableStateFlow(SearchRequest())
  private var lastExecutedRequest: SearchRequest? = SearchRequest()
  private var lastSearchGoToPos = -1

  init {
    table.setDefaultRenderer(LogEntry::class.java, LogCellRenderer())
    buildUi()
    searchOptionPanel.isVisible = false

    searchLast.addActionListener {
      searchInDirection(false)
    }

    searchNext.addActionListener {
      searchInDirection(true)
    }

    matchCaseOption.addItemListener {
      updateSearchRequest()
    }

    clearSearchText.addActionListener {
      searchText.text = ""
    }

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
    close.toolTipText = I18n.get(I18n.SEARCH_TOOLTIP_HIDE)

    searchText.whenTextChanges { updateSearchRequest() }

    table.model.addTableModelListener {
      // Re-perform search if the model inserted or deleted items, ignore simple update events
      if (searchOptionPanel.isVisible && it.type != TableModelEvent.UPDATE) {
        updateSearchRequest(revisionDelta = 1L)
      }
    }

    table.selectionModel.addListSelectionListener {
      lastSearchGoToPos = -1
      LogCellRenderer.setHighlightLine(table, -1)
      table.repaint()
    }

    searchRequest
      .debounce { request ->
        if (request.text.isBlank()) 0L else SEARCH_DEBOUNCE_MS
      }
      .distinctUntilChanged()
      .onEach { request ->
        val wasSearching = !lastExecutedRequest?.text.isNullOrBlank()
        val isSearching = request.text.isNotBlank()
        if (request != lastExecutedRequest && (isSearching || wasSearching)) {
          searchContent(request)
        }
      }
      .launchIn(scope)
  }

  private fun updateSearchRequest(revisionDelta: Long = 0L) {
    searchRequest.value = SearchRequest(
      text = searchText.text,
      matchCase = matchCaseOption.isSelected,
      revision = searchRequest.value.revision + revisionDelta
    )
  }

  internal fun findNextMatchIndex(matchedIndexList: List<Int>, lastPos: Int, searchDown: Boolean): Int {
    val idx = Collections.binarySearch(matchedIndexList, lastPos)
    val insertionPoint = if (idx >= 0) idx else -(idx + 1)
    return if (searchDown) {
      val next = if (idx >= 0) idx + 1 else insertionPoint
      if (next in matchedIndexList.indices) next else 0
    } else {
      val prev = if (idx >= 0) idx - 1 else insertionPoint - 1
      if (prev in matchedIndexList.indices) prev else matchedIndexList.lastIndex
    }
  }

  private fun searchInDirection(searchDown: Boolean) {
    scope.launch {
      val currentRequest = searchRequest.value
      val job = if (lastExecutedRequest != currentRequest) {
        searchContent(currentRequest)
      } else {
        lastSearchJob
      }
      val matchedIndexList = job?.await() ?: emptyList()
      if (matchedIndexList.isEmpty()) return@launch

      val lastPos = if (lastSearchGoToPos != -1) lastSearchGoToPos else table.selectedRow
      val itemIndex = findNextMatchIndex(matchedIndexList, lastPos, searchDown)
      searchResult.text = " ${itemIndex + 1}/${matchedIndexList.size} "
      val targetCellPos = matchedIndexList[itemIndex]
      SwingUtils.scrollToVisible(table, targetCellPos)
      LogCellRenderer.setHighlightLine(table, targetCellPos)
      table.repaint()
      lastSearchGoToPos = targetCellPos
    }
  }

  internal fun searchContent(request: SearchRequest): Deferred<List<Int>> {
    searchRequest.value = request
    lastExecutedRequest = request
    lastSearchJob?.cancel()
    lastSearchGoToPos = -1

    val job = scope.async(Dispatchers.Default) {
      val pattern = request.text
      val filterResult = if (pattern.isNotBlank()) runCatching {
        Filter(
          "search",
          pattern,
          Color.RED,
          LogLevel.VERBOSE,
          request.matchCase
        )
      }.onFailure { Logger.error("create filter error", it) } else null

      val filter = filterResult?.getOrNull()
      val totalRows = table.model.rowCount
      val matchedEntries = mutableListOf<Int>()

      if (filter != null) {
        for (index in 0 until totalRows) {
          if (!isActive) break
          val entry = table.model.getValueAt(index, 0) as LogEntry
          if (filter.appliesTo(entry)) {
            matchedEntries.add(index)
          }
        }
      }

      withContext(Dispatchers.Main) {
        LogCellRenderer.setSearchFilter(table, filter)
        searchResult.text = when {
          filterResult?.isFailure == true -> " ${I18n.get(I18n.SEARCH_BAD_PATTERN)} "
          pattern.isBlank() -> ""
          else -> "  ${I18n.format(I18n.SEARCH_RESULTS_COUNT, matchedEntries.size)}  "
        }
        table.repaint()
      }
      matchedEntries
    }
    lastSearchJob = job
    return job
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
    LogCellRenderer.setHighlightLine(table, -1)
    LogCellRenderer.setSearchFilter(table, null)
    table.repaint()
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
}