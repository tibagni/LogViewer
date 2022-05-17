package com.tibagni.logviewer

import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.ButtonsPane
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*

class AddIgnoredKeywordsDialog(owner: JFrame?, keyword: String?, model: DefaultListModel<String>?, index: Int) :
  JDialog(owner), ButtonsPane.Listener {
  private val buttonsPane = ButtonsPane(ButtonsPane.ButtonsMode.OK_CANCEL, this)
  private val contentPane = JPanel()
  private val ignoredKeywordTextArea = JTextArea(keyword)
  private val ignoredKeywordsModel = model
  private val modelIndex = index

  companion object {
    fun showAddIgnoredKeywordsDialog(parent: JFrame?, keyword: String?, model: DefaultListModel<String>?, index: Int) {
      val dialog = AddIgnoredKeywordsDialog(parent, keyword, model, index)
      dialog.pack()
      dialog.setLocationRelativeTo(parent)
      dialog.isVisible = true
    }
  }

  init {
    title = "Ignored Keywords"
    modalityType = ModalityType.APPLICATION_MODAL
    buildUi()
    setContentPane(contentPane)
    buttonsPane.setDefaultButtonOk()
    buttonsPane.setDefaultButtonCancel()
    defaultCloseOperation = DO_NOTHING_ON_CLOSE
    addWindowListener(object : WindowAdapter() {
      override fun windowClosing(e: WindowEvent) {
        onCancel()
      }
    })
  }

  private fun buildUi() {
    contentPane.layout = GridBagLayout()
    contentPane.border = BorderFactory.createEmptyBorder(
      UIScaleUtils.dip(10), UIScaleUtils.dip(10),
      UIScaleUtils.dip(10),
      UIScaleUtils.dip(10)
    )
    contentPane.add(JLabel("Put the keyword you want to ignore here"),
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(0)
        .withInsets(UIScaleUtils.scaleInsets(Insets(5, 0, 5, 0)))
        .withFill(GridBagConstraints.BOTH)
        .build()
    )

    ignoredKeywordTextArea.lineWrap = true
    ignoredKeywordTextArea.wrapStyleWord = true
    val ignoredKeywordScrollPane = JScrollPane(ignoredKeywordTextArea)
    ignoredKeywordScrollPane.preferredSize = Dimension(312, 60)
    contentPane.add(ignoredKeywordScrollPane,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(1)
        .withFill(GridBagConstraints.VERTICAL)
        .build()
    )
    contentPane.add(buttonsPane,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(2)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )
  }

  override fun onOk() {
    if (modelIndex == -1) {
      ignoredKeywordsModel?.addElement(ignoredKeywordTextArea.text)
    } else {
      ignoredKeywordsModel?.set(modelIndex, ignoredKeywordTextArea.text)
    }
    dispose()
  }

  override fun onCancel() {
    dispose()
  }
}