package com.tibagni.logviewer.live

import com.tibagni.logviewer.LogViewerPresenter
import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.filter.Filter
import com.tibagni.logviewer.filter.FiltersPanel
import com.tibagni.logviewer.filter.FiltersPanelHost
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.awt.Color
import com.tibagni.logviewer.filter.FiltersPresenter
import java.awt.Frame
import java.io.File
import javax.swing.JFrame
import javax.swing.JSplitPane

class LiveLogViewTests {

  @Mock
  private lateinit var mockPresenter: LiveLogPresenter

  @Mock
  private lateinit var mockFiltersPresenter: FiltersPresenter

  @Mock
  private lateinit var mockHost: FiltersPanelHost

  @Mock
  private lateinit var mockFrame: Frame

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    `when`(mockHost.parentWindow).thenReturn(mockFrame)
  }

  @Test
  fun testAttachFiltersPanelReparentsPanel() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    val filtersPanel = FiltersPanel(mockHost) { mockFiltersPresenter }

    liveLogView.attachFiltersPanel(filtersPanel)

    // Verify panel parent is inside liveLogView contentPane
    assertSame(filtersPanel.parent?.parent, liveLogView.contentPane)
    assertNotNull(filtersPanel.listener)
  }

  @Test
  fun testLiveLogViewDelegatesStreaming() {
    val liveLogView = LiveLogViewImpl { mockPresenter }
    `when`(mockPresenter.isStreaming).thenReturn(true)
    assertTrue(liveLogView.isStreaming)

    val device = com.tibagni.logviewer.adb.AdbDevice("device123", com.tibagni.logviewer.adb.DeviceState.ONLINE)
    liveLogView.startStreaming(device)
    verify(mockPresenter).startStreaming(device)

    liveLogView.stopStreaming()
    verify(mockPresenter).stopStreaming()
  }
}
