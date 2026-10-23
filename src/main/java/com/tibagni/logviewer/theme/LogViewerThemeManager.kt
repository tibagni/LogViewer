package com.tibagni.logviewer.theme

import com.formdev.flatlaf.FlatDarculaLaf
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatIntelliJLaf
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.themes.FlatMacDarkLaf
import com.formdev.flatlaf.themes.FlatMacLightLaf
import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.util.SwingUtils
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import java.awt.Color
import java.awt.Dimension
import java.awt.Insets
import java.lang.IllegalArgumentException
import javax.swing.UIManager

/**
 * Manages look-and-feel themes, accent colors, and global UI design properties
 * for the LogViewer application.
 *
 * Utilizes FlatLaf to provide modern light, dark, macOS, and IntelliJ/Darcula
 * themes with customizable accent colors, consistent component styling,
 * rounded corners, rounded selection arcs, pill progress bars, slender
 * splitters, floating inset scrollbars, underlined tabs, tooltip styling, drop
 * shadows, table presentation defaults, and unified window title pane
 * decorations.
 */
object LogViewerThemeManager {
  private val LIGHT = LogViewerTheme("Light", false) { FlatLightLaf.setup() }
  private val DARK = LogViewerTheme("Dark", true) { FlatDarkLaf.setup() }
  private val MAC_LIGHT =
    LogViewerTheme("macOS Light", false) { FlatMacLightLaf.setup() }
  private val MAC_DARK =
    LogViewerTheme("macOS Dark", true) { FlatMacDarkLaf.setup() }
  private val INTELLIJ =
    LogViewerTheme("IntelliJ Light", false) { FlatIntelliJLaf.setup() }
  private val DARCULA = LogViewerTheme("Darcula", true) { FlatDarculaLaf.setup() }
  private val NONE = LogViewerTheme("None", false) {
    throw IllegalArgumentException("Tried to install theme NONE")
  }
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
   * Map of accent color names to their corresponding hex color codes.
   * A null hex value represents the default look-and-feel accent color.
   */
  val ACCENT_COLORS = mapOf(
    "Default" to null,
    "Blue" to "#2670E8",
    "Purple" to "#7C4DFF",
    "Teal" to "#00BFA5",
    "Red" to "#E53935",
    "Orange" to "#FB8C00",
    "Green" to "#43A047"
  )

  /**
   * List of available accent color names supported for theme personalization.
   */
  val availableAccentColors = ACCENT_COLORS.keys.toList()

  /**
   * The name of the currently active accent color.
   */
  var currentAccentColor: String = "Default"
    private set

  /**
   * The name of the currently active theme.
   *
   * Setting this property applies the requested theme (e.g., "Light" or "Dark")
   * and updates application UI properties. If the provided theme name does not match
   * any registered theme and no theme has been installed yet, falls back to the
   * default theme ("Light").
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
   * Returns the standard text foreground color for labels and list items.
   */
  val textForeground: Color
    get() = UIManager.getColor("Label.foreground")
      ?: if (isDark) Color.WHITE else Color.BLACK

  /**
   * Returns the hex string representation (e.g. "#ffffff") of [textForeground].
   */
  val textForegroundHex: String
    get() = SwingUtils.toHexString(textForeground)

  /**
   * Returns the secondary/muted text foreground color for the active theme.
   */
  val mutedTextForeground: Color
    get() = UIManager.getColor("Label.disabledForeground")
      ?: if (isDark) Color(180, 180, 180) else Color(120, 120, 120)

  /**
   * Returns the hex string representation of [mutedTextForeground].
   */
  val mutedTextForegroundHex: String
    get() = SwingUtils.toHexString(mutedTextForeground)

  /**
   * Sets the accent color for FlatLaf themes and refreshes the UI.
   *
   * @param accentColor the name of the accent color to apply.
   */
  fun setAccentColor(accentColor: String) {
    currentAccentColor = if (ACCENT_COLORS.containsKey(accentColor)) {
      accentColor
    } else {
      "Default"
    }
    val hex = ACCENT_COLORS[currentAccentColor]
    FlatLaf.setGlobalExtraDefaults(
      if (hex != null) mapOf("@accentColor" to hex) else emptyMap()
    )
    if (installedTheme != NONE) {
      installedTheme.install()
      applyUiProperties()
      FlatLaf.updateUILater()
    }
  }

  /**
   * Returns the default accent color for the currently active theme.
   *
   * @return the theme's default accent [Color].
   */
  fun getDefaultAccentColor(): Color {
    if (currentAccentColor == "Default") {
      val uimColor = UIManager.getColor("Component.accentColor")
      if (uimColor != null) return uimColor
    }
    return when {
      currentTheme.startsWith("macOS") -> Color.decode("#007AFF")
      isDark -> Color.decode("#4B6EAF")
      else -> Color.decode("#2675BF")
    }
  }

