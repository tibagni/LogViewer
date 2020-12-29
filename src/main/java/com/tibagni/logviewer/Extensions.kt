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

fun <T> HashSet<T>.reset(otherCollection: Collection<T>) {
    this.clear()
    this.addAll(otherCollection)
}

fun <T> HashSet<T>.reset(elementsArray: Array<T>) {
    this.clear()
    this.addAll(elementsArray)
}

fun String.stringView(start: Int, end: Int) = StringView(this, start, end)

fun <T> List<T>.getOrNull(index: Int): T? {
    if (this.isEmpty()) return null
    if (index < 0) return null
    if (index >= this.size) return null

    return this[index]
}