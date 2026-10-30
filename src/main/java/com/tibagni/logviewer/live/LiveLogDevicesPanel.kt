package com.tibagni.logviewer.live

import com.tibagni.logviewer.adb.AdbDevice
import com.tibagni.logviewer.adb.DeviceState
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.preferences.LogViewerPreferencesDialog
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.layout.WrapLayout
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.CardBorderUtils
import com.tibagni.logviewer.view.FlatButton
import com.tibagni.logviewer.view.ToolbarDivider
import com.tibagni.logviewer.view.TriStateCheckbox
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * Panel responsible for displaying ADB device connection controls, scanning status,
 * and the list of available devices for live streaming.
 *
 * @param presenter The [LiveLogPresenter] handling device discovery and connection workflows.
 */
class LiveLogDevicesPanel(
  private val presenter: LiveLogPresenter
) : JPanel(BorderLayout()) {

  internal val refreshDevicesBtn: FlatButton
  internal val restartServerBtn: FlatButton
  internal val toolbarDivider: ToolbarDivider
  internal val buffersCard: JPanel
  internal val selectAllCb: TriStateCheckbox
  private val _bufferCheckboxes = mutableMapOf<String, JCheckBox>()
  internal val bufferCheckboxes: Map<String, JCheckBox>
    get() = _bufferCheckboxes
  private val activeStartButtons = mutableListOf<JButton>()
  private val deviceCardsContainer: JPanel
  internal val deviceItemsContainer: JPanel
  private val deviceSubCardLayout = CardLayout()

  companion object {
    private const val SUB_CARD_SCANNING = "scanning"
    private const val SUB_CARD_ADB_NOT_FOUND = "adb_not_found"
    private const val SUB_CARD_NO_DEVICES = "no_devices"
    private const val SUB_CARD_DEVICE_LIST = "device_list"
  }

  /**
   * Switches the UI to display the device scanning in-progress state.
   */
  internal fun showScanning() {
    deviceSubCardLayout.show(deviceCardsContainer, SUB_CARD_SCANNING)
  }

  /**
   * Switches the UI to display the empty state indicating no ADB devices were discovered.
   */
  internal fun showNoDevices() {
    deviceSubCardLayout.show(deviceCardsContainer, SUB_CARD_NO_DEVICES)
  }

  /**
   * Switches the UI to display the empty state indicating ADB is not found or not configured.
   */
  internal fun showAdbNotFound() {
    deviceSubCardLayout.show(deviceCardsContainer, SUB_CARD_ADB_NOT_FOUND)
  }

  private var currentDevices: List<AdbDevice> = emptyList()

  /**
   * Populates the device item cards and displays the list of connected devices.
   *
   * @param devices List of discovered [AdbDevice] instances to render.
   */
  internal fun showDeviceList(devices: List<AdbDevice>) {
    currentDevices = devices
    populateDeviceList(devices)
    deviceSubCardLayout.show(deviceCardsContainer, SUB_CARD_DEVICE_LIST)
  }

  /**
   * Refreshes UI styling (borders, backgrounds) when the global look-and-feel theme changes.
   */
  internal fun onThemeChanged() {
    updateBuffersCardStyle()
    refreshDevicesBtn.updateUI()
    restartServerBtn.updateUI()
    toolbarDivider.updateUI()
    if (currentDevices.isNotEmpty()) {
      populateDeviceList(currentDevices)
    }
  }

  private fun updateBuffersCardStyle(card: JPanel = buffersCard) {
    val padV = UIScaleUtils.dip(8)
    val padH = UIScaleUtils.dip(14)
    val cardBg = UIManager.getColor("Table.background")
      ?: UIManager.getColor("Panel.background")

    card.border = BorderFactory.createCompoundBorder(
      CardBorderUtils.createCardBorder(insets = Insets(0, 0, 0, 0)),
      EmptyBorder(padV, padH, padV, padH)
    )
    card.background = cardBg
  }

  /**
   * Returns the list of currently selected log buffers based on checkbox states.
   */
  internal fun getSelectedBuffers(): List<String> {
    return presenter.availableBuffers.filter { bufferCheckboxes[it]?.isSelected == true }
  }

  private fun updateStartButtonsState(hasSelection: Boolean) {
    for (btn in activeStartButtons) {
      btn.isEnabled = hasSelection
      btn.toolTipText = if (hasSelection) null else I18n.get(I18n.LIVE_LOGS_NO_BUFFERS_SELECTED_TOOLTIP)
    }
  }

  /**
   * Reconstructs device cards in the container according to the provided device list.
   *
   * @param devices The updated list of [AdbDevice] instances to display.
   */
  internal fun populateDeviceList(devices: List<AdbDevice>) {
    deviceItemsContainer.removeAll()
    activeStartButtons.clear()

    val hoverBorderColor = UIManager.getColor("Component.focusedBorderColor")
      ?: UIManager.getColor("Tree.selectionBackground")
      ?: Color(0, 120, 215)
    val cardBg = UIManager.getColor("Table.background")
      ?: UIManager.getColor("Panel.background")

    val padV = UIScaleUtils.dip(10)
    val padH = UIScaleUtils.dip(16)
    val normalBorder = BorderFactory.createCompoundBorder(
      CardBorderUtils.createCardBorder(insets = Insets(0, 0, 0, 0)),
      EmptyBorder(padV, padH, padV, padH)
    )
    val hoverBorder = BorderFactory.createCompoundBorder(
      CardBorderUtils.createCardBorder(insets = Insets(0, 0, 0, 0), lineColor = hoverBorderColor),
      EmptyBorder(padV, padH, padV, padH)
    )

    for (device in devices) {
      val itemPanel = JPanel(BorderLayout()).apply {
        border = normalBorder
        background = cardBg
        isOpaque = true
        alignmentX = Component.CENTER_ALIGNMENT
        val itemHeight = if (device.state == DeviceState.UNAUTHORIZED) 80 else 64
        maximumSize = Dimension(Int.MAX_VALUE, UIScaleUtils.dip(itemHeight))
      }

      val infoPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
      }

      val topRow = JPanel(FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(8), 0)).apply {
        isOpaque = false
      }

      val (statusText, statusColor) = when (device.state) {
        DeviceState.ONLINE -> "● Online" to Color(46, 139, 87)
        DeviceState.UNAUTHORIZED -> "● Unauthorized" to Color(204, 120, 0)
        DeviceState.OFFLINE -> "● Offline" to Color(178, 34, 34)
        else -> "● ${device.state.name.lowercase()}" to Color.GRAY
      }
      val statusBadge = JLabel(statusText).apply {
        foreground = statusColor
        font = font.deriveFont(Font.BOLD)
      }
      val readableName = (device.model ?: device.product ?: device.device ?: "Android Device").replace('_', ' ')
      val nameLabel = JLabel(readableName).apply {
        font = font.deriveFont(Font.BOLD, UIScaleUtils.dip(13).toFloat())
      }
      val serialLabel = JLabel("(${device.serialNumber})").apply { // i18n:ignore
        foreground = UIManager.getColor("Label.disabledForeground") ?: Color.GRAY
      }

      topRow.add(statusBadge)
      topRow.add(nameLabel)
      topRow.add(serialLabel)
      infoPanel.add(topRow)

      if (device.state == DeviceState.UNAUTHORIZED) {
        val warningRow = JPanel(FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(8), 0)).apply {
          isOpaque = false
        }
        val warningLabel = JLabel(I18n.get(I18n.LIVE_LOGS_DEVICE_UNAUTHORIZED)).apply {
          foreground = Color(204, 120, 0)
          font = font.deriveFont(Font.ITALIC)
        }
        warningRow.add(warningLabel)
        infoPanel.add(warningRow)
      }

      val actionsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, UIScaleUtils.dip(8), 0)).apply {
        isOpaque = false
      }
      if (device.isOnline) {
        val hasSelection = getSelectedBuffers().isNotEmpty()
        val startBtn = JButton(I18n.get(I18n.LIVE_LOGS_START)).apply {
          cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
          isEnabled = hasSelection
          toolTipText = if (hasSelection) null else I18n.get(I18n.LIVE_LOGS_NO_BUFFERS_SELECTED_TOOLTIP)
          addActionListener {
            val selected = getSelectedBuffers()
            if (selected.isNotEmpty()) {
              presenter.startStreaming(device, selected)
            }
          }
        }
        activeStartButtons.add(startBtn)

        val termBtn = JButton(I18n.get(I18n.LIVE_LOGS_TERMINAL)).apply {
          cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
          addActionListener { presenter.openDeviceTerminal(device) }
        }

        // TODO hardcoded string
        val bugreportBtn = JButton("Bugreport").apply {
          cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
          addActionListener { presenter.pullDeviceBugreport(device) }
        }
        actionsPanel.add(startBtn)
        actionsPanel.add(termBtn)
        actionsPanel.add(bugreportBtn)

        itemPanel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        itemPanel.addMouseListener(object : MouseAdapter() {
          override fun mouseEntered(e: MouseEvent) {
            itemPanel.border = hoverBorder
            itemPanel.repaint()
          }

          override fun mouseExited(e: MouseEvent) {
            if (e.x !in 0 until itemPanel.width || e.y !in 0 until itemPanel.height) {
              itemPanel.border = normalBorder
              itemPanel.repaint()
            }
          }

          override fun mouseClicked(e: MouseEvent) {
            if (e.clickCount == 2 && SwingUtilities.isLeftMouseButton(e)) {
              val selected = getSelectedBuffers()
              if (selected.isNotEmpty()) {
                presenter.startStreaming(device, selected)
              }
            }
          }
        })
      }

      itemPanel.add(infoPanel, BorderLayout.WEST)
      itemPanel.add(actionsPanel, BorderLayout.EAST)
      deviceItemsContainer.add(itemPanel)
      if (device != devices.last()) {
        deviceItemsContainer.add(Box.createVerticalStrut(UIScaleUtils.dip(8)))
      }
    }

    deviceItemsContainer.revalidate()
    deviceItemsContainer.repaint()
  }

  private fun createBuffersCard(): Pair<JPanel, TriStateCheckbox> {
    val card = BuffersCardContainer(
      WrapLayout(FlowLayout.CENTER, UIScaleUtils.dip(8), UIScaleUtils.dip(4))
    )
    updateBuffersCardStyle(card)

    val label = JLabel(I18n.get(I18n.LIVE_LOGS_BUFFERS)).apply {
      font = font.deriveFont(Font.BOLD)
    }
    card.add(label)

    val allCb = TriStateCheckbox(I18n.get(I18n.COMMON_ALL)).apply {
      isOpaque = false
      toolTipText = I18n.get(I18n.COMMON_SELECT_UNSELECT_ALL)
      addSelectionChangedListener(object : TriStateCheckbox.SelectionChangedListener {
        override fun onSelectionChanged(newSelectionState: TriStateCheckbox.SelectionState) {
          val selectAll = newSelectionState == TriStateCheckbox.SelectionState.SELECTED
          bufferCheckboxes.values.forEach { it.isSelected = selectAll }
          updateStartButtonsState(selectAll)
        }
      })
    }
    card.add(allCb)

    card.add(JSeparator(SwingConstants.VERTICAL).apply {
      preferredSize = Dimension(1, UIScaleUtils.dip(16))
    })

    val defaultSelected = presenter.defaultBuffers.toSet()
    for (buf in presenter.availableBuffers) {
      val checkbox = JCheckBox(buf).apply {
        isOpaque = false
        isSelected = defaultSelected.contains(buf)
        addActionListener {
          val total = bufferCheckboxes.size
          val selectedCount = bufferCheckboxes.values.count { it.isSelected }
          allCb.selectionState = when (selectedCount) {
            total -> TriStateCheckbox.SelectionState.SELECTED
            0 -> TriStateCheckbox.SelectionState.NOT_SELECTED
            else -> TriStateCheckbox.SelectionState.PARTIALLY_SELECTED
          }
          updateStartButtonsState(getSelectedBuffers().isNotEmpty())
        }
      }
      _bufferCheckboxes[buf] = checkbox
      card.add(checkbox)
    }

    val total = bufferCheckboxes.size
    val selectedCount = bufferCheckboxes.values.count { it.isSelected }
    allCb.selectionState = when (selectedCount) {
      total -> TriStateCheckbox.SelectionState.SELECTED
      0 -> TriStateCheckbox.SelectionState.NOT_SELECTED
      else -> TriStateCheckbox.SelectionState.PARTIALLY_SELECTED
    }

    return card to allCb
  }

  init {
    val (card, allCb) = createBuffersCard()
    buffersCard = card
    selectAllCb = allCb
    val toolbar = JPanel(GridBagLayout()).apply {
      val borderCol = CardBorderUtils.resolveCardBorderColor()
      val padV = UIScaleUtils.dip(8)
      val padH = UIScaleUtils.dip(16)
      border = BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, UIScaleUtils.dip(1), 0, borderCol),
        EmptyBorder(padV, padH, padV, padH)
      )
    }

    val titleLabel = JLabel(I18n.get(I18n.LIVE_LOGS_CONNECTED_DEVICES)).apply {
      font = font.deriveFont(Font.BOLD, UIScaleUtils.dip(14).toFloat())
    }

    refreshDevicesBtn = FlatButton(I18n.get(I18n.LIVE_LOGS_REFRESH)).apply {
      addActionListener { presenter.loadDevices() }
    }
    restartServerBtn = FlatButton(I18n.get(I18n.LIVE_LOGS_RESTART_SERVER)).apply {
      addActionListener { presenter.restartAdbServer() }
    }
    toolbarDivider = ToolbarDivider()

    val buttonsPanel = JPanel(
      FlowLayout(FlowLayout.RIGHT, UIScaleUtils.dip(4), 0)
    ).apply {
      isOpaque = false
      add(refreshDevicesBtn)
      add(toolbarDivider)
      add(restartServerBtn)
    }

    toolbar.add(
      titleLabel,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(0)
        .withWeightx(1.0)
        .withAnchor(GridBagConstraints.WEST)
        .build()
    )
    toolbar.add(
      buttonsPanel,
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(0)
        .withWeightx(0.0)
        .withAnchor(GridBagConstraints.EAST)
        .build()
    )

    add(toolbar, BorderLayout.NORTH)

    deviceCardsContainer = JPanel(deviceSubCardLayout)

    val scanningPanel = createEmptyStatePanel(
      title = I18n.get(I18n.LIVE_LOGS_SCANNING),
      message = ""
    )
    val noDevicesPanel = createEmptyStatePanel(
      title = I18n.get(I18n.LIVE_LOGS_NO_DEVICES_TITLE),
      message = I18n.get(I18n.LIVE_LOGS_NO_DEVICES_MSG),
      buttonText = I18n.get(I18n.LIVE_LOGS_REFRESH),
      onButtonClick = { presenter.loadDevices() }
    )
    val adbNotFoundPanel = createEmptyStatePanel(
      title = I18n.get(I18n.LIVE_LOGS_ADB_NOT_FOUND_TITLE),
      message = I18n.get(I18n.LIVE_LOGS_ADB_NOT_FOUND_MSG),
      buttonText = I18n.get(I18n.LIVE_LOGS_CONFIGURE_ADB),
      onButtonClick = {
        val frame = SwingUtilities.getWindowAncestor(this@LiveLogDevicesPanel) as? JFrame
        LogViewerPreferencesDialog.showPreferencesDialog(frame)
        presenter.loadDevices()
      }
    )

    deviceItemsContainer = DeviceListContainer()

    val centeringPanel = CenteredScrollablePanel().apply {
      val buffersGbc = GridBagConstraints().apply {
        gridx = 0
        gridy = 0
        weightx = 1.0
        weighty = 0.0
        anchor = GridBagConstraints.NORTH
        fill = GridBagConstraints.NONE
        insets = Insets(
          UIScaleUtils.dip(20),
          UIScaleUtils.dip(16),
          UIScaleUtils.dip(12),
          UIScaleUtils.dip(16)
        )
      }
      add(buffersCard, buffersGbc)

      val devicesGbc = GridBagConstraints().apply {
        gridx = 0
        gridy = 1
        weightx = 1.0
        weighty = 0.0
        anchor = GridBagConstraints.NORTH
        fill = GridBagConstraints.NONE
        insets = Insets(
          0,
          UIScaleUtils.dip(16),
          UIScaleUtils.dip(20),
          UIScaleUtils.dip(16)
        )
      }
      add(deviceItemsContainer, devicesGbc)

      val filler = JPanel().apply { isOpaque = false }
      val fillerGbc = GridBagConstraints().apply {
        gridx = 0
        gridy = 2
        weightx = 1.0
        weighty = 1.0
        fill = GridBagConstraints.BOTH
      }
      add(filler, fillerGbc)

      addComponentListener(object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent) {
          buffersCard.revalidate()
          deviceItemsContainer.revalidate()
        }
      })
    }

    val deviceListScroll = JScrollPane(centeringPanel).apply {
      border = BorderFactory.createEmptyBorder()
      verticalScrollBar.unitIncrement = UIScaleUtils.dip(16)
    }

    deviceCardsContainer.add(scanningPanel, SUB_CARD_SCANNING)
    deviceCardsContainer.add(noDevicesPanel, SUB_CARD_NO_DEVICES)
    deviceCardsContainer.add(adbNotFoundPanel, SUB_CARD_ADB_NOT_FOUND)
    deviceCardsContainer.add(deviceListScroll, SUB_CARD_DEVICE_LIST)

    add(deviceCardsContainer, BorderLayout.CENTER)
  }

  private fun createEmptyStatePanel(
    title: String,
    message: String,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null
  ): JPanel {
    val panel = JPanel(GridBagLayout())
    val content = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      isOpaque = false
    }

    val titleLabel = JLabel(title).apply {
      font = font.deriveFont(Font.BOLD, UIScaleUtils.dip(15).toFloat())
      alignmentX = Component.CENTER_ALIGNMENT
    }
    content.add(titleLabel)

    if (message.isNotEmpty()) {
      content.add(Box.createVerticalStrut(UIScaleUtils.dip(8)))
      val msgLabel = JLabel("<html><center style='width: 380px;'>$message</center></html>").apply { // i18n:ignore
        foreground = UIManager.getColor("Label.disabledForeground") ?: Color.GRAY
        alignmentX = Component.CENTER_ALIGNMENT
      }
      content.add(msgLabel)
    }

    if (buttonText != null && onButtonClick != null) {
      content.add(Box.createVerticalStrut(UIScaleUtils.dip(16)))
      val btn = JButton(buttonText).apply {
        alignmentX = Component.CENTER_ALIGNMENT
        addActionListener { onButtonClick() }
      }
      content.add(btn)
    }

    panel.add(content)
    return panel
  }
}

