package com.tibagni.logviewer.filter

import com.tibagni.logviewer.FiltersRepository
import com.tibagni.logviewer.MockExecutorService
import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.log.LogLineParser
import com.tibagni.logviewer.preferences.LogViewerPreferences
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.awt.Color
import java.io.File

class FiltersPresenterTests {

  @Mock
  private lateinit var view: FiltersView

  @Mock
  private lateinit var mockPrefs: LogViewerPreferences

  @Mock
  private lateinit var mockFiltersRepository: FiltersRepository

  @Mock
  private lateinit var mockFilterDuplicateUtils: FilterDuplicateUtils

  @Captor
  private lateinit var filterCaptor: ArgumentCaptor<Filter>

  private lateinit var presenter: FiltersPresenterImpl
  private val openedFilters = mutableMapOf<String, MutableList<Filter>>()
  private val openedFiles = mutableMapOf<String, File>()

  private fun <T> anyOrNull(): T = any<T>()
  private fun <T : Any> anyNonNull(default: T): T {
    any<T>()
    return default
  }
  private fun <T : Any> eqNonNull(value: T): T {
    eq(value)
    return value
  }
  private fun <T : Any> captureNonNull(captor: ArgumentCaptor<T>, default: T): T {
    captor.capture()
    return default
  }

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    openedFilters.clear()
    openedFiles.clear()

    `when`(mockFiltersRepository.currentlyOpenedFilters).thenReturn(openedFilters as Map<String, List<Filter>>)
    `when`(mockFiltersRepository.currentlyOpenedFilterFiles).thenReturn(openedFiles)

