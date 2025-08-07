package com.tibagni.logviewer.preferences

import javax.swing.filechooser.FileSystemView
import java.io.File
import java.util.HashSet
import java.util.prefs.Preferences

object LogViewerPreferencesImpl : LogViewerPreferences {

    /*Visible for Testing*/ const val FILTERS_PATH = "filters_path"
    /*Visible for Testing*/ const val LAST_FILTER_PATH = "last_filter_path"
    /*Visible for Testing*/ const val OPEN_LAST_FILTER = "open_last_filter"
    /*Visible for Testing*/ const val LOGS_PATH = "logs_path"
    /*Visible for Testing*/ const val LOOK_AND_FEEL = "look_and_feel"
    /*Visible for Testing*/ const val REAPPLY_FILTERS_AFTER_EDIT = "reapply_filters_after_edit"
    /*Visible for Testing*/ const val REMEMBER_APPLIED_FILTERS = "remember_applied_filters"
    /*Visible for Testing*/ const val REMEMBER_APPLIED_FILTERS_PREFIX = "applied_for_group_"
    /*Visible for Testing*/ const val PREFERRED_TEXT_EDITOR = "preferred_text_editor"
    /*Visible for Testing*/ const val COLLAPSE_ALL_GROUPS_STARTUP = "collapse_all_groups_startup"
    /*Visible for Testing*/ const val SHOW_LINE_NUMBERS = "show_line_numbers"
    /*Visible for Testing*/ const val REAPPLY_FILTERS_ON_CHANGE = "reapply_filters_on_change"
    /*Visible for Testing*/ const val AI_HOST = "ai_host"
    /*Visible for Testing*/ const val AI_MODEL = "ai_model"

    // Allow changing for tests
    private var preferences = Preferences.userRoot().node(javaClass.name)
    private val listeners = HashSet<LogViewerPreferences.Listener>()

    override var defaultFiltersPath: File
        get() {
            val path = preferences.get(FILTERS_PATH, null)

            return if (path != null)
                File(path)
            else
                FileSystemView.getFileSystemView().homeDirectory
        }
        set(defaultFolder) {
            preferences.put(FILTERS_PATH, defaultFolder.absolutePath)
            listeners.forEach { l -> l.onDefaultFiltersPathChanged() }
        }

    override var lastFilterPaths: Array<File>
        get() {
            val pathStrings = preferences.get(LAST_FILTER_PATH, null)
            val paths = pathStrings?.split("\\$".toRegex())

            return paths?.filter { it.isNotEmpty() }?.map { File(it) }?.toTypedArray() ?: arrayOf()
        }
        set(filterPaths) {
            val pathsString = filterPaths.joinToString(separator = "$") { it.absolutePath }
            preferences.put(LAST_FILTER_PATH, pathsString)
            listeners.forEach { l -> l.onLastFilterPathChanged() }
        }

    override var defaultLogsPath: File
        get() {
            val path = preferences.get(LOGS_PATH, null)

            return if (path != null)
                File(path)
            else
                FileSystemView.getFileSystemView().homeDirectory
        }
        set(defaultFolder) {
            preferences.put(LOGS_PATH, defaultFolder.absolutePath)
            listeners.forEach { l -> l.onDefaultLogsPathChanged() }
        }

    override var lookAndFeel: String
        get() = preferences.get(LOOK_AND_FEEL, "")
        set(lookAndFeel) {
            preferences.put(LOOK_AND_FEEL, lookAndFeel)
            listeners.forEach { l -> l.onLookAndFeelChanged() }
        }

    override var openLastFilter: Boolean
        get() = preferences.getBoolean(OPEN_LAST_FILTER, false)
        set(openLastFilter) {
            preferences.putBoolean(OPEN_LAST_FILTER, openLastFilter)
            listeners.forEach { l -> l.onOpenLastFilterChanged() }
        }

    override var reapplyFiltersAfterEdit: Boolean
        get() = preferences.getBoolean(REAPPLY_FILTERS_AFTER_EDIT, true)
        set(reApply) {
            preferences.putBoolean(REAPPLY_FILTERS_AFTER_EDIT, reApply)
            listeners.forEach { l -> l.onReapplyFiltersConfigChanged() }
        }

    override var rememberAppliedFilters: Boolean
        get() = preferences.getBoolean(REMEMBER_APPLIED_FILTERS, true)
        set(remember) {
            preferences.putBoolean(REMEMBER_APPLIED_FILTERS, remember)
            listeners.forEach { l -> l.onRememberAppliedFiltersConfigChanged() }
        }
    override var preferredTextEditor: File?
        get() {
            val path = preferences.get(PREFERRED_TEXT_EDITOR, null)
            return if (!path.isNullOrEmpty()) File(path) else null
        }
        set(textEditorFile) {
            preferences.put(PREFERRED_TEXT_EDITOR, textEditorFile?.absolutePath ?: "")
            listeners.forEach { l -> l.onPreferredTextEditorChanged() }
        }
    override var collapseAllGroupsStartup: Boolean
        get() = preferences.getBoolean(COLLAPSE_ALL_GROUPS_STARTUP, false)
        set(collapse) {
            preferences.putBoolean(COLLAPSE_ALL_GROUPS_STARTUP, collapse)
            listeners.forEach { l -> l.onCollapseAllGroupsStartupChanged() }
        }

    override var showLineNumbers: Boolean
        get() = preferences.getBoolean(SHOW_LINE_NUMBERS, true)
        set(show) {
            preferences.putBoolean(SHOW_LINE_NUMBERS, show)
            listeners.forEach { l -> l.onShowLineNumbersChanged() }
        }

    /**
     * If true, the filters will be re-applied every time when a checkbox is checked or unchecked.
     */
    override var applyFilterOnCheck: Boolean
        get() = preferences.getBoolean(REAPPLY_FILTERS_ON_CHANGE, true)
        set(reApply) {
            preferences.putBoolean(REAPPLY_FILTERS_ON_CHANGE, reApply)
            listeners.forEach { l -> l.onApplyFiltersOnCheckChanged() }
        }

    /**
     * The AI server host URL.
     * Default is "http://localhost:11434".
     */
    override var aiHost: String
        get() = preferences.get(AI_HOST, "http://localhost:11434")
        set(host) {
            preferences.put(AI_HOST, host)
            listeners.forEach { l -> l.onAiHostChanged() }
        }

    /**
     * The model to use for AI assistance.
     * Default is an empty string, which means no model is set.
     */
    override var aiModel: String
        get() = preferences.get(AI_MODEL, "")
        set(model) {
            preferences.put(AI_MODEL, model)
            listeners.forEach { l -> l.onAiModelChanged() }
        }

    override fun setAppliedFiltersIndices(group: String, indices: List<Int>) {
        preferences.put(REMEMBER_APPLIED_FILTERS_PREFIX + group, indices.joinToString(separator = ","))
    }

    override fun getAppliedFiltersIndices(group: String): List<Int> {
        val savedData = preferences.get(REMEMBER_APPLIED_FILTERS_PREFIX + group, "")
        return savedData.split(",".toRegex()).mapNotNull { it.toIntOrNull() }
    }

    override fun addPreferenceListener(listener: LogViewerPreferences.Listener) {
        listeners.add(listener)
    }

    override fun removePreferenceListener(listener: LogViewerPreferences.Listener) {
        listeners.remove(listener)
    }

    // For testing
    fun setMockPreferences(mockPrefs: Preferences) {
        preferences = mockPrefs
    }
}
