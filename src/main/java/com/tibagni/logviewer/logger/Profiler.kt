package com.tibagni.logviewer.logger

class Profiler(private val scopeName: String) {
  private var start = 0L

  fun begin() {
    start = System.currentTimeMillis()
  }

  fun end() {
    val total = System.currentTimeMillis() - start
    Logger.debug("---- PROFILER ---- { $scopeName } took $total ms")
  }
}

fun <T> wrapProfiler(scopeName: String, execution: () -> T): T {
  if (!Logger.isDebugLevel()) {
    return execution()
  }

  val profiler = Profiler(scopeName).also { it.begin() }
  val result = execution()
  profiler.end()

  return result
}