package com.tibagni.logviewer

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