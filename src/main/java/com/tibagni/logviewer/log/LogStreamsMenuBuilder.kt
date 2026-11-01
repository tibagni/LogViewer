package com.tibagni.logviewer.log

import com.tibagni.logviewer.i18n.I18n
import javax.swing.JCheckBoxMenuItem
import javax.swing.JMenu

/**
 * Helper object responsible for building dynamic log stream selection menus.
 */
object LogStreamsMenuBuilder {
  /**
   * Builds the dynamic streams menu containing checkbox menu items for each available
   * log stream, or returns null if no streams are available.
   *
   * @param availableStreams Set of available log streams discovered in loaded logs.
   * @param isStreamAllowed Callback to determine whether a given stream is currently enabled.
   * @param onStreamToggled Callback invoked when a stream checkbox menu item state changes.
   * @return Configured [JMenu] containing stream checkbox items, or null if empty.
   */
  fun buildStreamsMenu(
    availableStreams: Set<LogStream>,
    isStreamAllowed: (LogStream) -> Boolean,
    onStreamToggled: (LogStream, Boolean) -> Unit
  ): JMenu? {
    if (availableStreams.isEmpty()) return null

    val streamsMenu = JMenu(I18n.get(I18n.MENU_STREAMS))
    for (stream in availableStreams) {
      val item = JCheckBoxMenuItem(stream.toString())
      item.state = isStreamAllowed(stream)
      item.addItemListener { onStreamToggled(stream, item.isSelected) }
      streamsMenu.add(item)
    }

    return streamsMenu
  }
}
