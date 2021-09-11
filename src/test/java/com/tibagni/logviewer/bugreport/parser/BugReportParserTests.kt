package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.ProgressReporter
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class BugReportParserTests {
  private lateinit var bugReportParser: BugReportParser

  @Mock
  private lateinit var mockSectionParser: BugReportSectionParser
  @Mock
  private lateinit var mockSectionParser2: BugReportSectionParser
  @Mock
  private lateinit var mockProgressReporter: ProgressReporter

  private lateinit var closeable: AutoCloseable

  @Before
  fun setUp() {
    closeable = MockitoAnnotations.openMocks(this)
  }

  @After
  fun tearDown() {
    closeable.close()
  }

  @Test
  fun testParseBugReportSingleParser() {
    bugReportParser = BugReportParserImpl(listOf(mockSectionParser))
    `when`(mockSectionParser.name).thenReturn("name")

    bugReportParser.parseBugReport("path", "text", mockProgressReporter)

    verify(mockProgressReporter).onProgress(0, "Opening bugreport")
    verify(mockProgressReporter).onProgress(100, "Parsing name")
    verify(mockProgressReporter).onProgress(100, "Done!")

    verify(mockSectionParser).parse("path", "text")
  }

  @Test
  fun testParseBugReportMultipleParsers() {
    bugReportParser = BugReportParserImpl(listOf(mockSectionParser, mockSectionParser2))
    `when`(mockSectionParser.name).thenReturn("name")
    `when`(mockSectionParser2.name).thenReturn("name2")

    bugReportParser.parseBugReport("path", "text", mockProgressReporter)

    verify(mockProgressReporter).onProgress(0, "Opening bugreport")
    verify(mockProgressReporter).onProgress(50, "Parsing name")
    verify(mockProgressReporter).onProgress(100, "Parsing name2")
    verify(mockProgressReporter).onProgress(100, "Done!")

    verify(mockSectionParser).parse("path", "text")
    verify(mockSectionParser2).parse("path", "text")
  }
}