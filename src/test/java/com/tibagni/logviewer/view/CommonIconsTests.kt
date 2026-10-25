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

  /**
   * Verifies that [CommonIcons.chevronUpIcon] is created with expected
   * dimensions and non-empty pixel buffers for default and custom sizes.
   */
  @Test
  fun testChevronUpIconCreatedSuccessfully() {
    val defaultIcon = CommonIcons.chevronUpIcon()
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
    assertTrue("chevronUpIcon should have painted pixels", hasPaintedPixels)

    val customIcon = CommonIcons.chevronUpIcon(size = 24)
    assertNotNull(customIcon)
    assertEquals(24, customIcon.iconWidth)
    assertEquals(24, customIcon.iconHeight)
  }

  /**
   * Verifies that [CommonIcons.chevronUpIcon] dynamically resolves
   * foreground color from Look and Feel defaults at paint time.
   */
  @Test
  fun testChevronUpIconThemeAwarePainting() {
    val origLabelFg = UIManager.get("Label.foreground")
    try {
      UIManager.put("Label.foreground", Color.CYAN)
      val icon = CommonIcons.chevronUpIcon()
      assertNotNull(icon)
      val image = icon.image as? BufferedImage
      assertNotNull(image)
      val hasCyanPixel = (0 until image!!.width).any { x ->
        (0 until image.height).any { y ->
          val rgb = image.getRGB(x, y)
          val alpha = (rgb ushr 24) and 0xFF
          val green = (rgb ushr 8) and 0xFF
          val blue = rgb and 0xFF
          alpha > 0 && green > 100 && blue > 100
        }
      }
      assertTrue("chevronUpIcon should paint with theme color", hasCyanPixel)
    } finally {
      UIManager.put("Label.foreground", origLabelFg)
    }
  }

  /**
   * Verifies that [CommonIcons.chevronDownIcon] is created with expected
   * dimensions and non-empty pixel buffers for default and custom sizes.
   */
  @Test
  fun testChevronDownIconCreatedSuccessfully() {
    val defaultIcon = CommonIcons.chevronDownIcon()
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
    assertTrue(
      "chevronDownIcon should have painted pixels",
      hasPaintedPixels
    )

    val customIcon = CommonIcons.chevronDownIcon(size = 24)
    assertNotNull(customIcon)
    assertEquals(24, customIcon.iconWidth)
    assertEquals(24, customIcon.iconHeight)
  }

  /**
   * Verifies that [CommonIcons.chevronDownIcon] dynamically resolves
   * foreground color from Look and Feel defaults at paint time.
   */
  @Test
  fun testChevronDownIconThemeAwarePainting() {
    val origLabelFg = UIManager.get("Label.foreground")
    try {
      UIManager.put("Label.foreground", Color.ORANGE)
      val icon = CommonIcons.chevronDownIcon()
      assertNotNull(icon)
      val image = icon.image as? BufferedImage
      assertNotNull(image)
      val hasOrangePixel = (0 until image!!.width).any { x ->
        (0 until image.height).any { y ->
          val rgb = image.getRGB(x, y)
          val alpha = (rgb ushr 24) and 0xFF
          val red = (rgb ushr 16) and 0xFF
          val green = (rgb ushr 8) and 0xFF
          alpha > 0 && red > 100 && green > 50
        }
      }
      assertTrue(
        "chevronDownIcon should paint with theme color",
        hasOrangePixel
      )
    } finally {
      UIManager.put("Label.foreground", origLabelFg)
    }
  }

  /**
   * Verifies that [CommonIcons.closeIcon] is created with expected
   * dimensions and non-empty pixel buffers for default and custom sizes.
   */
  @Test
  fun testCloseIconCreatedSuccessfully() {
    val defaultIcon = CommonIcons.closeIcon()
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
    assertTrue("closeIcon should have painted pixels", hasPaintedPixels)

    val customIcon = CommonIcons.closeIcon(size = 24)
    assertNotNull(customIcon)
    assertEquals(24, customIcon.iconWidth)
    assertEquals(24, customIcon.iconHeight)
  }

  /**
   * Verifies that [CommonIcons.closeIcon] dynamically resolves
   * foreground color from Look and Feel defaults at paint time.
   */
  @Test
  fun testCloseIconThemeAwarePainting() {
    val origLabelFg = UIManager.get("Label.foreground")
    try {
      UIManager.put("Label.foreground", Color.RED)
      val icon = CommonIcons.closeIcon()
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
      assertTrue("closeIcon should paint with theme color", hasRedPixel)
    } finally {
      UIManager.put("Label.foreground", origLabelFg)
    }
  }

  /**
   * Verifies that [CommonIcons.chevronRightIcon] is created with expected
   * dimensions and non-empty pixel buffers for default and custom sizes.
   */
  @Test
  fun testChevronRightIconCreatedSuccessfully() {
    val defaultIcon = CommonIcons.chevronRightIcon()
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
    assertTrue(
      "chevronRightIcon should have painted pixels",
      hasPaintedPixels
    )

    val customIcon = CommonIcons.chevronRightIcon(size = 24)
    assertNotNull(customIcon)
    assertEquals(24, customIcon.iconWidth)
    assertEquals(24, customIcon.iconHeight)
  }

  /**
   * Verifies that [CommonIcons.chevronRightIcon] dynamically resolves
   * foreground color from Look and Feel defaults at paint time.
   */
  @Test
  fun testChevronRightIconThemeAwarePainting() {
    val origLabelFg = UIManager.get("Label.foreground")
    try {
      UIManager.put("Label.foreground", Color.GREEN)
      val icon = CommonIcons.chevronRightIcon()
      assertNotNull(icon)
      val image = icon.image as? BufferedImage
      assertNotNull(image)
      val hasGreenPixel = (0 until image!!.width).any { x ->
        (0 until image.height).any { y ->
          val rgb = image.getRGB(x, y)
          val alpha = (rgb ushr 24) and 0xFF
          val green = (rgb ushr 8) and 0xFF
          alpha > 0 && green > 100
        }
      }
      assertTrue(
        "chevronRightIcon should paint with theme color",
        hasGreenPixel
      )
    } finally {
      UIManager.put("Label.foreground", origLabelFg)
    }
  }

  /**
   * Verifies that [CommonIcons.chevronLeftIcon] is created with expected
   * dimensions and non-empty pixel buffers for default and custom sizes.
   */
  @Test
  fun testChevronLeftIconCreatedSuccessfully() {
    val defaultIcon = CommonIcons.chevronLeftIcon()
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
    assertTrue("chevronLeftIcon should have painted pixels", hasPaintedPixels)

    val customIcon = CommonIcons.chevronLeftIcon(size = 24)
    assertNotNull(customIcon)
    assertEquals(24, customIcon.iconWidth)
    assertEquals(24, customIcon.iconHeight)
  }

  /**
   * Verifies that [CommonIcons.chevronLeftIcon] dynamically resolves
   * foreground color from Look and Feel defaults at paint time.
   */
  @Test
  fun testChevronLeftIconThemeAwarePainting() {
    val origLabelFg = UIManager.get("Label.foreground")
    try {
      UIManager.put("Label.foreground", Color.BLUE)
      val icon = CommonIcons.chevronLeftIcon()
      assertNotNull(icon)
      val image = icon.image as? BufferedImage
      assertNotNull(image)
      val hasBluePixel = (0 until image!!.width).any { x ->
        (0 until image.height).any { y ->
          val rgb = image.getRGB(x, y)
          val alpha = (rgb ushr 24) and 0xFF
          val blue = rgb and 0xFF
          alpha > 0 && blue > 100
        }
      }
      assertTrue(
        "chevronLeftIcon should paint with theme color",
        hasBluePixel
      )
    } finally {
      UIManager.put("Label.foreground", origLabelFg)
    }
  }

  /**
   * Verifies that [CommonIcons.plusIcon] is created with expected
   * dimensions and non-empty pixel buffers for default and custom sizes.
   */
  @Test
  fun testPlusIconCreatedSuccessfully() {
    val defaultIcon = CommonIcons.plusIcon()
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
    assertTrue("plusIcon should have painted pixels", hasPaintedPixels)

    val customIcon = CommonIcons.plusIcon(size = 24)
    assertNotNull(customIcon)
    assertEquals(24, customIcon.iconWidth)
    assertEquals(24, customIcon.iconHeight)
  }

  /**
   * Verifies that [CommonIcons.plusIcon] dynamically resolves
   * foreground color from Look and Feel defaults at paint time.
   */
  @Test
  fun testPlusIconThemeAwarePainting() {
    val origLabelFg = UIManager.get("Label.foreground")
    try {
      UIManager.put("Label.foreground", Color.MAGENTA)
      val icon = CommonIcons.plusIcon()
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
      assertTrue("plusIcon should paint with theme color", hasMagentaPixel)
    } finally {
      UIManager.put("Label.foreground", origLabelFg)
    }
  }

  /**
   * Verifies that vector icons paint with component foreground when supplied.
   */
  @Test
  fun testDynamicVectorIconPaintsWithComponentForeground() {
    val icon = CommonIcons.chevronUpIcon()
    val btn = FlatButton("")
    btn.foreground = Color.YELLOW
    val img = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
    val g = img.createGraphics()
    try {
      icon.paintIcon(btn, g, 0, 0)
    } finally {
      g.dispose()
    }
    val hasYellowPixel = (0 until img.width).any { x ->
      (0 until img.height).any { y ->
        val rgb = img.getRGB(x, y)
        val alpha = (rgb ushr 24) and 0xFF
        val red = (rgb ushr 16) and 0xFF
        val green = (rgb ushr 8) and 0xFF
        alpha > 0 && red > 100 && green > 100
      }
    }
    assertTrue("Should paint using button foreground", hasYellowPixel)
  }

  /**
   * Verifies that [CommonIcons.folderIcon] is created with expected
   * dimensions and non-empty pixel buffers for default and custom sizes.
   */
  @Test
  fun testFolderIconCreatedSuccessfully() {
    val defaultIcon = CommonIcons.folderIcon()
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
    assertTrue("folderIcon should have painted pixels", hasPaintedPixels)

    val customIcon = CommonIcons.folderIcon(size = 24)
    assertNotNull(customIcon)
    assertEquals(24, customIcon.iconWidth)
    assertEquals(24, customIcon.iconHeight)
  }

  /**
   * Verifies that [CommonIcons.folderIcon] dynamically resolves
   * foreground color from Look and Feel defaults at paint time.
   */
  @Test
  fun testFolderIconThemeAwarePainting() {
    val origLabelFg = UIManager.get("Label.foreground")
    try {
      UIManager.put("Label.foreground", Color.GREEN)
      val icon = CommonIcons.folderIcon()
      assertNotNull(icon)
      val image = icon.image as? BufferedImage
      assertNotNull(image)
      val hasGreenPixel = (0 until image!!.width).any { x ->
        (0 until image.height).any { y ->
          val rgb = image.getRGB(x, y)
          val alpha = (rgb ushr 24) and 0xFF
          val green = (rgb ushr 8) and 0xFF
          alpha > 0 && green > 100
        }
      }
      assertTrue(
        "folderIcon should paint with theme color",
        hasGreenPixel
      )
    } finally {
      UIManager.put("Label.foreground", origLabelFg)
    }
  }
}
