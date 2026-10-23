package com.tibagni.logviewer.view

import com.tibagni.logviewer.util.scaling.UIScaleUtils
import java.awt.Component
import java.awt.GraphicsEnvironment
import java.awt.Point
import javax.swing.Icon
import javax.swing.Popup
import javax.swing.PopupFactory
import javax.swing.SwingUtilities
import javax.swing.Timer

/**
 * Modern Material 3 floating pill toast notification reusing [StatusBadge]
 * with [StatusBadge.Variant.ELEVATED].
 *
 * Displays lightweight, non-intrusive feedback centered horizontally above
 * the bottom of a parent component. Adapts to light and dark themes,
 * supports optional icons, and automatically dismisses after a specified
 * duration.
 *
 * @property parent the parent component anchoring the toast.
 * @property badge the visual [StatusBadge] pill component.
 * @property duration display duration in milliseconds.
 */
class Toast internal constructor(
  private val parent: Component?,
  internal val badge: StatusBadge,
  private val duration: Int = LENGTH_SHORT
) {
  internal var popup: Popup? = null
  internal var dismissTimer: Timer? = null
  internal var isDismissed: Boolean = false
    private set

  /**
   * Displays the toast on screen using [PopupFactory] and schedules its
   * automatic dismissal via a Swing [Timer].
   */
  internal fun show() {
    val activeParent = parent
    if (activeParent == null || !activeParent.isShowing) return

    val size = badge.preferredSize
    val location = calculateLocation(
      activeParent,
      size.width,
      size.height
    )
    val createdPopup = PopupFactory.getSharedInstance().getPopup(
      activeParent,
      badge,
      location.x,
      location.y
    )
    popup = createdPopup
    createdPopup.show()

    val timer = Timer(duration) {
      dismiss()
    }
    timer.isRepeats = false
    dismissTimer = timer
    timer.start()
  }

  /**
   * Dismisses this toast notification, stopping its auto-dismiss timer
   * and hiding the active popup window on the Swing Event Dispatch Thread.
   */
  fun dismiss() {
    isDismissed = true
    dismissTimer?.stop()
    dismissTimer = null
    if (currentToast === this) {
      currentToast = null
    }

    val activePopup = popup ?: return
    popup = null

    if (SwingUtilities.isEventDispatchThread()) {
      try {
        activePopup.hide()
      } catch (_: Throwable) {
        // Ignored if popup already hidden or disposed
      }
    } else {
      SwingUtilities.invokeLater {
        try {
          activePopup.hide()
        } catch (_: Throwable) {
          // Ignored if popup already hidden or disposed
        }
      }
    }
  }

  companion object {
    /**
     * Default duration for long toast messages (3500 milliseconds).
     */
    @JvmField
    val LENGTH_LONG: Int = 3500

    /**
     * Default duration for short toast messages (2000 milliseconds).
     */
    @JvmField
    val LENGTH_SHORT: Int = 2000

    /**
     * Bottom margin in density-independent pixels between the bottom
     * edge of the parent component and the bottom edge of the toast.
     */
    internal val BOTTOM_MARGIN: Int
      get() = UIScaleUtils.dip(50)

    internal var currentToast: Toast? = null

    /**
     * Replaces the currently active toast with a new toast instance,
     * ensuring any existing toast is cleanly dismissed first.
     *
     * @param newToast the replacement toast, or null to clear.
     */
    internal fun replaceToast(newToast: Toast?) {
      currentToast?.dismiss()
      currentToast = newToast
    }

    /**
     * Dismisses the currently visible toast notification if present.
     */
    @JvmStatic
    fun dismissCurrentToast() {
      replaceToast(null)
    }

    /**
     * Displays a modern floating pill toast notification with text only.
     *
     * @param parent the parent component anchoring the toast.
     * @param message the message text to display.
     * @param duration display duration in milliseconds (defaults to
     *   [LENGTH_SHORT]).
     */
    @JvmStatic
    @JvmOverloads
    fun showToast(
      parent: Component?,
      message: String,
      duration: Int = LENGTH_SHORT
    ) {
      showToast(parent, message, null, duration)
    }

    /**
     * Displays a modern floating pill toast notification with an icon
     * using the default [LENGTH_SHORT] duration.
     *
     * @param parent the parent component anchoring the toast.
     * @param message the message text to display.
     * @param icon optional icon to display alongside the text.
     */
    @JvmStatic
    fun showToast(
      parent: Component?,
      message: String,
      icon: Icon?
    ) {
      showToast(parent, message, icon, LENGTH_SHORT)
    }

    /**
     * Displays a modern floating pill toast notification with an optional
     * icon.
     *
     * @param parent the parent component anchoring the toast.
     * @param message the message text to display.
     * @param icon optional icon to display alongside the text.
     * @param duration display duration in milliseconds (defaults to
     *   [LENGTH_SHORT]).
     */
    @JvmStatic
    fun showToast(
      parent: Component?,
      message: String,
      icon: Icon?,
      duration: Int = LENGTH_SHORT
    ) {
      if (GraphicsEnvironment.isHeadless()) return
      if (parent == null || !parent.isShowing) return

      val showAction = Runnable {
        if (!parent.isShowing) return@Runnable
        val badge = StatusBadge(StatusBadge.Variant.ELEVATED).apply {
          text = message
          this.icon = icon
          arcDp = 16
          paddingVerticalDp = 8
          paddingHorizontalDp = 16
          isVisible = true
        }
        val toast = Toast(parent, badge, duration)
        replaceToast(toast)
        toast.show()
      }

      if (SwingUtilities.isEventDispatchThread()) {
        showAction.run()
      } else {
        SwingUtilities.invokeLater(showAction)
      }
    }

    /**
     * Calculates the toast coordinates horizontally centered on the parent
     * and offset from the parent bottom by [BOTTOM_MARGIN].
     *
     * @param parentLocation screen coordinates of the parent component.
     * @param parentWidth width of the parent component.
     * @param parentHeight height of the parent component.
     * @param toastWidth width of the toast component.
     * @param toastHeight height of the toast component.
     * @return the calculated [Point] on screen.
     */
    internal fun calculateLocation(
      parentLocation: Point,
      parentWidth: Int,
      parentHeight: Int,
      toastWidth: Int,
      toastHeight: Int
    ): Point {
      val x = parentLocation.x + (parentWidth - toastWidth) / 2
      val y = parentLocation.y + parentHeight - toastHeight - BOTTOM_MARGIN
      return Point(x, y)
    }

    /**
     * Calculates the toast coordinates for the given parent component.
     *
     * @param parent the parent component to anchor to.
     * @param toastWidth width of the toast component.
     * @param toastHeight height of the toast component.
     * @return the calculated [Point] on screen.
     */
    internal fun calculateLocation(
      parent: Component,
      toastWidth: Int,
      toastHeight: Int
    ): Point {
      val locationOnScreen = if (parent.isShowing) {
        parent.locationOnScreen
      } else {
        Point(0, 0)
      }
      return calculateLocation(
        locationOnScreen,
        parent.width,
        parent.height,
        toastWidth,
        toastHeight
      )
    }
  }
}
