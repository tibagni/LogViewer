package com.tibagni.logviewer.shortcuts

import com.tibagni.logviewer.i18n.I18n
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * Logical grouping for keyboard shortcuts in the Settings UI and generated documentation.
 */
enum class ShortcutCategory(val titleKey: String) {
  FILE(I18n.SHORTCUT_CATEGORY_FILE),
  LOGS(I18n.SHORTCUT_CATEGORY_LOGS),
  FILTERS(I18n.SHORTCUT_CATEGORY_FILTERS),
  SEARCH(I18n.SHORTCUT_CATEGORY_SEARCH),
  NAVIGATION(I18n.SHORTCUT_CATEGORY_NAVIGATION);

  val displayName: String
    get() = I18n.get(titleKey)
}

/**
 * Activation scope of a keyboard shortcut, used to detect conflicts within the same or global context.
 */
enum class ShortcutScope {
  /** Active anywhere in the main application window (menu accelerators and tab navigation). */
  GLOBAL,
  /** Active when a log table (All Logs, Filtered Logs, My Logs) has focus. */
  LOGS_TABLE,
  /** Active when searching inside a log table or search bar. */
  SEARCH_BAR,
  /** Active when a filter group list in the Filters panel has focus. */
  FILTERS_LIST;

  fun conflictsWith(other: ShortcutScope): Boolean {
    return this == other || this == GLOBAL || other == GLOBAL
  }
}

/**
 * Central catalog of all keyboard shortcuts in LogViewer.
 */
