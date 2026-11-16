package com.tibagni.logviewer

import com.tibagni.logviewer.i18n.I18n
import com.tibagni.logviewer.log.LogTimestamp
import com.tibagni.logviewer.view.CommonIcons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import javax.swing.Icon

/**
 * Unit tests for [LogViewerViewImpl] verifying dialog provider integration,
 * Go to Timestamp menu handling, and vector icon configuration.
 */
class LogViewerViewTests {

  private lateinit var mockMain: MainView
  private lateinit var mockPresenter: LogViewerPresenter
  private lateinit var view: LogViewerViewImpl

  /**
   * Sets up test fixtures and mocks prior to each test run.
   */
  @Before
  fun setUp() {
    mockMain = mock(MainView::class.java)
    mockPresenter = mock(LogViewerPresenter::class.java)
    view = LogViewerViewImpl(
      mainView = mockMain,
      initialLogFiles = emptySet(),
      customPresenter = mockPresenter
    )
  }

  /**
   * Verifies that when input is confirmed in the Go to Timestamp dialog,
   * the vector clock icon is provided and [LogViewerPresenter.goToTimestamp] is called.
   */
  @Test
  fun testGoToTimestampInvokesPresenterWhenInputProvided() {
    var capturedIcon: Icon? = null
    var capturedTitle: String? = null
    var capturedMsg: String? = null

    view.inputDialogProvider = { _, msg, title, _, icon, _, _ ->
      capturedMsg = msg
      capturedTitle = title
      capturedIcon = icon
      "05-18 12:00:00.000"
    }

    view.handleGoToTimestampMenu()

    assertEquals(I18n.get(I18n.LOGS_GOTO_TIMESTAMP_TITLE), capturedTitle)
    assertEquals(I18n.get(I18n.LOGS_GOTO_TIMESTAMP_MSG), capturedMsg)
    assertNotNull(capturedIcon)
    assertTrue(capturedIcon is CommonIcons.DynamicVectorIcon)
    verify(mockPresenter).goToTimestamp("05-18 12:00:00.000")
  }

  /**
   * Verifies that cancelling or providing empty input in the Go to Timestamp dialog
   * aborts without invoking [LogViewerPresenter.goToTimestamp].
   */
  @Test
  fun testGoToTimestampAbortsWhenCancelledOrEmpty() {
    view.inputDialogProvider = { _, _, _, _, _, _, _ -> null }
    view.handleGoToTimestampMenu()
    verify(mockPresenter, never()).goToTimestamp(anyString())

    view.inputDialogProvider = { _, _, _, _, _, _, _ -> "" }
    view.handleGoToTimestampMenu()
    verify(mockPresenter, never()).goToTimestamp(anyString())
  }

  /**
   * Verifies that when a log entry is selected, its timestamp is formatted as the
   * initial selection hint text for the Go to Timestamp dialog.
   */
  @Test
  fun testGoToTimestampUsesTimestampHintFromSelectedLog() {
    val ts = LogTimestamp(10, 25, 14, 30, 45, 99)
    view.selectedTimestampProvider = { ts }

    var capturedInitialValue: Any? = null
    view.inputDialogProvider = { _, _, _, _, _, _, initial ->
      capturedInitialValue = initial
      null
    }

    view.handleGoToTimestampMenu()
    assertEquals("10-25 14:30:45.99", capturedInitialValue)
  }

  /**
   * Verifies that when no log entry is selected, the default placeholder format string
   * is used as the initial hint value.
   */
  @Test
  fun testGoToTimestampUsesDefaultHintWhenNoLogSelected() {
    view.selectedTimestampProvider = { null }

    var capturedInitialValue: Any? = null
    view.inputDialogProvider = { _, _, _, _, _, _, initial ->
      capturedInitialValue = initial
      null
    }

    view.handleGoToTimestampMenu()
    assertEquals("{month}-{day} {hour}:{min}:{sec}:{hund}", capturedInitialValue)
  }
}
