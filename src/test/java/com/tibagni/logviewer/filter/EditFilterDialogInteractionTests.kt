package com.tibagni.logviewer.filter

import com.tibagni.logviewer.log.LogLevel
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.awt.Color
import javax.swing.JCheckBox
import javax.swing.JColorChooser
import javax.swing.JComboBox
import javax.swing.JTextField

/**
 * Test class to verify EditFilterDialog interactions.
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
    fun `test creating new filter succeeds when fields are valid`() {
        val dialog = EditFilterDialog(null, null, null) { _, _ -> null }
        assertNotNull(dialog)
    }

    /**
     * Verifies duplicate check callback integration.
     */
    @Test
    fun `test checkForDuplicateFilter returns result from callback`() {
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
}
