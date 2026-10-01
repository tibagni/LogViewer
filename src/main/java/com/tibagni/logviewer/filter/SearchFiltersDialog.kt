package com.tibagni.logviewer.filter

import com.tibagni.logviewer.FiltersRepository
import com.tibagni.logviewer.LogViewerPresenter
import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.util.StringUtils
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.FlatButton
import com.tibagni.logviewer.view.HintTextField
import com.tibagni.logviewer.view.whenTextChanges
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

class SearchFiltersDialog(
  private val owner: Frame,
  private val presenter: LogViewerPresenter,
  private val filtersRepository: FiltersRepository,
  private val onFilterSelected: (group: String, filter: Filter) -> Unit
) : JDialog(owner, "Find Filters", true) {

  private val tableModel = SearchFiltersTableModel(presenter)
  private val resultsTable = JTable(tableModel)
  private val searchField = HintTextField("Search filters by name or regex pattern...")
  private val clearBtn = FlatButton("Clear")
  private val matchesLabel = JLabel("")

  private val jumpBtn = JButton("Jump to Filter")
  private val editBtn = JButton("Edit...")
  private val deleteBtn = JButton("Delete")
  private val closeBtn = JButton("Close")

  init {
    layout = BorderLayout()
    buildUi()
    setupActions()

    preferredSize = Dimension(UIScaleUtils.dip(780), UIScaleUtils.dip(480))
    pack()
    setLocationRelativeTo(owner)

    // Run initial search with empty query to display all filters
    performSearch("")
  }

  private fun buildUi() {
    val topPanel = JPanel(GridBagLayout()).apply {
      border = BorderFactory.createEmptyBorder(
        UIScaleUtils.dip(10), UIScaleUtils.dip(10),
        UIScaleUtils.dip(6), UIScaleUtils.dip(10)
      )

      add(
        searchField,
        GBConstraintsBuilder()
          .withGridx(0).withGridy(0)
          .withWeightx(1.0)
          .withFill(GridBagConstraints.HORIZONTAL)
          .build()
      )

      clearBtn.toolTipText = "Clear search query"
      add(
        clearBtn,
        GBConstraintsBuilder()
          .withGridx(1).withGridy(0)
          .withAnchor(GridBagConstraints.EAST)
          .build()
      )

      matchesLabel.border = BorderFactory.createEmptyBorder(UIScaleUtils.dip(4), 0, 0, 0)
      matchesLabel.foreground = if (ServiceLocator.themeManager.isDark) Color(170, 170, 170) else Color(100, 100, 100)
      add(
        matchesLabel,
        GBConstraintsBuilder()
          .withGridx(0).withGridy(1)
          .withGridWidth(2)
          .withWeightx(1.0)
          .withAnchor(GridBagConstraints.WEST)
          .build()
      )
    }

    resultsTable.apply {
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
      rowHeight = UIScaleUtils.dip(24)
      tableHeader.reorderingAllowed = false

      columnModel.getColumn(0).apply {
        preferredWidth = UIScaleUtils.dip(65)
        maxWidth = UIScaleUtils.dip(80)
        minWidth = UIScaleUtils.dip(50)
      }
      columnModel.getColumn(1).apply {
        preferredWidth = UIScaleUtils.dip(220)
        cellRenderer = FilterNameCellRenderer(tableModel)
      }
      columnModel.getColumn(2).apply {
        preferredWidth = UIScaleUtils.dip(310)
        cellRenderer = FilterPatternCellRenderer(tableModel)
      }
      columnModel.getColumn(3).apply {
        preferredWidth = UIScaleUtils.dip(160)
        cellRenderer = FilterGroupCellRenderer()
      }
    }

    val tableScrollPane = JScrollPane(resultsTable).apply {
      border = BorderFactory.createCompoundBorder(
        BorderFactory.createEmptyBorder(0, UIScaleUtils.dip(10), 0, UIScaleUtils.dip(10)),
        BorderFactory.createLineBorder(if (ServiceLocator.themeManager.isDark) Color(60, 60, 60) else Color(200, 200, 200))
      )
    }

    val bottomPanel = JPanel(BorderLayout()).apply {
      border = BorderFactory.createEmptyBorder(
        UIScaleUtils.dip(8), UIScaleUtils.dip(10),
        UIScaleUtils.dip(10), UIScaleUtils.dip(10)
      )

      val hintLabel = JLabel("Space: Toggle applied  |  Enter: Jump to filter  |  Del: Delete")
      hintLabel.font = hintLabel.font.deriveFont(UIScaleUtils.scaleFont(11).toFloat())
      hintLabel.foreground = if (ServiceLocator.themeManager.isDark) Color(150, 150, 150) else Color(120, 120, 120)
      add(hintLabel, BorderLayout.WEST)

      val buttonsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, UIScaleUtils.dip(5), 0)).apply {
        add(jumpBtn)
        add(editBtn)
        add(deleteBtn)
        add(closeBtn)
      }
      add(buttonsPanel, BorderLayout.EAST)
    }

    rootPane.defaultButton = jumpBtn

    add(topPanel, BorderLayout.NORTH)
    add(tableScrollPane, BorderLayout.CENTER)
    add(bottomPanel, BorderLayout.SOUTH)
  }

  private fun setupActions() {
    searchField.whenTextChanges { performSearch(searchField.text) }

    clearBtn.addActionListener {
      searchField.text = ""
      searchField.requestFocusInWindow()
    }

    searchField.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        when (e.keyCode) {
          KeyEvent.VK_DOWN -> {
            if (resultsTable.rowCount > 0) {
              if (resultsTable.selectedRow < 0) {
                resultsTable.setRowSelectionInterval(0, 0)
              }
              resultsTable.requestFocusInWindow()
            }
          }
          KeyEvent.VK_ESCAPE -> dispose()
        }
      }
    })

    resultsTable.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        when (e.keyCode) {
          KeyEvent.VK_ENTER -> {
            e.consume()
            jumpToSelectedFilter()
          }
          KeyEvent.VK_SPACE -> {
            e.consume()
            toggleSelectedFilterApplied()
          }
          KeyEvent.VK_DELETE -> {
            e.consume()
            deleteSelectedFilter()
          }
          KeyEvent.VK_ESCAPE -> dispose()
          KeyEvent.VK_E -> {
            if (!e.isControlDown && !e.isMetaDown) {
              e.consume()
              editSelectedFilter()
            }
          }
        }
      }
    })

    resultsTable.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (e.clickCount == 2 && resultsTable.selectedRow >= 0) {
          // Double click on a column other than the checkbox jumps to filter
          if (resultsTable.columnAtPoint(e.point) != 0) {
            jumpToSelectedFilter()
          }
        }
      }

      override fun mousePressed(e: MouseEvent) {
        if (e.isPopupTrigger) showContextMenu(e)
      }

      override fun mouseReleased(e: MouseEvent) {
        if (e.isPopupTrigger) showContextMenu(e)
      }
    })

    resultsTable.selectionModel.addListSelectionListener {
      updateButtonStates()
    }

    jumpBtn.addActionListener { jumpToSelectedFilter() }
    editBtn.addActionListener { editSelectedFilter() }
    deleteBtn.addActionListener { deleteSelectedFilter() }
    closeBtn.addActionListener { dispose() }

    rootPane.registerKeyboardAction(
      { dispose() },
      KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
      JComponent.WHEN_IN_FOCUSED_WINDOW
    )

    updateButtonStates()
  }

  private fun showContextMenu(e: MouseEvent) {
    val row = resultsTable.rowAtPoint(e.point)
    if (row >= 0) {
      resultsTable.setRowSelectionInterval(row, row)
      updateButtonStates()

      val popup = JPopupMenu().apply {
        val jumpItem = JMenuItem("Jump to Filter in Main Window").apply {
          addActionListener { jumpToSelectedFilter() }
        }
        val editItem = JMenuItem("Edit Filter...").apply {
          addActionListener { editSelectedFilter() }
        }
        val deleteItem = JMenuItem("Delete Filter").apply {
          addActionListener { deleteSelectedFilter() }
        }
        add(jumpItem)
        addSeparator()
        add(editItem)
        add(deleteItem)
      }
      popup.show(e.component, e.x, e.y)
    }
  }

  private fun performSearch(query: String) {
    val allFilters = filtersRepository.currentlyOpenedFilters
    val totalFilters = allFilters.values.sumOf { it.size }
    val matchedResults = FiltersSearchLogic.search(allFilters, query)

    tableModel.setItems(matchedResults, query)

    if (query.isBlank()) {
      matchesLabel.text = "Showing all $totalFilters filter(s)"
    } else {
      matchesLabel.text = "Showing ${matchedResults.size} of $totalFilters filter(s)"
    }

    if (matchedResults.isNotEmpty()) {
      resultsTable.setRowSelectionInterval(0, 0)
    }
    updateButtonStates()
  }

  private fun updateButtonStates() {
    val hasSelection = resultsTable.selectedRow >= 0
    jumpBtn.isEnabled = hasSelection
    editBtn.isEnabled = hasSelection
    deleteBtn.isEnabled = hasSelection
  }

  private fun getSelectedItem(): FilterSearchResult? {
    val row = resultsTable.selectedRow
    return if (row >= 0) tableModel.getItem(row) else null
  }

  private fun toggleSelectedFilterApplied() {
    val row = resultsTable.selectedRow
    val item = getSelectedItem() ?: return
    val newApplied = !item.filter.isApplied
    tableModel.setValueAt(newApplied, row, 0)
  }

  private fun jumpToSelectedFilter() {
    val item = getSelectedItem() ?: return
    dispose()
    onFilterSelected(item.group, item.filter)
  }

  private fun editSelectedFilter() {
    val row = resultsTable.selectedRow
    val item = getSelectedItem() ?: return

    val edited = EditFilterDialog.showEditFilterDialog(owner, item.filter)
    if (edited != null) {
      presenter.filterEdited(item.filter)
      tableModel.fireTableRowsUpdated(row, row)
    }
  }

  private fun deleteSelectedFilter() {
    val item = getSelectedItem() ?: return
    val confirm = JOptionPane.showConfirmDialog(
      this,
      "Are you sure you want to delete filter \"${item.filter.name}\" from group \"${item.group}\"?",
      "Delete Filter",
      JOptionPane.YES_NO_OPTION,
      JOptionPane.WARNING_MESSAGE
    )
    if (confirm != JOptionPane.YES_OPTION) return

    val groupFilters = filtersRepository.currentlyOpenedFilters[item.group] ?: emptyList()
    val index = groupFilters.indexOf(item.filter)
    if (index >= 0) {
      presenter.removeFilters(item.group, intArrayOf(index))
      performSearch(searchField.text)
    }
  }

  companion object {
    fun show(
      owner: Frame,
      presenter: LogViewerPresenter,
      filtersRepository: FiltersRepository,
      onFilterSelected: (group: String, filter: Filter) -> Unit
    ) {
      val dialog = SearchFiltersDialog(owner, presenter, filtersRepository, onFilterSelected)
      dialog.isVisible = true
    }
  }
}

