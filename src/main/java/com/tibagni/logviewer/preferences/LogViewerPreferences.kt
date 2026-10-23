package com.tibagni.logviewer.preferences

import java.io.File

interface LogViewerPreferences {
    var defaultFiltersPath: File
    var lastFilterPaths: Array<File>
    var defaultLogsPath: File
    var lookAndFeel: String
    /**
     * The accent color chosen by the user (e.g. "Default", "Blue", "Purple",
     * "Teal", "Red", "Orange", "Green").
     */
    var accentColor: String
    var openLastFilter: Boolean
    var reapplyFiltersAfterEdit: Boolean
    var rememberAppliedFilters: Boolean
    var preferredTextEditor: File?
    var adbPath: File?
    var collapseAllGroupsStartup: Boolean
    var showLineNumbers: Boolean
    var showLogLineDividers: Boolean
    var applyFilterOnCheck: Boolean
    /**
     * The font size used for log text and filter lines.
     */
    var logFontSize: Int
    /**
     * The font family used for log text (e.g., "Monospaced", "JetBrains Mono").
     */
    var logFontFamily: String
    /**
     * Whether the log text should be bold.
     */
    var logFontBold: Boolean

    fun setAppliedFiltersIndices(group: String, indices: List<Int>)
    fun getAppliedFiltersIndices(group: String): List<Int>
    fun getCustomShortcut(actionId: String): String? = null
    fun setCustomShortcut(actionId: String, serializedKeyStroke: String?) {}
    fun applyCustomShortcuts(shortcuts: Map<String, String?>) {}
    fun addPreferenceListener(listener: Listener)
    fun removePreferenceListener(listener: Listener)

    interface Listener {
        fun onLookAndFeelChanged()
        /**
         * Invoked when the accent color preference changes.
         */
        fun onAccentColorChanged() {}
        fun onDefaultFiltersPathChanged()
        fun onLastFilterPathChanged()
        fun onOpenLastFilterChanged()
        fun onDefaultLogsPathChanged()
        fun onReapplyFiltersConfigChanged()
        fun onRememberAppliedFiltersConfigChanged()
        fun onPreferredTextEditorChanged()
        fun onAdbPathChanged() {}
        fun onCollapseAllGroupsStartupChanged()
        fun onShowLineNumbersChanged()
        fun onShowLogLineDividersChanged() {}
        fun onApplyFiltersOnCheckChanged() {}
        fun onLogFontSizeChanged() {}
        fun onLogFontFamilyChanged() {}
        fun onLogFontBoldChanged() {}
        fun onShortcutsChanged() {}
    }

    abstract class Adapter : Listener {
        override fun onLookAndFeelChanged() {}
        override fun onAccentColorChanged() {}
        override fun onDefaultFiltersPathChanged() {}
        override fun onLastFilterPathChanged() {}
        override fun onOpenLastFilterChanged() {}
        override fun onDefaultLogsPathChanged() {}
        override fun onReapplyFiltersConfigChanged() {}
        override fun onRememberAppliedFiltersConfigChanged() {}
        override fun onPreferredTextEditorChanged() {}
        override fun onAdbPathChanged() {}
        override fun onCollapseAllGroupsStartupChanged() {}
        override fun onShowLineNumbersChanged() {}
        override fun onShowLogLineDividersChanged() {}
        override fun onApplyFiltersOnCheckChanged() {}
        override fun onLogFontSizeChanged() {}
        override fun onLogFontFamilyChanged() {}
        override fun onLogFontBoldChanged() {}
        override fun onShortcutsChanged() {}
    }
}