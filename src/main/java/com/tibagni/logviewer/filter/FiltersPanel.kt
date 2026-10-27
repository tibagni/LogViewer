package com.tibagni.logviewer.filter

import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.ui.FlatLineBorder
import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.View
import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.preferences.LogViewerPreferences
import com.tibagni.logviewer.util.StringUtils
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.CardBorderUtils
import com.tibagni.logviewer.view.CommonIcons
import com.tibagni.logviewer.view.FlatButton
import com.tibagni.logviewer.view.MultipleChoiceDialog
import com.tibagni.logviewer.view.SingleChoiceDialog
import com.tibagni.logviewer.view.Toast
import com.tibagni.logviewer.view.ToolbarDivider
import java.awt.BorderLayout
import java.awt.Color
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

open class FiltersPanel @VisibleForTesting internal constructor(
  private val host: FiltersPanelHost,
  presenterFactory: ((FiltersView) -> FiltersPresenter)?,
  @VisibleForTesting
  internal val userPrefs: LogViewerPreferences
) : JPanel(), FiltersView, View {

  constructor(
    host: FiltersPanelHost,
    presenterFactory: ((FiltersView) -> FiltersPresenter)? = null
  ) : this(host, presenterFactory, ServiceLocator.logViewerPrefs)

  @VisibleForTesting
  internal val presenter: FiltersPresenter =
    presenterFactory?.invoke(this) ?: FiltersPresenterImpl(this)

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

  @VisibleForTesting
  internal lateinit var addNewFilterGroupBtn: FlatButton
  @VisibleForTesting
  internal lateinit var moreFilterOptionsBtn: FlatButton
  @VisibleForTesting
  internal lateinit var collapseExpandAllGroupsBtn: FlatButton
  @VisibleForTesting
  internal lateinit var findFiltersBtn: FlatButton
  @VisibleForTesting
  internal lateinit var applyFiltersBtn: FlatButton
  @VisibleForTesting
  internal lateinit var applyDivider: ToolbarDivider
  @VisibleForTesting
  internal lateinit var filterActionButtonsPane: JPanel
  @VisibleForTesting
  internal lateinit var scrollPane: JScrollPane
  val filtersPane: FiltersList = FiltersList()

  var listener: FiltersPanelListener? = null

  val isEmpty: Boolean get() = filtersPane.isEmpty

  init {
    buildUi()
    setupActions()
    presenter.init()
  }

  /**
   * Resolves the border color for the main filters panel titled border.
   */
  fun resolvePanelBorderColor(): Color =
    CardBorderUtils.resolveCardBorderColor()

  /**
   * Creates the titled panel border styled with a subtle rounded card border.
   */
  private fun createPanelBorder(): javax.swing.border.Border {
    val pad = UIScaleUtils.dip(4)
    return CardBorderUtils.createTitledCardBorder(
      title = I18n.get(I18n.FILTERS_PANEL_TITLE),
      insets = Insets(pad, pad, pad, pad)
    )
  }

  private fun buildUi() {
    layout = GridBagLayout()
    border = createPanelBorder()

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

    scrollPane = JScrollPane(filtersPane).apply {
      verticalScrollBar.unitIncrement = 16
      border = BorderFactory.createEmptyBorder()
      viewportBorder = null
      isOpaque = false
      viewport.isOpaque = false
    }
    add(
      scrollPane,
      GBConstraintsBuilder()
        .withGridx(0)
        .withGridy(1)
        .withWeightx(1.0)
        .withWeighty(1.0)
        .withFill(GridBagConstraints.BOTH)
        .build()
    )

    val filterButtonsPane = JPanel(BorderLayout())
    filterActionButtonsPane = JPanel(
      FlowLayout(FlowLayout.RIGHT, UIScaleUtils.dip(4), 0)
    )
    collapseExpandAllGroupsBtn = FlatButton()
    updateCollapseExpandButtonState()
    addNewFilterGroupBtn = FlatButton().apply {
      actionCommand = "Add"
      text = I18n.get(I18n.FILTERS_BTN_NEW_GROUP)
      icon = CommonIcons.plusIcon(14)
      iconTextGap = UIScaleUtils.dip(4)
    }
    moreFilterOptionsBtn = FlatButton().apply {
      toolTipText = I18n.get(
        I18n.FILTERS_TOOLTIP_MORE_OPTIONS
      )
      text = StringUtils.THREE_LINES
    }
    applyFiltersBtn = FlatButton(I18n.get(I18n.FILTERS_BTN_APPLY)).apply {
      actionCommand = "Apply"
      toolTipText = I18n.get(I18n.FILTERS_TOOLTIP_APPLY)
    }
    applyDivider = ToolbarDivider()
    findFiltersBtn = FlatButton().apply {
      actionCommand = "Find"
      text = I18n.get(I18n.FILTERS_BTN_FIND)
      toolTipText = I18n.get(I18n.FILTERS_TOOLTIP_FIND)
    }

    filterActionButtonsPane.add(applyFiltersBtn)
    filterActionButtonsPane.add(applyDivider)
    filterActionButtonsPane.add(findFiltersBtn)
    filterActionButtonsPane.add(ToolbarDivider())
    filterActionButtonsPane.add(addNewFilterGroupBtn)
    filterActionButtonsPane.add(ToolbarDivider())
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
            UIScaleUtils.dip(6),
            UIScaleUtils.dip(2)
          )
        )
        .withFill(GridBagConstraints.BOTH)
        .build()
    )
  }

  private fun setupActions() {
    userPrefs.addPreferenceListener(object : LogViewerPreferences.Adapter() {
      override fun onReapplyFiltersConfigChanged() {
        updateApplyFiltersButtonVisibility()
      }

      override fun onApplyFiltersOnCheckChanged() {
        updateApplyFiltersButtonVisibility()
      }
    })
    updateApplyFiltersButtonVisibility()

    addNewFilterGroupBtn.addActionListener { addGroup() }
    moreFilterOptionsBtn.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        showFilterOptionsMenu(e)
      }
    })
    collapseExpandAllGroupsBtn.addActionListener {
      filtersPane.toggleGroupsVisibility()
    }
    findFiltersBtn.addActionListener { showSearchFiltersDialog() }
    applyFiltersBtn.addActionListener { applyFilters() }

    setupFiltersContextActions()
  }

  /**
   * Updates the visibility of the Apply Filters button and its adjacent
   * divider based on user preferences, triggering container layout refresh.
   */
  private fun updateApplyFiltersButtonVisibility() {
    val shouldShow = !userPrefs.reapplyFiltersAfterEdit ||
      !userPrefs.applyFilterOnCheck
    applyFiltersBtn.isVisible = shouldShow
    applyDivider.isVisible = shouldShow
    filterActionButtonsPane.revalidate()
    filterActionButtonsPane.repaint()
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
    val hasVisible = filtersPane.hasAtLeastOneGroupVisible()
    collapseExpandAllGroupsBtn.icon = if (hasVisible) {
      CommonIcons.chevronDownIcon(14)
    } else {
      CommonIcons.chevronRightIcon(14)
    }
    collapseExpandAllGroupsBtn.text = allText
    collapseExpandAllGroupsBtn.iconTextGap = UIScaleUtils.dip(4)
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
    border = createPanelBorder()
    updateCollapseExpandButtonState()
    collapseExpandAllGroupsBtn.updateUI()
    addNewFilterGroupBtn.updateUI()
    moreFilterOptionsBtn.updateUI()
    findFiltersBtn.updateUI()
    applyFiltersBtn.updateUI()
    updateUI()
  }
}
