package com.tibagni.logviewer.filter

import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.ui.FlatLineBorder
import com.tibagni.logviewer.annotation.VisibleForTesting
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.StringUtils
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.ButtonsPane
import com.tibagni.logviewer.view.CardBorderUtils
import com.tibagni.logviewer.view.StatusBadge
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.Frame
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.border.Border

/**
 * Deduplication resolution calculation logic for duplicate filter clusters.
 */
object FilterDeduplicationLogic {

  /**
   * Calculates filters to be removed based on cluster decisions and options.
   *
   * @param clusters duplicate clusters to evaluate
   * @param decisions cluster index mapped to pair of (shouldDedup, chosenGroup)
   * @param includeCrossFileDuplicates whether cross-file duplicates are kept
   * @return map of group names to filters scheduled for removal
   */
  fun calculateRemovals(
    clusters: List<DuplicateCluster>,
    decisions: Map<Int, Pair<Boolean, String?>>,
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

      val matchesInRetainingGroup = cluster.matches.filter {
        it.group == retainingGroup
      }
      if (matchesInRetainingGroup.size > 1) {
        for (i in 1 until matchesInRetainingGroup.size) {
          result.getOrPut(retainingGroup) {
            mutableListOf()
          }.add(matchesInRetainingGroup[i].filter)
        }
      }

      val matchesInOtherGroups = cluster.matches.filter {
        it.group != retainingGroup
      }
      for (match in matchesInOtherGroups) {
        result.getOrPut(match.group) {
          mutableListOf()
        }.add(match.filter)
      }
    }
    return result
  }
}

/**
 * Modal dialog allowing users to review duplicate filter clusters and choose
 * retaining groups with Material 3 Expressive FlatLaf styling and StatusBadge.
 */
