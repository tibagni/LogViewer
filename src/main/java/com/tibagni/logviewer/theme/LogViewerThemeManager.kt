package com.tibagni.logviewer.theme

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.FlatLightLaf
import java.lang.IllegalArgumentException

object LogViewerThemeManager {
  private val LIGHT = LogViewerTheme("Light", false) { FlatLightLaf.install() }
  private val DARK = LogViewerTheme("Dark", true) { FlatDarkLaf.install() }
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
    FlatLaf.updateUILater()
  }

  val availableThemes = allThemes.keys
}