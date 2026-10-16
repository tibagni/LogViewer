package com.tibagni.logviewer.util.layout

import com.tibagni.logviewer.util.scaling.UIScaleUtils
import java.awt.Container
import java.awt.Dimension
import java.awt.FlowLayout

/**
 * A [FlowLayout] that calculates preferred and minimum layout sizes by wrapping components
 * based on the container's available width.
 */
class WrapLayout(align: Int = CENTER, hgap: Int = 5, vgap: Int = 5) : FlowLayout(align, hgap, vgap) {

  override fun preferredLayoutSize(target: Container): Dimension {
    return layoutSize(target, preferred = true)
  }

  override fun minimumLayoutSize(target: Container): Dimension {
    val min = layoutSize(target, preferred = false)
    min.width -= (hgap + 1)
    return min
  }

  private fun layoutSize(target: Container, preferred: Boolean): Dimension {
    synchronized(target.treeLock) {
      var targetWidth = target.width
      if (targetWidth == 0) {
        val parentWidth = target.parent?.width ?: 0
        val maxAllowed = UIScaleUtils.dip(700)
        targetWidth = if (parentWidth > 0) {
          minOf(maxAllowed, parentWidth - UIScaleUtils.dip(32))
        } else {
          maxAllowed
        }
      }

      val insets = target.insets
      val maxRowWidth = targetWidth - (insets.left + insets.right + hgap * 2)

      val dim = Dimension(0, 0)
      var rowWidth = 0
      var rowHeight = 0

      val nmembers = target.componentCount
      for (i in 0 until nmembers) {
        val m = target.getComponent(i)
        if (m.isVisible) {
          val d = if (preferred) m.preferredSize else m.minimumSize

          if (rowWidth + d.width > maxRowWidth && rowWidth > 0) {
            dim.width = maxOf(dim.width, rowWidth)
            dim.height += rowHeight + vgap
            rowWidth = 0
            rowHeight = 0
          }

          if (rowWidth != 0) {
            rowWidth += hgap
          }
          rowWidth += d.width
          rowHeight = maxOf(rowHeight, d.height)
        }
      }

      dim.width = maxOf(dim.width, rowWidth)
      dim.height += rowHeight

      dim.width += insets.left + insets.right + hgap * 2
      dim.height += insets.top + insets.bottom + vgap * 2

      return dim
    }
  }
}
