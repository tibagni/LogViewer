package com.tibagni.logviewer.util

fun interface IFileHandle {
  fun handle(sliceIndex: Int, line: String)
}