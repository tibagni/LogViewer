package com.tibagni.logviewer.util

import java.awt.event.InputEvent

object PlatformUtils {
  var propertyProvider: (String) -> String? = System::getProperty
  var envProvider: (String) -> String? = System::getenv

  @JvmStatic
  val isWindows: Boolean
    get() = (propertyProvider("os.name")?.lowercase() ?: "").contains("win")

  @JvmStatic
  val isMac: Boolean
    get() = (propertyProvider("os.name")?.lowercase() ?: "").contains("mac")

  @JvmStatic
  val isLinux: Boolean
    get() = !isWindows && !isMac

  /**
   * Returns the platform-appropriate menu shortcut modifier mask ([InputEvent.META_DOWN_MASK] on
   * macOS, [InputEvent.CTRL_DOWN_MASK] on other platforms), safe for headless environments.
   */
  @JvmStatic
  val menuShortcutMask: Int
    get() = if (isMac) InputEvent.META_DOWN_MASK else InputEvent.CTRL_DOWN_MASK

  @JvmStatic
  val userHome: String
    get() = propertyProvider("user.home") ?: ""

  @JvmStatic
  val lineSeparator: String
    get() = propertyProvider("line.separator") ?: "\n"

  @JvmStatic
  fun getEnv(name: String): String? = envProvider(name)

  @JvmStatic
  fun getSystemProperty(key: String): String? = propertyProvider(key)

  @JvmStatic
  fun reset() {
    propertyProvider = System::getProperty
    envProvider = System::getenv
  }
}
