package com.tibagni.logviewer.live

import com.formdev.flatlaf.ui.FlatLineBorder
import com.tibagni.logviewer.adb.AdbDevice
import com.tibagni.logviewer.adb.DeviceState
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import com.tibagni.logviewer.view.FlatButton
import com.tibagni.logviewer.view.ToolbarDivider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.awt.Cursor
import javax.swing.JPanel
import javax.swing.border.CompoundBorder

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

  @Test
  fun testToolbarActionButtonsHaveHandCursor() {
    val panel = LiveLogDevicesPanel(mockPresenter)
    val handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    assertEquals(handCursor, panel.refreshDevicesBtn.cursor)
    assertEquals(handCursor, panel.restartServerBtn.cursor)
  }

  @Test
  fun testBuffersCardHasRoundedBorder() {
    val panel = LiveLogDevicesPanel(mockPresenter)
    val border = panel.buffersCard.border
    assertTrue(border is CompoundBorder)
    val compound = border as CompoundBorder
    assertTrue(compound.outsideBorder is FlatLineBorder)
    val lineBorder = compound.outsideBorder as FlatLineBorder
    assertEquals(UIScaleUtils.dip(8), lineBorder.arc)
  }

  @Test
  fun testDeviceCardsHaveRoundedBorders() {
    `when`(mockPresenter.availableBuffers).thenReturn(listOf("main", "system"))
    val panel = LiveLogDevicesPanel(mockPresenter)

    val device = AdbDevice(
      serialNumber = "serial_001",
      state = DeviceState.ONLINE,
      model = "Pixel_8"
    )
    panel.populateDeviceList(listOf(device))

    assertEquals(1, panel.deviceItemsContainer.componentCount)
    val card = panel.deviceItemsContainer.getComponent(0) as JPanel
    val border = card.border
    assertTrue(border is CompoundBorder)
    val compound = border as CompoundBorder
    assertTrue(compound.outsideBorder is FlatLineBorder)
    val lineBorder = compound.outsideBorder as FlatLineBorder
    assertEquals(UIScaleUtils.dip(8), lineBorder.arc)
  }
}
