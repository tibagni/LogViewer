package com.tibagni.logviewer

import com.tibagni.logviewer.util.StringView

fun <T> MutableList<T>.reset(otherCollection: Collection<T>) {
  this.clear()
  this.addAll(otherCollection)
}

fun <T> MutableList<T>.reset(elementsArray: Array<T>) {
  this.clear()
  this.addAll(elementsArray)
}

fun <T> HashSet<T>.reset(otherCollection: Collection<T>?) {
  this.clear()
  if (otherCollection != null) {
    this.addAll(otherCollection)
  }
}

fun <T> HashSet<T>.reset(elementsArray: Array<T>?) {
  this.clear()
  if (elementsArray != null) {
    this.addAll(elementsArray)
  }
}

fun <K, V> MutableMap<K, V>.reset(otherMap: Map<K, V>) {
  this.clear()
  this.putAll(otherMap)
}

fun String.stringView(start: Int, end: Int) = StringView(this, start, end)

fun <T> List<T>.getOrNull(index: Int): T? {
  if (this.isEmpty()) return null
  if (index < 0) return null
  if (index >= this.size) return null

  return this[index]
}