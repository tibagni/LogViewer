package com.tibagni.logviewer.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.image.BufferedImage

class CommonIconsTests {
  @Test
  fun testAllIconsCreatedSuccessfully() {
    val icons = listOf(
      CommonIcons.playIcon(),
      CommonIcons.stopIcon(),
      CommonIcons.clearIcon(),
      CommonIcons.autoScrollIcon(),
      CommonIcons.terminalIcon(),
      CommonIcons.changeDeviceIcon(),
      CommonIcons.expandIcon(),
      CommonIcons.collapseIcon(),
      CommonIcons.viewListIcon(),
      CommonIcons.myLogsIcon()
    )

    for (icon in icons) {
      assertNotNull(icon)
      assertEquals(25, icon.iconWidth)
      assertEquals(25, icon.iconHeight)

      val image = icon.image as? BufferedImage
      assertNotNull("Icon should be backed by a BufferedImage", image)
      val hasPaintedPixels = (0 until image!!.width).any { x ->
        (0 until image.height).any { y ->
          (image.getRGB(x, y) ushr 24 and 0xFF) > 0
        }
      }
      assertTrue("Icon should contain visible painted pixels", hasPaintedPixels)
    }
  }
}
