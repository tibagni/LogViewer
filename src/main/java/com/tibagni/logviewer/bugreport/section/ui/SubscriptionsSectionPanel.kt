package com.tibagni.logviewer.bugreport.section.ui

import com.tibagni.logviewer.bugreport.section.SubscriptionInfo
import com.tibagni.logviewer.bugreport.section.SubscriptionsSection
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.view.SearchableTextArea
import com.tibagni.logviewer.view.maxWidthOfColumn
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class SubscriptionsSectionPanel(private val section: SubscriptionsSection) :
  SectionPanel(section.sectionName) {

  companion object {
    private val TABLE_HEADERS = arrayOf(
      "Sub Id",
      "Carrier Id",
      "Display Name",
      "MCC",
      "MNC",
      "GID1",
      "Is Embedded",
      "Profile Class"
    )

    private const val LEADING_PREFIX_PATTERN = """^\[SubscriptionInfo[a-zA-Z]*:\s*"""
    private const val LEADING_BRACKET_PATTERN = """^[\[\{]\s*"""
    private const val TRAILING_BRACKET_PATTERN = """\s*[\]\}]$"""
    private const val PROPERTY_KEY_PATTERN = """(?:^|[\s,]+)(\w+)="""

    private val LEADING_PREFIX_REGEX = LEADING_PREFIX_PATTERN.toRegex()
    private val LEADING_BRACKET_REGEX = LEADING_BRACKET_PATTERN.toRegex()
    private val TRAILING_BRACKET_REGEX = TRAILING_BRACKET_PATTERN.toRegex()
    private val PROPERTY_KEY_REGEX = PROPERTY_KEY_PATTERN.toRegex()
  }

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
      val logsTitle = JLabel(I18n.get(I18n.BUGREPORT_SUBS_SERVICE_DUMP))
      logsTitle.font = Font(Font.DIALOG, Font.BOLD, logsTitle.font.size)
      it.add(logsTitle)
    })
    container.add(SearchableTextArea(false).also {
      it.lineWrap = true
      it.isEditable = false
      it.text = section.logs
      it.caretPosition = 0
    })

    val scrollPane = JScrollPane(container).also {
      it.verticalScrollBar.unitIncrement = 16
      it.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
    }
    add(
      scrollPane,
      GBConstraintsBuilder()
        .withGridx(1)
        .withGridy(1)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )
    SwingUtilities.invokeLater {
      scrollPane.verticalScrollBar.value = 0
      scrollPane.horizontalScrollBar.value = 0
    }
  }

  private fun buildPropertiesTable(): Component {
    val data = section.subscriptionManagerProperties.map {
      arrayOf(it.key, it.value)
    }.toTypedArray()
    val propTable = JTable()
    propTable.model = PropTableModel(data, arrayOf("Property", "Value"))
    propTable.setDefaultRenderer(Object::class.java, PropTableRenderer())

    val propColWidth = propTable.maxWidthOfColumn(0)
    if (propColWidth > 0) {
      propTable.columnModel.getColumn(0).minWidth = (propColWidth * 1.5).toInt()
      propTable.columnModel.getColumn(0).maxWidth = propColWidth * 3
      propTable.columnModel.getColumn(0).preferredWidth = propColWidth * 2
    }

    propTable.tableHeader.resizingAllowed = true

    val panel = JPanel()
    panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
    panel.add(propTable.tableHeader)
    panel.add(propTable)
    return panel
  }

  private fun buildSubsListsPanel(): Component {
    val container = JPanel()
    container.layout = BoxLayout(container, BoxLayout.Y_AXIS)

    val activeSubList = JTable()
    activeSubList.model =
      PropTableModel(getDataFromSubList(section.activeSubscriptions), TABLE_HEADERS)
    activeSubList.setDefaultRenderer(Object::class.java, PropTableRenderer())
    activeSubList.tableHeader.resizingAllowed = true
    configureTableColumns(activeSubList)

    val allSubsList = JTable()
    allSubsList.model =
      PropTableModel(getDataFromSubList(section.allSubscriptions), TABLE_HEADERS)
    allSubsList.setDefaultRenderer(Object::class.java, PropTableRenderer())
    allSubsList.tableHeader.resizingAllowed = true
    configureTableColumns(allSubsList)

    container.add(JPanel(FlowLayout(FlowLayout.LEFT)).also {
      val logsTitle = JLabel(I18n.get(I18n.BUGREPORT_SUBS_ACTIVE))
      logsTitle.font = Font(Font.DIALOG, Font.BOLD, logsTitle.font.size)
      it.add(logsTitle)
    })
    container.add(activeSubList.tableHeader)
    container.add(activeSubList)
    container.add(JPanel(FlowLayout(FlowLayout.LEFT)).also {
      val logsTitle = JLabel(I18n.get(I18n.BUGREPORT_SUBS_ALL))
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

  private fun configureTableColumns(table: JTable) {
    for (i in 0 until table.columnCount) {
      val col = table.columnModel.getColumn(i)
      val headerRenderer = table.tableHeader?.defaultRenderer
      val headerComp = headerRenderer?.getTableCellRendererComponent(
        table, col.headerValue, false, false, -1, i
      )
      val headerWidth = headerComp?.preferredSize?.width ?: 60
      val cellWidth = table.maxWidthOfColumn(i)
      val targetWidth = maxOf(headerWidth + 16, cellWidth + 16, 75)
      col.preferredWidth = targetWidth
    }
  }

  private fun getDataFromSubList(subList: List<SubscriptionInfo>): Array<Array<String>> {
    return subList.map {
      arrayOf(
        it.id.toString(),
        it.carrierId.toString(),
        it.displayName,
        it.mcc.toString(),
        it.mnc.toString(),
        it.gid1,
        it.isEmbedded.toString(),
        it.profileClass.toString()
      )
    }.toTypedArray()
  }

  private fun onSubscriptionClicked(subList: JTable) {
    if (subList.selectedRow < 0) return
    val subIdStr = subList.model.getValueAt(subList.selectedRow, 0)?.toString() ?: return
    val subId = subIdStr.toIntOrNull() ?: return
    val subInfo = section.allSubscriptions.find { it.id == subId }
      ?: section.activeSubscriptions.find { it.id == subId }
      ?: return

    val formattedText = formatSubscriptionDetails(subInfo.fullText)
    val textArea = JTextArea(formattedText)
    textArea.columns = 30
    textArea.rows = 20
    textArea.lineWrap = true
    textArea.wrapStyleWord = true
    textArea.isEditable = false
    textArea.caretPosition = 0

    JOptionPane.showMessageDialog(
      subList,
      JScrollPane(textArea),
      subInfo.displayName,
      JOptionPane.PLAIN_MESSAGE
    )
  }

  private fun formatSubscriptionDetails(fullText: String): String {
    var raw = fullText.trim()
    raw = LEADING_PREFIX_REGEX.replace(raw, "")
    raw = LEADING_BRACKET_REGEX.replace(raw, "")
    raw = TRAILING_BRACKET_REGEX.replace(raw, "")

    val matches = PROPERTY_KEY_REGEX.findAll(raw).toList()
    if (matches.isEmpty()) return raw

    val sb = StringBuilder()
    for (i in matches.indices) {
      val match = matches[i]
      val key = match.groupValues[1]
      val valStart = match.range.last + 1
      val valEnd = if (i + 1 < matches.size) matches[i + 1].range.first else raw.length
      val value = raw.substring(valStart, valEnd).trim().removeSuffix(",")
      sb.append(key).append(" = ").append(value)
      if (i + 1 < matches.size) {
        sb.append("\n")
      }
    }
    return sb.toString()
  }
}
