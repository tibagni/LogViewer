package com.tibagni.logviewer.util

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
