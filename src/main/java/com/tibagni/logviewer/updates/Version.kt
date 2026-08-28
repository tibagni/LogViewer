package com.tibagni.logviewer.updates

/**
 * A robust, semantic-aware version representation for LogViewer application.
 *
 * Implements [Comparable] to support direct relational comparisons natively in Kotlin (e.g., `latest > current`).
 * Decouples version checks from raw floating-point calculations to resolve R6 (Primitive Obsession).
 *
 * Pre-conditions:
 * - The input [versionStr] must match [VERSION_REGEX] (validated via [isValid]).
 *
 * Post-conditions:
 * - Guarantees [cleanString] and [parts] are populated with numeric, comparable segments.
 *
 * @param versionStr The raw tag string to parse (e.g., "2.7", "v2.7.1", "2.7-beta").
 * @throws IllegalArgumentException If the [versionStr] has an invalid format.
 */
class Version(versionStr: String) : Comparable<Version> {
  val originalString: String = versionStr.trim()
  val cleanString: String
  val parts: List<Int>

  companion object {
    private val VERSION_REGEX = Regex("^[vV]?[0-9]+(\\.[0-9]+)*(-[a-zA-Z0-9.]+)?$")

    /**
     * Checks if a given version string matches standard semantic or numeric patterns.
     *
     * @param versionStr The version string to check.
     * @return True if the string is structurally valid, false otherwise.
     */
    fun isValid(versionStr: String): Boolean {
      return VERSION_REGEX.matches(versionStr.trim())
    }
  }

  init {
    if (!isValid(originalString)) {
      throw IllegalArgumentException("Invalid version format: $versionStr")
    }

    // Strip "v" or "V" prefix if present
    var s = originalString
    if (s.startsWith("v", ignoreCase = true)) {
      s = s.substring(1).trim()
    }

    // Isolate the numeric portion by discarding any trailing pre-release tags (e.g., -beta)
    val mainVersion = s.substringBefore('-')
    cleanString = mainVersion

    // Split numeric version segments and parse to integer parts
    parts = mainVersion.split('.')
      .map { part ->
        part.toIntOrNull() ?: 0
      }
  }

  /**
   * Compares this version with [other] version sequentially segment by segment.
   *
   * Invariants:
   * - Trailing zeroes are treated as equivalent to absent segments (e.g., `2.7` is equivalent to `2.7.0`).
   * - Suffixes (like `-beta`) are omitted during comparative rank resolution.
   *
   * @param other The other version instance to compare against.
   * @return A negative integer, zero, or a positive integer as this version is less than,
   *         equal to, or greater than [other].
   */
  override fun compareTo(other: Version): Int {
    val maxParts = maxOf(this.parts.size, other.parts.size)
    for (i in 0 until maxParts) {
      val thisPart = this.parts.getOrElse(i) { 0 }
      val otherPart = other.parts.getOrElse(i) { 0 }
      if (thisPart != otherPart) {
        return thisPart.compareTo(otherPart)
      }
    }
    return 0
  }

  /**
   * Evaluates if this version is equivalent in comparative rank to [other].
   */
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Version) return false
    return this.compareTo(other) == 0
  }

  /**
   * Generates a stable hash code based on normalized numeric segments (discarding trailing zeroes).
   */
  override fun hashCode(): Int {
    val normalizedParts = parts.dropLastWhile { it == 0 }
    return normalizedParts.hashCode()
  }

  override fun toString(): String {
    return originalString
  }
}