internal class BuffersCardContainer(layout: WrapLayout) : JPanel(layout) {
  init {
    isOpaque = true
    alignmentX = Component.CENTER_ALIGNMENT
  }

  override fun getPreferredSize(): Dimension {
    val maxAllowed = UIScaleUtils.dip(DeviceListContainer.MAX_WIDTH)
    val parentWidth = parent?.width ?: 0
    val available = if (parentWidth > 0) parentWidth - UIScaleUtils.dip(32) else maxAllowed
    val targetWidth = minOf(maxAllowed, maxOf(UIScaleUtils.dip(320), available))
    val base = super.getPreferredSize()
    return Dimension(targetWidth, base.height)
  }

  override fun getMinimumSize(): Dimension {
    val pref = preferredSize
    return Dimension(minOf(pref.width, UIScaleUtils.dip(320)), pref.height)
  }

  override fun getMaximumSize(): Dimension {
    val pref = preferredSize
    return Dimension(pref.width, pref.height)
  }
}

internal class CenteredScrollablePanel : JPanel(GridBagLayout()), Scrollable {
  init {
    isOpaque = false
  }

  override fun getScrollableTracksViewportWidth(): Boolean = true

  override fun getScrollableTracksViewportHeight(): Boolean {
    val parent = parent
    return if (parent is JViewport) {
      parent.height > preferredSize.height
    } else {
      false
    }
  }

