package com.tibagni.logviewer.preferences

import com.jgoodies.forms.layout.CellConstraints
import com.jgoodies.forms.layout.FormLayout
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.JFileChooserExt
import java.awt.BorderLayout
import java.awt.Component
import java.io.File
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel

class FiltersPreferencePanel(
  private val parent: Component,
  private val userPrefs: LogViewerPreferences
) : PreferencePanel {

  override val id = ID
  override val title: String get() = I18n.get(I18n.PREF_CATEGORY_FILTERS)

  private val filtersPathTxt = createPathTextField()
  private val filtersPathBtn = JButton("...").apply {
    toolTipText = I18n.get(I18n.PREF_DEFAULT_FILTERS_PATH)
  }
  private val openLastFilterChbx = JCheckBox()
  private val applyFiltersAfterEditChbx = JCheckBox()
  private val rememberAppliedFiltersChbx = JCheckBox()
  private val collapseAllGroupsStartup = JCheckBox()
  private val applyFiltersOnCheckChbx = JCheckBox()

  private var selectedFiltersPath: File = userPrefs.defaultFiltersPath
  private var filterFolderChooser: JFileChooser? = null

  override val panel: JComponent = buildPanel()

  init {
    updatePathText(filtersPathTxt, selectedFiltersPath.absolutePath)
    openLastFilterChbx.isSelected = userPrefs.openLastFilter
    applyFiltersAfterEditChbx.isSelected = userPrefs.reapplyFiltersAfterEdit
    rememberAppliedFiltersChbx.isSelected = userPrefs.rememberAppliedFilters
    collapseAllGroupsStartup.isSelected = userPrefs.collapseAllGroupsStartup
    applyFiltersOnCheckChbx.isSelected = userPrefs.applyFilterOnCheck

    filtersPathBtn.addActionListener {
      val chooser = filterFolderChooser ?: JFileChooserExt(selectedFiltersPath).apply {
        dialogTitle = I18n.get(I18n.PREF_DEFAULT_FILTERS_PATH)
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        filterFolderChooser = this
      }
      chooser.selectedFile = selectedFiltersPath
      if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
        val file = chooser.selectedFile
        selectedFiltersPath = file
        updatePathText(filtersPathTxt, file.absolutePath)
      }
    }
  }

  override fun apply() {
    if (selectedFiltersPath != userPrefs.defaultFiltersPath) {
      userPrefs.defaultFiltersPath = selectedFiltersPath
    }
    if (openLastFilterChbx.isSelected != userPrefs.openLastFilter) {
      userPrefs.openLastFilter = openLastFilterChbx.isSelected
    }
    if (applyFiltersAfterEditChbx.isSelected != userPrefs.reapplyFiltersAfterEdit) {
      userPrefs.reapplyFiltersAfterEdit = applyFiltersAfterEditChbx.isSelected
    }
    if (rememberAppliedFiltersChbx.isSelected != userPrefs.rememberAppliedFilters) {
      userPrefs.rememberAppliedFilters = rememberAppliedFiltersChbx.isSelected
    }
    if (collapseAllGroupsStartup.isSelected != userPrefs.collapseAllGroupsStartup) {
      userPrefs.collapseAllGroupsStartup = collapseAllGroupsStartup.isSelected
    }
    if (applyFiltersOnCheckChbx.isSelected != userPrefs.applyFilterOnCheck) {
      userPrefs.applyFilterOnCheck = applyFiltersOnCheckChbx.isSelected
    }
  }

  private fun buildPanel(): JComponent {
    val cc = CellConstraints()
    val form = JPanel(
      FormLayout(
        "fill:pref,left:6dlu:noGrow,fill:default:grow",
        "center:d:noGrow,top:4dlu:noGrow,center:d:noGrow,top:4dlu:noGrow,center:d:noGrow,top:4dlu:noGrow,center:d:noGrow,top:4dlu:noGrow,center:d:noGrow,top:4dlu:noGrow,center:d:noGrow"
      )
    ).apply { isOpaque = false }

    // Row 1: Default filters path
    val filtersPathLbl = JLabel(I18n.get(I18n.PREF_DEFAULT_FILTERS_PATH))
    val filtersPathPanel = JPanel(BorderLayout(UIScaleUtils.dip(4), 0)).apply {
      isOpaque = false
      add(filtersPathTxt, BorderLayout.CENTER)
      add(filtersPathBtn, BorderLayout.EAST)
    }
    form.add(filtersPathLbl, cc.xy(1, 1))
    form.add(filtersPathPanel, cc.xy(3, 1))

    // Row 3: Open last filter
    val openLastFilterLbl = JLabel(I18n.get(I18n.PREF_OPEN_LAST_FILTER))
    bindLabelToCheckbox(openLastFilterLbl, openLastFilterChbx)
    form.add(openLastFilterLbl, cc.xy(1, 3))
    form.add(openLastFilterChbx, cc.xy(3, 3))

    // Row 5: Reapply filters after edit
    val applyAfterEditLbl = JLabel(I18n.get(I18n.PREF_APPLY_FILTERS_AFTER_EDIT))
    bindLabelToCheckbox(applyAfterEditLbl, applyFiltersAfterEditChbx)
    form.add(applyAfterEditLbl, cc.xy(1, 5))
    form.add(applyFiltersAfterEditChbx, cc.xy(3, 5))

    // Row 7: Remember applied filters
    val rememberAppliedLbl = JLabel(I18n.get(I18n.PREF_REMEMBER_APPLIED_FILTERS))
    bindLabelToCheckbox(rememberAppliedLbl, rememberAppliedFiltersChbx)
    form.add(rememberAppliedLbl, cc.xy(1, 7))
    form.add(rememberAppliedFiltersChbx, cc.xy(3, 7))

    // Row 9: Collapse all groups startup
    val collapseGroupsLbl = JLabel(I18n.get(I18n.PREF_COLLAPSE_ALL_GROUPS_STARTUP))
    bindLabelToCheckbox(collapseGroupsLbl, collapseAllGroupsStartup)
    form.add(collapseGroupsLbl, cc.xy(1, 9))
    form.add(collapseAllGroupsStartup, cc.xy(3, 9))

    // Row 11: Apply filters on check
    val applyOnCheckLbl = JLabel(I18n.get(I18n.PREF_APPLY_FILTERS_ON_CHECK))
    bindLabelToCheckbox(applyOnCheckLbl, applyFiltersOnCheckChbx)
    form.add(applyOnCheckLbl, cc.xy(1, 11))
    form.add(applyFiltersOnCheckChbx, cc.xy(3, 11))

    return form
  }

  companion object {
    const val ID = "filters"
  }
}
