package com.tibagni.logviewer.bugreport.section

import com.tibagni.logviewer.util.layout.FontBuilder
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

abstract class SectionPanel(private val title: String): JPanel() {
  init {
    buildUi()
  }

  private fun buildUi() {
    layout = GridBagLayout()
    val titleLabel = JLabel()
    titleLabel.text = title
    titleLabel.font = FontBuilder(titleLabel).withStyle(Font.BOLD).build();
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
  }
}