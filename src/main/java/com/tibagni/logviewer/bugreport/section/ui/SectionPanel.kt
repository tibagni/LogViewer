package com.tibagni.logviewer.bugreport.section.ui

import com.tibagni.logviewer.util.layout.FontBuilder
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.HintTextField
import com.tibagni.logviewer.view.whenTextChanges
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.util.*
import java.util.Timer
import javax.swing.*
import kotlin.concurrent.schedule

abstract class SectionPanel(private val title: String, private val isSearchable: Boolean = false): JPanel() {
  private var searchField: HintTextField? = null
  private var searchTimerTask: TimerTask? = null
  private var searchTimer: Timer? = null

  init {
    buildUi()

    searchField?.whenTextChanges {
      if (searchTimer == null) {
        searchTimer = Timer("search-timer", false)
      }

      searchTimerTask?.cancel()
      searchTimerTask = searchTimer?.schedule(500) {
        onSearch(it)
      }
    }
  }

  protected open fun onSearch(searchText: String) {}

  private fun buildUi() {
    layout = GridBagLayout()
    val titleLabel = JLabel()
    titleLabel.text = title
    titleLabel.font = FontBuilder(titleLabel).withStyle(Font.BOLD).build()
    add(
      titleLabel,
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(0)
        .withIpady(UIScaleUtils.dip(10))
        .withWeightx(1.0)
        .withFill(GridBagConstraints.HORIZONTAL)
        .build()
    )

    if (isSearchable) {
      val searchText = HintTextField("Search")
      add(
        searchText,
        GBConstraintsBuilder()
          .withGridx(1)
          .withGridy(1)
          .withWeightx(1.0)
          .withFill(GridBagConstraints.HORIZONTAL)
          .build()
      )
      searchField = searchText
    }
  }

  override fun removeNotify() {
    super.removeNotify()
    // This method is called by the Container when this component is being removed from it
    // Use this opportunity to clean up the text changer thread so we do not leak anything
    searchTimer?.cancel()
    searchTimer = null
  }
}