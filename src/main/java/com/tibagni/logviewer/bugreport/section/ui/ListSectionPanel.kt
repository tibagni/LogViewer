package com.tibagni.logviewer.bugreport.section.ui

import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.PaddingListCellRenderer
import com.tibagni.logviewer.view.SearchableTextArea
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

/**
 * Base panel for bugreport sections displaying a selectable list on top
 * and detailed [SearchableTextArea] content below within a vertical split pane.
 *
 * @param title section title.
 * @param isSearchable whether the section contains a search input field.
 */
abstract class ListSectionPanel(
  title: String,
  isSearchable: Boolean = true
) : SectionPanel(title, isSearchable) {
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
    listItems.forEach { listModel.addElement(it) }
    list.invalidate()
  }

  protected fun setDetailsText(details: String?) {
    detailsText.text = details
    detailsText.caretPosition = 0
  }

  private fun buildUi() {
    listModel = DefaultListModel()
    list = JList(listModel).apply {
      selectionMode = ListSelectionModel.SINGLE_SELECTION
      cellRenderer = PaddingListCellRenderer(UIScaleUtils.dip(3))
    }

    splitPane = JSplitPane().apply {
      orientation = JSplitPane.VERTICAL_SPLIT
      resizeWeight = 0.4
      dividerSize = UIScaleUtils.dip(5)
      isOneTouchExpandable = true
    }

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

    val detailsPane = JPanel().apply {
      layout = GridBagLayout()
    }
    detailsText = SearchableTextArea().apply {
      isEditable = false
      wrapStyleWord = true
    }
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

    val scrollPane = JScrollPane(list).apply {
      border = BorderFactory.createEmptyBorder()
      verticalScrollBar.unitIncrement = 16
    }
    splitPane.leftComponent = scrollPane // Left or above (above in this case)
    splitPane.rightComponent = detailsPane
  }
}
