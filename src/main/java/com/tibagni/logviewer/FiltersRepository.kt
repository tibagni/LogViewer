package com.tibagni.logviewer

import com.tibagni.logviewer.filter.Filter
import com.tibagni.logviewer.filter.FilterException
import com.tibagni.logviewer.filter.serialized
import java.io.*
import java.lang.Exception
import java.util.ArrayList


class OpenFiltersException(message: String?, cause: Throwable) : java.lang.Exception(message, cause)
class PersistFiltersException(message: String?, cause: Throwable) : java.lang.Exception(message, cause)

interface FiltersRepository {
  val currentlyOpenedFilters: Map<String, List<Filter>>
  val currentlyOpenedFilterFiles: Map<String, File>
  var ignoredKeywords: List<String>

  @Throws(OpenFiltersException::class)
  fun openFilterFiles(files: Array<File>)

  fun addFilter(group: String, filter: Filter)
  fun addFilters(group: String, filters: List<Filter>)
  fun deleteFilters(group: String, indices: IntArray): List<Filter>
  fun reorderFilters(group: String, indOrig: Int, indDest: Int)
  fun addGroup(group: String): String
  fun deleteGroup(group: String): Boolean

  @Throws(PersistFiltersException::class)
  fun persistGroup(file: File, group: String)

  fun getChangedGroupsSinceLastOpened(): List<String>
  fun closeAllFilters()
}

class FiltersRepositoryImpl: FiltersRepository {
  private val _currentlyOpenedFilters = mutableMapOf<String, MutableList<Filter>>()
  override val currentlyOpenedFilters: Map<String, List<Filter>>
    get() = _currentlyOpenedFilters

  private val _currentlyOpenedFilterFiles = mutableMapOf<String, File>()
  override val currentlyOpenedFilterFiles: Map<String, File>
    get() = _currentlyOpenedFilterFiles

  private val lastOpenedSerializedFilters = mutableMapOf<String, List<String>>()


  private var _ignoredKeywords = mutableListOf<String>()
  override var ignoredKeywords: List<String>
    get() = _ignoredKeywords
    set(value) {
      _ignoredKeywords.clear()
      _ignoredKeywords.addAll(value)
    }

  @Throws(OpenFiltersException::class)
  override fun openFilterFiles(files: Array<File>) {
    for (file in files) {
      val group = file.name
      try {
        val fileText = file.inputStream().bufferedReader().use(BufferedReader::readText)
        val serializedFilters = fileText.lines()
        val filters = serializedFilters.map { Filter.createFromString(it) }

        lastOpenedSerializedFilters[group] = serializedFilters
        _currentlyOpenedFilterFiles[group] = file
        _currentlyOpenedFilters[group] = filters.toMutableList()
      } catch (e: Exception) {
        when (e) {
          is FilterException,
          is IOException -> {
            throw OpenFiltersException(e.message, e)
          }
          else -> throw e
        }
      }
    }
  }

  override fun addFilter(group: String, filter: Filter) {
    if (_currentlyOpenedFilters.containsKey(group)) {
      _currentlyOpenedFilters[group]?.add(filter)
    } else {
      _currentlyOpenedFilters[group] = mutableListOf(filter)
    }
  }

  override fun addFilters(group: String, filters: List<Filter>) {
    if (_currentlyOpenedFilters.containsKey(group)) {
      _currentlyOpenedFilters[group]?.addAll(filters)
    } else {
      _currentlyOpenedFilters[group] = filters.toMutableList()
    }
  }

  override fun deleteFilters(group: String, indices: IntArray): List<Filter> {
    val deletedFilters = ArrayList<Filter>()
    // Iterate backwards otherwise the indices will change
    // and we will end up deleting wrong items
    val filtersFromGroup = _currentlyOpenedFilters[group]
    filtersFromGroup?.let {
      for (i in indices.indices.reversed()) {
        if (indices[i] < it.size && indices[i] >= 0) {
          val deletedFilter = it.removeAt(indices[i])
          deletedFilters.add(deletedFilter)
        }
      }
    }

    return deletedFilters
  }

  override fun reorderFilters(group: String, indOrig: Int, indDest: Int) {
    if (indOrig == indDest) return

    val destIndex: Int = if (indDest > indOrig) indDest - 1 else indDest
    val filtersFromGroup = _currentlyOpenedFilters[group]

    filtersFromGroup?.let {
      val filter = it.removeAt(indOrig)
      it.add(destIndex, filter)
    }
  }

  override fun addGroup(group: String): String {
    var n = 1
    var groupName = group
    while (_currentlyOpenedFilters.containsKey(groupName)) {
      groupName = group + (n++)
    }

    _currentlyOpenedFilters[groupName] = ArrayList()
    return groupName
  }

  override fun deleteGroup(group: String): Boolean {
    val deletedGroup = _currentlyOpenedFilters.remove(group)
    _currentlyOpenedFilterFiles.remove(group)

    return deletedGroup != null
  }

  @Throws(PersistFiltersException::class)
  override fun persistGroup(file: File, group: String) {
    var fileWriter: BufferedWriter? = null
    try {
      var firstLoop = true
      fileWriter = BufferedWriter(FileWriter(file))
      val serializedFilters = _currentlyOpenedFilters[group].serialized()
      for (serializedFilter in serializedFilters) {
        if (firstLoop) {
          firstLoop = false
        } else {
          fileWriter.newLine()
        }
        fileWriter.write(serializedFilter)
      }
      _currentlyOpenedFilterFiles[group] = file

      // Now that we persisted the filters, add it to lastOpenedSerializedFilters
      lastOpenedSerializedFilters.remove(group)
      lastOpenedSerializedFilters[group] = serializedFilters
    } catch (e: IOException) {
      throw PersistFiltersException(e.message, e)
    } finally {
      fileWriter?.close()
    }
  }

  override fun getChangedGroupsSinceLastOpened(): List<String> {
    return _currentlyOpenedFilters.keys.filter {
      val currentState = _currentlyOpenedFilters[it].serialized()
      val prevState = lastOpenedSerializedFilters[it]
        ?: // new group since last opened
        return@filter true

      return@filter currentState != prevState
    }
  }

  override fun closeAllFilters() {
    _currentlyOpenedFilterFiles.clear()
    _currentlyOpenedFilters.clear()
    lastOpenedSerializedFilters.clear()
  }
}