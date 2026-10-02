package com.tibagni.logviewer.view

import com.tibagni.logviewer.rc.UIScaleConfig
import com.tibagni.logviewer.util.scaling.UIScaleUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.awt.Dimension
import java.awt.image.BufferedImage

class LoadingSpinnerTests {

  @Before
  fun setUp() {
    UIScaleUtils.initialize(null)
  }

  @Test
  fun testInitialDimensions() {
    val spinner = LoadingSpinner()
    assertNotNull(spinner.preferredSize)
    assertTrue(spinner.preferredSize.width >= 18)
    assertTrue(spinner.preferredSize.height >= 18)
  }

  @Test
  fun testVisibilityChangesTimer() {
    val spinner = LoadingSpinner()
    // Initially not visible when setVisible(false) is called
    spinner.isVisible = false
    assertFalse(spinner.isVisible)
    assertFalse(spinner.isTimerRunning)

    spinner.isVisible = true
    assertTrue(spinner.isVisible)
    assertTrue(spinner.isTimerRunning)

    spinner.isVisible = false
    assertFalse(spinner.isVisible)
    assertFalse(spinner.isTimerRunning)
  }

  @Test
  fun testPaintingWhenVisible() {
    val spinner = LoadingSpinner()
    spinner.size = Dimension(24, 24)
    spinner.isVisible = true

    val img = BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB)
    spinner.paint(img.graphics)

    // Verify painting succeeds without exceptions
    assertNotNull(img)
    spinner.isVisible = false
  }

  @Test
  fun testRemoveNotifyStopsTimer() {
    val spinner = LoadingSpinner()
    spinner.isVisible = true
    assertTrue(spinner.isTimerRunning)

    spinner.removeNotify()
    assertFalse(spinner.isTimerRunning)
  }
}
