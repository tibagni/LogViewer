package com.tibagni.logviewer.bugreport.section.ui

import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.PaddingListCellRenderer
import com.tibagni.logviewer.view.SearchableTextArea
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

abstract class ListSectionPanel(title: String, isSearchable: Boolean = true) : SectionPanel(title, isSearchable) {
  private lateinit var listModel: DefaultListModel<String>
  private lateinit var list: JList<String>
  private lateinit var splitPane: JSplitPane
  private lateinit var detailsText: SearchableTextArea

  init {
    buildUi()

    list.addListSelectionListener {
      if (list.selectedIndex > listModel.size() || list.selectedIndex < 0) {
        return@addListSelectionListener
      }

      onItemSelected(listModel[list.selectedIndex])
    }
  }

  protected abstract fun onItemSelected(selectedValue: String)

  protected fun updateListData(listItems: List<String>) {
    listModel.removeAllElements()
    listModel.addAll(listItems)
    list.invalidate()
  }

  protected fun setDetailsText(details: String?) {
    detailsText.text = details
    detailsText.caretPosition = 0
  }

  private fun buildUi() {
    listModel = DefaultListModel()
    list = JList(listModel)
    list.selectionMode = ListSelectionModel.SINGLE_SELECTION
    list.cellRenderer = PaddingListCellRenderer(UIScaleUtils.dip(2))

    splitPane = JSplitPane()
    splitPane.orientation = JSplitPane.VERTICAL_SPLIT
    splitPane.resizeWeight = 0.5
    add(
      splitPane,
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(2)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )

    val detailsPane = JPanel()
    detailsPane.layout = GridBagLayout()
    detailsText = SearchableTextArea()
    detailsText.isEditable = false
    detailsText.wrapStyleWord = true
    detailsPane.add(
      detailsText,
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(1)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )

    splitPane.leftComponent = JScrollPane(list) // Left or above (above in this case)
    splitPane.rightComponent = detailsPane
  }
}