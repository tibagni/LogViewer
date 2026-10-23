package com.tibagni.logviewer.preferences

import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.shortcuts.ShortcutAction
import com.tibagni.logviewer.shortcuts.ShortcutManager
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.HintTextField
import com.tibagni.logviewer.view.whenTextChanges
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dialog
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Window
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

class KeymapPreferencePanel(
  private val parent: Component,
  private val shortcutManager: ShortcutManager = ServiceLocator.shortcutManager
) : PreferencePanel {

  override val id: String = ID
  override val title: String get() = I18n.get(I18n.PREF_CATEGORY_KEYMAP)

  @VisibleForTesting
  internal val pendingMappings: MutableMap<ShortcutAction, KeyStroke> =
    ShortcutAction.values().associateWith { shortcutManager.getKeyStroke(it) }.toMutableMap()

  private var visibleActions: List<ShortcutAction> = ShortcutAction.values().toList()

  @VisibleForTesting
  internal val searchField = HintTextField(I18n.get(I18n.KEYMAP_SEARCH_HINT))

  private val tableModel = KeymapTableModel()
  @VisibleForTesting
  internal val shortcutsTable = JTable(tableModel)

  @VisibleForTesting
  internal val changeBtn = JButton(I18n.get(I18n.KEYMAP_BTN_CHANGE))
  @VisibleForTesting
  internal val resetSelectedBtn = JButton(I18n.get(I18n.KEYMAP_BTN_RESET_SELECTED))
  @VisibleForTesting
  internal val resetAllBtn = JButton(I18n.get(I18n.KEYMAP_BTN_RESET_ALL))

  override val panel: JComponent = buildPanel()

  init {
    setupListeners()
    updateButtonStates()
  }

  private fun buildPanel(): JComponent {
    val root = JPanel(BorderLayout(0, UIScaleUtils.dip(8))).apply {
      isOpaque = false
    }

    shortcutsTable.apply {
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
      rowHeight = UIScaleUtils.dip(26)
      tableHeader.reorderingAllowed = false
      columnModel.getColumn(0).preferredWidth = UIScaleUtils.dip(85)
      columnModel.getColumn(1).preferredWidth = UIScaleUtils.dip(275)
      columnModel.getColumn(2).preferredWidth = UIScaleUtils.dip(160)
      setDefaultRenderer(Any::class.java, ShortcutCellRenderer())
    }

    val scrollPane = JScrollPane(shortcutsTable).apply {
      preferredSize = Dimension(UIScaleUtils.dip(520), UIScaleUtils.dip(286))
    }

    val bottomBar = JPanel(FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(6), 0)).apply {
      isOpaque = false
      border = BorderFactory.createEmptyBorder(UIScaleUtils.dip(2), 0, 0, 0)
      add(changeBtn)
      add(resetSelectedBtn)
      add(resetAllBtn)
    }

    root.add(searchField, BorderLayout.NORTH)
    root.add(scrollPane, BorderLayout.CENTER)
    root.add(bottomBar, BorderLayout.SOUTH)
    return root
  }

  private fun setupListeners() {
    searchField.whenTextChanges { filterShortcuts(searchField.text) }

    shortcutsTable.selectionModel.addListSelectionListener {
      if (!it.valueIsAdjusting) {
        updateButtonStates()
      }
    }

    shortcutsTable.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (e.clickCount == 2 && SwingUtilities.isLeftMouseButton(e)) {
          val selected = getSelectedAction()
          if (selected != null && selected.isRebindable) {
            promptForNewShortcut(selected)
          }
        }
      }
    })

    changeBtn.addActionListener {
      val selected = getSelectedAction()
      if (selected != null && selected.isRebindable) {
        promptForNewShortcut(selected)
      }
    }

    resetSelectedBtn.addActionListener {
      val selected = getSelectedAction() ?: return@addActionListener
      resetActionToDefault(selected)
    }

    resetAllBtn.addActionListener {
      resetAllToDefaults()
    }
  }

  internal fun filterShortcuts(query: String) {
    val trimmed = query.trim().lowercase()
    visibleActions = if (trimmed.isEmpty()) {
      ShortcutAction.values().toList()
    } else {
      ShortcutAction.values().filter { action ->
        val categoryMatch = action.category.displayName.lowercase().contains(trimmed)
        val nameMatch = action.displayName.lowercase().contains(trimmed)
        val ksText = formatActionShortcut(action).lowercase()
        val keyMatch = ksText.contains(trimmed)
        categoryMatch || nameMatch || keyMatch
      }
    }
    tableModel.fireTableDataChanged()
    if (visibleActions.isNotEmpty()) {
      shortcutsTable.setRowSelectionInterval(0, 0)
    }
    updateButtonStates()
  }

  internal fun getSelectedAction(): ShortcutAction? {
    val row = shortcutsTable.selectedRow
    return if (row in visibleActions.indices) visibleActions[row] else null
  }

  internal fun assignShortcut(action: ShortcutAction, newKeyStroke: KeyStroke): Boolean {
    if (!action.isRebindable) return false

    val conflicts = shortcutManager.findConflicts(action, newKeyStroke, pendingMappings)
    val readOnlyConflict = conflicts.firstOrNull { !it.isRebindable }
    if (readOnlyConflict != null) {
      val formattedKey = ShortcutManager.formatKeyStroke(newKeyStroke, shortcutManager.isMac)
      JOptionPane.showMessageDialog(
        parent,
        I18n.format(I18n.KEYMAP_CONFLICT_READONLY_MSG, formattedKey, readOnlyConflict.displayName),
        I18n.get(I18n.KEYMAP_CONFLICT_TITLE),
        JOptionPane.WARNING_MESSAGE
      )
      return false
    }

    if (conflicts.isNotEmpty()) {
      val formattedKey = ShortcutManager.formatKeyStroke(newKeyStroke, shortcutManager.isMac)
      val conflictNames = conflicts.joinToString(", ") { it.displayName }
      val choice = JOptionPane.showConfirmDialog(
        parent,
        I18n.format(I18n.KEYMAP_CONFLICT_MSG, formattedKey, conflictNames),
        I18n.get(I18n.KEYMAP_CONFLICT_TITLE),
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE
      )
      if (choice != JOptionPane.YES_OPTION) {
        return false
      }
    }

    pendingMappings[action] = newKeyStroke
    tableModel.fireTableDataChanged()
    val idx = visibleActions.indexOf(action)
    if (idx >= 0) {
      shortcutsTable.setRowSelectionInterval(idx, idx)
    }
    updateButtonStates()
    return true
  }

  internal fun resetActionToDefault(action: ShortcutAction) {
    if (!action.isRebindable) return
    pendingMappings[action] = shortcutManager.getDefaultKeyStroke(action)
    tableModel.fireTableDataChanged()
    val idx = visibleActions.indexOf(action)
    if (idx >= 0) {
      shortcutsTable.setRowSelectionInterval(idx, idx)
    }
    updateButtonStates()
  }

  internal fun resetAllToDefaults() {
    for (action in ShortcutAction.values()) {
      pendingMappings[action] = shortcutManager.getDefaultKeyStroke(action)
    }
    tableModel.fireTableDataChanged()
    updateButtonStates()
  }

  private fun promptForNewShortcut(action: ShortcutAction) {
    val ownerWindow = SwingUtilities.getWindowAncestor(parent)
    val captureDialog = JDialog(ownerWindow, I18n.get(I18n.KEYMAP_CAPTURE_TITLE), Dialog.ModalityType.APPLICATION_MODAL)
    val promptLabel = JLabel(
      I18n.format(I18n.KEYMAP_CAPTURE_PROMPT, action.displayName),
      SwingConstants.CENTER
    ).apply {
      border = BorderFactory.createEmptyBorder(
        UIScaleUtils.dip(24),
        UIScaleUtils.dip(28),
        UIScaleUtils.dip(24),
        UIScaleUtils.dip(28)
      )
    }

    var captured: KeyStroke? = null
    promptLabel.isFocusable = true
    promptLabel.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        e.consume()
        if (e.keyCode == KeyEvent.VK_ESCAPE && (e.modifiersEx and ShortcutManager.MODIFIER_MASK) == 0) {
          captureDialog.dispose()
          return
        }
        if (ShortcutManager.isModifierOnlyKey(e.keyCode)) {
          return
        }
        val mods = e.modifiersEx and ShortcutManager.MODIFIER_MASK
        captured = KeyStroke.getKeyStroke(e.keyCode, mods)
        captureDialog.dispose()
      }
    })

    captureDialog.contentPane.layout = BorderLayout()
    captureDialog.contentPane.add(promptLabel, BorderLayout.CENTER)
    captureDialog.pack()
    captureDialog.setLocationRelativeTo(parent)
    SwingUtilities.invokeLater { promptLabel.requestFocusInWindow() }
    captureDialog.isVisible = true

    val result = captured
    if (result != null) {
      assignShortcut(action, result)
    }
  }

  private fun updateButtonStates() {
    val selected = getSelectedAction()
    changeBtn.isEnabled = selected != null && selected.isRebindable
    resetSelectedBtn.isEnabled = selected != null && selected.isRebindable && isActionModified(selected)
    resetAllBtn.isEnabled = ShortcutAction.values().any { isActionModified(it) }
  }

  private fun isActionModified(action: ShortcutAction): Boolean {
    if (!action.isRebindable) return false
    val current = pendingMappings[action] ?: shortcutManager.getDefaultKeyStroke(action)
    return current != shortcutManager.getDefaultKeyStroke(action)
  }

  private fun formatActionShortcut(action: ShortcutAction): String {
    val ks = pendingMappings[action] ?: shortcutManager.getDefaultKeyStroke(action)
    val formatted = ShortcutManager.formatKeyStroke(ks, shortcutManager.isMac)
    return if (action.isRebindable) {
      formatted
    } else {
      "$formatted ${I18n.get(I18n.KEYMAP_READ_ONLY_BADGE)}"
    }
  }

  override fun apply() {
    shortcutManager.applyMappings(pendingMappings)
  }

  private inner class KeymapTableModel : AbstractTableModel() {
    override fun getRowCount(): Int = visibleActions.size
    override fun getColumnCount(): Int = 3

    override fun getColumnName(column: Int): String {
      return when (column) {
        0 -> I18n.get(I18n.KEYMAP_COL_CATEGORY)
        1 -> I18n.get(I18n.KEYMAP_COL_ACTION)
        2 -> I18n.get(I18n.KEYMAP_COL_SHORTCUT)
        else -> ""
      }
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
      val action = visibleActions[rowIndex]
      return when (columnIndex) {
        0 -> action.category.displayName
        1 -> action.displayName
        2 -> formatActionShortcut(action)
        else -> ""
      }
    }
  }

  private inner class ShortcutCellRenderer : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
      table: JTable?,
      value: Any?,
      isSelected: Boolean,
      hasFocus: Boolean,
      row: Int,
      column: Int
    ): Component {
      val comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
      val action = visibleActions.getOrNull(row)
      if (action != null) {
        val modified = isActionModified(action)
        font = font.deriveFont(if (modified) Font.BOLD else Font.PLAIN)
      }
      return comp
    }
  }

  companion object {
    const val ID = "keymap"
  }
}
