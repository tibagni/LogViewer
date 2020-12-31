package com.tibagni.logviewer.bugreport.content

/**
 * Represents a single package in the "Packages" sections of a bug report
 * Contains information about the application package such as package name, version, data dir and more
 */
data class AppPackage(
  val packageName: CharSequence,
  val versionCode: CharSequence,
  val versionName: CharSequence,
  val dataDir: CharSequence,
  val rawText: CharSequence
) {
  fun toSummary(): String {
    return "$packageName | v$versionName ($versionCode)"
  }
}