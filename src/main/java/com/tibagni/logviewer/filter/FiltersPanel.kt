package com.tibagni.logviewer.filter

import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.View
import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.preferences.LogViewerPreferences
import com.tibagni.logviewer.util.StringUtils
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.FlatButton
import com.tibagni.logviewer.view.MultipleChoiceDialog
import com.tibagni.logviewer.view.SingleChoiceDialog
import com.tibagni.logviewer.view.Toast
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import java.awt.Frame
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*

interface FiltersPanelListener {
  fun onNavigateNextFilteredLog(filter: Filter) {}
  fun onNavigatePrevFilteredLog(filter: Filter) {}
}

interface FiltersPanelHost {
  val parentWindow: Frame
  fun showSaveFilterFileChooser(suggestedFileName: String?): File?
  fun showOpenMultipleFiltersFileChooser(): Array<File>
}

open class FiltersPanel(
  private val host: FiltersPanelHost,
  // Allows tests to inject a mock presenter
  @VisibleForTesting
  presenterFactory: ((FiltersView) -> FiltersPresenter)? = null
) : JPanel(), FiltersView, View {

  @VisibleForTesting
  internal val presenter: FiltersPresenter = presenterFactory?.invoke(this) ?: FiltersPresenterImpl(this)
  private val userPrefs: LogViewerPreferences = ServiceLocator.logViewerPrefs

  // Allow tests to fake user dialog choices without opening real Swing dialogs
  @VisibleForTesting
  internal var singleChoiceDialogProvider: (title: String, message: String, choices: Array<String>, defaultIdx: Int, parent: Component) -> Int =
    { title, msg, choices, def, parent -> SingleChoiceDialog(title, msg, choices, def).show(parent) }

  @VisibleForTesting
  internal var inputDialogProvider: (parent: Component, message: String, title: String) -> String? =
    { parent, msg, title -> JOptionPane.showInputDialog(parent, msg, title, JOptionPane.PLAIN_MESSAGE) }

  override var activeFilterContextTag: FilterContextTag?
    get() = filtersPane.activeFilterContextTag
    set(value) {
      filtersPane.activeFilterContextTag = value
      updateFiltersUI()
    }

  fun addFilterForPid(pid: Int) {
    presenter.addFilterForPid(pid)
  }

  fun addFilterForTag(tag: String) {
    presenter.addFilterForTag(tag)
  }

  fun createFilterFromText(text: String) {
    val filter = EditFilterDialog.showEditFilterDialogWithText(host.parentWindow, text) { pattern, caseSensitive ->
      presenter.findDuplicateFilter(pattern, caseSensitive, null)
    }
    if (filter != null) {
      val currentGroups = presenter.groups
      val group = showSelectFilterGroup(currentGroups)
      if (!group.isNullOrEmpty()) {
        presenter.addFilter(group, filter)
      }
    }
  }

  override fun requestFinish(doFinish: () -> Unit) {
    if (presenter.finishing()) {
      doFinish()
    }
  }

  private lateinit var addNewFilterGroupBtn: JButton
  private lateinit var moreFilterOptionsBtn: JButton
  private lateinit var collapseExpandAllGroupsBtn: JButton
  private lateinit var findFiltersBtn: JButton
  private lateinit var applyFiltersBtn: JButton
  val filtersPane: FiltersList = FiltersList()

  var listener: FiltersPanelListener? = null

  val isEmpty: Boolean get() = filtersPane.isEmpty

  init {
    buildUi()
    setupActions()
    presenter.init()
  }

  private fun buildUi() {
    layout = GridBagLayout()
    border = BorderFactory.createTitledBorder(I18n.get(I18n.FILTERS_PANEL_TITLE))

    val emptyPane = JPanel()
    emptyPane.layout = FlowLayout(FlowLayout.CENTER, 5, 5)
    add(
      emptyPane,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(2)
        .withWeightx(1.0)
        .withAnchor(GridBagConstraints.SOUTH)
        .build()
    )

    add(
      JScrollPane(filtersPane).also { it.verticalScrollBar.unitIncrement = 16 },
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(1)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )

    val filterButtonsPane = JPanel(BorderLayout())
    val filterActionButtonsPane = JPanel()
    collapseExpandAllGroupsBtn = FlatButton()
    addNewFilterGroupBtn = JButton()
    addNewFilterGroupBtn.actionCommand = "Add"
    addNewFilterGroupBtn.text = I18n.get(I18n.FILTERS_BTN_NEW_GROUP)
    moreFilterOptionsBtn = JButton()
    moreFilterOptionsBtn.toolTipText = I18n.get(I18n.FILTERS_TOOLTIP_MORE_OPTIONS)
    moreFilterOptionsBtn.text = StringUtils.THREE_LINES
    applyFiltersBtn = JButton()
    applyFiltersBtn.actionCommand = "Apply"
    applyFiltersBtn.text = I18n.get(I18n.FILTERS_BTN_APPLY)
    applyFiltersBtn.toolTipText = I18n.get(I18n.FILTERS_TOOLTIP_APPLY)
    findFiltersBtn = JButton()
    findFiltersBtn.actionCommand = "Find"
    findFiltersBtn.text = I18n.get(I18n.FILTERS_BTN_FIND)
    findFiltersBtn.toolTipText = I18n.get(I18n.FILTERS_TOOLTIP_FIND)

    filterActionButtonsPane.add(applyFiltersBtn)
    filterActionButtonsPane.add(findFiltersBtn)
    filterActionButtonsPane.add(addNewFilterGroupBtn)
    filterActionButtonsPane.add(moreFilterOptionsBtn)

    filterButtonsPane.add(collapseExpandAllGroupsBtn, BorderLayout.WEST)
    filterButtonsPane.add(filterActionButtonsPane, BorderLayout.EAST)

    add(
      filterButtonsPane,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(0)
        .withInsets(
          Insets(
            UIScaleUtils.dip(0),
            UIScaleUtils.dip(0),
            UIScaleUtils.dip(10),
            UIScaleUtils.dip(5)
          )
        )
        .withFill(GridBagConstraints.BOTH)
        .build()
    )
  }

  private fun setupActions() {
    userPrefs.addPreferenceListener(object : LogViewerPreferences.Adapter() {
      override fun onApplyFiltersOnCheckChanged() {
        applyFiltersBtn.isVisible = !userPrefs.applyFilterOnCheck
      }
    })
    applyFiltersBtn.isVisible = !userPrefs.applyFilterOnCheck

    addNewFilterGroupBtn.addActionListener { addGroup() }
    moreFilterOptionsBtn.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        showFilterOptionsMenu(e)
      }
    })
    collapseExpandAllGroupsBtn.addActionListener { filtersPane.toggleGroupsVisibility() }
    findFiltersBtn.addActionListener { showSearchFiltersDialog() }
    applyFiltersBtn.addActionListener { applyFilters() }

    setupFiltersContextActions()
  }

  private fun setupFiltersContextActions() {
    filtersPane.setFiltersListener(object : FiltersList.FiltersListener {
      override fun onReordered(group: String, orig: Int, dest: Int) {
        presenter.reorderFilters(group, orig, dest)
      }

      override fun onFiltersApplied() {
        applyFilters()
      }

      override fun onEditFilter(filter: Filter) {
        val edited = EditFilterDialog.showEditFilterDialog(host.parentWindow, filter) { pattern, caseSensitive ->
          presenter.findDuplicateFilter(pattern, caseSensitive, filter)
        }
        if (edited != null) {
          presenter.filterEdited(filter)
        }
      }

      override fun onDuplicateFilter(group: String, filter: Filter) {
        val duplicatedFilter = Filter(filter)
        duplicatedFilter.isApplied = false
        presenter.addFilter(group, duplicatedFilter, true)
      }

      override fun onDeleteFilters(group: String, indices: IntArray) {
        val userChoice = JOptionPane.showConfirmDialog(
          host.parentWindow,
          I18n.get(I18n.FILTERS_CONFIRM_DELETE_MSG),
          I18n.get(I18n.COMMON_CONFIRM_TITLE),
          JOptionPane.YES_NO_OPTION,
          JOptionPane.WARNING_MESSAGE
        )
        if (userChoice != JOptionPane.YES_OPTION) return
        presenter.removeFilters(group, indices)
      }

      override fun onMoveFilters(group: String, indices: IntArray) {
        val groups = presenter.groups
        if (groups.size <= 1) return

        val options = groups.filter { !StringUtils.areEquals(it, group) }.toTypedArray() + arrayOf(I18n.get(I18n.FILTERS_MOVE_CREATE_NEW))
        val choice = singleChoiceDialogProvider(
          I18n.format(I18n.FILTERS_MOVE_DIALOG_TITLE, indices.size),
          I18n.get(I18n.FILTERS_MOVE_DIALOG_MSG),
          options,
          0,
          host.parentWindow
        )
        if (choice == SingleChoiceDialog.DIALOG_CANCELLED) {
          return
        }

        val destGroup = if (choice == options.lastIndex) {
          addGroup(false)
        } else {
          options[choice]
        }

        if (destGroup.isNullOrEmpty()) {
          return
        }

        presenter.moveFilters(group, destGroup, indices)
      }

      override fun onCloseGroup(group: String) {
        val userChoice = JOptionPane.showConfirmDialog(
          host.parentWindow,
          I18n.get(I18n.FILTERS_CONFIRM_CLOSE_GROUP_MSG),
          I18n.get(I18n.COMMON_CONFIRM_TITLE),
          JOptionPane.YES_NO_OPTION,
          JOptionPane.WARNING_MESSAGE
        )
        if (userChoice != JOptionPane.YES_OPTION) return
        presenter.removeGroup(group)
      }

      override fun onNavigateNextFilteredLog(filter: Filter) {
        listener?.onNavigateNextFilteredLog(filter)
      }

      override fun onNavigatePrevFilteredLog(filter: Filter) {
        listener?.onNavigatePrevFilteredLog(filter)
      }

      override fun onAddFilter(group: String) {
        addFilter(group)
      }

      override fun onSaveFilters(group: String) {
        saveFilter(group)
      }

      override fun onGroupVisibilityChanged(group: String?) {
        updateCollapseExpandButtonState()
      }
    })
  }

  fun applyFilters() {
    if (filtersPane.isEmpty) {
      JOptionPane.showMessageDialog(
        host.parentWindow,
        I18n.get(I18n.FILTERS_NO_FILTERS_APPLY_MSG),
        I18n.get(I18n.FILTERS_NO_FILTERS_TITLE),
        JOptionPane.INFORMATION_MESSAGE
      )
      return
    }
    presenter.applyFilters()
  }

  open override fun updateFiltersUI() {
    filtersPane.updateActionPaneButtons()
    filtersPane.repaint()
  }

  fun updateCollapseExpandButtonState() {
    val allText = I18n.get(I18n.COMMON_ALL)
    collapseExpandAllGroupsBtn.text = if (filtersPane.hasAtLeastOneGroupVisible()) {
      "${StringUtils.DOWN_ARROW_HEAD} $allText"
    } else {
      "${StringUtils.RIGHT_ARROW_HEAD} $allText"
    }
  }

  fun addGroup(initializeFilter: Boolean = true): String? {
    var newGroupName = inputDialogProvider(
      host.parentWindow,
      I18n.get(I18n.FILTERS_NEW_GROUP_MSG),
      I18n.get(I18n.FILTERS_NEW_GROUP_TITLE)
    )

    if (!StringUtils.isEmpty(newGroupName)) {
      newGroupName = presenter.addGroup(newGroupName!!)
      if (initializeFilter && !newGroupName.isNullOrEmpty()) {
        addFilter(newGroupName)
      }
    }

    updateCollapseExpandButtonState()
    return newGroupName
  }

  fun addFilter(group: String) {
    val newFilter = EditFilterDialog.showEditFilterDialog(host.parentWindow) { pattern, caseSensitive ->
      presenter.findDuplicateFilter(pattern, caseSensitive, null)
    }
    if (newFilter != null) {
      presenter.addFilter(group, newFilter)
    }
  }

  fun saveFilter(filtersGroup: String) = presenter.saveFilters(filtersGroup)

  private fun showFilterOptionsMenu(e: MouseEvent) {
    val popup = JPopupMenu()
    val closeAllGroupsItem = popup.add(I18n.get(I18n.FILTERS_MENU_CLOSE_ALL_GROUPS))
    closeAllGroupsItem.toolTipText = I18n.get(I18n.FILTERS_TOOLTIP_CLOSE_ALL_GROUPS)

    val unApplyAllFilters = popup.add(I18n.get(I18n.FILTERS_MENU_UNAPPLY_ALL))
    unApplyAllFilters.toolTipText = I18n.get(I18n.FILTERS_TOOLTIP_UNAPPLY_ALL)

    val cleanDuplicatesItem = popup.add(I18n.get(I18n.FILTERS_MENU_CLEAN_DUPLICATES))
    cleanDuplicatesItem.toolTipText = I18n.get(I18n.FILTERS_TOOLTIP_CLEAN_DUPLICATES)

    closeAllGroupsItem.addActionListener { closeAllGroups() }
    unApplyAllFilters.addActionListener { clearAllFiltersSelection() }
    cleanDuplicatesItem.addActionListener { handleCleanDuplicateFilters() }

    closeAllGroupsItem.isEnabled = presenter.groups.isNotEmpty()
    cleanDuplicatesItem.isEnabled = presenter.groups.isNotEmpty()

    popup.show(e.component, e.x, e.y)
  }

  fun closeAllGroups() {
    val userChoice = JOptionPane.showConfirmDialog(
      host.parentWindow,
      I18n.get(I18n.FILTERS_CONFIRM_CLOSE_ALL_GROUPS_MSG),
      I18n.get(I18n.COMMON_CONFIRM_TITLE),
      JOptionPane.YES_NO_OPTION,
      JOptionPane.WARNING_MESSAGE
    )
    if (userChoice == JOptionPane.YES_NO_OPTION) {
      val openGroups = presenter.groups
      openGroups.forEach { presenter.removeGroup(it) }
    }
  }

  fun clearAllFiltersSelection() {
    presenter.setAllFiltersApplied(false)
  }

  fun handleCleanDuplicateFilters() {
    presenter.cleanDuplicateFilters()
  }

  fun handleOpenFiltersMenu() {
    val filterFiles = host.showOpenMultipleFiltersFileChooser()
    if (filterFiles.isNotEmpty()) {
      var keepCurrentFilters = false
      if (!filtersPane.isEmpty) {
        val dialog = SingleChoiceDialog(
          I18n.get(I18n.FILTERS_OPEN_EXISTING_TITLE),
          I18n.get(I18n.FILTERS_OPEN_EXISTING_MSG),
          arrayOf(
            I18n.get(I18n.FILTERS_OPEN_KEEP_EXISTING),
            I18n.get(I18n.FILTERS_OPEN_REPLACE_EXISTING)
          ),
          0
        )

        val choice = dialog.show(host.parentWindow)
        if (choice == SingleChoiceDialog.DIALOG_CANCELLED) {
          return
        }
        keepCurrentFilters = choice == 0
      }

      presenter.loadFilters(filterFiles, keepCurrentFilters)
    }
  }

  fun showSearchFiltersDialog() {
    if (filtersPane.isEmpty) {
      JOptionPane.showMessageDialog(
        host.parentWindow,
        I18n.get(I18n.FILTERS_NO_FILTERS_SEARCH_MSG),
        I18n.get(I18n.FILTERS_NO_FILTERS_TITLE),
        JOptionPane.INFORMATION_MESSAGE
      )
      return
    }

    SearchFiltersDialog.show(
      host.parentWindow,
      presenter,
      ServiceLocator.filtersRepository
    ) { group, filter ->
      filtersPane.selectFilter(group, filter)
    }
    filtersPane.updateUI()
  }

  override fun configureFiltersList(filters: Map<String, List<Filter>>?) {
    filtersPane.setFilters(HashMap(filters))
    updateCollapseExpandButtonState()
  }

  override fun showUnsavedFilterIndication(group: String?) {
    if (group != null) {
      filtersPane.showUnsavedIndication(group, true)
    }
  }

  override fun hideUnsavedFilterIndication(group: String?) {
    if (group != null) {
      filtersPane.showUnsavedIndication(group, false)
    }
  }

  override fun collapseAllGroups() {
    filtersPane.setAllGroupsVisibility(false)
    updateCollapseExpandButtonState()
  }

  override fun showDuplicateFilterWarning(pattern: String, group: String): Boolean {
    val message = I18n.format(I18n.FILTER_DUPLICATE_WARNING_MSG, pattern, group)
    val title = I18n.get(I18n.FILTER_DUPLICATE_WARNING_TITLE)
    val result = JOptionPane.showConfirmDialog(
      host.parentWindow,
      message,
      title,
      JOptionPane.YES_NO_OPTION,
      JOptionPane.WARNING_MESSAGE
    )
    return result == JOptionPane.YES_OPTION
  }

  override fun showDuplicateFiltersDialog(clusters: List<DuplicateCluster>): Map<String, List<Filter>>? {
    return FilterDeduplicationDialog.showDialog(host.parentWindow, clusters)
  }

  override fun showSelectFilterGroup(groups: List<String>): String? {
    val options = groups.toTypedArray() + arrayOf(I18n.get(I18n.FILTERS_MOVE_CREATE_NEW))
    val createNewOptionIdx = options.size - 1

    val choice = singleChoiceDialogProvider(
      I18n.get(I18n.LOGS_SELECT_FILTER_GROUP_TITLE),
      I18n.get(I18n.LOGS_SELECT_FILTER_GROUP_MSG),
      options,
      createNewOptionIdx,
      host.parentWindow
    )
    return when (choice) {
      SingleChoiceDialog.DIALOG_CANCELLED -> null
      createNewOptionIdx -> addGroup(false)
      else -> options[choice]
    }
  }

  override fun showAskToSaveFilterDialog(group: String?): FiltersPresenter.UserSelection {
    val userChoice = JOptionPane.showConfirmDialog(
      host.parentWindow,
      I18n.format(I18n.FILTERS_SAVE_UNSAVED_MSG, group),
      I18n.get(I18n.FILTERS_SAVE_UNSAVED_TITLE),
      JOptionPane.YES_NO_CANCEL_OPTION,
      JOptionPane.WARNING_MESSAGE
    )
    return when (userChoice) {
      JOptionPane.YES_OPTION -> FiltersPresenter.UserSelection.CONFIRMED
      JOptionPane.NO_OPTION -> FiltersPresenter.UserSelection.REJECTED
      else -> FiltersPresenter.UserSelection.CANCELLED
    }
  }

  override fun showAskToSaveMultipleFiltersDialog(unsavedGroups: Array<String>): Array<Boolean>? {
    val dialog = MultipleChoiceDialog(
      I18n.get(I18n.FILTERS_SAVE_MULTIPLE_TITLE),
      I18n.get(I18n.FILTERS_SAVE_MULTIPLE_MSG),
      unsavedGroups,
      true
    )
    return dialog.show(host.parentWindow)
  }

  override fun showSaveFilters(group: String?): File? = host.showSaveFilterFileChooser(group)

  override fun showInfoMessage(title: String, message: String) {
    JOptionPane.showMessageDialog(host.parentWindow, message, title, JOptionPane.INFORMATION_MESSAGE)
  }

  override fun showErrorMessage(message: String?) {
    JOptionPane.showMessageDialog(host.parentWindow, message, I18n.get(I18n.COMMON_ERROR), JOptionPane.ERROR_MESSAGE)
  }

  override fun showToast(message: String) {
    Toast.showToast(host.parentWindow, message, Toast.LENGTH_SHORT)
  }

  override fun onAppliedFiltersRemembered() {
    updateFiltersUI()
  }

  fun onThemeChanged() {
    SwingUtilities.updateComponentTreeUI(filtersPane)
    collapseExpandAllGroupsBtn.updateUI()
    updateUI()
  }
}
