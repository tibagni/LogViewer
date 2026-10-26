package com.tibagni.logviewer.log

import com.formdev.flatlaf.ui.FlatLineBorder
import com.tibagni.logviewer.filter.Filter
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.StatusBadge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.awt.Color
import java.awt.GraphicsEnvironment
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.UIManager
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder

class LogLineDetailsDialogTests {

  @Test
  fun testLogLineInfoProperties() {
    val tempFile = File.createTempFile("testLog", ".txt")
    tempFile.deleteOnExit()
    val entry = LogEntry(
      "some log line",
      LogLevel.INFO,
      LogTimestamp(1, 2, 3, 4, 5, 6),
      LogStream.SYSTEM,
      tempFile,
      50
    )
    val filter = Filter("TestFilter", "pattern", Color.RED, LogLevel.INFO)
    val info = LogLineInfo(
      entry = entry,
      sourceFile = tempFile,
      lineNumber = 50,
      logStream = LogStream.SYSTEM,
      logLevel = LogLevel.INFO,
      timestamp = LogTimestamp(1, 2, 3, 4, 5, 6),
      pid = 1234,
      tid = 5678,
      tag = "TestTag",
      message = "Hello world",
      appliedFilter = filter,
      index = 10
    )

    assertEquals(tempFile, info.sourceFile)
    assertEquals(tempFile.name, info.fileName)
    assertEquals(tempFile.absolutePath, info.filePath)
    assertEquals(50, info.lineNumber)
    assertEquals("50", info.formattedLineNumber)
    assertTrue(info.hasFile)
    assertEquals(LogStream.SYSTEM, info.logStream)
    assertEquals(LogLevel.INFO, info.logLevel)
    assertEquals(1234, info.pid)
    assertEquals("1234", info.formattedPid)
    assertEquals(5678, info.tid)
    assertEquals("5678", info.formattedTid)
    assertEquals("TestTag", info.tag)
    assertEquals("TestTag", info.formattedTag)
    assertEquals("Hello world", info.message)
    assertEquals(filter, info.appliedFilter)
    assertEquals(10, info.index)
    assertEquals("11", info.formattedIndex)
    assertEquals("01-02 03:04:05.006", info.formattedTimestamp)
  }

  @Test
  fun testLogLineInfoDefaultsWhenFieldsNull() {
    val entry = LogEntry("some log line", LogLevel.DEBUG, null)
    val info = LogLineInfo(
      entry = entry,
      sourceFile = null,
      lineNumber = -1,
      logStream = LogStream.UNKNOWN,
      logLevel = LogLevel.DEBUG,
      timestamp = null,
      pid = null,
      tid = null,
      tag = null,
      message = "some log line",
      appliedFilter = null,
      index = -1
    )

    assertNull(info.sourceFile)
    assertEquals("-", info.fileName)
    assertEquals("-", info.filePath)
    assertEquals(-1, info.lineNumber)
    assertEquals("-", info.formattedLineNumber)
    assertFalse(info.hasFile)
    assertNull(info.pid)
    assertEquals("-", info.formattedPid)
    assertNull(info.tid)
    assertEquals("-", info.formattedTid)
    assertNull(info.tag)
    assertEquals("-", info.formattedTag)
    assertEquals("-", info.formattedIndex)
    assertEquals("-", info.formattedTimestamp)
  }

  @Test
  fun testDialogInstantiationInHeadlessMode() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val file = File("/path/to/log.txt")
    val entry = LogEntry(
      "10-12 22:33:46.839 3172 3172 V Tag: message",
      LogLevel.VERBOSE,
      null,
      LogStream.MAIN,
      file,
      100
    )
    val info = LogLineParser.parseLineInfo(entry)

    val dialog = LogLineDetailsDialog(
      owner = null,
      info = info!!,
      onFilterByPid = { _ -> },
      onFilterByTag = { _ -> },
      onOpenInEditor = { _, _ -> }
    )

