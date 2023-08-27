package com.tibagni.logviewer.util;

import com.tibagni.logviewer.logger.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

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
        cellRectangle.y, (int) visibleRectangle.getWidth(), (int) visibleRectangle.getHeight() / 2)));
  }

  public static void setLookAndFeel(String className) {
    try {
      UIManager.setLookAndFeel(className);
    } catch (Exception e) {
      // Just ignore
    }
  }

  public static void updateLookAndFeelAfterStart(String className) {
    setLookAndFeel(className);
    Window windows[] = Window.getWindows();
    for (Window window : windows) {
      if (window.isDisplayable()) {
        SwingUtilities.updateComponentTreeUI(window);
      }
    }
  }

  public static <T> void doAsync(Callable<T> onBackground,
                                 Consumer<T> onComplete) {
    doAsync(onBackground, onComplete, null);
  }

  public static <T> void doAsync(Callable<T> onBackground,
                                    Consumer<T> onComplete,
                                    Consumer<Throwable> onFailed) {
    new Thread(() -> {
      try {
        T result = onBackground.call();
        onComplete.accept(result);
      } catch (Throwable tr) {
        if (onFailed != null) {
          SwingUtilities.invokeLater(() -> onFailed.accept(tr));
        }
      }
    }).start();
  }

  public static ImageIcon getIconFromResource(Object object, String path) {
    try {
      InputStream is = object.getClass().getClassLoader().getResourceAsStream(path);
      return new ImageIcon(ImageIO.read(is));
    } catch (IOException e) {
      Logger.error("Failed to open " + path);
      return null;
    }
  }

  public static String truncateTextFor(JLabel dest,
                                       String prefix,
                                       String variableText,
                                       int totalWidth,
                                       JComponent... components) {
    String text = prefix + " " + variableText;
    Logger.verbose("truncateTextFor: " + text);

    int availableWidth = totalWidth;
    for (JComponent component : components) {
      Insets borderInsets = component.getBorder().getBorderInsets(component);
      availableWidth -= component.getWidth();
      availableWidth -= borderInsets.left - borderInsets.right;
    }

    FontMetrics fm = dest.getFontMetrics(dest.getFont());
    int textWidth = fm.stringWidth(text);

    Logger.verbose("AvailableWidth: " + availableWidth);
    Logger.verbose("Text width: " + textWidth);

    if (fm.stringWidth(text) > availableWidth) {
      String newPrefix = prefix + " ...";

      // HACK: Use extra chars here as margin to be sure it will
      // only take available space
      int currentWidth = fm.stringWidth(newPrefix + " ... ");

      int truncateAt = 0;
      for (int i = text.length() - 1; i > 0; i--) {
        currentWidth += fm.charWidth(text.charAt(i));
        Logger.verbose("text width now: " + currentWidth + " - " + text.substring(i + 1));

        if (currentWidth >= availableWidth) {
          truncateAt = i;
          break;
        }
      }
      return newPrefix + text.substring(truncateAt + 1);
    }

    return text;
  }

  public static Color changeColorAlpha(Color source, int alpha) {
    return new Color(source.getRed(), source.getGreen(), source.getBlue(), alpha);
  }

  public static ImageIcon tintImage(ImageIcon originalIcon, Color tintColor) {
    BufferedImage originalImage = new BufferedImage(
        originalIcon.getIconWidth(),
        originalIcon.getIconHeight(),
        BufferedImage.TYPE_INT_ARGB);

    Graphics2D graphics = originalImage.createGraphics();
    originalIcon.paintIcon(null, graphics, 0, 0);
    graphics.dispose();

    BufferedImage tintedImage = new BufferedImage(
        originalIcon.getIconWidth(),
        originalIcon.getIconHeight(),
        BufferedImage.TYPE_INT_ARGB);

    for (int x = 0; x < originalImage.getWidth(); x++) {
      for (int y = 0; y < originalImage.getHeight(); y++) {
        int rgba = originalImage.getRGB(x, y);
        int alpha = (rgba >> 24) & 0xFF;
        int red = (rgba >> 16) & 0xFF;
        int green = (rgba >> 8) & 0xFF;
        int blue = rgba & 0xFF;

        Color newColor = new Color(
            (red + tintColor.getRed()) / 2,
            (green + tintColor.getGreen()) / 2,
            (blue + tintColor.getBlue()) / 2,
            alpha);

        tintedImage.setRGB(x, y, newColor.getRGB());
      }
    }

    return new ImageIcon(tintedImage);
  }

  public static ImageIcon resizeImage(ImageIcon originalIcon, int width, int height) {
    Image image = originalIcon.getImage();
    Image resizedImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    return new ImageIcon(resizedImage);
  }
}
