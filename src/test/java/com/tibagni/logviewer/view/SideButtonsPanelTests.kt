package com.tibagni.logviewer.view

import com.tibagni.logviewer.theme.LogViewerThemeManager
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import org.junit.Assert.*
import org.junit.Test

class SideButtonsPanelTests {

  @Test
  fun testSideButtonsPanelNonExpandable() {
    var actionClicked = false
    var toggleState = false

    val panel = SideButtonsPanel(
      isExpandable = false,
      entries = listOf(
        SidePanelEntry.Action(
          id = "action_1",
          icon = CommonIcons.clearIcon(),
          text = "Clear"
        ) { actionClicked = true },
        SidePanelEntry.Separator,
        SidePanelEntry.Toggle(
          id = "toggle_1",
          icon = CommonIcons.autoScrollIcon(),
          text = "Auto-scroll",
          initialActive = false
        ) { toggleState = it }
      )
    )

    assertNull(panel.toggleExpandBtn)
    assertEquals(UIScaleUtils.dip(SideButton.COLLAPSED_WIDTH), panel.preferredSize.width)

    // Verify action button
    assertTrue(panel.isItemEnabled("action_1"))
    panel.performAction("action_1")
    assertTrue(actionClicked)

    panel.setItemEnabled("action_1", false)
    assertFalse(panel.isItemEnabled("action_1"))

    // Verify toggle button
    assertFalse(panel.isItemActive("toggle_1"))
    panel.setItemActive("toggle_1", true)
    assertTrue(panel.isItemActive("toggle_1"))

    panel.performAction("toggle_1")
    assertFalse(panel.isItemActive("toggle_1"))
    assertFalse(toggleState)

    // Cannot expand when isExpandable = false
    panel.isExpanded = true
    assertFalse(panel.isExpanded)
    assertEquals(UIScaleUtils.dip(SideButton.COLLAPSED_WIDTH), panel.preferredSize.width)
  }

  @Test
  fun testSideButtonsPanelExpandable() {
    val panel = SideButtonsPanel(
      isExpandable = true,
      entries = listOf(
        SidePanelEntry.Action(
          id = "action_1",
          icon = CommonIcons.stopIcon(),
          text = "Stop"
        ) {}
      )
    )

    assertNotNull(panel.toggleExpandBtn)
    assertFalse(panel.isExpanded)
    assertEquals(UIScaleUtils.dip(SideButton.COLLAPSED_WIDTH), panel.preferredSize.width)

    val actionBtn = panel.getButton("action_1")
    assertNotNull(actionBtn)
    assertFalse(actionBtn!!.isExpanded)

    // Expand panel
    panel.isExpanded = true
    assertTrue(panel.isExpanded)
    assertEquals(UIScaleUtils.dip(SideButton.EXPANDED_WIDTH), panel.preferredSize.width)
    assertTrue(actionBtn.isExpanded)

    // Click toggle expand button to collapse
    panel.toggleExpandBtn!!.doClick()
    assertFalse(panel.isExpanded)
    assertEquals(UIScaleUtils.dip(SideButton.COLLAPSED_WIDTH), panel.preferredSize.width)
    assertFalse(actionBtn.isExpanded)
  }

  @Test
  fun testUpdateItem() {
    val panel = SideButtonsPanel(
      isExpandable = false,
      entries = listOf(
        SidePanelEntry.Action(
          id = "action_1",
          icon = CommonIcons.stopIcon(),
          text = "Stop"
        ) {}
      )
    )

    val button = panel.getButton("action_1")
    assertNotNull(button)
    assertEquals("Stop", button!!.toolTipText)

    panel.updateItem("action_1", CommonIcons.playIcon(), "Start")
    assertEquals("Start", button.toolTipText)
  }

  @Test
  fun testThemeChangeUpdatesUI() {
    LogViewerThemeManager.currentTheme = "Light"
    val panel = SideButtonsPanel(
      isExpandable = true,
      entries = listOf(
        SidePanelEntry.Action("btn", CommonIcons.clearIcon(), "Clear") {}
      )
    )

    assertNotNull(panel.border)

    LogViewerThemeManager.currentTheme = "Dark"
    panel.updateUI()
    assertNotNull(panel.border)
  }
}
