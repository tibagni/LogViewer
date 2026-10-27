package com.tibagni.logviewer.view

import com.formdev.flatlaf.FlatClientProperties
import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.Insets
import javax.swing.Icon
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JTextArea
import javax.swing.UIManager
import javax.swing.border.EmptyBorder

/**
 * Undecorated progress dialog displaying asynchronous operation progress,
 * animated spinner, and descriptive status notes with Material 3 FlatLaf styling.
 *
 * @param owner the owning parent [JFrame], or `null` if unparented.
 * @param initialText the initial descriptive progress message.
 * @param initialIcon optional [Icon] to display; if `null`, animated spinner is used.
 * @param showAnimation whether to display animated loading spinner when no icon is set.
 */
open class ProgressDialog @JvmOverloads constructor(
  private val owner: JFrame?,
  private val initialText: String = I18n.get(I18n.COMMON_LOADING),
  initialIcon: Icon? = null,
  private val showAnimation: Boolean = true
) : JDialog(owner) {

  @VisibleForTesting
  internal val contentPane: JPanel = JPanel()

  @VisibleForTesting
  internal val progressbar: JProgressBar = JProgressBar(0, 100)

  @VisibleForTesting
  internal val titleLabel: JLabel = JLabel(I18n.get(I18n.COMMON_LOADING))

  @VisibleForTesting
  internal val progressText: JTextArea = object : JTextArea(2, 40) {
    override fun getPreferredSize(): Dimension {
      val baseSize = super.getPreferredSize()
      val currentWidth = if (width > 0) width else UIScaleUtils.dip(400)
      setSize(currentWidth, Short.MAX_VALUE.toInt())
      val wrappedSize = super.getPreferredSize()
      return Dimension(
        maxOf(baseSize.width, currentWidth),
        maxOf(baseSize.height, wrappedSize.height)
      )
    }
  }

  @VisibleForTesting
  internal val statusIcon: JLabel = JLabel()

  @VisibleForTesting
  internal val loadingSpinner: LoadingSpinner = LoadingSpinner(28)

  @VisibleForTesting
  internal val iconPanel: JPanel = JPanel(BorderLayout()).apply {
    isOpaque = false
    add(statusIcon, BorderLayout.CENTER)
    add(loadingSpinner, BorderLayout.NORTH)
  }

  companion object {
    /**
     * Creates, packs, centers, and displays a [ProgressDialog] for the given [parent].
     *
     * @param parent the owning parent [JFrame].
     * @param initialText the initial descriptive progress message.
     * @param initialIcon optional [Icon] to display.
     * @param showAnimation whether to show animated loading spinner when icon is null.
     * @return the visible [ProgressDialog] instance.
     */
    @JvmStatic
    @JvmOverloads
    fun showProgressDialog(
      parent: JFrame?,
      initialText: String = I18n.get(I18n.COMMON_LOADING),
      initialIcon: Icon? = null,
      showAnimation: Boolean = true
    ): ProgressDialog {
      val dialog = ProgressDialog(parent, initialText, initialIcon, showAnimation)
      dialog.pack()
      val targetW = maxOf(dialog.width, dialog.minimumSize.width)
      val targetH = maxOf(dialog.height, dialog.minimumSize.height)
      dialog.setSize(targetW, targetH)
      dialog.setLocationRelativeTo(parent)
      dialog.isVisible = true
      return dialog
    }
  }

  init {
    buildUi()
    isUndecorated = true
    setContentPane(contentPane)

    minimumSize = Dimension(UIScaleUtils.dip(480), UIScaleUtils.dip(115))

    val cardBorder = CardBorderUtils.createCardBorder(
      arc = CardBorderUtils.DEFAULT_CARD_ARC,
      insets = Insets(0, 0, 0, 0)
    )
    rootPane.border = cardBorder
    updateProgressIcon(initialIcon)
    owner?.isEnabled = false
  }

  /**
   * Updates or clears the progress icon displayed in the dialog.
   *
   * @param icon the [Icon] to display; if `null`, returns to animated spinner or none.
   */
  open fun updateProgressIcon(icon: Icon?) {
    statusIcon.icon = icon
    if (icon != null) {
      statusIcon.isVisible = true
      loadingSpinner.isVisible = false
      iconPanel.isVisible = true
    } else if (showAnimation) {
      statusIcon.isVisible = false
      loadingSpinner.isVisible = true
      iconPanel.isVisible = true
    } else {
      statusIcon.isVisible = false
      loadingSpinner.isVisible = false
      iconPanel.isVisible = false
    }
  }

  open fun publishProgress(progress: Int) {
    if (progress > 0 && progressbar.isIndeterminate) {
      progressbar.isIndeterminate = false
    }
    progressbar.value = progress
  }

  open fun updateProgressText(text: String?) {
    progressText.text = text ?: ""
    progressText.toolTipText = if (text.isNullOrEmpty()) null else text
    if (isShowing) {
      val prevHeight = height
      pack()
      val targetW = maxOf(width, minimumSize.width)
      val targetH = maxOf(height, minimumSize.height)
      if (width != targetW || height != targetH) {
        setSize(targetW, targetH)
      }
      if (height != prevHeight && owner != null) {
        setLocationRelativeTo(owner)
      }
    }
  }

  open fun finishProgress() {
    loadingSpinner.isVisible = false
    owner?.isEnabled = true
    dispose()
  }

  private fun buildUi() {
    contentPane.layout = BorderLayout(UIScaleUtils.dip(14), 0)
    contentPane.background = UIManager.getColor("Panel.background")

    val padV = UIScaleUtils.dip(16)
    val padH = UIScaleUtils.dip(20)
    contentPane.border = EmptyBorder(padV, padH, padV, padH)

    progressbar.isStringPainted = true
    progressbar.isIndeterminate = true
    progressbar.putClientProperty(FlatClientProperties.PROGRESS_BAR_LARGE_HEIGHT, true)
    progressbar.putClientProperty(FlatClientProperties.PROGRESS_BAR_SQUARE, false)

    val baseFont = UIManager.getFont("Label.font") ?: Font(Font.SANS_SERIF, Font.PLAIN, 12)
    titleLabel.font = baseFont.deriveFont(Font.BOLD, UIScaleUtils.dip(13).toFloat())

    progressText.font = baseFont.deriveFont(Font.PLAIN, UIScaleUtils.dip(12).toFloat())
    progressText.foreground = CardBorderUtils.resolveSecondaryLabelColor()
    progressText.lineWrap = true
    progressText.wrapStyleWord = true
    progressText.isEditable = false
    progressText.isFocusable = false
    progressText.isOpaque = false
    progressText.text = initialText

    val rightPanel = JPanel(BorderLayout(0, UIScaleUtils.dip(6))).apply {
      isOpaque = false
      add(titleLabel, BorderLayout.NORTH)
      add(progressText, BorderLayout.CENTER)
      add(progressbar, BorderLayout.SOUTH)
    }

    val iconContainer = JPanel(BorderLayout()).apply {
      isOpaque = false
      add(iconPanel, BorderLayout.NORTH)
    }

    contentPane.add(iconContainer, BorderLayout.WEST)
    contentPane.add(rightPanel, BorderLayout.CENTER)
  }
}
