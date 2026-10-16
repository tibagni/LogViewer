package com.tibagni.logviewer.util.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JPanel

class WrapLayoutTests {

  @Test
  fun testPreferredLayoutSizeSingleRowWhenWide() {
    val layout = WrapLayout(FlowLayout.CENTER, 10, 5)
    val container = JPanel(layout).apply {
      setSize(500, 100)
    }

    val comp1 = JPanel().apply { preferredSize = Dimension(100, 30) }
    val comp2 = JPanel().apply { preferredSize = Dimension(100, 30) }
    val comp3 = JPanel().apply { preferredSize = Dimension(100, 30) }

    container.add(comp1)
    container.add(comp2)
    container.add(comp3)

    val prefSize = layout.preferredLayoutSize(container)
    assertEquals(40, prefSize.height)
    assertTrue(prefSize.width >= 300)
  }

  @Test
  fun testPreferredLayoutSizeWrapsWhenNarrow() {
    val layout = WrapLayout(FlowLayout.CENTER, 10, 5)
    val container = JPanel(layout).apply {
      setSize(250, 100) // Cannot fit 3 * 100 components on 1 row
    }

    val comp1 = JPanel().apply { preferredSize = Dimension(100, 30) }
    val comp2 = JPanel().apply { preferredSize = Dimension(100, 30) }
    val comp3 = JPanel().apply { preferredSize = Dimension(100, 30) }

    container.add(comp1)
    container.add(comp2)
    container.add(comp3)

    val prefSize = layout.preferredLayoutSize(container)
    // Should wrap to at least 2 rows
    assertTrue(prefSize.height > 40)
  }

  @Test
  fun testMinimumLayoutSize() {
    val layout = WrapLayout(FlowLayout.CENTER, 10, 5)
    val container = JPanel(layout).apply {
      setSize(500, 100)
    }
    val comp = JPanel().apply {
      minimumSize = Dimension(50, 20)
      preferredSize = Dimension(100, 30)
    }
    container.add(comp)

    val minSize = layout.minimumLayoutSize(container)
    assertTrue(minSize.height > 0)
    assertTrue(minSize.width > 0)
  }
}
