package com.tibagni.logviewer.bugreport.section.ui

import com.tibagni.logviewer.bugreport.section.CarrierConfigSection
import com.tibagni.logviewer.util.StringUtils
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.view.SearchableTextArea
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel


class CarrierConfigSectionPanel(private val section: CarrierConfigSection) : SectionPanel(section.sectionName, true) {
  private val carrierConfigModels = HashMap<String, CCTableModel>()
  private val ccHeaderTitles = arrayOf("Property", "Value")

  init {
    buildUi()
  }

  override fun onSearch(searchText: String) {
    for (entry in section.configs.entries) {
      val filteredRows =
        section.configs[entry.key]?.filterKeys { it.contains(searchText) }?.map { arrayOf(it.key, it.value) }
          ?.toTypedArray()
      carrierConfigModels[entry.key]?.setDataVector(filteredRows, ccHeaderTitles)
    }
  }

  private fun buildUi() {
    val container = JPanel()
    container.layout = BoxLayout(container, BoxLayout.Y_AXIS)

    for (entry in section.configs.entries) {
      val source = entry.key
      val configs = entry.value
      val rows = configs.map { arrayOf(it.key, it.value) }.toTypedArray()
      container.add(createConfigTable(source, rows))
    }
    container.add(JPanel(FlowLayout(FlowLayout.LEFT)).also {
      val logsTitle = JLabel("Loading logs")
      logsTitle.font = Font(Font.DIALOG, Font.BOLD, logsTitle.font.size)
      it.add(logsTitle)
    })
    container.add(JSeparator())
    container.add(SearchableTextArea(false).also {
      it.isEditable = false
      it.text = section.loadingLogs
    })

    add(
      JScrollPane(container).also { it.verticalScrollBar.unitIncrement = 16 },
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(2)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )
  }

  private fun createConfigTable(title: String, rows: Array<Array<String>>): JPanel {
    val panel = JPanel(GridBagLayout())
    val collapseButton = JButton(StringUtils.RIGHT_ARROW_HEAD)
    val header = JPanel(FlowLayout(FlowLayout.LEFT))
    val table = JTable()
    val model = CCTableModel(rows, ccHeaderTitles)
    carrierConfigModels[title] = model
    table.model = model
    table.tableHeader.resizingAllowed = true

    header.add(collapseButton)
    header.add(JLabel(title))

    panel.add(
      header,
      GBConstraintsBuilder()
        .withGridx(1)
        .withWeightx(1.0)
        .withFill(GridBagConstraints.HORIZONTAL)
        .build()
    )
    panel.add(
      table,
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(1)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )

    // Update the maximum size every time the contents of the table changes so we always display all the elements
    model.addTableModelListener { SwingUtilities.invokeLater { truncatePanelMaxSize(panel) } }

    collapseButton.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent?) {
        table.isVisible = !table.isVisible
        collapseButton.text = if (table.isVisible) StringUtils.DOWN_ARROW_HEAD else StringUtils.RIGHT_ARROW_HEAD

        // Update the maximum size here so the components are not centered when collapsed
        truncatePanelMaxSize(panel)
        revalidate()
      }
    })

    // Start collapsed
    table.isVisible = false
    truncatePanelMaxSize(panel)
    return panel
  }

  private fun truncatePanelMaxSize(panel: JPanel) {
    panel.maximumSize = Dimension(panel.maximumSize.width, panel.preferredSize.height)
  }
}

private class CCTableModel(data: Array<Array<String>>, cols: Array<String>) : DefaultTableModel(data, cols) {
  override fun isCellEditable(row: Int, column: Int) = false
}