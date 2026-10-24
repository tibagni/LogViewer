package com.tibagni.logviewer.filter

import com.formdev.flatlaf.FlatClientProperties
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.theme.LogViewerThemeManager
import com.tibagni.logviewer.util.StringUtils
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.TriStateCheckbox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.awt.Color
import java.awt.GraphicsEnvironment
import java.awt.image.BufferedImage
import javax.swing.UIManager

/**
 * Unit tests for [FilterUIGroup] header presentation, count badges,
 * action button styling, and visibility toggling.
 */
class FilterUIGroupTests {

  private lateinit var listener: FiltersList.FiltersListener
  private lateinit var filterA: Filter
  private lateinit var filterB: Filter
  private lateinit var filterC: Filter

  @Before
  fun setUp() {
    assumeFalse(GraphicsEnvironment.isHeadless())
    listener = mock(FiltersList.FiltersListener::class.java)
    filterA = Filter("FilterA", "patternA", Color.RED, LogLevel.DEBUG, false)
    filterA.isApplied = true
    filterB = Filter("FilterB", "patternB", Color.BLUE, LogLevel.INFO, false)
    filterB.isApplied = true
    filterC = Filter(
      "FilterC", "patternC", Color.GREEN, LogLevel.WARNING, false
    )
    filterC.isApplied = false
  }

  private fun createGroup(
    groupName: String = "TestGroup",
    filters: Array<Filter> = arrayOf(filterA, filterB)
  ): FilterUIGroup {
    return FilterUIGroup(
      groupName,
      filters,
      { null },
      { listener },
      null
    )
  }

  @Test
  fun testHeaderDisplaysGroupNameWithFilterCount() {
    val group = createGroup("MyFilters", arrayOf(filterA, filterB, filterC))
    val text = group.hideGroupBtn.text
    assertTrue(text.startsWith(StringUtils.DOWN_ARROW_HEAD))
    assertTrue(text.contains("MyFilters"))
    assertTrue(text.endsWith("(3)"))
  }

  @Test
  fun testHeaderUpdatesChevronAndCountOnToggleVisibility() {
    val group = createGroup("Network", arrayOf(filterA, filterB))
    assertTrue(group.isGroupVisible)
    assertTrue(group.hideGroupBtn.text.contains(StringUtils.DOWN_ARROW_HEAD))

    group.toggleGroupVisibility()
    assertFalse(group.isGroupVisible)
    assertTrue(group.hideGroupBtn.text.contains(StringUtils.RIGHT_ARROW_HEAD))
    assertTrue(group.hideGroupBtn.text.endsWith("(2)"))
    verify(listener).onGroupVisibilityChanged("Network")

    group.toggleGroupVisibility()
    assertTrue(group.isGroupVisible)
    assertTrue(group.hideGroupBtn.text.contains(StringUtils.DOWN_ARROW_HEAD))
    assertTrue(group.hideGroupBtn.text.endsWith("(2)"))
  }

  @Test
  fun testForceGroupVisibilitySilently() {
    val group = createGroup("Silent", arrayOf(filterA))
    group.forceGroupVisibilitySilently(false)
    assertFalse(group.isGroupVisible)
    assertTrue(group.hideGroupBtn.text.contains(StringUtils.RIGHT_ARROW_HEAD))

    group.forceGroupVisibilitySilently(true)
    assertTrue(group.isGroupVisible)
    assertTrue(group.hideGroupBtn.text.contains(StringUtils.DOWN_ARROW_HEAD))
  }

  @Test
  fun testHeaderUpdatesCountWhenListDataChanges() {
    val group = createGroup("Dynamic", arrayOf(filterA))
    assertTrue(group.hideGroupBtn.text.endsWith("(1)"))

    group.setListData(arrayOf(filterA, filterB, filterC))
    assertTrue(group.hideGroupBtn.text.endsWith("(3)"))

    group.setListData(emptyArray())
    assertTrue(group.hideGroupBtn.text.endsWith("(0)"))
  }

  @Test
  fun testHeaderOptionsPaneStylingAndBorder() {
    val group = createGroup()
    assertNotNull(group.optionsPane)
    assertFalse(group.isOpaque)
    assertFalse(group.optionsPane.isOpaque)
    assertFalse(group.list.isOpaque)
    assertNotNull(group.optionsPane.background)
    assertNotNull(group.optionsPane.border)
    assertNotNull(group.border)
  }

  @Test
  fun testActionButtonsToolbarButtonStyling() {
    val expectedType = FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON
    val group = createGroup()

    assertEquals(
      expectedType,
      group.selectAllCb.getClientProperty(FlatClientProperties.BUTTON_TYPE)
    )
    assertEquals(
      expectedType,
      group.saveBtn.getClientProperty(FlatClientProperties.BUTTON_TYPE)
    )
    assertEquals(
      expectedType,
      group.addBtn.getClientProperty(FlatClientProperties.BUTTON_TYPE)
    )
    assertEquals(
      expectedType,
      group.prevBtn.getClientProperty(FlatClientProperties.BUTTON_TYPE)
    )
    assertEquals(
      expectedType,
      group.nextBtn.getClientProperty(FlatClientProperties.BUTTON_TYPE)
    )
    assertEquals(
      expectedType,
      group.closeBtn.getClientProperty(FlatClientProperties.BUTTON_TYPE)
    )
    assertEquals(
      expectedType,
      group.hideGroupBtn.getClientProperty(FlatClientProperties.BUTTON_TYPE)
    )
  }

