package com.tibagni.logviewer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class StringViewTests {

  @Test
  fun testStringViewWholeString() {
    val s = "I am a normal String with 35 chars."
    val sv = StringView(s, 0, 35)

    assertEquals(35, sv.length)
    assertEquals(s, sv.toString())
  }

  @Test
  fun testStringViewPartialString() {
    val s = "I am a normal String with 35 chars."
    val sv = StringView(s, 7, 20)

    assertEquals(13, sv.length)
    assertEquals("normal String", sv.toString())
  }

  @Test
  fun testStringViewGetCharWholeString() {
    val s = "I am a normal String with 35 chars."
    val sv = StringView(s, 0, 35)

    assertEquals('n', sv[7])
    assertEquals('l', sv[12])
  }

  @Test
  fun testStringViewGetCharPartialString() {
    val s = "I am a normal String with 35 chars."
    val sv = StringView(s, 7, 20)

    assertEquals('n', sv[0])
    assertEquals('S', sv[7])
  }

  @Test
  fun testSubStringViewFirstHalf() {
    val s = "I am a normal String with 35 chars."
    val sv = StringView(s, 7, 20)
    val ssv = sv.subStringView(0, 6)

    assertEquals(6, ssv.length)
    assertEquals("normal", ssv.toString())
  }

  @Test
  fun testSubStringViewSecondHalf() {
    val s = "I am a normal String with 35 chars."
    val sv = StringView(s, 7, 20)
    val ssv = sv.subStringView(7)

    assertEquals(6, ssv.length)
    assertEquals("String", ssv.toString())
  }

  @Test
  fun testSubStringViewMiddle() {
    val s = "I am a normal String with 35 chars."
    val sv = StringView(s, 7, 20)
    val ssv = sv.subStringView(3, 9)

    assertEquals(6, ssv.length)
    assertEquals("mal St", ssv.toString())
  }

  @Test
  fun testEqualsDifferentStrings() {
    val s = "I am a normal String with 35 chars."
    val s2 = "I am a different normal String with"
    val sv = StringView(s, 7, 20)
    val sv2 = StringView(s2, 17, 30)

    assertEquals(sv, sv2)
    assertEquals(sv.hashCode(), sv2.hashCode())
  }

  @Test
  fun testEqualsSameString() {
    val s = "I am a normal String with 35 chars."
    val sv = StringView(s, 7, 20)
    val sv2 = StringView(s, 7, 20)

    assertEquals(sv, sv2)
    assertEquals(sv.hashCode(), sv2.hashCode())
  }

  @Test
  fun testNotEqualDifferentStrings() {
    val s = "I am a normal String with 35 chars."
    val s2 = "I am a different normal String with"
    val sv = StringView(s, 7, 20)
    val sv2 = StringView(s2, 7, 20)

    assertNotEquals(sv, sv2)
    assertNotEquals(sv.hashCode(), sv2.hashCode())
  }

  @Test
  fun testNotEqualSameString() {
    val s = "I am a normal String with 35 chars."
    val sv = StringView(s, 7, 20)
    val sv2 = StringView(s, 5, 15)

    assertNotEquals(sv, sv2)
    assertNotEquals(sv.hashCode(), sv2.hashCode())
  }
}