package com.tibagni.logviewer.util;

import com.tibagni.logviewer.preferences.LogViewerPreferencesDialog;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A collection of utility methods for Swing.
 *
 * @author Darryl Burke
 */
public final class SwingUtils {

  private SwingUtils() {
    throw new Error("SwingUtils is just a container for static methods");
  }


  /**
   * Convenience method for searching below <code>container</code> in the
   * component hierarchy and return nested components that are instances of
   * class <code>clazz</code> it finds. Returns an empty list if no such
   * components exist in the container.
   * <p>
   * Invoking this method with a class parameter of JComponent.class
   * will return all nested components.
   *
   * @param clazz     the class of components whose instances are to be found.
   * @param container the container at which to begin the search
   * @param nested    true to list components nested within another listed
   *                  component, false otherwise
   * @return the List of components
   */
  public static <T extends JComponent> List<T> getDescendantsOfType(
      Class<T> clazz, Container container, boolean nested) {
    List<T> tList = new ArrayList<>();
    for (Component component : container.getComponents()) {
      if (clazz.isAssignableFrom(component.getClass())) {
        tList.add(clazz.cast(component));
      }
      if (nested || !clazz.isAssignableFrom(component.getClass())) {
        tList.addAll(SwingUtils.getDescendantsOfType(clazz,
            (Container) component, nested));
      }
    }
    return tList;
  }

  /**
   * Convenience method to remove buttons from a dialog
   *
   * @param dialog The dialog whose buttons should be removed from.
   */
  public static void removeButtonsFromDialog(JDialog dialog) {
    java.util.List<JButton> components = getDescendantsOfType(JButton.class, dialog, true);
    JButton button = components.get(0);
    button.setVisible(false);
  }

  public static void scrollToVisible(JTable table, int rowIndex) {
    JViewport viewport = (JViewport) table.getParent();
    Rectangle cellRectangle = table.getCellRect(rowIndex, 0, true);
    Rectangle visibleRectangle = viewport.getVisibleRect();
    SwingUtilities.invokeLater(() -> table.scrollRectToVisible(new Rectangle(cellRectangle.x,
        cellRectangle.y, (int) visibleRectangle.getWidth(), (int) visibleRectangle.getHeight())));
  }

  public static void setLookAndFeel(String className) {
    try {
      UIManager.setLookAndFeel(className);
    } catch (Exception e) {
      // Just ignore
    }
  }

  public static void updateLookAndFeelAfterStart(String className, Frame frame) {
    setLookAndFeel(className);
    SwingUtilities.updateComponentTreeUI(frame);
    frame.pack();
  }
}
