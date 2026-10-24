package com.tibagni.logviewer.bugreport.section.ui

import com.formdev.flatlaf.FlatClientProperties
import com.tibagni.logviewer.bugreport.section.CarrierConfigSection
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.CommonIcons
import com.tibagni.logviewer.view.FlatButton
import com.tibagni.logviewer.view.SearchableTextArea
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSeparator
import javax.swing.JTable
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.table.DefaultTableModel

/**
 * Panel displaying carrier configuration information parsed from bugreports.
 *
 * Each carrier configuration source is presented in a collapsible panel with
 * a table styled with alternating rows, theme-aware chevrons, and toolbar
 * button controls.
 *
 * @param section carrier configuration section containing config entries
 *                and loading logs
 */
class CarrierConfigSectionPanel(
  private val section: CarrierConfigSection
) : SectionPanel(section.sectionName, true) {
  private val carrierConfigModels = HashMap<String, CCTableModel>()
  internal val configTables = mutableListOf<JTable>()
  internal val collapseButtons = mutableListOf<FlatButton>()
  internal val titleLabels = mutableListOf<JLabel>()
  internal val configPanels = mutableListOf<JPanel>()
  private val ccHeaderTitles = arrayOf("Property", "Value")

  init {
    buildUi()
  }

  /**
   * Filters carrier configuration table rows across all sources based on
   * the provided search query.
   *
   * @param searchText the search text to match against configuration keys
   */
  public override fun onSearch(searchText: String) {
    for (entry in section.configs.entries) {
      val filteredRows = section.configs[entry.key]
        ?.filterKeys { it.contains(searchText) }
        ?.map { arrayOf(it.key, it.value) }
        ?.toTypedArray()
      carrierConfigModels[entry.key]?.setDataVector(
        filteredRows,
        ccHeaderTitles
      )
    }
  }

  /**
   * Builds the panel layout including collapsible carrier configuration
   * tables and the loading logs text area.
   */
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
      val logsTitle = JLabel(
        I18n.get(I18n.BUGREPORT_CARRIER_CONFIG_LOADING_LOGS)
      )
      logsTitle.font = Font(Font.DIALOG, Font.BOLD, logsTitle.font.size)
      it.add(logsTitle)
    })
    container.add(JSeparator())
    container.add(SearchableTextArea(false).also {
      it.isEditable = false
      it.text = section.loadingLogs
    })

    add(
      JScrollPane(container).also {
        it.verticalScrollBar.unitIncrement = 16
      },
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(2)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )
  }

  /**
   * Creates a collapsible panel containing a carrier configuration table
   * for the specified source.
   *
   * @param title the configuration source name
   * @param rows the key-value rows for this configuration source
   * @return [JPanel] containing the collapsible header and table
   */
  private fun createConfigTable(
    title: String,
    rows: Array<Array<String>>
  ): JPanel {
    val panel = JPanel(GridBagLayout())
    val collapseButton = FlatButton().apply {
      putClientProperty(
        FlatClientProperties.BUTTON_TYPE,
        FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON
      )
      icon = CommonIcons.chevronRightIcon(14)
      text = ""
    }
    val header = JPanel(FlowLayout(FlowLayout.LEFT))
    val table = JTable().apply {
      rowHeight = UIScaleUtils.dip(22)
      showHorizontalLines = true
      showVerticalLines = false
      fillsViewportHeight = true
      intercellSpacing = Dimension(0, 1)
      setDefaultRenderer(Any::class.java, PropTableRenderer())
      tableHeader.resizingAllowed = true
    }
    val model = CCTableModel(rows, ccHeaderTitles)
    carrierConfigModels[title] = model
    table.model = model

    val toggleTableVisibility = {
      table.isVisible = !table.isVisible
      collapseButton.icon = if (table.isVisible) {
        CommonIcons.chevronDownIcon(14)
      } else {
        CommonIcons.chevronRightIcon(14)
      }
      truncatePanelMaxSize(panel)
      revalidate()
    }

    collapseButton.addActionListener {
      toggleTableVisibility()
    }

    val titleLabel = JLabel("$title (${rows.size})").apply { // i18n:ignore
      font = Font(font.name, Font.BOLD, font.size)
      cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
      addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent?) {
          toggleTableVisibility()
        }
      })
    }

    header.add(collapseButton)
    header.add(titleLabel)

    val sepColor = UIManager.getColor("Separator.foreground")
      ?: UIManager.getColor("Component.borderColor")
      ?: Color(128, 128, 128, 80)
    panel.border = BorderFactory.createMatteBorder(0, 0, 1, 0, sepColor)

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

    // Update the maximum size every time the contents of the table changes
    // so we always display all the elements
    model.addTableModelListener {
      SwingUtilities.invokeLater { truncatePanelMaxSize(panel) }
    }

    // Start collapsed
    table.isVisible = false
    truncatePanelMaxSize(panel)

    collapseButtons.add(collapseButton)
    titleLabels.add(titleLabel)
    configTables.add(table)
    configPanels.add(panel)

    return panel
  }

  /**
   * Clamps the panel's maximum height to its preferred height so components
   * do not expand vertically when collapsed in a BoxLayout container.
   *
   * @param panel the target panel whose maximum size to update
   */
  private fun truncatePanelMaxSize(panel: JPanel) {
    panel.maximumSize = Dimension(
      panel.maximumSize.width,
      panel.preferredSize.height
    )
  }
}

/**
 * Uneditable table model for carrier configuration key-value properties.
 *
 * @param data row data array of key-value pairs
 * @param cols column header names array
 */
private class CCTableModel(
  data: Array<Array<String>>,
  cols: Array<String>
) : DefaultTableModel(data, cols) {
  /**
   * Returns false so that carrier configuration table cells are uneditable.
   *
   * @param row table row index
   * @param column table column index
   * @return false always
   */
  override fun isCellEditable(row: Int, column: Int) = false
}
