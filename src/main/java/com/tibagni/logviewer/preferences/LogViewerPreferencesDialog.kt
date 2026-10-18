package com.tibagni.logviewer.preferences

import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.theme.LogViewerThemeManager
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.ButtonsPane
import com.tibagni.logviewer.view.LoadingSpinner
import com.tibagni.logviewer.view.PaddingListCellRenderer
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagLayout
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSeparator
import javax.swing.JSplitPane
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel
import javax.swing.ScrollPaneConstants
import javax.swing.Scrollable
import javax.swing.Timer

class LogViewerPreferencesDialog(parent: JFrame?) : JDialog(parent), ButtonsPane.Listener {

  private lateinit var mainPanel: JPanel
  private lateinit var buttonsPane: ButtonsPane
  private lateinit var categoriesList: JList<PreferencePanel>
  private lateinit var categoriesListModel: DefaultListModel<PreferencePanel>
  private lateinit var cardsPanel: JPanel
  private val cardLayout = CardLayout()
  private var isSaving = false

  private val userPrefs: LogViewerPreferences = ServiceLocator.logViewerPrefs
  private val themeManager: LogViewerThemeManager = ServiceLocator.themeManager

  internal val preferencePanels: List<PreferencePanel> by lazy {
    listOf(
      AppearancePreferencePanel(userPrefs, themeManager),
      LogsPreferencePanel(this, userPrefs),
      FiltersPreferencePanel(this, userPrefs),
      ToolsPreferencePanel(this, userPrefs)
    )
  }

  init {
    title = I18n.get(I18n.PREF_DIALOG_TITLE)
    buildUi()
    contentPane = mainPanel
    isModal = true
    buttonsPane.setDefaultButtonOk()

    // Adjust size according to content
    val prefSize = mainPanel.preferredSize
    prefSize.width = maxOf(prefSize.width, UIScaleUtils.dip(700))
    prefSize.height = maxOf(prefSize.height, UIScaleUtils.dip(480))
    mainPanel.preferredSize = prefSize
    mainPanel.validate()

    // Call onCancel() when cross is clicked
    defaultCloseOperation = DO_NOTHING_ON_CLOSE
    addWindowListener(object : WindowAdapter() {
      override fun windowClosing(e: WindowEvent) {
        onCancel()
      }
    })

    // Call onCancel() on ESCAPE
    mainPanel.registerKeyboardAction(
      { onCancel() },
      KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
      JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
    )
  }

  private fun buildUi() {
    mainPanel = JPanel(BorderLayout()).apply {
      border = BorderFactory.createEmptyBorder(
        UIScaleUtils.dip(10),
        UIScaleUtils.dip(10),
        UIScaleUtils.dip(10),
        UIScaleUtils.dip(10)
      )
    }

    // Categories list setup
    categoriesListModel = DefaultListModel<PreferencePanel>().apply {
      preferencePanels.forEach { addElement(it) }
    }
    categoriesList = JList(categoriesListModel).apply {
      cellRenderer = object : PaddingListCellRenderer(
        UIScaleUtils.dip(8),
        UIScaleUtils.dip(14),
        UIScaleUtils.dip(8),
        UIScaleUtils.dip(14)
      ) {
        override fun getListCellRendererComponent(
          list: JList<*>?,
          value: Any?,
          index: Int,
          isSelected: Boolean,
          cellHasFocus: Boolean
        ): Component {
          val c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
          if (value is PreferencePanel) {
            text = value.title
          }
          return c
        }
      }
      selectionMode = ListSelectionModel.SINGLE_SELECTION
      selectedIndex = 0
    }

    val sidebarScrollPane = JScrollPane(categoriesList).apply {
      border = BorderFactory.createEmptyBorder()
      horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
      viewport.background = categoriesList.background
    }

    // Cards panel setup
    cardsPanel = JPanel(cardLayout)
    preferencePanels.forEach { panel ->
      cardsPanel.add(
        createCardPanel(panel.title, panel.panel),
        panel.id
      )
    }
    cardsPanel.add(createSavingCard(), SAVING_CARD_ID)

    categoriesList.addListSelectionListener { e ->
      if (e.valueIsAdjusting || isSaving) return@addListSelectionListener
      val selected = categoriesList.selectedValue ?: return@addListSelectionListener
      cardLayout.show(cardsPanel, selected.id)
    }

    // Split pane holding category navigation on left and settings cards on right
    val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebarScrollPane, cardsPanel).apply {
      dividerSize = UIScaleUtils.dip(1)
      dividerLocation = UIScaleUtils.dip(170)
      isContinuousLayout = true
      border = BorderFactory.createEtchedBorder()
    }

