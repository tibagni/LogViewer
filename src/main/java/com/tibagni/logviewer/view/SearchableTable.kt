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
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableColumnModel
import javax.swing.table.TableModel


class SearchableTable @JvmOverloads constructor(
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

  private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
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
      val filterResult = if (pattern.isNotBlank()) runCatching {
        Filter(
          "search",
          pattern,
          Color.RED,
          LogLevel.DEBUG,
          matchCaseOption.isSelected
        )
      }.onFailure { Logger.error("create filter error", it) } else null

      val matchedEntries = mutableListOf<Int>()
      val updatedRow = mutableListOf<Int>()
      for (index in 0 until table.model.rowCount) {
        val entry = table.model.getValueAt(index, 0) as LogEntry
        if (filterResult?.getOrNull()?.appliesTo(entry) == true) {
          matchedEntries += index
          updatedRow += index
          entry.searchFilter = filterResult.getOrNull()
        } else {
          if (entry.searchFilter != null) {
            updatedRow += index
            entry.searchFilter = null
          }
        }
      }

      withContext(Dispatchers.Main) {
        searchResult.text =
          if (filterResult?.isFailure == true) " bad pattern " else "  ${matchedEntries.size} results  "
        updatedRow.forEach {
          (table.model as AbstractTableModel).fireTableCellUpdated(it, 0)
        }
      }
      matchedEntries
    }
  }

  private fun showSearch() {
    if (searchOptionPanel.isVisible) return

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
}