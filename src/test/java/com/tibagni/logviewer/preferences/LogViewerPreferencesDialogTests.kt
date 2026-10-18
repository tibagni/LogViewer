package com.tibagni.logviewer.preferences

import com.tibagni.logviewer.i18n.I18n
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.GraphicsEnvironment
import java.util.Locale

class LogViewerPreferencesDialogTests {

  @Test
  fun testPreferencePanelIds() {
    if (GraphicsEnvironment.isHeadless()) {
      return
    }

    val dialog = LogViewerPreferencesDialog(null)
    try {
      val ids = dialog.preferencePanels.map { it.id }
      assertEquals(4, ids.size)
      assertEquals(AppearancePreferencePanel.ID, ids[0])
      assertEquals(LogsPreferencePanel.ID, ids[1])
      assertEquals(FiltersPreferencePanel.ID, ids[2])
      assertEquals(ToolsPreferencePanel.ID, ids[3])
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
    } finally {
      I18n.setLocale(originalLocale)
    }
  }

  @Test
  fun testDialogInstantiationInNonHeadless() {
    if (GraphicsEnvironment.isHeadless()) {
      return
    }

    val dialog = LogViewerPreferencesDialog(null)
    assertNotNull(dialog)
    assertTrue(dialog.isModal)
    assertEquals(I18n.get(I18n.PREF_DIALOG_TITLE), dialog.title)
    dialog.onCancel()
  }

  @Test
  fun testPathTextFieldHelper() {
    if (GraphicsEnvironment.isHeadless()) {
      return
    }

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
      override val panel = javax.swing.JPanel()
      override fun apply() {}
    }
    org.junit.Assert.assertFalse(dummy.hasTimeConsumingOperation)
  }
}
