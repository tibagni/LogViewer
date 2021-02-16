package com.tibagni.logviewer.logger

fun <T> wrapProfiler(scopeName: String, execution: () -> T): T {
  val start = System.currentTimeMillis()
  val result = execution()
  val total = System.currentTimeMillis() - start
  Logger.debug("---- PROFILER ---- { $scopeName } took $total ms")

  return result
}