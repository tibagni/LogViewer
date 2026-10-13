package com.tibagni.logviewer.view

import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.ImageIcon
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.SwingConstants
import javax.swing.UIManager
import org.jetbrains.annotations.VisibleForTesting

sealed class SidePanelEntry {
  data class Action(
    val id: String,
    val icon: ImageIcon,
    val text: String,
    val isEnabled: Boolean = true,
    val onAction: () -> Unit
  ) : SidePanelEntry()

  data class Toggle(
    val id: String,
    val icon: ImageIcon,
    val text: String,
    val initialActive: Boolean = false,
    val isEnabled: Boolean = true,
    val onToggle: (Boolean) -> Unit
  ) : SidePanelEntry()

  object Separator : SidePanelEntry()
}

private const val BUTTON_SPACING_DIP = 4
private const val SEPARATOR_SPACING_DIP = 6
private const val SEPARATOR_TOP_SPACING_DIP = 2
private const val SEPARATOR_HEIGHT_DIP = 2

class SideButtonsPanel(
  val isExpandable: Boolean = true,
  entries: List<SidePanelEntry>
) : JPanel() {

  private val buttons = mutableMapOf<String, SideButton>()
  @VisibleForTesting internal var toggleExpandBtn: SideButton? = null

  var isExpanded: Boolean = false
    set(value) {
      if (field != value && isExpandable) {
        field = value
        updateExpandedState()
      }
    }

  init {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    updateBorder()
    updateDimensions()

    if (isExpandable) {
      val expandBtn = SideButton(
        CommonIcons.expandIcon(),
        I18n.get(I18n.LIVE_LOGS_EXPAND_SIDEBAR),
        I18n.get(I18n.LIVE_LOGS_COLLAPSE_SIDEBAR)
      ).apply {
        addActionListener { this@SideButtonsPanel.isExpanded = !this@SideButtonsPanel.isExpanded }
      }
      toggleExpandBtn = expandBtn
      add(expandBtn)
      addSeparator()
    }

    entries.forEachIndexed { index, entry ->
      when (entry) {
        is SidePanelEntry.Action -> addButton(entry.id, createActionButton(entry), hasNextButton(entries, index))
        is SidePanelEntry.Toggle -> addButton(entry.id, createToggleButton(entry), hasNextButton(entries, index))
        is SidePanelEntry.Separator -> addSeparator(topSpacingDip = SEPARATOR_TOP_SPACING_DIP)
      }
    }

    add(Box.createVerticalGlue())
  }

  private fun addButton(id: String, button: SideButton, hasNextButton: Boolean) {
    buttons[id] = button
    add(button)
    if (hasNextButton) {
      add(createButtonSpacing())
    }
  }

  private fun addSeparator(
    topSpacingDip: Int = SEPARATOR_SPACING_DIP,
    bottomSpacingDip: Int = SEPARATOR_SPACING_DIP
  ) {
    add(createSeparatorSpacing(topSpacingDip))
    add(createSeparator())
    add(createSeparatorSpacing(bottomSpacingDip))
  }

  private fun createActionButton(entry: SidePanelEntry.Action): SideButton =
    SideButton(
      entry.icon,
      entry.text,
      entry.text
    ).apply {
      isEnabled = entry.isEnabled
      addActionListener { entry.onAction() }
    }

  private fun createToggleButton(entry: SidePanelEntry.Toggle): ToggleSideButton =
    ToggleSideButton(
      imageIcon = entry.icon,
      tooltip = entry.text,
      initialActive = entry.initialActive,
      textLabel = entry.text,
      listener = entry.onToggle
    ).apply {
      isEnabled = entry.isEnabled
    }

  private fun hasNextButton(entries: List<SidePanelEntry>, index: Int): Boolean =
    index < entries.size - 1 && entries[index + 1] !is SidePanelEntry.Separator

  private fun createButtonSpacing(): Component =
    Box.createVerticalStrut(UIScaleUtils.dip(BUTTON_SPACING_DIP))

  private fun createSeparatorSpacing(spacingDip: Int = SEPARATOR_SPACING_DIP): Component =
    Box.createVerticalStrut(UIScaleUtils.dip(spacingDip))

  private fun createSeparator(): JSeparator =
    JSeparator(SwingConstants.HORIZONTAL).apply {
      maximumSize = Dimension(Short.MAX_VALUE.toInt(), UIScaleUtils.dip(SEPARATOR_HEIGHT_DIP))
    }

  override fun updateUI() {
    super.updateUI()
    @Suppress("SENSELESS_COMPARISON")
    if (buttons != null) {
      updateBorder()
    }
  }

  private fun updateBorder() {
    border = BorderFactory.createCompoundBorder(
      BorderFactory.createMatteBorder(
        0, 1, 0, 0,
        UIManager.getColor("Separator.foreground") ?: Color(220, 220, 220)
      ),
      BorderFactory.createEmptyBorder(
        UIScaleUtils.dip(6),
        UIScaleUtils.dip(2),
        UIScaleUtils.dip(6),
        UIScaleUtils.dip(2)
      )
    )
  }

  private fun updateDimensions() {
    val width = if (isExpanded) {
      UIScaleUtils.dip(SideButton.EXPANDED_WIDTH)
    } else {
      UIScaleUtils.dip(SideButton.COLLAPSED_WIDTH)
    }
    preferredSize = Dimension(width, 0)
    maximumSize = Dimension(width, Short.MAX_VALUE.toInt())
    minimumSize = Dimension(width, 0)
  }

  private fun updateExpandedState() {
    updateDimensions()
    if (isExpanded) {
      toggleExpandBtn?.setIconAndTooltip(
        CommonIcons.collapseIcon(),
        I18n.get(I18n.LIVE_LOGS_COLLAPSE_SIDEBAR),
        I18n.get(I18n.LIVE_LOGS_COLLAPSE_SIDEBAR)
      )
    } else {
      toggleExpandBtn?.setIconAndTooltip(
        CommonIcons.expandIcon(),
        I18n.get(I18n.LIVE_LOGS_EXPAND_SIDEBAR),
        I18n.get(I18n.LIVE_LOGS_EXPAND_SIDEBAR)
      )
    }
    toggleExpandBtn?.isExpanded = isExpanded
    buttons.values.forEach { it.isExpanded = isExpanded }
    revalidate()
    repaint()
    parent?.revalidate()
    parent?.repaint()
  }

  fun setItemEnabled(id: String, enabled: Boolean) {
    buttons[id]?.isEnabled = enabled
  }

  fun isItemEnabled(id: String): Boolean {
    return buttons[id]?.isEnabled ?: false
  }

  fun setItemActive(id: String, active: Boolean) {
    val btn = buttons[id] as? ToggleSideButton
    btn?.isActive = active
  }

  fun isItemActive(id: String): Boolean {
    val btn = buttons[id] as? ToggleSideButton
    return btn?.isActive ?: false
  }

  fun updateItem(id: String, icon: ImageIcon, text: String) {
    buttons[id]?.setIconAndTooltip(icon, text, text)
  }

  fun performAction(id: String) {
    buttons[id]?.doClick()
  }

  @VisibleForTesting
  internal fun getButton(id: String): SideButton? = buttons[id]
}
