package com.tibagni.logviewer.view

import com.formdev.flatlaf.FlatClientProperties
import com.jgoodies.forms.builder.PanelBuilder
import com.jgoodies.forms.factories.CC
import com.jgoodies.forms.layout.FormLayout
import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.shortcuts.ShortcutAction
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import java.awt.Component
import java.awt.Cursor
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.util.regex.Pattern
import javax.swing.BorderFactory
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.border.Border
import javax.swing.text.DefaultHighlighter

/**
 * A text area component equipped with an integrated modern find bar,
 * real-time highlighting, match navigation, and Material 3 / FlatLaf styling.
 *
 * @param hasScroll whether to wrap the text area in a [JScrollPane].
 */
class SearchableTextArea(hasScroll: Boolean = true) : JPanel() {

  @VisibleForTesting
  internal val textArea = JTextArea()

  @VisibleForTesting
  internal val searchOptionPanel = JPanel()

  @VisibleForTesting
  internal val searchText = HintTextField("Search").apply {
    putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true)
  }

  @VisibleForTesting
  internal val searchResult = StatusBadge()

  @VisibleForTesting
  internal val searchLast = FlatButton("").apply {
    icon = CommonIcons.chevronUpIcon()
    toolTipText = I18n.get(I18n.SEARCH_PREV_MATCH)
  }

  @VisibleForTesting
  internal val searchNext = FlatButton("").apply {
    icon = CommonIcons.chevronDownIcon()
    toolTipText = I18n.get(I18n.SEARCH_NEXT_MATCH)
  }

  @VisibleForTesting
  internal val searchNavDivider = ToolbarDivider()

  @VisibleForTesting
  internal val matchCaseOption = JCheckBox(I18n.get(I18n.SEARCH_MATCH_CASE)).apply {
    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  }

  @VisibleForTesting
  internal val close = FlatButton("").apply {
    icon = CommonIcons.closeIcon()
    toolTipText = I18n.get(I18n.SEARCH_TOOLTIP_HIDE)
  }

  @VisibleForTesting
  internal val currentMatches = mutableListOf<Pair<Int, Int>>()

  @VisibleForTesting
  internal var currentMatchIndex = -1

  var isEditable: Boolean
    get() = textArea.isEditable
    set(value) {
      textArea.isEditable = value
    }

  var wrapStyleWord: Boolean
    get() = textArea.wrapStyleWord
    set(value) {
      textArea.wrapStyleWord = value
    }

  var text: String?
    get() = textArea.text
    set(value) {
      textArea.text = value
      if (searchOptionPanel.isVisible && searchText.text.isNotEmpty()) {
        updateHighlights()
      }
    }

  var caretPosition: Int
    get() = textArea.caretPosition
    set(value) {
      textArea.caretPosition = value
    }

  var lineWrap: Boolean
    get() = textArea.lineWrap
    set(value) {
      textArea.lineWrap = value
    }

  init {
    buildUi(hasScroll)
    searchOptionPanel.isVisible = false

    searchLast.addActionListener { navigateInDirection(false) }
    searchNext.addActionListener { navigateInDirection(true) }
    close.addActionListener { hideSearch() }
    matchCaseOption.addItemListener { updateHighlights() }

    textArea.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        val shortcutManager = ServiceLocator.shortcutManager
        when {
          shortcutManager.matches(e, ShortcutAction.SEARCH_FIND_IN_TABLE) -> showSearch()
          e.keyCode == KeyEvent.VK_ESCAPE -> hideSearch()
        }
      }
    })

    searchText.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        when (e.keyCode) {
          KeyEvent.VK_ESCAPE -> hideSearch()
          KeyEvent.VK_ENTER -> {
            if (e.isShiftDown) {
              navigateInDirection(false)
            } else {
              navigateInDirection(true)
            }
          }
        }
      }
    })

    searchText.whenTextChanges {
      updateHighlights()
    }
  }

  fun showSearch() {
    if (searchOptionPanel.isVisible) {
      if (!searchText.hasFocus()) {
        searchText.requestFocus()
      }
      return
    }

    searchOptionPanel.isVisible = true
    searchText.requestFocus()
    revalidate()
  }

  fun hideSearch() {
    if (!searchOptionPanel.isVisible) return

    searchOptionPanel.isVisible = false
    searchText.text = ""
    matchCaseOption.isSelected = false
    currentMatches.clear()
    currentMatchIndex = -1
    searchResult.clear()
    textArea.highlighter.removeAllHighlights()
    textArea.requestFocus()
    revalidate()
  }

  /**
   * Refreshes vector icons, search badge, and border styling on theme change.
   */
  fun onThemeChanged() {
    searchLast.icon = CommonIcons.chevronUpIcon()
    searchNext.icon = CommonIcons.chevronDownIcon()
    close.icon = CommonIcons.closeIcon()
    searchResult.updateUI()
    searchOptionPanel.border = createSearchOptionPanelBorder()
    searchOptionPanel.repaint()
    repaint()
  }

  internal fun updateHighlights() {
    val searchString = searchText.text
    val contentString = textArea.text
    val highlighter = textArea.highlighter

    highlighter.removeAllHighlights()
    currentMatches.clear()
    currentMatchIndex = -1

    if (searchString.isNullOrEmpty() || contentString.isNullOrEmpty()) {
      searchResult.clear()
      return
    }

    try {
      val flags = if (matchCaseOption.isSelected) 0 else Pattern.CASE_INSENSITIVE
      val pattern = Pattern.compile(Pattern.quote(searchString), flags)
      val matcher = pattern.matcher(contentString)
      while (matcher.find()) {
        val start = matcher.start()
        val end = matcher.end()
        currentMatches.add(start to end)
        highlighter.addHighlight(start, end, DefaultHighlighter.DefaultPainter)
      }
    } catch (_: Exception) {
      // Safely ignore pattern errors
    }

    if (currentMatches.isNotEmpty()) {
      navigateToMatch(0)
    } else {
      searchResult.updateResult(0, 0)
    }
  }

  internal fun navigateInDirection(forward: Boolean) {
    if (currentMatches.isEmpty()) return

    val nextIndex = if (forward) {
      (currentMatchIndex + 1) % currentMatches.size
    } else {
      if (currentMatchIndex <= 0) currentMatches.size - 1 else currentMatchIndex - 1
    }
    navigateToMatch(nextIndex)
  }

  internal fun navigateToMatch(index: Int) {
    if (index !in currentMatches.indices) return
    currentMatchIndex = index
    val (start, end) = currentMatches[index]
    textArea.select(start, end)
    textArea.caretPosition = end
    searchResult.updateResult(index + 1, currentMatches.size)
  }

  internal fun createSearchOptionPanelBorder(): Border {
    val borderCol = CardBorderUtils.resolveCardBorderColor()
    val padV = UIScaleUtils.dip(4)
    val padH = UIScaleUtils.dip(6)
    return BorderFactory.createCompoundBorder(
      BorderFactory.createMatteBorder(0, 0, UIScaleUtils.dip(1), 0, borderCol),
      BorderFactory.createEmptyBorder(padV, padH, padV, padH)
    )
  }

  private fun buildUi(hasScroll: Boolean) {
    layout = GridBagLayout()

    searchOptionPanel.isOpaque = false
    searchOptionPanel.border = createSearchOptionPanelBorder()

    val formLayout = FormLayout(
      "150dlu, 4dlu, pref, 4dlu, pref, 2dlu, pref, 4dlu, pref, 4dlu, pref, pref:grow, right:pref",
      "pref"
    )
    val builder = PanelBuilder(formLayout, searchOptionPanel)
    builder.add(searchText, CC.xy(1, 1))
    builder.add(searchResult, CC.xy(3, 1))
    builder.add(searchLast, CC.xy(5, 1))
    builder.add(searchNext, CC.xy(7, 1))
    builder.add(searchNavDivider, CC.xy(9, 1))
    builder.add(matchCaseOption, CC.xy(11, 1))
    builder.add(JLabel(), CC.xy(12, 1))
    builder.add(close, CC.xy(13, 1))

    add(
      searchOptionPanel,
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(1)
        .withWeightx(1.0)
        .withFill(GridBagConstraints.HORIZONTAL)
        .build()
    )

    val comp: Component = if (hasScroll) {
      JScrollPane(textArea).apply {
        border = BorderFactory.createEmptyBorder()
        viewportBorder = BorderFactory.createEmptyBorder()
      }
    } else {
      textArea
    }

    add(
      comp,
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(2)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )
  }
}
