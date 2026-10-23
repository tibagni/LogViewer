package com.tibagni.logviewer.live

import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.log.LogCellRenderer
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogListTableModel
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.CommonIcons
import com.tibagni.logviewer.view.SearchableTable
import com.tibagni.logviewer.view.SideButtonsPanel
import com.tibagni.logviewer.view.SidePanelEntry
import com.tibagni.logviewer.view.StatusBadge
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.*

/**
 * Panel responsible for rendering the live log streams (all and filtered),
 * status toolbars, and the side action panel (auto-scroll, clear, terminal).
 *
 * @param presenter The [LiveLogPresenter] governing the live log stream.
 * @param logRenderer The [LogCellRenderer] utilized to paint log entries.
 * @param onAutoScrollToggled Callback invoked when auto-scroll toggles.
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
  internal val statusBadge: StatusBadge = StatusBadge()
  internal val filteringProgressBar: JProgressBar
  internal val headerPanel: JPanel

  internal val logListTableModel: LogListTableModel
  internal val logList: SearchableTable
  internal val filteredLogListTableModel: LogListTableModel
  internal val filteredLogList: SearchableTable

  internal val mainSplitPane: JSplitPane
  internal val sidePanel: SideButtonsPanel

  init {
    // Top status/info header
    val toolbar = JPanel(
      FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(8), UIScaleUtils.dip(4))
    ).apply {
      isOpaque = false
      border = BorderFactory.createEmptyBorder(
        UIScaleUtils.dip(2),
        UIScaleUtils.dip(4),
        UIScaleUtils.dip(2),
        UIScaleUtils.dip(4)
      )
    }

    deviceLabel = JLabel("-").apply {
      icon = CommonIcons.smartphoneIcon()
      iconTextGap = UIScaleUtils.dip(6)
    }

    statusLabel = JLabel("-").apply {
      icon = CommonIcons.statusIndicatorIcon(false)
      iconTextGap = UIScaleUtils.dip(6)
    }

    statsLabel = JLabel(I18n.format(I18n.LIVE_LOGS_STATUS_SUMMARY, 0, 0))

    toolbar.add(deviceLabel)
    toolbar.add(createVerticalDivider())
    toolbar.add(statusLabel)

    val topBar = JPanel(BorderLayout()).apply { isOpaque = false }
    topBar.add(toolbar, BorderLayout.CENTER)

    val badgePanel = JPanel(
      FlowLayout(FlowLayout.RIGHT, UIScaleUtils.dip(8), UIScaleUtils.dip(4))
    ).apply {
      isOpaque = false
      add(statusBadge)
    }
    topBar.add(badgePanel, BorderLayout.EAST)

    headerPanel = object : JPanel(BorderLayout()) {
      override fun paintComponent(g: Graphics) {
        val g2 = g.create() as? Graphics2D ?: return
        try {
          if (width > 0 && height > 0) {
            g2.setRenderingHint(
              RenderingHints.KEY_ANTIALIASING,
              RenderingHints.VALUE_ANTIALIAS_ON
            )
            val bg = UIManager.getColor("Panel.background")
              ?: background
              ?: Color(240, 240, 240)
            val isDark = isDarkTheme(bg)
            val tint = if (isDark) {
              Color(255, 255, 255, 8)
            } else {
              Color(0, 0, 0, 10)
            }
            g2.color = bg
            g2.fillRect(0, 0, width, height)
            g2.color = tint
            g2.fillRect(0, 0, width, height)
          }
        } finally {
          g2.dispose()
        }
        super.paintComponent(g)
      }
    }
    headerPanel.isOpaque = false
    headerPanel.border = createHeaderBorder()
    headerPanel.add(topBar, BorderLayout.CENTER)

    filteringProgressBar = JProgressBar(0, 100).apply {
      preferredSize = Dimension(preferredSize.width, UIScaleUtils.dip(4))
      isStringPainted = false
      isVisible = false
    }
    headerPanel.add(filteringProgressBar, BorderLayout.SOUTH)

    add(headerPanel, BorderLayout.NORTH)

    // Center split pane with logs
    val logsSplitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
    logsSplitPane.dividerSize = UIScaleUtils.dip(4)
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
      dividerSize = UIScaleUtils.dip(4)
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

  /**
   * Creates a theme-aware vertical hairline divider separating toolbar sections.
   *
   * @return [JComponent] representing the vertical divider separator.
   */
  internal fun createVerticalDivider(): JComponent {
    return object : JComponent() {
      init {
        val width = UIScaleUtils.dip(1).coerceAtLeast(1)
        val height = UIScaleUtils.dip(14)
        preferredSize = Dimension(width, height)
        minimumSize = Dimension(width, height)
        maximumSize = Dimension(width, height)
        isOpaque = false
      }

      override fun paintComponent(g: Graphics) {
        val g2 = g.create() as? Graphics2D ?: return
        try {
          val dividerColor = UIManager.getColor("Separator.foreground")
            ?: UIManager.getColor("Component.borderColor")
            ?: Color(128, 128, 128, 60)
          g2.color = dividerColor
          g2.fillRect(0, 0, width, height)
        } finally {
          g2.dispose()
        }
      }
    }
  }

  /**
   * Creates a bottom matte border matching theme component borders.
   *
   * @return [Border] for the header panel bottom edge.
   */
  internal fun createHeaderBorder(): javax.swing.border.Border {
    return BorderFactory.createMatteBorder(
      0,
      0,
      UIScaleUtils.dip(1),
      0,
      UIManager.getColor("Component.borderColor")
        ?: Color(128, 128, 128, 60)
    )
  }

  /**
   * Determines whether the active theme is a dark theme, using the theme
   * manager or falling back to relative luminance of [fallbackBg].
   */
  private fun isDarkTheme(fallbackBg: Color): Boolean {
    return try {
      ServiceLocator.themeManager.isDark
    } catch (_: Throwable) {
      val lum = (0.299 * fallbackBg.red +
        0.587 * fallbackBg.green +
        0.114 * fallbackBg.blue) / 255.0
      lum < 0.5
    }
  }

  /**
   * Refreshes dynamic theme colors, icons, borders, and repaints
   * the live log header panel when Look and Feel changes.
   *
   * @param isStreaming whether streaming is currently active
   */
  fun onThemeChanged(isStreaming: Boolean) {
    deviceLabel.icon = CommonIcons.smartphoneIcon()
    statusLabel.icon = CommonIcons.statusIndicatorIcon(isStreaming)
    headerPanel.border = createHeaderBorder()
    headerPanel.repaint()
  }
}
