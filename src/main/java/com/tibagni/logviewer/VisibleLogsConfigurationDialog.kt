package com.tibagni.logviewer

import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.ButtonsPane
import java.awt.Color
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*

class VisibleLogsConfigurationDialog(owner: JFrame?, configuration: VisibleLogConfiguration) :
  JDialog(owner),
  ButtonsPane.Listener {
  private val buttonsPane = ButtonsPane(ButtonsPane.ButtonsMode.OK_CANCEL, this)
  private val contentPane = JPanel()

  private val startingPointLogText = JTextArea(3, 30)
  private val endingPointLogText = JTextArea(3, 30)
  private val clearStartingPointBtn = JButton("Clear")
  private val clearEndingPointBtn = JButton("Clear")

  private var startingLog: LogEntry? = configuration.startingLog
  private var endingLog: LogEntry? = configuration.endingLog
  private var returnConfiguration: VisibleLogConfiguration? = null

  companion object {
    fun showIgnoredLogsConfigurationDialog(
      parent: JFrame?,
      configuration: VisibleLogConfiguration
    ): VisibleLogConfiguration? {
      val dialog = VisibleLogsConfigurationDialog(parent, configuration)
      dialog.pack()
      dialog.setLocationRelativeTo(parent)
      dialog.isVisible = true
      return dialog.returnConfiguration
    }
  }

  init {
    title = "Visible Logs"
    buildUi()
    setContentPane(contentPane)
    isModal = true
    buttonsPane.setDefaultButtonOk()
    buttonsPane.setDefaultButtonCancel()

    // call onCancel() when cross is clicked
    defaultCloseOperation = DO_NOTHING_ON_CLOSE
    addWindowListener(object : WindowAdapter() {
      override fun windowClosing(e: WindowEvent) {
        onCancel()
      }
    })

    // call onCancel() on ESCAPE
    contentPane.registerKeyboardAction(
      { onCancel() },
      KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
      JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
    )

    clearStartingPointBtn.addActionListener {
      startingLog = null
      updateVisibleLogsState()
    }
    clearEndingPointBtn.addActionListener {
      endingLog = null
      updateVisibleLogsState()
    }

    updateVisibleLogsState()
  }

  private fun updateVisibleLogsState() {
    val localStartingLog = startingLog
    val localEndingLog = endingLog

    startingPointLogText.text = localStartingLog?.logText ?: "There is no \"starting point\" set for the visible logs"
    endingPointLogText.text = localEndingLog?.logText ?: "There is no \"ending point\" set for the visible logs"

    clearStartingPointBtn.isEnabled = localStartingLog != null
    clearEndingPointBtn.isEnabled = localEndingLog != null

    startingPointLogText.caretPosition = 0
    endingPointLogText.caretPosition = 0
  }

  private fun buildUi() {
    contentPane.layout = GridBagLayout()
    contentPane.border = BorderFactory.createEmptyBorder(
      UIScaleUtils.dip(10), UIScaleUtils.dip(10),
      UIScaleUtils.dip(10),
      UIScaleUtils.dip(10)
    )
    contentPane.add(
      buttonsPane,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(1)
        .withWeightx(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )

    val positionsPane = JPanel()
    positionsPane.layout = GridBagLayout()
    positionsPane.add(
      JLabel(
        "<html>Below you can check the starting and ending points of the \"visible\" logs.<br>" +
            "\"Visible logs\" are all the logs that are not ignored.<br><br>" +
            "You can ignore parts of the log you are not interested in and focus only on what matters<br></html>"
      ),
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(1)
        .withGridWidth(GridBagConstraints.REMAINDER)
        .withFill(GridBagConstraints.HORIZONTAL)
        .build()
    )
    positionsPane.add(
      JSeparator(),
      GBConstraintsBuilder()
        .withGridy(2)
        .withGridWidth(GridBagConstraints.REMAINDER)
        .withInsets(UIScaleUtils.scaleInsets(Insets(15, 5, 15, 5)))
        .withFill(GridBagConstraints.HORIZONTAL)
        .build()
    )

    positionsPane.add(
      JLabel("First visible Log (all lines before are ignored)"),
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(3)
        .withGridWidth(2)
        .withWeightx(1.0)
        .withFill(GridBagConstraints.HORIZONTAL)
        .build()
    )

    val borderColor = if (ServiceLocator.themeManager.isDark) Color.LIGHT_GRAY else Color.DARK_GRAY
    startingPointLogText.isEditable = false
    startingPointLogText.border = BorderFactory.createDashedBorder(borderColor)
    startingPointLogText.lineWrap = true
    positionsPane.add(
      JScrollPane(startingPointLogText),
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(4)
        .withGridWidth(1)
        .withWeightx(1.0)
        .withFill(GridBagConstraints.HORIZONTAL)
        .build()
    )

    positionsPane.add(
      clearStartingPointBtn,
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(4)
        .withInsets(UIScaleUtils.scaleInsets(Insets(0, 5, 0, 0)))
        .withFill(GridBagConstraints.NONE)
        .build()
    )

    positionsPane.add(
      JSeparator(),
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(5)
        .withGridWidth(GridBagConstraints.REMAINDER)
        .withInsets(UIScaleUtils.scaleInsets(Insets(15, 5, 15, 5)))
        .withFill(GridBagConstraints.HORIZONTAL)
        .build()
    )

    positionsPane.add(
      JLabel("Last visible Log (all lines after are ignored)"),
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(6)
        .withGridWidth(2)
        .withWeightx(1.0)
        .withFill(GridBagConstraints.HORIZONTAL)
        .build()
    )

    endingPointLogText.isEditable = false
    endingPointLogText.border = BorderFactory.createDashedBorder(borderColor)
    endingPointLogText.lineWrap = true
    positionsPane.add(
      JScrollPane(endingPointLogText),
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(7)
        .withGridWidth(1)
        .withWeightx(1.0)
        .withFill(GridBagConstraints.HORIZONTAL)
        .build()
    )

    positionsPane.add(
      clearEndingPointBtn,
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(7)
        .withInsets(UIScaleUtils.scaleInsets(Insets(0, 5, 0, 0)))
        .withFill(GridBagConstraints.NONE)
        .build()
    )

    contentPane.add(
      positionsPane,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(0)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )
  }

  override fun onOk() {
    returnConfiguration = VisibleLogConfiguration(startingLog, endingLog)
    dispose()
  }

  override fun onCancel() {
    returnConfiguration = null
    dispose()
  }
}

data class VisibleLogConfiguration(val startingLog: LogEntry?, val endingLog: LogEntry?)