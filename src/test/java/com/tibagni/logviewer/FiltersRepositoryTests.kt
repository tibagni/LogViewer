package com.tibagni.logviewer

import com.tibagni.logviewer.filter.Filter
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class FiltersRepositoryTests {
  private var temporaryFiles: Array<File>? = null
  private lateinit var filtersRepository: FiltersRepository

  companion object {
    private const val TEST_SERIALIZED_FILTER = "Test,VGVzdA==,2,255:0:0"
    private const val TEST_SERIALIZED_FILTER2 = "Test2,VGVzdA==,2,255:0:0"
    private const val TEST_SERIALIZED_FILTER3 = "Test3,VGVzdA==,2,255:0:0"
  }

  private fun createTempFilterFiles(vararg filterFiles: Pair<String, String>): Array<File> {
    val files = filterFiles.map {
      File.createTempFile(it.first, "filter").apply {
        writeText(it.second)
      }
    }

    temporaryFiles = files.toTypedArray()
    return temporaryFiles!!
  }

  @Before
  fun setUp() {
    filtersRepository = FiltersRepositoryImpl()
  }

  @After
  fun tearDown() {
    // Cleanup any temporary files created by the test case (if any)
    temporaryFiles?.forEach { it.delete() }
  }

  @Test
  fun testOpenSingleFilterFileSingleFilter() {
    val temporaryFilterFile = createTempFilterFiles("test" to TEST_SERIALIZED_FILTER)

    filtersRepository.openFilterFiles(temporaryFilterFile)

    assertEquals(1, filtersRepository.currentlyOpenedFilterFiles.size)
    assertEquals(1, filtersRepository.currentlyOpenedFilters.size)
    assertNotNull(filtersRepository.currentlyOpenedFilters[temporaryFilterFile[0].name])
    assertEquals(1, filtersRepository.currentlyOpenedFilters[temporaryFilterFile[0].name]?.size)
  }

  @Test
  fun testOpenSingleFilterFileMultipleFilters() {
    val temporaryFilterFile =
      createTempFilterFiles(
        "test" to listOf
          (TEST_SERIALIZED_FILTER, TEST_SERIALIZED_FILTER2).joinToString("\n")
      )

    filtersRepository.openFilterFiles(temporaryFilterFile)

    assertEquals(1, filtersRepository.currentlyOpenedFilterFiles.size)
    assertEquals(1, filtersRepository.currentlyOpenedFilters.size)
    assertNotNull(filtersRepository.currentlyOpenedFilters[temporaryFilterFile[0].name])
    assertEquals(2, filtersRepository.currentlyOpenedFilters[temporaryFilterFile[0].name]?.size)
  }

  @Test
  fun testOpenMultipleFilterFilesSingleFilter() {
    val temporaryFilterFiles =
      createTempFilterFiles(
        "test" to TEST_SERIALIZED_FILTER,
        "test2" to TEST_SERIALIZED_FILTER2
      )

    filtersRepository.openFilterFiles(temporaryFilterFiles)

    assertEquals(2, filtersRepository.currentlyOpenedFilterFiles.size)
    assertEquals(2, filtersRepository.currentlyOpenedFilters.size)
    temporaryFilterFiles.forEach {
      assertNotNull(filtersRepository.currentlyOpenedFilters[it.name])
      assertEquals(1, filtersRepository.currentlyOpenedFilters[it.name]?.size)
    }
  }

  @Test
  fun testOpenMultipleFilterFilesMultipleFilters() {
    val temporaryFilterFiles =
      createTempFilterFiles(
        "test" to listOf
          (TEST_SERIALIZED_FILTER, TEST_SERIALIZED_FILTER2).joinToString("\n"),
        "test2" to listOf
          (TEST_SERIALIZED_FILTER, TEST_SERIALIZED_FILTER3).joinToString("\n")
      )

    filtersRepository.openFilterFiles(temporaryFilterFiles)

    assertEquals(2, filtersRepository.currentlyOpenedFilterFiles.size)
    assertEquals(2, filtersRepository.currentlyOpenedFilters.size)
    temporaryFilterFiles.forEach {
      assertNotNull(filtersRepository.currentlyOpenedFilters[it.name])
      assertEquals(2, filtersRepository.currentlyOpenedFilters[it.name]?.size)
    }
  }

  @Test
  fun testOpenMultipleFilterFilesMultipleAndSingleFilters() {
    val temporaryFilterFiles =
      createTempFilterFiles(
        "test" to TEST_SERIALIZED_FILTER,
        "test2" to listOf
          (TEST_SERIALIZED_FILTER2, TEST_SERIALIZED_FILTER3).joinToString("\n")
      )

    filtersRepository.openFilterFiles(temporaryFilterFiles)

    assertEquals(2, filtersRepository.currentlyOpenedFilterFiles.size)
    assertEquals(2, filtersRepository.currentlyOpenedFilters.size)

    assertNotNull(filtersRepository.currentlyOpenedFilters[temporaryFilterFiles[0].name])
    assertEquals(1, filtersRepository.currentlyOpenedFilters[temporaryFilterFiles[0].name]?.size)

    assertNotNull(filtersRepository.currentlyOpenedFilters[temporaryFilterFiles[1].name])
    assertEquals(2, filtersRepository.currentlyOpenedFilters[temporaryFilterFiles[1].name]?.size)
  }

  @Test
  fun testAddNewFilterToNewGroup() {
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER))

    assertEquals(1, filtersRepository.currentlyOpenedFilters.size)
    assertNotNull(filtersRepository.currentlyOpenedFilters["testGroup"])
    assertEquals(1, filtersRepository.currentlyOpenedFilters["testGroup"]?.size)
  }

  @Test
  fun testAddNewFilterToExistingGroup() {
    // add one filter to create the group first
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER))
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER2))

    assertEquals(1, filtersRepository.currentlyOpenedFilters.size)
    assertNotNull(filtersRepository.currentlyOpenedFilters["testGroup"])
    assertEquals(2, filtersRepository.currentlyOpenedFilters["testGroup"]?.size)
  }

  @Test
  fun testDeleteSingleFilter() {
    // add filters first
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER))
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER2))
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER3))

    val deletedFilters = filtersRepository.deleteFilters("testGroup", intArrayOf(0))

    assertEquals(1, deletedFilters.size)
    assertEquals(TEST_SERIALIZED_FILTER, deletedFilters[0].serializeFilter())
    assertEquals(2, filtersRepository.currentlyOpenedFilters["testGroup"]?.size)
  }

  @Test
  fun testDeleteMultipleFilters() {
    // add filters first
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER))
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER2))
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER3))

    val deletedFilters = filtersRepository.deleteFilters("testGroup", intArrayOf(0, 2))
    val deletedFiltersSerialized = deletedFilters.map { it.serializeFilter() }

    assertEquals(2, deletedFilters.size)
    assertTrue(deletedFiltersSerialized.contains(TEST_SERIALIZED_FILTER))
    assertTrue(deletedFiltersSerialized.contains(TEST_SERIALIZED_FILTER3))
    assertEquals(1, filtersRepository.currentlyOpenedFilters["testGroup"]?.size)
    assertEquals(
      TEST_SERIALIZED_FILTER2,
      filtersRepository.currentlyOpenedFilters["testGroup"]?.get(0)?.serializeFilter()
    )
  }

  @Test
  fun testDeleteAllFilters() {
    // add filters first
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER))
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER2))
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER3))

    val deletedFilters = filtersRepository.deleteFilters("testGroup", intArrayOf(0, 1, 2))

    assertEquals(3, deletedFilters.size)
    assertEquals(0, filtersRepository.currentlyOpenedFilters["testGroup"]?.size)
  }

  @Test
  fun testDeleteFilterInvalidIndex() {
    // add filters first
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER))
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER2))
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER3))

    val deletedFilters = filtersRepository.deleteFilters("testGroup", intArrayOf(3))

    assertEquals(0, deletedFilters.size)
    assertEquals(3, filtersRepository.currentlyOpenedFilters["testGroup"]?.size)
  }

  @Test
  fun testReorderFiltersToSamePosition() {
    // add filters first
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER))
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER2))
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER3))

    filtersRepository.reorderFilters("testGroup", 1, 1)

    assertEquals(
      TEST_SERIALIZED_FILTER2,
      filtersRepository.currentlyOpenedFilters["testGroup"]?.get(1)?.serializeFilter()
    )
  }

  @Test
  fun testReorderFiltersFirstToThird() {
    // add filters first
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER))
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER2))
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER3))

    filtersRepository.reorderFilters("testGroup", 0, 3)

    assertEquals(
      TEST_SERIALIZED_FILTER,
      filtersRepository.currentlyOpenedFilters["testGroup"]?.get(2)?.serializeFilter()
    )
    assertEquals(
      TEST_SERIALIZED_FILTER2,
      filtersRepository.currentlyOpenedFilters["testGroup"]?.get(0)?.serializeFilter()
    )
    assertEquals(
      TEST_SERIALIZED_FILTER3,
      filtersRepository.currentlyOpenedFilters["testGroup"]?.get(1)?.serializeFilter()
    )
  }

  @Test
  fun testReorderFiltersFirstToSecond() {
    // add filters first
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER))
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER2))
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER3))

    filtersRepository.reorderFilters("testGroup", 0, 2)

    assertEquals(
      TEST_SERIALIZED_FILTER,
      filtersRepository.currentlyOpenedFilters["testGroup"]?.get(1)?.serializeFilter()
    )
    assertEquals(
      TEST_SERIALIZED_FILTER2,
      filtersRepository.currentlyOpenedFilters["testGroup"]?.get(0)?.serializeFilter()
    )
    assertEquals(
      TEST_SERIALIZED_FILTER3,
      filtersRepository.currentlyOpenedFilters["testGroup"]?.get(2)?.serializeFilter()
    )
  }

  @Test
  fun testReorderFiltersThirdToFirst() {
    // add filters first
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER))
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER2))
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER3))

    filtersRepository.reorderFilters("testGroup", 2, 0)

    assertEquals(
      TEST_SERIALIZED_FILTER,
      filtersRepository.currentlyOpenedFilters["testGroup"]?.get(1)?.serializeFilter()
    )
    assertEquals(
      TEST_SERIALIZED_FILTER2,
      filtersRepository.currentlyOpenedFilters["testGroup"]?.get(2)?.serializeFilter()
    )
    assertEquals(
      TEST_SERIALIZED_FILTER3,
      filtersRepository.currentlyOpenedFilters["testGroup"]?.get(0)?.serializeFilter()
    )
  }

  @Test
  fun testReorderFiltersThirdToSecond() {
    // add filters first
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER))
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER2))
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER3))

    filtersRepository.reorderFilters("testGroup", 2, 1)

    assertEquals(
      TEST_SERIALIZED_FILTER,
      filtersRepository.currentlyOpenedFilters["testGroup"]?.get(0)?.serializeFilter()
    )
    assertEquals(
      TEST_SERIALIZED_FILTER2,
      filtersRepository.currentlyOpenedFilters["testGroup"]?.get(2)?.serializeFilter()
    )
    assertEquals(
      TEST_SERIALIZED_FILTER3,
      filtersRepository.currentlyOpenedFilters["testGroup"]?.get(1)?.serializeFilter()
    )
  }

  @Test
  fun testAddNewGroup() {
    val added = filtersRepository.addGroup("testGroup")

    assertEquals("testGroup", added)
    assertEquals(1, filtersRepository.currentlyOpenedFilters.size)
    assertNotNull(filtersRepository.currentlyOpenedFilters["testGroup"])
  }

  @Test
  fun testAddNewGroupWithSameName() {
    filtersRepository.addGroup("testGroup")
    val added = filtersRepository.addGroup("testGroup")

    assertEquals("testGroup1", added)
    assertEquals(2, filtersRepository.currentlyOpenedFilters.size)
    assertNotNull(filtersRepository.currentlyOpenedFilters["testGroup1"])
  }

  @Test
  fun testAddNewGroupWithSameNameSecondTime() {
    filtersRepository.addGroup("testGroup")
    filtersRepository.addGroup("testGroup")
    val added = filtersRepository.addGroup("testGroup")

    assertEquals("testGroup2", added)
    assertEquals(3, filtersRepository.currentlyOpenedFilters.size)
    assertNotNull(filtersRepository.currentlyOpenedFilters["testGroup2"])
  }

  @Test
  fun testDeleteExistingGroup() {
    filtersRepository.addGroup("testGroup")
    val deleted = filtersRepository.deleteGroup("testGroup")

    assertTrue(deleted)
    assertEquals(0, filtersRepository.currentlyOpenedFilters.size)
  }

  @Test
  fun testDeleteNonExistingGroup() {
    val deleted = filtersRepository.deleteGroup("testGroup")

    assertFalse(deleted)
    assertEquals(0, filtersRepository.currentlyOpenedFilters.size)
  }

  @Test
  fun testDeleteNonExistingGroup2() {
    filtersRepository.addGroup("testGroup")
    val deleted = filtersRepository.deleteGroup("nonExistingGroup")

    assertFalse(deleted)
    assertEquals(1, filtersRepository.currentlyOpenedFilters.size)
    assertNotNull(filtersRepository.currentlyOpenedFilters["testGroup"])
    assertNull(filtersRepository.currentlyOpenedFilters["nonExistingGroup"])
  }

  @Test
  fun testPersistGroupSingleFilter() {
    // Create a temp file to validate saved contents
    val temporaryFilterFile = createTempFilterFiles("test" to "")

    // add a new group with 2 filters
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER))
    filtersRepository.persistGroup(temporaryFilterFile[0], "testGroup")

    // Check contents of the saved file
    val savedText = temporaryFilterFile[0].inputStream().bufferedReader().readText()
    val savedFilters = savedText.split("\n")

    assertNotNull(savedFilters)
    assertEquals(1, savedFilters.size)
    assertEquals(TEST_SERIALIZED_FILTER, savedFilters[0])
  }

  @Test
  fun testPersistGroupMultipleFilters() {
    // Create a temp file to validate saved contents
    val temporaryFilterFile = createTempFilterFiles("test" to "")

    // add a new group with 2 filters
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER))
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER2))
    filtersRepository.addFilter("testGroup", Filter.createFromString(TEST_SERIALIZED_FILTER3))
    filtersRepository.persistGroup(temporaryFilterFile[0], "testGroup")

    // Check contents of the saved file
    val savedText = temporaryFilterFile[0].inputStream().bufferedReader().readText()
    val savedFilters = savedText.split("\n")

    assertNotNull(savedFilters)
    assertEquals(3, savedFilters.size)
    assertEquals(TEST_SERIALIZED_FILTER, savedFilters[0])
    assertEquals(TEST_SERIALIZED_FILTER2, savedFilters[1])
    assertEquals(TEST_SERIALIZED_FILTER3, savedFilters[2])
  }

  @Test
  fun testChangedGroupsSinceLastOpenedNotChanged() {
    // Load filters from a file as it will set up all the internal states correctly
    val temporaryFilterFile = createTempFilterFiles("test" to TEST_SERIALIZED_FILTER)
    filtersRepository.openFilterFiles(temporaryFilterFile)

    val changed = filtersRepository.getChangedGroupsSinceLastOpened()

    assertTrue(changed.isEmpty())
  }

  @Test
  fun testChangedGroupsSinceLastOpenedFilterAdded() {
    // Load filters from a file as it will set up all the internal states correctly
    val temporaryFilterFile = createTempFilterFiles("test" to TEST_SERIALIZED_FILTER)
    filtersRepository.openFilterFiles(temporaryFilterFile)
    filtersRepository.addFilter(temporaryFilterFile[0].name, Filter.createFromString(TEST_SERIALIZED_FILTER2))

    val changed = filtersRepository.getChangedGroupsSinceLastOpened()

    assertEquals(1, changed.size)
    assertEquals(temporaryFilterFile[0].name, changed[0])
  }

  @Test
  fun testChangedGroupsSinceLastOpenedFilterRemovedGroupEmpty() {
    // Load filters from a file as it will set up all the internal states correctly
    val temporaryFilterFile = createTempFilterFiles("test" to TEST_SERIALIZED_FILTER)
    filtersRepository.openFilterFiles(temporaryFilterFile)
    filtersRepository.deleteFilters("test", intArrayOf(0))

    val changed = filtersRepository.getChangedGroupsSinceLastOpened()

    // And empty group is not reported as changed to prevent the user from saving an empty file
    assertTrue(changed.isEmpty())
  }

  @Test
  fun testChangedGroupsSinceLastOpenedFilterRemovedGroupNotEmpty() {
    // Load filters from a file as it will set up all the internal states correctly
    val temporaryFilterFile =
      createTempFilterFiles(
        "test" to
            listOf(
              TEST_SERIALIZED_FILTER,
              TEST_SERIALIZED_FILTER2
            ).joinToString("\n")
      )
    filtersRepository.openFilterFiles(temporaryFilterFile)
    filtersRepository.deleteFilters(temporaryFilterFile[0].name, intArrayOf(0))

    val changed = filtersRepository.getChangedGroupsSinceLastOpened()

    assertEquals(1, changed.size)
    assertEquals(temporaryFilterFile[0].name, changed[0])
  }

  @Test
  fun testChangedGroupsSinceLastOpenedFilterEdited() {
    // Load filters from a file as it will set up all the internal states correctly
    val temporaryFilterFile =
      createTempFilterFiles(
        "test" to
            listOf(
              TEST_SERIALIZED_FILTER,
              TEST_SERIALIZED_FILTER2
            ).joinToString("\n")
      )
    filtersRepository.openFilterFiles(temporaryFilterFile)
    val filterToEdit = filtersRepository.currentlyOpenedFilters[temporaryFilterFile[0].name]?.get(0)
    filterToEdit?.let {
      it.updateFilter("newName", it.patternString, it.color, it.isCaseSensitive)
    }

    val changed = filtersRepository.getChangedGroupsSinceLastOpened()

    assertEquals(1, changed.size)
    assertEquals(temporaryFilterFile[0].name, changed[0])
  }

  @Test
  fun testChangedGroupsSinceLastOpenedNewGroup() {
    // Load filters from a file as it will set up all the internal states correctly
    val temporaryFilterFile =
      createTempFilterFiles(
        "test" to
            listOf(
              TEST_SERIALIZED_FILTER,
              TEST_SERIALIZED_FILTER2
            ).joinToString("\n")
      )
    filtersRepository.openFilterFiles(temporaryFilterFile)
    filtersRepository.addFilter("newGroup", Filter.createFromString(TEST_SERIALIZED_FILTER3))

    val changed = filtersRepository.getChangedGroupsSinceLastOpened()

    assertEquals(1, changed.size)
    assertEquals("newGroup", changed[0])
  }

  @Test
  fun testChangedGroupsSinceLastOpenedReordered() {
    // Load filters from a file as it will set up all the internal states correctly
    val temporaryFilterFile =
      createTempFilterFiles(
        "test" to
            listOf(
              TEST_SERIALIZED_FILTER,
              TEST_SERIALIZED_FILTER2
            ).joinToString("\n")
      )
    filtersRepository.openFilterFiles(temporaryFilterFile)
    filtersRepository.reorderFilters(temporaryFilterFile[0].name, 0, 2)

    val changed = filtersRepository.getChangedGroupsSinceLastOpened()

    assertEquals(1, changed.size)
    assertEquals(temporaryFilterFile[0].name, changed[0])
  }

  @Test
  fun testCloseAllFilters() {
    // Load filters from a file as it will set up all the internal states correctly
    val temporaryFilterFile =
      createTempFilterFiles(
        "test" to
            listOf(
              TEST_SERIALIZED_FILTER,
              TEST_SERIALIZED_FILTER2
            ).joinToString("\n")
      )
    filtersRepository.openFilterFiles(temporaryFilterFile)

    filtersRepository.closeAllFilters()

    assertTrue(filtersRepository.currentlyOpenedFilters.isEmpty())
    assertTrue(filtersRepository.currentlyOpenedFilterFiles.isEmpty())
  }
}