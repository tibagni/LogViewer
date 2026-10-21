package com.tibagni.logviewer.filter.regex

import com.formdev.flatlaf.FlatClientProperties
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.awt.GraphicsEnvironment

/**
 * Unit tests for [RegexEditorDialog] verifying UI behavior and FlatLaf outline integration.
 */
class RegexEditorDialogTests {

    /**
     * Verifies that [FlatClientProperties.OUTLINE] is null when the regex pattern is valid,
     * and is set to [FlatClientProperties.OUTLINE_ERROR] ("error") when the regex pattern is malformed.
     */
    @Test
    fun testRegexOutlineValidAndInvalid() {
        if (GraphicsEnvironment.isHeadless()) {
            return
        }

        val dialog = RegexEditorDialog(null)
        try {
            // Test valid regex pattern: outline should be null
            dialog.regexEdit.text = "valid.*pattern"
            assertNull(dialog.regexEdit.getClientProperty(FlatClientProperties.OUTLINE))

            // Test malformed regex pattern (unclosed bracket): outline should be OUTLINE_ERROR ("error")
            dialog.regexEdit.text = "["
            assertEquals(
                FlatClientProperties.OUTLINE_ERROR,
                dialog.regexEdit.getClientProperty(FlatClientProperties.OUTLINE)
            )
            assertEquals(
                "error",
                dialog.regexEdit.getClientProperty(FlatClientProperties.OUTLINE)
            )

            // Test transition back to valid pattern: outline should return to null
            dialog.regexEdit.text = "[0-9]+"
            assertNull(dialog.regexEdit.getClientProperty(FlatClientProperties.OUTLINE))

            // Test empty regex: outline should be null
            dialog.regexEdit.text = ""
            assertNull(dialog.regexEdit.getClientProperty(FlatClientProperties.OUTLINE))
        } finally {
            dialog.dispose()
        }
    }
}
