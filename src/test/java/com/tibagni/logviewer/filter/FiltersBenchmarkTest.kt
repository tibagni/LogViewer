package com.tibagni.logviewer.filter

import com.tibagni.logviewer.ProgressReporter
import com.tibagni.logviewer.log.LogEntry
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.log.LogStream
import com.tibagni.logviewer.log.LogTimestamp
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mockito.mock
import java.awt.Color
import kotlin.system.measureTimeMillis

class FiltersBenchmarkTest {

  @Test
  @Ignore("Benchmark test for manual performance evaluation")
  fun benchmarkApplyFilters() {
    val filters = arrayOf(
      Filter("ActivityManager", "ActivityManager", Color.BLUE, LogLevel.INFO),
      Filter("AudioFlinger", "AudioFlinger", Color.GREEN, LogLevel.DEBUG),
      Filter("Broadcast", "Broadcasting: Intent", Color.RED, LogLevel.VERBOSE),
      Filter("Exception", "Exception", Color.RED, LogLevel.ERROR),
      Filter("Telephony", "TelephonyRegistry", Color.ORANGE, LogLevel.DEBUG),
      Filter("WiFi", "WifiService", Color.CYAN, LogLevel.VERBOSE),
      Filter("Camera", "CameraService", Color.MAGENTA, LogLevel.INFO),
      Filter("Bluetooth", "BluetoothManagerService", Color.PINK, LogLevel.VERBOSE),
      Filter("SurfaceFlinger", "SurfaceFlinger", Color.YELLOW, LogLevel.WARNING),
      Filter("RegexPid", "pid=\\d+", Color.LIGHT_GRAY, LogLevel.DEBUG)
    )

    val levels = LogLevel.values()
    val sampleTexts = listOf(
      "05-12 13:45:10.123  1000  1000 I ActivityManager: Start proc 12345:com.android.settings/u0a100 for activity",
      "05-12 13:45:10.124  1000  1000 D AudioFlinger: MixerThread 0x1234 active tracks: 2",
      "05-12 13:45:10.125  1000  1000 V ActivityManager: Broadcasting: Intent { act=android.intent.action.BATTERY_CHANGED }",
      "05-12 13:45:10.126  1000  1000 E System: java.lang.NullPointerException: Null pointer Exception in service",
      "05-12 13:45:10.127  1000  1000 D TelephonyRegistry: notifyServiceStateForSubscriber: subId=1 state=0",
      "05-12 13:45:10.128  1000  1000 V WifiService: getWifiEnabledState() uid=1000 pid=12345",
      "05-12 13:45:10.129  1000  1000 I CameraService: CameraService::connect call (PID 12345)",
      "05-12 13:45:10.130  1000  1000 V BluetoothManagerService: Message: 1",
      "05-12 13:45:10.131  1000  1000 W SurfaceFlinger: Timed out waiting for previous vsync",
      "05-12 13:45:10.132  1000  1000 D Regular: Just a normal boring verbose log line that does not match anything at all"
    )

    val numLines = 300_000
    val entries = ArrayList<LogEntry>(numLines)
    for (i in 0 until numLines) {
      val template = sampleTexts[i % sampleTexts.size]
      val level = levels[i % levels.size]
      entries.add(LogEntry(template, level, LogTimestamp(5, 12, 13, 45, 10, i % 1000), "main"))
    }

    val pr = mock(ProgressReporter::class.java)

    // Warm-up
    Filters.applyMultipleFilters(entries.subList(0, 10_000), filters, pr)

    // Run benchmark 3 times
    val times = (1..3).map {
      measureTimeMillis {
        val result = Filters.applyMultipleFilters(entries, filters, pr)
        println("Filtered ${result.size} matches")
      }
    }

    println("=== Benchmark Results (300k lines, 10 filters) ===")
    times.forEachIndexed { idx, t -> println("Run ${idx + 1}: ${t}ms") }
    println("Average: ${times.average()}ms")
  }
}
