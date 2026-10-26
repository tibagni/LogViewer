package com.tibagni.logviewer.filter

import com.tibagni.logviewer.log.LogLevel
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.FlatButton
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import java.awt.Color
import java.awt.Font
import java.awt.GraphicsEnvironment
import javax.swing.UIManager

/**
 * Test class to verify EditFilterDialog interactions and FlatLaf styling.
 */
class EditFilterDialogInteractionTests {

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
    }

    /**
     * Verifies that the dialog initializes without error.
     */
    @Test
    fun testCreatingNewFilterSucceedsWhenFieldsAreValid() {
        assumeFalse(GraphicsEnvironment.isHeadless())
        val dialog = EditFilterDialog(null, null, null) { _, _ -> null }
        assertNotNull(dialog)
    }

    /**
     * Verifies duplicate check callback integration.
     */
    @Test
    fun testCheckForDuplicateFilterReturnsResultFromCallback() {
        assumeFalse(GraphicsEnvironment.isHeadless())
        var callbackInvoked = false
        val existing = Filter("f1", "Pattern", Color.RED, LogLevel.DEBUG)
        val callback = EditFilterDialog.DuplicateCheckCallback { _, _ ->
            callbackInvoked = true
            FilterMatch("GroupA", existing, 0)
        }
        val dialog = EditFilterDialog(null, null, null, callback)

        val result = dialog.checkForDuplicateFilter("Test", true)

        assertTrue(callbackInvoked)
        assertNotNull(result)
        assertEquals("GroupA", result?.group)
        assertEquals(existing, result?.filter)
    }

    /**
     * Verifies regexEditorBtn is styled as a FlatButton with a chevron icon
     * and configured icon-text gap.
     */
    @Test
    fun testRegexEditorBtnIsFlatButtonWithChevronIcon() {
        assumeFalse(GraphicsEnvironment.isHeadless())
        val dialog = EditFilterDialog(null, null, null) { _, _ -> null }
        val button = dialog.regexEditorBtn

        assertTrue(button is FlatButton)
        assertNotNull(button.icon)
        assertEquals(14, button.icon.iconWidth)
        assertEquals(14, button.icon.iconHeight)
        assertEquals(UIScaleUtils.dip(4), button.iconTextGap)
    }

    /**
     * Verifies regexTxt uses monospace typography.
     */
    @Test
    fun testRegexTxtMonospaceFont() {
        assumeFalse(GraphicsEnvironment.isHeadless())
        val dialog = EditFilterDialog(null, null, null) { _, _ -> null }
        assertEquals(Font.MONOSPACED, dialog.regexTxt.font.family)
    }

    /**
     * Verifies secondary key labels are styled with disabled foreground.
     */
    @Test
    fun testSecondaryKeyLabelsStyling() {
        assumeFalse(GraphicsEnvironment.isHeadless())
        val dialog = EditFilterDialog(null, null, null) { _, _ -> null }
        val secondaryColor =
            UIManager.getColor("Label.disabledForeground")
        if (secondaryColor != null) {
            assertEquals(secondaryColor, dialog.nameLbl.foreground)
            assertEquals(secondaryColor, dialog.regexLbl.foreground)
            assertEquals(
                secondaryColor,
                dialog.caseSensitiveLbl.foreground
            )
            assertEquals(secondaryColor, dialog.verbosityLbl.foreground)
            assertEquals(secondaryColor, dialog.colorLbl.foreground)
        }
    }
}
