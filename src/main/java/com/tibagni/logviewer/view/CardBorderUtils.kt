package com.tibagni.logviewer.view

import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.ui.FlatLineBorder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import java.awt.Color
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.UIManager
import javax.swing.border.Border
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder

/**
 * Shared utility functions for creating Material 3 / FlatLaf card borders
 * and resolving adaptive border and label colors across dialogs and panels.
 */
object CardBorderUtils {

  const val DEFAULT_CARD_ARC: Int = 8
  const val DEFAULT_BORDER_THICKNESS: Float = 1f

  /**
   * Resolves the theme-adaptive border color for cards and container panels.
   *
   * @return the resolved [Color] based on current Look and Feel
   */
  @JvmStatic
  fun resolveCardBorderColor(): Color {
    return UIManager.getColor("Component.borderColor")
      ?: UIManager.getColor("Separator.foreground")
      ?: if (FlatLaf.isLafDark()) {
        Color(255, 255, 255, 40)
      } else {
        Color(0, 0, 0, 40)
      }
  }

  /**
   * Resolves the secondary foreground color for field key labels or hints.
   *
   * @return the resolved [Color] based on current Look and Feel
   */
  @JvmStatic
  fun resolveSecondaryLabelColor(): Color {
    return UIManager.getColor("Label.disabledForeground")
      ?: if (FlatLaf.isLafDark()) {
        Color(160, 160, 160)
      } else {
        Color(110, 110, 110)
      }
  }

  /**
   * Creates an 8dp rounded [FlatLineBorder] card container border.
   *
   * @param arc corner arc in dips (defaults to 8dp)
   * @param insets border insets
   * @param lineColor border line color (defaults to [resolveCardBorderColor])
   * @param thickness border line thickness
   * @return a rounded [FlatLineBorder]
   */
  @JvmStatic
  @JvmOverloads
  fun createCardBorder(
    arc: Int = DEFAULT_CARD_ARC,
    insets: Insets = Insets(4, 4, 4, 4),
    lineColor: Color = resolveCardBorderColor(),
    thickness: Float = DEFAULT_BORDER_THICKNESS
  ): FlatLineBorder {
    return FlatLineBorder(insets, lineColor, thickness, UIScaleUtils.dip(arc))
  }

  /**
   * Creates a titled section card border with rounded [FlatLineBorder]
   * and internal [EmptyBorder] padding.
   *
   * @param title the section title string
   * @param arc corner arc in dips (defaults to 8dp)
   * @param insets line border insets
   * @param padding internal padding (defaults to 6dp vertical, 8dp horizontal)
   * @return a [Border] composed of [TitledBorder] and [EmptyBorder]
   */
  @JvmStatic
  @JvmOverloads
  fun createTitledCardBorder(
    title: String,
    arc: Int = DEFAULT_CARD_ARC,
    insets: Insets = Insets(4, 4, 4, 4),
    padding: EmptyBorder? = null
  ): Border {
    val lineBorder = createCardBorder(arc = arc, insets = insets)
    val titledBorder = BorderFactory.createTitledBorder(lineBorder, title)
    val pad = padding ?: EmptyBorder(
      UIScaleUtils.dip(6),
      UIScaleUtils.dip(8),
      UIScaleUtils.dip(6),
      UIScaleUtils.dip(8)
    )
    return CompoundBorder(titledBorder, pad)
  }
}
