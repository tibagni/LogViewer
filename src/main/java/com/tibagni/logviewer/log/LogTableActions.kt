package com.tibagni.logviewer.log

import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.filter.FiltersPanel
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.shortcuts.ShortcutAction
import com.tibagni.logviewer.util.CommonUtils
import com.tibagni.logviewer.util.EditorLauncher
import com.tibagni.logviewer.util.PlatformUtils
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.CommonIcons
import com.tibagni.logviewer.view.Toast
import java.awt.Component
import java.awt.Window
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractAction
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
  val copyLogsHandler = {
    if (selectedRowCount > 0) {
      val selectedLogs = selectedRows.map { row ->
        model.getEntry(row) ?: (model.getValueAt(row, 0) as LogEntry)
      }
      if (selectedLogs.isNotEmpty()) {
        val text = selectedLogs.joinToString("\n") { it.logText }
        CommonUtils.copyToClipboard(text)
        val window = parentWindow ?: SwingUtilities.getWindowAncestor(this@setupLogTableActions)
        Toast.showToast(window, I18n.get(I18n.COMMON_COPIED_TO_CLIPBOARD), Toast.LENGTH_SHORT)
      }
    }
  }

  val copyAction = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      copyLogsHandler()
    }
  }
  actionMap.put("copy", copyAction)

  val ctrlC = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK)
  val metaC = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.META_DOWN_MASK)
  inputMap.put(ctrlC, "copy")
  inputMap.put(metaC, "copy")
  getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ctrlC, "copy")
  getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(metaC, "copy")

  addKeyListener(object : KeyAdapter() {
    override fun keyPressed(e: KeyEvent) {
      val isInspect =
        ServiceLocator.shortcutManager.matches(e, ShortcutAction.LOGS_INSPECT_LINE)
      if (selectedRowCount == 1 && isInspect) {
        e.consume()
        val entry =
          model.getEntry(selectedRow) ?: (model.getValueAt(selectedRow, 0) as LogEntry)
        showLogLineDetails(this@setupLogTableActions, entry, parentWindow, filtersPanelProvider)
      } else if (e.keyCode == KeyEvent.VK_C && (e.isControlDown || e.isMetaDown)) {
        if (selectedRowCount > 0) {
          e.consume()
          copyLogsHandler()
        }
      }
    }
  })

  addMouseListener(object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      if (e.clickCount == 2) {
        val selectedIndex = selectedRow
        if (selectedIndex != -1) {
          val clickedEntry =
            model.getEntry(selectedIndex) ?: (model.getValueAt(selectedIndex, 0) as LogEntry)
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
  val selectedEntries = table.selectedRows.map {
    model.getEntry(it) ?: (model.getValueAt(it, 0) as LogEntry)
  }
  val singleEntry = selectedEntries.singleOrNull()

  if (selectedEntries.isNotEmpty()) {
    val copyItem = JMenuItem(I18n.get(I18n.COMMON_COPY)).apply {
      icon = CommonIcons.copyIcon(UIScaleUtils.dip(14))
      accelerator = KeyStroke.getKeyStroke(
        KeyEvent.VK_C,
        PlatformUtils.menuShortcutMask
      )
      addActionListener {
        val text = selectedEntries.joinToString("\n") { it.logText }
        CommonUtils.copyToClipboard(text)
        val window = parentWindow ?: SwingUtilities.getWindowAncestor(table)
        Toast.showToast(window, I18n.get(I18n.COMMON_COPIED_TO_CLIPBOARD), Toast.LENGTH_SHORT)
      }
    }
    popup.add(copyItem)
  }

  if (addSelectionActions != null) {
    val countBefore = popup.componentCount
    addSelectionActions.invoke(popup, selectedEntries)
    if (popup.componentCount > countBefore && countBefore > 0) {
      popup.insert(JSeparator(), countBefore)
    }
  }

  if (selectedEntries.size == 2) {
    addElapsedTimeContextActions(
      table,
      popup,
      selectedEntries[0],
      selectedEntries[1],
      parentWindow
    )
  }
  if (singleEntry != null) {
    addSingleLogLineContextActions(table, popup, singleEntry, parentWindow, filtersPanelProvider)
    addExtraSingleEntryActions?.invoke(popup, singleEntry)
  }
  return popup
}

private fun JPopupMenu.addSeparatorIfNeeded() {
  if (componentCount > 0 && getComponent(componentCount - 1) !is JSeparator) {
    add(JSeparator())
  }
}

private fun addElapsedTimeContextActions(
  table: Component,
  popup: JPopupMenu,
  first: LogEntry,
  second: LogEntry,
  parentWindow: Window?
) {
  val t1 = first.timestamp ?: return
  val t2 = second.timestamp ?: return
  val durationResult = ElapsedTimeHelper.calculateDuration(t1, t2) ?: return
  val durationText = ElapsedTimeHelper.formatDurationMenuText(durationResult)
  val fullDurationStr = ElapsedTimeHelper.formatDurationWithSeconds(durationResult)

  popup.addSeparatorIfNeeded()

  val toastParent = parentWindow ?: SwingUtilities.getWindowAncestor(table)
  val elapsedTimeMenu = JMenu(I18n.format(I18n.LOGS_MENU_ELAPSED_TIME, durationText))

  val copySnippetItem = JMenuItem(I18n.get(I18n.LOGS_MENU_COPY_ELAPSED_TIME_SNIPPET))
  copySnippetItem.addActionListener {
    val snippet = ElapsedTimeHelper.createSnippet(first, second, durationResult)
    CommonUtils.copyToClipboard(snippet)
    Toast.showToast(
      toastParent,
      I18n.get(I18n.COMMON_COPIED_TO_CLIPBOARD),
      Toast.LENGTH_SHORT
    )
  }
  elapsedTimeMenu.add(copySnippetItem)

  val copyDurationOnlyItem = JMenuItem(I18n.get(I18n.LOGS_MENU_COPY_DURATION_ONLY))
  copyDurationOnlyItem.addActionListener {
    CommonUtils.copyToClipboard(fullDurationStr)
    Toast.showToast(
      toastParent,
      I18n.get(I18n.COMMON_COPIED_TO_CLIPBOARD),
      Toast.LENGTH_SHORT
    )
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

  popup.addSeparatorIfNeeded()
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
