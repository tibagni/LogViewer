package com.tibagni.logviewer.adb

import java.io.IOException

data class CommandResult(
  val exitCode: Int,
  val output: String,
  val error: String
) {
  val isSuccess: Boolean get() = exitCode == 0
}

interface CommandRunner {
  @Throws(IOException::class, InterruptedException::class)
  fun exec(command: List<String>): CommandResult

  @Throws(IOException::class)
  fun startProcess(command: List<String>): Process
}

class DefaultCommandRunner : CommandRunner {
  @Throws(IOException::class, InterruptedException::class)
  override fun exec(command: List<String>): CommandResult {
    val process = ProcessBuilder(command).start()
    val output = process.inputStream.bufferedReader().use { it.readText() }
    val error = process.errorStream.bufferedReader().use { it.readText() }
    val exitCode = process.waitFor()
    return CommandResult(exitCode, output, error)
  }

  @Throws(IOException::class)
  override fun startProcess(command: List<String>): Process {
    return ProcessBuilder(command).start()
  }
}
