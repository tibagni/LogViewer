package com.tibagni.logviewer.filter

import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.StringUtils
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.ButtonsPane
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Frame
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

object FilterDeduplicationLogic {
    fun calculateRemovals(
        clusters: List<DuplicateCluster>,
        decisions: Map<Int, Pair<Boolean, String?>>, // clusterIndex -> (shouldDeduplicate, chosenGroup)
        includeCrossFileDuplicates: Boolean = true
    ): Map<String, List<Filter>> {
        val result = mutableMapOf<String, MutableList<Filter>>()
        clusters.forEachIndexed { index, cluster ->
            if (!includeCrossFileDuplicates && cluster.isCrossGroup) {
                return@forEachIndexed
            }

            val (enabled, chosenGroup) = decisions[index] ?: (true to null)
            if (!enabled) return@forEachIndexed

            val retainingGroup = chosenGroup ?: cluster.affectedGroups.first()

            val matchesInRetainingGroup = cluster.matches.filter { it.group == retainingGroup }
            if (matchesInRetainingGroup.size > 1) {
                for (i in 1 until matchesInRetainingGroup.size) {
                    result.getOrPut(retainingGroup) { mutableListOf() }.add(matchesInRetainingGroup[i].filter)
                }
            }

            val matchesInOtherGroups = cluster.matches.filter { it.group != retainingGroup }
            for (match in matchesInOtherGroups) {
                result.getOrPut(match.group) { mutableListOf() }.add(match.filter)
            }
        }
        return result
    }
}