  override fun getPreferredScrollableViewportSize(): Dimension = preferredSize
  override fun getScrollableBlockIncrement(visibleRect: Rectangle, orientation: Int, direction: Int): Int = UIScaleUtils.dip(64)
  override fun getScrollableUnitIncrement(visibleRect: Rectangle, orientation: Int, direction: Int): Int = UIScaleUtils.dip(16)
}

internal class DeviceListContainer : JPanel() {
  init {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    isOpaque = false
  }

  companion object {
    const val MAX_WIDTH = 700
  }

  override fun getPreferredSize(): Dimension {
    val base = super.getPreferredSize()
    val maxAllowed = UIScaleUtils.dip(MAX_WIDTH)
    val parentWidth = parent?.width ?: 0
    val available = if (parentWidth > 0) parentWidth - UIScaleUtils.dip(32) else maxAllowed
    val targetWidth = minOf(maxAllowed, maxOf(UIScaleUtils.dip(320), available))
    return Dimension(targetWidth, base.height)
  }

  override fun getMinimumSize(): Dimension {
    val pref = preferredSize
    return Dimension(minOf(pref.width, UIScaleUtils.dip(320)), pref.height)
  }

  override fun getMaximumSize(): Dimension {
    val pref = preferredSize
    return Dimension(pref.width, Int.MAX_VALUE)
  }
}
