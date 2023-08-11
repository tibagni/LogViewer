package com.tibagni.logviewer.preferences

import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.File
import java.util.prefs.Preferences
import javax.swing.filechooser.FileSystemView

class LogViewerPreferencesImplTests {

    @Mock
    private lateinit var mockPrefs: Preferences

    @Mock
    private lateinit var mockListener: LogViewerPreferences.Listener

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        LogViewerPreferencesImpl.setMockPreferences(mockPrefs)
        LogViewerPreferencesImpl.addPreferenceListener(mockListener)
    }

    @Test
    fun testSettingDefaultFiltersPath() {
        val testFile = File("test")
        LogViewerPreferencesImpl.defaultFiltersPath = testFile

        verify(mockPrefs, times(1)).put(LogViewerPreferencesImpl.FILTERS_PATH, testFile.absolutePath)
        verify(mockListener, only()).onDefaultFiltersPathChanged()
    }

    @Test
    fun testGettingDefaultFiltersPath() {
        val testFile = File("test")

        `when`(mockPrefs.get(eq(LogViewerPreferencesImpl.FILTERS_PATH), any())).thenReturn(testFile.absolutePath)

        val returnedFile = LogViewerPreferencesImpl.defaultFiltersPath

        verify(mockPrefs, never()).put(any(), any())
        verify(mockListener, never()).onDefaultFiltersPathChanged()

        assertEquals(testFile.absolutePath, returnedFile.absolutePath)
    }

    @Test
    fun testGettingDefaultFiltersPathUnset() {
        `when`(mockPrefs.get(eq(LogViewerPreferencesImpl.FILTERS_PATH), any())).thenReturn(null)

        val returnedFile = LogViewerPreferencesImpl.defaultFiltersPath

        verify(mockPrefs, never()).put(any(), any())
        verify(mockListener, never()).onDefaultFiltersPathChanged()

        assertEquals(FileSystemView.getFileSystemView().homeDirectory.absolutePath, returnedFile.absolutePath)
    }

    @Test
    fun testSettingLastFilterPathsSingleFilter() {
        val testFilters = arrayOf(File("testFilter"))
        LogViewerPreferencesImpl.lastFilterPaths = testFilters

        verify(mockPrefs, times(1)).put(LogViewerPreferencesImpl.LAST_FILTER_PATH,
            testFilters[0].absolutePath
        )
        verify(mockListener, only()).onLastFilterPathChanged()
    }

    @Test
    fun testSettingLastFilterPathsMultipleFilters() {
        val testFilters = arrayOf(File("testFilter1"),
                File("testFilter2"),
                File("testFilter3"))
        LogViewerPreferencesImpl.lastFilterPaths = testFilters

        verify(mockPrefs, times(1)).put(LogViewerPreferencesImpl.LAST_FILTER_PATH,
                "${testFilters[0].absolutePath}\$${testFilters[1].absolutePath}\$${testFilters[2].absolutePath}")
        verify(mockListener, only()).onLastFilterPathChanged()
    }

    @Test
    fun testGettingLastFilterPathsSingleFilter() {
        val testFilter = File("testFilter")

        `when`(mockPrefs.get(eq(LogViewerPreferencesImpl.LAST_FILTER_PATH), any())).thenReturn(testFilter.absolutePath)

        val returnedFilters = LogViewerPreferencesImpl.lastFilterPaths

        verify(mockPrefs, never()).put(any(), any())
        verify(mockListener, never()).onLastFilterPathChanged()

        assertEquals(1, returnedFilters.size)
        assertEquals(testFilter.absolutePath, returnedFilters[0].absolutePath)
    }

    @Test
    fun testGettingLastFilterPathsMultipleFilters() {
        val testFilters = arrayOf(File("testFilter1"),
                File("testFilter2"),
                File("testFilter3"))

        `when`(mockPrefs.get(eq(LogViewerPreferencesImpl.LAST_FILTER_PATH), any())).thenReturn(
                "${testFilters[0].absolutePath}\$${testFilters[1].absolutePath}\$${testFilters[2].absolutePath}")

        val returnedFilters = LogViewerPreferencesImpl.lastFilterPaths

        verify(mockPrefs, never()).put(any(), any())
        verify(mockListener, never()).onLastFilterPathChanged()

        assertEquals(3, returnedFilters.size)
        assertEquals(testFilters[0].absolutePath, returnedFilters[0].absolutePath)
        assertEquals(testFilters[1].absolutePath, returnedFilters[1].absolutePath)
        assertEquals(testFilters[2].absolutePath, returnedFilters[2].absolutePath)
    }

    @Test
    fun testSettingDefaultLogsPath() {
        val testFile = File("test")
        LogViewerPreferencesImpl.defaultLogsPath = testFile

        verify(mockPrefs, times(1)).put(LogViewerPreferencesImpl.LOGS_PATH, testFile.absolutePath)
        verify(mockListener, only()).onDefaultLogsPathChanged()
    }

    @Test
    fun testGettingDefaultLogsPath() {
        val testFile = File("test")

        `when`(mockPrefs.get(eq(LogViewerPreferencesImpl.LOGS_PATH), any())).thenReturn(testFile.absolutePath)

        val returnedFile = LogViewerPreferencesImpl.defaultLogsPath

        verify(mockPrefs, never()).put(any(), any())
        verify(mockListener, never()).onDefaultLogsPathChanged()

        assertEquals(testFile.absolutePath, returnedFile.absolutePath)
    }

    @Test
    fun testGettingDefaultLogsPathUnset() {
        `when`(mockPrefs.get(eq(LogViewerPreferencesImpl.LOGS_PATH), any())).thenReturn(null)

        val returnedFile = LogViewerPreferencesImpl.defaultLogsPath

        verify(mockPrefs, never()).put(any(), any())
        verify(mockListener, never()).onDefaultFiltersPathChanged()

        assertEquals(FileSystemView.getFileSystemView().homeDirectory.absolutePath, returnedFile.absolutePath)
    }

    @Test
    fun testSettingLookAndFeel() {
        val testLookAndFeel = "testLookAndFeel"
        LogViewerPreferencesImpl.lookAndFeel = testLookAndFeel

        verify(mockPrefs, times(1)).put(LogViewerPreferencesImpl.LOOK_AND_FEEL, testLookAndFeel)
        verify(mockListener, only()).onLookAndFeelChanged()
    }

    @Test
    fun testGettingLookAndFeel() {
        val testLookAndFeel = "testLookAndFeel"
        `when`(mockPrefs.get(eq(LogViewerPreferencesImpl.LOOK_AND_FEEL), any())).thenReturn(testLookAndFeel)
        val returnedLookAndFeel = LogViewerPreferencesImpl.lookAndFeel

        verify(mockPrefs, never()).put(LogViewerPreferencesImpl.LOOK_AND_FEEL, testLookAndFeel)
        verify(mockListener, never()).onLookAndFeelChanged()
        assertEquals(testLookAndFeel, returnedLookAndFeel)
    }

    @Test
    fun testSettingOpenLastFilter() {
        LogViewerPreferencesImpl.openLastFilter = true

        verify(mockPrefs, times(1)).putBoolean(LogViewerPreferencesImpl.OPEN_LAST_FILTER, true)
        verify(mockListener, only()).onOpenLastFilterChanged()
    }

    @Test
    fun testGettingOpenLastFilter() {
        `when`(mockPrefs.getBoolean(eq(LogViewerPreferencesImpl.OPEN_LAST_FILTER), anyBoolean())).thenReturn(true)
        val returnedOpenLastFilter = LogViewerPreferencesImpl.openLastFilter

        verify(mockPrefs, never()).putBoolean(LogViewerPreferencesImpl.OPEN_LAST_FILTER, true)
        verify(mockListener, never()).onOpenLastFilterChanged()
        assertEquals(true, returnedOpenLastFilter)
    }

    @Test
    fun testSettingReapplyFiltersAfterEdit() {
        LogViewerPreferencesImpl.reapplyFiltersAfterEdit = true

        verify(mockPrefs, times(1)).putBoolean(LogViewerPreferencesImpl.REAPPLY_FILTERS_AFTER_EDIT, true)
        verify(mockListener, only()).onReapplyFiltersConfigChanged()
    }

    @Test
    fun testGettingReapplyFiltersAfterEdit() {
        `when`(mockPrefs.getBoolean(eq(LogViewerPreferencesImpl.REAPPLY_FILTERS_AFTER_EDIT), anyBoolean())).thenReturn(true)
        val returnedVal = LogViewerPreferencesImpl.reapplyFiltersAfterEdit

        verify(mockPrefs, never()).putBoolean(LogViewerPreferencesImpl.REAPPLY_FILTERS_AFTER_EDIT, true)
        verify(mockListener, never()).onReapplyFiltersConfigChanged()
        assertEquals(true, returnedVal)
    }

    @Test
    fun testSettingRememberAppliedFilters() {
        LogViewerPreferencesImpl.rememberAppliedFilters = true

        verify(mockPrefs, times(1)).putBoolean(LogViewerPreferencesImpl.REMEMBER_APPLIED_FILTERS, true)
        verify(mockListener, only()).onRememberAppliedFiltersConfigChanged()
    }

    @Test
    fun testSettingRememberAppliedFilters2() {
        LogViewerPreferencesImpl.rememberAppliedFilters = false

        verify(mockPrefs, times(1)).putBoolean(LogViewerPreferencesImpl.REMEMBER_APPLIED_FILTERS, false)
        verify(mockListener, only()).onRememberAppliedFiltersConfigChanged()
    }

    @Test
    fun testGettingRememberAppliedFilters() {
        `when`(mockPrefs.getBoolean(eq(LogViewerPreferencesImpl.REMEMBER_APPLIED_FILTERS), anyBoolean())).thenReturn(true)
        val returnedVal = LogViewerPreferencesImpl.rememberAppliedFilters

        verify(mockPrefs, never()).putBoolean(LogViewerPreferencesImpl.REMEMBER_APPLIED_FILTERS, false)
        verify(mockPrefs, never()).putBoolean(LogViewerPreferencesImpl.REMEMBER_APPLIED_FILTERS, true)
        verify(mockListener, never()).onRememberAppliedFiltersConfigChanged()
        assertEquals(true, returnedVal)
    }

    @Test
    fun testGettingRememberAppliedFilters2() {
        `when`(mockPrefs.getBoolean(eq(LogViewerPreferencesImpl.REMEMBER_APPLIED_FILTERS), anyBoolean())).thenReturn(false)
        val returnedVal = LogViewerPreferencesImpl.rememberAppliedFilters

        verify(mockPrefs, never()).putBoolean(LogViewerPreferencesImpl.REMEMBER_APPLIED_FILTERS, false)
        verify(mockPrefs, never()).putBoolean(LogViewerPreferencesImpl.REMEMBER_APPLIED_FILTERS, true)
        verify(mockListener, never()).onRememberAppliedFiltersConfigChanged()
        assertEquals(false, returnedVal)
    }

    @Test
    fun testSettingRememberAppliedFiltersIndicesSingleIndex() {
        val testGroup = "testGroup"
        val testIndices = listOf(1)

        val prefName = LogViewerPreferencesImpl.REMEMBER_APPLIED_FILTERS_PREFIX + testGroup
        val stringContent = "1"

        LogViewerPreferencesImpl.setAppliedFiltersIndices(testGroup, testIndices)

        verify(mockPrefs, times(1)).put(prefName, stringContent)
    }

    @Test
    fun testSettingRememberAppliedFiltersIndicesMultipleIndices() {
        val testGroup = "testGroup"
        val testIndices = listOf(1, 6, 7, 9, 10)

        val prefName = LogViewerPreferencesImpl.REMEMBER_APPLIED_FILTERS_PREFIX + testGroup
        val stringContent = "1,6,7,9,10"

        LogViewerPreferencesImpl.setAppliedFiltersIndices(testGroup, testIndices)

        verify(mockPrefs, times(1)).put(prefName, stringContent)
    }

    @Test
    fun testGettingRememberAppliedFiltersIndicesSingleIndex() {
        val testGroup = "testGroup"

        val prefName = LogViewerPreferencesImpl.REMEMBER_APPLIED_FILTERS_PREFIX + testGroup
        `when`(mockPrefs.get(eq(prefName), any())).thenReturn("11")
        val returnedVal = LogViewerPreferencesImpl.getAppliedFiltersIndices(testGroup)

        verify(mockPrefs, never()).put(any(), any())
        assertEquals(1, returnedVal.size)
        assertEquals(11, returnedVal[0])
    }

    @Test
    fun testGettingRememberAppliedFiltersIndicesMultipleIndices() {
        val testGroup = "testGroup"

        val prefName = LogViewerPreferencesImpl.REMEMBER_APPLIED_FILTERS_PREFIX + testGroup
        `when`(mockPrefs.get(eq(prefName), any())).thenReturn("1,2,4,11,6")
        val returnedVal = LogViewerPreferencesImpl.getAppliedFiltersIndices(testGroup)

        verify(mockPrefs, never()).put(any(), any())
        assertEquals(5, returnedVal.size)
        assertEquals(1, returnedVal[0])
        assertEquals(2, returnedVal[1])
        assertEquals(4, returnedVal[2])
        assertEquals(11, returnedVal[3])
        assertEquals(6, returnedVal[4])
    }

    @Test
    fun testGettingRememberAppliedFiltersIndicesWithInvalidValues() {
        val testGroup = "testGroup"

        val prefName = LogViewerPreferencesImpl.REMEMBER_APPLIED_FILTERS_PREFIX + testGroup
        `when`(mockPrefs.get(eq(prefName), any())).thenReturn("a,hb6,4,11,6")
        val returnedVal = LogViewerPreferencesImpl.getAppliedFiltersIndices(testGroup)

        verify(mockPrefs, never()).put(any(), any())
        assertEquals(3, returnedVal.size)
        assertEquals(4, returnedVal[0])
        assertEquals(11, returnedVal[1])
        assertEquals(6, returnedVal[2])
    }

    @Test
    fun testSettingCollapseAllGroupsStartup() {
        LogViewerPreferencesImpl.collapseAllGroupsStartup = true

        verify(mockPrefs, times(1)).putBoolean(LogViewerPreferencesImpl.COLLAPSE_ALL_GROUPS_STARTUP, true)
        verify(mockListener, only()).onCollapseAllGroupsStartupChanged()
    }

    @Test
    fun testSettingCollapseAllGroupsStartup2() {
        LogViewerPreferencesImpl.collapseAllGroupsStartup = false

        verify(mockPrefs, times(1)).putBoolean(LogViewerPreferencesImpl.COLLAPSE_ALL_GROUPS_STARTUP, false)
        verify(mockListener, only()).onCollapseAllGroupsStartupChanged()
    }

    @Test
    fun testGettingCollapseAllGroupsStartup() {
        `when`(mockPrefs.getBoolean(eq(LogViewerPreferencesImpl.COLLAPSE_ALL_GROUPS_STARTUP), anyBoolean())).thenReturn(true)
        val returnedVal = LogViewerPreferencesImpl.collapseAllGroupsStartup

        verify(mockPrefs, never()).putBoolean(LogViewerPreferencesImpl.COLLAPSE_ALL_GROUPS_STARTUP, false)
        verify(mockPrefs, never()).putBoolean(LogViewerPreferencesImpl.COLLAPSE_ALL_GROUPS_STARTUP, true)
        verify(mockListener, never()).onCollapseAllGroupsStartupChanged()
        assertEquals(true, returnedVal)
    }

    @Test
    fun testGettingCollapseAllGroupsStartup2() {
        `when`(mockPrefs.getBoolean(eq(LogViewerPreferencesImpl.COLLAPSE_ALL_GROUPS_STARTUP), anyBoolean())).thenReturn(false)
        val returnedVal = LogViewerPreferencesImpl.collapseAllGroupsStartup

        verify(mockPrefs, never()).putBoolean(LogViewerPreferencesImpl.COLLAPSE_ALL_GROUPS_STARTUP, false)
        verify(mockPrefs, never()).putBoolean(LogViewerPreferencesImpl.COLLAPSE_ALL_GROUPS_STARTUP, true)
        verify(mockListener, never()).onCollapseAllGroupsStartupChanged()
        assertEquals(false, returnedVal)
    }

    @Test
    fun testSettingShowLineNumbers() {
        LogViewerPreferencesImpl.showLineNumbers = true

        verify(mockPrefs, times(1)).putBoolean(LogViewerPreferencesImpl.SHOW_LINE_NUMBERS, true)
        verify(mockListener, only()).onShowLineNumbersChanged()
    }

    @Test
    fun testSettingShowLineNumbers2() {
        LogViewerPreferencesImpl.showLineNumbers = false

        verify(mockPrefs, times(1)).putBoolean(LogViewerPreferencesImpl.SHOW_LINE_NUMBERS, false)
        verify(mockListener, only()).onShowLineNumbersChanged()
    }

    @Test
    fun testGettingShowLineNumbers() {
        `when`(mockPrefs.getBoolean(eq(LogViewerPreferencesImpl.SHOW_LINE_NUMBERS), anyBoolean())).thenReturn(true)
        val returnedVal = LogViewerPreferencesImpl.showLineNumbers

        verify(mockPrefs, never()).putBoolean(LogViewerPreferencesImpl.SHOW_LINE_NUMBERS, false)
        verify(mockPrefs, never()).putBoolean(LogViewerPreferencesImpl.SHOW_LINE_NUMBERS, true)
        verify(mockListener, never()).onShowLineNumbersChanged()
        assertEquals(true, returnedVal)
    }

    @Test
    fun testGettingShowLineNumbers2() {
        `when`(mockPrefs.getBoolean(eq(LogViewerPreferencesImpl.SHOW_LINE_NUMBERS), anyBoolean())).thenReturn(false)
        val returnedVal = LogViewerPreferencesImpl.showLineNumbers

        verify(mockPrefs, never()).putBoolean(LogViewerPreferencesImpl.SHOW_LINE_NUMBERS, false)
        verify(mockPrefs, never()).putBoolean(LogViewerPreferencesImpl.SHOW_LINE_NUMBERS, true)
        verify(mockListener, never()).onShowLineNumbersChanged()
        assertEquals(false, returnedVal)
    }

    @Test
    fun testSettingPreferredEditorPath() {
        val testFile = File("test")
        LogViewerPreferencesImpl.preferredTextEditor = testFile

        verify(mockPrefs, times(1)).put(LogViewerPreferencesImpl.PREFERRED_TEXT_EDITOR, testFile.absolutePath)
        verify(mockListener, only()).onPreferredTextEditorChanged()
    }

    @Test
    fun testGettingPreferredEditorPath() {
        val testFile = File("test")

        `when`(mockPrefs.get(eq(LogViewerPreferencesImpl.PREFERRED_TEXT_EDITOR), any())).thenReturn(testFile.absolutePath)

        val returnedFile = LogViewerPreferencesImpl.preferredTextEditor

        verify(mockPrefs, never()).put(any(), any())
        verify(mockListener, never()).onPreferredTextEditorChanged()

        assertEquals(testFile.absolutePath, returnedFile?.absolutePath)
    }

    @Test
    fun testGettingPreferredEditorUnset() {
        `when`(mockPrefs.get(eq(LogViewerPreferencesImpl.PREFERRED_TEXT_EDITOR), any())).thenReturn(null)

        val returnedFile = LogViewerPreferencesImpl.preferredTextEditor

        verify(mockPrefs, never()).put(any(), any())
        verify(mockListener, never()).onPreferredTextEditorChanged()

        assertNull(returnedFile)
    }

    @Test
    fun testGettingPreferredEditorEmpty() {
        `when`(mockPrefs.get(eq(LogViewerPreferencesImpl.PREFERRED_TEXT_EDITOR), any())).thenReturn("")

        val returnedFile = LogViewerPreferencesImpl.preferredTextEditor

        verify(mockPrefs, never()).put(any(), any())
        verify(mockListener, never()).onPreferredTextEditorChanged()

        assertNull(returnedFile)
    }

    @Test
    fun testUnSettingPreferredEditorPath() {
        LogViewerPreferencesImpl.preferredTextEditor = null

        verify(mockPrefs, times(1)).put(LogViewerPreferencesImpl.PREFERRED_TEXT_EDITOR, "")
        verify(mockListener, only()).onPreferredTextEditorChanged()
    }
}