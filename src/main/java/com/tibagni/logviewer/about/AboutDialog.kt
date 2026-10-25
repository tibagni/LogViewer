package com.tibagni.logviewer.about

import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.ui.FlatLineBorder
import com.tibagni.logviewer.AppInfo
import com.tibagni.logviewer.AppInfo.currentVersion
import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.updates.ReleaseInfo
import com.tibagni.logviewer.updates.UpdateAvailableDialog
import com.tibagni.logviewer.updates.UpdateManager
import com.tibagni.logviewer.updates.UpdateManager.UpdateListener
import com.tibagni.logviewer.util.StringUtils
import com.tibagni.logviewer.util.layout.FontBuilder
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.ButtonsPane
import com.tibagni.logviewer.view.CardBorderUtils
import com.tibagni.logviewer.view.FlatButton
import com.tibagni.logviewer.view.StatusBadge
import java.awt.Color
import java.awt.Cursor
import java.awt.Desktop
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.IOException
import java.net.URISyntaxException
import java.net.URL
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.KeyStroke
import javax.swing.UIManager

/**
 * About dialog presenting application metadata, version information,
 * open source repository links, and update availability status styled with
 * Material 3 Expressive FlatLaf cards and [StatusBadge] indicators.
 *
 * @param owner the owning parent [JFrame], or `null` if unparented.
 * @param updateManagerFactory optional factory callback producing an
 *   [UpdateManager] given an [UpdateListener], primarily used for testing.
 */
