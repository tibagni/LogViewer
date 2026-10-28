package com.tibagni.logviewer.bugreport.section.ui

import com.formdev.flatlaf.FlatClientProperties
import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.CommonIcons
import com.tibagni.logviewer.view.HintTextField
import com.tibagni.logviewer.view.whenTextChanges
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.util.*
import java.util.Timer
import javax.swing.*
import javax.swing.border.EmptyBorder
import kotlin.concurrent.schedule

/**
 * Base panel for bugreport sections providing a styled title header and optional
 * live search filtering with debounced execution and modern FlatLaf decorations.
 *
 * @param title display title for this section.
 * @param isSearchable whether a live search input field should be included.
 */
abstract class SectionPanel(
  private val title: String,
  private val isSearchable: Boolean = false
) : JPanel() {
  @VisibleForTesting
  internal var searchField: HintTextField? = null
  private var searchTimerTask: TimerTask? = null
  private var searchTimer: Timer? = null

  @VisibleForTesting
  internal lateinit var titleLabel: JLabel

  init {
    buildUi()

    searchField?.whenTextChanges {
      if (searchTimer == null) {
        searchTimer = Timer("search-timer", false)
      }

      searchTimerTask?.cancel()
      searchTimerTask = searchTimer?.schedule(1000) {
        SwingUtilities.invokeLater {
          onSearch(it)
        }
      }
    }
  }

  protected open fun onSearch(searchText: String) {}

  private fun buildUi() {
    layout = GridBagLayout()
    border = EmptyBorder(
      UIScaleUtils.dip(6),
      UIScaleUtils.dip(10),
      UIScaleUtils.dip(10),
      UIScaleUtils.dip(10)
    )

    titleLabel = JLabel(title).apply {
      font = font.deriveFont(Font.BOLD, UIScaleUtils.dip(14).toFloat())
    }
    add(
      titleLabel,
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(0)
        .withInsets(Insets(UIScaleUtils.dip(4), 0, UIScaleUtils.dip(8), 0))
        .withWeightx(1.0)
        .withFill(GridBagConstraints.HORIZONTAL)
        .build()
    )

    if (isSearchable) {
      val searchText = HintTextField(I18n.get(I18n.SEARCH_HINT)).apply {
        putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true)
        putClientProperty(
          FlatClientProperties.TEXT_FIELD_LEADING_ICON,
          CommonIcons.searchIcon(UIScaleUtils.dip(14))
        )
      }
      add(
        searchText,
        GBConstraintsBuilder()
          .withGridx(1)
          .withGridy(1)
          .withInsets(Insets(0, 0, UIScaleUtils.dip(8), 0))
          .withWeightx(1.0)
          .withFill(GridBagConstraints.HORIZONTAL)
          .build()
      )
      searchField = searchText
    }
  }

  override fun removeNotify() {
    super.removeNotify()
    // Clean up the text changer thread so we do not leak anything
    searchTimer?.cancel()
    searchTimer = null
  }
}
