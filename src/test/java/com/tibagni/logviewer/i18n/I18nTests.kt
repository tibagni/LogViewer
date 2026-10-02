package com.tibagni.logviewer.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier
import java.util.Locale

class I18nTests {

  @Test
  fun testAllDeclaredKeysExistInBundle() {
    val fields = I18n::class.java.declaredFields
    var checkedKeysCount = 0

    for (field in fields) {
      val modifiers = field.modifiers
      if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)
        && field.type == String::class.java
      ) {
        val key = field.get(null) as String
        val value = I18n.get(key)

        assertNotNull("Value for key $key should not be null", value)
        assertFalse("Key '$key' is missing translation in bundle", value.startsWith("!") && value.endsWith("!"))
        assertTrue("Value for key $key should not be empty", value.isNotEmpty())
        checkedKeysCount++
      }
    }

    assertTrue("Expected to check at least one declared key", checkedKeysCount > 0)
  }

  @Test
  fun testMissingKeyFallback() {
    val missingKey = "some.non.existent.key"
    val result = I18n.get(missingKey)
    assertEquals("!some.non.existent.key!", result)
  }

  @Test
  fun testFormatWithArguments() {
    // Test with placeholder formatting
    val formatted = I18n.format("pref.dialog.title")
    assertEquals("Preferences", formatted)
  }

  @Test
  fun testSetAndGetLocale() {
    val originalLocale = I18n.getLocale()
    try {
      I18n.setLocale(Locale.US)
      assertEquals(Locale.US, I18n.getLocale())
      assertEquals("OK", I18n.get(I18n.COMMON_OK))

      I18n.setLocale(null)
      assertEquals(Locale.getDefault(), I18n.getLocale())
    } finally {
      I18n.setLocale(originalLocale)
    }
  }

  @Test
  fun testFormatWithInvalidPattern() {
    // When formatting a key whose value contains an unmatched brace
    val formatted = I18n.format("some.non.existent.key{invalid", "arg")
    assertEquals("!some.non.existent.key{invalid!", formatted)
  }
}
