package com.tibagni.logviewer.util

import org.junit.Assert.*
import org.junit.Test

class StringUtilsTests {
  @Test
  fun testIsEmpty() {
    assertTrue(StringUtils.isEmpty(null))
    assertTrue(StringUtils.isEmpty(""))
    assertTrue(StringUtils.isEmpty(" "))
    assertFalse(StringUtils.isEmpty("not empty"))
  }

  @Test
  fun testIsNotEmpty() {
    assertFalse(StringUtils.isNotEmpty(null))
    assertFalse(StringUtils.isNotEmpty(""))
    assertFalse(StringUtils.isNotEmpty(" "))
    assertTrue(StringUtils.isNotEmpty("not empty"))
  }

  @Test
  fun testEquals() {
    assertTrue(StringUtils.areEquals("str", "str"))
    assertTrue(StringUtils.areEquals("", ""))
    assertTrue(StringUtils.areEquals(" ", " "))
    assertTrue(StringUtils.areEquals(null, null))
    assertFalse(StringUtils.areEquals("", null))
    assertFalse(StringUtils.areEquals(" ", ""))
    assertFalse(StringUtils.areEquals(" ", "something"))
  }

  @Test
  fun testHtmlHighlightAndEscape() {
    assertEquals("ex<span style=\"background-color:yellow;color:black\">amp</span>le",
      StringUtils.htmlHighlightAndEscape("example", 2, 5))
    assertEquals("text <span style=\"background-color:yellow;color:black\">highlighted</span> in html",
      StringUtils.htmlHighlightAndEscape("text highlighted in html", 5, 16))
  }

  @Test
  fun testEndsWith() {
    assertTrue(StringUtils.endsWithOneOf("ending with bla", arrayOf("bla", "ff", "something else")))
    assertFalse(StringUtils.endsWithOneOf("ending with end", arrayOf("bla", "ff", "something else")))
  }

  @Test
  fun testWrapHtml() {
    assertEquals("<html>wrapped in html</html>", StringUtils.wrapHtml("wrapped in html"))
  }
}