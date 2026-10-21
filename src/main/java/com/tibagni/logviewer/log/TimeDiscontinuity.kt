package com.tibagni.logviewer.log

import java.io.File

data class TimeDiscontinuity(
  val sourceFile: File?,
  val sourceFileName: String,
  val lineNumber: Int,
  val previousTimestamp: LogTimestamp,
  val currentTimestamp: LogTimestamp
) {
  val isYearRollover: Boolean
    get() = previousTimestamp.month == 12 && currentTimestamp.month == 1

  fun formatPreviousTime(): String = previousTimestamp.formatDisplay()
  fun formatCurrentTime(): String = currentTimestamp.formatDisplay()

  fun formatDelta(): String {
    if (isYearRollover) {
      return "Year rollover"
    }

    if (previousTimestamp.month != currentTimestamp.month) {
      val monthDiff = previousTimestamp.month - currentTimestamp.month
      return "-${monthDiff}mo"
    }

    val prevSeconds = toSecondsOfDay(previousTimestamp)
    val currSeconds = toSecondsOfDay(currentTimestamp)
    val dayDiff = previousTimestamp.day - currentTimestamp.day
    val totalSecDiff = dayDiff * 86400L + (prevSeconds - currSeconds)

    if (totalSecDiff <= 0) return ""

    val days = totalSecDiff / 86400L
    val hours = (totalSecDiff % 86400L) / 3600L
    val minutes = (totalSecDiff % 3600L) / 60L
    val seconds = totalSecDiff % 60L

    val parts = mutableListOf<String>()
    if (days > 0) parts.add("${days}d")
    if (hours > 0) parts.add("${hours}h")
    if (minutes > 0) parts.add("${minutes}m")
    if (seconds > 0 || parts.isEmpty()) parts.add("${seconds}s")

    return "-${parts.joinToString(" ")}"
  }

  private fun toSecondsOfDay(t: LogTimestamp): Long {
    return t.hour.toLong() * 3600 + t.minutes * 60 + t.seconds
  }
}

fun LogTimestamp.formatDisplay(): String {
  return String.format("%02d-%02d %02d:%02d:%02d", month, day, hour, minutes, seconds)
}