class SearchFiltersTableModel(private val presenter: LogViewerPresenter) : AbstractTableModel() {
  private val items = mutableListOf<FilterSearchResult>()
  var currentQuery: String = ""

  fun setItems(newItems: List<FilterSearchResult>, query: String) {
    items.clear()
    items.addAll(newItems)
    currentQuery = query
    fireTableDataChanged()
  }

  fun getItem(row: Int): FilterSearchResult? {
    return if (row in 0 until items.size) items[row] else null
  }

  override fun getRowCount(): Int = items.size
  override fun getColumnCount(): Int = 4

  override fun getColumnName(column: Int): String {
    return when (column) {
      0 -> "Applied"
      1 -> "Name"
      2 -> "Pattern"
      3 -> "Group"
      else -> ""
    }
  }

  override fun getColumnClass(columnIndex: Int): Class<*> {
    return when (columnIndex) {
      0 -> java.lang.Boolean::class.java
      else -> String::class.java
    }
  }

  override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
    return columnIndex == 0
  }

  override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
    val item = items[rowIndex]
    return when (columnIndex) {
      0 -> item.filter.isApplied
      1 -> item.filter.name ?: ""
      2 -> item.filter.patternString ?: ""
      3 -> item.group
      else -> ""
    }
  }

  override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
    if (columnIndex == 0 && aValue is Boolean) {
      val item = items[rowIndex]
      item.filter.isApplied = aValue
      fireTableCellUpdated(rowIndex, columnIndex)

      if (ServiceLocator.logViewerPrefs.applyFilterOnCheck) {
        presenter.applyFilters()
      }
    }
  }
}

