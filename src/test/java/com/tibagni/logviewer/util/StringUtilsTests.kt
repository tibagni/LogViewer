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

  @Test
  fun testHtmlEscape() {
    assertEquals("&lt;&gt;&amp;&quot;", StringUtils.htmlEscape("<>&\""))
  }

  @Test
  fun testPotentialRegexForValidRegex() {
    assertTrue(StringUtils.isPotentialRegex(".*"))
    assertTrue(StringUtils.isPotentialRegex("."))
    assertTrue(StringUtils.isPotentialRegex("\\d{3}-\\d{2}-\\d{4}"))
    assertTrue(StringUtils.isPotentialRegex("[A-Za-z]+"))
    assertTrue(StringUtils.isPotentialRegex("\\d{1,3}"))
    assertTrue(StringUtils.isPotentialRegex("(red|blue)"))
    assertTrue(StringUtils.isPotentialRegex("a{5}"))
    assertTrue(StringUtils.isPotentialRegex("a{3,5}"))
    assertTrue(StringUtils.isPotentialRegex("^[A-Za-z]+$"))
    assertTrue(StringUtils.isPotentialRegex("[0-9]{2,3}|[A-Za-z]{3,4}"))
    assertTrue(StringUtils.isPotentialRegex("(\\d{3}-\\d{2}-\\d{4})|(\\d{4}-\\d{2}-\\d{2})"))
    assertTrue(StringUtils.isPotentialRegex("word.word"))
  }

  @Test
  fun testPotentialRegexForNonRegex() {
    assertFalse(StringUtils.isPotentialRegex("Not a regex"))
    assertFalse(StringUtils.isPotentialRegex("This is a normal string"))
    assertFalse(StringUtils.isPotentialRegex("12345"))
  }
}