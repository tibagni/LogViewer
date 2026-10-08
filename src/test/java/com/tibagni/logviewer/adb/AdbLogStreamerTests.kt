package com.tibagni.logviewer.adb

import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.log.LogTimestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.*
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class AdbLogStreamerTests {

  private class FakeProcess(
    private val inputStreamData: InputStream,
    private val errorStreamData: InputStream = ByteArrayInputStream(ByteArray(0))
  ) : Process() {
    var isDestroyed = false
      private set
    var isDestroyedForcibly = false
      private set

    override fun getOutputStream(): OutputStream = ByteArrayOutputStream()
    override fun getInputStream(): InputStream = inputStreamData
    override fun getErrorStream(): InputStream = errorStreamData
    override fun waitFor(): Int = 0
    override fun exitValue(): Int = 0
    override fun destroy() {
      isDestroyed = true
    }
    override fun destroyForcibly(): Process {
      isDestroyedForcibly = true
      return this
    }
    override fun isAlive(): Boolean = !isDestroyed && !isDestroyedForcibly
  }

  private class FakeCommandRunner(
    private val processSupplier: (List<String>) -> Process
  ) : CommandRunner {
    val executedCommands = mutableListOf<List<String>>()

    override fun exec(command: List<String>): CommandResult {
      executedCommands.add(command)
      return CommandResult(0, "", "")
    }

    override fun startProcess(command: List<String>): Process {
      executedCommands.add(command)
      return processSupplier(command)
    }
  }

  private class FakeAdbManager(
    private val adbPath: File? = File("/usr/bin/adb")
  ) : AdbManager {
    override val isAdbAvailable: Boolean get() = adbPath != null
    override fun getAdbExecutable(): File? = adbPath
    override fun setCustomAdbPath(path: File?) {}
    override suspend fun getConnectedDevices(): List<AdbDevice> = emptyList()
    override suspend fun restartServer(): Boolean = true
    override fun launchDeviceTerminal(device: AdbDevice): Boolean = true
  }

  @Test
  fun testStreamEmitsSingleLogLines() = runBlocking {
    val logOutput = """
      05-12 13:45:26.123  1234  5678 D Tag1: First line
      05-12 13:45:27.456  1234  5678 I Tag2: Second line
    """.trimIndent() + "\n"

    val fakeProcess = FakeProcess(logOutput.byteInputStream())
    val runner = FakeCommandRunner { fakeProcess }
    val streamer = AdbLogStreamerImpl(FakeAdbManager(), runner, continuationTimeout = 10.milliseconds)

    val entries = streamer.startStream("device123").toList()

    assertEquals(2, entries.size)
    assertEquals("05-12 13:45:26.123  1234  5678 D Tag1: First line", entries[0].logText)
    assertEquals(LogLevel.DEBUG, entries[0].logLevel)
    assertEquals(LogTimestamp(5, 12, 13, 45, 26, 123), entries[0].timestamp)

    assertEquals("05-12 13:45:27.456  1234  5678 I Tag2: Second line", entries[1].logText)
    assertEquals(LogLevel.INFO, entries[1].logLevel)
    assertEquals(LogTimestamp(5, 12, 13, 45, 27, 456), entries[1].timestamp)
  }

  @Test
  fun testStreamCombinesMultiLineContinuations() = runBlocking {
    val logOutput = """
      05-12 13:45:26.123  1234  5678 E AndroidRuntime: FATAL EXCEPTION
          at com.example.MainActivity.onCreate(MainActivity.kt:10)
          at android.app.Activity.performCreate(Activity.java:100)
      05-12 13:45:28.000  1234  5678 I ActivityManager: Process finished
    """.trimIndent() + "\n"

    val fakeProcess = FakeProcess(logOutput.byteInputStream())
    val runner = FakeCommandRunner { fakeProcess }
    val streamer = AdbLogStreamerImpl(FakeAdbManager(), runner, continuationTimeout = 10.milliseconds)

    val entries = streamer.startStream("device123").toList()

    assertEquals(2, entries.size)
    assertEquals(LogLevel.ERROR, entries[0].logLevel)
    assertTrue(entries[0].logText.contains("FATAL EXCEPTION"))
    assertTrue(entries[0].logText.contains("at com.example.MainActivity.onCreate"))
    assertTrue(entries[0].logText.contains("at android.app.Activity.performCreate"))

    assertEquals(LogLevel.INFO, entries[1].logLevel)
    assertEquals("05-12 13:45:28.000  1234  5678 I ActivityManager: Process finished", entries[1].logText)
  }

  @Test
  fun testStreamIgnoresHeaderLines() = runBlocking {
    val logOutput = """
      --------- beginning of main
      05-12 13:45:26.123  1234  5678 D Tag: Log line
      --------- beginning of system
      05-12 13:45:27.123  1234  5678 I Tag: Another line
    """.trimIndent() + "\n"

    val fakeProcess = FakeProcess(logOutput.byteInputStream())
    val runner = FakeCommandRunner { fakeProcess }
    val streamer = AdbLogStreamerImpl(FakeAdbManager(), runner, continuationTimeout = 10.milliseconds)

    val entries = streamer.startStream("").toList()

    assertEquals(2, entries.size)
    assertEquals("05-12 13:45:26.123  1234  5678 D Tag: Log line", entries[0].logText)
    assertEquals("05-12 13:45:27.123  1234  5678 I Tag: Another line", entries[1].logText)
  }

  @Test
  fun testStreamCommandIncludesSerialWhenProvided() = runBlocking {
    val fakeProcess = FakeProcess("05-12 13:45:26.123  1234  5678 D Tag: Line\n".byteInputStream())
    val runner = FakeCommandRunner { fakeProcess }
    val streamer = AdbLogStreamerImpl(FakeAdbManager(File("/custom/adb")), runner, continuationTimeout = 10.milliseconds)

    streamer.startStream("deviceXYZ").toList()

    assertEquals(1, runner.executedCommands.size)
    assertEquals(
      listOf("/custom/adb", "-s", "deviceXYZ", "logcat", "-v", "threadtime"),
      runner.executedCommands[0]
    )
  }

  @Test
  fun testStreamCommandOmitsSerialWhenBlank() = runBlocking {
    val fakeProcess = FakeProcess("05-12 13:45:26.123  1234  5678 D Tag: Line\n".byteInputStream())
    val runner = FakeCommandRunner { fakeProcess }
    val streamer = AdbLogStreamerImpl(FakeAdbManager(File("/custom/adb")), runner, continuationTimeout = 10.milliseconds)

    streamer.startStream("").toList()

    assertEquals(1, runner.executedCommands.size)
    assertEquals(
      listOf("/custom/adb", "logcat", "-v", "threadtime"),
      runner.executedCommands[0]
    )
  }

  @Test
  fun testStreamThrowsWhenAdbNotFound() = runBlocking {
    val runner = FakeCommandRunner { FakeProcess("".byteInputStream()) }
    val streamer = AdbLogStreamerImpl(FakeAdbManager(null), runner)

    try {
      streamer.startStream("device123").toList()
      fail("Expected IllegalStateException")
    } catch (e: IllegalStateException) {
      assertEquals("ADB executable not found", e.message)
    }
  }

  @Test
  fun testStopStreamKillsProcessAndResetsState() = runBlocking {
    val fakeProcess = FakeProcess("05-12 13:45:26.123  1234  5678 D Tag: Line 1\n".byteInputStream())
    val runner = FakeCommandRunner { fakeProcess }
    val streamer = AdbLogStreamerImpl(FakeAdbManager(), runner, continuationTimeout = 10.milliseconds)

    val entries = streamer.startStream("device123").take(1).toList()

    assertEquals(1, entries.size)
    assertTrue(fakeProcess.isDestroyed || fakeProcess.isDestroyedForcibly)
    assertFalse(streamer.isStreaming)
  }

  @Test
  fun testManualStopStream() {
    val fakeProcess = FakeProcess(ByteArrayInputStream(ByteArray(0)))
    val runner = FakeCommandRunner { fakeProcess }
    val streamer = AdbLogStreamerImpl(FakeAdbManager(), runner)

    // stopStream on idle streamer should not throw
    assertFalse(streamer.isStreaming)
    streamer.stopStream()
    assertFalse(streamer.isStreaming)
  }
}
