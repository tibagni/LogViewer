package com.tibagni.logviewer.view

import com.formdev.flatlaf.FlatClientProperties
import com.jgoodies.forms.builder.PanelBuilder
import com.tibagni.logviewer.annotation.VisibleForTesting
import com.jgoodies.forms.factories.CC
import com.jgoodies.forms.layout.FormLayout
import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.filter.Filter
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.log.LogCellRenderer
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.shortcuts.ShortcutAction
import com.tibagni.logviewer.util.SwingUtils
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.util.Collections
import javax.swing.*
import javax.swing.border.Border
import javax.swing.event.TableModelEvent
import javax.swing.table.TableColumnModel
import javax.swing.table.TableModel

@OptIn(FlowPreview::class)
class SearchableTable @JvmOverloads constructor(
  dm: TableModel? = null,
  cm: TableColumnModel? = null,
  sm: ListSelectionModel? = null
) : JPanel() {

  internal val searchOptionPanel = object : JPanel() {
    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      try {
        if (width > 0 && height > 0) {
          g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
          )
          val bg = UIManager.getColor("Panel.background")
            ?: background
            ?: Color(240, 240, 240)
          val isDark = isDarkTheme(bg)
          val tint = if (isDark) {
            Color(255, 255, 255, 8)
          } else {
            Color(0, 0, 0, 10)
          }
          g2.color = bg
          g2.fillRect(0, 0, width, height)
          g2.color = tint
          g2.fillRect(0, 0, width, height)
        }
      } finally {
        g2.dispose()
      }
      super.paintComponent(g)
    }
  }

  internal val searchText = HintTextField(
    I18n.get(I18n.SEARCH_HINT)
  ).apply {
    putClientProperty(
      FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON,
      true
    )
  }
  @VisibleForTesting
  internal val searchResult = StatusBadge()
  @VisibleForTesting
  internal val searchLast = FlatButton("").apply {
    icon = CommonIcons.chevronUpIcon()
    toolTipText = I18n.get(I18n.SEARCH_PREV_MATCH)
    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  }
  @VisibleForTesting
  internal val searchNext = FlatButton("").apply {
    icon = CommonIcons.chevronDownIcon()
    toolTipText = I18n.get(I18n.SEARCH_NEXT_MATCH)
    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  }
  @VisibleForTesting
  internal val searchNavDivider = ToolbarDivider()
  @VisibleForTesting
  internal val matchCaseOption = JCheckBox(
    I18n.get(I18n.SEARCH_MATCH_CASE)
  ).apply {
    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  }
  @VisibleForTesting
  internal val close = FlatButton("").apply {
    icon = CommonIcons.closeIcon()
    toolTipText = I18n.get(I18n.SEARCH_TOOLTIP_HIDE)
    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  }

  val table = JTable(dm, cm, sm)
  val scrollPane = JScrollPane(table).apply {
    border = BorderFactory.createEmptyBorder()
    viewportBorder = BorderFactory.createEmptyBorder()
  }

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

    table.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        val shortcutManager = ServiceLocator.shortcutManager
        when {
          shortcutManager.matches(
            e,
            ShortcutAction.SEARCH_FIND_IN_TABLE
          ) -> showSearch()
          shortcutManager.matches(
            e,
            ShortcutAction.SEARCH_NEXT_MATCH
          ) -> {
            if (searchOptionPanel.isVisible) searchInDirection(true)
          }
          shortcutManager.matches(
            e,
            ShortcutAction.SEARCH_PREV_MATCH
          ) -> {
            if (searchOptionPanel.isVisible) searchInDirection(false)
          }
          e.keyCode == KeyEvent.VK_ESCAPE -> hideSearch()
        }
      }
    })

    searchText.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        val shortcutManager = ServiceLocator.shortcutManager
        when {
          e.keyCode == KeyEvent.VK_ESCAPE -> hideSearch()
          e.keyCode == KeyEvent.VK_ENTER -> {
            searchInDirection(!e.isShiftDown)
          }
          shortcutManager.matches(
            e,
            ShortcutAction.SEARCH_NEXT_MATCH
          ) -> searchInDirection(true)
          shortcutManager.matches(
            e,
            ShortcutAction.SEARCH_PREV_MATCH
          ) -> searchInDirection(false)
        }
      }
    })

    close.addActionListener { hideSearch() }

    searchText.whenTextChanges { updateSearchRequest() }

    table.model.addTableModelListener {
      // Re-perform search if the model inserted or deleted items
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
      .drop(1)
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

  /**
   * Refreshes vector icons, search result badge, panel borders, and repaints
   * the search option panel when the active Look and Feel changes.
   */
  fun onThemeChanged() {
    searchLast.icon = CommonIcons.chevronUpIcon()
    searchNext.icon = CommonIcons.chevronDownIcon()
    close.icon = CommonIcons.closeIcon()
    searchResult.updateUI()
    searchOptionPanel.border = createSearchOptionPanelBorder()
    searchOptionPanel.repaint()
    repaint()
  }

  private fun isDarkTheme(fallbackBg: Color): Boolean {
    return try {
      ServiceLocator.themeManager.isDark
    } catch (_: Throwable) {
      val lum = (0.299 * fallbackBg.red +
        0.587 * fallbackBg.green +
        0.114 * fallbackBg.blue) / 255.0
      lum < 0.5
    }
  }

  private fun updateSearchRequest(revisionDelta: Long = 0L) {
    searchRequest.value = SearchRequest(
      text = searchText.text,
      matchCase = matchCaseOption.isSelected,
      revision = searchRequest.value.revision + revisionDelta
    )
  }

  internal fun findNextMatchIndex(
    matchedIndexList: List<Int>,
    lastPos: Int,
    searchDown: Boolean
  ): Int {
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

      val lastPos = if (lastSearchGoToPos != -1) {
        lastSearchGoToPos
      } else {
        table.selectedRow
      }
      val itemIndex = findNextMatchIndex(
        matchedIndexList,
        lastPos,
        searchDown
      )
      searchResult.updateResult(
        currentMatch = itemIndex + 1,
        totalMatches = matchedIndexList.size,
        targetVariant = StatusBadge.Variant.NORMAL
      )
      val targetCellPos = matchedIndexList[itemIndex]
      SwingUtils.scrollToVisible(table, targetCellPos)
      LogCellRenderer.setHighlightLine(table, targetCellPos)
      table.repaint()
      lastSearchGoToPos = targetCellPos
    }
  }

  internal fun searchContent(
    request: SearchRequest
  ): Deferred<List<Int>> {
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
        when {
          filterResult?.isFailure == true -> {
            searchResult.updateResult(
              currentMatch = 0,
              totalMatches = 0,
              targetVariant = StatusBadge.Variant.WARNING,
              customMessage = I18n.get(I18n.SEARCH_BAD_PATTERN)
            )
          }
          pattern.isBlank() -> {
            searchResult.clear()
          }
          matchedEntries.isEmpty() -> {
            searchResult.updateResult(
              currentMatch = 0,
              totalMatches = 0,
              targetVariant = StatusBadge.Variant.NO_RESULTS
            )
          }
          else -> {
            searchResult.updateResult(
              currentMatch = 0,
              totalMatches = matchedEntries.size,
              targetVariant = StatusBadge.Variant.NORMAL
            )
          }
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
    searchResult.clear()
    LogCellRenderer.setHighlightLine(table, -1)
    LogCellRenderer.setSearchFilter(table, null)
    table.repaint()
    table.requestFocus()
    revalidate()
  }

  private fun createSearchOptionPanelBorder(): Border {
    return BorderFactory.createCompoundBorder(
      BorderFactory.createMatteBorder(
        UIScaleUtils.dip(1), 0, 0, 0,
        CardBorderUtils.resolveCardBorderColor()
      ),
      BorderFactory.createEmptyBorder(
        UIScaleUtils.dip(4), UIScaleUtils.dip(6),
        UIScaleUtils.dip(4), UIScaleUtils.dip(6)
      )
    )
  }

  private fun buildUi() {
    layout = GridBagLayout()

    searchOptionPanel.isOpaque = false
    searchOptionPanel.border = createSearchOptionPanelBorder()

    val layout = FormLayout(
      "200dlu, 4dlu, pref, 4dlu, pref, 2dlu, pref, 4dlu, pref, 4dlu, " +
        "pref, pref:grow, right:pref",
      "pref"
    )
    val builder = PanelBuilder(layout, searchOptionPanel)
    builder.add(searchText, CC.xy(1, 1))
    builder.add(searchResult, CC.xy(3, 1))
    builder.add(searchLast, CC.xy(5, 1))
    builder.add(searchNext, CC.xy(7, 1))
    builder.add(searchNavDivider, CC.xy(9, 1))
    builder.add(matchCaseOption, CC.xy(11, 1))
    builder.add(JLabel(), CC.xy(12, 1)) // Empty space
    builder.add(close, CC.xy(13, 1))

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
      scrollPane,
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
