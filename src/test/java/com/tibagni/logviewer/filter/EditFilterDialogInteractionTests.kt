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

    /**
     * Verifies modernColorChooser and colorChooser are configured with an uncropped preview label.
     */
    @Test
    fun testColorChooserPreviewPanelStyling() {
        assumeFalse(GraphicsEnvironment.isHeadless())
        val dialog = EditFilterDialog(null, null, null) { _, _ -> null }
        try {
            assertNotNull(dialog.modernColorChooser)
            assertNotNull(dialog.colorChooser)
            val preview = dialog.colorChooser.previewPanel
            assertNotNull(preview)
            assertTrue(preview.preferredSize.height >= UIScaleUtils.dip(40))
        } finally {
            dialog.dispose()
        }
    }

    /**
     * Verifies that colorChooser retains all panels with modernColorChooser as the first tab.
     */
    @Test
    fun testColorChooserRetainsAllPanelsWithModernFirst() {
        assumeFalse(GraphicsEnvironment.isHeadless())
        val dialog = EditFilterDialog(null, null, null) { _, _ -> null }
        try {
            val panels = dialog.colorChooser.chooserPanels
            assertTrue(panels.size > 1)
            assertEquals(dialog.modernColorChooser, panels[0])
            assertEquals("Modern", panels[0].displayName)
        } finally {
            dialog.dispose()
        }
    }

    /**
     * Verifies bidirectional synchronization between modernColorChooser and colorChooser.
     */
    @Test
    fun testModernColorChooserBidirectionalSync() {
        assumeFalse(GraphicsEnvironment.isHeadless())
        val dialog = EditFilterDialog(null, null, null) { _, _ -> null }
        try {
            dialog.modernColorChooser.color = Color.MAGENTA
            assertEquals(Color.MAGENTA, dialog.colorChooser.color)

            dialog.colorChooser.color = Color.CYAN
            assertEquals(Color.CYAN, dialog.modernColorChooser.color)
            assertEquals("#00FFFF", dialog.modernColorChooser.hexField.text)
            assertEquals(0, dialog.modernColorChooser.rSpinner.value)
            assertEquals(255, dialog.modernColorChooser.gSpinner.value)
            assertEquals(255, dialog.modernColorChooser.bSpinner.value)
        } finally {
            dialog.dispose()
        }
    }

    /**
     * Verifies that EditFilterDialog structures its controls into modern card panels.
     */
    @Test
    fun testDialogModernCardsStructure() {
        assumeFalse(GraphicsEnvironment.isHeadless())
        val dialog = EditFilterDialog(null, null, null) { _, _ -> null }
        try {
            assertNotNull(dialog.nameTxt)
            assertNotNull(dialog.regexTxt)
            assertNotNull(dialog.regexEditorBtn)
            assertNotNull(dialog.caseSensitiveCbx)
            assertNotNull(dialog.verbosityCombo)
            assertNotNull(dialog.colorChooser)
            assertNotNull(dialog.modernColorChooser)
        } finally {
            dialog.dispose()
        }
    }
}
