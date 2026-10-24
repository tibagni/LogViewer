package com.tibagni.logviewer.view

import com.tibagni.logviewer.util.scaling.UIScaleUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.awt.Component
import java.awt.Graphics
import java.awt.Point
import java.awt.image.BufferedImage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer

/**
 * Unit tests verifying [Toast] presentation, duration constants,
 * location centering math, headless execution safety, [StatusBadge]
 * configuration, off-screen buffer rendering, and active toast dismissal.
 */
class ToastTests {

  /**
   * Dismisses any active toast before each test execution.
   */
  @Before
  fun setUp() {
    Toast.dismissCurrentToast()
  }

  /**
   * Dismisses any active toast after each test execution.
   */
  @After
  fun tearDown() {
    Toast.dismissCurrentToast()
  }

  /**
   * Verifies default duration constants for short and long toasts.
   */
  @Test
  fun testDurationConstants() {
    assertEquals(2000, Toast.LENGTH_SHORT)
    assertEquals(3500, Toast.LENGTH_LONG)
  }

  /**
   * Verifies horizontal centering math on a wide parent window.
   */
  @Test
  fun testHorizontalCenteringWideParent() {
    val parentLocation = Point(100, 200)
    val parentWidth = 1000
    val parentHeight = 800
    val toastWidth = 200
    val toastHeight = 40

    val location = Toast.calculateLocation(
      parentLocation,
      parentWidth,
      parentHeight,
      toastWidth,
      toastHeight
    )

    val expectedX = 100 + (1000 - 200) / 2
    val expectedY = 200 + 800 - 40 - Toast.BOTTOM_MARGIN
    assertEquals(expectedX, location.x)
    assertEquals(expectedY, location.y)
  }

  /**
   * Verifies horizontal centering math on a narrow parent window.
   */
  @Test
  fun testHorizontalCenteringNarrowParent() {
    val parentLocation = Point(50, 80)
    val parentWidth = 300
    val parentHeight = 450
    val toastWidth = 240
    val toastHeight = 50

    val location = Toast.calculateLocation(
      parentLocation,
      parentWidth,
      parentHeight,
      toastWidth,
      toastHeight
    )

    val expectedX = 50 + (300 - 240) / 2
    val expectedY = 80 + 450 - 50 - Toast.BOTTOM_MARGIN
    assertEquals(expectedX, location.x)
    assertEquals(expectedY, location.y)
  }

  /**
   * Verifies horizontal centering math when the toast is wider than
   * the parent.
   */
  @Test
  fun testHorizontalCenteringWhenToastWiderThanParent() {
    val parentLocation = Point(150, 100)
    val parentWidth = 120
    val parentHeight = 300
    val toastWidth = 200
    val toastHeight = 40

    val location = Toast.calculateLocation(
      parentLocation,
      parentWidth,
      parentHeight,
      toastWidth,
      toastHeight
    )

    val expectedX = 150 + (120 - 200) / 2
    val expectedY = 100 + 300 - 40 - Toast.BOTTOM_MARGIN
    assertEquals(expectedX, location.x)
    assertEquals(expectedY, location.y)
  }

  /**
   * Verifies calculating location with a component that is not showing
   * safely falls back to origin without throwing exceptions.
   */
  @Test
  fun testCalculateLocationWithNonShowingComponent() {
    val parent = JPanel().apply {
      setSize(400, 300)
    }

    val location = Toast.calculateLocation(parent, 150, 30)
    val expectedX = (400 - 150) / 2
    val expectedY = 300 - 30 - Toast.BOTTOM_MARGIN
    assertEquals(expectedX, location.x)
    assertEquals(expectedY, location.y)
  }

  /**
   * Verifies invoking showToast in headless environments does not throw.
   */
  @Test
  fun testHeadlessSafetyShowToast() {
    Toast.showToast(null, "Test headless")
    Toast.showToast(JPanel(), "Test headless", Toast.LENGTH_SHORT)
    Toast.showToast(JPanel(), "Test headless", Toast.LENGTH_LONG)
    Toast.showToast(null, "Test headless", null)
    Toast.showToast(JPanel(), "Test headless", null, Toast.LENGTH_SHORT)
  }