class FilterDeduplicationDialog(
  owner: Frame?,
  val clusters: List<DuplicateCluster>
) : JDialog(owner), ButtonsPane.Listener {

  private val buttonsPane = ButtonsPane(
    ButtonsPane.ButtonsMode.OK_CANCEL,
    this
  )

  @VisibleForTesting
  val mainContentPane = JPanel()

  @VisibleForTesting
  val mergeCrossFileCb = JCheckBox(
    I18n.get(I18n.FILTER_DEDUP_CLEAN_CROSS_FILE),
    true
  ).apply {
    isFocusPainted = false
  }

  // Cluster resolution state: cluster index -> (shouldDeduplicate, chosenGroup)
  @VisibleForTesting
  val clusterSelections =
    mutableMapOf<Int, Pair<JCheckBox, JComboBox<String>?>>()

  @VisibleForTesting
  val clusterBoxes = mutableMapOf<Int, JPanel>()

  @VisibleForTesting
  val clusterBadges = mutableMapOf<Int, StatusBadge>()

  var isApplied: Boolean = false
    private set

  /**
   * Returns a map of group name to filters scheduled for removal based on
   * user selections and active deduplication choices.
   */
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
    setContentPane(mainContentPane)
    buttonsPane.setOkText(I18n.get(I18n.FILTER_DEDUP_BTN_CLEAN))
    buttonsPane.setCancelText(I18n.get(I18n.COMMON_CANCEL))
    buttonsPane.setDefaultButtonOk()
  }

  /**
   * Returns the primary content panel for the dialog.
   *
   * @return [JPanel] hosting dialog layout components
   */
  @VisibleForTesting
  override fun getContentPane(): JPanel = mainContentPane

  /**
   * Resolves the theme-adaptive border color for cluster card containers.
   *
   * @return theme-adaptive [Color] matching FlatLaf active palette
   */
  fun resolveCardBorderColor(): Color =
    CardBorderUtils.resolveCardBorderColor()

  /**
   * Creates an 8dp rounded [FlatLineBorder] for a cluster card container.
   *
   * @return styled [Border] with 8dp rounded corners and theme-adaptive color
   */
  fun createCardBorder(): Border {
    return CardBorderUtils.createCardBorder(
      insets = Insets(
        UIScaleUtils.dip(10),
        UIScaleUtils.dip(12),
        UIScaleUtils.dip(10),
        UIScaleUtils.dip(12)
      )
    )
  }

  /**
   * Constructs dialog UI components including header, cluster cards, and
   * action buttons.
   */
  private fun buildUi() {
    mainContentPane.layout = BorderLayout()
    mainContentPane.border = BorderFactory.createEmptyBorder(
      UIScaleUtils.dip(15),
      UIScaleUtils.dip(15),
      UIScaleUtils.dip(15),
      UIScaleUtils.dip(15)
    )

    val headerPanel = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      border = BorderFactory.createEmptyBorder(
        0, 0, UIScaleUtils.dip(10), 0
      )
    }

    val headerTitle = JLabel(
      I18n.get(I18n.FILTER_DEDUP_HEADER_TITLE)
    ).apply {
      font = font.deriveFont(Font.BOLD, font.size2D + UIScaleUtils.dip(2))
    }
    headerPanel.add(headerTitle)
    headerPanel.add(Box.createRigidArea(Dimension(0, UIScaleUtils.dip(4))))

    val headerMsg = JLabel(
      StringUtils.wrapHtml(
        I18n.format(I18n.FILTER_DEDUP_HEADER_MSG, clusters.size)
      )
    )
    headerPanel.add(headerMsg)

    val hasCrossGroup = clusters.any { it.isCrossGroup }
    if (hasCrossGroup) {
      mergeCrossFileCb.addActionListener {
        val includeCross = mergeCrossFileCb.isSelected
        clusters.forEachIndexed { index, cluster ->
          if (cluster.isCrossGroup) {
            clusterBoxes[index]?.isVisible = includeCross
            val (enableCb, _) = clusterSelections[index]
              ?: return@forEachIndexed
            enableCb.isSelected = includeCross
          }
        }
        mainContentPane.revalidate()
        mainContentPane.repaint()
      }
      headerPanel.add(Box.createRigidArea(Dimension(0, UIScaleUtils.dip(8))))
      headerPanel.add(mergeCrossFileCb)
    }

    mainContentPane.add(headerPanel, BorderLayout.NORTH)

    val listPanel = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      border = BorderFactory.createEmptyBorder(
        UIScaleUtils.dip(4),
        UIScaleUtils.dip(4),
        UIScaleUtils.dip(4),
        UIScaleUtils.dip(4)
      )
    }

    clusters.forEachIndexed { index, cluster ->
      val clusterBox = JPanel(GridBagLayout()).apply {
        border = createCardBorder()
      }

      val enableCb = JCheckBox().apply {
        isSelected = true
        isFocusPainted = false
        toolTipText = I18n.get(I18n.FILTER_DEDUP_CLUSTER_ENABLE)
        accessibleContext.accessibleName =
          I18n.get(I18n.FILTER_DEDUP_CLUSTER_ENABLE)
      }

      val patternLabel = JLabel(
        cluster.representative.patternString
      ).apply {
        font = Font(Font.MONOSPACED, Font.BOLD, font.size)
        labelFor = enableCb
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addMouseListener(object : MouseAdapter() {
          override fun mouseClicked(e: MouseEvent) {
            if (SwingUtilities.isLeftMouseButton(e) && enableCb.isEnabled) {
              enableCb.doClick()
            }
          }
        })
      }

      val badge = if (cluster.isCrossGroup) {
        StatusBadge(StatusBadge.Variant.SURFACE).apply {
          text = I18n.get(I18n.FILTER_DEDUP_BADGE_CROSS_GROUP)
        }
      } else {
        StatusBadge(StatusBadge.Variant.NORMAL).apply {
          text = I18n.get(I18n.FILTER_DEDUP_BADGE_SAME_GROUP)
        }
      }
      clusterBadges[index] = badge

      val headerRow = JPanel(BorderLayout(UIScaleUtils.dip(8), 0)).apply {
        isOpaque = false
        val left = JPanel(BorderLayout(UIScaleUtils.dip(6), 0)).apply {
          isOpaque = false
          add(enableCb, BorderLayout.WEST)
          add(patternLabel, BorderLayout.CENTER)
        }
        add(left, BorderLayout.CENTER)
        add(badge, BorderLayout.EAST)
      }

      clusterBox.add(
        headerRow,
        GBConstraintsBuilder()
          .withGridx(0).withGridy(0)
          .withWeightx(1.0)
          .withFill(GridBagConstraints.HORIZONTAL)
          .withAnchor(GridBagConstraints.WEST)
          .withInsets(Insets(0, 0, UIScaleUtils.dip(6), 0))
          .build()
      )

      var groupCombo: JComboBox<String>? = null
      if (cluster.isCrossGroup) {
        val groupChoicePanel = JPanel().apply {
          layout = BoxLayout(this, BoxLayout.X_AXIS)
          isOpaque = false
        }
        val keepLabel = JLabel(
          I18n.get(I18n.FILTER_DEDUP_KEEP_IN_GROUP)
        ).apply {
          foreground = UIManager.getColor("Label.disabledForeground")
            ?: Color.GRAY
        }
        groupChoicePanel.add(keepLabel)
        groupChoicePanel.add(
          Box.createRigidArea(Dimension(UIScaleUtils.dip(6), 0))
        )
        groupCombo = JComboBox(cluster.affectedGroups.toTypedArray())
        groupChoicePanel.add(groupCombo)

        clusterBox.add(
          groupChoicePanel,
          GBConstraintsBuilder()
            .withGridx(0).withGridy(1)
            .withWeightx(1.0)
            .withAnchor(GridBagConstraints.WEST)
            .withInsets(Insets(0, 0, UIScaleUtils.dip(4), 0))
            .build()
        )

        val detailsText = cluster.matches.joinToString("<br>") { match ->
          I18n.format(
            I18n.FILTER_DEDUP_CROSS_GROUP_ITEM,
            match.group,
            match.filter.patternString
          )
        }
        val detailsLabel = JLabel(
          StringUtils.wrapHtml("<small>$detailsText</small>")
        )
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
        val infoLabel = JLabel(
          StringUtils.wrapHtml(
            I18n.format(
              I18n.FILTER_DEDUP_SAME_GROUP_INFO,
              groupName,
              cluster.matches.size
            )
          )
        )
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

    val scrollPane = JScrollPane(listPanel).apply {
      preferredSize = Dimension(
        UIScaleUtils.dip(560),
        UIScaleUtils.dip(360)
      )
      border = FlatLineBorder(
        Insets(
          UIScaleUtils.dip(2),
          UIScaleUtils.dip(2),
          UIScaleUtils.dip(2),
          UIScaleUtils.dip(2)
        ),
        resolveCardBorderColor(),
        1f,
        UIScaleUtils.dip(8)
      )
      viewportBorder = null
      verticalScrollBar.unitIncrement = 16
    }
    mainContentPane.add(scrollPane, BorderLayout.CENTER)
    mainContentPane.add(buttonsPane, BorderLayout.SOUTH)
  }

  /**
   * Handles OK action, marking deduplication as applied and closing dialog.
   */
  override fun onOk() {
    isApplied = true
    dispose()
  }

  /**
   * Handles Cancel action, closing dialog without applying deduplication.
   */
  override fun onCancel() {
    isApplied = false
    dispose()
  }

  companion object {

    /**
     * Displays the deduplication dialog and returns removal results if applied.
     *
     * @param parent owner frame
     * @param clusters duplicate clusters to resolve
     * @return map of group name to filters to remove, or null if cancelled
     */
    fun showDialog(
      parent: Frame?,
      clusters: List<DuplicateCluster>
    ): Map<String, List<Filter>>? {
      val dialog = FilterDeduplicationDialog(parent, clusters)
      dialog.pack()
      dialog.setLocationRelativeTo(parent)
      dialog.isVisible = true
      return if (dialog.isApplied) dialog.removalsByGroup else null
    }
  }
}
