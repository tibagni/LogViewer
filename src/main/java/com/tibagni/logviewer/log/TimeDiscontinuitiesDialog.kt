package com.tibagni.logviewer.log

import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Frame
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.AbstractTableModel

class TimeDiscontinuitiesDialog(
  owner: Frame?,
  private val discontinuities: List<TimeDiscontinuity>,
  private val onNavigateToLine: ((TimeDiscontinuity) -> Unit)? = null
) : JDialog(owner, true) {

  private val tableModel = DiscontinuityTableModel(discontinuities)
  private val table = JTable(tableModel)
  private val goToLineBtn = JButton(I18n.get(I18n.TIME_DISCONTINUITY_GO_TO_LINE))
  private val closeBtn = JButton(I18n.get(I18n.COMMON_CLOSE))

  init {
    title = I18n.get(I18n.TIME_DISCONTINUITY_DIALOG_TITLE)
    buildUi()
    setupActions()
  }

  private fun buildUi() {
    val mainPanel = JPanel(BorderLayout(UIScaleUtils.dip(10), UIScaleUtils.dip(10)))
    mainPanel.border = BorderFactory.createEmptyBorder(
      UIScaleUtils.dip(12),
      UIScaleUtils.dip(12),
      UIScaleUtils.dip(12),
      UIScaleUtils.dip(12)
    )

    val headerPanel = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }
    val titleLabel = JLabel(I18n.format(I18n.TIME_DISCONTINUITY_DIALOG_HEADER, discontinuities.size.toString())).apply {
      font = font.deriveFont(font.size2D + UIScaleUtils.dip(2).toFloat()).deriveFont(java.awt.Font.BOLD)
    }
    val descArea = JTextArea(I18n.get(I18n.TIME_DISCONTINUITY_DIALOG_DESC)).apply {
      isEditable = false
      isOpaque = false
      isFocusable = false
      wrapStyleWord = true
      lineWrap = true
      border = BorderFactory.createEmptyBorder(UIScaleUtils.dip(4), 0, UIScaleUtils.dip(8), 0)
    }
    headerPanel.add(titleLabel)
    headerPanel.add(descArea)
    mainPanel.add(headerPanel, BorderLayout.NORTH)

    table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
    table.rowHeight = UIScaleUtils.dip(24)
    table.tableHeader.reorderingAllowed = false
    if (discontinuities.isNotEmpty()) {
      table.setRowSelectionInterval(0, 0)
    }
    val scrollPane = JScrollPane(table).apply {
      preferredSize = Dimension(UIScaleUtils.dip(650), UIScaleUtils.dip(220))
    }
    mainPanel.add(scrollPane, BorderLayout.CENTER)

    val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, UIScaleUtils.dip(5), 0))
    if (onNavigateToLine != null) {
      buttonPanel.add(goToLineBtn)
    }
    buttonPanel.add(closeBtn)
    mainPanel.add(buttonPanel, BorderLayout.SOUTH)

    contentPane = mainPanel
    defaultCloseOperation = DISPOSE_ON_CLOSE
  }

  private fun setupActions() {
    closeBtn.addActionListener { dispose() }

    goToLineBtn.addActionListener {
      navigateToSelected()
    }

    table.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (e.clickCount == 2 && SwingUtilities.isLeftMouseButton(e)) {
          navigateToSelected()
        }
      }
    })

    rootPane.registerKeyboardAction(
      { dispose() },
      KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
      JComponent.WHEN_IN_FOCUSED_WINDOW
    )

    rootPane.registerKeyboardAction(
      { navigateToSelected() },
      KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
      JComponent.WHEN_IN_FOCUSED_WINDOW
    )
  }

  private fun navigateToSelected() {
    val selectedRow = table.selectedRow
    if (selectedRow >= 0 && selectedRow < discontinuities.size) {
      val item = discontinuities[selectedRow]
      onNavigateToLine?.invoke(item)
      dispose()
    }
  }

  private class DiscontinuityTableModel(
    private val items: List<TimeDiscontinuity>
  ) : AbstractTableModel() {
    private val columnNames = arrayOf(
      I18n.get(I18n.TIME_DISCONTINUITY_COL_FILE),
      I18n.get(I18n.TIME_DISCONTINUITY_COL_LINE),
      I18n.get(I18n.TIME_DISCONTINUITY_COL_PREV),
      I18n.get(I18n.TIME_DISCONTINUITY_COL_CURR),
      I18n.get(I18n.TIME_DISCONTINUITY_COL_DELTA)
    )

    override fun getRowCount(): Int = items.size
    override fun getColumnCount(): Int = columnNames.size
    override fun getColumnName(column: Int): String = columnNames[column]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
      val item = items[rowIndex]
      return when (columnIndex) {
        0 -> item.sourceFileName
        1 -> item.lineNumber
        2 -> item.formatPreviousTime()
        3 -> item.formatCurrentTime()
        4 -> item.formatDelta()
        else -> ""
      }
    }
  }

  companion object {
    fun show(
      owner: Frame?,
      discontinuities: List<TimeDiscontinuity>,
      onNavigateToLine: ((TimeDiscontinuity) -> Unit)? = null
    ) {
      val dialog = TimeDiscontinuitiesDialog(owner, discontinuities, onNavigateToLine)
      dialog.pack()
      dialog.setLocationRelativeTo(owner)
      dialog.isVisible = true
    }
  }
}