  /**
   * Verifies [Toast] creates and configures a [StatusBadge] with
   * [StatusBadge.Variant.ELEVATED], custom padding, arc, text, and icon.
   */
  @Test
  fun testToastCreatesAndConfiguresStatusBadge() {
    val dummyIcon = object : Icon {
      override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {}
      override fun getIconWidth(): Int = 16
      override fun getIconHeight(): Int = 16
    }
    val badge = StatusBadge(StatusBadge.Variant.ELEVATED).apply {
      text = "Sample Toast Message"
      icon = dummyIcon
      arcDp = 16
      paddingVerticalDp = 8
      paddingHorizontalDp = 16
      isVisible = true
    }
    val toast = Toast(null, badge, Toast.LENGTH_SHORT)

    assertEquals(badge, toast.badge)
    assertEquals(StatusBadge.Variant.ELEVATED, toast.badge.variant)
    assertEquals("Sample Toast Message", toast.badge.text)
    assertEquals(dummyIcon, toast.badge.icon)
    assertEquals(16, toast.badge.arcDp)
    assertEquals(8, toast.badge.paddingVerticalDp)
    assertEquals(16, toast.badge.paddingHorizontalDp)
    assertTrue(toast.badge.isVisible)
    assertFalse(toast.badge.isOpaque)
  }

  /**
   * Verifies toast [StatusBadge] rendering onto an off-screen buffered image
   * without crashing and with non-zero alpha pixels.
   */
  @Test
  fun testToastBadgeHeadlessPainting() {
    val badge = StatusBadge(StatusBadge.Variant.ELEVATED).apply {
      text = "Headless Toast"
      arcDp = 16
      paddingVerticalDp = 8
      paddingHorizontalDp = 16
      isVisible = true
    }
    val w = UIScaleUtils.dip(160)
    val h = UIScaleUtils.dip(36)
    badge.setSize(w, h)

    val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    try {
      badge.paint(g)
    } finally {
      g.dispose()
    }

    var hasPaintedPixel = false
    val midX = w / 2
    for (y in 0 until h) {
      val argb = image.getRGB(midX, y)
      val alpha = (argb ushr 24) and 0xFF
      if (alpha > 0) {
        hasPaintedPixel = true
        break
      }
    }
    assertTrue(hasPaintedPixel)
  }

  /**
   * Verifies active toast replacement cleanly dismisses any previously
   * visible toast before installing the new one.
   */
  @Test
  fun testToastReplacementAndDismissal() {
    val badge1 = StatusBadge(StatusBadge.Variant.ELEVATED).apply {
      text = "First Toast"
    }
    val toast1 = Toast(null, badge1, Toast.LENGTH_SHORT)
    Toast.replaceToast(toast1)
    assertEquals(toast1, Toast.currentToast)
    assertFalse(toast1.isDismissed)

    val badge2 = StatusBadge(StatusBadge.Variant.ELEVATED).apply {
      text = "Second Toast"
    }
    val toast2 = Toast(null, badge2, Toast.LENGTH_LONG)
    Toast.replaceToast(toast2)

    assertTrue(toast1.isDismissed)
    assertEquals(toast2, Toast.currentToast)
    assertFalse(toast2.isDismissed)

    Toast.dismissCurrentToast()
    assertTrue(toast2.isDismissed)
    assertNull(Toast.currentToast)
  }

  /**
   * Verifies auto-dismiss timer is canceled and cleaned up when a toast
   * is dismissed.
   */
  @Test
  fun testToastTimerCleanupOnDismiss() {
    val badge = StatusBadge(StatusBadge.Variant.ELEVATED)
    val toast = Toast(null, badge, Toast.LENGTH_SHORT)
    val timer = Timer(10000) {}
    toast.dismissTimer = timer
    timer.start()
    assertTrue(timer.isRunning)

    toast.dismiss()
    assertTrue(toast.isDismissed)
    assertNull(toast.dismissTimer)
    assertFalse(timer.isRunning)
  }

  /**
   * Verifies dismiss() invoked from a background thread dispatches to EDT
   * and completes dismissal.
   */
  @Test
  fun testDismissFromBackgroundThreadDispatchesToEdt() {
    val badge = StatusBadge(StatusBadge.Variant.ELEVATED)
    val toast = Toast(null, badge, Toast.LENGTH_SHORT)
    Toast.replaceToast(toast)
    val latch = CountDownLatch(1)
    Thread {
      toast.dismiss()
      SwingUtilities.invokeLater { latch.countDown() }
    }.start()
    assertTrue(latch.await(2, TimeUnit.SECONDS))
    assertTrue(toast.isDismissed)
  }
}
