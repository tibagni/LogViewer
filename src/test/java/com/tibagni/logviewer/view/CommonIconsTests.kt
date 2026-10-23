package com.tibagni.logviewer.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Color
import java.awt.image.BufferedImage
import javax.swing.UIManager

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

  /**
   * Verifies that [CommonIcons.fileDocumentIcon] is created with expected
   * dimensions and non-empty pixel buffers for default and custom sizes.
   */
  @Test
  fun testFileDocumentIconCreatedSuccessfully() {
    val defaultIcon = CommonIcons.fileDocumentIcon()
    assertNotNull(defaultIcon)
    assertEquals(16, defaultIcon.iconWidth)
    assertEquals(16, defaultIcon.iconHeight)

    val image = defaultIcon.image as? BufferedImage
    assertNotNull("Icon should be backed by a BufferedImage", image)
    val hasPaintedPixels = (0 until image!!.width).any { x ->
      (0 until image.height).any { y ->
        (image.getRGB(x, y) ushr 24 and 0xFF) > 0
      }
    }
    assertTrue("fileDocumentIcon should have painted pixels", hasPaintedPixels)

    val customIcon = CommonIcons.fileDocumentIcon(size = 24)
    assertNotNull(customIcon)
    assertEquals(24, customIcon.iconWidth)
    assertEquals(24, customIcon.iconHeight)
  }

  /**
   * Verifies that [CommonIcons.fileDocumentIcon] dynamically resolves
   * foreground color from Look and Feel defaults at paint time.
   */
  @Test
  fun testFileDocumentIconThemeAwarePainting() {
    val origLabelFg = UIManager.get("Label.foreground")
    try {
      UIManager.put("Label.foreground", Color.RED)
      val icon = CommonIcons.fileDocumentIcon()
      assertNotNull(icon)
      val image = icon.image as? BufferedImage
      assertNotNull(image)
      val hasRedPixel = (0 until image!!.width).any { x ->
        (0 until image.height).any { y ->
          val rgb = image.getRGB(x, y)
          val alpha = (rgb ushr 24) and 0xFF
          val red = (rgb ushr 16) and 0xFF
          alpha > 0 && red > 100
        }
      }
      assertTrue("Icon should paint with theme color", hasRedPixel)
    } finally {
      UIManager.put("Label.foreground", origLabelFg)
    }
  }

  /**
   * Verifies that [CommonIcons.smartphoneIcon] is created with expected
   * dimensions and non-empty pixel buffers for default and custom sizes.
   */
  @Test
  fun testSmartphoneIconCreatedSuccessfully() {
    val defaultIcon = CommonIcons.smartphoneIcon()
    assertNotNull(defaultIcon)
    assertEquals(16, defaultIcon.iconWidth)
    assertEquals(16, defaultIcon.iconHeight)

    val image = defaultIcon.image as? BufferedImage
    assertNotNull("Icon should be backed by a BufferedImage", image)
    val hasPaintedPixels = (0 until image!!.width).any { x ->
      (0 until image.height).any { y ->
        (image.getRGB(x, y) ushr 24 and 0xFF) > 0
      }
    }
    assertTrue("smartphoneIcon should have painted pixels", hasPaintedPixels)

    val customIcon = CommonIcons.smartphoneIcon(size = 24)
    assertNotNull(customIcon)
    assertEquals(24, customIcon.iconWidth)
    assertEquals(24, customIcon.iconHeight)
  }

  /**
   * Verifies that [CommonIcons.smartphoneIcon] dynamically resolves
   * foreground color from Look and Feel defaults at paint time.
   */
  @Test
  fun testSmartphoneIconThemeAwarePainting() {
    val origLabelFg = UIManager.get("Label.foreground")
    try {
      UIManager.put("Label.foreground", Color.MAGENTA)
      val icon = CommonIcons.smartphoneIcon()
      assertNotNull(icon)
      val image = icon.image as? BufferedImage
      assertNotNull(image)
      val hasMagentaPixel = (0 until image!!.width).any { x ->
        (0 until image.height).any { y ->
          val rgb = image.getRGB(x, y)
          val alpha = (rgb ushr 24) and 0xFF
          val red = (rgb ushr 16) and 0xFF
          val blue = rgb and 0xFF
          alpha > 0 && red > 100 && blue > 100
        }
      }
      assertTrue("Icon should paint with theme color", hasMagentaPixel)
    } finally {
      UIManager.put("Label.foreground", origLabelFg)
    }
  }

  /**
   * Verifies that [CommonIcons.statusIndicatorIcon] paints active green
   * indicators and inactive neutral indicators correctly.
   */
  @Test
  fun testStatusIndicatorIconActiveAndInactive() {
    val activeIcon = CommonIcons.statusIndicatorIcon(active = true)
    assertNotNull(activeIcon)
    assertEquals(16, activeIcon.iconWidth)
    assertEquals(16, activeIcon.iconHeight)

    val activeImage = activeIcon.image as? BufferedImage
    assertNotNull("Active icon should have BufferedImage", activeImage)
    val hasGreenPixel = (0 until activeImage!!.width).any { x ->
      (0 until activeImage.height).any { y ->
        val rgb = activeImage.getRGB(x, y)
        val alpha = (rgb ushr 24) and 0xFF
        val red = (rgb ushr 16) and 0xFF
        val green = (rgb ushr 8) and 0xFF
        alpha > 0 && green > 150 && red < 100
      }
    }
    assertTrue("Active icon should paint green pixels", hasGreenPixel)

    val inactiveIcon = CommonIcons.statusIndicatorIcon(active = false)
    assertNotNull(inactiveIcon)
    assertEquals(16, inactiveIcon.iconWidth)
    assertEquals(16, inactiveIcon.iconHeight)

    val inactiveImage = inactiveIcon.image as? BufferedImage
    assertNotNull("Inactive icon should have BufferedImage", inactiveImage)
    val hasInactivePixels = (0 until inactiveImage!!.width).any { x ->
      (0 until inactiveImage.height).any { y ->
        (inactiveImage.getRGB(x, y) ushr 24 and 0xFF) > 0
      }
    }
    assertTrue(
      "Inactive icon should paint neutral pixels",
      hasInactivePixels
    )

    val customSizeIcon = CommonIcons.statusIndicatorIcon(
      active = true,
      size = 24
    )
    assertEquals(24, customSizeIcon.iconWidth)
    assertEquals(24, customSizeIcon.iconHeight)
  }
}
