package com.tibagni.logviewer.adb

import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogLineParser
import com.tibagni.logviewer.logger.Logger
import com.tibagni.logviewer.util.PlatformUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

interface AdbLogStreamer {
  val isStreaming: Boolean
  val isStreamingFlow: StateFlow<Boolean>
  fun startStream(serialNumber: String): Flow<LogEntry>
  fun stopStream()
}

class AdbLogStreamerImpl(
  private val adbManager: AdbManager = ServiceLocator.adbManager,
  private val commandRunner: CommandRunner = DefaultCommandRunner(),
  private val continuationTimeout: Duration = DEFAULT_CONTINUATION_TIMEOUT,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AdbLogStreamer {

  private val _isStreaming = MutableStateFlow(false)
  override val isStreaming: Boolean get() = _isStreaming.value
  override val isStreamingFlow: StateFlow<Boolean> = _isStreaming.asStateFlow()

  private var currentProcess: Process? = null

  override fun startStream(serialNumber: String): Flow<LogEntry> = channelFlow {
    stopStream()

    val adbExecutable = adbManager.getAdbExecutable()
      ?: throw IllegalStateException("ADB executable not found")

    val cmd = mutableListOf(adbExecutable.absolutePath)
    if (serialNumber.isNotBlank()) {
      cmd.addAll(listOf("-s", serialNumber))
    }
    cmd.addAll(listOf("logcat", "-v", "threadtime"))

    val process = commandRunner.startProcess(cmd)
    synchronized(this@AdbLogStreamerImpl) {
      currentProcess = process
      _isStreaming.value = true
    }

    val lineChannel = Channel<String>(capacity = 2048)

    val readerJob = launch(ioDispatcher) {
      try {
        // In java.lang.Process, inputStream represents stdout of the child process
        process.inputStream.bufferedReader().use { reader ->
          while (isActive) {
            val line = reader.readLine() ?: break
            lineChannel.send(line)
          }
        }
      } catch (ignored: IOException) {
        // Stream closed or process terminated
      } finally {
        lineChannel.close()
      }
    }

    var currentLine: StringBuilder? = null
    var currentLineNumber = 0
    var rawLineNumber = 0

    suspend fun flushCurrent() {
      val pending = currentLine?.toString()
      if (pending != null) {
        send(LogLineParser.createLogEntry(pending, currentLineNumber))
        currentLine = null
      }
    }

    try {
      while (isActive) {
        val result = if (currentLine != null) {
          withTimeoutOrNull(continuationTimeout) {
            lineChannel.receiveCatching()
          }
        } else {
          lineChannel.receiveCatching()
        }

        if (result == null) {
          flushCurrent()
          continue
        }

        if (result.isClosed) {
          flushCurrent()
          break
        }

        val nextLine = result.getOrThrow()
        rawLineNumber++
        if (LogLineParser.isLogLine(nextLine)) {
          flushCurrent()
          currentLine = StringBuilder(nextLine)
          currentLineNumber = rawLineNumber
        } else if (!LogLineParser.shouldIgnoreLine(nextLine)) {
          currentLine?.append(PlatformUtils.lineSeparator)?.append(nextLine)
        }
      }
      flushCurrent()
    } finally {
      readerJob.cancel()
      stopStream()
    }
  }

  override fun stopStream() {
    synchronized(this) {
      currentProcess?.let { proc ->
        try {
          proc.destroy()
          if (!proc.waitFor(100, TimeUnit.MILLISECONDS)) {
            proc.destroyForcibly()
          }
        } catch (e: Exception) {
          Logger.error("Failed to destroy adb process", e)
        }
      }
      currentProcess = null
      _isStreaming.value = false
    }
  }

  companion object {
    val DEFAULT_CONTINUATION_TIMEOUT: Duration = 50.milliseconds
  }
}