class FilterNameCellRenderer(private val tableModel: SearchFiltersTableModel) : DefaultTableCellRenderer() {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel
    val item = tableModel.getItem(row) ?: return comp

    val filter = item.filter
    val filterName = filter.name ?: ""
    val query = tableModel.currentQuery

    val truncated = FiltersSearchLogic.truncateAndHighlight(filterName, query, maxLength = 60)
    val verbosity = filter.verbosity?.toString()?.firstOrNull() ?: 'V'
    val verbosityColor = if (ServiceLocator.themeManager.isDark) "#CCC" else "#555"

    comp.text = StringUtils.wrapHtml("<nobr><small color=\"$verbosityColor\">[$verbosity]</small> $truncated</nobr>")
    comp.toolTipText = "${filter.name} (${filter.patternString})"

    if (isSelected) {
      comp.foreground = table.selectionForeground
      comp.background = table.selectionBackground
    } else {
      comp.foreground = filter.color
      comp.background = table.background
    }

    return comp
  }
}

class FilterPatternCellRenderer(private val tableModel: SearchFiltersTableModel) : DefaultTableCellRenderer() {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel
    val item = tableModel.getItem(row) ?: return comp

    val pattern = item.filter.patternString ?: ""
    val query = tableModel.currentQuery
    val truncated = FiltersSearchLogic.truncateAndHighlight(pattern, query, maxLength = 80)

    comp.text = StringUtils.wrapHtml("<nobr>$truncated</nobr>")
    comp.toolTipText = pattern

    if (isSelected) {
      comp.foreground = table.selectionForeground
      comp.background = table.selectionBackground
    } else {
      comp.foreground = if (ServiceLocator.themeManager.isDark) Color(180, 180, 180) else Color(80, 80, 80)
      comp.background = table.background
    }

    return comp
  }
}

class FilterGroupCellRenderer : DefaultTableCellRenderer() {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel
    val groupName = value?.toString() ?: ""
    comp.text = StringUtils.wrapHtml("<nobr>${StringUtils.htmlEscape(groupName)}</nobr>")
    comp.toolTipText = groupName

    if (isSelected) {
      comp.foreground = table.selectionForeground
      comp.background = table.selectionBackground
    } else {
      comp.foreground = if (ServiceLocator.themeManager.isDark) Color(150, 150, 150) else Color(110, 110, 110)
      comp.background = table.background
    }
    return comp
  }
}
