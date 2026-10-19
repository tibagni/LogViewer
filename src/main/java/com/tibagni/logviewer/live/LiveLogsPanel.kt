package com.tibagni.logviewer.live

import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.log.LogCellRenderer
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogListTableModel
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.CommonIcons
import com.tibagni.logviewer.view.SearchableTable
import com.tibagni.logviewer.view.SideButtonsPanel
import com.tibagni.logviewer.view.SidePanelEntry
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

/**
 * Panel responsible for rendering the live log streams (all logs and filtered logs),
 * status toolbars, and the side action panel (auto-scroll, clear, terminal).
 *
 * @param presenter The [LiveLogPresenter] governing the live log stream state.
 * @param logRenderer The [LogCellRenderer] utilized by the log lists to paint entries.
 * @param onAutoScrollToggled Callback invoked when the user toggles the auto-scroll state.
 */
class LiveLogsPanel(
  private val presenter: LiveLogPresenter,
  private val logRenderer: LogCellRenderer,
  private val onAutoScrollToggled: (Boolean) -> Unit
) : JPanel(BorderLayout()) {

  companion object {
    internal const val ID_STOP_RESUME = "stop_resume"
    internal const val ID_CLEAR = "clear"
    internal const val ID_AUTOSCROLL = "autoscroll"
    internal const val ID_TERMINAL = "terminal"
    internal const val ID_CHANGE_DEVICE = "change_device"
  }

  internal val deviceLabel: JLabel
  internal val statusLabel: JLabel
  internal val statsLabel: JLabel
  internal val filteringProgressBar: JProgressBar

  internal val logListTableModel: LogListTableModel
  internal val logList: SearchableTable
  internal val filteredLogListTableModel: LogListTableModel
  internal val filteredLogList: SearchableTable

  internal val mainSplitPane: JSplitPane
  internal val sidePanel: SideButtonsPanel

  init {
    // Top status/info header
    val toolbar = JPanel(FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(8), UIScaleUtils.dip(4)))
    toolbar.border = BorderFactory.createEmptyBorder(
      UIScaleUtils.dip(2),
      UIScaleUtils.dip(8),
      UIScaleUtils.dip(2),
      UIScaleUtils.dip(8)
    )

    deviceLabel = JLabel(I18n.format(I18n.LIVE_LOGS_LABEL_DEVICE, "-"))
    statusLabel = JLabel(I18n.format(I18n.LIVE_LOGS_LABEL_STATUS, "-"))
    statsLabel = JLabel(I18n.format(I18n.LIVE_LOGS_STATUS_SUMMARY, 0, 0))

    toolbar.add(deviceLabel)
    toolbar.add(JSeparator(SwingConstants.VERTICAL).apply { preferredSize = Dimension(1, UIScaleUtils.dip(16)) })
    toolbar.add(statusLabel)
    toolbar.add(JSeparator(SwingConstants.VERTICAL).apply { preferredSize = Dimension(1, UIScaleUtils.dip(16)) })
    toolbar.add(statsLabel)

    val headerPanel = JPanel(BorderLayout())
    headerPanel.add(toolbar, BorderLayout.CENTER)

    filteringProgressBar = JProgressBar(0, 100).apply {
      preferredSize = Dimension(preferredSize.width, UIScaleUtils.dip(4))
      isStringPainted = false
      isVisible = false
    }
    headerPanel.add(filteringProgressBar, BorderLayout.SOUTH)

    add(headerPanel, BorderLayout.NORTH)

    // Center split pane with logs
    val logsSplitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
    logsSplitPane.dividerSize = UIScaleUtils.dip(5)
    logsSplitPane.resizeWeight = 0.5

    logListTableModel = LogListTableModel(I18n.get(I18n.LOGS_TAB_ALL))
    logList = SearchableTable(logListTableModel)
    logList.table.setDefaultRenderer(LogEntry::class.java, logRenderer)

    filteredLogListTableModel = LogListTableModel(I18n.get(I18n.LOGS_TAB_FILTERED))
    filteredLogList = SearchableTable(filteredLogListTableModel)
    filteredLogList.table.setDefaultRenderer(LogEntry::class.java, logRenderer)

    logsSplitPane.topComponent = logList
    logsSplitPane.bottomComponent = filteredLogList

    mainSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
      dividerSize = UIScaleUtils.dip(5)
      isOneTouchExpandable = true
      resizeWeight = 0.15
      rightComponent = logsSplitPane
    }

    add(mainSplitPane, BorderLayout.CENTER)

    // Right-side vertical action bar
    sidePanel = buildSidePanel()
    add(sidePanel, BorderLayout.EAST)
  }

  private fun buildSidePanel(): SideButtonsPanel {
    return SideButtonsPanel(
      isExpandable = true,
      entries = listOf(
        SidePanelEntry.Action(
          id = ID_STOP_RESUME,
          icon = CommonIcons.stopIcon(),
          text = I18n.get(I18n.LIVE_LOGS_STOP),
          isEnabled = false
        ) {
          if (presenter.isStreaming) {
            presenter.stopStreaming()
          } else {
            presenter.connectedDevice?.let { presenter.startStreaming(it) }
          }
        },
        SidePanelEntry.Action(
          id = ID_CLEAR,
          icon = CommonIcons.clearIcon(),
          text = I18n.get(I18n.COMMON_CLEAR)
        ) { presenter.clearLogs() },
        SidePanelEntry.Toggle(
          id = ID_AUTOSCROLL,
          icon = CommonIcons.autoScrollIcon(),
          text = I18n.get(I18n.LIVE_LOGS_AUTOSCROLL),
          initialActive = true
        ) { active ->
          onAutoScrollToggled(active)
        },
        SidePanelEntry.Separator,
        SidePanelEntry.Action(
          id = ID_TERMINAL,
          icon = CommonIcons.terminalIcon(),
          text = I18n.get(I18n.LIVE_LOGS_TERMINAL),
          isEnabled = false
        ) {
          presenter.connectedDevice?.let { presenter.openDeviceTerminal(it) }
        },
        SidePanelEntry.Action(
          id = ID_CHANGE_DEVICE,
          icon = CommonIcons.changeDeviceIcon(),
          text = I18n.get(I18n.LIVE_LOGS_CHANGE_DEVICE)
        ) { presenter.disconnectDevice() }
      )
    )
  }
}
