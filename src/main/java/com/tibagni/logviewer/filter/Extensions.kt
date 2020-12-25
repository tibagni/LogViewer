package com.tibagni.logviewer.filter

fun List<Filter>?.serialized(): List<String> {
  return this?.map { obj: Filter -> obj.serializeFilter() } ?: emptyList()
}