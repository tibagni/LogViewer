package com.tibagni.logviewer.log

import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.CommonUtils
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.ButtonsPane
import com.tibagni.logviewer.view.FlatButton
import java.awt.*
import java.awt.event.KeyEvent
import java.io.File
import javax.swing.*
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder

class LogLineDetailsDialog(
  owner: Frame?,
  val info: LogLineInfo,
  private val onFilterByPid: ((Int) -> Unit)? = null,
  private val onFilterByTag: ((String) -> Unit)? = null,
  private val onOpenInEditor: ((File, Int) -> Unit)? = null
) : JDialog(owner, I18n.get(I18n.LOG_DETAILS_TITLE), true), ButtonsPane.Listener {

  private val buttonsPane = ButtonsPane(ButtonsPane.ButtonsMode.OK_ONLY, this)

  init {
    buildUi()
  }

  private fun buildUi() {
    val mainPanel = JPanel(BorderLayout(0, UIScaleUtils.dip(10))).apply {
      border = EmptyBorder(UIScaleUtils.dip(10), UIScaleUtils.dip(12), UIScaleUtils.dip(10), UIScaleUtils.dip(12))
    }

    val topPanel = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      add(createSourceSection())
      add(Box.createVerticalStrut(UIScaleUtils.dip(8)))
      add(createMetadataSection())
    }

    mainPanel.add(topPanel, BorderLayout.NORTH)
    mainPanel.add(createMessageSection(), BorderLayout.CENTER)
    mainPanel.add(buttonsPane, BorderLayout.SOUTH)

    contentPane = mainPanel
    buttonsPane.setDefaultButtonOk()

    // Close on ESC
    rootPane.registerKeyboardAction(
      { dispose() },
      KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
      JComponent.WHEN_IN_FOCUSED_WINDOW
    )

    isResizable = true
    preferredSize = Dimension(UIScaleUtils.dip(680), UIScaleUtils.dip(560))
    minimumSize = Dimension(UIScaleUtils.dip(500), UIScaleUtils.dip(420))
    pack()
    setLocationRelativeTo(owner)
  }

  private fun createSourceSection(): JPanel {
    val panel = JPanel(GridBagLayout()).apply {
      border = CompoundBorder(
        TitledBorder(I18n.get(I18n.LOG_DETAILS_SOURCE_SECTION)),
        EmptyBorder(UIScaleUtils.dip(6), UIScaleUtils.dip(8), UIScaleUtils.dip(6), UIScaleUtils.dip(8))
      )
    }

    val gbc = GridBagConstraints().apply {
      fill = GridBagConstraints.HORIZONTAL
      insets = Insets(UIScaleUtils.dip(2), UIScaleUtils.dip(4), UIScaleUtils.dip(2), UIScaleUtils.dip(4))
    }

    // Row 0: File Name + Action buttons
    gbc.gridx = 0
    gbc.gridy = 0
    gbc.weightx = 0.0
    panel.add(JLabel(I18n.get(I18n.LOG_DETAILS_FILE_NAME)), gbc)

    gbc.gridx = 1
    gbc.weightx = 1.0
    val fileNameLabel = JLabel(info.fileName).apply {
      font = font.deriveFont(Font.BOLD)
    }
    panel.add(fileNameLabel, gbc)

    val actionsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, UIScaleUtils.dip(4), 0)).apply {
      isOpaque = false
    }

    val copyPathBtn = FlatButton(I18n.get(I18n.LOG_DETAILS_COPY_PATH)).apply {
      isEnabled = info.sourceFile != null
      addActionListener {
        info.sourceFile?.let { CommonUtils.copyToClipboard(it.absolutePath) }
      }
    }
    actionsPanel.add(copyPathBtn)

    val openEditorBtn = FlatButton(I18n.get(I18n.LOG_DETAILS_OPEN_EDITOR)).apply {
      isEnabled = info.hasFile
      addActionListener {
        info.sourceFile?.let { onOpenInEditor?.invoke(it, info.lineNumber) }
      }
    }
    actionsPanel.add(openEditorBtn)

    gbc.gridx = 2
    gbc.weightx = 0.0
    panel.add(actionsPanel, gbc)

    // Row 1: File Path
    gbc.gridx = 0
    gbc.gridy = 1
    gbc.weightx = 0.0
    panel.add(JLabel(I18n.get(I18n.LOG_DETAILS_FILE_PATH)), gbc)

    gbc.gridx = 1
    gbc.gridwidth = 2
    gbc.weightx = 1.0
    val pathField = JTextField(info.filePath).apply {
      isEditable = false
      border = null
      isOpaque = false
    }
    panel.add(pathField, gbc)

    // Row 2: Line in File & Stream
    gbc.gridwidth = 1
    gbc.gridx = 0
    gbc.gridy = 2
    gbc.weightx = 0.0
    panel.add(JLabel(I18n.get(I18n.LOG_DETAILS_LINE_NUMBER)), gbc)

    gbc.gridx = 1
    gbc.weightx = 0.5
    panel.add(JLabel(info.formattedLineNumber), gbc)

    gbc.gridx = 2
    gbc.weightx = 0.5
    val streamBox = JPanel(FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(4), 0)).apply {
      isOpaque = false
      add(JLabel(I18n.get(I18n.LOG_DETAILS_STREAM)))
      add(JLabel(info.logStream.name).apply {
        font = font.deriveFont(Font.BOLD)
      })
    }
    panel.add(streamBox, gbc)

    return panel
  }

  private fun createMetadataSection(): JPanel {
    val panel = JPanel(GridBagLayout()).apply {
      border = CompoundBorder(
        TitledBorder(I18n.get(I18n.LOG_DETAILS_METADATA_SECTION)),
        EmptyBorder(UIScaleUtils.dip(6), UIScaleUtils.dip(8), UIScaleUtils.dip(6), UIScaleUtils.dip(8))
      )
    }

    val gbc = GridBagConstraints().apply {
      fill = GridBagConstraints.HORIZONTAL
      insets = Insets(UIScaleUtils.dip(2), UIScaleUtils.dip(4), UIScaleUtils.dip(2), UIScaleUtils.dip(4))
    }

    // Row 0: Timestamp & Table Index
    gbc.gridx = 0
    gbc.gridy = 0
    gbc.weightx = 0.0
    panel.add(JLabel(I18n.get(I18n.LOG_DETAILS_TIMESTAMP)), gbc)

    gbc.gridx = 1
    gbc.weightx = 0.5
    panel.add(JLabel(info.formattedTimestamp), gbc)

    gbc.gridx = 2
    gbc.weightx = 0.0
    panel.add(JLabel(I18n.get(I18n.LOG_DETAILS_LOG_INDEX)), gbc)

    gbc.gridx = 3
    gbc.weightx = 0.5
    panel.add(JLabel("#${info.formattedIndex}"), gbc) // i18n:ignore

    // Row 1: Level & Filter
    gbc.gridx = 0
    gbc.gridy = 1
    gbc.weightx = 0.0
    panel.add(JLabel(I18n.get(I18n.LOG_DETAILS_LEVEL)), gbc)

    gbc.gridx = 1
    gbc.weightx = 0.5
    val levelPanel = JPanel(FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(4), 0)).apply {
      isOpaque = false
      val indicator = JPanel().apply {
        preferredSize = Dimension(UIScaleUtils.dip(12), UIScaleUtils.dip(12))
        background = LogCellRenderer.getColorForLogLevel(info.logLevel)
      }
      add(indicator)
      add(JLabel(info.logLevel.name))
    }
    panel.add(levelPanel, gbc)

    gbc.gridx = 2
    gbc.weightx = 0.0
    panel.add(JLabel(I18n.get(I18n.LOG_DETAILS_FILTER)), gbc)

    gbc.gridx = 3
    gbc.weightx = 0.5
    val filterPanel = JPanel(FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(4), 0)).apply {
      isOpaque = false
      if (info.appliedFilter != null) {
        val filterColorSquare = JPanel().apply {
          preferredSize = Dimension(UIScaleUtils.dip(12), UIScaleUtils.dip(12))
          background = info.appliedFilter.color
        }
        add(filterColorSquare)
        add(JLabel(info.appliedFilter.name))
      } else {
        add(JLabel("-"))
      }
    }
    panel.add(filterPanel, gbc)

    // Row 2: PID & TID
    gbc.gridx = 0
    gbc.gridy = 2
    gbc.weightx = 0.0
    panel.add(JLabel(I18n.get(I18n.LOG_DETAILS_PID)), gbc)

    gbc.gridx = 1
    gbc.weightx = 0.5
    val pidPanel = JPanel(FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(4), 0)).apply {
      isOpaque = false
      add(JLabel(info.formattedPid))
      if (info.pid != null && info.pid > 0) {
        val filterPidBtn = FlatButton(I18n.get(I18n.LOG_DETAILS_BTN_FILTER_PID)).apply {
          addActionListener {
            onFilterByPid?.invoke(info.pid)
            dispose()
          }
        }
        add(filterPidBtn)
      }
    }
    panel.add(pidPanel, gbc)

    gbc.gridx = 2
    gbc.weightx = 0.0
    panel.add(JLabel(I18n.get(I18n.LOG_DETAILS_TID)), gbc)

    gbc.gridx = 3
    gbc.weightx = 0.5
    panel.add(JLabel(info.formattedTid), gbc)

    // Row 3: Tag
    gbc.gridx = 0
    gbc.gridy = 3
    gbc.weightx = 0.0
    panel.add(JLabel(I18n.get(I18n.LOG_DETAILS_TAG)), gbc)

    gbc.gridx = 1
    gbc.gridwidth = 3
    gbc.weightx = 1.0
    val tagPanel = JPanel(FlowLayout(FlowLayout.LEFT, UIScaleUtils.dip(4), 0)).apply {
      isOpaque = false
      add(JLabel(info.formattedTag).apply {
        font = font.deriveFont(Font.BOLD)
      })
      if (!info.tag.isNullOrBlank()) {
        val filterTagBtn = FlatButton(I18n.get(I18n.LOG_DETAILS_BTN_FILTER_TAG)).apply {
          addActionListener {
            onFilterByTag?.invoke(info.tag)
            dispose()
          }
        }
        add(filterTagBtn)

        val copyTagBtn = FlatButton(I18n.get(I18n.LOG_DETAILS_COPY_TAG)).apply {
          addActionListener {
            CommonUtils.copyToClipboard(info.tag)
          }
        }
        add(copyTagBtn)
      }
    }
    panel.add(tagPanel, gbc)

    return panel
  }

  private fun createMessageSection(): JPanel {
    val panel = JPanel(BorderLayout(0, UIScaleUtils.dip(4))).apply {
      border = CompoundBorder(
        TitledBorder(I18n.get(I18n.LOG_DETAILS_MESSAGE_SECTION)),
        EmptyBorder(UIScaleUtils.dip(6), UIScaleUtils.dip(8), UIScaleUtils.dip(6), UIScaleUtils.dip(8))
      )
    }

    val toolbar = JPanel(FlowLayout(FlowLayout.RIGHT, UIScaleUtils.dip(4), 0)).apply {
      isOpaque = false
    }

    val copyLineBtn = FlatButton(I18n.get(I18n.LOG_DETAILS_COPY_LINE)).apply {
      addActionListener {
        CommonUtils.copyToClipboard(info.entry.logText)
      }
    }
    toolbar.add(copyLineBtn)

    val copyMsgBtn = FlatButton(I18n.get(I18n.LOG_DETAILS_COPY_MESSAGE)).apply {
      addActionListener {
        CommonUtils.copyToClipboard(info.message)
      }
    }
    toolbar.add(copyMsgBtn)

    panel.add(toolbar, BorderLayout.NORTH)

    val textArea = JTextArea(info.message).apply {
      isEditable = false
      lineWrap = true
      wrapStyleWord = true
      font = Font("Monospaced", Font.PLAIN, ServiceLocator.logViewerPrefs.logFontSize)
      caretPosition = 0
    }

    val scrollPane = JScrollPane(textArea).apply {
      preferredSize = Dimension(UIScaleUtils.dip(640), UIScaleUtils.dip(160))
    }
    panel.add(scrollPane, BorderLayout.CENTER)

    return panel
  }

  override fun onOk() {
    dispose()
  }

  override fun onCancel() {
    dispose()
  }
}
