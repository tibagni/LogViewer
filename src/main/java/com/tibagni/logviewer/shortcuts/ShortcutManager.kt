package com.tibagni.logviewer.shortcuts

import com.tibagni.logviewer.preferences.LogViewerPreferences
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.JMenuItem
import javax.swing.KeyStroke

/**
 * Single source of truth for resolving, formatting, serializing, and validating keyboard shortcuts.
 */
class ShortcutManager(
  private val userPrefs: LogViewerPreferences,
  private val isMacProvider: () -> Boolean = { isMacPlatform() }
) {

  val isMac: Boolean
    get() = isMacProvider()

  fun getDefaultKeyStroke(action: ShortcutAction): KeyStroke {
    return action.getDefaultKeyStroke(isMac)
  }

  fun getKeyStroke(action: ShortcutAction): KeyStroke {
    if (!action.isRebindable) {
      return getDefaultKeyStroke(action)
    }
    val customRaw = userPrefs.getCustomShortcut(action.id)
    if (!customRaw.isNullOrBlank()) {
      val parsed = deserializeKeyStroke(customRaw)
      if (parsed != null) {
        return parsed
      }
    }
    return getDefaultKeyStroke(action)
  }

  fun isCustomized(action: ShortcutAction): Boolean {
    if (!action.isRebindable) return false
    return getKeyStroke(action) != getDefaultKeyStroke(action)
  }

  fun setCustomKeyStroke(action: ShortcutAction, keyStroke: KeyStroke?) {
    if (!action.isRebindable) return
    val defaultKs = getDefaultKeyStroke(action)
    if (keyStroke == null || keyStroke == defaultKs) {
      userPrefs.setCustomShortcut(action.id, null)
    } else {
      userPrefs.setCustomShortcut(action.id, serializeKeyStroke(keyStroke))
    }
  }

  fun applyMappings(mappings: Map<ShortcutAction, KeyStroke>) {
    val serialized = mutableMapOf<String, String?>()
    for (action in ShortcutAction.values()) {
      if (!action.isRebindable) continue
      val defaultKs = getDefaultKeyStroke(action)
      val targetKs = mappings[action] ?: defaultKs
      serialized[action.id] = if (targetKs == defaultKs) null else serializeKeyStroke(targetKs)
    }
    userPrefs.applyCustomShortcuts(serialized)
  }

  fun resetAllToDefaults() {
    val cleared = ShortcutAction.values()
      .filter { it.isRebindable }
      .associate { it.id to null as String? }
    userPrefs.applyCustomShortcuts(cleared)
  }

  fun bindToMenuItem(menuItem: JMenuItem, action: ShortcutAction) {
    menuItem.accelerator = getKeyStroke(action)
  }

  /**
   * Checks whether [event] (`keyPressed` or `keyTyped`) matches the active keystroke of [action].
   */
  fun matches(event: KeyEvent, action: ShortcutAction): Boolean {
    val ks = getKeyStroke(action)
    return matchesKeyStroke(event, ks)
  }

  /**
   * Returns the single trigger character if [action]'s active keystroke is an unmodified printable key
   * (such as `/`, `,`, `.`, or a letter/digit), or `null` if [action] uses modifiers or a non-printable key.
   */
  fun getUnmodifiedTriggerChar(action: ShortcutAction): Char? {
    val ks = getKeyStroke(action)
    if ((ks.modifiers and MODIFIER_MASK) != 0) return null
    return keyCodeToChar(ks.keyCode)
  }

  /**
   * Finds all actions in [currentMappings] that conflict with assigning [candidateKeyStroke] to [candidateAction].
   */
  fun findConflicts(
    candidateAction: ShortcutAction,
    candidateKeyStroke: KeyStroke,
    currentMappings: Map<ShortcutAction, KeyStroke> = ShortcutAction.values().associateWith { getKeyStroke(it) }
  ): List<ShortcutAction> {
    return ShortcutAction.values().filter { other ->
      other != candidateAction &&
        candidateAction.scope.conflictsWith(other.scope) &&
        (currentMappings[other] ?: getKeyStroke(other)) == candidateKeyStroke
    }
  }

  companion object {
    const val MODIFIER_MASK: Int =
      InputEvent.META_DOWN_MASK or
        InputEvent.CTRL_DOWN_MASK or
        InputEvent.ALT_DOWN_MASK or
        InputEvent.SHIFT_DOWN_MASK

    @JvmStatic
    fun isMacPlatform(): Boolean {
      val osName = System.getProperty("os.name")?.lowercase() ?: ""
      return osName.contains("mac") || osName.contains("darwin")
    }

    @JvmStatic
    fun serializeKeyStroke(keyStroke: KeyStroke): String {
      val normalizedMods = keyStroke.modifiers and MODIFIER_MASK
      return "${keyStroke.keyCode}:$normalizedMods"
    }

    @JvmStatic
    fun deserializeKeyStroke(raw: String?): KeyStroke? {
      if (raw.isNullOrBlank()) return null
      val parts = raw.trim().split(":")
      if (parts.size == 2) {
        val keyCode = parts[0].toIntOrNull() ?: return null
        val modifiers = parts[1].toIntOrNull() ?: return null
        if (keyCode <= 0) return null
        return KeyStroke.getKeyStroke(keyCode, modifiers and MODIFIER_MASK)
      }
      return KeyStroke.getKeyStroke(raw)
    }

    @JvmStatic
    @JvmOverloads
    fun formatKeyStroke(keyStroke: KeyStroke, isMac: Boolean = isMacPlatform()): String {
      val mods = keyStroke.modifiers and MODIFIER_MASK
      val parts = mutableListOf<String>()
      if ((mods and InputEvent.META_DOWN_MASK) != 0) {
        parts.add(if (isMac) "Cmd" else "Meta")
      }
      if ((mods and InputEvent.CTRL_DOWN_MASK) != 0) {
        parts.add("Ctrl")
      }
      if ((mods and InputEvent.ALT_DOWN_MASK) != 0) {
        parts.add(if (isMac) "Opt" else "Alt")
      }
      if ((mods and InputEvent.SHIFT_DOWN_MASK) != 0) {
        parts.add("Shift")
      }
      parts.add(formatKeyCode(keyStroke.keyCode))
      return parts.joinToString("+")
    }

    @JvmStatic
    fun formatKeyCode(keyCode: Int): String {
      return when (keyCode) {
        KeyEvent.VK_COMMA -> ","
        KeyEvent.VK_PERIOD -> "."
        KeyEvent.VK_SLASH -> "/"
        KeyEvent.VK_SPACE -> "Space"
        KeyEvent.VK_ENTER -> "Enter"
        KeyEvent.VK_DELETE -> "Delete"
        KeyEvent.VK_BACK_SPACE -> "Backspace"
        KeyEvent.VK_ESCAPE -> "Esc"
        else -> KeyEvent.getKeyText(keyCode)
      }
    }

    @JvmStatic
    fun matchesKeyStroke(event: KeyEvent, ks: KeyStroke): Boolean {
      val ksMods = ks.modifiers and MODIFIER_MASK
      val eventMods = event.modifiersEx and MODIFIER_MASK
      if (event.id == KeyEvent.KEY_PRESSED) {
        if (event.keyCode == ks.keyCode && eventMods == ksMods) {
          return true
        }
        // Fallback for unmodified punctuation keys (/, ,, .) on international layouts
        if (ksMods == 0 && !event.isControlDown && !event.isMetaDown && !event.isAltDown) {
          val expectedChar = keyCodeToChar(ks.keyCode)
          if (expectedChar != null && event.keyChar == expectedChar) {
            return true
          }
        }
      } else if (event.id == KeyEvent.KEY_TYPED && ksMods == 0) {
        val expectedChar = keyCodeToChar(ks.keyCode)
        if (expectedChar != null && event.keyChar == expectedChar &&
          !event.isControlDown && !event.isMetaDown && !event.isAltDown
        ) {
          return true
        }
      }
      return false
    }

    @JvmStatic
    fun keyCodeToChar(keyCode: Int): Char? {
      return when (keyCode) {
        KeyEvent.VK_SLASH -> '/'
        KeyEvent.VK_COMMA -> ','
        KeyEvent.VK_PERIOD -> '.'
        in KeyEvent.VK_A..KeyEvent.VK_Z -> (keyCode - KeyEvent.VK_A + 'a'.code).toChar()
        in KeyEvent.VK_0..KeyEvent.VK_9 -> (keyCode - KeyEvent.VK_0 + '0'.code).toChar()
        else -> null
      }
    }

    @JvmStatic
    fun isModifierOnlyKey(keyCode: Int): Boolean {
      return keyCode == KeyEvent.VK_SHIFT ||
        keyCode == KeyEvent.VK_CONTROL ||
        keyCode == KeyEvent.VK_META ||
        keyCode == KeyEvent.VK_ALT ||
        keyCode == KeyEvent.VK_ALT_GRAPH ||
        keyCode == KeyEvent.VK_CAPS_LOCK ||
        keyCode == KeyEvent.VK_UNDEFINED
    }
  }
}
