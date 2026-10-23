package com.tibagni.logviewer.tools

import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.shortcuts.ShortcutAction
import com.tibagni.logviewer.shortcuts.ShortcutManager
import java.io.File
import java.util.Locale

/**
 * Programmatically generates and updates the Markdown Keyboard Shortcuts table in `README.md`
 * from [ShortcutAction], ensuring documentation never drifts from the codebase.
 */
object ShortcutReadmeGenerator {

  const val SECTION_HEADER = "## ⌨ Keyboard Shortcuts"

  fun generateMarkdownTable(): String {
    val previousLocale = I18n.getLocale()
    return try {
      I18n.setLocale(Locale.US)
      val sb = StringBuilder()
      sb.appendLine("| Category | Shortcut (macOS / Linux & Windows) | Action |")
      sb.appendLine("| :--- | :--- | :--- |")
      for (action in ShortcutAction.values()) {
        val category = action.category.displayName
        val shortcutCol = formatReadmeShortcutColumn(action)
        val actionDesc = action.displayName
        sb.appendLine("| **$category** | $shortcutCol | $actionDesc |")
      }
      sb.toString().trimEnd()
    } finally {
      I18n.setLocale(previousLocale)
    }
  }

  fun formatReadmeShortcutColumn(action: ShortcutAction): String {
    action.readmeShortcutOverride?.let { return it }
    val macStr = ShortcutManager.formatKeyStroke(action.defaultMacKeyStroke, isMac = true)
    val winStr = ShortcutManager.formatKeyStroke(action.defaultWinLinuxKeyStroke, isMac = false)
    return if (macStr == winStr) {
      "`$macStr`"
    } else {
      "`$macStr` / `$winStr`"
    }
  }

  /**
   * Extracts the markdown table under `## ⌨ Keyboard Shortcuts` from [readmeContent].
   */
  fun extractTableFromReadme(readmeContent: String): String? {
    val lines = readmeContent.lines()
    val headerIdx = lines.indexOfFirst { it.trim() == SECTION_HEADER }
    if (headerIdx < 0) return null

    val tableLines = mutableListOf<String>()
    var inTable = false
    for (i in (headerIdx + 1) until lines.size) {
      val line = lines[i].trim()
      if (line.startsWith("|")) {
        inTable = true
        tableLines.add(line)
      } else if (inTable) {
        break
      } else if (line.startsWith("## ") || line.startsWith("---")) {
        break
      }
    }
    return if (tableLines.isNotEmpty()) tableLines.joinToString("\n") else null
  }

  /**
   * Replaces the Keyboard Shortcuts table in [readmeFile] with [generateMarkdownTable].
   */
  fun updateReadmeFile(readmeFile: File) {
    val content = readmeFile.readText()
    val currentTable = extractTableFromReadme(content)
      ?: error("Could not locate '$SECTION_HEADER' table in ${readmeFile.absolutePath}")
    val updatedContent = content.replace(currentTable, generateMarkdownTable())
    readmeFile.writeText(updatedContent)
  }

  /**
   * Validates that the Keyboard Shortcuts table in [readmeFile] matches [generateMarkdownTable].
   * Returns `true` if up to date, or `false` if missing or out of sync.
   */
  fun checkReadmeFile(readmeFile: File): Boolean {
    if (!readmeFile.exists()) return false
    val currentTable = extractTableFromReadme(readmeFile.readText()) ?: return false
    return currentTable.trim() == generateMarkdownTable().trim()
  }

  @JvmStatic
  fun main(args: Array<String>) {
    val checkOnly = args.contains("--check")
    val pathArg = args.firstOrNull { it != "--check" } ?: "README.md"
    val readmeFile = File(pathArg)
    if (checkOnly) {
      if (!checkReadmeFile(readmeFile)) {
        System.err.println(
          "❌ Keyboard shortcuts table in ${readmeFile.name} is out of date with ShortcutAction definitions.\n" +
            "   Run './gradlew updateReadmeShortcuts' to regenerate it."
        )
        kotlin.system.exitProcess(1)
      }
      println("✅ Keyboard shortcuts table in ${readmeFile.name} is up to date.")
    } else {
      updateReadmeFile(readmeFile)
      println("Updated keyboard shortcuts table in ${readmeFile.absolutePath}")
    }
  }
}
