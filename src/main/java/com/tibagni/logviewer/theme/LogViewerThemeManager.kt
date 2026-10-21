package com.tibagni.logviewer.theme

import com.formdev.flatlaf.FlatDarculaLaf
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatIntelliJLaf
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.themes.FlatMacDarkLaf
import com.formdev.flatlaf.themes.FlatMacLightLaf
import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import java.awt.Dimension
import java.lang.IllegalArgumentException
import javax.swing.UIManager

/**
 * Manages look-and-feel themes and global UI design tokens for the LogViewer application.
 *
 * Utilizes FlatLaf to provide modern light, dark, macOS, and IntelliJ/Darcula themes with consistent
 * component styling, rounded corners, rounded selection arcs, custom scrollbar appearances,
 * table presentation defaults, and unified window title pane decorations.
 */
object LogViewerThemeManager {
  private val LIGHT = LogViewerTheme("Light", false) { FlatLightLaf.setup() }
  private val DARK = LogViewerTheme("Dark", true) { FlatDarkLaf.setup() }
  private val MAC_LIGHT = LogViewerTheme("macOS Light", false) { FlatMacLightLaf.setup() }
  private val MAC_DARK = LogViewerTheme("macOS Dark", true) { FlatMacDarkLaf.setup() }
  private val INTELLIJ = LogViewerTheme("IntelliJ Light", false) { FlatIntelliJLaf.setup() }
  private val DARCULA = LogViewerTheme("Darcula", true) { FlatDarculaLaf.setup() }
  private val NONE = LogViewerTheme("None", false) { throw IllegalArgumentException("Tried to install theme NONE") }
  private val DEFAULT = LIGHT

  private val allThemes = mapOf(
    LIGHT.name to LIGHT,
    DARK.name to DARK,
    MAC_LIGHT.name to MAC_LIGHT,
    MAC_DARK.name to MAC_DARK,
    INTELLIJ.name to INTELLIJ,
    DARCULA.name to DARCULA
  )
  private var installedTheme: LogViewerTheme = NONE

  /**
   * The name of the currently active theme.
   *
   * Setting this property applies the requested theme (e.g., "Light" or "Dark") and updates
   * application UI tokens. If the provided theme name does not match any registered theme
   * and no theme has been installed yet, falls back to the default theme ("Light").
   */
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

  /**
   * Indicates whether the currently active theme is a dark variant.
   */
  val isDark: Boolean
    get() = installedTheme.isDark

  /**
   * Installs the specified theme and configures application-wide Swing UI defaults and tokens.
   *
   * Configures modern component rounding, rounded selection arcs (menus, combo boxes, lists, tables),
   * scrollbar styling, tabbed pane separators, table grid metrics, and unified window title pane
   * decorations across the user interface.
   *
   * @param theme the [LogViewerTheme] instance to install and activate.
   */
  private fun install(theme: LogViewerTheme) {
    installedTheme = theme
    theme.install()

    val showDividers = try {
      ServiceLocator.logViewerPrefs.showLogLineDividers
    } catch (e: Throwable) {
      true
    }

    UIManager.put("Component.arc", 8)
    UIManager.put("Button.arc", 8)
    UIManager.put("TextComponent.arc", 8)
    UIManager.put("Component.arrowType", "chevron")
    UIManager.put("MenuItem.selectionArc", 4)
    UIManager.put("ComboBox.selectionArc", 4)
    UIManager.put("List.selectionArc", 4)
    UIManager.put("Table.selectionArc", 4)
    UIManager.put("ScrollBar.thumbArc", 6)
    UIManager.put("ScrollBar.trackArc", 6)
    UIManager.put("ScrollBar.showButtons", false)
    UIManager.put("TabbedPane.showTabSeparators", true)
    UIManager.put("TabbedPane.tabSeparatorsFullHeight", false)
    UIManager.put("Table.showHorizontalLines", showDividers)
    UIManager.put("Table.showVerticalLines", false)
    UIManager.put("Table.intercellSpacing", Dimension(0, 1))
    UIManager.put("Table.rowHeight", UIScaleUtils.dip(22))
    UIManager.put("TitlePane.unifiedBackground", true)
    UIManager.put("TitlePane.menuBarEmbedded", true)

    FlatLaf.updateUILater()
  }

  /**
   * The set of all available theme names supported by the theme manager.
   */
  val availableThemes = allThemes.keys
}