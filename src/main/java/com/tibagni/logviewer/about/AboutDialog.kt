package com.tibagni.logviewer.about

import com.tibagni.logviewer.AppInfo
import com.tibagni.logviewer.AppInfo.currentVersion
import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.updates.ReleaseInfo
import com.tibagni.logviewer.updates.UpdateAvailableDialog
import com.tibagni.logviewer.updates.UpdateManager
import com.tibagni.logviewer.updates.UpdateManager.UpdateListener
import com.tibagni.logviewer.util.layout.FontBuilder
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.ButtonsPane
import java.awt.*
import java.awt.event.*
import java.io.IOException
import java.net.URISyntaxException
import java.net.URL
import javax.swing.*

class AboutDialog(owner: JFrame?) : JDialog(owner), ButtonsPane.Listener {
  private val buttonsPane = ButtonsPane(ButtonsPane.ButtonsMode.OK_ONLY, this)
  private val contentPane = JPanel()
  private val applicationName = JLabel()
  private val versionStatus = JLabel()
  private val updateStatusProgress = JProgressBar()
  private val openSourceInfo = JLabel()
  private val updateBtn = JButton()
  private val updateManager: UpdateManager

  companion object {
    fun showAboutDialog(parent: JFrame?) {
      val dialog = AboutDialog(parent)
      dialog.pack()
      dialog.setLocationRelativeTo(parent)
      dialog.isVisible = true
    }
  }

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
    applicationName.text = AppInfo.APPLICATION_NAME + " - Version: " + currentVersion
    openSourceInfo.text = "<html>Open Source Software available on " +
        "<font color=\"#000099\"><u>github</u></font></html>"
    versionStatus.text = "Checking for updates..."
    updateStatusProgress.isVisible = true
    updateBtn.isVisible = false
    openSourceInfo.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(event: MouseEvent) {
        try {
          Desktop.getDesktop().browse(URL(AppInfo.GITHUB_URL).toURI())
        } catch (e: IOException) {
          Logger.error("Failed to open github link", e)
        } catch (e: URISyntaxException) {
          Logger.error("Failed to open github link", e)
        }
      }
    })
    updateManager = UpdateManager(object : UpdateListener {
      override fun onUpdateFound(newRelease: ReleaseInfo) {
        versionStatus.text = "There is a new version of Log Viewer available!"
        updateStatusProgress.isVisible = false
        updateBtn.isVisible = true
        updateBtn.text = "Update to " + newRelease.versionName
        updateBtn.addActionListener { onUpdate(newRelease) }
      }

      override fun onUpToDate() {
        versionStatus.text = "Log Viewer is already up to date!"
        updateStatusProgress.isVisible = false
        updateBtn.isVisible = false
      }

      override fun onFailedToCheckForUpdate(tr: Throwable) {
        versionStatus.text = "Not possible to check for updates this time"
        updateStatusProgress.isVisible = false
        updateBtn.isVisible = false
      }
    })
    updateManager.checkForUpdates()
  }


  override fun onOk() {
    dispose()
  }

  override fun onCancel() {
    dispose()
  }

  private fun onUpdate(newRelease: ReleaseInfo) {
    dispose()
    UpdateAvailableDialog.showUpdateAvailableDialog(parent, newRelease)
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
    contentPane.add(
      buildInfoPane(),
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(0)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )
  }

  private fun buildInfoPane(): JPanel {
    val infoPane = JPanel()
    infoPane.layout = GridBagLayout()
    applicationName.font = FontBuilder(applicationName)
      .withStyle(Font.BOLD)
      .withSize(UIScaleUtils.scaleFont(20))
      .build()
    applicationName.text = ""
    infoPane.add(
      applicationName,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(0)
        .withGridWidth(3)
        .withWeightx(1.0)
        .withFill(GridBagConstraints.HORIZONTAL)
        .build()
    )
    openSourceInfo.text = "Open Source Software available on"
    infoPane.add(
      openSourceInfo,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(1)
        .withGridWidth(2)
        .withWeightx(1.0)
        .withFill(GridBagConstraints.HORIZONTAL)
        .build()
    )
    versionStatus.text = ""
    infoPane.add(
      versionStatus,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(2)
        .withGridWidth(2)
        .withWeightx(1.0)
        .withFill(GridBagConstraints.HORIZONTAL)
        .build()
    )
    updateStatusProgress.isIndeterminate = true
    infoPane.add(
      updateStatusProgress,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(4)
        .withWeightx(1.0)
        .withFill(GridBagConstraints.HORIZONTAL)
        .build()
    )
    val aboutImage = JLabel()
    aboutImage.icon = ImageIcon(javaClass.getResource("/Images/about.png"))
    aboutImage.text = ""
    infoPane.add(
      aboutImage,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(5)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.HORIZONTAL)
        .build()
    )
    updateBtn.text = "Update"
    val constraints = GBConstraintsBuilder()
      .withGridx(0)
      .withGridy(3)
      .withWeightx(1.0)
      .withFill(GridBagConstraints.HORIZONTAL)
      .build()
    constraints.insets = Insets(UIScaleUtils.dip(5), 0, UIScaleUtils.dip(5), 0)
    infoPane.add(updateBtn, constraints)
    return infoPane
  }
}