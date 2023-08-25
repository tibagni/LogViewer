package com.tibagni.logviewer.preferences

import java.awt.Font
import java.io.File

interface LogViewerPreferences {
    var defaultFiltersPath: File
    var lastFilterPaths: Array<File>
    var defaultLogsPath: File
    var lookAndFeel: String
    var openLastFilter: Boolean
    var reapplyFiltersAfterEdit: Boolean
    var rememberAppliedFilters: Boolean
    var preferredTextEditor: File?
    var collapseAllGroupsStartup: Boolean
    var showLineNumbers: Boolean
    var globalCustomFont: Font

    fun setAppliedFiltersIndices(group: String, indices: List<Int>)
    fun getAppliedFiltersIndices(group: String): List<Int>
    fun addPreferenceListener(listener: Listener)
    fun removePreferenceListener(listener: Listener)

    interface Listener {
        fun onLookAndFeelChanged()
        fun onDefaultFiltersPathChanged()
        fun onLastFilterPathChanged()
        fun onOpenLastFilterChanged()
        fun onDefaultLogsPathChanged()
        fun onReapplyFiltersConfigChanged()
        fun onRememberAppliedFiltersConfigChanged()
        fun onPreferredTextEditorChanged()
        fun onCollapseAllGroupsStartupChanged()
        fun onShowLineNumbersChanged()
        fun onGlobalCustomFontChanged()
    }

    abstract class Adapter : Listener {
        override fun onLookAndFeelChanged() {}
        override fun onDefaultFiltersPathChanged() {}
        override fun onLastFilterPathChanged() {}
        override fun onOpenLastFilterChanged() {}
        override fun onDefaultLogsPathChanged() {}
        override fun onReapplyFiltersConfigChanged() {}
        override fun onRememberAppliedFiltersConfigChanged() {}
        override fun onPreferredTextEditorChanged() {}
        override fun onCollapseAllGroupsStartupChanged() {}
        override fun onShowLineNumbersChanged() {}
        override fun onGlobalCustomFontChanged() {}
    }
}