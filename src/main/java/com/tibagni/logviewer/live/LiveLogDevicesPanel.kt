package com.tibagni.logviewer.live

import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.preferences.LogViewerPreferencesDialog
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.layout.WrapLayout
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.TriStateCheckbox
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
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

  internal val refreshDevicesBtn: JButton
  internal val restartServerBtn: JButton
  internal lateinit var buffersCard: JPanel
  internal lateinit var selectAllCb: TriStateCheckbox
  internal val bufferCheckboxes = mutableMapOf<String, JCheckBox>()
  internal val activeStartButtons = mutableListOf<JButton>()
  internal val deviceCardsContainer: JPanel
  internal val deviceItemsContainer: JPanel
  internal val deviceSubCardLayout = CardLayout()

  companion object {
    internal const val SUB_CARD_SCANNING = "scanning"
    internal const val SUB_CARD_ADB_NOT_FOUND = "adb_not_found"
    internal const val SUB_CARD_NO_DEVICES = "no_devices"
    internal const val SUB_CARD_DEVICE_LIST = "device_list"
  }

  internal fun createBuffersCard(): JPanel {
    val card = BuffersCardContainer(
      WrapLayout(FlowLayout.CENTER, UIScaleUtils.dip(8), UIScaleUtils.dip(4))
    )
    updateBuffersCardStyle(card)

    val label = JLabel(I18n.get(I18n.LIVE_LOGS_BUFFERS)).apply {
      font = font.deriveFont(Font.BOLD)
    }
    card.add(label)

    selectAllCb = TriStateCheckbox(I18n.get(I18n.COMMON_ALL)).apply {
      isOpaque = false
      toolTipText = I18n.get(I18n.COMMON_SELECT_UNSELECT_ALL)
      addSelectionChangedListener(object : TriStateCheckbox.SelectionChangedListener {
        override fun onSelectionChanged(newSelectionState: TriStateCheckbox.SelectionState) {
          val selectAll = newSelectionState == TriStateCheckbox.SelectionState.SELECTED
          bufferCheckboxes.values.forEach { it.isSelected = selectAll }
        }
      })
    }
    card.add(selectAllCb)

    card.add(JSeparator(SwingConstants.VERTICAL).apply {
      preferredSize = Dimension(1, UIScaleUtils.dip(16))
    })

    val defaultSelected = presenter.defaultBuffers.toSet()
    for (buf in presenter.availableBuffers) {
      val checkbox = JCheckBox(buf).apply {
        isOpaque = false
        isSelected = defaultSelected.contains(buf)
        addActionListener {
          evaluateSelectAllCbState()
        }
      }
      bufferCheckboxes[buf] = checkbox
      card.add(checkbox)
    }

    evaluateSelectAllCbState()
    return card
  }

  private fun evaluateSelectAllCbState() {
    val total = bufferCheckboxes.size
    val selectedCount = bufferCheckboxes.values.count { it.isSelected }
    selectAllCb.selectionState = when (selectedCount) {
      total -> TriStateCheckbox.SelectionState.SELECTED
      0 -> TriStateCheckbox.SelectionState.NOT_SELECTED
      else -> TriStateCheckbox.SelectionState.PARTIALLY_SELECTED
    }
    selectAllCb.updateUI()
  }

  internal fun onThemeChanged() {
    updateBuffersCardStyle()
  }

  internal fun updateBuffersCardStyle(card: JPanel = buffersCard) {
    val borderColor = UIManager.getColor("Component.borderColor")
      ?: UIManager.getColor("Separator.foreground")
      ?: Color(200, 200, 200)
    val cardBg = UIManager.getColor("Table.background") ?: UIManager.getColor("Panel.background")

    card.border = BorderFactory.createCompoundBorder(
      BorderFactory.createLineBorder(borderColor, 1),
      EmptyBorder(UIScaleUtils.dip(8), UIScaleUtils.dip(14), UIScaleUtils.dip(8), UIScaleUtils.dip(14))
    )
    card.background = cardBg
  }

  internal fun getSelectedBuffers(): List<String> {
    return presenter.availableBuffers.filter { bufferCheckboxes[it]?.isSelected == true }
  }

  internal fun updateStartButtonsState(hasSelection: Boolean) {
    for (btn in activeStartButtons) {
      btn.isEnabled = hasSelection
      btn.toolTipText = if (hasSelection) null else I18n.get(I18n.LIVE_LOGS_NO_BUFFERS_SELECTED_TOOLTIP)
    }
  }

  init {
    buffersCard = createBuffersCard()
    val toolbar = JPanel(GridBagLayout()).apply {
      border = BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground") ?: Color.LIGHT_GRAY),
        EmptyBorder(UIScaleUtils.dip(8), UIScaleUtils.dip(16), UIScaleUtils.dip(8), UIScaleUtils.dip(16))
      )
    }

    val titleLabel = JLabel(I18n.get(I18n.LIVE_LOGS_CONNECTED_DEVICES)).apply {
      font = font.deriveFont(Font.BOLD, UIScaleUtils.dip(14).toFloat())
    }

    refreshDevicesBtn = JButton(I18n.get(I18n.LIVE_LOGS_REFRESH)).apply {
      addActionListener { presenter.loadDevices() }
    }
    restartServerBtn = JButton(I18n.get(I18n.LIVE_LOGS_RESTART_SERVER)).apply {
      addActionListener { presenter.restartAdbServer() }
    }

    val buttonsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, UIScaleUtils.dip(8), 0)).apply {
      isOpaque = false
      add(refreshDevicesBtn)
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

    deviceCardsContainer.add(scanningPanel, LiveLogViewImpl.SUB_CARD_SCANNING)
    deviceCardsContainer.add(noDevicesPanel, LiveLogViewImpl.SUB_CARD_NO_DEVICES)
    deviceCardsContainer.add(adbNotFoundPanel, LiveLogViewImpl.SUB_CARD_ADB_NOT_FOUND)
    deviceCardsContainer.add(deviceListScroll, LiveLogViewImpl.SUB_CARD_DEVICE_LIST)

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
