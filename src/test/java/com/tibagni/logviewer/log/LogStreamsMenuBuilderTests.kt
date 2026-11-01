package com.tibagni.logviewer.log

import com.tibagni.logviewer.i18n.I18n
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.GraphicsEnvironment
import javax.swing.JCheckBoxMenuItem

/**
 * Unit tests for [LogStreamsMenuBuilder] verifying menu construction, stream item
 * initialization, and toggle event propagation.
 */
class LogStreamsMenuBuilderTests {

  /**
   * Verifies that buildStreamsMenu returns null when the available streams set is empty.
   */
  @Test
  fun testBuildStreamsMenuReturnsNullWhenStreamsEmpty() {
    if (GraphicsEnvironment.isHeadless()) return

    val menu = LogStreamsMenuBuilder.buildStreamsMenu(
      availableStreams = emptySet(),
      isStreamAllowed = { true },
      onStreamToggled = { _, _ -> }
    )

    assertNull(menu)
  }

  /**
   * Verifies that buildStreamsMenu constructs a menu with checkbox items representing each stream
   * and initializes their selection state according to isStreamAllowed.
   */
  @Test
  fun testBuildStreamsMenuConstructsMenuItemsWithCorrectState() {
    if (GraphicsEnvironment.isHeadless()) return

    val streams = linkedSetOf(LogStream.MAIN, LogStream.SYSTEM)
    val menu = LogStreamsMenuBuilder.buildStreamsMenu(
      availableStreams = streams,
      isStreamAllowed = { it == LogStream.MAIN },
      onStreamToggled = { _, _ -> }
    )

    assertNotNull(menu)
    assertEquals(I18n.get(I18n.MENU_STREAMS), menu?.text)
    assertEquals(2, menu?.itemCount)

    val mainItem = menu?.getItem(0) as JCheckBoxMenuItem
    assertEquals(LogStream.MAIN.toString(), mainItem.text)
    assertTrue(mainItem.isSelected)

    val systemItem = menu.getItem(1) as JCheckBoxMenuItem
    assertEquals(LogStream.SYSTEM.toString(), systemItem.text)
    assertFalse(systemItem.isSelected)
  }

  /**
   * Verifies that toggling a checkbox menu item invokes the onStreamToggled callback with the
   * corresponding stream and its updated selection state.
   */
  @Test
  fun testBuildStreamsMenuItemToggledInvokesCallback() {
    if (GraphicsEnvironment.isHeadless()) return

    val toggledEvents = mutableListOf<Pair<LogStream, Boolean>>()
    val menu = LogStreamsMenuBuilder.buildStreamsMenu(
      availableStreams = setOf(LogStream.MAIN),
      isStreamAllowed = { true },
      onStreamToggled = { stream, allowed -> toggledEvents.add(stream to allowed) }
    )

    assertNotNull(menu)
    val item = menu?.getItem(0) as JCheckBoxMenuItem
    assertTrue(item.isSelected)

    item.doClick()

    assertEquals(1, toggledEvents.size)
    assertEquals(LogStream.MAIN to false, toggledEvents[0])

    item.doClick()

    assertEquals(2, toggledEvents.size)
    assertEquals(LogStream.MAIN to true, toggledEvents[1])
  }
}
