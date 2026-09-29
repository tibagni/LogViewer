package com.tibagni.logviewer.theme

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.FlatLightLaf
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import java.awt.Dimension
import java.lang.IllegalArgumentException
import javax.swing.UIManager

object LogViewerThemeManager {
  private val LIGHT = LogViewerTheme("Light", false) { FlatLightLaf.setup() }
  private val DARK = LogViewerTheme("Dark", true) { FlatDarkLaf.setup() }
  private val NONE = LogViewerTheme("None", false) { throw IllegalArgumentException("Tried to install theme NONE") }
  private val DEFAULT = LIGHT

  private val allThemes = mapOf(
    LIGHT.name to LIGHT,
    DARK.name to DARK
  )
  private var installedTheme: LogViewerTheme = NONE

  var currentTheme: String
    get() = installedTheme.name
    set(value) {
      val t = allThemes[value]
      if (t == null && installedTheme == NONE) {
        install(DEFAULT) // fallback to default
      } else {
        t?.let { install(it) }
      }
    }

  val isDark: Boolean
    get() = installedTheme.isDark

  private fun install(theme: LogViewerTheme) {
    installedTheme = theme
    theme.install()
    
    UIManager.put("Component.arc", 8)
    UIManager.put("Button.arc", 8)
    UIManager.put("TextComponent.arc", 8)
    UIManager.put("ScrollBar.thumbArc", 6)
    UIManager.put("ScrollBar.showButtons", false)
    UIManager.put("TabbedPane.showTabSeparators", true)
    UIManager.put("TabbedPane.tabSeparatorsFullHeight", false)
    UIManager.put("Table.showHorizontalLines", true)
    UIManager.put("Table.showVerticalLines", false)
    UIManager.put("Table.intercellSpacing", Dimension(0, 1))
    UIManager.put("Table.rowHeight", UIScaleUtils.dip(22))
    
    FlatLaf.updateUILater()
  }

  val availableThemes = allThemes.keys
}