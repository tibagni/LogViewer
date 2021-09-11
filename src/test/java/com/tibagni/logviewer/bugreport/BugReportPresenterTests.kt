package com.tibagni.logviewer.bugreport

import com.tibagni.logviewer.MockExecutorService
import com.tibagni.logviewer.ProgressReporter
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.*
import org.mockito.Mockito.*

class BugReportPresenterTests {
  private lateinit var presenter: BugReportPresenterImpl

  @Mock
  private lateinit var mockView: BugReportPresenterView

  @Mock
  private lateinit var mockRepository: BugReportRepository

  @Captor
  private lateinit var progressReporterCaptor: ArgumentCaptor<ProgressReporter>

  private lateinit var closeable: AutoCloseable

  /**
   * Returns Mockito.any() as nullable type to avoid java.lang.IllegalStateException when
   * null is returned.
   */
  private fun <T> anyOrNull(): T = Mockito.any<T>()
  private fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()

  @Before
  fun setUp() {
    closeable = MockitoAnnotations.openMocks(this)
    presenter = BugReportPresenterImpl(mockView, mockRepository)
    presenter.setBgExecutorService(MockExecutorService())
    presenter.setUiExecutor { it.run() }
  }

  @After
  fun tearDown() {
    closeable.close()
  }

  @Test
  fun testLoadBugReport() {
    `when`(mockRepository.bugReport).thenReturn(BugReport("text", listOf()))

    presenter.loadBugReport("path", "text")

    verify(mockRepository).loadBugReport(anyString(), anyString(), capture(progressReporterCaptor))
    verify(mockView).showBugReport(anyOrNull())
    verify(mockView, never()).showErrorMessage(anyString())
    verify(mockView).showStartLoading()

    // Use the captured progress reporter to test the progress is forwarded to the UI
    progressReporterCaptor.value.onProgress(50, "test 1")
    progressReporterCaptor.value.onProgress(100, "test 2")

    verify(mockView).showLoadingProgress(50, "test 1")
    verify(mockView).finishLoading()
  }

  @Test
  fun testLoadBugReportEmpty() {
    `when`(mockRepository.bugReport).thenReturn(null)
    presenter.loadBugReport("path", "text")

    verify(mockRepository).loadBugReport(anyString(), anyString(), capture(progressReporterCaptor))
    verify(mockView, never()).showBugReport(anyOrNull())
    verify(mockView).showErrorMessage(anyString())
    verify(mockView).showStartLoading()

    // Use the captured progress reporter to test the progress is forwarded to the UI
    progressReporterCaptor.value.onProgress(50, "test 1")
    progressReporterCaptor.value.onProgress(100, "test 2")

    verify(mockView).showLoadingProgress(50, "test 1")
    verify(mockView).finishLoading()
  }

  @Test
  fun testLoadBugReportFailed() {
    `when`(mockRepository.loadBugReport(anyString(), anyString(), anyOrNull())).thenThrow(
      OpenBugReportException(
        "",
        Throwable()
      )
    )

    presenter.loadBugReport("path", "text")

    verify(mockView, never()).showBugReport(anyOrNull())
    verify(mockView).showErrorMessage(anyString())
    verify(mockView).showStartLoading()
    verify(mockView).finishLoading()
  }

  @Test
  fun testCloseBugReport() {
    presenter.closeBugReport()

    verify(mockRepository).closeBugReport()
  }
}