    // Bottom action buttons (OK / Cancel)
    buttonsPane = ButtonsPane(ButtonsPane.ButtonsMode.OK_CANCEL, this).apply {
      border = BorderFactory.createEmptyBorder(UIScaleUtils.dip(10), 0, 0, 0)
    }

    mainPanel.add(splitPane, BorderLayout.CENTER)
    mainPanel.add(buttonsPane, BorderLayout.SOUTH)
  }

  private fun createCardPanel(title: String, formContent: JComponent): JPanel {
    val card = JPanel(BorderLayout())

    // Category Header
    val headerPanel = JPanel(BorderLayout()).apply {
      val titleLabel = JLabel(title).apply {
        font = font.deriveFont(Font.BOLD, font.size2D + UIScaleUtils.dip(2))
        border = BorderFactory.createEmptyBorder(
          UIScaleUtils.dip(12),
          UIScaleUtils.dip(16),
          UIScaleUtils.dip(8),
          UIScaleUtils.dip(16)
        )
      }
      add(titleLabel, BorderLayout.NORTH)
      add(JSeparator(), BorderLayout.SOUTH)
    }
    card.add(headerPanel, BorderLayout.NORTH)

    // Form content wrapper implementing Scrollable to track viewport width
    val contentWrapper = object : JPanel(BorderLayout()), Scrollable {
      override fun getPreferredScrollableViewportSize(): Dimension = preferredSize
      override fun getScrollableUnitIncrement(r: Rectangle?, o: Int, d: Int): Int = UIScaleUtils.dip(16)
      override fun getScrollableBlockIncrement(r: Rectangle?, o: Int, d: Int): Int = UIScaleUtils.dip(32)
      override fun getScrollableTracksViewportWidth(): Boolean = true
      override fun getScrollableTracksViewportHeight(): Boolean = false
    }.apply {
      border = BorderFactory.createEmptyBorder(
        UIScaleUtils.dip(12),
        UIScaleUtils.dip(16),
        UIScaleUtils.dip(12),
        UIScaleUtils.dip(16)
      )
      add(formContent, BorderLayout.NORTH)
    }

    val scrollPane = JScrollPane(contentWrapper).apply {
      border = BorderFactory.createEmptyBorder()
      horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
      verticalScrollBar.unitIncrement = UIScaleUtils.dip(16)
    }

    card.add(scrollPane, BorderLayout.CENTER)
    return card
  }

  private fun createSavingCard(): JPanel {
    return JPanel(GridBagLayout()).apply {
      val content = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false

        val spinner = LoadingSpinner().apply {
          alignmentX = Component.CENTER_ALIGNMENT
        }
        val label = JLabel(I18n.get(I18n.PREF_SAVING_CHANGES)).apply {
          alignmentX = Component.CENTER_ALIGNMENT
          border = BorderFactory.createEmptyBorder(UIScaleUtils.dip(12), 0, 0, 0)
        }
        add(spinner)
        add(label)
      }
      add(content)
    }
  }

  override fun onOk() {
    if (isSaving) return
    val isTimeConsuming = preferencePanels.any { it.hasTimeConsumingOperation }

    if (isTimeConsuming) {
      isSaving = true
      cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
      categoriesList.isEnabled = false
      buttonsPane.enableOkButton(false)
      buttonsPane.enableCancelButton(false)
      cardLayout.show(cardsPanel, SAVING_CARD_ID)

      val timer = Timer(20) {
        try {
          preferencePanels.forEach { it.apply() }
        } finally {
          cursor = Cursor.getDefaultCursor()
          dispose()
        }
      }
      timer.isRepeats = false
      timer.start()
    } else {
      preferencePanels.forEach { it.apply() }
      dispose()
    }
  }

  override fun onCancel() {
    if (isSaving) return
    dispose()
  }

  companion object {
    private const val SAVING_CARD_ID = "pref.saving"

    @JvmStatic
    fun showPreferencesDialog(parent: JFrame?) {
      val dialog = LogViewerPreferencesDialog(parent)
      dialog.pack()
      dialog.setLocationRelativeTo(parent)
      dialog.isVisible = true
    }
  }
}
