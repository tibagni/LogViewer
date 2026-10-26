package com.tibagni.logviewer.view

import com.formdev.flatlaf.ui.FlatLineBorder
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Color
import java.awt.Insets
import javax.swing.UIManager
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder

/**
 * Unit tests for [CardBorderUtils] verifying card border creation,
 * titled card borders, and theme-adaptive color resolution.
 */
class CardBorderUtilsTests {

  @Test
  fun testResolveCardBorderColorFallback() {
    val originalBorderColor = UIManager.get("Component.borderColor")
    val originalSeparatorColor = UIManager.get("Separator.foreground")
    try {
      UIManager.put("Component.borderColor", null)
      UIManager.put("Separator.foreground", null)

      val color = CardBorderUtils.resolveCardBorderColor()
      assertNotNull(color)
      assertTrue(color.alpha > 0)
    } finally {
      UIManager.put("Component.borderColor", originalBorderColor)
      UIManager.put("Separator.foreground", originalSeparatorColor)
    }
  }

  @Test
  fun testResolveCardBorderColorFromUIManager() {
    val originalBorderColor = UIManager.get("Component.borderColor")
    try {
      val testColor = Color(12, 34, 56)
      UIManager.put("Component.borderColor", testColor)

      val resolved = CardBorderUtils.resolveCardBorderColor()
      assertEquals(testColor, resolved)
    } finally {
      UIManager.put("Component.borderColor", originalBorderColor)
    }
  }

  @Test
  fun testResolveSecondaryLabelColor() {
    val originalDisabled = UIManager.get("Label.disabledForeground")
    try {
      val testColor = Color(88, 99, 100)
      UIManager.put("Label.disabledForeground", testColor)

      val resolved = CardBorderUtils.resolveSecondaryLabelColor()
      assertEquals(testColor, resolved)
    } finally {
      UIManager.put("Label.disabledForeground", originalDisabled)
    }
  }

  @Test
  fun testCreateCardBorderDefaults() {
    val border = CardBorderUtils.createCardBorder()
    assertNotNull(border)
    assertEquals(Insets(4, 4, 4, 4), border.borderInsets)
    assertEquals(UIScaleUtils.dip(8).toFloat(), border.arc.toFloat(), 0.01f)
  }

  @Test
  fun testCreateCardBorderCustomParameters() {
    val customInsets = Insets(2, 6, 2, 6)
    val customColor = Color.CYAN
    val border = CardBorderUtils.createCardBorder(
      arc = 12,
      insets = customInsets,
      lineColor = customColor,
      thickness = 2f
    )

    assertEquals(customInsets, border.borderInsets)
    assertEquals(customColor, border.lineColor)
    assertEquals(UIScaleUtils.dip(12).toFloat(), border.arc.toFloat(), 0.01f)
    assertEquals(2f, border.lineThickness, 0.01f)
  }

  @Test
  fun testCreateTitledCardBorder() {
    val title = "Sample Section"
    val border = CardBorderUtils.createTitledCardBorder(title)

    assertTrue(border is CompoundBorder)
    val compound = border as CompoundBorder

    assertTrue(compound.outsideBorder is TitledBorder)
    val titled = compound.outsideBorder as TitledBorder
    assertEquals(title, titled.title)
    assertTrue(titled.border is FlatLineBorder)

    assertTrue(compound.insideBorder is EmptyBorder)
    val empty = compound.insideBorder as EmptyBorder
    assertEquals(UIScaleUtils.dip(6), empty.borderInsets.top)
    assertEquals(UIScaleUtils.dip(8), empty.borderInsets.left)
    assertEquals(UIScaleUtils.dip(6), empty.borderInsets.bottom)
    assertEquals(UIScaleUtils.dip(8), empty.borderInsets.right)
  }
}
