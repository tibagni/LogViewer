package com.tibagni.logviewer.util

/**
 * Tracks the location of a subsequence in a parent String. Used to avoid excessive copies
 * of Strings when processing large data. Use of StringView instead of creating substrings of the original string
 * is much more efficient as it will work on the original String only, no additional memory will be created
 *
 * This code is based on StringView.java from Squarespace Template Compiler
 * (https://github.com/Squarespace/template-compiler)
 */
class StringView(private val original: String, private val start: Int, private val end: Int) : CharSequence {
  private var _hashVal = 0

  // Let's not create a new String everytime this is invoked
  // and make sure to only really create a string representation
  // when absolutely necessary
  private val strRepr: String by lazy { original.substring(start, end) }

  override val length: Int
    get() = end - start

  fun subStringView(startIndex: Int, endIndex: Int) = StringView(original, start + startIndex, start + endIndex)

  fun subStringView(startIndex: Int) = StringView(original, start + startIndex, end)

  override fun get(index: Int) = original[start + index]

  override fun subSequence(startIndex: Int, endIndex: Int) = subStringView(startIndex, endIndex)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as StringView

    if (length != other.length) return false

    val s1: String = original
    val s2: String = other.original
    var start1 = start
    var start2: Int = other.start
    while (start1 < end && start2 < other.end) {
      if (s1[start1] != s2[start2]) {
        return false
      }
      start1++
      start2++
    }
    return true
  }

  override fun hashCode(): Int {
    if (_hashVal == 0) {
      var h = 0x01000193
      for (i in start until end) {
        h = 31 * h + original[i].toInt()
      }
      if (h == 0) {
        h++
      }
      _hashVal = h
    }
    return _hashVal
  }

  override fun toString(): String {
    return strRepr
  }
}