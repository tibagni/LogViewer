package com.tibagni.logviewer.filter

import com.tibagni.logviewer.FiltersRepository
import com.tibagni.logviewer.OpenFiltersException
import com.tibagni.logviewer.PersistFiltersException
import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.log.LogLineParser
import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.preferences.LogViewerPreferences
import java.io.File

interface FiltersView {
  fun configureFiltersList(filters: Map<String, List<Filter>>?)
  fun showUnsavedFilterIndication(group: String?)
  fun hideUnsavedFilterIndication(group: String?)
  fun collapseAllGroups()
  fun showDuplicateFilterWarning(pattern: String, group: String): Boolean
  fun showDuplicateFiltersDialog(clusters: List<DuplicateCluster>): Map<String, List<Filter>>?
  fun showSelectFilterGroup(groups: List<String>): String?
  fun showAskToSaveFilterDialog(group: String?): FiltersPresenter.UserSelection
  fun showAskToSaveMultipleFiltersDialog(unsavedGroups: Array<String>): Array<Boolean>?
  fun showSaveFilters(group: String?): File?
  fun showInfoMessage(title: String, message: String)
  fun showErrorMessage(message: String?)
  fun showToast(message: String)
  fun updateFiltersUI()
  fun onAppliedFiltersRemembered()
  val activeFilterContextTag: FilterContextTag?
    get() = null
}

interface FiltersPresenter {
  enum class UserSelection {
    CONFIRMED,
    REJECTED,
    CANCELLED
  }

  fun init()
  fun addFilter(group: String, newFilter: Filter)
  fun addFilter(group: String, newFilter: Filter, ignoreReapply: Boolean)
  fun addGroup(group: String): String?
  val groups: List<String>
  fun removeFilters(group: String, indices: IntArray)
  fun moveFilters(origGroup: String, destGroup: String, indices: IntArray)
  fun removeGroup(group: String)
  fun reorderFilters(group: String, orig: Int, dest: Int)
  fun saveFilters(group: String)
  fun loadFilters(filtersFiles: Array<File>, keepCurrentFilters: Boolean)
  fun applyFilters()
  fun filterEdited(filter: Filter)
  fun setAllFiltersApplied(group: String, isApplied: Boolean)
  fun setAllFiltersApplied(isApplied: Boolean)
  fun cleanDuplicateFilters()
  fun findDuplicateFilter(pattern: String, isCaseSensitive: Boolean, editingFilter: Filter?): FilterMatch?
  fun addFilterForPid(pid: Int)
  fun addFilterForTag(tag: String)
  fun finishing(): Boolean
}

