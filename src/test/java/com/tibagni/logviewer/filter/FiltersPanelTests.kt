package com.tibagni.logviewer.filter

import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.preferences.LogViewerPreferences
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.view.SingleChoiceDialog
import com.tibagni.logviewer.view.ToolbarDivider
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.FlatButton
import java.awt.FlowLayout
import javax.swing.JButton
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.awt.Color
import java.awt.Frame
import java.awt.GraphicsEnvironment

class FiltersPanelTests {

  @Mock
  private lateinit var mockPresenter: FiltersPresenter

  @Mock
  private lateinit var mockHost: FiltersPanelHost

  @Mock
  private lateinit var mockPrefs: LogViewerPreferences

  @Mock
  private lateinit var mockFrame: Frame

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    `when`(mockHost.parentWindow).thenReturn(mockFrame)
  }

  @Test
  fun testFiltersPanelCreation() {
    val panel = FiltersPanel(mockHost) { mockPresenter }
    assertNotNull(panel.filtersPane)
    assertTrue(panel.isEmpty)
    verify(mockPresenter).init()
  }

  @Test
  fun testConfigureFiltersList() {
    assumeFalse(GraphicsEnvironment.isHeadless())
    val panel = FiltersPanel(mockHost) { mockPresenter }
    val filter1 = Filter("test1", "pattern1", Color.RED, LogLevel.DEBUG)
    val filter2 = Filter("test2", "pattern2", Color.BLUE, LogLevel.INFO)
    val map = mapOf("Group1" to listOf(filter1, filter2))

    panel.configureFiltersList(map)
    assertFalse(panel.isEmpty)
  }

  @Test
  fun testClearAllFiltersSelection() {
    val panel = FiltersPanel(mockHost) { mockPresenter }
    panel.clearAllFiltersSelection()
    verify(mockPresenter).setAllFiltersApplied(false)
  }

  @Test
  fun testUpdateFiltersUIDoesNotThrow() {
    val panel = FiltersPanel(mockHost) { mockPresenter }
    panel.updateFiltersUI()
  }

  @Test
  fun testShowUnsavedIndication() {
    val panel = FiltersPanel(mockHost) { mockPresenter }
    // Null check safety (does not require display)
    panel.showUnsavedFilterIndication(null)
    panel.hideUnsavedFilterIndication(null)

    assumeFalse(GraphicsEnvironment.isHeadless())
    val filter = Filter("test", "pattern", Color.RED, LogLevel.DEBUG)
    panel.configureFiltersList(mapOf("Group1" to listOf(filter)))
    panel.showUnsavedFilterIndication("Group1")
    panel.hideUnsavedFilterIndication("Group1")
  }

  @Test
  fun testCollapseAllGroups() {
    assumeFalse(GraphicsEnvironment.isHeadless())
    val panel = FiltersPanel(mockHost) { mockPresenter }
    val filter = Filter("test", "pattern", Color.RED, LogLevel.DEBUG)
    panel.configureFiltersList(mapOf("Group1" to listOf(filter)))

    panel.collapseAllGroups()
    // Should not throw and collapse state updated
  }

  @Test
  fun testAddFilterForPidDelegatesToPresenter() {
    val panel = FiltersPanel(mockHost) { mockPresenter }
    panel.addFilterForPid(1234)
    verify(mockPresenter).addFilterForPid(1234)
  }

  @Test
  fun testAddFilterForTagDelegatesToPresenter() {
    val panel = FiltersPanel(mockHost) { mockPresenter }
    panel.addFilterForTag("ActivityManager")
    verify(mockPresenter).addFilterForTag("ActivityManager")
  }

  @Test
  fun testRequestFinishWhenPresenterAllowsFinish() {
    val panel = FiltersPanel(mockHost) { mockPresenter }
    `when`(mockPresenter.finishing()).thenReturn(true)

    var finished = false
    panel.requestFinish { finished = true }

    assertTrue(finished)
    verify(mockPresenter).finishing()
  }

  @Test
  fun testRequestFinishWhenPresenterPreventsFinish() {
    val panel = FiltersPanel(mockHost) { mockPresenter }
    `when`(mockPresenter.finishing()).thenReturn(false)

    var finished = false
    panel.requestFinish { finished = true }

    assertFalse(finished)
    verify(mockPresenter).finishing()
  }


  @Test
  fun testShowSelectFilterGroupIncludesCreateNewOption() {
    val panel = FiltersPanel(mockHost) { mockPresenter }
    var capturedOptions: Array<String>? = null
    var capturedDefaultIdx = -1

    panel.singleChoiceDialogProvider = { _, _, choices, defaultIdx, _ ->
      capturedOptions = choices
      capturedDefaultIdx = defaultIdx
      0 // select first option
    }

    val selected = panel.showSelectFilterGroup(listOf("Group1", "Group2"))
    assertEquals("Group1", selected)

    assertNotNull(capturedOptions)
    assertEquals(3, capturedOptions!!.size)
    assertEquals("Group1", capturedOptions!![0])
    assertEquals("Group2", capturedOptions!![1])
    assertEquals(I18n.get(I18n.FILTERS_MOVE_CREATE_NEW), capturedOptions!![2])
    assertEquals(2, capturedDefaultIdx)
  }

  @Test
  fun testShowSelectFilterGroupSingleGroupIncludesCreateNewOption() {
    val panel = FiltersPanel(mockHost) { mockPresenter }
    var capturedOptions: Array<String>? = null

    panel.singleChoiceDialogProvider = { _, _, choices, _, _ ->
      capturedOptions = choices
      0
    }

    val selected = panel.showSelectFilterGroup(listOf("OnlyGroup"))
    assertEquals("OnlyGroup", selected)

    assertNotNull(capturedOptions)
    assertEquals(2, capturedOptions!!.size)
    assertEquals("OnlyGroup", capturedOptions!![0])
    assertEquals(I18n.get(I18n.FILTERS_MOVE_CREATE_NEW), capturedOptions!![1])
  }

  @Test
  fun testShowSelectFilterGroupEmptyGroupsIncludesCreateNewOption() {
    val panel = FiltersPanel(mockHost) { mockPresenter }
    var capturedOptions: Array<String>? = null

    panel.singleChoiceDialogProvider = { _, _, choices, _, _ ->
      capturedOptions = choices
      0
    }

    panel.inputDialogProvider = { _, _, _ -> "FirstGroup" }
    `when`(mockPresenter.addGroup("FirstGroup")).thenReturn("FirstGroup")

    val selected = panel.showSelectFilterGroup(emptyList())
    assertEquals("FirstGroup", selected)

    assertNotNull(capturedOptions)
    assertEquals(1, capturedOptions!!.size)
    assertEquals(I18n.get(I18n.FILTERS_MOVE_CREATE_NEW), capturedOptions!![0])
  }

  @Test
  fun testShowSelectFilterGroupSelectingCreateNewPromptsAndAddsGroup() {
    val panel = FiltersPanel(mockHost) { mockPresenter }
    `when`(mockPresenter.addGroup("BrandNewGroup")).thenReturn("BrandNewGroup")

    panel.singleChoiceDialogProvider = { _, _, choices, _, _ ->
      choices.lastIndex // select "Create new group"
    }
    panel.inputDialogProvider = { _, _, _ ->
      "BrandNewGroup"
    }

    val selected = panel.showSelectFilterGroup(listOf("Group1"))
    assertEquals("BrandNewGroup", selected)
    verify(mockPresenter).addGroup("BrandNewGroup")
  }

  @Test
  fun testShowSelectFilterGroupCancelledReturnsNull() {
    val panel = FiltersPanel(mockHost) { mockPresenter }
    panel.singleChoiceDialogProvider = { _, _, _, _, _ ->
      SingleChoiceDialog.DIALOG_CANCELLED
    }

    val selected = panel.showSelectFilterGroup(listOf("Group1"))
    assertNull(selected)
  }
  @Test
  fun testToolbarActionButtonsInstantiatedAndTyped() {
    val panel = FiltersPanel(mockHost) { mockPresenter }
    assertEquals(
      FlatButton::class.java,
      panel.addNewFilterGroupBtn.javaClass
    )
    assertEquals(
      FlatButton::class.java,
      panel.moreFilterOptionsBtn.javaClass
    )
    assertEquals(
      FlatButton::class.java,
      panel.collapseExpandAllGroupsBtn.javaClass
    )
    assertEquals(
      FlatButton::class.java,
      panel.findFiltersBtn.javaClass
    )
    assertEquals(
      FlatButton::class.java,
      panel.applyFiltersBtn.javaClass
    )
    assertEquals(
      ToolbarDivider::class.java,
      panel.applyDivider.javaClass
    )
    val layout = panel.filterActionButtonsPane.layout as FlowLayout
    assertEquals(FlowLayout.RIGHT, layout.alignment)
    assertEquals(UIScaleUtils.dip(4), layout.hgap)
    assertEquals(0, layout.vgap)
  }

  @Test
  fun testApplyFiltersButtonVisibilityWhenBothReapplyTrue() {
    `when`(mockPrefs.reapplyFiltersAfterEdit).thenReturn(true)
    `when`(mockPrefs.applyFilterOnCheck).thenReturn(true)
    val panel = FiltersPanel(mockHost, { mockPresenter }, mockPrefs)
    assertFalse(panel.applyFiltersBtn.isVisible)
    assertFalse(panel.applyDivider.isVisible)
  }

  @Test
  fun testApplyFiltersButtonVisibilityWhenReapplyFiltersAfterEditFalse() {
    `when`(mockPrefs.reapplyFiltersAfterEdit).thenReturn(false)
    `when`(mockPrefs.applyFilterOnCheck).thenReturn(true)
    val panel = FiltersPanel(mockHost, { mockPresenter }, mockPrefs)
    assertTrue(panel.applyFiltersBtn.isVisible)
    assertTrue(panel.applyDivider.isVisible)
  }

  @Test
  fun testApplyFiltersButtonVisibilityWhenApplyFilterOnCheckFalse() {
    `when`(mockPrefs.reapplyFiltersAfterEdit).thenReturn(true)
    `when`(mockPrefs.applyFilterOnCheck).thenReturn(false)
    val panel = FiltersPanel(mockHost, { mockPresenter }, mockPrefs)
    assertTrue(panel.applyFiltersBtn.isVisible)
    assertTrue(panel.applyDivider.isVisible)
  }

  @Test
  fun testApplyFiltersButtonVisibilityWhenBothFalse() {
    `when`(mockPrefs.reapplyFiltersAfterEdit).thenReturn(false)
    `when`(mockPrefs.applyFilterOnCheck).thenReturn(false)
    val panel = FiltersPanel(mockHost, { mockPresenter }, mockPrefs)
    assertTrue(panel.applyFiltersBtn.isVisible)
    assertTrue(panel.applyDivider.isVisible)
  }

  @Test
  fun testApplyFiltersButtonVisibilityPreferenceListenerUpdates() {
    `when`(mockPrefs.reapplyFiltersAfterEdit).thenReturn(true)
    `when`(mockPrefs.applyFilterOnCheck).thenReturn(true)

    val panel = FiltersPanel(mockHost, { mockPresenter }, mockPrefs)
    assertFalse(panel.applyFiltersBtn.isVisible)
    assertFalse(panel.applyDivider.isVisible)

    val captor = ArgumentCaptor.forClass(
      LogViewerPreferences.Listener::class.java
    )
    verify(mockPrefs).addPreferenceListener(
      captureNonNull(captor, dummyPrefsListener)
    )
    val listener = captor.value

    `when`(mockPrefs.reapplyFiltersAfterEdit).thenReturn(false)
    listener.onReapplyFiltersConfigChanged()
    assertTrue(panel.applyFiltersBtn.isVisible)
    assertTrue(panel.applyDivider.isVisible)

    `when`(mockPrefs.reapplyFiltersAfterEdit).thenReturn(true)
    `when`(mockPrefs.applyFilterOnCheck).thenReturn(false)
    listener.onApplyFiltersOnCheckChanged()
    assertTrue(panel.applyFiltersBtn.isVisible)
    assertTrue(panel.applyDivider.isVisible)

    `when`(mockPrefs.applyFilterOnCheck).thenReturn(true)
    listener.onApplyFiltersOnCheckChanged()
    assertFalse(panel.applyFiltersBtn.isVisible)
    assertFalse(panel.applyDivider.isVisible)
  }

  private fun <T : Any> captureNonNull(
    captor: ArgumentCaptor<T>,
    fallback: T
  ): T {
    captor.capture()
    return fallback
  }

  private val dummyPrefsListener = object : LogViewerPreferences.Adapter() {}
}
