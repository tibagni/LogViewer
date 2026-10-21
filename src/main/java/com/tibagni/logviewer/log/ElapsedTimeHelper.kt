package com.tibagni.logviewer.log

import com.tibagni.logviewer.i18n.I18n
import java.time.Duration
import java.time.LocalDateTime
import java.util.Locale

data class DurationResult(
  val duration: Duration,
  val isNegative: Boolean
) {
  val totalMillis: Long = duration.toMillis()
}

object ElapsedTimeHelper {

  /**
   * Calculates the elapsed time [DurationResult] between two [LogTimestamp] instances.
   * If parsing or date construction fails, returns null.
   */
  fun calculateDuration(t1: LogTimestamp, t2: LogTimestamp): DurationResult? {
    return try {
      val refYear = 2024 // Leap year to support Feb 29
      val nanos1 = toNanos(t1.hundredth)
      val dt1 = LocalDateTime.of(refYear, t1.month, t1.day, t1.hour, t1.minutes, t1.seconds, nanos1.toInt())

      // Account for year rollover if logs span between December and January
      val year2 = when {
        t1.month == 12 && t2.month == 1 -> refYear + 1
        t1.month == 1 && t2.month == 12 -> refYear - 1
        else -> refYear
      }
      val nanos2 = toNanos(t2.hundredth)
      val dt2 = LocalDateTime.of(year2, t2.month, t2.day, t2.hour, t2.minutes, t2.seconds, nanos2.toInt())

      val duration = Duration.between(dt1, dt2)
      DurationResult(
        duration = duration.abs(),
        isNegative = duration.isNegative
      )
    } catch (e: Exception) {
      null
    }
  }

  private fun toNanos(hundredth: Int): Long {
    val nonNegative = maxOf(0, hundredth)
    return when {
      nonNegative < 1_000 -> nonNegative * 1_000_000L
      nonNegative < 1_000_000 -> nonNegative * 1_000L
      else -> minOf(999_999_999L, nonNegative.toLong())
    }
  }

  /**
   * Formats a human-readable duration breakdown (e.g. "1m 23s 450ms", "450ms", "2h 15m 30s 100ms").
   */
  fun formatBreakdown(duration: Duration): String {
    val totalDays = duration.toDays()
    val hours = duration.toHours() % 24
    val minutes = duration.toMinutes() % 60
    val seconds = duration.seconds % 60
    val millis = duration.toMillis() % 1000

    return buildString {
      if (totalDays > 0) {
        append("${totalDays}d ")
      }
      if (hours > 0 || totalDays > 0) {
        append("${hours}h ")
      }
      if (minutes > 0 || hours > 0 || totalDays > 0) {
        append("${minutes}m ")
      }
      if (seconds > 0 || minutes > 0 || hours > 0 || totalDays > 0) {
        append("${seconds}s ")
      }
      append("${millis}ms")
    }.trim()
  }

  /**
   * Formats the total duration with seconds decimal, e.g. "1m 23s 450ms (83.450s)".
   */
  fun formatDurationWithSeconds(result: DurationResult): String {
    val breakdown = formatBreakdown(result.duration)
    val totalSeconds = String.format(Locale.US, "%.3fs", result.totalMillis / 1000.0)
    val prefix = if (result.isNegative) "-" else ""
    return "$prefix$breakdown ($prefix$totalSeconds)"
  }

  /**
   * Formats duration for display in the context menu title, e.g. "1m 23s 450ms (83.450s)".
   */
  fun formatDurationMenuText(result: DurationResult): String {
    return formatDurationWithSeconds(result)
  }

  /**
   * Creates the clipboard snippet consisting of line 1, the elapsed time indicator, and line 2.
   */
  fun createSnippet(entry1: LogEntry, entry2: LogEntry, result: DurationResult): String {
    val durationStr = formatDurationWithSeconds(result)
    val backwardSuffix = if (result.isNegative) " (${I18n.get(I18n.ELAPSED_TIME_BACKWARD)})" else ""
    val separator = "... ${I18n.get(I18n.ELAPSED_TIME_LABEL)}: $durationStr$backwardSuffix ..."
    val lineSep = System.lineSeparator()
    return "${entry1.logText}${lineSep}$separator${lineSep}${entry2.logText}"
  }
}
