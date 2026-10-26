package com.tibagni.logviewer

import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.ui.FlatLineBorder
import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.ButtonsPane
import com.tibagni.logviewer.view.CardBorderUtils
import com.tibagni.logviewer.view.FlatButton
import com.tibagni.logviewer.view.StatusBadge
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.KeyStroke
import javax.swing.UIManager
import javax.swing.border.EmptyBorder

/**
 * Dialog for reviewing and adjusting the starting and ending points of
 * visible logs. Styled with Material 3 Expressive FlatLaf rounded cards,
 * [StatusBadge] state indicators, and borderless [FlatButton] actions.
 *
 * @param owner the parent frame owning this modal dialog.
 * @param configuration the initial visible log boundary configuration.
 */
class VisibleLogsConfigurationDialog(
  owner: JFrame?,
  configuration: VisibleLogConfiguration
) : JDialog(owner), ButtonsPane.Listener {

  private val buttonsPane = ButtonsPane(
    ButtonsPane.ButtonsMode.OK_CANCEL,
    this
  )
  private val contentPane = JPanel()

  @VisibleForTesting
  internal val instructionLabel = JLabel(
    I18n.get(I18n.VISIBLE_LOGS_INSTRUCTION)
  )

  @VisibleForTesting
  internal val startingBadge = StatusBadge()

  @VisibleForTesting
  internal val endingBadge = StatusBadge()

  @VisibleForTesting
  internal val clearStartingPointBtn = FlatButton(
    I18n.get(I18n.COMMON_CLEAR)
  )

  @VisibleForTesting
  internal val clearEndingPointBtn = FlatButton(
    I18n.get(I18n.COMMON_CLEAR)
  )

  @VisibleForTesting
  internal val startingPointLogText = JTextArea(3, 30)

  @VisibleForTesting
  internal val endingPointLogText = JTextArea(3, 30)

  @VisibleForTesting
  internal lateinit var startingCard: JPanel

  @VisibleForTesting
  internal lateinit var endingCard: JPanel

  @VisibleForTesting
  internal lateinit var startingScrollPane: JScrollPane

  @VisibleForTesting
  internal lateinit var endingScrollPane: JScrollPane

  @VisibleForTesting
  internal var startingLog: LogEntry? = configuration.startingLog

  @VisibleForTesting
  internal var endingLog: LogEntry? = configuration.endingLog

  @VisibleForTesting
  internal var returnConfiguration: VisibleLogConfiguration? = null

  companion object {
    /**
     * Displays the visible logs configuration dialog relative to the
     * provided parent frame and returns the chosen configuration or null
     * if cancelled.
     *
     * @param parent the hosting frame.
     * @param configuration the initial boundary log entries.
     * @return updated [VisibleLogConfiguration] on confirmation, or null.
     */
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
    title = I18n.get(I18n.VISIBLE_LOGS_TITLE)
    buildUi()
    setContentPane(contentPane)
    isModal = true
    buttonsPane.setDefaultButtonOk()
    buttonsPane.setDefaultButtonCancel()

    defaultCloseOperation = DO_NOTHING_ON_CLOSE
    addWindowListener(object : WindowAdapter() {
      override fun windowClosing(e: WindowEvent) {
        onCancel()
      }
    })

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

  /**
   * Updates visual badges, text areas, and clear button states based on
   * current boundary logs.
   */
  @VisibleForTesting
  internal fun updateVisibleLogsState() {
    val localStartingLog = startingLog
    val localEndingLog = endingLog

    if (localStartingLog != null) {
      startingPointLogText.text = localStartingLog.logText
      startingPointLogText.foreground = resolveDefaultTextForeground()
      startingBadge.variant = StatusBadge.Variant.NORMAL
      startingBadge.text = I18n.get(I18n.VISIBLE_LOGS_STATUS_ACTIVE)
      clearStartingPointBtn.isEnabled = true
    } else {
      startingPointLogText.text = I18n.get(
        I18n.VISIBLE_LOGS_NO_STARTING_POINT
      )
      startingPointLogText.foreground = resolveDisabledForeground()
      startingBadge.variant = StatusBadge.Variant.NO_RESULTS
      startingBadge.text = I18n.get(I18n.VISIBLE_LOGS_STATUS_NOT_SET)
      clearStartingPointBtn.isEnabled = false
    }
    startingBadge.isVisible = true

    if (localEndingLog != null) {
      endingPointLogText.text = localEndingLog.logText
      endingPointLogText.foreground = resolveDefaultTextForeground()
      endingBadge.variant = StatusBadge.Variant.NORMAL
      endingBadge.text = I18n.get(I18n.VISIBLE_LOGS_STATUS_ACTIVE)
      clearEndingPointBtn.isEnabled = true
    } else {
      endingPointLogText.text = I18n.get(
        I18n.VISIBLE_LOGS_NO_ENDING_POINT
      )
      endingPointLogText.foreground = resolveDisabledForeground()
      endingBadge.variant = StatusBadge.Variant.NO_RESULTS
      endingBadge.text = I18n.get(I18n.VISIBLE_LOGS_STATUS_NOT_SET)
      clearEndingPointBtn.isEnabled = false
    }
    endingBadge.isVisible = true

    startingPointLogText.caretPosition = 0
    endingPointLogText.caretPosition = 0
  }

  /**
   * Resolves the border color for cards and inner text scroll panes.
   *
   * @return the theme border color or dark/light fallback.
   */
  @VisibleForTesting
  internal fun resolveBorderColor(): Color =
    CardBorderUtils.resolveCardBorderColor()

  /**
   * Resolves the disabled text foreground for unset placeholders.
   *
   * @return the disabled label foreground color.
   */
  @VisibleForTesting
  internal fun resolveDisabledForeground(): Color =
    CardBorderUtils.resolveSecondaryLabelColor()

  /**
   * Resolves the primary foreground for active log text contents.
   *
   * @return the text area foreground or label foreground color.
   */
  @VisibleForTesting
  internal fun resolveDefaultTextForeground(): Color {
    return UIManager.getColor("TextArea.foreground")
      ?: UIManager.getColor("Label.foreground")
      ?: Color.DARK_GRAY
  }

  /**
   * Builds the card container for either starting or ending log section.
   *
   * @param titleText header section title string.
   * @param badge [StatusBadge] indicator component for the section.
   * @param clearBtn [FlatButton] clear action for the section.
   * @param textArea [JTextArea] log display area.
   * @return pair containing the card [JPanel] and its [JScrollPane].
   */
  private fun createSectionCard(
    titleText: String,
    badge: StatusBadge,
    clearBtn: FlatButton,
    textArea: JTextArea
  ): Pair<JPanel, JScrollPane> {
    val card = JPanel(BorderLayout(0, UIScaleUtils.dip(8))).apply {
      border = CardBorderUtils.createCardBorder(
        insets = UIScaleUtils.scaleInsets(Insets(10, 10, 10, 10))
      )
    }

    val headerPanel = JPanel(GridBagLayout()).apply {
      isOpaque = false
      val titleLabel = JLabel(titleText).apply {
        font = font.deriveFont(Font.BOLD)
      }
      add(
        titleLabel,
        GBConstraintsBuilder()
          .withGridx(0).withGridy(0)
          .withAnchor(GridBagConstraints.WEST)
          .build()
      )
      add(
        badge,
        GBConstraintsBuilder()
          .withGridx(1).withGridy(0)
          .withAnchor(GridBagConstraints.WEST)
          .withInsets(UIScaleUtils.scaleInsets(Insets(0, 8, 0, 0)))
          .build()
      )
      val spacer = Box.createHorizontalGlue()
      add(
        spacer,
        GBConstraintsBuilder()
          .withGridx(2).withGridy(0)
          .withWeightx(1.0)
          .withFill(GridBagConstraints.HORIZONTAL)
          .build()
      )
      add(
        clearBtn,
        GBConstraintsBuilder()
          .withGridx(3).withGridy(0)
          .withAnchor(GridBagConstraints.EAST)
          .build()
      )
    }

    clearBtn.toolTipText = I18n.get(I18n.COMMON_CLEAR)

    textArea.apply {
      font = Font(Font.MONOSPACED, Font.PLAIN, font.size)
      isEditable = false
      lineWrap = true
      wrapStyleWord = true
      border = EmptyBorder(
        UIScaleUtils.dip(6),
        UIScaleUtils.dip(8),
        UIScaleUtils.dip(6),
        UIScaleUtils.dip(8)
      )
    }

    val scrollPane = JScrollPane(textArea).apply {
      border = CardBorderUtils.createCardBorder(
        arc = 4,
        insets = UIScaleUtils.scaleInsets(Insets(1, 1, 1, 1))
      )
    }

    card.add(headerPanel, BorderLayout.NORTH)
    card.add(scrollPane, BorderLayout.CENTER)

    return Pair(card, scrollPane)
  }

  /**
   * Assembles dialog views, section cards, and action buttons pane.
   */
  private fun buildUi() {
    contentPane.layout = GridBagLayout()
    contentPane.border = BorderFactory.createEmptyBorder(
      UIScaleUtils.dip(12),
      UIScaleUtils.dip(12),
      UIScaleUtils.dip(12),
      UIScaleUtils.dip(12)
    )

    instructionLabel.foreground = resolveDisabledForeground()

    val (startCardPanel, startScroll) = createSectionCard(
      I18n.get(I18n.VISIBLE_LOGS_FIRST_LABEL),
      startingBadge,
      clearStartingPointBtn,
      startingPointLogText
    )
    startingCard = startCardPanel
    startingScrollPane = startScroll

    val (endCardPanel, endScroll) = createSectionCard(
      I18n.get(I18n.VISIBLE_LOGS_LAST_LABEL),
      endingBadge,
      clearEndingPointBtn,
      endingPointLogText
    )
    endingCard = endCardPanel
    endingScrollPane = endScroll

    val positionsPane = JPanel(GridBagLayout()).apply {
      add(
        instructionLabel,
        GBConstraintsBuilder()
          .withGridx(0).withGridy(0)
          .withWeightx(1.0)
          .withFill(GridBagConstraints.HORIZONTAL)
          .withInsets(UIScaleUtils.scaleInsets(Insets(0, 0, 10, 0)))
          .build()
      )
      add(
        startingCard,
        GBConstraintsBuilder()
          .withGridx(0).withGridy(1)
          .withWeightx(1.0)
          .withWeighty(0.5)
          .withFill(GridBagConstraints.BOTH)
          .withInsets(UIScaleUtils.scaleInsets(Insets(0, 0, 10, 0)))
          .build()
      )
      add(
        endingCard,
        GBConstraintsBuilder()
          .withGridx(0).withGridy(2)
          .withWeightx(1.0)
          .withWeighty(0.5)
          .withFill(GridBagConstraints.BOTH)
          .build()
      )
    }

    contentPane.add(
      positionsPane,
      GBConstraintsBuilder()
        .withGridx(0).withGridy(0)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )

    contentPane.add(
      buttonsPane,
      GBConstraintsBuilder()
        .withGridx(0).withGridy(1)
        .withWeightx(1.0)
        .withFill(GridBagConstraints.HORIZONTAL)
        .withInsets(UIScaleUtils.scaleInsets(Insets(10, 0, 0, 0)))
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

/**
 * Encapsulates the visible boundary log entries selected by the user.
 *
 * @property startingLog the first visible log entry, or null if unset.
 * @property endingLog the last visible log entry, or null if unset.
 */
data class VisibleLogConfiguration(
  val startingLog: LogEntry?,
  val endingLog: LogEntry?
)