  /**
   * Returns the [Color] corresponding to the specified accent color name.
   * If the accent color is "Default", returns [getDefaultAccentColor].
   * If the color is unknown or invalid, falls back to [getDefaultAccentColor].
   *
   * @param accentColor the name of the accent color (e.g., "Default", "Blue", etc.).
   * @return the resolved [Color].
   */
  fun getColorForAccent(accentColor: String): Color {
    val hex = ACCENT_COLORS[accentColor]
    return if (hex != null) {
      try {
        Color.decode(hex)
      } catch (e: NumberFormatException) {
        getDefaultAccentColor()
      }
    } else {
      getDefaultAccentColor()
    }
  }

  /**
   * Installs the specified theme and configures application-wide Swing UI
   * defaults and properties.
   *
   * Configures modern component rounding, rounded selection arcs (menus,
   * combo boxes, lists, tables), pill progress bars, slender splitters with
   * centered one-touch buttons, underlined tab styling and tab separators,
   * tooltip arcs, floating inset scrollbars, popup drop shadows, table header
   * metrics, table cell margins, table grid metrics, and unified window title
   * pane decorations across the user interface.
   *
   * @param theme the [LogViewerTheme] instance to install and activate.
   */
  private fun install(theme: LogViewerTheme) {
    installedTheme = theme
    val hex = ACCENT_COLORS[currentAccentColor]
    FlatLaf.setGlobalExtraDefaults(
      if (hex != null) mapOf("@accentColor" to hex) else emptyMap()
    )
    theme.install()
    applyUiProperties()
    FlatLaf.updateUILater()
  }

  /**
   * Configures application-wide Swing UI defaults and design properties.
   *
   * Sets component corner rounding, selection arcs, scrollbar styling,
   * split pane sizing, tab presentation, popup drop shadows, and modern
   * table metrics including table header height, cell margins, and trailing
   * vertical line suppression.
   */
  private fun applyUiProperties() {
    val showDividers = try {
      ServiceLocator.logViewerPrefs.showLogLineDividers
    } catch (e: Throwable) {
      true
    }

    UIManager.put("Component.arc", 8)
    UIManager.put("Button.arc", 8)
    UIManager.put("TextComponent.arc", 8)
    UIManager.put("ProgressBar.arc", 999)
    UIManager.put("ProgressBar.cycleTime", 1000)
    UIManager.put("Component.arrowType", "chevron")
    UIManager.put("MenuItem.selectionArc", 4)
    UIManager.put("ComboBox.selectionArc", 4)
    UIManager.put("List.selectionArc", 4)
    UIManager.put("Table.selectionArc", 4)
    UIManager.put("SplitPane.dividerSize", UIScaleUtils.dip(4))
    UIManager.put("SplitPane.centerOneTouchButtons", true)
    UIManager.put("TabbedPane.tabType", "underlined")
    UIManager.put("TabbedPane.showTabSeparators", true)
    UIManager.put("TabbedPane.tabSeparatorsFullHeight", false)
    UIManager.put("ToolTip.arc", 6)
    UIManager.put("ScrollBar.thumbArc", 6)
    UIManager.put("ScrollBar.trackArc", 6)
    UIManager.put("ScrollBar.thumbInsets", Insets(2, 2, 2, 2))
    UIManager.put("ScrollBar.width", UIScaleUtils.dip(10))
    UIManager.put("ScrollBar.showButtons", false)
    UIManager.put("Popup.dropShadowPainted", true)
    UIManager.put("Table.showHorizontalLines", showDividers)
    UIManager.put("Table.showVerticalLines", false)
    UIManager.put("Table.intercellSpacing", Dimension(0, 1))
    UIManager.put("Table.rowHeight", UIScaleUtils.dip(22))
    UIManager.put("TableHeader.height", UIScaleUtils.dip(26))
    val tableCellMargins = Insets(
      UIScaleUtils.dip(2), UIScaleUtils.dip(6),
      UIScaleUtils.dip(2), UIScaleUtils.dip(6)
    )
    UIManager.put("TableHeader.cellMargins", tableCellMargins)
    UIManager.put("Table.cellMargins", tableCellMargins)
    UIManager.put("TableHeader.showTrailingVerticalLine", false)
    UIManager.put("TitlePane.unifiedBackground", true)
    UIManager.put("TitlePane.menuBarEmbedded", true)
  }

  /**
   * The set of all available theme names supported by the theme manager.
   */
  val availableThemes = allThemes.keys
}