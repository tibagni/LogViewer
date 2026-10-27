package com.tibagni.logviewer.preferences

import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.CommonIcons
import com.tibagni.logviewer.view.FlatButton
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JTextField
import javax.swing.SwingUtilities

interface PreferencePanel {
  val id: String
  val title: String
  val panel: JComponent
  fun apply()
  val hasTimeConsumingOperation: Boolean get() = false
}

internal fun createPathTextField(): JTextField {
  return object : JTextField() {
    override fun getMinimumSize(): Dimension {
      val min = super.getMinimumSize()
      min.width = UIScaleUtils.dip(50)
      return min
    }

    override fun getPreferredSize(): Dimension {
      val pref = super.getPreferredSize()
      pref.width = UIScaleUtils.dip(150)
      return pref
    }
  }.apply {
    isEditable = false
    columns = 10
  }
}

internal fun updatePathText(textField: JTextField, path: String?) {
  textField.text = path ?: ""
  textField.toolTipText = if (path.isNullOrEmpty()) null else path
  textField.caretPosition = 0
}

internal fun bindLabelToCheckbox(label: JLabel, checkbox: JCheckBox) {
  label.labelFor = checkbox
  label.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  label.addMouseListener(object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      if (SwingUtilities.isLeftMouseButton(e) && checkbox.isEnabled) {
        checkbox.doClick()
      }
    }
  })
}

/**
 * Creates a modern styled browse button with a folder or document icon,
 * hand cursor, and tooltip text.
 *
 * @param tooltipText the tooltip text describing the browse target
 * @param icon the icon to display; defaults to [CommonIcons.folderIcon]
 * @return configured [FlatButton] with icon, tooltip, and hand cursor
 */
internal fun createBrowseButton(
  tooltipText: String,
  icon: Icon = CommonIcons.folderIcon()
): FlatButton {
  return FlatButton(icon).apply {
    toolTipText = tooltipText
  }
}
