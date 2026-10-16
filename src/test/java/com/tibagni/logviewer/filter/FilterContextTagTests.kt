package com.tibagni.logviewer.filter

import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class FilterContextTagTests {

  @After
  fun tearDown() {
    FilterContextTag.unregisterForTesting("tag_a")
    FilterContextTag.unregisterForTesting("tag_b")
    FilterContextTag.unregisterForTesting("dup_tag")
  }

  @Test
  fun testRegisterSuccessful() {
    val tag = FilterContextTag.register("tag_a")
    assertNotNull(tag)
    assertEquals("tag_a", tag.id)
    assertEquals("tag_a", tag.toString())
  }

  @Test(expected = IllegalArgumentException::class)
  fun testDuplicateRegistrationThrows() {
    FilterContextTag.register("dup_tag")
    FilterContextTag.register("dup_tag")
  }

  @Test(expected = IllegalArgumentException::class)
  fun testEmptyIdThrows() {
    FilterContextTag.register("")
  }

  @Test(expected = IllegalArgumentException::class)
  fun testBlankIdThrows() {
    FilterContextTag.register("   ")
  }

  @Test
  fun testEqualsAndHashCode() {
    val tag1 = FilterContextTag.register("tag_a")
    val tag2 = FilterContextTag.register("tag_b")

    assertEquals(tag1, tag1)
    assertNotEquals(tag1, tag2)
    assertNotEquals(tag1, "tag_a")
  }
}
