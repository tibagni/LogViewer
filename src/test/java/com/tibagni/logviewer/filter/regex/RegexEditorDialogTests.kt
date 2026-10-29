package com.tibagni.logviewer.filter.regex

import com.formdev.flatlaf.FlatClientProperties
import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.StatusBadge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.awt.Font
import java.awt.GraphicsEnvironment
import javax.swing.UIManager

/**
 * Unit tests for [RegexEditorDialog] verifying UI behavior, FlatLaf outline,
 * StatusBadge feedback transitions, and monospace typography.
 */
class RegexEditorDialogTests {

    /**
     * Verifies that [FlatClientProperties.OUTLINE] is null when the regex
     * pattern is valid, and is set to [FlatClientProperties.OUTLINE_ERROR]
     * ("error") when the regex pattern is malformed.
     */
    @Test
    fun testRegexOutlineValidAndInvalid() {
        assumeFalse(GraphicsEnvironment.isHeadless())

        val dialog = RegexEditorDialog(null)
        try {
            dialog.regexEdit.text = "valid.*pattern"
            assertNull(
                dialog.regexEdit.getClientProperty(
                    FlatClientProperties.OUTLINE
                )
            )

            dialog.regexEdit.text = "["
            assertEquals(
                FlatClientProperties.OUTLINE_ERROR,
                dialog.regexEdit.getClientProperty(
                    FlatClientProperties.OUTLINE
                )
            )

            dialog.regexEdit.text = "[0-9]+"
            assertNull(
                dialog.regexEdit.getClientProperty(
                    FlatClientProperties.OUTLINE
                )
            )

            dialog.regexEdit.text = ""
            assertNull(
                dialog.regexEdit.getClientProperty(
                    FlatClientProperties.OUTLINE
                )
            )
        } finally {
            dialog.dispose()
        }
    }

    /**
     * Verifies [StatusBadge] match feedback lifecycle transitions:
     * empty -> hidden, matching -> NORMAL with count,
     * non-matching -> NO_RESULTS with no_matches, invalid -> NO_RESULTS.
     */
    @Test
    fun testMatchStatusBadgeTransitions() {
        assumeFalse(GraphicsEnvironment.isHeadless())

        val dialog = RegexEditorDialog(null)
        try {
            val badge = dialog.matchStatusBadge
            assertNotNull(badge)

            // Initially empty pattern -> badge is hidden
            dialog.regexEdit.text = ""
            assertFalse(badge.isVisible)

            // Custom preview sample text with known match counts
            dialog.regexPreview.text = "abc 123 def 456 ghi 789"

            // Valid pattern with matches -> NORMAL variant with count
            dialog.regexEdit.text = "[0-9]+"
            assertTrue(badge.isVisible)
            assertEquals(StatusBadge.Variant.NORMAL, badge.variant)
            assertEquals(
                I18n.format(I18n.REGEX_EDITOR_MATCHES_COUNT, 3),
                badge.text
            )

            // Valid pattern with no matches -> NO_RESULTS variant with no_matches
            dialog.regexEdit.text = "xyz[0-9]+"
            assertTrue(badge.isVisible)
            assertEquals(StatusBadge.Variant.NO_RESULTS, badge.variant)
            assertEquals(
                I18n.get(I18n.REGEX_EDITOR_NO_MATCHES),
                badge.text
            )

            // Invalid pattern -> NO_RESULTS variant with invalid_regex and error tooltip
            dialog.regexEdit.text = "[unclosed"
            assertTrue(badge.isVisible)
            assertEquals(StatusBadge.Variant.NO_RESULTS, badge.variant)
            assertEquals(
                I18n.get(I18n.REGEX_EDITOR_INVALID_REGEX),
                badge.text
            )
            assertNotNull(badge.toolTipText)

            // Transition back to empty pattern -> hidden and tooltip cleared
            dialog.regexEdit.text = ""
            assertFalse(badge.isVisible)
            assertNull(badge.toolTipText)
        } finally {
            dialog.dispose()
        }
    }

    /**
     * Verifies monospace typography and padding on regex text components.
     */
    @Test
    fun testMonospaceFontAndPaddingOnRegexAreas() {
        assumeFalse(GraphicsEnvironment.isHeadless())

        val dialog = RegexEditorDialog(null)
        try {
            assertEquals(Font.MONOSPACED, dialog.regexEdit.font.family)
            assertEquals(Font.MONOSPACED, dialog.regexPreview.font.family)

            val editInsets = dialog.regexEdit.insets
            assertEquals(UIScaleUtils.dip(4), editInsets.top)
            assertEquals(UIScaleUtils.dip(6), editInsets.left)
            assertEquals(UIScaleUtils.dip(4), editInsets.bottom)
            assertEquals(UIScaleUtils.dip(6), editInsets.right)

            val previewInsets = dialog.regexPreview.insets
            assertEquals(UIScaleUtils.dip(4), previewInsets.top)
            assertEquals(UIScaleUtils.dip(6), previewInsets.left)
            assertEquals(UIScaleUtils.dip(4), previewInsets.bottom)
            assertEquals(UIScaleUtils.dip(6), previewInsets.right)
        } finally {
            dialog.dispose()
        }
    }

    /**
     * Verifies secondary key labels are styled with disabled foreground.
     */
    @Test
    fun testSecondaryKeyLabelsStyling() {
        assumeFalse(GraphicsEnvironment.isHeadless())

        val dialog = RegexEditorDialog(null)
        try {
            val secondaryColor =
                UIManager.getColor("Label.disabledForeground")
            if (secondaryColor != null) {
                assertEquals(secondaryColor, dialog.flagsLbl.foreground)
                assertEquals(secondaryColor, dialog.regexLbl.foreground)
                assertEquals(secondaryColor, dialog.previewLbl.foreground)
            }
        } finally {
            dialog.dispose()
        }
    }

    /**
     * Verifies that the dialog initializes with modern card structure and controls.
     */
    @Test
    fun testModernCardStructure() {
        assumeFalse(GraphicsEnvironment.isHeadless())

        val dialog = RegexEditorDialog(null)
        try {
            assertNotNull(dialog.regexEdit)
            assertNotNull(dialog.regexPreview)
            assertNotNull(dialog.caseSensitive)
            assertNotNull(dialog.matchStatusBadge)
            assertNotNull(dialog.flagsLbl)
            assertNotNull(dialog.regexLbl)
            assertNotNull(dialog.previewLbl)
        } finally {
            dialog.dispose()
        }
    }
}
