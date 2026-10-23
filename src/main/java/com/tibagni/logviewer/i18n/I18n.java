package com.tibagni.logviewer.i18n;

import com.tibagni.logviewer.logger.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public final class I18n {
  private static final String BUNDLE_BASE_NAME = "properties.strings";
  private static final ResourceBundle.Control UTF8_CONTROL = new Utf8ResourceBundleControl();

  private static Locale currentLocale = Locale.getDefault();
  private static ResourceBundle bundle = loadBundle(currentLocale);

  // Common
  public static final String COMMON_OK = "common.ok";
  public static final String COMMON_CANCEL = "common.cancel";
  public static final String COMMON_CLOSE = "common.close";
  public static final String COMMON_CLEAR = "common.clear";
  public static final String COMMON_DELETE = "common.delete";
  public static final String COMMON_EDIT = "common.edit";
  public static final String COMMON_ERROR = "common.error";
  public static final String COMMON_DONE = "common.done";
  public static final String COMMON_SAVE = "common.save";
  public static final String COMMON_ALL = "common.all";
  public static final String COMMON_REMOVE = "common.remove";
  public static final String COMMON_CONFIRM_TITLE = "common.confirm_title";
  public static final String COMMON_SELECT_UNSELECT_ALL = "common.select_unselect_all";

  // Preferences Dialog
  public static final String PREF_DIALOG_TITLE = "pref.dialog.title";
  public static final String PREF_LOOK_AND_FEEL = "pref.look_and_feel";
  public static final String PREF_ACCENT_COLOR = "pref.accent_color";
  public static final String PREF_LOG_FONT_SIZE = "pref.log_font_size";
  public static final String PREF_LOG_FONT_BOLD = "pref.log_font_bold";
  public static final String PREF_DEFAULT_LOGS_PATH = "pref.default_logs_path";
  public static final String PREF_DEFAULT_FILTERS_PATH = "pref.default_filters_path";
  public static final String PREF_OPEN_LAST_FILTER = "pref.open_last_filter";
  public static final String PREF_APPLY_FILTERS_AFTER_EDIT = "pref.apply_filters_after_edit";
  public static final String PREF_REMEMBER_APPLIED_FILTERS = "pref.remember_applied_filters";
  public static final String PREF_COLLAPSE_ALL_GROUPS_STARTUP = "pref.collapse_all_groups_startup";
  public static final String PREF_SHOW_LINE_NUMBERS = "pref.show_line_numbers";
  public static final String PREF_SHOW_LOG_DIVIDERS = "pref.show_log_dividers";
  public static final String PREF_APPLY_FILTERS_ON_CHECK = "pref.apply_filters_on_check";
  public static final String PREF_PREFERRED_TEXT_EDITOR = "pref.preferred_text_editor";
  public static final String PREF_ADB_PATH = "pref.adb_path";
  public static final String PREF_LOG_FONT_FAMILY = "pref.log_font_family";
  public static final String PREF_CATEGORY_APPEARANCE = "pref.category.appearance";
  public static final String PREF_CATEGORY_LOGS = "pref.category.logs";
  public static final String PREF_CATEGORY_FILTERS = "pref.category.filters";
  public static final String PREF_CATEGORY_TOOLS = "pref.category.tools";
  public static final String PREF_CATEGORY_KEYMAP = "pref.category.keymap";
  public static final String PREF_SAVING_CHANGES = "pref.saving_changes";

  // Main View & File Choosers
  public static final String MAIN_FILECHOOSER_OPEN_LOGS = "main.filechooser.open.logs";
  public static final String MAIN_FILECHOOSER_OPEN_LOG = "main.filechooser.open.log";
  public static final String MAIN_FILECHOOSER_SAVE_FILTERED_LOGS = "main.filechooser.save.filtered.logs";
  public static final String MAIN_FILECHOOSER_SAVE_FILTER = "main.filechooser.save.filter";
  public static final String MAIN_FILECHOOSER_OPEN_FILTERS = "main.filechooser.open.filters";
  public static final String MAIN_FILECHOOSER_FILTER_FILES = "main.filechooser.filter.files";

  // Menus
  public static final String MENU_FILE = "main.menu.file";
  public static final String MENU_ITEM_SETTINGS = "main.menu.file.settings";
  public static final String MENU_LOGS = "main.menu.logs";
  public static final String MENU_ITEM_OPEN_LOGS = "main.menu.logs.open";
  public static final String MENU_ITEM_REFRESH = "main.menu.logs.refresh";
  public static final String MENU_ITEM_CLOSE_LOGS = "main.menu.logs.close";
  public static final String MENU_ENCODING = "main.menu.logs.encoding";
  public static final String MENU_ITEM_SAVE_FILTERED_LOGS = "main.menu.logs.save.filtered";
  public static final String MENU_ITEM_GO_TO_TIMESTAMP = "main.menu.logs.goto.timestamp";
  public static final String MENU_ITEM_VISIBLE_LOGS = "main.menu.logs.visible";
  public static final String MENU_FILTERS = "main.menu.filters";
  public static final String MENU_ITEM_OPEN_FILTERS = "main.menu.filters.open";
  public static final String MENU_ITEM_FIND_FILTERS = "main.menu.filters.find";
  public static final String MENU_ITEM_CLEAN_DUPLICATE_FILTERS = "main.menu.filters.clean.duplicates";
  public static final String MENU_STREAMS = "main.menu.streams";
  public static final String MENU_HELP = "main.menu.help";
  public static final String MENU_ITEM_KEYBOARD_SHORTCUTS = "main.menu.help.shortcuts";
  public static final String MENU_ITEM_ABOUT = "main.menu.help.about";
  public static final String MENU_ITEM_USER_GUIDE = "main.menu.help.user.guide";

  // ADB Operations & Errors
  public static final String ADB_DEVICES_RESTART_SUCCESS = "adb.dialog.devices.restart_success";
  public static final String ADB_DEVICES_RESTART_FAILED = "adb.dialog.devices.restart_failed";
  public static final String ADB_SHELL_TERMINAL_FAILED = "adb.dialog.shell.terminal_failed";
  public static final String ADB_ERROR_TITLE = "adb.dialog.error.title";

  // Encoding Submenu
  public static final String MENU_ITEM_ENCODING_ASCII = "main.menu.encoding.ascii";
  public static final String MENU_ITEM_ENCODING_LATIN = "main.menu.encoding.latin";
  public static final String MENU_ITEM_ENCODING_UTF8 = "main.menu.encoding.utf8";
  public static final String MENU_ITEM_ENCODING_UTF16_BE = "main.menu.encoding.utf16.be";
  public static final String MENU_ITEM_ENCODING_UTF16_LE = "main.menu.encoding.utf16.le";
  public static final String MENU_ITEM_ENCODING_UTF16 = "main.menu.encoding.utf16";

  // Main Tabs
  public static final String MAIN_TAB_LOGS = "main.tab.logs";
  public static final String MAIN_TAB_BUG_REPORT = "main.tab.bugreport";
  public static final String MAIN_TAB_LIVE_LOGS = "main.tab.live_logs";

  // Live Logs
  public static final String LIVE_LOGS_START = "live.logs.start";
  public static final String LIVE_LOGS_TERMINAL = "live.logs.terminal";
  public static final String LIVE_LOGS_CHANGE_DEVICE = "live.logs.change_device";
  public static final String LIVE_LOGS_STOP = "live.logs.stop";
  public static final String LIVE_LOGS_REFRESH = "live.logs.refresh";
  public static final String LIVE_LOGS_RESTART_SERVER = "live.logs.restart_server";
  public static final String LIVE_LOGS_RESTARTING_SERVER = "live.logs.restarting_server";
  public static final String LIVE_LOGS_AUTOSCROLL = "live.logs.autoscroll";
  public static final String LIVE_LOGS_STATUS_STREAMING = "live.logs.status.streaming";
  public static final String LIVE_LOGS_STATUS_STOPPED = "live.logs.status.stopped";
  public static final String LIVE_LOGS_STATUS_SUMMARY = "live.logs.status.summary";
  public static final String LIVE_LOGS_LABEL_DEVICE = "live.logs.label.device";
  public static final String LIVE_LOGS_LABEL_STATUS = "live.logs.label.status";
  public static final String LIVE_LOGS_CONNECTED_DEVICES = "live.logs.connected_devices";
  public static final String LIVE_LOGS_SCANNING = "live.logs.scanning";
  public static final String LIVE_LOGS_NO_DEVICES_TITLE = "live.logs.no_devices_title";
  public static final String LIVE_LOGS_NO_DEVICES_MSG = "live.logs.no_devices_msg";
  public static final String LIVE_LOGS_ADB_NOT_FOUND_TITLE = "live.logs.adb_not_found_title";
  public static final String LIVE_LOGS_ADB_NOT_FOUND_MSG = "live.logs.adb_not_found_msg";
  public static final String LIVE_LOGS_CONFIGURE_ADB = "live.logs.configure_adb";
  public static final String LIVE_LOGS_DEVICE_UNAUTHORIZED = "live.logs.device_unauthorized";
  public static final String LIVE_LOGS_EXPAND_SIDEBAR = "live.logs.expand_sidebar";
  public static final String LIVE_LOGS_COLLAPSE_SIDEBAR = "live.logs.collapse_sidebar";
  public static final String LIVE_LOGS_BUFFERS = "live.logs.buffers";
  public static final String LIVE_LOGS_NO_BUFFERS_SELECTED_TOOLTIP = "live.logs.no_buffers_selected_tooltip";

  // Edit Filter Dialog
  public static final String FILTER_DIALOG_EDIT_TITLE = "filter.dialog.edit_title";
  public static final String FILTER_DIALOG_NEW_TITLE = "filter.dialog.new_title";
  public static final String FILTER_LABEL_NAME = "filter.dialog.name";
  public static final String FILTER_TOOLTIP_NAME = "filter.dialog.name.tooltip";
  public static final String FILTER_LABEL_REGEX = "filter.dialog.regex";
  public static final String FILTER_TOOLTIP_REGEX = "filter.dialog.regex.tooltip";
  public static final String FILTER_BTN_REGEX_EDITOR = "filter.dialog.editor";
  public static final String FILTER_TOOLTIP_REGEX_EDITOR = "filter.dialog.editor.tooltip";
  public static final String FILTER_LABEL_CASE_SENSITIVE = "filter.dialog.case_sensitive";
  public static final String FILTER_LABEL_ENABLE_CASE_SENSITIVE = "filter.dialog.enable_case_sensitive";
  public static final String FILTER_LABEL_VERBOSITY = "filter.dialog.verbosity";
  public static final String FILTER_LABEL_COLOR = "filter.dialog.color";
  public static final String FILTER_TOOLTIP_COLOR = "filter.dialog.color.tooltip";
  public static final String FILTER_PREVIEW_COLOR = "filter.dialog.preview_color";
  public static final String FILTER_DUPLICATE_WARNING_TITLE = "filter.dialog.duplicate.title";
  public static final String FILTER_DUPLICATE_WARNING_MSG = "filter.dialog.duplicate.msg";
  public static final String FILTER_DUPLICATE_ADD_ANYWAY = "filter.dialog.duplicate.add_anyway";

  // Regex Editor Dialog
  public static final String REGEX_EDITOR_DIALOG_TITLE = "regex.editor.title";
  public static final String REGEX_EDITOR_LABEL_REGEX = "regex.editor.regex";
  public static final String REGEX_EDITOR_LABEL_PREVIEW = "regex.editor.preview";
  public static final String REGEX_EDITOR_LABEL_FLAGS = "regex.editor.flags";
  public static final String REGEX_EDITOR_CASE_SENSITIVE = "regex.editor.case_sensitive";
  public static final String REGEX_EDITOR_SAMPLE_TEXT = "regex.editor.sample_text";

  // Search Filters Dialog
  public static final String SEARCH_FILTERS_DIALOG_TITLE = "filters.search.title";
  public static final String SEARCH_FILTERS_SEARCH_HINT = "filters.search.search_hint";
  public static final String SEARCH_FILTERS_TOOLTIP_CLEAR = "filters.search.clear_tooltip";
  public static final String SEARCH_FILTERS_BTN_JUMP = "filters.search.btn_jump";
  public static final String SEARCH_FILTERS_HINT_SHORTCUTS = "filters.search.hint_shortcuts";
  public static final String SEARCH_FILTERS_MENU_JUMP = "filters.search.menu.jump";
  public static final String SEARCH_FILTERS_MENU_EDIT = "filters.search.menu.edit";
  public static final String SEARCH_FILTERS_MENU_DELETE = "filters.search.menu.delete";
  public static final String SEARCH_FILTERS_SHOWING_ALL = "filters.search.showing_all";
  public static final String SEARCH_FILTERS_SHOWING_MATCHES = "filters.search.showing_matches";
  public static final String SEARCH_FILTERS_DELETE_CONFIRM_TITLE = "filters.search.delete_confirm_title";
  public static final String SEARCH_FILTERS_DELETE_CONFIRM_MSG = "filters.search.delete_confirm_msg";
  public static final String SEARCH_FILTERS_COL_APPLIED = "filters.search.col.applied";
  public static final String SEARCH_FILTERS_COL_NAME = "filters.search.col.name";
  public static final String SEARCH_FILTERS_COL_PATTERN = "filters.search.col.pattern";
  public static final String SEARCH_FILTERS_COL_GROUP = "filters.search.col.group";

  // Filter Deduplication Dialog
  public static final String FILTER_DEDUP_DIALOG_TITLE = "filters.dedup.title";
  public static final String FILTER_DEDUP_BTN_CLEAN = "filters.dedup.btn_clean";
  public static final String FILTER_DEDUP_CLEAN_CROSS_FILE = "filters.dedup.clean_cross_file";
  public static final String FILTER_DEDUP_HEADER_TITLE = "filters.dedup.header_title";
  public static final String FILTER_DEDUP_HEADER_MSG = "filters.dedup.header_msg";
  public static final String FILTER_DEDUP_CLUSTER_TITLE = "filters.dedup.cluster_title";
  public static final String FILTER_DEDUP_CLUSTER_ENABLE = "filters.dedup.cluster_enable";
  public static final String FILTER_DEDUP_KEEP_IN_GROUP = "filters.dedup.keep_in_group";
  public static final String FILTER_DEDUP_SAME_GROUP_INFO = "filters.dedup.same_group_info";
  public static final String FILTER_DEDUP_CROSS_GROUP_ITEM = "filters.dedup.cross_group_item";

  // Filters List & Actions
  public static final String FILTERS_TOOLTIP_PREV = "filters.tooltip.prev";
  public static final String FILTERS_TOOLTIP_NEXT = "filters.tooltip.next";
  public static final String FILTERS_TOOLTIP_PREV_FORMAT = "filters.tooltip.prev.format";
  public static final String FILTERS_TOOLTIP_NEXT_FORMAT = "filters.tooltip.next.format";
  public static final String FILTERS_TOOLTIP_ADD = "filters.tooltip.add";
  public static final String FILTERS_TOOLTIP_SAVE = "filters.tooltip.save";
  public static final String FILTERS_TOOLTIP_CLOSE_GROUP = "filters.tooltip.close_group";
  public static final String FILTERS_TOOLTIP_SELECT_ALL = "filters.tooltip.select_all";
  public static final String FILTERS_MENU_ITEMS_SELECTED = "filters.menu.items_selected";
  public static final String FILTERS_MENU_DUPLICATE = "filters.menu.duplicate";
  public static final String FILTERS_MENU_MOVE = "filters.menu.move";
  public static final String FILTERS_PROGRESS_APPLYING = "filters.progress.applying";

  // Filter Model & Validation Errors
  public static final String FILTER_ERROR_EMPTY_FIELDS = "filter.error.empty_fields";
  public static final String FILTER_ERROR_WRONG_COLOR_FORMAT = "filter.error.wrong_color_format";
  public static final String FILTER_ERROR_WRONG_FORMAT = "filter.error.wrong_format";
  public static final String FILTER_ERROR_INVALID_PATTERN = "filter.error.invalid_pattern";

  // Search in tables (SearchableTable)
  public static final String SEARCH_HINT = "search.hint";
  public static final String SEARCH_MATCH_CASE = "search.match_case";
  public static final String SEARCH_TOOLTIP_HIDE = "search.tooltip.hide";
  public static final String SEARCH_BAD_PATTERN = "search.bad_pattern";
  public static final String SEARCH_RESULTS_COUNT = "search.results_count";

  // Filters Panel & Controls (LogViewerView)
  public static final String FILTERS_PANEL_TITLE = "filters.panel.title";
  public static final String FILTERS_BTN_NEW_GROUP = "filters.btn.new_group";
  public static final String FILTERS_BTN_APPLY = "filters.btn.apply";
  public static final String FILTERS_TOOLTIP_APPLY = "filters.tooltip.apply";
  public static final String FILTERS_BTN_FIND = "filters.btn.find";
  public static final String FILTERS_TOOLTIP_FIND = "filters.tooltip.find";
  public static final String FILTERS_TOOLTIP_MORE_OPTIONS = "filters.tooltip.more_options";
  public static final String FILTERS_MENU_CLOSE_ALL_GROUPS = "filters.menu.close_all_groups";
  public static final String FILTERS_TOOLTIP_CLOSE_ALL_GROUPS = "filters.tooltip.close_all_groups";
  public static final String FILTERS_MENU_UNAPPLY_ALL = "filters.menu.unapply_all";
  public static final String FILTERS_TOOLTIP_UNAPPLY_ALL = "filters.tooltip.unapply_all";
  public static final String FILTERS_MENU_CLEAN_DUPLICATES = "filters.menu.clean_duplicates";
  public static final String FILTERS_TOOLTIP_CLEAN_DUPLICATES = "filters.tooltip.clean_duplicates";
  public static final String FILTERS_CONFIRM_CLOSE_ALL_GROUPS_MSG = "filters.confirm.close_all_groups";
  public static final String FILTERS_CONFIRM_CLOSE_GROUP_MSG = "filters.confirm.close_group";
  public static final String FILTERS_CONFIRM_DELETE_MSG = "filters.confirm.delete_selected";
  public static final String FILTERS_NEW_GROUP_TITLE = "filters.new_group.title";
  public static final String FILTERS_NEW_GROUP_MSG = "filters.new_group.msg";
  public static final String FILTERS_NO_FILTERS_TITLE = "filters.no_filters.title";
  public static final String FILTERS_NO_FILTERS_APPLY_MSG = "filters.no_filters.apply_msg";
  public static final String FILTERS_NO_FILTERS_SEARCH_MSG = "filters.no_filters.search_msg";
  public static final String FILTERS_MOVE_DIALOG_TITLE = "filters.move.dialog_title";
  public static final String FILTERS_MOVE_DIALOG_MSG = "filters.move.dialog_msg";
  public static final String FILTERS_MOVE_CREATE_NEW = "filters.move.create_new";
  public static final String FILTERS_OPEN_EXISTING_TITLE = "filters.open_existing.title";
  public static final String FILTERS_OPEN_EXISTING_MSG = "filters.open_existing.msg";
  public static final String FILTERS_OPEN_KEEP_EXISTING = "filters.open_existing.keep";
  public static final String FILTERS_OPEN_REPLACE_EXISTING = "filters.open_existing.replace";
  public static final String FILTERS_SAVE_UNSAVED_TITLE = "filters.save_unsaved.title";
  public static final String FILTERS_SAVE_UNSAVED_MSG = "filters.save_unsaved.msg";
  public static final String FILTERS_SAVE_MULTIPLE_TITLE = "filters.save_multiple.title";
  public static final String FILTERS_SAVE_MULTIPLE_MSG = "filters.save_multiple.msg";

  // Logs View & Context Actions (LogViewerView)
  public static final String LOGS_PATH_PREFIX = "logs.path_prefix";
  public static final String LOGS_STATUS_ALL = "logs.status.all";
  public static final String LOGS_STATUS_FILTERED = "logs.status.filtered";
  public static final String LOGS_STATUS_ALL_DROPPED = "logs.status.all.dropped";
  public static final String LOGS_STATUS_FILTERED_DROPPED =
      "logs.status.filtered.dropped";
  public static final String LOGS_TAB_ALL = "logs.tab.all";
  public static final String LOGS_TAB_FILTERED = "logs.tab.filtered";
  public static final String LOGS_TAB_MY_LOGS = "logs.tab.my_logs";
  public static final String LOGS_MY_LOGS_TOOLTIP = "logs.my_logs.tooltip";
  public static final String LOGS_MENU_IGNORE_BEFORE = "logs.menu.ignore_before";
  public static final String LOGS_MENU_IGNORE_AFTER = "logs.menu.ignore_after";
  public static final String LOGS_MENU_ADD_TO_MY_LOGS = "logs.menu.add_to_my_logs";
  public static final String LOGS_MENU_CREATE_FILTER = "logs.menu.create_filter";
  public static final String LOGS_MENU_FILTER_BY_PID = "logs.menu.filter_by_pid";
  public static final String LOGS_MENU_FILTER_BY_TAG = "logs.menu.filter_by_tag";
  public static final String LOGS_MENU_LOG_DETAILS = "logs.menu.log_details";
  public static final String LOGS_MENU_ELAPSED_TIME = "logs.menu.elapsed_time";
  public static final String LOGS_MENU_COPY_ELAPSED_TIME_SNIPPET = "logs.menu.copy_elapsed_time_snippet";
  public static final String LOGS_MENU_COPY_DURATION_ONLY = "logs.menu.copy_duration_only";
  public static final String ELAPSED_TIME_LABEL = "logs.elapsed_time_label";
  public static final String ELAPSED_TIME_BACKWARD = "logs.elapsed_time_backward";

  // Log Line Details Dialog
  public static final String LOG_DETAILS_TITLE = "logs.details.title";
  public static final String LOG_DETAILS_SOURCE_SECTION = "logs.details.source_section";
  public static final String LOG_DETAILS_METADATA_SECTION = "logs.details.metadata_section";
  public static final String LOG_DETAILS_MESSAGE_SECTION = "logs.details.message_section";
  public static final String LOG_DETAILS_FILE_NAME = "logs.details.file_name";
  public static final String LOG_DETAILS_FILE_PATH = "logs.details.file_path";
  public static final String LOG_DETAILS_LINE_NUMBER = "logs.details.line_number";
  public static final String LOG_DETAILS_LOG_INDEX = "logs.details.log_index";
  public static final String LOG_DETAILS_STREAM = "logs.details.stream";
  public static final String LOG_DETAILS_TIMESTAMP = "logs.details.timestamp";
  public static final String LOG_DETAILS_LEVEL = "logs.details.level";
  public static final String LOG_DETAILS_PID = "logs.details.pid";
  public static final String LOG_DETAILS_TID = "logs.details.tid";
  public static final String LOG_DETAILS_TAG = "logs.details.tag";
  public static final String LOG_DETAILS_FILTER = "logs.details.filter";
  public static final String LOG_DETAILS_COPY_PATH = "logs.details.copy_path";
  public static final String LOG_DETAILS_COPY_LINE = "logs.details.copy_line";
  public static final String LOG_DETAILS_COPY_MESSAGE = "logs.details.copy_message";
  public static final String LOG_DETAILS_COPY_TAG = "logs.details.copy_tag";
  public static final String LOG_DETAILS_OPEN_EDITOR = "logs.details.open_editor";
  public static final String LOG_DETAILS_BTN_FILTER_PID = "logs.details.btn_filter_pid";
  public static final String LOG_DETAILS_BTN_FILTER_TAG = "logs.details.btn_filter_tag";
  public static final String LOG_DETAILS_OPEN_EDITOR_ERROR = "logs.details.open_editor_error";
  public static final String LOG_DETAILS_NO_EDITOR_CONFIGURED_MSG = "logs.details.no_editor_msg";
  public static final String LOGS_SELECT_FILTER_GROUP_TITLE = "logs.select_group.title";
  public static final String LOGS_SELECT_FILTER_GROUP_MSG = "logs.select_group.msg";
  public static final String LOGS_GOTO_TIMESTAMP_TITLE = "logs.goto_timestamp.title";
  public static final String LOGS_GOTO_TIMESTAMP_MSG = "logs.goto_timestamp.msg";
  public static final String LOGS_SKIPPED_FILES_TITLE = "logs.skipped_files.title";
  public static final String LOGS_SKIPPED_FILES_MSG = "logs.skipped_files.msg";
  public static final String LOGS_TIMESTAMP_PARSE_ERROR_TITLE = "logs.timestamp_error.title";
  public static final String LOGS_TIMESTAMP_PARSE_ERROR_MSG = "logs.timestamp_error.msg";

  // Visible Logs Configuration (VisibleLogsConfigurationDialog)
  public static final String VISIBLE_LOGS_TITLE = "visible_logs.title";
  public static final String VISIBLE_LOGS_FIRST_LABEL = "visible_logs.first_log_label";
  public static final String VISIBLE_LOGS_LAST_LABEL = "visible_logs.last_log_label";

  // Bugreport View & Sections
  public static final String BUGREPORT_OPEN_HINT = "bugreport.open_hint";
  public static final String BUGREPORT_SUBS_SERVICE_DUMP = "bugreport.subs.service_dump";
  public static final String BUGREPORT_SUBS_ACTIVE = "bugreport.subs.active";
  public static final String BUGREPORT_SUBS_ALL = "bugreport.subs.all";
  public static final String BUGREPORT_CARRIER_CONFIG_LOADING_LOGS = "bugreport.carrier_config.loading_logs";

  // About Dialog
  public static final String ABOUT_DIALOG_TITLE = "about.dialog_title";
  public static final String ABOUT_VERSION_FORMAT = "about.version_format";
  public static final String ABOUT_OPEN_SOURCE_INFO = "about.open_source_info";
  public static final String ABOUT_CHECKING_UPDATES = "about.checking_updates";
  public static final String ABOUT_UPDATE_AVAILABLE = "about.update_available";
  public static final String ABOUT_UPDATE_TO = "about.update_to";
  public static final String ABOUT_UP_TO_DATE = "about.up_to_date";
  public static final String ABOUT_CHECK_UPDATE_FAILED = "about.check_update_failed";
  public static final String ABOUT_UPDATE_BTN = "about.update_btn";

  // Updates Dialog
  public static final String UPDATES_TITLE = "updates.title";
  public static final String UPDATES_NEW_VERSION_AVAILABLE = "updates.new_version_available";
  public static final String UPDATES_NEW_VERSION_LABEL = "updates.new_version_label";
  public static final String UPDATES_RELEASE_NOTES_LABEL = "updates.release_notes_label";

  // Keyboard Shortcuts & Keymap Settings
  public static final String SHORTCUT_CATEGORY_FILE = "shortcuts.category.file";
  public static final String SHORTCUT_CATEGORY_NAVIGATION = "shortcuts.category.navigation";
  public static final String SHORTCUT_CATEGORY_LOGS = "shortcuts.category.logs";
  public static final String SHORTCUT_CATEGORY_FILTERS = "shortcuts.category.filters";
  public static final String SHORTCUT_CATEGORY_SEARCH = "shortcuts.category.search";

  public static final String SHORTCUT_ACTION_FILE_SETTINGS = "shortcuts.action.file.settings";
  public static final String SHORTCUT_ACTION_LOGS_OPEN = "shortcuts.action.logs.open";
  public static final String SHORTCUT_ACTION_LOGS_REFRESH = "shortcuts.action.logs.refresh";
  public static final String SHORTCUT_ACTION_LOGS_CLOSE = "shortcuts.action.logs.close";
  public static final String SHORTCUT_ACTION_LOGS_GOTO_TIMESTAMP = "shortcuts.action.logs.goto_timestamp";
  public static final String SHORTCUT_ACTION_LOGS_INSPECT_LINE = "shortcuts.action.logs.inspect_line";
  public static final String SHORTCUT_ACTION_LOGS_REMOVE_MY_LOG = "shortcuts.action.logs.remove_my_log";
  public static final String SHORTCUT_ACTION_NAV_TAB_LOGS = "shortcuts.action.nav.tab_logs";
  public static final String SHORTCUT_ACTION_NAV_TAB_BUGREPORT = "shortcuts.action.nav.tab_bugreport";
  public static final String SHORTCUT_ACTION_NAV_TAB_LIVE_LOGS = "shortcuts.action.nav.tab_live_logs";
  public static final String SHORTCUT_ACTION_FILTERS_OPEN = "shortcuts.action.filters.open";
  public static final String SHORTCUT_ACTION_FILTERS_FIND_GLOBAL = "shortcuts.action.filters.find_global";
  public static final String SHORTCUT_ACTION_FILTERS_INLINE_SEARCH = "shortcuts.action.filters.inline_search";
  public static final String SHORTCUT_ACTION_FILTERS_TOGGLE_APPLIED = "shortcuts.action.filters.toggle_applied";
  public static final String SHORTCUT_ACTION_FILTERS_PREV_MATCH = "shortcuts.action.filters.prev_match";
  public static final String SHORTCUT_ACTION_FILTERS_NEXT_MATCH = "shortcuts.action.filters.next_match";
  public static final String SHORTCUT_ACTION_FILTERS_EDIT_SELECTED = "shortcuts.action.filters.edit_selected";
  public static final String SHORTCUT_ACTION_FILTERS_DELETE_SELECTED = "shortcuts.action.filters.delete_selected";
  public static final String SHORTCUT_ACTION_SEARCH_FIND_IN_TABLE = "shortcuts.action.search.find_in_table";
  public static final String SHORTCUT_ACTION_SEARCH_NEXT_MATCH = "shortcuts.action.search.next_match";
  public static final String SHORTCUT_ACTION_SEARCH_PREV_MATCH = "shortcuts.action.search.prev_match";

  public static final String KEYMAP_SEARCH_HINT = "keymap.search_hint";
  public static final String KEYMAP_COL_CATEGORY = "keymap.col.category";
  public static final String KEYMAP_COL_ACTION = "keymap.col.action";
  public static final String KEYMAP_COL_SHORTCUT = "keymap.col.shortcut";
  public static final String KEYMAP_BTN_CHANGE = "keymap.btn.change";
  public static final String KEYMAP_BTN_RESET_SELECTED = "keymap.btn.reset_selected";
  public static final String KEYMAP_BTN_RESET_ALL = "keymap.btn.reset_all";
  public static final String KEYMAP_READ_ONLY_BADGE = "keymap.read_only_badge";
  public static final String KEYMAP_CAPTURE_TITLE = "keymap.capture.title";
  public static final String KEYMAP_CAPTURE_PROMPT = "keymap.capture.prompt";
  public static final String KEYMAP_CONFLICT_TITLE = "keymap.conflict.title";
  public static final String KEYMAP_CONFLICT_MSG = "keymap.conflict.msg";
  public static final String KEYMAP_CONFLICT_READONLY_MSG = "keymap.conflict.readonly_msg";

  private I18n() {}

  public static synchronized void setLocale(Locale locale) {
    currentLocale = locale != null ? locale : Locale.getDefault();
    bundle = loadBundle(currentLocale);
  }

  public static synchronized Locale getLocale() {
    return currentLocale;
  }

  public static String get(String key) {
    try {
      return bundle.getString(key);
    } catch (MissingResourceException e) {
      Logger.error("Missing translation for key: " + key);
      return "!" + key + "!";
    }
  }

  public static String format(String key, Object... args) {
    String pattern = get(key);
    try {
      return MessageFormat.format(pattern, args);
    } catch (Exception e) {
      Logger.error("Failed to format translation for key: " + key, e);
      return pattern;
    }
  }

  private static ResourceBundle loadBundle(Locale locale) {
    try {
      return ResourceBundle.getBundle(BUNDLE_BASE_NAME, locale, UTF8_CONTROL);
    } catch (MissingResourceException e) {
      Logger.error("Failed to load resource bundle for locale: " + locale, e);
      return ResourceBundle.getBundle(BUNDLE_BASE_NAME, Locale.ROOT, UTF8_CONTROL);
    }
  }

  private static class Utf8ResourceBundleControl extends ResourceBundle.Control {
    @Override
    public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                    ClassLoader loader, boolean reload)
        throws IllegalAccessException, InstantiationException, IOException {
      String bundleName = toBundleName(baseName, locale);
      String resourceName = toResourceName(bundleName, "properties");
      try (InputStream stream = loader.getResourceAsStream(resourceName)) {
        if (stream != null) {
          try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new PropertyResourceBundle(reader);
          }
        }
      }
      return super.newBundle(baseName, locale, format, loader, reload);
    }
  }
}
