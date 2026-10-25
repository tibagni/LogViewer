package com.tibagni.logviewer.updates

import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.ui.FlatLineBorder
import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.util.layout.FontBuilder
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.ButtonsPane
import com.tibagni.logviewer.view.CardBorderUtils
import com.tibagni.logviewer.view.StatusBadge
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.net.URL
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.KeyStroke
import javax.swing.UIManager
import javax.swing.border.Border
import javax.swing.border.EmptyBorder

/**
 * Dialog displaying information about newly available application updates,
 * presenting version badges, release notes in an expressive card layout,
 * and a direct action to download the update.
 */
class UpdateAvailableDialog @VisibleForTesting constructor(
  private val latestInfo: ReleaseInfo,
  private val urlLauncher: ((String) -> Unit)?
) : JDialog(), ButtonsPane.Listener {

  /**
   * Primary constructor for production use opening the release URL in the
   * system browser.
   *
   * @param latestInfo the remote release information to display
   */
  constructor(latestInfo: ReleaseInfo) : this(latestInfo, null)

  /**
   * Button panel containing Download Update and Cancel actions.
   */
  @VisibleForTesting
  internal val buttonsPane = ButtonsPane(
    ButtonsPane.ButtonsMode.OK_CANCEL,
    this
  )

  private val contentPane = JPanel()

  /**
   * Non-editable text area presenting the release notes markdown/text.
   */
  @VisibleForTesting
  internal val releaseInfo = JTextArea()

  /**
   * Elevated surface badge component indicating the latest version tag.
   */
  @VisibleForTesting
  internal val versionBadge = StatusBadge(
    StatusBadge.Variant.SURFACE
  )

  /**
   * Panel housing the header and release notes card layout.
   */
  @VisibleForTesting
  internal lateinit var releaseInfoPane: JPanel

  init {
    title = I18n.get(I18n.UPDATES_TITLE)
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
    populateReleaseData()
  }

  /**
   * Populates the version badge and release notes text with release data.
   */
  private fun populateReleaseData() {
    versionBadge.text = latestInfo.versionName
    versionBadge.isVisible = true
    releaseInfo.text = latestInfo.releaseNotes
    releaseInfo.caretPosition = 0
  }

  /**
   * Initiates download via the injected URL launcher or Desktop browse.
   *
   * @param url the destination URL to open
   */
  private fun onDownload(url: String) {
    try {
      if (urlLauncher != null) {
        urlLauncher.invoke(url)
      } else {
        Desktop.getDesktop().browse(URL(url).toURI())
      }
    } catch (e: Exception) {
      Logger.error("Failed to download new Version from URL: $url", e)
    } finally {
      dispose()
    }
  }

  /**
   * Callback invoked when the user confirms the download action.
   */
  override fun onOk() {
    onDownload(latestInfo.releaseUrl)
  }

  /**
   * Callback invoked when the dialog is dismissed or canceled.
   */
  override fun onCancel() {
    dispose()
  }

  /**
   * Resolves the border color for card borders from Look and Feel defaults.
   *
   * @return the resolved [Color] for card border outlines
   */
  @VisibleForTesting
  internal fun resolveCardBorderColor(): Color =
    CardBorderUtils.resolveCardBorderColor()

  /**
   * Creates an 8dp rounded card border with theme-adaptive stroke color.
   *
   * @return a [Border] instance configured with FlatLineBorder
   */
  @VisibleForTesting
  internal fun createCardBorder(): Border {
    val pad = UIScaleUtils.dip(CARD_BORDER_INSET_DP)
    return CardBorderUtils.createCardBorder(
      arc = CARD_CORNER_ARC_DP,
      insets = Insets(pad, pad, pad, pad)
    )
  }

  /**
   * Builds the dialog layout and main view hierarchy.
   */
  private fun buildUi() {
    contentPane.layout = GridBagLayout()
    contentPane.minimumSize = Dimension(
      UIScaleUtils.dip(550),
      UIScaleUtils.dip(280)
    )
    contentPane.preferredSize = Dimension(
      UIScaleUtils.dip(550),
      UIScaleUtils.dip(320)
    )
    contentPane.border = BorderFactory.createEmptyBorder(
      UIScaleUtils.dip(12),
      UIScaleUtils.dip(14),
      UIScaleUtils.dip(12),
      UIScaleUtils.dip(14)
    )

    configureActionButtons()

    releaseInfoPane = buildReleaseInfoPane()

    contentPane.add(
      releaseInfoPane,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(0)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )

    contentPane.add(
      buttonsPane,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(1)
        .withWeightx(1.0)
        .withFill(GridBagConstraints.HORIZONTAL)
        .build()
    )
  }

  /**
   * Configures the action button text and FlatLaf styling attributes.
   */
  private fun configureActionButtons() {
    buttonsPane.setOkText(I18n.get(I18n.UPDATES_BTN_DOWNLOAD))
    buttonsPane.buttonOk?.let { btn ->
      btn.putClientProperty(
        FlatClientProperties.BUTTON_TYPE,
        FlatClientProperties.BUTTON_TYPE_ROUND_RECT
      )
      btn.putClientProperty(
        "JButton.borderColor",
        UIManager.getColor("Component.accentColor")
      )
    }
  }

  /**
   * Constructs the top header panel with title and version badge.
   *
   * @return the populated header [JPanel]
   */
  private fun buildHeaderPanel(): JPanel {
    val headerPanel = JPanel(BorderLayout(UIScaleUtils.dip(8), 0))
    headerPanel.isOpaque = false

    val titleLabel = JLabel(I18n.get(I18n.UPDATES_NEW_VERSION_AVAILABLE))
    titleLabel.font = FontBuilder(titleLabel)
      .withStyle(Font.BOLD)
      .build()

    headerPanel.add(titleLabel, BorderLayout.WEST)
    headerPanel.add(versionBadge, BorderLayout.EAST)
    return headerPanel
  }

  /**
   * Constructs the release notes card panel wrapped in a rounded border.
   *
   * @return the styled card [JPanel]
   */
  private fun buildReleaseNotesCard(): JPanel {
    val cardPanel = JPanel(BorderLayout(0, UIScaleUtils.dip(6)))
    cardPanel.border = createCardBorder()

    val sectionLabel = JLabel(I18n.get(I18n.UPDATES_RELEASE_NOTES_LABEL))
    val disabledFg = UIManager.getColor("Label.disabledForeground")
    if (disabledFg != null) {
      sectionLabel.foreground = disabledFg
    }
    sectionLabel.font = FontBuilder(sectionLabel)
      .withStyle(Font.BOLD)
      .build()
    sectionLabel.border = EmptyBorder(
      UIScaleUtils.dip(6),
      UIScaleUtils.dip(8),
      0,
      UIScaleUtils.dip(8)
    )

    releaseInfo.isEditable = false
    releaseInfo.lineWrap = true
    releaseInfo.wrapStyleWord = true
    releaseInfo.border = EmptyBorder(
      UIScaleUtils.dip(8),
      UIScaleUtils.dip(10),
      UIScaleUtils.dip(8),
      UIScaleUtils.dip(10)
    )

    val scrollPane = JScrollPane(releaseInfo)
    scrollPane.border = FlatLineBorder(
      Insets(0, 0, 0, 0),
      resolveCardBorderColor(),
      1f,
      UIScaleUtils.dip(SCROLL_CORNER_ARC_DP)
    )

    cardPanel.add(sectionLabel, BorderLayout.NORTH)
    cardPanel.add(scrollPane, BorderLayout.CENTER)
    return cardPanel
  }

  /**
   * Constructs the inner vertical content pane container.
   *
   * @return the release info [JPanel]
   */
  private fun buildReleaseInfoPane(): JPanel {
    val pane = JPanel()
    pane.layout = BoxLayout(pane, BoxLayout.Y_AXIS)
    pane.autoscrolls = false

    val headerPanel = buildHeaderPanel()
    pane.add(headerPanel)
    pane.add(Box.createVerticalStrut(UIScaleUtils.dip(10)))

    val card = buildReleaseNotesCard()
    pane.add(card)
    return pane
  }

  companion object {
    private const val CARD_BORDER_INSET_DP = 6
    private const val CARD_CORNER_ARC_DP = 8
    private const val SCROLL_CORNER_ARC_DP = 4

    /**
     * Factory method displaying the [UpdateAvailableDialog] relative to
     * a specified parent component.
     *
     * @param relativeTo the parent component to center the dialog on
     * @param latest the [ReleaseInfo] describing the update
     */
    fun showUpdateAvailableDialog(
      relativeTo: Component?,
      latest: ReleaseInfo
    ) {
      val dialog = UpdateAvailableDialog(latest)
      dialog.pack()
      dialog.setLocationRelativeTo(relativeTo)
      dialog.isVisible = true
    }
  }
}
