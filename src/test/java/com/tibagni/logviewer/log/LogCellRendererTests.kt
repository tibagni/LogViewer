package com.tibagni.logviewer.log

import com.tibagni.logviewer.filter.Filter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.awt.Color
import java.awt.GraphicsEnvironment
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.UIManager
import javax.swing.plaf.ColorUIResource
import javax.swing.table.DefaultTableModel

/**
 * Unit tests verifying [LogCellRenderer] cell rendering, color styling,
 * dimension calculations, and fallback model resolution.
 */
class LogCellRendererTests {

  /**
   * Subclass exposing the protected text view for test assertion inspection.
   */
  private class TestableLogCellRenderer : LogCellRenderer() {
    val renderedTextView: JTextArea
      get() = textView
  }

  /**
   * Verifies that selected rows use the table's selection foreground and background colors.
   */
  @Test
  fun testSelectedRowColors() {
    if (GraphicsEnvironment.isHeadless()) return

    val table = JTable(DefaultTableModel(1, 1))
    table.selectionForeground = Color.YELLOW
    table.selectionBackground = Color.BLUE

    val renderer = TestableLogCellRenderer()
    val entry = LogEntry("Test log entry", LogLevel.INFO, null)

    renderer.getTableCellRendererComponent(table, entry, true, false, 0, 0)

    assertEquals(Color.YELLOW, renderer.renderedTextView.foreground)
    assertEquals(Color.BLUE, renderer.renderedTextView.background)
  }

  /**
   * Verifies that a row designated as highlighted is styled identically to a selected row.
   */
  @Test
  fun testHighlightedRowTreatedAsSelected() {
    if (GraphicsEnvironment.isHeadless()) return

    val table = JTable(DefaultTableModel(2, 1))
    table.selectionForeground = Color.CYAN
    table.selectionBackground = Color.MAGENTA
    LogCellRenderer.setHighlightLine(table, 1)

    val renderer = TestableLogCellRenderer()
    val entry = LogEntry("Highlighted entry", LogLevel.VERBOSE, null)

    renderer.getTableCellRendererComponent(table, entry, false, false, 1, 0)

    assertEquals(Color.CYAN, renderer.renderedTextView.foreground)
    assertEquals(Color.MAGENTA, renderer.renderedTextView.background)
  }

  /**
   * Verifies that non-selected rows with an applied filter adopt the filter's foreground color.
   */
  @Test
  fun testFilteredLogEntryForegroundNonSelected() {
    if (GraphicsEnvironment.isHeadless()) return

    val table = JTable(DefaultTableModel(1, 1))
    table.foreground = Color.BLACK
    table.background = Color.WHITE

    val filter = Filter("ErrorFilter", "error.*", Color.RED, LogLevel.ERROR)
    val entry = LogEntry("Error: disk failure", LogLevel.ERROR, null)
    entry.appliedFilter = filter

    val renderer = TestableLogCellRenderer()
    renderer.getTableCellRendererComponent(table, entry, false, false, 0, 0)

    assertEquals(Color.RED, renderer.renderedTextView.foreground)
    assertEquals(Color.WHITE, renderer.renderedTextView.background)
  }

  /**
   * Verifies that unselected and unfiltered rows adopt the standard table
   * foreground and background.
   */
  @Test
  fun testUnfilteredLogEntryColorsNonSelected() {
    if (GraphicsEnvironment.isHeadless()) return

    val table = JTable(DefaultTableModel(1, 1))
    table.foreground = Color.DARK_GRAY
    table.background = Color.WHITE

    val entry = LogEntry("Standard line", LogLevel.INFO, null)
    val renderer = TestableLogCellRenderer()

    renderer.getTableCellRendererComponent(table, entry, false, false, 0, 0)

    assertEquals(Color.DARK_GRAY, renderer.renderedTextView.foreground)
    assertEquals(Color.WHITE, renderer.renderedTextView.background)
  }

