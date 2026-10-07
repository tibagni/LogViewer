package com.tibagni.logviewer.adb

import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

class AdbDevicesDialog(
  private val owner: Frame?,
  private val adbManager: AdbManager
) : JDialog(owner, I18n.get(I18n.ADB_DEVICES_DIALOG_TITLE), false) {

  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
  private val tableModel = DevicesTableModel()
  private val table = JTable(tableModel)

  private val refreshBtn = JButton(I18n.get(I18n.ADB_DEVICES_REFRESH))
  private val openTerminalBtn = JButton(I18n.get(I18n.ADB_DEVICES_OPEN_TERMINAL))
  private val restartServerBtn = JButton(I18n.get(I18n.ADB_DEVICES_RESTART_SERVER))
  private val closeBtn = JButton(I18n.get(I18n.COMMON_CLOSE))

  private val statusBanner = JLabel()

  init {
    buildUi()
    setupListeners()
    refreshDevices()
  }

  private fun buildUi() {
    val mainPanel = JPanel(BorderLayout(0, UIScaleUtils.dip(8)))
    mainPanel.border = EmptyBorder(UIScaleUtils.dip(10), UIScaleUtils.dip(12), UIScaleUtils.dip(10), UIScaleUtils.dip(12))

    // Top: Status Banner
    statusBanner.isVisible = false
    statusBanner.border = EmptyBorder(UIScaleUtils.dip(6), UIScaleUtils.dip(8), UIScaleUtils.dip(6), UIScaleUtils.dip(8))
    mainPanel.add(statusBanner, BorderLayout.NORTH)

    // Center: Devices Table
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    table.rowHeight = UIScaleUtils.dip(24)
    table.columnModel.getColumn(0).preferredWidth = UIScaleUtils.dip(100) // Status
    table.columnModel.getColumn(1).preferredWidth = UIScaleUtils.dip(200) // Device
    table.columnModel.getColumn(2).preferredWidth = UIScaleUtils.dip(180) // Serial
    table.columnModel.getColumn(3).preferredWidth = UIScaleUtils.dip(90)  // Transport ID
    table.columnModel.getColumn(0).cellRenderer = StatusCellRenderer()
    val scrollPane = JScrollPane(table)
    mainPanel.add(scrollPane, BorderLayout.CENTER)

    // Bottom: Button Actions
    val bottomPanel = JPanel(BorderLayout())
    val leftActions = JPanel(FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(4), 0))
    leftActions.add(restartServerBtn)
    val rightActions = JPanel(FlowLayout(FlowLayout.RIGHT, UIScaleUtils.dip(4), 0))
    rightActions.add(refreshBtn)
    rightActions.add(openTerminalBtn)
    rightActions.add(closeBtn)
    bottomPanel.add(leftActions, BorderLayout.WEST)
    bottomPanel.add(rightActions, BorderLayout.EAST)
    mainPanel.add(bottomPanel, BorderLayout.SOUTH)

    contentPane = mainPanel
    preferredSize = Dimension(UIScaleUtils.dip(700), UIScaleUtils.dip(380))
    minimumSize = Dimension(UIScaleUtils.dip(550), UIScaleUtils.dip(280))
    pack()
    setLocationRelativeTo(owner)
  }

  private fun setupListeners() {
    refreshBtn.addActionListener { refreshDevices() }

    openTerminalBtn.isEnabled = false
    openTerminalBtn.addActionListener { openTerminalForSelectedDevice() }

    restartServerBtn.addActionListener { restartServer() }
    closeBtn.addActionListener { dispose() }

    table.selectionModel.addListSelectionListener {
      val selectedDevice = getSelectedDevice()
      val isOnline = selectedDevice?.isOnline == true
      openTerminalBtn.isEnabled = isOnline
    }

    table.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (e.clickCount == 2 && SwingUtilities.isLeftMouseButton(e)) {
          openTerminalForSelectedDevice()
        }
      }
    })

    rootPane.registerKeyboardAction(
      { dispose() },
      KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
      JComponent.WHEN_IN_FOCUSED_WINDOW
    )
  }

  private fun getSelectedDevice(): AdbDevice? {
    val row = table.selectedRow
    return if (row >= 0 && row < tableModel.devices.size) {
      tableModel.devices[row]
    } else null
  }

  private fun openTerminalForSelectedDevice() {
    val device = getSelectedDevice() ?: return
    if (!device.isOnline) return

    val launched = adbManager.launchDeviceTerminal(device)
    if (!launched) {
      JOptionPane.showMessageDialog(
        this,
        I18n.get(I18n.ADB_SHELL_TERMINAL_FAILED),
        I18n.get(I18n.ADB_ERROR_TITLE),
        JOptionPane.WARNING_MESSAGE
      )
    }
  }

  private fun refreshDevices() {
    if (!adbManager.isAdbAvailable) {
      showStatusBanner(I18n.get(I18n.ADB_DEVICES_ADB_NOT_FOUND), isError = true)
      tableModel.setDevices(emptyList())
      return
    }

    setButtonsEnabled(false)
    scope.launch {
      val devices = adbManager.getConnectedDevices()
      tableModel.setDevices(devices)
      setButtonsEnabled(true)

      when {
        devices.isEmpty() -> {
          showStatusBanner(I18n.get(I18n.ADB_DEVICES_NO_DEVICES), isError = false)
        }
        devices.any { it.state == DeviceState.UNAUTHORIZED } -> {
          showStatusBanner(I18n.get(I18n.ADB_DEVICES_UNAUTHORIZED_WARNING), isError = false)
        }
        else -> {
          statusBanner.isVisible = false
        }
      }

      if (devices.isNotEmpty() && table.selectedRow == -1) {
        table.setRowSelectionInterval(0, 0)
      }
    }
  }

  private fun restartServer() {
    setButtonsEnabled(false)
    scope.launch {
      val success = adbManager.restartServer()
      setButtonsEnabled(true)
      if (success) {
        refreshDevices()
      } else {
        showStatusBanner(I18n.get(I18n.ADB_DEVICES_RESTART_FAILED), isError = true)
      }
    }
  }

  private fun setButtonsEnabled(enabled: Boolean) {
    refreshBtn.isEnabled = enabled
    restartServerBtn.isEnabled = enabled
    val selectedDevice = getSelectedDevice()
    openTerminalBtn.isEnabled = enabled && (selectedDevice?.isOnline == true)
  }

  private fun showStatusBanner(message: String, isError: Boolean) {
    statusBanner.text = message
    statusBanner.foreground = if (isError) Color(220, 50, 50) else Color(200, 150, 0)
    statusBanner.isVisible = true
  }

  override fun dispose() {
    scope.cancel()
    super.dispose()
  }

  private class DevicesTableModel : AbstractTableModel() {
    val devices = mutableListOf<AdbDevice>()

    private val columns = arrayOf(
      I18n.get(I18n.ADB_DEVICES_COL_STATUS),
      I18n.get(I18n.ADB_DEVICES_COL_NAME),
      I18n.get(I18n.ADB_DEVICES_COL_SERIAL),
      I18n.get(I18n.ADB_DEVICES_COL_TRANSPORT_ID)
    )

    fun setDevices(newDevices: List<AdbDevice>) {
      devices.clear()
      devices.addAll(newDevices)
      fireTableDataChanged()
    }

    override fun getRowCount(): Int = devices.size
    override fun getColumnCount(): Int = columns.size
    override fun getColumnName(column: Int): String = columns[column]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
      val device = devices[rowIndex]
      return when (columnIndex) {
        0 -> device.state
        1 -> (device.model ?: device.product ?: device.device ?: "-").replace('_', ' ')
        2 -> device.serialNumber
        3 -> device.transportId ?: "-"
        else -> ""
      }
    }
  }

  private class StatusCellRenderer : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
      table: JTable?,
      value: Any?,
      isSelected: Boolean,
      hasFocus: Boolean,
      row: Int,
      column: Int
    ): Component {
      val comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
      val state = value as? DeviceState ?: DeviceState.UNKNOWN

      if (!isSelected) {
        foreground = when (state) {
          DeviceState.ONLINE -> Color(46, 139, 87) // Sea Green
          DeviceState.UNAUTHORIZED -> Color(204, 120, 0) // Amber / Orange
          DeviceState.OFFLINE -> Color(178, 34, 34) // Firebrick Red
          else -> Color.GRAY
        }
      }
      text = when (state) {
        DeviceState.ONLINE -> "● Online"
        DeviceState.UNAUTHORIZED -> "● Unauthorized"
        DeviceState.OFFLINE -> "● Offline"
        DeviceState.BOOTLOADER -> "● Bootloader"
        DeviceState.RECOVERY -> "● Recovery"
        DeviceState.SIDELOAD -> "● Sideload"
        DeviceState.UNKNOWN -> "● Unknown"
      }
      return comp
    }
  }

  companion object {
    fun show(owner: Frame?, adbManager: AdbManager) {
      val dialog = AdbDevicesDialog(owner, adbManager)
      dialog.isVisible = true
    }
  }
}
