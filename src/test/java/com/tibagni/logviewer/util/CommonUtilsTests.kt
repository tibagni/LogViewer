package com.tibagni.logviewer.util

import org.junit.Assert.*
import org.junit.Test

class CommonUtilsTests {
  @Test
  fun testAsSortedList() {
    val sorted = CommonUtils.asSortedList(listOf("a", "z", "c", "b", "j"))
    assertEquals("a", sorted[0])
    assertEquals("b", sorted[1])
    assertEquals("c", sorted[2])
    assertEquals("j", sorted[3])
    assertEquals("z", sorted[4])
  }

  @Test
  fun testListOf() {
    val list = CommonUtils.listOf("a", "z", "c", "b", "j")
    assertEquals("a", list[0])
    assertEquals("z", list[1])
    assertEquals("c", list[2])
    assertEquals("b", list[3])
    assertEquals("j", list[4])
  }

  @Test
  fun testSleepSilently() {
    val start = System.currentTimeMillis()
    CommonUtils.sleepSilently(100)
    val end = System.currentTimeMillis()
    val duration = end - start

    assertTrue(duration >= 100)
  }
}