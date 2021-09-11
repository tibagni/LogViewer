package com.tibagni.logviewer.bugreport

import com.tibagni.logviewer.ProgressReporter
import com.tibagni.logviewer.bugreport.parser.BugReportParser
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class BugReportRepositoryTests {
  private lateinit var bugReportRepository: BugReportRepository

  @Mock
  private lateinit var mockBugreportParser: BugReportParser

  @Mock
  private lateinit var mockProgressReporter: ProgressReporter

  private lateinit var closeable: AutoCloseable

  @Before
  fun setUp() {
    closeable = MockitoAnnotations.openMocks(this)
    bugReportRepository = BugReportRepositoryImpl(mockBugreportParser)
  }

  @After
  fun tearDown() {
    closeable.close()
  }

  @Test
  fun testParseBugReport() {
    val bugreport = BugReport("", listOf())
    `when`(mockBugreportParser.parseBugReport("test_path", "test_text", mockProgressReporter))
      .thenReturn(bugreport)
    bugReportRepository.loadBugReport("test_path", "test_text", mockProgressReporter)

    verify(mockBugreportParser).parseBugReport("test_path", "test_text", mockProgressReporter)
    assertNotNull(bugReportRepository.bugReport)
  }

  @Test(expected = OpenBugReportException::class)
  fun testParseBugReportFail() {
    `when`(mockBugreportParser.parseBugReport("fail", "fail", mockProgressReporter))
      .thenThrow(RuntimeException("Test Exception"))
    bugReportRepository.loadBugReport("fail", "fail", mockProgressReporter)
  }

  @Test
  fun testCloseBugReport() {
    testParseBugReport()

    bugReportRepository.closeBugReport()

    assertNull(bugReportRepository.bugReport)
  }
}