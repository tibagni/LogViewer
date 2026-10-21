package com.tibagni.logviewer.log

import com.tibagni.logviewer.filter.FiltersPanel
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.util.CommonUtils
import com.tibagni.logviewer.util.EditorLauncher
import com.tibagni.logviewer.util.SwingUtils
import java.awt.Component
import java.awt.Window
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JSeparator
import javax.swing.JTable
import javax.swing.KeyStroke
import javax.swing.SwingUtilities

/**
 * Attaches common log table interactions (double-click navigation, right-click context menu,
 * and keyboard shortcuts like Cmd+I / Ctrl+I to inspect log details) to the specified [JTable].
 */
fun JTable.setupLogTableActions(
  model: LogListTableModel,
  parentWindow: Window? = null,
  filtersPanelProvider: (() -> FiltersPanel?)? = null,
  onDoubleClick: ((selectedIndex: Int, clickedEntry: LogEntry) -> Unit)? = null,
  addSelectionActions: ((popup: JPopupMenu, selectedEntries: List<LogEntry>) -> Unit)? = null,
  addExtraSingleEntryActions: ((popup: JPopupMenu, singleEntry: LogEntry) -> Unit)? = null
) {
  val shortcutMask = try {
    SwingUtils.getMenuShortcutKeyMask()
  } catch (e: Throwable) {
    KeyEvent.CTRL_DOWN_MASK
  }
  val inspectShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_I, shortcutMask)
  registerKeyboardAction(
    {
      if (selectedRowCount == 1) {
        val entry = model.getEntry(selectedRow) ?: (model.getValueAt(selectedRow, 0) as LogEntry)
        showLogLineDetails(this, entry, parentWindow, filtersPanelProvider)
      }
    },
    inspectShortcut,
    JComponent.WHEN_FOCUSED
  )

  addMouseListener(object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      if (e.clickCount == 2) {
        val selectedIndex = selectedRow
        if (selectedIndex != -1) {
          val clickedEntry = model.getEntry(selectedIndex) ?: (model.getValueAt(selectedIndex, 0) as LogEntry)
          onDoubleClick?.invoke(selectedIndex, clickedEntry)
        }
      } else if (SwingUtilities.isRightMouseButton(e)) {
        val row = rowAtPoint(e.point)
        if (row != -1 && !isRowSelected(row)) {
          setRowSelectionInterval(row, row)
        }

        if (selectedRow != -1) {
          val popup = createPopupMenu(
            this@setupLogTableActions,
            model,
            parentWindow,
            filtersPanelProvider,
            addSelectionActions,
            addExtraSingleEntryActions
          )

          try {
            popup.show(this@setupLogTableActions, e.x, e.y)
          } catch (ex: Throwable) {
            // Ignored if table is not showing on screen (e.g. headless tests)
          }
        }
      }
    }
  })
}

internal fun createPopupMenu(
  table: JTable,
  model: LogListTableModel,
  parentWindow: Window? = null,
  filtersPanelProvider: (() -> FiltersPanel?)? = null,
  addSelectionActions: ((popup: JPopupMenu, selectedEntries: List<LogEntry>) -> Unit)? = null,
  addExtraSingleEntryActions: ((popup: JPopupMenu, singleEntry: LogEntry) -> Unit)? = null
): JPopupMenu {
  val popup = JPopupMenu()
  val selectedEntries = table.selectedRows.map { model.getEntry(it) ?: (model.getValueAt(it, 0) as LogEntry) }
  val singleEntry = selectedEntries.singleOrNull()

  addSelectionActions?.invoke(popup, selectedEntries)
  if (selectedEntries.size == 2) {
    addElapsedTimeContextActions(popup, selectedEntries[0], selectedEntries[1])
  }
  if (singleEntry != null) {
    addSingleLogLineContextActions(table, popup, singleEntry, parentWindow, filtersPanelProvider)
    addExtraSingleEntryActions?.invoke(popup, singleEntry)
  }
  return popup
}

private fun addElapsedTimeContextActions(
  popup: JPopupMenu,
  first: LogEntry,
  second: LogEntry
) {
  val t1 = first.timestamp ?: return
  val t2 = second.timestamp ?: return
  val durationResult = ElapsedTimeHelper.calculateDuration(t1, t2) ?: return
  val durationText = ElapsedTimeHelper.formatDurationMenuText(durationResult)
  val fullDurationStr = ElapsedTimeHelper.formatDurationWithSeconds(durationResult)

  if (popup.componentCount > 0) {
    popup.add(JSeparator())
  }

  val elapsedTimeMenu = JMenu(I18n.format(I18n.LOGS_MENU_ELAPSED_TIME, durationText))

  val copySnippetItem = JMenuItem(I18n.get(I18n.LOGS_MENU_COPY_ELAPSED_TIME_SNIPPET))
  copySnippetItem.addActionListener {
    val snippet = ElapsedTimeHelper.createSnippet(first, second, durationResult)
    CommonUtils.copyToClipboard(snippet)
  }
  elapsedTimeMenu.add(copySnippetItem)

  val copyDurationOnlyItem = JMenuItem(I18n.get(I18n.LOGS_MENU_COPY_DURATION_ONLY))
  copyDurationOnlyItem.addActionListener {
    CommonUtils.copyToClipboard(fullDurationStr)
  }
  elapsedTimeMenu.add(copyDurationOnlyItem)

  popup.add(elapsedTimeMenu)
}

private fun addSingleLogLineContextActions(
  table: Component,
  popup: JPopupMenu,
  entry: LogEntry,
  parentWindow: Window?,
  filtersPanelProvider: (() -> FiltersPanel?)?
) {
  val info = LogLineParser.parseLineInfo(entry)
  val filtersPanel = filtersPanelProvider?.invoke()

  popup.add(JSeparator())
  popup.add(I18n.get(I18n.LOGS_MENU_LOG_DETAILS)).addActionListener {
    showLogLineDetails(table, entry, parentWindow, filtersPanelProvider)
  }

  val pid = info?.pid
  if (pid != null && pid > 0 && filtersPanel != null) {
    popup.add(I18n.format(I18n.LOGS_MENU_FILTER_BY_PID, pid.toString())).addActionListener {
      filtersPanel.addFilterForPid(pid)
    }
  }

  val tag = info?.tag
  if (!tag.isNullOrBlank() && filtersPanel != null) {
    popup.add(I18n.format(I18n.LOGS_MENU_FILTER_BY_TAG, tag)).addActionListener {
      filtersPanel.addFilterForTag(tag)
    }
  }

  if (filtersPanel != null) {
    popup.add(I18n.get(I18n.LOGS_MENU_CREATE_FILTER)).addActionListener {
      filtersPanel.createFilterFromText(entry.logText)
    }
  }
}

private fun showLogLineDetails(
  table: Component,
  entry: LogEntry,
  parentWindow: Window?,
  filtersPanelProvider: (() -> FiltersPanel?)?
) {
  try {
    val info = LogLineParser.parseLineInfo(entry) ?: return
    val owner = parentWindow ?: SwingUtilities.getWindowAncestor(table)
    val dialog = LogLineDetailsDialog(
      owner = owner,
      info = info,
      onFilterByPid = { pid -> filtersPanelProvider?.invoke()?.addFilterForPid(pid) },
      onFilterByTag = { tag -> filtersPanelProvider?.invoke()?.addFilterForTag(tag) },
      onOpenInEditor = { file, line -> EditorLauncher.openInEditor(owner, file, line) }
    )
    dialog.isVisible = true
  } catch (e: Exception) {
    Logger.error("Failed to show log line details dialog", e)
  }
}
