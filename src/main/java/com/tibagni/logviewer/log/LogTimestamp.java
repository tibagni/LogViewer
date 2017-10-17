package com.tibagni.logviewer.log;

public class LogTimestamp implements Comparable<LogTimestamp> {
  private final int month;
  private final int day;

  private final int hour;
  private final int minutes;
  private final int seconds;
  private final int hundredth;

  public LogTimestamp(int month, int day, int hour, int minutes, int seconds, int hundredth) {
    this.month = month;
    this.day = day;
    this.hour = hour;
    this.minutes = minutes;
    this.seconds = seconds;
    this.hundredth = hundredth;
  }

  public LogTimestamp(String month, String day, String hour, String minutes, String seconds, String hundredth) {
    this.month = Integer.parseInt(month);
    this.day = Integer.parseInt(day);
    this.hour = Integer.parseInt(hour);
    this.minutes = Integer.parseInt(minutes);
    this.seconds = Integer.parseInt(seconds);
    this.hundredth = Integer.parseInt(hundredth);
  }

  @Override
  public int compareTo(LogTimestamp o) {
    if (month > o.month) return 1;
    if (month < o.month) return -1;

    if (day > o.day) return 1;
    if (day < o.day) return -1;

    if (hour > o.hour) return 1;
    if (hour < o.hour) return -1;

    if (minutes > o.minutes) return 1;
    if (minutes < o.minutes) return -1;

    if (seconds > o.seconds) return 1;
    if (seconds < o.seconds) return -1;

    if (hundredth > o.hundredth) return 1;
    if (hundredth < o.hundredth) return -1;

    return 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    LogTimestamp that = (LogTimestamp) o;

    if (day != that.day) return false;
    if (month != that.month) return false;
    if (hour != that.hour) return false;
    if (minutes != that.minutes) return false;
    if (seconds != that.seconds) return false;
    return hundredth == that.hundredth;
  }

  @Override
  public int hashCode() {
    int result = day;
    result = 31 * result + month;
    result = 31 * result + hour;
    result = 31 * result + minutes;
    result = 31 * result + seconds;
    result = 31 * result + hundredth;
    return result;
  }
}