    presenter = FiltersPresenterImpl(
      view,
      mockPrefs,
      mockFiltersRepository,
      mockFilterDuplicateUtils
    )
  }

  @After
  fun tearDown() {
  }

  @Test
  fun testInitNotLoadingLastFilter() {
    `when`(mockPrefs.openLastFilter).thenReturn(false)
    presenter.init()

    verify(mockPrefs, never()).lastFilterPaths
    verify(view, never()).configureFiltersList(any())
  }

  @Test
  fun testInitLoadingLastFilterNoFilterAvailable() {
    `when`(mockPrefs.openLastFilter).thenReturn(true)
    `when`(mockPrefs.rememberAppliedFilters).thenReturn(false)
    `when`(mockPrefs.lastFilterPaths).thenReturn(arrayOf())
    presenter.init()

    verify(view, never()).configureFiltersList(any())
  }

  @Test
  fun testInitLoadingLastFilter() {
    val inputFile = File("mock")
    `when`(mockPrefs.openLastFilter).thenReturn(true)
    `when`(mockPrefs.rememberAppliedFilters).thenReturn(false)
    `when`(mockPrefs.lastFilterPaths).thenReturn(arrayOf(inputFile))
    presenter.init()

    verify(mockFiltersRepository).openFilterFiles(arrayOf(inputFile))
    verify(view).configureFiltersList(any())
  }

  @Test
  fun testInitCollapseAllGroupsStartup() {
    val inputFile = File("mock")
    `when`(mockPrefs.openLastFilter).thenReturn(true)
    `when`(mockPrefs.collapseAllGroupsStartup).thenReturn(true)
    `when`(mockPrefs.lastFilterPaths).thenReturn(arrayOf(inputFile))

    presenter.init()

    verify(mockFiltersRepository).openFilterFiles(arrayOf(inputFile))
    verify(view).configureFiltersList(any())
    verify(view).collapseAllGroups()
  }

  @Test
  fun testAddFilterNoApplyOnAdd() {
    val toAdd = Filter("test", "pattern", Color.RED, LogLevel.DEBUG)
    `when`(mockPrefs.reapplyFiltersAfterEdit).thenReturn(false)

    presenter.addFilter("testGroup", toAdd)

    verify(mockFiltersRepository).addFilter("testGroup", toAdd)
    verify(view).configureFiltersList(any())
    verify(mockFiltersRepository, never()).notifyFiltersApplied()
  }

  @Test
  fun testAddFilterWithApplyOnAdd() {
    val toAdd = Filter("test", "pattern", Color.RED, LogLevel.DEBUG)
    `when`(mockPrefs.reapplyFiltersAfterEdit).thenReturn(true)

    presenter.addFilter("testGroup", toAdd)

    verify(mockFiltersRepository).addFilter("testGroup", toAdd)
    verify(view).configureFiltersList(any())
    assertTrue(toAdd.isApplied)
    verify(mockFiltersRepository).notifyFiltersApplied()
  }

  @Test
  fun testAddGroup() {
    `when`(mockFiltersRepository.addGroup("NewGroup")).thenReturn("NewGroup")

    val result = presenter.addGroup("NewGroup")

    assertEquals("NewGroup", result)
    verify(mockFiltersRepository).addGroup("NewGroup")
    verify(view).configureFiltersList(any())
  }

  @Test
  fun testGroups() {
    openedFilters["Group1"] = mutableListOf()
    openedFilters["Group2"] = mutableListOf()

    val groups = presenter.groups
    assertEquals(listOf("Group1", "Group2"), groups)
  }

  @Test
  fun testRemoveFiltersNotApplied() {
    val f = Filter("test", "pattern", Color.RED, LogLevel.DEBUG)
    f.isApplied = false
    `when`(mockFiltersRepository.deleteFilters(eqNonNull("Group1"), anyNonNull(intArrayOf(0)))).thenReturn(listOf(f))

    presenter.removeFilters("Group1", intArrayOf(0))

    verify(view).configureFiltersList(any())
    verify(mockFiltersRepository, never()).notifyFiltersApplied()
  }

  @Test
  fun testRemoveFiltersApplied() {
    val f = Filter("test", "pattern", Color.RED, LogLevel.DEBUG)
    f.isApplied = true
    `when`(mockFiltersRepository.deleteFilters(eqNonNull("Group1"), anyNonNull(intArrayOf(0)))).thenReturn(listOf(f))

    presenter.removeFilters("Group1", intArrayOf(0))

    verify(view).configureFiltersList(any())
    verify(mockFiltersRepository).notifyFiltersApplied()
  }

  @Test
  fun testMoveFilters() {
    val f = Filter("test", "pattern", Color.RED, LogLevel.DEBUG)
    f.isApplied = true
    `when`(mockFiltersRepository.deleteFilters(eqNonNull("Group1"), anyNonNull(intArrayOf(0)))).thenReturn(listOf(f))

    presenter.moveFilters("Group1", "Group2", intArrayOf(0))

    verify(mockFiltersRepository).addFilters("Group2", listOf(f))
    verify(view).configureFiltersList(any())
    verify(mockFiltersRepository).notifyFiltersApplied()
  }

  @Test
  fun testMoveFiltersSameGroupDoesNothing() {
    presenter.moveFilters("Group1", "Group1", intArrayOf(0))
    verify(mockFiltersRepository, never()).deleteFilters(anyNonNull(""), anyNonNull(intArrayOf()))
  }

  @Test
  fun testRemoveGroupUnsavedCancel() {
    presenter.setUnsavedGroupForTesting("Group1")
    `when`(view.showAskToSaveFilterDialog("Group1")).thenReturn(FiltersPresenter.UserSelection.CANCELLED)

    presenter.removeGroup("Group1")

    verify(mockFiltersRepository, never()).deleteGroup(anyNonNull(""))
  }

  @Test
  fun testRemoveGroupUnsavedSave() {
    presenter.setUnsavedGroupForTesting("Group1")
    val file = File("test.filters")
    openedFiles["Group1"] = file
    val f = Filter("test", "pattern", Color.RED, LogLevel.DEBUG)
    f.isApplied = true
    openedFilters["Group1"] = mutableListOf(f)

    `when`(view.showAskToSaveFilterDialog("Group1")).thenReturn(FiltersPresenter.UserSelection.CONFIRMED)

    presenter.removeGroup("Group1")

    verify(mockFiltersRepository).persistGroup(file, "Group1")
    verify(mockFiltersRepository).deleteGroup("Group1")
    verify(mockFiltersRepository).notifyFiltersApplied()
  }

  @Test
  fun testRemoveGroupNoUnsavedChanges() {
    val file = File("test.filters")
    openedFiles["Group1"] = file
    val f = Filter("test", "pattern", Color.RED, LogLevel.DEBUG)
    f.isApplied = true
    openedFilters["Group1"] = mutableListOf(f)

    presenter.removeGroup("Group1")

    verify(view, never()).showAskToSaveFilterDialog(anyNonNull(""))
    verify(mockFiltersRepository, never()).persistGroup(anyNonNull(file), anyNonNull(""))
    verify(mockFiltersRepository).deleteGroup("Group1")
    verify(mockFiltersRepository).notifyFiltersApplied()
  }

  @Test
  fun testReorderFilters() {
    presenter.reorderFilters("Group1", 0, 2)

    verify(mockFiltersRepository).reorderFilters("Group1", 0, 2)
    verify(view).configureFiltersList(any())
  }

  @Test
  fun testSaveFiltersNewFile() {
    val file = File("test.filters")
    `when`(view.showSaveFilters("Group1")).thenReturn(file)

    presenter.saveFilters("Group1")

    verify(mockFiltersRepository).persistGroup(file, "Group1")
  }

  @Test
  fun testSaveFiltersExistingFile() {
    val file = File("test.filters")
    openedFiles["Group1"] = file

    presenter.saveFilters("Group1")

    verify(mockFiltersRepository).persistGroup(file, "Group1")
    verify(view, never()).showSaveFilters(any())
  }

  @Test
  fun testLoadFiltersRememberApplied() {
    val file = File("test.filters")
    val f1 = Filter("test1", "pattern1", Color.RED, LogLevel.DEBUG)
    f1.isApplied = true
    val f2 = Filter("test2", "pattern2", Color.BLUE, LogLevel.INFO)
    f2.isApplied = false
    openedFilters["Group1"] = mutableListOf(f1, f2)
    openedFiles["Group1"] = file

    `when`(mockPrefs.rememberAppliedFilters).thenReturn(true)
    `when`(mockPrefs.getAppliedFiltersIndices("Group1")).thenReturn(listOf(0))

    presenter.loadFilters(arrayOf(file), false)

    verify(mockFiltersRepository).openFilterFiles(arrayOf(file))
    verify(view).configureFiltersList(any())
    verify(view).onAppliedFiltersRemembered()
    assertTrue(f1.isApplied)
    verify(mockFiltersRepository).notifyFiltersApplied()
  }

  @Test
  fun testLoadFiltersMigratesLegacyFormat() {
    val file = File("legacy.filters")
    val legacyFilter = Filter("legacy", "pattern", Color.RED, LogLevel.DEBUG)
    legacyFilter.wasLoadedFromLegacyFile = true
    openedFilters["legacy.filters"] = mutableListOf(legacyFilter)
    openedFiles["legacy.filters"] = file

    presenter.loadFilters(arrayOf(file), false)

    assertFalse(legacyFilter.wasLoadedFromLegacyFile)
    verify(mockFiltersRepository).persistGroup(file, "legacy.filters")
  }

  @Test
  fun testFilterEditedReapply() {
    val f = Filter("test", "pattern", Color.RED, LogLevel.DEBUG)
    f.isApplied = true
    `when`(mockPrefs.reapplyFiltersAfterEdit).thenReturn(true)

    presenter.filterEdited(f)

    verify(mockFiltersRepository).notifyFiltersApplied()
  }

  @Test
  fun testFilterEditedReapplyWhenPreviouslyUnapplied() {
    val f = Filter("test", "pattern", Color.RED, LogLevel.DEBUG)
    f.isApplied = false
    `when`(mockPrefs.reapplyFiltersAfterEdit).thenReturn(true)

    presenter.filterEdited(f)

    assertTrue(f.isApplied)
    verify(mockFiltersRepository).notifyFiltersApplied()
  }

  @Test
  fun testFilterEditedNoReapply() {
    val f = Filter("test", "pattern", Color.RED, LogLevel.DEBUG)
    f.isApplied = false
    `when`(mockPrefs.reapplyFiltersAfterEdit).thenReturn(false)

    presenter.filterEdited(f)

    assertFalse(f.isApplied)
    verify(mockFiltersRepository, never()).notifyFiltersApplied()
  }

  @Test
  fun testSetAllFiltersAppliedGroup() {
    val f1 = Filter("test1", "pattern1", Color.RED, LogLevel.DEBUG)
    f1.isApplied = false
    val f2 = Filter("test2", "pattern2", Color.BLUE, LogLevel.INFO)
    f2.isApplied = false
    openedFilters["Group1"] = mutableListOf(f1, f2)

    presenter.setAllFiltersApplied("Group1", true)

    assertTrue(f1.isApplied)
    assertTrue(f2.isApplied)
    verify(mockFiltersRepository).notifyFiltersApplied()
  }

  @Test
  fun testSetAllFiltersAppliedAll() {
    val f1 = Filter("test1", "pattern1", Color.RED, LogLevel.DEBUG)
    f1.isApplied = true
    val f2 = Filter("test2", "pattern2", Color.BLUE, LogLevel.INFO)
    f2.isApplied = true
    openedFilters["Group1"] = mutableListOf(f1)
    openedFilters["Group2"] = mutableListOf(f2)

    presenter.setAllFiltersApplied(false)

    assertFalse(f1.isApplied)
    assertFalse(f2.isApplied)
    verify(mockFiltersRepository).notifyFiltersApplied()
  }

  @Test
  fun testCleanDuplicateFiltersEmpty() {
    presenter.cleanDuplicateFilters()
    verify(view).showInfoMessage("Clean Duplicate Filters", "There are no filters loaded.")
  }

  @Test
  fun testCleanDuplicateFiltersNoDuplicates() {
    openedFilters["Group1"] = mutableListOf(Filter("test", "pattern", Color.RED, LogLevel.DEBUG))
    `when`(mockFilterDuplicateUtils.findDuplicateClusters(anyNonNull(emptyMap()))).thenReturn(emptyList())

    presenter.cleanDuplicateFilters()

    verify(view).showInfoMessage("Clean Duplicate Filters", "No duplicate filters found.")
  }

  @Test
  fun testCleanDuplicateFiltersResolutionApplied() {
    val f1 = Filter("test1", "pattern", Color.RED, LogLevel.DEBUG)
    val f2 = Filter("test2", "pattern", Color.BLUE, LogLevel.DEBUG)
    f2.isApplied = true
    openedFilters["Group1"] = mutableListOf(f1, f2)

    val cluster = DuplicateCluster(f1, listOf(FilterMatch("Group1", f2, 1)))
    `when`(mockFilterDuplicateUtils.findDuplicateClusters(anyNonNull(emptyMap()))).thenReturn(listOf(cluster))
    `when`(view.showDuplicateFiltersDialog(listOf(cluster))).thenReturn(mapOf("Group1" to listOf(f2)))

    presenter.cleanDuplicateFilters()

    verify(mockFiltersRepository).deleteFilters(eqNonNull("Group1"), anyNonNull(intArrayOf(1)))
    verify(view).showToast("Cleaned 1 duplicate filter(s)")
    verify(mockFiltersRepository).notifyFiltersApplied()
  }

  @Test
  fun testCleanDuplicateFiltersCancelledByUser() {
    val f1 = Filter("test1", "pattern", Color.RED, LogLevel.DEBUG)
    val f2 = Filter("test2", "pattern", Color.BLUE, LogLevel.DEBUG)
    openedFilters["Group1"] = mutableListOf(f1, f2)

    val cluster = DuplicateCluster(f1, listOf(FilterMatch("Group1", f2, 1)))
    `when`(mockFilterDuplicateUtils.findDuplicateClusters(anyNonNull(emptyMap()))).thenReturn(listOf(cluster))
    `when`(view.showDuplicateFiltersDialog(listOf(cluster))).thenReturn(null)

    presenter.cleanDuplicateFilters()

    verify(mockFiltersRepository, never()).deleteFilters(anyNonNull(""), anyNonNull(intArrayOf()))
    verify(mockFiltersRepository, never()).notifyFiltersApplied()
  }

  @Test
  fun testFindDuplicateFilter() {
    val f = Filter("test", "pattern", Color.RED, LogLevel.DEBUG)
    val match = FilterMatch("Group1", f, 0)
    `when`(mockFilterDuplicateUtils.findFirstDuplicate("pattern", false, null, openedFilters)).thenReturn(match)

    val result = presenter.findDuplicateFilter("pattern", false, null)

    assertEquals(match, result)
  }

  @Test
  fun testAddFilterForPidSingleGroup() {
    val dummyFilter = Filter("dummy", "dummy", Color.BLACK, LogLevel.VERBOSE)
    openedFilters["Group1"] = mutableListOf()
    `when`(mockFilterDuplicateUtils.findFirstDuplicate(anyNonNull(""), anyBoolean(), anyOrNull(), anyNonNull(emptyMap()))).thenReturn(null)
    `when`(mockPrefs.reapplyFiltersAfterEdit).thenReturn(true)
    `when`(view.showSelectFilterGroup(listOf("Group1"))).thenReturn("Group1")

    presenter.addFilterForPid(1234)

    verify(view).showSelectFilterGroup(listOf("Group1"))
    verify(mockFiltersRepository).addFilter(eqNonNull("Group1"), captureNonNull(filterCaptor, dummyFilter))
    val captured = filterCaptor.value
    assertEquals("PID 1234", captured.name)
    assertEquals(LogLineParser.getFilterPatternForPid(1234), captured.patternString)
    assertTrue(captured.isApplied)
    verify(mockFiltersRepository).notifyFiltersApplied()
  }

  @Test
  fun testAddFilterForPidCreateNewGroup() {
    val dummyFilter = Filter("dummy", "dummy", Color.BLACK, LogLevel.VERBOSE)
    openedFilters["Group1"] = mutableListOf()
    `when`(mockFilterDuplicateUtils.findFirstDuplicate(anyNonNull(""), anyBoolean(), anyOrNull(), anyNonNull(emptyMap()))).thenReturn(null)
    `when`(mockPrefs.reapplyFiltersAfterEdit).thenReturn(true)
    `when`(view.showSelectFilterGroup(listOf("Group1"))).thenReturn("NewGroup")

    presenter.addFilterForPid(1234)

    verify(view).showSelectFilterGroup(listOf("Group1"))
    verify(mockFiltersRepository).addFilter(eqNonNull("NewGroup"), captureNonNull(filterCaptor, dummyFilter))
    val captured = filterCaptor.value
    assertEquals("PID 1234", captured.name)
    assertEquals(LogLineParser.getFilterPatternForPid(1234), captured.patternString)
    assertTrue(captured.isApplied)
    verify(mockFiltersRepository).notifyFiltersApplied()
  }

  @Test
  fun testAddFilterForPidWhenReapplyAfterEditIsFalse() {
    val dummyFilter = Filter("dummy", "dummy", Color.BLACK, LogLevel.VERBOSE)
    openedFilters["Group1"] = mutableListOf()
    `when`(mockFilterDuplicateUtils.findFirstDuplicate(anyNonNull(""), anyBoolean(), anyOrNull(), anyNonNull(emptyMap()))).thenReturn(null)
    `when`(mockPrefs.reapplyFiltersAfterEdit).thenReturn(false)
    `when`(view.showSelectFilterGroup(listOf("Group1"))).thenReturn("Group1")

    presenter.addFilterForPid(1234)

    verify(mockFiltersRepository).addFilter(eqNonNull("Group1"), captureNonNull(filterCaptor, dummyFilter))
    val captured = filterCaptor.value
    assertTrue(captured.isApplied)
    verify(mockFiltersRepository).notifyFiltersApplied()
  }

  @Test
  fun testAddFilterForTagMultipleGroups() {
    val dummyFilter = Filter("dummy", "dummy", Color.BLACK, LogLevel.VERBOSE)
    openedFilters["Group1"] = mutableListOf()
    openedFilters["Group2"] = mutableListOf()
    `when`(mockFilterDuplicateUtils.findFirstDuplicate(anyNonNull(""), anyBoolean(), anyOrNull(), anyNonNull(emptyMap()))).thenReturn(null)
    `when`(view.showSelectFilterGroup(listOf("Group1", "Group2"))).thenReturn("Group2")
    `when`(mockPrefs.reapplyFiltersAfterEdit).thenReturn(true)

    presenter.addFilterForTag("ActivityManager")

    verify(mockFiltersRepository).addFilter(eqNonNull("Group2"), captureNonNull(filterCaptor, dummyFilter))
    val captured = filterCaptor.value
    assertEquals("ActivityManager", captured.name)
    assertEquals(LogLineParser.getFilterPatternForTag("ActivityManager"), captured.patternString)
    assertTrue(captured.isApplied)
    verify(mockFiltersRepository).notifyFiltersApplied()
  }

  @Test
  fun testAddFilterDuplicateRejectedActivatesExisting() {
    val existing = Filter("existing", "pattern", Color.RED, LogLevel.DEBUG)
    existing.isApplied = false
    val match = FilterMatch("Group1", existing, 0)

    `when`(mockFilterDuplicateUtils.findFirstDuplicate(anyNonNull(""), anyBoolean(), anyOrNull(), anyNonNull(emptyMap()))).thenReturn(match)
    `when`(view.showDuplicateFilterWarning(anyNonNull(""), anyNonNull(""))).thenReturn(false) // User cancels duplicate

    presenter.addFilterForPid(1234)

    verify(mockFiltersRepository, never()).addFilter(anyNonNull(""), anyNonNull(existing))
    assertTrue(existing.isApplied)
    verify(mockFiltersRepository).notifyFiltersApplied()
  }

  @Test
  fun testFinishingNoUnsaved() {
    val result = presenter.finishing()
    assertTrue(result)
  }

  @Test
  fun testFinishingUnsavedCancelled() {
    presenter.setUnsavedGroupForTesting("Group1")
    `when`(view.showAskToSaveMultipleFiltersDialog(arrayOf("Group1"))).thenReturn(null)

    val result = presenter.finishing()
    assertFalse(result)
  }

  @Test
  fun testFinishingUnsavedSaveSelected() {
    presenter.setUnsavedGroupForTesting("Group1")
    val file = File("test.filters")
    openedFiles["Group1"] = file

    `when`(view.showAskToSaveMultipleFiltersDialog(arrayOf("Group1"))).thenReturn(arrayOf(true))

    val result = presenter.finishing()
    assertTrue(result)
    verify(mockFiltersRepository).persistGroup(file, "Group1")
  }

  @Test
  fun testApplyFiltersCleansUpTemporaryInfoAndNotifies() {
    val f = Filter("test", "pattern", Color.RED, LogLevel.DEBUG)
    f.initTemporaryInfo()
    assertNotNull(f.temporaryInfo)
    openedFilters["Group1"] = mutableListOf(f)

    presenter.applyFilters()

    assertNull(f.temporaryInfo)
    verify(mockFiltersRepository).notifyFiltersApplied()
  }
}
