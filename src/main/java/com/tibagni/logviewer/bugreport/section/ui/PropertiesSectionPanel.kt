package com.tibagni.logviewer.bugreport.section.ui

import com.tibagni.logviewer.bugreport.section.PropertiesSection
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.view.maxWidthOfColumn
import java.awt.GridBagConstraints
import javax.swing.JScrollPane
import javax.swing.JTable

class PropertiesSectionPanel(private val section: PropertiesSection) : SectionPanel(section.sectionName) {
  init {
    buildUi()
  }

  private fun buildUi() {
    val data = arrayOf(
      arrayOf("Build", section.build),
      arrayOf("Fingerprint", section.fingerprint),
      arrayOf("Bootloader", section.bootloader),
      arrayOf("Kernel", section.kernel),
      arrayOf("Uptime", section.uptime)
    )
    val propTable = JTable()
    propTable.model = PropTableModel(data, arrayOf("Property", "Value"))
    propTable.setDefaultRenderer(Object::class.java, PropTableRenderer())

    // Resize the first column according to its maximum width
    val propColWidth = propTable.maxWidthOfColumn(0)
    propTable.columnModel.getColumn(0).minWidth = propColWidth * 1.5.toInt()
    propTable.columnModel.getColumn(0).maxWidth = propColWidth * 3
    propTable.columnModel.getColumn(0).preferredWidth = propColWidth * 2

    propTable.tableHeader.resizingAllowed = true
    add(
      JScrollPane(propTable),
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(1)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )
  }
}