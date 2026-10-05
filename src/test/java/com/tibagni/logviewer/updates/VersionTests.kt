package com.tibagni.logviewer.updates

import org.junit.Assert.*
import org.junit.Test

class VersionTests {

  @Test
  fun testVersionParsingAndStringRepresentation() {
    val v1 = Version("2.7")
    assertEquals("2.7", v1.originalString)
    assertEquals("2.7", v1.cleanString)
    assertEquals(listOf(2, 7), v1.parts)
    assertEquals("2.7", v1.toString())
  }

  @Test
  fun testVersionPrefixes() {
    val vLower = Version("v2.7")
    val vUpper = Version("V2.7")
    val plain = Version("2.7")

    assertEquals(listOf(2, 7), vLower.parts)
    assertEquals(listOf(2, 7), vUpper.parts)
    assertEquals(plain, vLower)
    assertEquals(plain, vUpper)
  }

  @Test
  fun testPreReleaseSuffixIgnoredInComparison() {
    val vBeta = Version("2.7-beta")
    val plain = Version("2.7")

    assertEquals("2.7-beta", vBeta.originalString)
    assertEquals("2.7", vBeta.cleanString)
    assertEquals(listOf(2, 7), vBeta.parts)
    assertEquals(plain, vBeta)
  }

  @Test
  fun testVersionComparisons() {
    val v1 = Version("2.7")
    val v2 = Version("2.7.1")
    val v3 = Version("10.0")

    assertTrue(v2 > v1)
    assertTrue(v3 > v2)
    assertTrue(v1 < v3)
    assertTrue(v1 == Version("2.7.0")) // extra trailing zero equates to same
  }

  @Test
  fun testEqualsAndHashCode() {
    val v1 = Version("2.7")
    val v2 = Version("2.7.0")
    val v3 = Version("v2.7")

    assertEquals(v1, v2)
    assertEquals(v1, v3)
    assertEquals(v1.hashCode(), v2.hashCode())
    assertEquals(v1.hashCode(), v3.hashCode())
  }

  @Test
  fun testIsValid() {
    assertTrue(Version.isValid("2.7"))
    assertTrue(Version.isValid("v2.7"))
    assertTrue(Version.isValid("V2.7"))
    assertTrue(Version.isValid("2.7.1"))
    assertTrue(Version.isValid("2.7-beta"))
    assertTrue(Version.isValid("2.7-rc.1"))

    assertFalse(Version.isValid("2.3s"))
    assertFalse(Version.isValid("abc"))
    assertFalse(Version.isValid(""))
  }

  @Test(expected = IllegalArgumentException::class)
  fun testInvalidVersionThrows() {
    Version("2.3s")
  }
}