enum class ShortcutAction(
  val id: String,
  val category: ShortcutCategory,
  val scope: ShortcutScope,
  val nameKey: String,
  val defaultMacKeyStroke: KeyStroke,
  val defaultWinLinuxKeyStroke: KeyStroke,
  val isRebindable: Boolean = true,
  val readmeShortcutOverride: String? = null
) {
  FILE_SETTINGS(
    id = "file.settings",
    category = ShortcutCategory.FILE,
    scope = ShortcutScope.GLOBAL,
    nameKey = I18n.SHORTCUT_ACTION_FILE_SETTINGS,
    defaultMacKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, InputEvent.META_DOWN_MASK),
    defaultWinLinuxKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, InputEvent.CTRL_DOWN_MASK)
  ),

  LOGS_OPEN(
    id = "logs.open",
    category = ShortcutCategory.LOGS,
    scope = ShortcutScope.GLOBAL,
    nameKey = I18n.SHORTCUT_ACTION_LOGS_OPEN,
    defaultMacKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.META_DOWN_MASK),
    defaultWinLinuxKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK)
  ),
  LOGS_REFRESH(
    id = "logs.refresh",
    category = ShortcutCategory.LOGS,
    scope = ShortcutScope.GLOBAL,
    nameKey = I18n.SHORTCUT_ACTION_LOGS_REFRESH,
    defaultMacKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.META_DOWN_MASK),
    defaultWinLinuxKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)
  ),
  LOGS_CLOSE(
    id = "logs.close",
    category = ShortcutCategory.LOGS,
    scope = ShortcutScope.GLOBAL,
    nameKey = I18n.SHORTCUT_ACTION_LOGS_CLOSE,
    defaultMacKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.META_DOWN_MASK),
    defaultWinLinuxKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK)
  ),
  LOGS_GO_TO_TIMESTAMP(
    id = "logs.goto_timestamp",
    category = ShortcutCategory.LOGS,
    scope = ShortcutScope.GLOBAL,
    nameKey = I18n.SHORTCUT_ACTION_LOGS_GOTO_TIMESTAMP,
    defaultMacKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.META_DOWN_MASK),
    defaultWinLinuxKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK)
  ),
  LOGS_INSPECT_LINE(
    id = "logs.inspect_line",
    category = ShortcutCategory.LOGS,
    scope = ShortcutScope.LOGS_TABLE,
    nameKey = I18n.SHORTCUT_ACTION_LOGS_INSPECT_LINE,
    defaultMacKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.META_DOWN_MASK),
    defaultWinLinuxKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK)
  ),
  LOGS_REMOVE_MY_LOG(
    id = "logs.remove_my_log",
    category = ShortcutCategory.LOGS,
    scope = ShortcutScope.LOGS_TABLE,
    nameKey = I18n.SHORTCUT_ACTION_LOGS_REMOVE_MY_LOG,
    defaultMacKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
    defaultWinLinuxKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
    isRebindable = false
  ),

  FILTERS_OPEN(
    id = "filters.open",
    category = ShortcutCategory.FILTERS,
    scope = ShortcutScope.GLOBAL,
    nameKey = I18n.SHORTCUT_ACTION_FILTERS_OPEN,
    defaultMacKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.META_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK),
    defaultWinLinuxKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)
  ),
  FILTERS_FIND_GLOBAL(
    id = "filters.find_global",
    category = ShortcutCategory.FILTERS,
    scope = ShortcutScope.GLOBAL,
    nameKey = I18n.SHORTCUT_ACTION_FILTERS_FIND_GLOBAL,
    defaultMacKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.META_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK),
    defaultWinLinuxKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK)
  ),
  FILTERS_INLINE_SEARCH(
    id = "filters.inline_search",
    category = ShortcutCategory.FILTERS,
    scope = ShortcutScope.FILTERS_LIST,
    nameKey = I18n.SHORTCUT_ACTION_FILTERS_INLINE_SEARCH,
    defaultMacKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, 0),
    defaultWinLinuxKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, 0)
  ),
  FILTERS_PREV_MATCH(
    id = "filters.prev_match",
    category = ShortcutCategory.FILTERS,
    scope = ShortcutScope.FILTERS_LIST,
    nameKey = I18n.SHORTCUT_ACTION_FILTERS_PREV_MATCH,
    defaultMacKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, 0),
    defaultWinLinuxKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, 0),
    readmeShortcutOverride = "`, (<)`"
  ),
  FILTERS_NEXT_MATCH(
    id = "filters.next_match",
    category = ShortcutCategory.FILTERS,
    scope = ShortcutScope.FILTERS_LIST,
    nameKey = I18n.SHORTCUT_ACTION_FILTERS_NEXT_MATCH,
    defaultMacKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, 0),
    defaultWinLinuxKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, 0),
    readmeShortcutOverride = "`. (>)`"
  ),
  FILTERS_TOGGLE_APPLIED(
    id = "filters.toggle_applied",
    category = ShortcutCategory.FILTERS,
    scope = ShortcutScope.FILTERS_LIST,
    nameKey = I18n.SHORTCUT_ACTION_FILTERS_TOGGLE_APPLIED,
    defaultMacKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
    defaultWinLinuxKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
    isRebindable = false
  ),
  FILTERS_EDIT_SELECTED(
    id = "filters.edit_selected",
    category = ShortcutCategory.FILTERS,
    scope = ShortcutScope.FILTERS_LIST,
    nameKey = I18n.SHORTCUT_ACTION_FILTERS_EDIT_SELECTED,
    defaultMacKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
    defaultWinLinuxKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
    isRebindable = false
  ),
  FILTERS_DELETE_SELECTED(
    id = "filters.delete_selected",
    category = ShortcutCategory.FILTERS,
    scope = ShortcutScope.FILTERS_LIST,
    nameKey = I18n.SHORTCUT_ACTION_FILTERS_DELETE_SELECTED,
    defaultMacKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
    defaultWinLinuxKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
    isRebindable = false
  ),

  SEARCH_FIND_IN_TABLE(
    id = "search.find_in_table",
    category = ShortcutCategory.SEARCH,
    scope = ShortcutScope.LOGS_TABLE,
    nameKey = I18n.SHORTCUT_ACTION_SEARCH_FIND_IN_TABLE,
    defaultMacKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.META_DOWN_MASK),
    defaultWinLinuxKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK)
  ),
  SEARCH_NEXT_MATCH(
    id = "search.next_match",
    category = ShortcutCategory.SEARCH,
    scope = ShortcutScope.SEARCH_BAR,
    nameKey = I18n.SHORTCUT_ACTION_SEARCH_NEXT_MATCH,
    defaultMacKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0),
    defaultWinLinuxKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0),
    readmeShortcutOverride = "`F3` / `Enter`"
  ),
  SEARCH_PREV_MATCH(
    id = "search.prev_match",
    category = ShortcutCategory.SEARCH,
    scope = ShortcutScope.SEARCH_BAR,
    nameKey = I18n.SHORTCUT_ACTION_SEARCH_PREV_MATCH,
    defaultMacKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK),
    defaultWinLinuxKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK),
    readmeShortcutOverride = "`Shift+F3` / `Shift+Enter`"
  ),

  NAV_TAB_LOGS(
    id = "nav.tab_logs",
    category = ShortcutCategory.NAVIGATION,
    scope = ShortcutScope.GLOBAL,
    nameKey = I18n.SHORTCUT_ACTION_NAV_TAB_LOGS,
    defaultMacKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.META_DOWN_MASK),
    defaultWinLinuxKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.CTRL_DOWN_MASK)
  ),
  NAV_TAB_BUGREPORT(
    id = "nav.tab_bugreport",
    category = ShortcutCategory.NAVIGATION,
    scope = ShortcutScope.GLOBAL,
    nameKey = I18n.SHORTCUT_ACTION_NAV_TAB_BUGREPORT,
    defaultMacKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.META_DOWN_MASK),
    defaultWinLinuxKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.CTRL_DOWN_MASK)
  ),
  NAV_TAB_LIVE_LOGS(
    id = "nav.tab_live_logs",
    category = ShortcutCategory.NAVIGATION,
    scope = ShortcutScope.GLOBAL,
    nameKey = I18n.SHORTCUT_ACTION_NAV_TAB_LIVE_LOGS,
    defaultMacKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_3, InputEvent.META_DOWN_MASK),
    defaultWinLinuxKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_3, InputEvent.CTRL_DOWN_MASK)
  );

  val displayName: String
    get() = I18n.get(nameKey)

  fun getDefaultKeyStroke(isMac: Boolean): KeyStroke {
    return if (isMac) defaultMacKeyStroke else defaultWinLinuxKeyStroke
  }

  companion object {
    fun fromId(id: String): ShortcutAction? = values().firstOrNull { it.id == id }
  }
}
