package com.tibagni.logviewer.filter

import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.util.StringUtils
import com.tibagni.logviewer.util.SwingUtils
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.StatusBadge
import java.awt.BorderLayout
import java.awt.Component
import java.awt.GridBagLayout
import java.awt.font.TextAttribute
import java.io.Serializable
import java.util.HashMap
import java.util.function.Supplier
import javax.swing.BorderFactory
import javax.swing.JCheckBox
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer
import javax.swing.border.Border
import javax.swing.border.EmptyBorder

/**
 * List cell renderer for filter rows displaying a checkbox with the filter name and
 * an aligned [StatusBadge] displaying the matching line count.
 */
class FilterCellRenderer @JvmOverloads constructor(
  private val contextTagSupplier: Supplier<FilterContextTag>? = null
) : JPanel(BorderLayout()), ListCellRenderer<Any>, Serializable {

  companion object {
    private val DEFAULT_NO_FOCUS_BORDER: Border = EmptyBorder(
      UIScaleUtils.dip(1),
      UIScaleUtils.dip(1),
      UIScaleUtils.dip(1),
      UIScaleUtils.dip(1)
    )
  }

  val checkBox: JCheckBox = JCheckBox()
  val countBadge: StatusBadge = StatusBadge()

  private val badgeWrapper = JPanel(GridBagLayout()).apply {
    isOpaque = false
    border = BorderFactory.createEmptyBorder(0, UIScaleUtils.dip(4), 0, UIScaleUtils.dip(4))
    add(countBadge)
  }

  var highlightedText: String? = null

  val text: String
    get() = checkBox.text

  init {
    isOpaque = true
    border = DEFAULT_NO_FOCUS_BORDER
    name = "List.cellRenderer"

    checkBox.isOpaque = false
    countBadge.paddingHorizontalDp = 6

    add(checkBox, BorderLayout.CENTER)
    add(badgeWrapper, BorderLayout.EAST)
  }

  override fun updateUI() {
    super.updateUI()
    border = DEFAULT_NO_FOCUS_BORDER
    // Swing calls updateUI() from the superclass constructor before child properties
    // are initialized. Child components are only present after init {...} has run.
    if (componentCount > 0) {
      checkBox.updateUI()
      countBadge.updateUI()
    }
  }

  override fun getListCellRendererComponent(
    list: JList<*>,
    value: Any,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean
  ): Component {
    val filter = value as Filter
    var text = filter.name

    val hlText = highlightedText?.takeIf { it.isNotEmpty() }
    val hlStart = if (hlText != null) text.indexOf(hlText, ignoreCase = true) else -1

    text = if (hlStart >= 0 && hlText != null) {
      val hlEnd = hlStart + hlText.length
      StringUtils.htmlHighlightAndEscape(text, hlStart, hlEnd)
    } else {
      StringUtils.htmlEscape(text)
    }

    val verbosityMarkColor = ServiceLocator.themeManager.textForegroundHex
    text = "<small color=\"$verbosityMarkColor\">${filter.verbosity.toString()[0]}</small> $text"

    val contextTag = contextTagSupplier?.get()
    val contextInfo = if (contextTag != null) filter.getContext(contextTag) else null
    if (contextInfo != null) {
      countBadge.updateCount(contextInfo.totalLinesFound)
    } else {
      countBadge.clear()
    }

    componentOrientation = list.componentOrientation
    checkBox.isSelected = filter.isApplied

    var itemSelected = isSelected
    val dropLocation = list.dropLocation
    if (dropLocation != null && !dropLocation.isInsert && dropLocation.index == index) {
      itemSelected = true
    }

    if (itemSelected) {
      background = SwingUtils.changeColorAlpha(list.selectionBackground, 25)
    } else {
      background = list.background
    }

    checkBox.foreground = filter.color
    text = StringUtils.wrapHtml(text)

    var font = list.font
    if (!filter.nameIsPattern()) {
      val attributes = HashMap(font.attributes)
      attributes[TextAttribute.UNDERLINE] = TextAttribute.UNDERLINE_ON
      font = font.deriveFont(attributes)
    }

    checkBox.text = text
    checkBox.isEnabled = list.isEnabled
    checkBox.font = font
    toolTipText = StringUtils.wrapHtml("${filter.patternString}<br>${filter.verbosity}")

    return this
  }
}
