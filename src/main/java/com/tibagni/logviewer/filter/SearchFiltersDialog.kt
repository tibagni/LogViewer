package com.tibagni.logviewer.filter

import com.formdev.flatlaf.FlatClientProperties
import com.tibagni.logviewer.FiltersRepository
import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.CommonUtils
import com.tibagni.logviewer.util.PlatformUtils
import com.tibagni.logviewer.util.StringUtils
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.CardBorderUtils
import com.tibagni.logviewer.view.CommonIcons
import com.tibagni.logviewer.view.FlatButton
import com.tibagni.logviewer.view.HintTextField
import com.tibagni.logviewer.view.StatusBadge
import com.tibagni.logviewer.view.Toast
import com.tibagni.logviewer.view.whenTextChanges
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

class SearchFiltersDialog(
  private val owner: Frame? = null,
  private val presenter: FiltersPresenter,
  private val filtersRepository: FiltersRepository,
  private val contextTag: FilterContextTag? = null,
  private val onFilterSelected: (group: String, filter: Filter) -> Unit
) : JDialog(owner, I18n.get(I18n.SEARCH_FILTERS_DIALOG_TITLE), true) {

  @VisibleForTesting
  internal val tableModel = SearchFiltersTableModel(presenter, contextTag)

  @VisibleForTesting
  internal val resultsTable = JTable(tableModel)

  @VisibleForTesting
  internal val searchField =
    HintTextField(I18n.get(I18n.SEARCH_FILTERS_SEARCH_HINT))

  @VisibleForTesting
  internal val matchesBadge = StatusBadge(StatusBadge.Variant.NORMAL)

  @VisibleForTesting
  internal val jumpBtn = FlatButton(I18n.get(I18n.SEARCH_FILTERS_BTN_JUMP)).apply {
    icon = CommonIcons.chevronRightIcon(UIScaleUtils.dip(14))
    iconTextGap = UIScaleUtils.dip(4)
  }

  @VisibleForTesting
  internal val editBtn = FlatButton(I18n.get(I18n.COMMON_EDIT)).apply {
    icon = CommonIcons.editIcon(UIScaleUtils.dip(14))
    iconTextGap = UIScaleUtils.dip(4)
  }

  @VisibleForTesting
  internal val deleteBtn = FlatButton(I18n.get(I18n.COMMON_DELETE)).apply {
    icon = CommonIcons.deleteIcon(UIScaleUtils.dip(14))
    iconTextGap = UIScaleUtils.dip(4)
  }

  @VisibleForTesting
  internal val closeBtn = FlatButton(I18n.get(I18n.COMMON_CLOSE)).apply {
    icon = CommonIcons.closeIcon(UIScaleUtils.dip(14))
    iconTextGap = UIScaleUtils.dip(4)
  }

  @VisibleForTesting
  internal val clearSearchBtn = FlatButton(I18n.get(I18n.SEARCH_FILTERS_BTN_CLEAR)).apply {
    icon = CommonIcons.clearIcon(UIScaleUtils.dip(14))
    iconTextGap = UIScaleUtils.dip(4)
    addActionListener {
      searchField.text = ""
      searchField.requestFocusInWindow()
    }
  }

  @VisibleForTesting
  internal val hintLabel =
    JLabel(I18n.get(I18n.SEARCH_FILTERS_HINT_SHORTCUTS))

  @VisibleForTesting
  internal lateinit var emptyStateCard: JPanel
    private set

  @VisibleForTesting
  internal lateinit var tableScrollPane: JScrollPane
    private set

  private val cardLayout = CardLayout()
  private val centerCards = JPanel(cardLayout)

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

      searchField.putClientProperty(
        FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON,
        true
      )
      searchField.putClientProperty(
        FlatClientProperties.TEXT_FIELD_LEADING_ICON,
        CommonIcons.searchIcon(UIScaleUtils.dip(15))
      )
      add(
        searchField,
        GBConstraintsBuilder()
          .withGridx(0).withGridy(0)
          .withWeightx(1.0)
          .withFill(GridBagConstraints.HORIZONTAL)
          .build()
      )

      add(
        matchesBadge,
        GBConstraintsBuilder()
          .withGridx(0).withGridy(1)
          .withWeightx(1.0)
          .withAnchor(GridBagConstraints.WEST)
          .withInsets(Insets(UIScaleUtils.dip(4), 0, 0, 0))
          .build()
      )
    }

    resultsTable.apply {
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
      rowHeight = UIScaleUtils.dip(24)
      showHorizontalLines = true
      showVerticalLines = false
      intercellSpacing = Dimension(0, 1)
      tableHeader.reorderingAllowed = false
      tableHeader.resizingAllowed = true

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

    tableScrollPane = JScrollPane(resultsTable).apply {
      border = CardBorderUtils.createCardBorder(insets = Insets(0, 0, 0, 0))
    }

    emptyStateCard = JPanel(GridBagLayout()).apply {
      isOpaque = false
      border = CardBorderUtils.createCardBorder(
        insets = Insets(
          UIScaleUtils.dip(24),
          UIScaleUtils.dip(16),
          UIScaleUtils.dip(24),
          UIScaleUtils.dip(16)
        )
      )

      val iconLabel = JLabel(CommonIcons.searchIcon(UIScaleUtils.dip(36)))
      val titleLabel = JLabel(I18n.get(I18n.SEARCH_FILTERS_EMPTY_TITLE)).apply {
        font = font.deriveFont(Font.BOLD, font.size2D + UIScaleUtils.dip(2))
        foreground = UIManager.getColor("Label.foreground")
      }
      val emptyHintLabel = JLabel(I18n.get(I18n.SEARCH_FILTERS_EMPTY_HINT)).apply {
        foreground = CardBorderUtils.resolveSecondaryLabelColor()
      }

      add(
        iconLabel,
        GBConstraintsBuilder()
          .withGridx(0).withGridy(0)
          .withAnchor(GridBagConstraints.CENTER)
          .withInsets(Insets(0, 0, UIScaleUtils.dip(10), 0))
          .build()
      )
      add(
        titleLabel,
        GBConstraintsBuilder()
          .withGridx(0).withGridy(1)
          .withAnchor(GridBagConstraints.CENTER)
          .withInsets(Insets(0, 0, UIScaleUtils.dip(6), 0))
          .build()
      )
      add(
        emptyHintLabel,
        GBConstraintsBuilder()
          .withGridx(0).withGridy(2)
          .withAnchor(GridBagConstraints.CENTER)
          .withInsets(Insets(0, 0, UIScaleUtils.dip(14), 0))
          .build()
      )
      add(
        clearSearchBtn,
        GBConstraintsBuilder()
          .withGridx(0).withGridy(3)
          .withAnchor(GridBagConstraints.CENTER)
          .build()
      )
    }

    centerCards.apply {
      isOpaque = false
      border = BorderFactory.createEmptyBorder(
        0,
        UIScaleUtils.dip(10),
        0,
        UIScaleUtils.dip(10)
      )
      add(tableScrollPane, CARD_TABLE)
      add(emptyStateCard, CARD_EMPTY)
    }

    val bottomPanel = JPanel(BorderLayout()).apply {
      border = BorderFactory.createEmptyBorder(
        UIScaleUtils.dip(8), UIScaleUtils.dip(10),
        UIScaleUtils.dip(10), UIScaleUtils.dip(10)
      )

      hintLabel.font = hintLabel.font.deriveFont(
        UIScaleUtils.scaleFont(11).toFloat()
      )
      hintLabel.foreground = CardBorderUtils.resolveSecondaryLabelColor()
      add(hintLabel, BorderLayout.WEST)

      val buttonsPanel = JPanel(
        FlowLayout(FlowLayout.RIGHT, UIScaleUtils.dip(5), 0)
      ).apply {
        add(jumpBtn)
        add(editBtn)
        add(deleteBtn)
        add(closeBtn)
      }
      add(buttonsPanel, BorderLayout.EAST)
    }

    rootPane.defaultButton = jumpBtn

    add(topPanel, BorderLayout.NORTH)
    add(centerCards, BorderLayout.CENTER)
    add(bottomPanel, BorderLayout.SOUTH)
  }

  private fun setupActions() {
    searchField.whenTextChanges { performSearch(searchField.text) }

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

    resultsTable.actionMap.put("copy", object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        copyFilterHandler()
      }
    })

    resultsTable.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        val isCopy = e.keyCode == KeyEvent.VK_C && (e.isControlDown || e.isMetaDown)
        when {
          isCopy && resultsTable.selectedRowCount > 0 -> {
            e.consume()
            copyFilterHandler()
          }
          e.keyCode == KeyEvent.VK_ENTER -> {
            e.consume()
            jumpToSelectedFilter()
          }
          e.keyCode == KeyEvent.VK_SPACE -> {
            e.consume()
            toggleSelectedFilterApplied()
          }
          e.keyCode == KeyEvent.VK_DELETE -> {
            e.consume()
            deleteSelectedFilter()
          }
          e.keyCode == KeyEvent.VK_ESCAPE -> dispose()
          e.keyCode == KeyEvent.VK_E -> {
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

  private fun copyFilterHandler() {
    if (resultsTable.selectedRowCount > 0) {
      val selectedRows = resultsTable.selectedRows
      val text = selectedRows.toList().mapNotNull { row ->
        tableModel.getItem(row)?.let { item ->
          "${item.group} : ${item.filter.name} [${item.filter.pattern}]"
        }
      }.joinToString("\n")
      CommonUtils.copyToClipboard(text)
      Toast.showToast(
        this@SearchFiltersDialog,
        I18n.get(I18n.COMMON_COPIED_TO_CLIPBOARD),
        Toast.LENGTH_SHORT
      )
    }
  }

  private fun showContextMenu(e: MouseEvent) {
    val row = resultsTable.rowAtPoint(e.point)
    if (row >= 0) {
      resultsTable.setRowSelectionInterval(row, row)
      updateButtonStates()

      val popup = JPopupMenu().apply {
        val copyItem = JMenuItem(I18n.get(I18n.COMMON_COPY)).apply {
          icon = CommonIcons.copyIcon(UIScaleUtils.dip(14))
          accelerator = KeyStroke.getKeyStroke(
            KeyEvent.VK_C,
            PlatformUtils.menuShortcutMask
          )
          addActionListener { copyFilterHandler() }
        }
        val jumpItem = JMenuItem(I18n.get(I18n.SEARCH_FILTERS_MENU_JUMP)).apply {
          icon = CommonIcons.chevronRightIcon(UIScaleUtils.dip(14))
          addActionListener { jumpToSelectedFilter() }
        }
        val editItem = JMenuItem(I18n.get(I18n.SEARCH_FILTERS_MENU_EDIT)).apply {
          icon = CommonIcons.editIcon(UIScaleUtils.dip(14))
          addActionListener { editSelectedFilter() }
        }
        val deleteItem = JMenuItem(
          I18n.get(I18n.SEARCH_FILTERS_MENU_DELETE)
        ).apply {
          icon = CommonIcons.deleteIcon(UIScaleUtils.dip(14))
          addActionListener { deleteSelectedFilter() }
        }
        add(copyItem)
        addSeparator()
        add(jumpItem)
        addSeparator()
        add(editItem)
        add(deleteItem)
      }
      popup.show(e.component, e.x, e.y)
    }
  }

  @VisibleForTesting
  internal fun performSearch(query: String) {
    val allFilters = filtersRepository.currentlyOpenedFilters
    val totalFilters = allFilters.values.sumOf { it.size }
    val matchedResults = FiltersSearchLogic.search(allFilters, query)

    tableModel.setItems(matchedResults, query)

    if (query.isBlank()) {
      matchesBadge.updateResult(
        totalFilters,
        totalFilters,
        StatusBadge.Variant.NORMAL,
        customMessage = I18n.format(I18n.SEARCH_FILTERS_SHOWING_ALL, totalFilters)
      )
    } else if (matchedResults.isNotEmpty()) {
      matchesBadge.updateResult(
        matchedResults.size,
        totalFilters,
        StatusBadge.Variant.NORMAL,
        customMessage = I18n.format(
          I18n.SEARCH_FILTERS_SHOWING_MATCHES,
          matchedResults.size,
          totalFilters
        )
      )
    } else {
      matchesBadge.updateResult(
        0,
        totalFilters,
        StatusBadge.Variant.NO_RESULTS,
        customMessage = I18n.format(
          I18n.SEARCH_FILTERS_SHOWING_MATCHES,
          0,
          totalFilters
        )
      )
    }

    if (matchedResults.isNotEmpty()) {
      resultsTable.setRowSelectionInterval(0, 0)
      cardLayout.show(centerCards, CARD_TABLE)
    } else if (query.isNotBlank()) {
      cardLayout.show(centerCards, CARD_EMPTY)
    } else {
      cardLayout.show(centerCards, CARD_TABLE)
    }
    updateButtonStates()
  }

  @VisibleForTesting
  internal fun updateButtonStates() {
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
    val tag = contextTag ?: return
    val newApplied = !item.filter.isApplied(tag)
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

    val edited = EditFilterDialog.showEditFilterDialog(
      owner,
      item.filter
    ) { pattern, caseSensitive ->
      presenter.findDuplicateFilter(pattern, caseSensitive, item.filter)
    }
    if (edited != null) {
      presenter.filterEdited(item.filter)
      tableModel.fireTableRowsUpdated(row, row)
    }
  }

  private fun deleteSelectedFilter() {
    val item = getSelectedItem() ?: return
    val confirm = JOptionPane.showConfirmDialog(
      this,
      I18n.format(
        I18n.SEARCH_FILTERS_DELETE_CONFIRM_MSG,
        item.filter.name,
        item.group
      ),
      I18n.get(I18n.SEARCH_FILTERS_DELETE_CONFIRM_TITLE),
      JOptionPane.YES_NO_OPTION,
      JOptionPane.WARNING_MESSAGE
    )
    if (confirm != JOptionPane.YES_OPTION) return

    val groupFilters =
      filtersRepository.currentlyOpenedFilters[item.group] ?: emptyList()
    val index = groupFilters.indexOf(item.filter)
    if (index >= 0) {
      presenter.removeFilters(item.group, intArrayOf(index))
      performSearch(searchField.text)
    }
  }

  companion object {
    private const val CARD_TABLE = "table"
    private const val CARD_EMPTY = "empty"

    fun show(
      owner: Frame?,
      presenter: FiltersPresenter,
      filtersRepository: FiltersRepository,
      contextTag: FilterContextTag? = null,
      onFilterSelected: (group: String, filter: Filter) -> Unit
    ) {
      val dialog = SearchFiltersDialog(
        owner,
        presenter,
        filtersRepository,
        contextTag,
        onFilterSelected
      )
      dialog.isVisible = true
    }
  }
}

class SearchFiltersTableModel(
  private val presenter: FiltersPresenter,
  private val contextTag: FilterContextTag? = null
) : AbstractTableModel() {
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
      0 -> I18n.get(I18n.SEARCH_FILTERS_COL_APPLIED)
      1 -> I18n.get(I18n.SEARCH_FILTERS_COL_NAME)
      2 -> I18n.get(I18n.SEARCH_FILTERS_COL_PATTERN)
      3 -> I18n.get(I18n.SEARCH_FILTERS_COL_GROUP)
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
      0 -> contextTag != null && item.filter.isApplied(contextTag)
      1 -> item.filter.name ?: ""
      2 -> item.filter.patternString ?: ""
      3 -> item.group
      else -> ""
    }
  }

  override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
    if (columnIndex == 0 && aValue is Boolean) {
      val tag = contextTag ?: return
      val item = items[rowIndex]
      item.filter.setApplied(tag, aValue)
      fireTableCellUpdated(rowIndex, columnIndex)

      if (ServiceLocator.logViewerPrefs.applyFilterOnCheck) {
        presenter.applyFilters()
      }
    }
  }
}