    assertNotNull(dialog)
    dialog.dispose()
  }

  @Test
  fun testSectionBorderStyling() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val border = LogLineDetailsDialog.createSectionBorder("Test Section")
    assertTrue(border is CompoundBorder)
    val compoundBorder = border as CompoundBorder

    assertTrue(compoundBorder.outsideBorder is TitledBorder)
    val titledBorder = compoundBorder.outsideBorder as TitledBorder
    assertEquals("Test Section", titledBorder.title)

    assertTrue(titledBorder.border is FlatLineBorder)
    val lineBorder = titledBorder.border as FlatLineBorder
    assertEquals(UIScaleUtils.dip(8), lineBorder.arc)
    assertEquals(1f, lineBorder.lineThickness, 0.001f)
    assertNotNull(lineBorder.lineColor)

    assertTrue(compoundBorder.insideBorder is EmptyBorder)
    val insideBorder = compoundBorder.insideBorder as EmptyBorder
    assertEquals(UIScaleUtils.dip(6), insideBorder.borderInsets.top)
    assertEquals(UIScaleUtils.dip(8), insideBorder.borderInsets.left)
    assertEquals(UIScaleUtils.dip(6), insideBorder.borderInsets.bottom)
    assertEquals(UIScaleUtils.dip(8), insideBorder.borderInsets.right)
  }

  @Test
  fun testDialogComponentsAndStyling() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val tempFile = File.createTempFile("testLog", ".txt")
    tempFile.deleteOnExit()
    val entry = LogEntry(
      "05-18 12:00:00.000 100 200 I TestTag: test message",
      LogLevel.INFO,
      LogTimestamp(5, 18, 12, 0, 0, 0),
      LogStream.MAIN,
      tempFile,
      42
    )
    val filter = Filter("TestFilter", "pattern", Color.BLUE, LogLevel.INFO)
    val info = LogLineInfo(
      entry = entry,
      sourceFile = tempFile,
      lineNumber = 42,
      logStream = LogStream.MAIN,
      logLevel = LogLevel.INFO,
      timestamp = LogTimestamp(5, 18, 12, 0, 0, 0),
      pid = 100,
      tid = 200,
      tag = "TestTag",
      message = "test message",
      appliedFilter = filter,
      index = 5
    )

    val dialog = LogLineDetailsDialog(
      owner = null,
      info = info
    )
    assertNotNull(dialog)

    // Verify section borders
    val sections = listOf(
      dialog.sourceSection to I18n.get(I18n.LOG_DETAILS_SOURCE_SECTION),
      dialog.metadataSection to I18n.get(
        I18n.LOG_DETAILS_METADATA_SECTION
      ),
      dialog.messageSection to I18n.get(I18n.LOG_DETAILS_MESSAGE_SECTION)
    )
    for ((section, expectedTitle) in sections) {
      assertTrue(section.border is CompoundBorder)
      val compound = section.border as CompoundBorder
      assertTrue(compound.outsideBorder is TitledBorder)
      val titled = compound.outsideBorder as TitledBorder
      assertEquals(expectedTitle, titled.title)
      assertTrue(titled.border is FlatLineBorder)
      val flat = titled.border as FlatLineBorder
      assertEquals(UIScaleUtils.dip(8), flat.arc)
    }

    // Verify action button icons and gap
    assertNotNull(dialog.openEditorBtn.icon)
    assertEquals(UIScaleUtils.dip(4), dialog.openEditorBtn.iconTextGap)

    // Verify text area and scroll pane styling
    assertTrue(dialog.messageTextArea.border is EmptyBorder)
    val textInsets = (
      dialog.messageTextArea.border as EmptyBorder
    ).borderInsets
    assertEquals(UIScaleUtils.dip(6), textInsets.top)
    assertEquals(UIScaleUtils.dip(8), textInsets.left)
    assertEquals(UIScaleUtils.dip(6), textInsets.bottom)
    assertEquals(UIScaleUtils.dip(8), textInsets.right)

    assertTrue(dialog.messageScrollPane.border is FlatLineBorder)
    val scrollBorder = dialog.messageScrollPane.border as FlatLineBorder
    assertEquals(UIScaleUtils.dip(4), scrollBorder.arc)
    assertEquals(1f, scrollBorder.lineThickness, 0.001f)

    // Verify rounded indicator pills
    assertNotNull(dialog.levelIndicator)
    assertFalse(dialog.levelIndicator!!.isOpaque)
    assertTrue(
      dialog.levelIndicator is LogLineDetailsDialog.RoundedColorIndicator
    )
    val levelPill =
      dialog.levelIndicator as LogLineDetailsDialog.RoundedColorIndicator
    assertEquals(UIScaleUtils.dip(4), levelPill.arc)

    assertNotNull(dialog.filterColorSquare)
    assertFalse(dialog.filterColorSquare!!.isOpaque)
    assertTrue(
      dialog.filterColorSquare is
        LogLineDetailsDialog.RoundedColorIndicator
    )
    val filterPill =
      dialog.filterColorSquare as LogLineDetailsDialog.RoundedColorIndicator
    assertEquals(UIScaleUtils.dip(4), filterPill.arc)

    // Verify stream badge
    assertNotNull(dialog.streamBadge)
    assertEquals(StatusBadge.Variant.NORMAL, dialog.streamBadge.variant)
    assertEquals(info.logStream.name, dialog.streamBadge.text)
    assertTrue(dialog.streamBadge.isVisible)

    // Verify index badge
    assertNotNull(dialog.indexBadge)
    assertEquals(StatusBadge.Variant.NORMAL, dialog.indexBadge.variant)
    assertEquals("#${info.formattedIndex}", dialog.indexBadge.text)
    assertTrue(dialog.indexBadge.isVisible)

    // Verify message metrics badge for single-line message
    assertNotNull(dialog.messageMetricsBadge)
    assertEquals(
      StatusBadge.Variant.NORMAL,
      dialog.messageMetricsBadge.variant
    )
    assertEquals("12 chars", dialog.messageMetricsBadge.text)
    assertTrue(dialog.messageMetricsBadge.isVisible)

    dialog.dispose()
  }

  @Test
  fun testMessageMetricsBadgeMultiLine() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val multiLineMsg = "Line 1\nLine 2\nLine 3"
    val entry = LogEntry(
      multiLineMsg,
      LogLevel.DEBUG,
      null
    )
    val info = LogLineInfo(
      entry = entry,
      sourceFile = null,
      lineNumber = -1,
      logStream = LogStream.SYSTEM,
      logLevel = LogLevel.DEBUG,
      timestamp = null,
      pid = null,
      tid = null,
      tag = null,
      message = multiLineMsg,
      appliedFilter = null,
      index = -1
    )

    val dialog = LogLineDetailsDialog(owner = null, info = info)
    try {
      assertNotNull(dialog.messageMetricsBadge)
      assertEquals(
        StatusBadge.Variant.NORMAL,
        dialog.messageMetricsBadge.variant
      )
      val expectedText = "3 lines, ${multiLineMsg.length} chars"
      assertEquals(expectedText, dialog.messageMetricsBadge.text)
      assertTrue(dialog.messageMetricsBadge.isVisible)
    } finally {
      dialog.dispose()
    }
  }

  @Test
  fun testKeyLabelDisabledForegroundColorResolution() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val original = UIManager.getColor("Label.disabledForeground")
    try {
      // 1. When UIManager defines Label.disabledForeground
      val testColor = Color(123, 45, 67)
      UIManager.put("Label.disabledForeground", testColor)
      val resolved = LogLineDetailsDialog.resolveKeyLabelColor()
      assertEquals(testColor, resolved)

      val label = LogLineDetailsDialog.createKeyLabel("Test Key")
      assertEquals("Test Key", label.text)
      assertEquals(testColor, label.foreground)

      // 2. When UIManager has no Label.disabledForeground (fallback)
      UIManager.put("Label.disabledForeground", null)
      val fallbackResolved = LogLineDetailsDialog.resolveKeyLabelColor()
      assertNotNull(fallbackResolved)
      val fallbackLabel = LogLineDetailsDialog.createKeyLabel("Fallback")
      assertEquals(fallbackResolved, fallbackLabel.foreground)
    } finally {
      UIManager.put("Label.disabledForeground", original)
    }
  }

  @Test
  fun testRoundedColorIndicator() {
    assumeFalse(GraphicsEnvironment.isHeadless())

    val indicator = LogLineDetailsDialog.RoundedColorIndicator(
      Color.GREEN,
      UIScaleUtils.dip(6)
    )
    assertFalse(indicator.isOpaque)
    assertEquals(Color.GREEN, indicator.background)
    assertEquals(UIScaleUtils.dip(6), indicator.arc)

    val image = BufferedImage(
      16,
      16,
      BufferedImage.TYPE_INT_ARGB
    )
    val g2d = image.createGraphics()
    try {
      indicator.setBounds(0, 0, 16, 16)
      indicator.paint(g2d)
    } finally {
      g2d.dispose()
    }
  }
}
