package com.tibagni.logviewer.log

import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.ui.FlatLineBorder
import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.CommonUtils
import com.tibagni.logviewer.util.LogFontManager
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.ButtonsPane
import com.tibagni.logviewer.view.CardBorderUtils
import com.tibagni.logviewer.view.CommonIcons
import com.tibagni.logviewer.view.FlatButton
import com.tibagni.logviewer.view.StatusBadge
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.RenderingHints
import java.awt.Window
import java.awt.event.KeyEvent
import java.io.File
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.KeyStroke
import javax.swing.UIManager
import javax.swing.border.Border
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder

/**
 * Modal dialog displaying detailed information for an individual log entry.
 *
 * @param owner the parent window owning this dialog
 * @param info metadata and parsed fields of the log line
 * @param onFilterByPid optional callback when filtering by PID is requested
 * @param onFilterByTag optional callback when filtering by Tag is requested
 * @param onOpenInEditor optional callback when opening the source file
 */
class LogLineDetailsDialog(
  owner: Window?,
  val info: LogLineInfo,
  private val onFilterByPid: ((Int) -> Unit)? = null,
  private val onFilterByTag: ((String) -> Unit)? = null,
  private val onOpenInEditor: ((File, Int) -> Unit)? = null
) : JDialog(
  owner,
  I18n.get(I18n.LOG_DETAILS_TITLE),
  ModalityType.APPLICATION_MODAL
), ButtonsPane.Listener {

  private val buttonsPane = ButtonsPane(
    ButtonsPane.ButtonsMode.OK_ONLY,
    this
  )

  @VisibleForTesting
  internal lateinit var sourceSection: JPanel
    private set

  @VisibleForTesting
  internal lateinit var metadataSection: JPanel
    private set

  @VisibleForTesting
  internal lateinit var messageSection: JPanel
    private set

  @VisibleForTesting
  internal lateinit var copyPathBtn: FlatButton
    private set

  @VisibleForTesting
  internal lateinit var openEditorBtn: FlatButton
    private set

  @VisibleForTesting
  internal lateinit var copyLineBtn: FlatButton
    private set

  @VisibleForTesting
  internal lateinit var copyMsgBtn: FlatButton
    private set

  @VisibleForTesting
  internal lateinit var streamBadge: StatusBadge
    private set

  @VisibleForTesting
  internal lateinit var indexBadge: StatusBadge
    private set

  @VisibleForTesting
  internal lateinit var messageMetricsBadge: StatusBadge
    private set

  @VisibleForTesting
  internal lateinit var messageTextArea: JTextArea
    private set

  @VisibleForTesting
  internal lateinit var messageScrollPane: JScrollPane
    private set

  @VisibleForTesting
  internal var levelIndicator: JPanel? = null
    private set

  @VisibleForTesting
  internal var filterColorSquare: JPanel? = null
    private set

  init {
    buildUi()
  }

  private fun buildUi() {
    val mainPanel = JPanel(BorderLayout(0, UIScaleUtils.dip(10))).apply {
      border = EmptyBorder(
        UIScaleUtils.dip(10),
        UIScaleUtils.dip(12),
        UIScaleUtils.dip(10),
        UIScaleUtils.dip(12)
      )
    }

    sourceSection = createSourceSection()
    metadataSection = createMetadataSection()
    messageSection = createMessageSection()

    val topPanel = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      add(sourceSection)
      add(Box.createVerticalStrut(UIScaleUtils.dip(8)))
      add(metadataSection)
    }

    mainPanel.add(topPanel, BorderLayout.NORTH)
    mainPanel.add(messageSection, BorderLayout.CENTER)
    mainPanel.add(buttonsPane, BorderLayout.SOUTH)

    contentPane = mainPanel
    buttonsPane.setDefaultButtonOk()

    // Close on ESC
    rootPane.registerKeyboardAction(
      { dispose() },
      KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
      JComponent.WHEN_IN_FOCUSED_WINDOW
    )

    isResizable = true
    preferredSize = Dimension(
      UIScaleUtils.dip(680),
      UIScaleUtils.dip(560)
    )
    minimumSize = Dimension(
      UIScaleUtils.dip(500),
      UIScaleUtils.dip(420)
    )
    pack()
    setLocationRelativeTo(owner)
  }

  private fun createSourceSection(): JPanel {
    val panel = JPanel(GridBagLayout()).apply {
      border = createSectionBorder(
        I18n.get(I18n.LOG_DETAILS_SOURCE_SECTION)
      )
    }

    val gbc = GridBagConstraints().apply {
      fill = GridBagConstraints.HORIZONTAL
      insets = Insets(
        UIScaleUtils.dip(2),
        UIScaleUtils.dip(4),
        UIScaleUtils.dip(2),
        UIScaleUtils.dip(4)
      )
    }

    // Row 0: File Name + Action buttons
    gbc.gridx = 0
    gbc.gridy = 0
    gbc.weightx = 0.0
    panel.add(createKeyLabel(I18n.get(I18n.LOG_DETAILS_FILE_NAME)), gbc)

    gbc.gridx = 1
    gbc.weightx = 1.0
    val fileNameLabel = JLabel(info.fileName).apply {
      font = font.deriveFont(Font.BOLD)
    }
    val fileNamePanel = JPanel(
      FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(4), 0)
    ).apply {
      isOpaque = false
      add(JLabel(CommonIcons.fileDocumentIcon(16)))
      add(fileNameLabel)
    }
    panel.add(fileNamePanel, gbc)

    val actionsPanel = JPanel(
      FlowLayout(FlowLayout.RIGHT, UIScaleUtils.dip(4), 0)
    ).apply {
      isOpaque = false
    }

    copyPathBtn = FlatButton(I18n.get(I18n.LOG_DETAILS_COPY_PATH)).apply {
      isEnabled = info.sourceFile != null
      addActionListener {
        info.sourceFile?.let { CommonUtils.copyToClipboard(it.absolutePath) }
      }
    }
    actionsPanel.add(copyPathBtn)

    openEditorBtn = FlatButton(
      I18n.get(I18n.LOG_DETAILS_OPEN_EDITOR)
    ).apply {
      icon = CommonIcons.fileDocumentIcon(14)
      iconTextGap = UIScaleUtils.dip(4)
      isEnabled = info.hasFile
      addActionListener {
        info.sourceFile?.let {
          onOpenInEditor?.invoke(it, info.lineNumber)
        }
      }
    }
    actionsPanel.add(openEditorBtn)

    gbc.gridx = 2
    gbc.weightx = 0.0
    panel.add(actionsPanel, gbc)

    // Row 1: File Path
    gbc.gridx = 0
    gbc.gridy = 1
    gbc.weightx = 0.0
    panel.add(createKeyLabel(I18n.get(I18n.LOG_DETAILS_FILE_PATH)), gbc)

    gbc.gridx = 1
    gbc.gridwidth = 2
    gbc.weightx = 1.0
    val pathField = JTextField(info.filePath).apply {
      isEditable = false
      border = null
      isOpaque = false
      font = Font(Font.MONOSPACED, Font.PLAIN, font.size)
    }
    panel.add(pathField, gbc)

    // Row 2: Line in File & Stream
    gbc.gridwidth = 1
    gbc.gridx = 0
    gbc.gridy = 2
    gbc.weightx = 0.0
    panel.add(createKeyLabel(I18n.get(I18n.LOG_DETAILS_LINE_NUMBER)), gbc)

    gbc.gridx = 1
    gbc.weightx = 0.5
    val lineLabel = JLabel(info.formattedLineNumber).apply {
      font = Font(Font.MONOSPACED, Font.PLAIN, font.size)
    }
    panel.add(lineLabel, gbc)

    gbc.gridx = 2
    gbc.weightx = 0.5
    streamBadge = StatusBadge(StatusBadge.Variant.NORMAL).apply {
      text = info.logStream.name
      isVisible = true
    }
    val streamBox = JPanel(
      FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(4), 0)
    ).apply {
      isOpaque = false
      add(createKeyLabel(I18n.get(I18n.LOG_DETAILS_STREAM)))
      add(streamBadge)
    }
    panel.add(streamBox, gbc)

    return panel
  }

  private fun createMetadataSection(): JPanel {
    val panel = JPanel(GridBagLayout()).apply {
      border = createSectionBorder(
        I18n.get(I18n.LOG_DETAILS_METADATA_SECTION)
      )
    }

    val gbc = GridBagConstraints().apply {
      fill = GridBagConstraints.HORIZONTAL
      insets = Insets(
        UIScaleUtils.dip(2),
        UIScaleUtils.dip(4),
        UIScaleUtils.dip(2),
        UIScaleUtils.dip(4)
      )
    }

    // Row 0: Timestamp & Log Index
    gbc.gridx = 0
    gbc.gridy = 0
    gbc.weightx = 0.0
    panel.add(createKeyLabel(I18n.get(I18n.LOG_DETAILS_TIMESTAMP)), gbc)

    gbc.gridx = 1
    gbc.weightx = 0.5
    val timestampLabel = JLabel(info.formattedTimestamp).apply {
      font = Font(Font.MONOSPACED, Font.PLAIN, font.size)
    }
    panel.add(timestampLabel, gbc)

    gbc.gridx = 2
    gbc.weightx = 0.0
    panel.add(createKeyLabel(I18n.get(I18n.LOG_DETAILS_LOG_INDEX)), gbc)

    gbc.gridx = 3
    gbc.weightx = 0.5
    indexBadge = StatusBadge(StatusBadge.Variant.NORMAL).apply {
      text = "#${info.formattedIndex}" // i18n:ignore
      isVisible = true
    }
    val indexPanel = JPanel(
      FlowLayout(FlowLayout.LEFT, 0, 0)
    ).apply {
      isOpaque = false
      add(indexBadge)
    }
    panel.add(indexPanel, gbc)

    // Row 1: Level & Filter
    gbc.gridx = 0
    gbc.gridy = 1
    gbc.weightx = 0.0
    panel.add(createKeyLabel(I18n.get(I18n.LOG_DETAILS_LEVEL)), gbc)

    gbc.gridx = 1
    gbc.weightx = 0.5
    val levelPanel = JPanel(
      FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(4), 0)
    ).apply {
      isOpaque = false
      val indicator = RoundedColorIndicator(
        LogCellRenderer.getColorForLogLevel(info.logLevel),
        UIScaleUtils.dip(4)
      )
      levelIndicator = indicator
      add(indicator)
      add(JLabel(info.logLevel.name).apply {
        font = font.deriveFont(Font.BOLD)
      })
    }
    panel.add(levelPanel, gbc)

    gbc.gridx = 2
    gbc.weightx = 0.0
    panel.add(createKeyLabel(I18n.get(I18n.LOG_DETAILS_FILTER)), gbc)

    gbc.gridx = 3
    gbc.weightx = 0.5
    val filterPanel = JPanel(
      FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(4), 0)
    ).apply {
      isOpaque = false
      if (info.appliedFilter != null) {
        val filterColor = RoundedColorIndicator(
          info.appliedFilter.color,
          UIScaleUtils.dip(4)
        )
        filterColorSquare = filterColor
        add(filterColor)
        add(JLabel(info.appliedFilter.name).apply {
          font = font.deriveFont(Font.BOLD)
        })
      } else {
        filterColorSquare = null
        add(JLabel("-"))
      }
    }
    panel.add(filterPanel, gbc)

    // Row 2: PID & TID
    gbc.gridx = 0
    gbc.gridy = 2
    gbc.weightx = 0.0
    panel.add(createKeyLabel(I18n.get(I18n.LOG_DETAILS_PID)), gbc)

    gbc.gridx = 1
    gbc.weightx = 0.5
    val pidPanel = JPanel(
      FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(4), 0)
    ).apply {
      isOpaque = false
      add(JLabel(info.formattedPid).apply {
        font = Font(Font.MONOSPACED, Font.PLAIN, font.size)
      })
      if (info.pid != null && info.pid > 0) {
        val filterPidBtn = FlatButton(
          I18n.get(I18n.LOG_DETAILS_BTN_FILTER_PID)
        ).apply {
          addActionListener {
            onFilterByPid?.invoke(info.pid)
            dispose()
          }
        }
        add(filterPidBtn)
      }
    }
    panel.add(pidPanel, gbc)

    gbc.gridx = 2
    gbc.weightx = 0.0
    panel.add(createKeyLabel(I18n.get(I18n.LOG_DETAILS_TID)), gbc)

    gbc.gridx = 3
    gbc.weightx = 0.5
    val tidLabel = JLabel(info.formattedTid).apply {
      font = Font(Font.MONOSPACED, Font.PLAIN, font.size)
    }
    panel.add(tidLabel, gbc)

    // Row 3: Tag
    gbc.gridx = 0
    gbc.gridy = 3
    gbc.weightx = 0.0
    panel.add(createKeyLabel(I18n.get(I18n.LOG_DETAILS_TAG)), gbc)

    gbc.gridx = 1
    gbc.gridwidth = 3
    gbc.weightx = 1.0
    val tagPanel = JPanel(
      FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(4), 0)
    ).apply {
      isOpaque = false
      add(JLabel(info.formattedTag).apply {
        font = font.deriveFont(Font.BOLD)
      })
      if (!info.tag.isNullOrBlank()) {
        val filterTagBtn = FlatButton(
          I18n.get(I18n.LOG_DETAILS_BTN_FILTER_TAG)
        ).apply {
          addActionListener {
            onFilterByTag?.invoke(info.tag)
            dispose()
          }
        }
        add(filterTagBtn)

        val copyTagBtn = FlatButton(
          I18n.get(I18n.LOG_DETAILS_COPY_TAG)
        ).apply {
          addActionListener {
            CommonUtils.copyToClipboard(info.tag)
          }
        }
        add(copyTagBtn)
      }
    }
    panel.add(tagPanel, gbc)

    return panel
  }

  private fun createMessageSection(): JPanel {
    val panel = JPanel(BorderLayout(0, UIScaleUtils.dip(4))).apply {
      border = createSectionBorder(
        I18n.get(I18n.LOG_DETAILS_MESSAGE_SECTION)
      )
    }

    val lineCount = info.message.count { it == '\n' } + 1
    val charCount = info.message.length
    val badgeText = if (lineCount > 1) {
      "$lineCount lines, $charCount chars" // i18n:ignore
    } else {
      "$charCount chars" // i18n:ignore
    }

    messageMetricsBadge = StatusBadge(StatusBadge.Variant.NORMAL).apply {
      text = badgeText
      isVisible = true
    }

    val actionsPanel = JPanel(
      FlowLayout(FlowLayout.RIGHT, UIScaleUtils.dip(4), 0)
    ).apply {
      isOpaque = false
    }

    copyLineBtn = FlatButton(
      I18n.get(I18n.LOG_DETAILS_COPY_LINE)
    ).apply {
      addActionListener {
        CommonUtils.copyToClipboard(info.entry.logText)
      }
    }
    actionsPanel.add(copyLineBtn)

    copyMsgBtn = FlatButton(
      I18n.get(I18n.LOG_DETAILS_COPY_MESSAGE)
    ).apply {
      addActionListener {
        CommonUtils.copyToClipboard(info.message)
      }
    }
    actionsPanel.add(copyMsgBtn)

    val toolbar = JPanel(BorderLayout()).apply {
      isOpaque = false
      add(messageMetricsBadge, BorderLayout.WEST)
      add(actionsPanel, BorderLayout.EAST)
    }

    panel.add(toolbar, BorderLayout.NORTH)

    messageTextArea = JTextArea(info.message).apply {
      isEditable = false
      lineWrap = true
      wrapStyleWord = true
      border = EmptyBorder(
        UIScaleUtils.dip(6),
        UIScaleUtils.dip(8),
        UIScaleUtils.dip(6),
        UIScaleUtils.dip(8)
      )
      font = LogFontManager.getFont(
        ServiceLocator.logViewerPrefs.logFontFamily,
        Font.PLAIN,
        ServiceLocator.logViewerPrefs.logFontSize
      )
      caretPosition = 0
    }

    messageScrollPane = JScrollPane(messageTextArea).apply {
      preferredSize = Dimension(
        UIScaleUtils.dip(640),
        UIScaleUtils.dip(160)
      )
      border = FlatLineBorder(
        Insets(1, 1, 1, 1),
        resolvePanelBorderColor(),
        1f,
        UIScaleUtils.dip(4)
      )
    }
    panel.add(messageScrollPane, BorderLayout.CENTER)

    return panel
  }

  /**
   * Resolves the border color for section cards.
   *
   * @return the resolved [Color] based on current Look and Feel
   */
  @VisibleForTesting
  internal fun resolvePanelBorderColor(): Color =
    Companion.resolvePanelBorderColor()

  /**
   * Creates a titled section card border with rounded [FlatLineBorder].
   *
   * @param title the section title string
   * @return a [Border] composed of [TitledBorder] and [EmptyBorder]
   */
  @VisibleForTesting
  internal fun createSectionBorder(title: String): Border =
    Companion.createSectionBorder(title)

  /**
   * Resolves the secondary foreground color for field key labels.
   *
   * @return the resolved [Color] based on current Look and Feel
   */
  @VisibleForTesting
  internal fun resolveKeyLabelColor(): Color =
    Companion.resolveKeyLabelColor()

  /**
   * Creates a secondary field key label with disabled foreground color.
   *
   * @param text the label text to display
   * @return configured [JLabel] with secondary foreground styling
   */
  @VisibleForTesting
  internal fun createKeyLabel(text: String): JLabel =
    Companion.createKeyLabel(text)

  override fun onOk() {
    dispose()
  }

  override fun onCancel() {
    dispose()
  }

  /**
   * Component that paints a rounded color pill with antialiased corners.
   *
   * @param color the fill color for the indicator
   * @param arc corner radius arc in pixels
   */
  @VisibleForTesting
  internal class RoundedColorIndicator(
    color: Color,
    val arc: Int = UIScaleUtils.dip(4)
  ) : JPanel() {
    init {
      isOpaque = false
      background = color
      preferredSize = Dimension(
        UIScaleUtils.dip(12),
        UIScaleUtils.dip(12)
      )
    }

    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      val g2d = g.create() as? Graphics2D ?: return
      try {
        g2d.setRenderingHint(
          RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON
        )
        g2d.color = background
        g2d.fillRoundRect(0, 0, width, height, arc, arc)
      } finally {
        g2d.dispose()
      }
    }
  }

  companion object {
    /**
     * Resolves the border color for section cards.
     *
     * @return the resolved [Color] based on current Look and Feel
     */
    @VisibleForTesting
    internal fun resolvePanelBorderColor(): Color =
      CardBorderUtils.resolveCardBorderColor()

    /**
     * Creates a titled section card border with rounded [FlatLineBorder].
     *
     * @param title the section title string
     * @return a [Border] composed of [TitledBorder] and [EmptyBorder]
     */
    @VisibleForTesting
    internal fun createSectionBorder(title: String): Border =
      CardBorderUtils.createTitledCardBorder(title)

    /**
     * Resolves the secondary foreground color for field key labels.
     *
     * @return the resolved [Color] based on current Look and Feel
     */
    @VisibleForTesting
    internal fun resolveKeyLabelColor(): Color =
      CardBorderUtils.resolveSecondaryLabelColor()

    /**
     * Creates a secondary field key label with disabled foreground color.
     *
     * @param text the label text to display
     * @return configured [JLabel] with secondary foreground styling
     */
    @VisibleForTesting
    internal fun createKeyLabel(text: String): JLabel {
      return JLabel(text).apply {
        foreground = resolveKeyLabelColor()
      }
    }
  }
}
