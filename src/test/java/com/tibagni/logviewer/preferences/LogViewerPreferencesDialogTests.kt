package com.tibagni.logviewer.preferences

import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.ui.FlatLineBorder
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.theme.LogViewerThemeManager
import com.tibagni.logviewer.util.LogFontManager
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.CommonIcons
import java.awt.CardLayout
import java.awt.Cursor
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.io.File
import java.util.Locale
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class LogViewerPreferencesDialogTests {

  @Test
  fun testPreferencePanelIds() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val dialog = LogViewerPreferencesDialog(null)
    try {
      val ids = dialog.preferencePanels.map { it.id }
      assertEquals(5, ids.size)
      assertEquals(AppearancePreferencePanel.ID, ids[0])
      assertEquals(LogsPreferencePanel.ID, ids[1])
      assertEquals(FiltersPreferencePanel.ID, ids[2])
      assertEquals(ToolsPreferencePanel.ID, ids[3])
      assertEquals(KeymapPreferencePanel.ID, ids[4])

      dialog.selectCategory(KeymapPreferencePanel.ID)
      val selected = dialog.categoriesList.selectedValue?.id
      assertEquals(KeymapPreferencePanel.ID, selected)
    } finally {
      dialog.onCancel()
    }
  }

  @Test
  fun testCategoryTitlesEnglish() {
    val originalLocale = I18n.getLocale()
    try {
      I18n.setLocale(Locale.US)
      assertEquals("Appearance", I18n.get(I18n.PREF_CATEGORY_APPEARANCE))
      assertEquals("Logs", I18n.get(I18n.PREF_CATEGORY_LOGS))
      assertEquals("Filters", I18n.get(I18n.PREF_CATEGORY_FILTERS))
      assertEquals("Tools", I18n.get(I18n.PREF_CATEGORY_TOOLS))
      assertEquals("Keymap", I18n.get(I18n.PREF_CATEGORY_KEYMAP))
    } finally {
      I18n.setLocale(originalLocale)
    }
  }

  @Test
  fun testCategoryTitlesPortuguese() {
    val originalLocale = I18n.getLocale()
    try {
      I18n.setLocale(Locale("pt", "BR"))
      assertEquals("Aparência", I18n.get(I18n.PREF_CATEGORY_APPEARANCE))
      assertEquals("Logs", I18n.get(I18n.PREF_CATEGORY_LOGS))
      assertEquals("Filtros", I18n.get(I18n.PREF_CATEGORY_FILTERS))
      assertEquals("Ferramentas", I18n.get(I18n.PREF_CATEGORY_TOOLS))
      assertEquals("Atalhos", I18n.get(I18n.PREF_CATEGORY_KEYMAP))
    } finally {
      I18n.setLocale(originalLocale)
    }
  }

  @Test
  fun testDialogInstantiationInNonHeadless() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val dialog = LogViewerPreferencesDialog(null)
    assertNotNull(dialog)
    assertTrue(dialog.isModal)
    assertEquals(I18n.get(I18n.PREF_DIALOG_TITLE), dialog.title)
    dialog.onCancel()
  }

  @Test
  fun testPathTextFieldHelper() {
    val txt = createPathTextField()
    assertNotNull(txt)
    assertEquals(10, txt.columns)
    assertEquals(false, txt.isEditable)

    updatePathText(txt, "/path/to/test")
    assertEquals("/path/to/test", txt.text)
    assertEquals("/path/to/test", txt.toolTipText)
    assertEquals(0, txt.caretPosition)

    updatePathText(txt, null)
    assertEquals("", txt.text)
    assertEquals(null, txt.toolTipText)
  }

  @Test
  fun testHasTimeConsumingOperationDefaultsFalse() {
    val dummy = object : PreferencePanel {
      override val id = "dummy"
      override val title = "Dummy"
      override val panel = JPanel()
      override fun apply() {}
    }
    assertFalse(dummy.hasTimeConsumingOperation)
  }

  @Test
  fun testCreateBrowseButtonHelper() {
    val button = createBrowseButton(
      "Select directory",
      CommonIcons.folderIcon()
    )
    assertNotNull(button)
    assertEquals("Select directory", button.toolTipText)
    assertEquals(Cursor.HAND_CURSOR, button.cursor.type)
    assertNotNull(button.icon)
    assertEquals(16, button.icon.iconWidth)
  }

  @Test
  fun testAppearancePreferencePanelPreviewCard() {
    val mockPrefs = mock(LogViewerPreferences::class.java)
    `when`(mockPrefs.lookAndFeel).thenReturn("Light")
    `when`(mockPrefs.accentColor).thenReturn("Default")
    `when`(mockPrefs.logFontSize).thenReturn(14)
    `when`(mockPrefs.logFontFamily).thenReturn("Monospaced")
    `when`(mockPrefs.logFontBold).thenReturn(false)

    val panel = AppearancePreferencePanel(mockPrefs, LogViewerThemeManager)
    assertNotNull(panel.fontPreviewTxt)
    assertEquals(
      AppearancePreferencePanel.SAMPLE_PREVIEW_TEXT,
      panel.fontPreviewTxt.text
    )
    assertEquals(JTextField.CENTER, panel.fontPreviewTxt.horizontalAlignment)
    assertFalse(panel.fontPreviewTxt.isEditable)

    val border = panel.fontPreviewTxt.border
    assertTrue("Border should be compound border", border is CompoundBorder)
    val compound = border as CompoundBorder
    assertTrue(
      "Outside border should be FlatLineBorder",
      compound.outsideBorder is FlatLineBorder
    )
    val lineBorder = compound.outsideBorder as FlatLineBorder
    assertEquals(UIScaleUtils.dip(8), lineBorder.arc)
    assertTrue(
      "Inside border should be EmptyBorder",
      compound.insideBorder is EmptyBorder
    )
  }

  @Test
  fun testAppearancePreferencePanelFontUpdates() {
    val mockPrefs = mock(LogViewerPreferences::class.java)
    `when`(mockPrefs.lookAndFeel).thenReturn("Light")
    `when`(mockPrefs.accentColor).thenReturn("Default")
    `when`(mockPrefs.logFontSize).thenReturn(16)
    `when`(mockPrefs.logFontFamily).thenReturn("Monospaced")
    `when`(mockPrefs.logFontBold).thenReturn(true)

    val panel = AppearancePreferencePanel(mockPrefs, LogViewerThemeManager)
    val expectedFont = LogFontManager.getFont("Monospaced", Font.BOLD, 16)
    assertEquals(expectedFont.family, panel.fontPreviewTxt.font.family)
    assertEquals(expectedFont.size, panel.fontPreviewTxt.font.size)
    assertEquals(expectedFont.style, panel.fontPreviewTxt.font.style)
  }

  @Test
  fun testPreferencesDialogCardAndSidebarProperties() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val dialog = LogViewerPreferencesDialog(null)
    try {
      assertNotNull(dialog.splitPane)
      assertNotNull(dialog.cardsPanel)
      assertNotNull(dialog.sidebarScrollPane)
      assertNotNull(dialog.categoriesList)

      assertTrue(
        "splitPane border should be FlatLineBorder",
        dialog.splitPane.border is FlatLineBorder
      )
      val splitBorder = dialog.splitPane.border as FlatLineBorder
      assertEquals(UIScaleUtils.dip(8), splitBorder.arc)

      assertEquals(Cursor.HAND_CURSOR, dialog.categoriesList.cursor.type)
      assertNotNull(
        dialog.categoriesList.getClientProperty(FlatClientProperties.STYLE)
      )
      assertTrue(dialog.cardsPanel.layout is CardLayout)
      assertEquals(
        dialog.categoriesList,
        dialog.sidebarScrollPane.viewport.view
      )
    } finally {
      dialog.onCancel()
    }
  }

  @Test
  fun testPanelsBrowseButtonsProperties() {
    val mockPrefs = mock(LogViewerPreferences::class.java)
    val dummyDir = File("/test/logs")
    `when`(mockPrefs.defaultLogsPath).thenReturn(dummyDir)
    `when`(mockPrefs.defaultFiltersPath).thenReturn(dummyDir)
    `when`(mockPrefs.preferredTextEditor).thenReturn(File("/test/editor"))
    `when`(mockPrefs.adbPath).thenReturn(File("/test/adb"))

    val container = JPanel()
    val logsPanel = LogsPreferencePanel(container, mockPrefs)
    assertNotNull(logsPanel.logsPathBtn.icon)
    assertEquals(Cursor.HAND_CURSOR, logsPanel.logsPathBtn.cursor.type)
    assertEquals(
      I18n.get(I18n.PREF_DEFAULT_LOGS_PATH),
      logsPanel.logsPathBtn.toolTipText
    )

    val filtersPanel = FiltersPreferencePanel(container, mockPrefs)
    assertNotNull(filtersPanel.filtersPathBtn.icon)
    assertEquals(Cursor.HAND_CURSOR, filtersPanel.filtersPathBtn.cursor.type)
    assertEquals(
      I18n.get(I18n.PREF_DEFAULT_FILTERS_PATH),
      filtersPanel.filtersPathBtn.toolTipText
    )

    val toolsPanel = ToolsPreferencePanel(container, mockPrefs)
    assertNotNull(toolsPanel.preferredEditorPathBtn.icon)
    assertEquals(
      Cursor.HAND_CURSOR,
      toolsPanel.preferredEditorPathBtn.cursor.type
    )
    assertNotNull(toolsPanel.adbPathBtn.icon)
    assertEquals(Cursor.HAND_CURSOR, toolsPanel.adbPathBtn.cursor.type)
    assertEquals(Cursor.HAND_CURSOR, toolsPanel.adbPathClearBtn.cursor.type)
  }
}
