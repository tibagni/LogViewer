package com.tibagni.logviewer.log.parser

import com.tibagni.logviewer.ProgressReporter
import com.tibagni.logviewer.log.FileLogReader
import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.log.LogStream
import com.tibagni.logviewer.log.LogTimestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.system.measureTimeMillis

class RealWorldLogsVerificationTest {

  private val legacyLogStartPattern = "^\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.*"
  private val legacyTimestampPattern = Pattern.compile("^(\\d\\d)-(\\d\\d)\\s*(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d+)")

  private fun legacyIsLogLine(line: String): Boolean {
    return line.matches(Regex(legacyLogStartPattern))
  }

  private fun legacyFindTimestamp(logLine: String): LogTimestamp? {
    try {
      val matcher: Matcher = legacyTimestampPattern.matcher(logLine)
      if (matcher.find()) {
        return LogTimestamp(
          matcher.group(1).toInt(),
          matcher.group(2).toInt(),
          matcher.group(3).toInt(),
          matcher.group(4).toInt(),
          matcher.group(5).toInt(),
          matcher.group(6).toInt()
        )
      }
    } catch (e: Exception) {
      // ignore
    }
    return null
  }

  @Test
  fun testRealWorldLogsCompatibilityAndSpeed() {
    // TODO: Provide the directory or file path containing real-world logs to run this verification.
    // Pass via system property:
    //   ./gradlew test --tests RealWorldLogsVerificationTest -Dreal.world.logs.path=/path/to/logs
    // Or set the environment variable:
    //   REAL_WORLD_LOGS_PATH=/path/to/logs
    val logsPath = System.getProperty("real.world.logs.path")
      ?: System.getenv("REAL_WORLD_LOGS_PATH")

    org.junit.Assume.assumeTrue(
      "Skipping real-world logs test: no path provided. " +
          "Run with -Dreal.world.logs.path=/path/to/logs or set REAL_WORLD_LOGS_PATH",
      !logsPath.isNullOrBlank()
    )

    val root = File(logsPath!!)
    org.junit.Assume.assumeTrue("Log path does not exist: $logsPath", root.exists())

    val realFiles = if (root.isFile) {
      listOf(root)
    } else {
      root.walkTopDown()
        .filter { it.isFile && (it.extension == "txt" || it.extension == "log") }
        .toList()
    }

    org.junit.Assume.assumeTrue("No .txt or .log files found in $logsPath", realFiles.isNotEmpty())

    val parser = LogParser(mock(FileLogReader::class.java), mock(ProgressReporter::class.java))

    println("=================================================================")
    println("PHASE 1: LINE-BY-LINE MATCHING CAPABILITY & COMPATIBILITY CHECK")
    println("=================================================================")

    var totalLinesTested = 0L
    var totalLogLinesMatched = 0L
    var totalNonLogLines = 0L
    var totalTimestampsChecked = 0L

    for (file in realFiles) {
      val fileName = file.name
      val fileSizeMb = file.length() / (1024.0 * 1024.0)
      var fileLines = 0L
      var fileLogLines = 0L

      file.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
        for (line in lines) {
          totalLinesTested++
          fileLines++

          val legacyMatch = legacyIsLogLine(line)
          val newMatch = parser.isLogLine(line)

          if (legacyMatch != newMatch) {
            throw AssertionError(
              "CRITICAL MISMATCH in $fileName at line $fileLines!\n" +
                  "Line: \"$line\"\n" +
                  "Legacy isLogLine: $legacyMatch, New isLogLine: $newMatch"
            )
          }

          if (newMatch) {
            totalLogLinesMatched++
            fileLogLines++

            val legacyTs = legacyFindTimestamp(line)
            val newTs = parser.findTimestamp(line)

            assertEquals(
              "Timestamp mismatch in $fileName at line $fileLines for: \"$line\"",
              legacyTs,
              newTs
            )
            totalTimestampsChecked++
          } else {
            totalNonLogLines++
          }
        }
      }

      println(String.format("Verified %-20s (%.1f MB): %,8d lines, %,8d log entries (100%% match)",
        fileName, fileSizeMb, fileLines, fileLogLines))
    }