class FilterNameCellRenderer(
  private val tableModel: SearchFiltersTableModel
) : DefaultTableCellRenderer() {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val comp = super.getTableCellRendererComponent(
      table, value, isSelected, hasFocus, row, column
    ) as JLabel
    val item = tableModel.getItem(row) ?: return comp

    val filter = item.filter
    val filterName = filter.name ?: ""
    val query = tableModel.currentQuery

    val truncated = FiltersSearchLogic.truncateAndHighlight(filterName, query, maxLength = 60)
    val verbosity = filter.verbosity?.toString()?.firstOrNull() ?: 'V'
    val verbosityColor = ServiceLocator.themeManager.mutedTextForegroundHex

    val textSnippet =
      "<nobr><small color=\"$verbosityColor\">[$verbosity]</small> $truncated</nobr>"
    comp.text = StringUtils.wrapHtml(textSnippet)
    comp.toolTipText = "${filter.name} (${filter.patternString})" // no-i18n

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

class FilterPatternCellRenderer(
  private val tableModel: SearchFiltersTableModel
) : DefaultTableCellRenderer() {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val comp = super.getTableCellRendererComponent(
      table, value, isSelected, hasFocus, row, column
    ) as JLabel
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
      val isDark = ServiceLocator.themeManager.isDark
      comp.foreground = if (isDark) Color(180, 180, 180) else Color(80, 80, 80)
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
    val comp = super.getTableCellRendererComponent(
      table, value, isSelected, hasFocus, row, column
    ) as JLabel
    val groupName = value?.toString() ?: ""
    comp.text = StringUtils.wrapHtml("<nobr>${StringUtils.htmlEscape(groupName)}</nobr>")
    comp.toolTipText = groupName

    if (isSelected) {
      comp.foreground = table.selectionForeground
      comp.background = table.selectionBackground
    } else {
      val isDark = ServiceLocator.themeManager.isDark
      comp.foreground = if (isDark) Color(150, 150, 150) else Color(110, 110, 110)
      comp.background = table.background
    }
    return comp
  }
}
