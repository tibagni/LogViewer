package com.tibagni.logviewer.view

import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.Mockito.mock

class ButtonsPaneTests {

  @Test
  fun testEnableOkAndCancel() {
    val listener = mock(ButtonsPane.Listener::class.java)
    val pane = ButtonsPane(ButtonsPane.ButtonsMode.OK_CANCEL, listener)

    pane.enableOkButton(false)
    pane.enableCancelButton(false)
    pane.enableOkButton(true)
    pane.enableCancelButton(true)

    pane.setOkText("Confirm")
    pane.setCancelText("Dismiss")
    assertNotNull(pane)
  }

  @Test
  fun testCancelOnlyMode() {
    val listener = mock(ButtonsPane.Listener::class.java)
    val pane = ButtonsPane(ButtonsPane.ButtonsMode.CANCEL_ONLY, listener)
    pane.enableCancelButton(false)
    pane.setCancelText("Close")
    assertNotNull(pane)
  }

  @Test
  fun testOkOnlyMode() {
    val listener = mock(ButtonsPane.Listener::class.java)
    val pane = ButtonsPane(ButtonsPane.ButtonsMode.OK_ONLY, listener)
    pane.enableOkButton(false)
    pane.enableCancelButton(false) // Safe when buttonCancel is null
    assertNotNull(pane)
  }
}