class FiltersPresenterImpl(
  private val view: FiltersView,
  private val userPrefs: LogViewerPreferences = ServiceLocator.logViewerPrefs,
  private val filtersRepository: FiltersRepository = ServiceLocator.filtersRepository,
  private val filterDuplicateUtils: FilterDuplicateUtils = FilterDuplicateUtilsImpl()
) : FiltersPresenter {

  private val unsavedFilterGroups = mutableSetOf<String>()

  override fun init() {
    if (userPrefs.openLastFilter) {
      val lastFilters = userPrefs.lastFilterPaths
      val rememberApplied = userPrefs.rememberAppliedFilters
      if (!lastFilters.isNullOrEmpty()) {
        loadFilters(lastFilters, rememberApplied)
      }
    }

    if (userPrefs.collapseAllGroupsStartup) {
      view.collapseAllGroups()
    }
  }

  override fun addFilter(group: String, newFilter: Filter) {
    addFilter(group, newFilter, false)
  }

  override fun addFilter(group: String, newFilter: Filter, ignoreReapply: Boolean) {
    if (group.isNotEmpty()) {
      filtersRepository.addFilter(group, newFilter)
      view.configureFiltersList(filtersRepository.currentlyOpenedFilters)
      checkForUnsavedChanges()

      if (!ignoreReapply && userPrefs.reapplyFiltersAfterEdit) {
        newFilter.isApplied = true
        applyFilters()
      }
    }
  }

  override fun addGroup(group: String): String? {
    if (group.isNotEmpty()) {
      val addedGroup = filtersRepository.addGroup(group)
      view.configureFiltersList(filtersRepository.currentlyOpenedFilters)
      return addedGroup
    }
    return null
  }

  override val groups: List<String>
    get() = filtersRepository.currentlyOpenedFilters.keys.toList()

  override fun removeFilters(group: String, indices: IntArray) {
    val deletedFilters = filtersRepository.deleteFilters(group, indices)
    val shouldReapply = deletedFilters.any { it.isApplied }
    view.configureFiltersList(filtersRepository.currentlyOpenedFilters)

    if (filtersRepository.currentlyOpenedFilters.isNotEmpty()) {
      checkForUnsavedChanges()
    }

    if (shouldReapply) {
      applyFilters()
    }
  }

  override fun moveFilters(origGroup: String, destGroup: String, indices: IntArray) {
    if (origGroup == destGroup) return

    val movingFilters = filtersRepository.deleteFilters(origGroup, indices)
    filtersRepository.addFilters(destGroup, movingFilters)

    val shouldReapply = movingFilters.any { it.isApplied }
    view.configureFiltersList(filtersRepository.currentlyOpenedFilters)
    checkForUnsavedChanges()

    if (shouldReapply) {
      applyFilters()
    }
  }

  override fun removeGroup(group: String) {
    if (group.isNotEmpty()) {
      val unsavedChange = unsavedFilterGroups.contains(group)
      if (unsavedChange) {
        val userSelection = view.showAskToSaveFilterDialog(group)
        if (userSelection == FiltersPresenter.UserSelection.CONFIRMED) {
          saveFilters(group)
        } else if (userSelection == FiltersPresenter.UserSelection.CANCELLED) {
          return
        }
      }

      val filtersFromGroup = filtersRepository.currentlyOpenedFilters[group]
      val groupFile = filtersRepository.currentlyOpenedFilterFiles[group]
      if (filtersFromGroup != null) {
        filtersRepository.deleteGroup(group)
        view.configureFiltersList(filtersRepository.currentlyOpenedFilters)
        checkForUnsavedChanges()

        val shouldReapply = filtersFromGroup.any { it.isApplied }
        if (shouldReapply) {
          applyFilters()
        }
      }

      if (groupFile != null) {
        val currentSavedPaths = userPrefs.lastFilterPaths
        if (!currentSavedPaths.isNullOrEmpty()) {
          userPrefs.lastFilterPaths = currentSavedPaths.filter { it != groupFile }.toTypedArray()
        }
      }
    }
  }

  override fun reorderFilters(group: String, orig: Int, dest: Int) {
    if (orig == dest) return

    filtersRepository.reorderFilters(group, orig, dest)
    view.configureFiltersList(filtersRepository.currentlyOpenedFilters)
    checkForUnsavedChanges()
  }

  override fun saveFilters(group: String) {
    var saveFile = filtersRepository.currentlyOpenedFilterFiles[group]
    if (saveFile == null) {
      saveFile = view.showSaveFilters(group)
    }

    if (saveFile != null) {
      saveFilters(saveFile, group)
    }
  }

  private fun saveFilters(filterFile: File, group: String) {
    try {
      filtersRepository.persistGroup(filterFile, group)
      checkForUnsavedChanges()
    } catch (e: PersistFiltersException) {
      view.showErrorMessage(e.message)
    }
  }

  override fun loadFilters(filtersFiles: Array<File>, keepCurrentFilters: Boolean) {
    if (userPrefs.rememberAppliedFilters) {
      rememberAppliedFilters()
    }

    if (!keepCurrentFilters) {
      if (filtersRepository.currentlyOpenedFilters.isNotEmpty()) {
        val shouldAbort = !requestSaveUnsavedGroups()
        if (shouldAbort) return
      }
      filtersRepository.closeAllFilters()
    }

    try {
      filtersRepository.openFilterFiles(filtersFiles)
    } catch (e: OpenFiltersException) {
      view.showErrorMessage(e.message)
    }

    val currentlyOpenedFilters = filtersRepository.currentlyOpenedFilters
    for (filterFile in filtersFiles) {
      val group = filterFile.name
      val filters = currentlyOpenedFilters[group].orEmpty()
      val isLegacy = filters.any { it.wasLoadedFromLegacyFile }
      if (isLegacy) {
        Logger.info("Filter Group: $group is using old file format. Re-save it")
        filters.forEach { it.wasLoadedFromLegacyFile = false }
        saveFilters(group)
      }
    }

    view.configureFiltersList(currentlyOpenedFilters)
    checkForUnsavedChanges()

    val lastFilterPaths = filtersRepository.currentlyOpenedFilterFiles.values.toTypedArray()
    userPrefs.lastFilterPaths = lastFilterPaths
    if (userPrefs.rememberAppliedFilters) {
      reapplyRememberedFilters()
      view.onAppliedFiltersRemembered()
    }
  }

  override fun applyFilters() {
    cleanUpFilterTempInfo()
    filtersRepository.notifyFiltersApplied()
  }

  override fun filterEdited(filter: Filter) {
    checkForUnsavedChanges()
    if (userPrefs.reapplyFiltersAfterEdit) {
      filter.isApplied = true
      applyFilters()
    }
  }

  override fun setAllFiltersApplied(group: String, isApplied: Boolean) {
    filtersRepository.currentlyOpenedFilters[group]?.forEach { it.isApplied = isApplied }
    applyFilters()
    view.updateFiltersUI()
  }

  override fun setAllFiltersApplied(isApplied: Boolean) {
    filtersRepository.currentlyOpenedFilters.values.flatten().forEach { it.isApplied = isApplied }
    applyFilters()
    view.updateFiltersUI()
  }

  override fun cleanDuplicateFilters() {
    val currentFilters = filtersRepository.currentlyOpenedFilters
    if (currentFilters.isEmpty()) {
      view.showInfoMessage("Clean Duplicate Filters", "There are no filters loaded.")
      return
    }

    val clusters = filterDuplicateUtils.findDuplicateClusters(currentFilters)
    if (clusters.isEmpty()) {
      view.showInfoMessage("Clean Duplicate Filters", "No duplicate filters found.")
      return
    }

    val removals = view.showDuplicateFiltersDialog(clusters)
    if (removals.isNullOrEmpty()) {
      return
    }

    val shouldReapply = removals.values.flatten().any { it.isApplied }

    val totalDeleted = applyDeduplicationResolution(removals)
    if (totalDeleted > 0) {
      view.configureFiltersList(filtersRepository.currentlyOpenedFilters)

      if (filtersRepository.currentlyOpenedFilters.isNotEmpty()) {
        checkForUnsavedChanges()
      }

      if (shouldReapply) {
        applyFilters()
      }

      view.showToast("Cleaned $totalDeleted duplicate filter(s)")
    }
  }

  override fun findDuplicateFilter(pattern: String, isCaseSensitive: Boolean, editingFilter: Filter?): FilterMatch? {
    return filterDuplicateUtils.findFirstDuplicate(
      pattern,
      isCaseSensitive,
      editingFilter,
      filtersRepository.currentlyOpenedFilters
    )
  }

  override fun addFilterForPid(pid: Int) {
    addQuickFilter("PID $pid", LogLineParser.getFilterPatternForPid(pid))
  }

  override fun addFilterForTag(tag: String) {
    addQuickFilter(tag, LogLineParser.getFilterPatternForTag(tag))
  }

  private fun addQuickFilter(name: String, pattern: String) {
    val duplicateMatch = findDuplicateFilter(pattern, false, null)
    if (duplicateMatch != null) {
      val addAnyway = view.showDuplicateFilterWarning(
        duplicateMatch.filter.patternString,
        duplicateMatch.group
      )
      if (!addAnyway) {
        if (!duplicateMatch.filter.isApplied) {
          duplicateMatch.filter.isApplied = true
          applyFilters()
        }
        return
      }
    }

    val currentGroups = groups
    val group = view.showSelectFilterGroup(currentGroups)
    if (group.isNullOrEmpty()) {
      return
    }

    val color = EditFilterDialog.getRandomColor(ServiceLocator.themeManager.isDark)
    val newFilter = Filter(name, pattern, color, LogLevel.VERBOSE)
    newFilter.isApplied = true
    addFilter(group, newFilter)
    if (!userPrefs.reapplyFiltersAfterEdit) {
      applyFilters()
    }
  }

  override fun finishing(): Boolean {
    val shouldFinish = requestSaveUnsavedGroups()
    if (shouldFinish && userPrefs.rememberAppliedFilters) {
      rememberAppliedFilters()
    }
    return shouldFinish
  }

  private fun applyDeduplicationResolution(filtersToRemove: Map<String, Collection<Filter>>): Int {
    var totalDeleted = 0
    for ((group, toRemove) in filtersToRemove) {
      if (toRemove.isEmpty()) continue
      val current = filtersRepository.currentlyOpenedFilters[group] ?: continue
      val indices = toRemove.map { current.indexOf(it) }.filter { it >= 0 }.sorted().toIntArray()
      if (indices.isNotEmpty()) {
        filtersRepository.deleteFilters(group, indices)
        totalDeleted += indices.size
      }
    }
    return totalDeleted
  }

  private fun requestSaveUnsavedGroups(): Boolean {
    val unsavedGroups = unsavedFilterGroups.toTypedArray()
    if (unsavedGroups.isEmpty()) {
      return true
    }

    val groupsSelection = view.showAskToSaveMultipleFiltersDialog(unsavedGroups)
    if (groupsSelection != null) {
      for (i in unsavedGroups.indices) {
        if (groupsSelection[i]) {
          saveFilters(unsavedGroups[i])
        }
      }
      return true
    }
    return false
  }

  private fun cleanUpFilterTempInfo() {
    val tag = view.activeFilterContextTag ?: return
    filtersRepository.currentlyOpenedFilters.values.flatten().forEach { it.resetContext(tag) }
  }

  private fun checkForUnsavedChanges() {
    unsavedFilterGroups.clear()
    val changedGroups = filtersRepository.getChangedGroupsSinceLastOpened()
    val allGroups = filtersRepository.currentlyOpenedFilters.keys

    for (group in allGroups) {
      if (changedGroups.contains(group)) {
        view.showUnsavedFilterIndication(group)
        unsavedFilterGroups.add(group)
      } else {
        view.hideUnsavedFilterIndication(group)
      }
    }
  }

  private fun rememberAppliedFilters() {
    for ((group, filters) in filtersRepository.currentlyOpenedFilters) {
      val appliedIndices = filters.mapIndexedNotNull { index, filter ->
        if (filter.isApplied) index else null
      }
      userPrefs.setAppliedFiltersIndices(group, appliedIndices)
    }
  }

  private fun reapplyRememberedFilters() {
    for ((group, filters) in filtersRepository.currentlyOpenedFilters) {
      val appliedIndices = userPrefs.getAppliedFiltersIndices(group)
      if (appliedIndices.isNotEmpty()) {
        for ((index, filter) in filters.withIndex()) {
          if (appliedIndices.contains(index)) {
            filter.isApplied = true
          }
        }
      }
    }
    applyFilters()
  }

  fun setUnsavedGroupForTesting(group: String) {
    unsavedFilterGroups.add(group)
  }
}
