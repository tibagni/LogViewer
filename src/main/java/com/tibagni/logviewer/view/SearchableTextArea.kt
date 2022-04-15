package com.tibagni.logviewer.view

import com.tibagni.logviewer.util.StringUtils
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.util.regex.Pattern
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.text.DefaultHighlighter

class SearchableTextArea(hasScroll: Boolean = true) : JPanel() {
  private val textArea = JTextArea()
  private val searchText = HintTextField("Search")

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

    searchText.isVisible = false
    textArea.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        when (e.keyCode) {
          KeyEvent.VK_F -> if (e.isControlDown) showSearch()
          KeyEvent.VK_ESCAPE -> hideSearch()
        }
      }
    })

    searchText.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        if (e.keyCode == KeyEvent.VK_ESCAPE) {
          hideSearch()
        }
      }
    })

    searchText.whenTextChanges {
      val searchString = searchText.text
      val contentString = textArea.text
      val highlighter = textArea.highlighter

      highlighter.removeAllHighlights()
      if (StringUtils.isNotEmpty(searchString) and StringUtils.isNotEmpty(contentString)) {
        val pattern = Pattern.compile(searchString, 0)
        val matcher = pattern.matcher(contentString)
        while (matcher.find()) {
          val start = matcher.start()
          val end = matcher.end()
          highlighter.addHighlight(start, end, DefaultHighlighter.DefaultPainter)
        }
      }
    }
  }

  private fun showSearch() {
    if (searchText.isVisible) return

    searchText.isVisible = true
    searchText.requestFocus()
    revalidate()
  }

  private fun hideSearch() {
    if (!searchText.isVisible) return

    searchText.isVisible = false
    searchText.text = ""
    textArea.requestFocus()
    revalidate()
  }

  private fun buildUi(hasScroll: Boolean) {
    layout = GridBagLayout()
    add(
      searchText,
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(1)
        .withWeightx(1.0)
        .withFill(GridBagConstraints.HORIZONTAL)
        .build()
    )

    val comp: Component = if (hasScroll) JScrollPane(textArea) else textArea
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