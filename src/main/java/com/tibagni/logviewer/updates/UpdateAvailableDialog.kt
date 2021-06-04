package com.tibagni.logviewer.updates

import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.util.layout.FontBuilder
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.ButtonsPane
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.net.URL
import javax.swing.*

class UpdateAvailableDialog(private val latestInfo: ReleaseInfo) : JDialog(), ButtonsPane.Listener {
  private val buttonsPane = ButtonsPane(ButtonsPane.ButtonsMode.OK_CANCEL, this)
  private val contentPane = JPanel()
  private val releaseInfo = JTextArea()

  init {
    buildUi()
    setContentPane(contentPane)
    isModal = true
    buttonsPane.setDefaultButtonOk()

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
    releaseInfo.text = getLatestReleaseText(latestInfo)
  }

  private fun getLatestReleaseText(releaseInfo: ReleaseInfo): String {
    return """
New Version: ${releaseInfo.versionName}
--------------------------------------- 
Release Notes: 
${releaseInfo.releaseNotes}
    """
  }

  private fun onDownload(url: String) {
    try {
      Desktop.getDesktop().browse(URL(url).toURI())
    } catch (e: Exception) {
      Logger.error("Failed to download new Version from URL: $url", e)
      dispose()
    }
  }

  override fun onOk() {
    onDownload(latestInfo.releaseUrl)
  }

  override fun onCancel() {
    dispose()
  }

  private fun buildUi() {
    contentPane.layout = GridBagLayout()
    contentPane.minimumSize = Dimension(UIScaleUtils.dip(550), UIScaleUtils.dip(250))
    contentPane.preferredSize = Dimension(UIScaleUtils.dip(550), UIScaleUtils.dip(250))
    contentPane.border = BorderFactory.createEmptyBorder(
      UIScaleUtils.dip(10),
      UIScaleUtils.dip(10),
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
    contentPane.add(
      buildReleaseInfoPane(),
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(0)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )
  }

  private fun buildReleaseInfoPane(): JPanel {
    val releaseInfoPane = JPanel()
    releaseInfoPane.layout = GridBagLayout()
    releaseInfoPane.autoscrolls = false
    val title = JLabel()
    title.text = "There is a new version of LogViewer available for Download!"
    title.font = FontBuilder(title).withStyle(Font.BOLD).build()
    releaseInfoPane.add(
      title,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(0)
        .withWeightx(1.0)
        .withFill(GridBagConstraints.HORIZONTAL)
        .build()
    )
    releaseInfo.isEditable = false
    releaseInfoPane.add(
      JScrollPane(releaseInfo),
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(1)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )
    return releaseInfoPane
  }

  companion object {
    fun showUpdateAvailableDialog(relativeTo: Component?, latest: ReleaseInfo) {
      val dialog = UpdateAvailableDialog(latest)
      dialog.pack()
      dialog.setLocationRelativeTo(relativeTo)
      dialog.isVisible = true
    }
  }
}