  @Test
  fun testActionButtonsHaveTooltips() {
    val group = createGroup()
    assertNotNull(group.selectAllCb.toolTipText)
    assertFalse(group.selectAllCb.toolTipText.isEmpty())

    assertNotNull(group.saveBtn.toolTipText)
    assertFalse(group.saveBtn.toolTipText.isEmpty())

    assertNotNull(group.addBtn.toolTipText)
    assertFalse(group.addBtn.toolTipText.isEmpty())

    assertNotNull(group.prevBtn.toolTipText)
    assertFalse(group.prevBtn.toolTipText.isEmpty())

    assertNotNull(group.nextBtn.toolTipText)
    assertFalse(group.nextBtn.toolTipText.isEmpty())

    assertNotNull(group.closeBtn.toolTipText)
    assertFalse(group.closeBtn.toolTipText.isEmpty())
  }

  @Test
  fun testSaveButtonVisibilityIndication() {
    val group = createGroup()
    assertFalse(group.saveBtn.isVisible)

    group.showUnsavedIndication(true)
    assertTrue(group.saveBtn.isVisible)

    group.showUnsavedIndication(false)
    assertFalse(group.saveBtn.isVisible)
  }

  @Test
  fun testSelectAllCheckboxStateReflectsAppliedFilters() {
    val allApplied = createGroup("All", arrayOf(filterA, filterB))
    assertEquals(
      TriStateCheckbox.SelectionState.SELECTED,
      allApplied.selectAllCb.selectionState
    )

    val partiallyApplied = createGroup("Partial", arrayOf(filterA, filterC))
    assertEquals(
      TriStateCheckbox.SelectionState.PARTIALLY_SELECTED,
      partiallyApplied.selectAllCb.selectionState
    )

    val noneApplied = createGroup("None", arrayOf(filterC))
    assertEquals(
      TriStateCheckbox.SelectionState.NOT_SELECTED,
      noneApplied.selectAllCb.selectionState
    )
  }

  @Test
  fun testStylePreservedOnUpdateUI() {
    val group = createGroup()
    val initialOptionsBorder = group.optionsPane.border
    val initialCardBorder = group.border
    assertNotNull(initialOptionsBorder)
    assertNotNull(initialCardBorder)

    group.updateUI()
    assertFalse(group.isOpaque)
    assertFalse(group.optionsPane.isOpaque)
    assertFalse(group.list.isOpaque)
    assertNotNull(group.optionsPane.border)
    assertNotNull(group.border)
    assertTrue(group.hideGroupBtn.text.contains("TestGroup"))
  }

  @Test
  fun testResolveCardArcDefaultAndCustom() {
    val group = createGroup()
    val origArc = UIManager.get("Component.arc")
    try {
      UIManager.put("Component.arc", null)
      assertEquals(UIScaleUtils.dip(8), group.resolveCardArc())

      UIManager.put("Component.arc", 16)
      assertEquals(UIScaleUtils.dip(16), group.resolveCardArc())
    } finally {
      UIManager.put("Component.arc", origArc)
    }
  }

  @Test
  fun testThemeColorsResolutionInLightAndDarkModes() {
    val group = createGroup()

    LogViewerThemeManager.currentTheme = "Light"
    val lightCardBg = group.resolveCardBackground()
    val lightHeaderBg = group.resolveHeaderBackground()
    val lightBorderColor = group.resolveCardBorderColor()
    assertNotNull(lightCardBg)
    assertNotNull(lightHeaderBg)
    assertNotNull(lightBorderColor)
    val expectedLightTableBg = UIManager.getColor("Table.background")
      ?: Color.WHITE
    assertEquals(expectedLightTableBg, lightCardBg)
    val expectedLightHeaderBg = UIManager.getColor("ToolBar.background")
      ?: Color.decode("#f5f6f8")
    assertEquals(expectedLightHeaderBg, lightHeaderBg)

    LogViewerThemeManager.currentTheme = "Dark"
    val darkCardBg = group.resolveCardBackground()
    val darkHeaderBg = group.resolveHeaderBackground()
    val darkBorderColor = group.resolveCardBorderColor()
    assertEquals(Color.decode("#26282c"), darkCardBg)
    assertEquals(Color.decode("#303238"), darkHeaderBg)
    assertNotNull(darkBorderColor)

    // Reset back to Light theme
    LogViewerThemeManager.currentTheme = "Light"
  }

  @Test
  fun testOffscreenCardPaintingWithAlphaAndRoundedCorners() {
    val group = createGroup("RenderGroup", arrayOf(filterA, filterB))
    val width = 300
    val height = 150
    group.setSize(width, height)
    group.doLayout()

    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g2 = image.createGraphics()
    try {
      group.paint(g2)
    } finally {
      g2.dispose()
    }

    val vMargin = UIScaleUtils.dip(FilterUIGroup.MARGIN_VERTICAL_DP)
    val hMargin = UIScaleUtils.dip(FilterUIGroup.MARGIN_HORIZONTAL_DP)
    val cardX = hMargin
    val cardY = vMargin
    val arc = group.resolveCardArc()

    // The pixel at (cardX, cardY) is in the corner cut-out of the rounded arc,
    // so it must have 0 alpha even inside the margin boundary
    val cornerAlpha = (image.getRGB(cardX, cardY) ushr 24) and 0xFF
    assertEquals(0, cornerAlpha)

    // Interior card area inside the arc curve must be fully painted (alpha > 0)
    val interiorX = cardX + arc
    val interiorY = cardY + arc
    val interiorAlpha = (image.getRGB(interiorX, interiorY) ushr 24) and 0xFF
    assertTrue(interiorAlpha > 0)

    // Header center must have painted pixels (alpha > 0)
    val headerCenterX = width / 2
    val headerCenterY = group.optionsPane.y + (group.optionsPane.height / 2)
    val headerAlpha = (image.getRGB(headerCenterX, headerCenterY) ushr 24) and 0xFF
    assertTrue(headerAlpha > 0)
  }
}