class FilterDeduplicationDialog(
    owner: Frame?,
    val clusters: List<DuplicateCluster>
) : JDialog(owner), ButtonsPane.Listener {

    private val buttonsPane = ButtonsPane(ButtonsPane.ButtonsMode.OK_CANCEL, this)
    private val contentPane = JPanel()
    val mergeCrossFileCb = JCheckBox(I18n.get(I18n.FILTER_DEDUP_CLEAN_CROSS_FILE), true)

    // Cluster resolution state: cluster index -> (shouldDeduplicate, chosenRetainingGroup)
    val clusterSelections = mutableMapOf<Int, Pair<JCheckBox, JComboBox<String>?>>()
    private val clusterBoxes = mutableMapOf<Int, JPanel>()

    var isApplied: Boolean = false
        private set

    val removalsByGroup: Map<String, List<Filter>>
        get() {
            val decisions = clusterSelections.mapValues { (_, pair) ->
                val (cb, combo) = pair
                Pair(cb.isSelected, combo?.selectedItem as? String)
            }
            return FilterDeduplicationLogic.calculateRemovals(
                clusters,
                decisions,
                includeCrossFileDuplicates = mergeCrossFileCb.isSelected
            )
        }

    init {
        title = I18n.get(I18n.FILTER_DEDUP_DIALOG_TITLE)
        isModal = true
        buildUi()
        setContentPane(contentPane)
        buttonsPane.setOkText(I18n.get(I18n.FILTER_DEDUP_BTN_CLEAN))
        buttonsPane.setCancelText(I18n.get(I18n.COMMON_CANCEL))
        buttonsPane.setDefaultButtonOk()
    }

    private fun buildUi() {
        contentPane.layout = BorderLayout()
        contentPane.border = BorderFactory.createEmptyBorder(
            UIScaleUtils.dip(15), UIScaleUtils.dip(15),
            UIScaleUtils.dip(15), UIScaleUtils.dip(15)
        )

        val headerPanel = JPanel()
        headerPanel.layout = BoxLayout(headerPanel, BoxLayout.Y_AXIS)
        headerPanel.add(JLabel(StringUtils.wrapHtml("<h3>${I18n.get(I18n.FILTER_DEDUP_HEADER_TITLE)}</h3>")))
        headerPanel.add(Box.createRigidArea(Dimension(0, UIScaleUtils.dip(5))))
        headerPanel.add(JLabel(StringUtils.wrapHtml(I18n.format(I18n.FILTER_DEDUP_HEADER_MSG, clusters.size))))
        headerPanel.add(Box.createRigidArea(Dimension(0, UIScaleUtils.dip(10))))

        val hasCrossGroup = clusters.any { it.isCrossGroup }
        if (hasCrossGroup) {
            mergeCrossFileCb.addActionListener {
                val includeCross = mergeCrossFileCb.isSelected
                clusters.forEachIndexed { index, cluster ->
                    if (cluster.isCrossGroup) {
                        clusterBoxes[index]?.isVisible = includeCross
                        val (enableCb, _) = clusterSelections[index] ?: return@forEachIndexed
                        enableCb.isSelected = includeCross
                    }
                }
                contentPane.revalidate()
                contentPane.repaint()
            }
            headerPanel.add(mergeCrossFileCb)
            headerPanel.add(Box.createRigidArea(Dimension(0, UIScaleUtils.dip(10))))
        }

        contentPane.add(headerPanel, BorderLayout.NORTH)

        val listPanel = JPanel()
        listPanel.layout = BoxLayout(listPanel, BoxLayout.Y_AXIS)

        clusters.forEachIndexed { index, cluster ->
            val clusterBox = JPanel(GridBagLayout())
            clusterBox.border = BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(I18n.format(I18n.FILTER_DEDUP_CLUSTER_TITLE, cluster.representative.patternString)),
                BorderFactory.createEmptyBorder(UIScaleUtils.dip(8), UIScaleUtils.dip(8), UIScaleUtils.dip(8), UIScaleUtils.dip(8))
            )

            val enableCb = JCheckBox(I18n.get(I18n.FILTER_DEDUP_CLUSTER_ENABLE), true)
            var groupCombo: JComboBox<String>? = null

            clusterBox.add(
                enableCb,
                GBConstraintsBuilder()
                    .withGridx(0).withGridy(0)
                    .withWeightx(1.0)
                    .withAnchor(GridBagConstraints.WEST)
                    .build()
            )

            if (cluster.isCrossGroup) {
                val groupChoicePanel = JPanel()
                groupChoicePanel.layout = BoxLayout(groupChoicePanel, BoxLayout.X_AXIS)
                groupChoicePanel.add(JLabel(I18n.get(I18n.FILTER_DEDUP_KEEP_IN_GROUP)))
                groupCombo = JComboBox(cluster.affectedGroups.toTypedArray())
                groupChoicePanel.add(groupCombo)

                clusterBox.add(
                    groupChoicePanel,
                    GBConstraintsBuilder()
                        .withGridx(0).withGridy(1)
                        .withWeightx(1.0)
                        .withAnchor(GridBagConstraints.WEST)
                        .build()
                )

                val detailsText = cluster.matches.joinToString("<br>") { match ->
                    I18n.format(I18n.FILTER_DEDUP_CROSS_GROUP_ITEM, match.group, match.filter.patternString)
                }
                val detailsLabel = JLabel(StringUtils.wrapHtml("<small>$detailsText</small>"))
                clusterBox.add(
                    detailsLabel,
                    GBConstraintsBuilder()
                        .withGridx(0).withGridy(2)
                        .withWeightx(1.0)
                        .withAnchor(GridBagConstraints.WEST)
                        .build()
                )
            } else {
                val groupName = cluster.affectedGroups.first()
                val infoLabel = JLabel(StringUtils.wrapHtml(I18n.format(I18n.FILTER_DEDUP_SAME_GROUP_INFO, groupName, cluster.matches.size)))
                clusterBox.add(
                    infoLabel,
                    GBConstraintsBuilder()
                        .withGridx(0).withGridy(1)
                        .withWeightx(1.0)
                        .withAnchor(GridBagConstraints.WEST)
                        .build()
                )
            }

            clusterSelections[index] = Pair(enableCb, groupCombo)
            clusterBoxes[index] = clusterBox
            listPanel.add(clusterBox)
            listPanel.add(Box.createRigidArea(Dimension(0, UIScaleUtils.dip(8))))
        }

        val scrollPane = JScrollPane(listPanel)
        scrollPane.preferredSize = Dimension(UIScaleUtils.dip(550), UIScaleUtils.dip(350))
        contentPane.add(scrollPane, BorderLayout.CENTER)

        contentPane.add(buttonsPane, BorderLayout.SOUTH)
    }

    override fun onOk() {
        isApplied = true
        dispose()
    }

    override fun onCancel() {
        isApplied = false
        dispose()
    }

    companion object {
        fun showDialog(parent: Frame?, clusters: List<DuplicateCluster>): Map<String, List<Filter>>? {
            val dialog = FilterDeduplicationDialog(parent, clusters)
            dialog.pack()
            dialog.setLocationRelativeTo(parent)
            dialog.isVisible = true
            return if (dialog.isApplied) dialog.removalsByGroup else null
        }
    }
}