class AboutDialog @VisibleForTesting constructor(
  owner: JFrame?,
  updateManagerFactory: ((UpdateListener) -> UpdateManager)? = null
) : JDialog(owner), ButtonsPane.Listener {

  /**
   * Constructs an [AboutDialog] using the standard [UpdateManager].
   *
   * @param owner the owning parent [JFrame], or `null`.
   */
  constructor(owner: JFrame?) : this(owner, null)

  private val buttonsPane = ButtonsPane(ButtonsPane.ButtonsMode.OK_ONLY, this)
  private val contentPane = JPanel()

  @VisibleForTesting
  internal val applicationName = JLabel()

  @VisibleForTesting
  internal val versionStatusBadge = StatusBadge(StatusBadge.Variant.NORMAL)

  @VisibleForTesting
  internal val updateStatusProgress = JProgressBar()

  @VisibleForTesting
  internal val openSourceInfo = JLabel()

  @VisibleForTesting
  internal val updateBtn = FlatButton()

  @VisibleForTesting
  internal val infoPane = JPanel()

  private val updateManager: UpdateManager

  companion object {
    /**
     * Displays the [AboutDialog] centered relative to the specified [parent].
     *
     * @param parent the parent frame for dialog positioning and ownership.
     */
    @JvmStatic
    fun showAboutDialog(parent: JFrame?) {
      val dialog = AboutDialog(parent)
      dialog.pack()
      dialog.setLocationRelativeTo(parent)
      dialog.isVisible = true
    }
  }

  init {
    title = I18n.get(I18n.ABOUT_DIALOG_TITLE)
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

    applicationName.text = I18n.format(
      I18n.ABOUT_VERSION_FORMAT,
      AppInfo.APPLICATION_NAME,
      currentVersion
    )

    openSourceInfo.text = StringUtils.wrapHtml(
      "${I18n.get(I18n.ABOUT_OPEN_SOURCE_INFO)} " +
        "<font color=\"#16a1d9\"><u>github</u></font>"
    )
    openSourceInfo.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    openSourceInfo.toolTipText = AppInfo.GITHUB_URL
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

    versionStatusBadge.variant = StatusBadge.Variant.NORMAL
    versionStatusBadge.text = I18n.get(I18n.ABOUT_CHECKING_UPDATES)
    versionStatusBadge.isVisible = true

    updateStatusProgress.isIndeterminate = true
    updateStatusProgress.isVisible = true
    updateStatusProgress.putClientProperty(
      FlatClientProperties.PROGRESS_BAR_LARGE_HEIGHT,
      true
    )
    updateStatusProgress.putClientProperty(
      FlatClientProperties.PROGRESS_BAR_SQUARE,
      false
    )

    updateBtn.isVisible = false
    updateBtn.text = I18n.get(I18n.ABOUT_UPDATE_BTN)
    updateBtn.putClientProperty(
      FlatClientProperties.BUTTON_TYPE,
      FlatClientProperties.BUTTON_TYPE_ROUND_RECT
    )
    updateBtn.putClientProperty(
      FlatClientProperties.STYLE_CLASS,
      "accent"
    )

    val listener = object : UpdateListener {
      override fun onUpdateFound(newRelease: ReleaseInfo) {
        versionStatusBadge.variant = StatusBadge.Variant.SURFACE
        versionStatusBadge.text = I18n.get(I18n.ABOUT_UPDATE_AVAILABLE)
        updateStatusProgress.isVisible = false
        updateBtn.isVisible = true
        updateBtn.text = I18n.format(
          I18n.ABOUT_UPDATE_TO,
          newRelease.versionName
        )
        updateBtn.addActionListener { onUpdate(newRelease) }
      }

      override fun onUpToDate() {
        versionStatusBadge.variant = StatusBadge.Variant.NORMAL
        versionStatusBadge.text = I18n.get(I18n.ABOUT_UP_TO_DATE)
        updateStatusProgress.isVisible = false
        updateBtn.isVisible = false
      }

      override fun onFailedToCheckForUpdate(tr: Throwable) {
        versionStatusBadge.variant = StatusBadge.Variant.NO_RESULTS
        versionStatusBadge.text = I18n.get(I18n.ABOUT_CHECK_UPDATE_FAILED)
        updateStatusProgress.isVisible = false
        updateBtn.isVisible = false
      }
    }

    updateManager = updateManagerFactory?.invoke(listener)
      ?: UpdateManager(listener)
    updateManager.checkForUpdates()
  }

  /**
   * Invoked when the OK button is pressed, dismissing the dialog.
   */
  override fun onOk() {
    dispose()
  }

  /**
   * Invoked when the dialog is dismissed via escape or the window cross.
   */
  override fun onCancel() {
    dispose()
  }

  /**
   * Dismisses this dialog and presents the update available dialog.
   *
   * @param newRelease the discovered release information.
   */
  private fun onUpdate(newRelease: ReleaseInfo) {
    dispose()
    UpdateAvailableDialog.showUpdateAvailableDialog(parent, newRelease)
  }

  /**
   * Builds the root dialog layout with padded card presentation.
   */
  private fun buildUi() {
    contentPane.layout = GridBagLayout()
    contentPane.border = BorderFactory.createEmptyBorder(
      UIScaleUtils.dip(12),
      UIScaleUtils.dip(12),
      UIScaleUtils.dip(12),
      UIScaleUtils.dip(12)
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
    val buttonConstraints = GBConstraintsBuilder()
      .withGridx(0)
      .withGridy(1)
      .withWeightx(1.0)
      .withFill(GridBagConstraints.HORIZONTAL)
      .build()
    buttonConstraints.insets = Insets(UIScaleUtils.dip(10), 0, 0, 0)
    contentPane.add(buttonsPane, buttonConstraints)
  }

  /**
   * Builds the central information pane presenting branding, status badge,
   * progress indicator, and splash illustration within an 8dp rounded card.
   *
   * @return the configured [JPanel] container.
   */
  private fun buildInfoPane(): JPanel {
    infoPane.layout = GridBagLayout()
    val pad = UIScaleUtils.dip(16)
    infoPane.border = CardBorderUtils.createCardBorder(
      insets = Insets(pad, pad, pad, pad)
    )

    applicationName.font = FontBuilder(applicationName)
      .withStyle(Font.BOLD)
      .withSize(UIScaleUtils.scaleFont(20))
      .build()
    val appNameConstraints = GBConstraintsBuilder()
      .withGridx(0)
      .withGridy(0)
      .withGridWidth(1)
      .withWeightx(1.0)
      .withFill(GridBagConstraints.HORIZONTAL)
      .build()
    appNameConstraints.insets = Insets(0, 0, UIScaleUtils.dip(6), 0)
    infoPane.add(applicationName, appNameConstraints)

    val openSourceConstraints = GBConstraintsBuilder()
      .withGridx(0)
      .withGridy(1)
      .withGridWidth(1)
      .withWeightx(1.0)
      .withFill(GridBagConstraints.HORIZONTAL)
      .build()
    openSourceConstraints.insets = Insets(0, 0, UIScaleUtils.dip(10), 0)
    infoPane.add(openSourceInfo, openSourceConstraints)

    val badgeConstraints = GBConstraintsBuilder()
      .withGridx(0)
      .withGridy(2)
      .withGridWidth(1)
      .withAnchor(GridBagConstraints.WEST)
      .withFill(GridBagConstraints.NONE)
      .build()
    badgeConstraints.insets = Insets(0, 0, UIScaleUtils.dip(10), 0)
    infoPane.add(versionStatusBadge, badgeConstraints)

    val updateBtnConstraints = GBConstraintsBuilder()
      .withGridx(0)
      .withGridy(3)
      .withWeightx(1.0)
      .withFill(GridBagConstraints.HORIZONTAL)
      .build()
    updateBtnConstraints.insets = Insets(0, 0, UIScaleUtils.dip(8), 0)
    infoPane.add(updateBtn, updateBtnConstraints)

    val progressConstraints = GBConstraintsBuilder()
      .withGridx(0)
      .withGridy(4)
      .withWeightx(1.0)
      .withFill(GridBagConstraints.HORIZONTAL)
      .build()
    progressConstraints.insets = Insets(0, 0, UIScaleUtils.dip(10), 0)
    infoPane.add(updateStatusProgress, progressConstraints)

    val aboutImage = JLabel()
    aboutImage.icon = ImageIcon(javaClass.getResource("/Images/splash.png"))
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

    return infoPane
  }

  /**
   * Resolves the border color for the info card boundary.
   *
   * @return the [Color] to paint the card boundary.
   */
  @VisibleForTesting
  internal fun resolveCardBorderColor(): Color =
    CardBorderUtils.resolveCardBorderColor()
}