  /**
   * Verifies alternating row striping when table background is a UIResource
   * and alternate color exists.
   */
  @Test
  fun testAlternateTableRowBackgroundForOddRow() {
    if (GraphicsEnvironment.isHeadless()) return

    val table = JTable(DefaultTableModel(2, 1))
    table.background = ColorUIResource(Color.WHITE)

    val originalAlt = UIManager.getColor("Table.alternateRowColor")
    val alternateColor = Color(245, 245, 245)
    UIManager.put("Table.alternateRowColor", alternateColor)
    try {
      val renderer = TestableLogCellRenderer()
      val entry = LogEntry("Row content", LogLevel.DEBUG, null)

      // Odd row (1) should receive the alternate row background
      renderer.getTableCellRendererComponent(table, entry, false, false, 1, 0)
      assertEquals(alternateColor, renderer.renderedTextView.background)

      // Even row (0) should receive the default table background
      renderer.getTableCellRendererComponent(table, entry, false, false, 0, 0)
      assertEquals(table.background, renderer.renderedTextView.background)
    } finally {
      UIManager.put("Table.alternateRowColor", originalAlt)
    }
  }

  /**
   * Verifies that explicit non-UIResource backgrounds are preserved even for odd rows.
   */
  @Test
  fun testCustomNonUIResourceBackgroundDoesNotUseAlternate() {
    if (GraphicsEnvironment.isHeadless()) return

    val table = JTable(DefaultTableModel(2, 1))
    val customBg = Color(210, 220, 230)
    table.background = customBg // Explicit Color, not ColorUIResource

    val originalAlt = UIManager.getColor("Table.alternateRowColor")
    UIManager.put("Table.alternateRowColor", Color.PINK)
    try {
      val renderer = TestableLogCellRenderer()
      val entry = LogEntry("Row content", LogLevel.DEBUG, null)

      renderer.getTableCellRendererComponent(table, entry, false, false, 1, 0)
      assertEquals(customBg, renderer.renderedTextView.background)
    } finally {
      UIManager.put("Table.alternateRowColor", originalAlt)
    }
  }

  /**
   * Verifies that passing a null value falls back to retrieving the entry from the table model.
   */
  @Test
  fun testFallbackRetrievalWhenValueIsNull() {
    if (GraphicsEnvironment.isHeadless()) return

    val model = DefaultTableModel(1, 1)
    val entry = LogEntry("Fallback text from model", LogLevel.WARNING, null)
    model.setValueAt(entry, 0, 0)

    val table = JTable(model)
    val renderer = TestableLogCellRenderer()

    val comp = renderer.getTableCellRendererComponent(table, null, false, false, 0, 0)

    assertNotNull(comp)
    assertEquals(renderer, comp)
    assertEquals("Fallback text from model", renderer.renderedTextView.text)
  }

  /**
   * Verifies that passing null value when the table model also has null returns null.
   */
  @Test
  fun testFallbackRetrievalReturnsNullWhenModelValueIsNull() {
    if (GraphicsEnvironment.isHeadless()) return

    val model = DefaultTableModel(1, 1)
    model.setValueAt(null, 0, 0)

    val table = JTable(model)
    val renderer = TestableLogCellRenderer()

    val comp = renderer.getTableCellRendererComponent(table, null, false, false, 0, 0)

    assertNull(comp)
  }

  /**
   * Verifies that cell width matches column width and table row height is updated.
   */
  @Test
  fun testConfigureCellDimensionsUpdatesRowHeight() {
    if (GraphicsEnvironment.isHeadless()) return

    val table = JTable(DefaultTableModel(1, 1))
    table.columnModel.getColumn(0).width = 450
    table.setRowHeight(0, 10)

    val renderer = TestableLogCellRenderer()
    val entry = LogEntry("Sample line for sizing test", LogLevel.INFO, null)

    renderer.getTableCellRendererComponent(table, entry, false, false, 0, 0)

    assertEquals(450, renderer.width)
    assertEquals(renderer.preferredSize.height, table.getRowHeight(0))
  }
}
