package com.tibagni.logviewer.live

import com.tibagni.logviewer.view.FlatButton
import com.tibagni.logviewer.view.ToolbarDivider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

/**
 * Unit tests verifying [LiveLogDevicesPanel] action button styling, types,
 * and presenter delegation.
 */
class LiveLogDevicesPanelTests {

  @Mock
  private lateinit var mockPresenter: LiveLogPresenter

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)
  }

  @Test
  fun testHeaderActionButtonsInstantiatedAndTyped() {
    val panel = LiveLogDevicesPanel(mockPresenter)
    assertEquals(
      FlatButton::class.java,
      panel.refreshDevicesBtn.javaClass
    )
    assertEquals(
      FlatButton::class.java,
      panel.restartServerBtn.javaClass
    )
    assertEquals(
      ToolbarDivider::class.java,
      panel.toolbarDivider.javaClass
    )
  }

  @Test
  fun testRefreshDevicesBtnTriggersPresenter() {
    val panel = LiveLogDevicesPanel(mockPresenter)
    panel.refreshDevicesBtn.doClick()
    verify(mockPresenter).loadDevices()
  }

  @Test
  fun testRestartServerBtnTriggersPresenter() {
    val panel = LiveLogDevicesPanel(mockPresenter)
    panel.restartServerBtn.doClick()
    verify(mockPresenter).restartAdbServer()
  }
}
