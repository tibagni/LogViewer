package com.tibagni.logviewer.bugreport.section.ui

import com.tibagni.logviewer.bugreport.section.SubscriptionInfo
import com.tibagni.logviewer.bugreport.section.SubscriptionsSection
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.view.SearchableTextArea
import com.tibagni.logviewer.view.maxWidthOfColumn
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*


class SubscriptionsSectionPanel(private val section: SubscriptionsSection) : SectionPanel(section.sectionName) {
  init {
    buildUi()
  }

  private fun buildUi() {
    val container = JPanel()
    container.layout = BoxLayout(container, BoxLayout.Y_AXIS)
    container.add(buildPropertiesTable())
    container.add(JSeparator())
    container.add(buildSubsListsPanel())
    container.add(JSeparator())
    container.add(JPanel(FlowLayout(FlowLayout.LEFT)).also {
      val logsTitle = JLabel("Loading logs")
      logsTitle.font = Font(Font.DIALOG, Font.BOLD, logsTitle.font.size)
      it.add(logsTitle)
    })
    container.add(SearchableTextArea(false).also {
      it.lineWrap = true
      it.isEditable = false
      it.text = section.logs
    })

    add(
      JScrollPane(container).also {
        it.verticalScrollBar.unitIncrement = 16
        it.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
      },
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(1)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )
  }

  private fun buildPropertiesTable(): Component {
    val data = section.subscriptionManagerProperties.map { arrayOf(it.key, it.value) }.toTypedArray()
    val propTable = JTable()
    propTable.model = PropTableModel(data, arrayOf("Property", "Value"))
    propTable.setDefaultRenderer(Object::class.java, PropTableRenderer())

    // Resize the first column according to its maximum width
    val propColWidth = propTable.maxWidthOfColumn(0)
    propTable.columnModel.getColumn(0).minWidth = propColWidth * 1.5.toInt()
    propTable.columnModel.getColumn(0).maxWidth = propColWidth * 3
    propTable.columnModel.getColumn(0).preferredWidth = propColWidth * 2

    propTable.tableHeader.resizingAllowed = true
    return propTable
  }

  private fun buildSubsListsPanel(): Component {
    val container = JPanel()
    container.layout = BoxLayout(container, BoxLayout.Y_AXIS)
    val headers = arrayOf("Sub Id", "Carrier Id", "MCC", "MNC", "Display Name", "Carrier Name", "Name Source")

    val activeSubList = JTable()
    activeSubList.model = PropTableModel(getDataFromSubList(section.activeSubscriptions), headers)
    activeSubList.setDefaultRenderer(Object::class.java, PropTableRenderer())

    val allSubsList = JTable()
    allSubsList.model = PropTableModel(getDataFromSubList(section.allSubscriptions), headers)
    allSubsList.setDefaultRenderer(Object::class.java, PropTableRenderer())

    // Manually add the table headers as we are not using a scroll pane for each table
    container.add(JPanel(FlowLayout(FlowLayout.LEFT)).also {
      val logsTitle = JLabel("Active subscriptions")
      logsTitle.font = Font(Font.DIALOG, Font.BOLD, logsTitle.font.size)
      it.add(logsTitle)
    })
    container.add(activeSubList.tableHeader)
    container.add(activeSubList)
    container.add(JPanel(FlowLayout(FlowLayout.LEFT)).also {
      val logsTitle = JLabel("All subscriptions")
      logsTitle.font = Font(Font.DIALOG, Font.BOLD, logsTitle.font.size)
      it.add(logsTitle)
    })
    container.add(allSubsList.tableHeader)
    container.add(allSubsList)

    val clickHandler = object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (e.clickCount == 2) {
          onSubscriptionClicked(e.source as JTable)
        }
      }
    }
    val enterKeyHandler = object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        if (e.keyCode == KeyEvent.VK_ENTER) {
          onSubscriptionClicked(e.source as JTable)
        }
      }
    }
    activeSubList.addMouseListener(clickHandler)
    allSubsList.addMouseListener(clickHandler)
    activeSubList.addKeyListener(enterKeyHandler)
    allSubsList.addKeyListener(enterKeyHandler)

    return container
  }

  private fun getDataFromSubList(subList: List<SubscriptionInfo>): Array<Array<String>> {
    return subList.map {
      arrayOf(
        it.id.toString(),
        it.carrierId.toString(),
        it.mcc.toString(),
        it.mnc.toString(),
        it.displayName,
        it.carrierName,
        it.nameSource.toString())
    }.toTypedArray()
  }

  private fun onSubscriptionClicked(subList: JTable) {
    val subId = subList.model.getValueAt(subList.selectedRow, 0) as String
    val subInfo = section.allSubscriptions.find { it.id == subId.toInt() }

    // Format the text so each property is displayed in one line to make it more readable to the user
    val elements = subInfo?.fullText?.split("=") ?: listOf()
    val sb = StringBuilder("${elements[0]} = ")
    for (s in elements.subList(1, elements.size)) {
      val parts = s.split(" ")
      if (parts.size > 1) {
        val nextProperty = parts.last()
        sb.append(parts.subList(0, parts.lastIndex).joinToString(" "))
        sb.append("\n$nextProperty = ")
      } else {
        // We reached the end, so just append the last value
        sb.append(s)
      }
    }

    val formattedText = sb.toString().replace("{", "").replace("}", "")
    val textArea = JTextArea(formattedText)
    textArea.columns = 30
    textArea.rows = 20
    textArea.lineWrap = true
    textArea.wrapStyleWord = true

    JOptionPane.showMessageDialog(subList, JScrollPane(textArea), subInfo?.displayName, JOptionPane.PLAIN_MESSAGE)
  }
}