    println("-----------------------------------------------------------------")
    println(String.format("TOTAL LINES TESTED       : %,d", totalLinesTested))
    println(String.format("LOG ENTRIES MATCHED      : %,d (100%% MATCH WITH LEGACY REGEX)", totalLogLinesMatched))
    println(String.format("NON-LOG / CONTINUATIONS  : %,d (100%% MATCH WITH LEGACY REGEX)", totalNonLogLines))
    println(String.format("TIMESTAMPS VERIFIED      : %,d (100%% EXACT EQUALITY)", totalTimestampsChecked))
    println("=================================================================\n")

    println("=================================================================")
    println("PHASE 2: REAL-WORLD END-TO-END LOAD & SPEED PROGRESS BENCHMARK")
    println("=================================================================")

    // Benchmark 1: Largest single file discovered
    val mainFile = realFiles.maxByOrNull { it.length() } ?: realFiles.first()
    println("Testing single file: ${mainFile.name} (${String.format("%.1f", mainFile.length() / (1024.0 * 1024.0))} MB)")

    // Measure legacy speed on mainFile
    val rawText = mainFile.readText(StandardCharsets.UTF_8)
    val legacyTime = measureLegacyParse(rawText, mainFile.name, parser)

    // Measure new speed on mainFile
    val newTime = measureTimeMillis {
      val reader = FileLogReader(arrayOf(mainFile))
      val pr = mock(ProgressReporter::class.java)
      val p = LogParser(reader, pr)
      val entries = p.parseLogs(StandardCharsets.UTF_8)
      assertTrue(entries.isNotEmpty())
    }

    val speedupSingle = legacyTime.toDouble() / newTime.coerceAtLeast(1)
    println(String.format("Single file - Legacy: %,d ms | Optimized: %,d ms | Speedup: %.1fx",
      legacyTime, newTime, speedupSingle))

    // Benchmark 2: Multi-file bundle (take up to 5 files if available)
    if (realFiles.size > 1) {
      val combinedSet = realFiles.take(5)
      val totalSizeMb = combinedSet.sumOf { it.length() } / (1024.0 * 1024.0)
      println(String.format("\nTesting multi-file bundle (%d files, %.1f MB total):", combinedSet.size, totalSizeMb))

      val multiFileTime = measureTimeMillis {
        val reader = FileLogReader(combinedSet.toTypedArray())
        val pr = mock(ProgressReporter::class.java)
        val p = LogParser(reader, pr)
        val entries = p.parseLogs(StandardCharsets.UTF_8)

        println(String.format("Parsed %,d total merged entries", entries.size))

        // Verify sorted order
        for (i in 0 until entries.size - 1) {
          val curr = entries[i].timestamp
          val next = entries[i + 1].timestamp
          if (curr != null && next != null) {
            assertTrue("Entries must be sorted by timestamp", curr <= next)
          }
        }
      }

      println(String.format("Multi-file bundle total load & parse time: %,d ms (%.1f MB/sec)",
        multiFileTime, totalSizeMb / (multiFileTime / 1000.0)))
    }
    println("=================================================================")
  }

  private fun measureLegacyParse(logText: String, logPath: String, parser: LogParser): Long {
    return measureTimeMillis {
      val lines = logText.split(System.lineSeparator())
      val logLines = mutableListOf<String>()
      var currentLogLine: java.lang.StringBuilder? = null

      for (line in lines) {
        if (legacyIsLogLine(line)) {
          if (currentLogLine != null) {
            logLines.add(currentLogLine.toString())
          }
          currentLogLine = java.lang.StringBuilder(line)
        } else if (currentLogLine != null) {
          currentLogLine.append(System.lineSeparator()).append(line)
        }
      }
      if (currentLogLine != null) {
        logLines.add(currentLogLine.toString())
      }

      for (entryText in logLines) {
        legacyFindTimestamp(entryText)
        parser.findLogLevel(entryText)
        LogStream.inferLogStreamFromName(logPath)
      }
    }
  }
}
