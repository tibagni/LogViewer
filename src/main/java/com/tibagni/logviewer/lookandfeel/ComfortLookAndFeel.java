package com.tibagni.logviewer.lookandfeel;

import com.jtattoo.plaf.AbstractLookAndFeel;
import com.jtattoo.plaf.graphite.GraphiteDefaultTheme;
import com.jtattoo.plaf.graphite.GraphiteLookAndFeel;

import javax.swing.plaf.ColorUIResource;
import java.util.Properties;

public class ComfortLookAndFeel extends GraphiteLookAndFeel {
  private static ComfortDefaultTheme myTheme;

  public static void setTheme(Properties themesProps) {
    // Use parent class name here since this is just a small customization
    currentThemeName = "graphiteTheme";
    if (myTheme == null) {
      myTheme = new ComfortDefaultTheme();
    }

    if (myTheme != null && themesProps != null) {
      myTheme.setUpColor();
      myTheme.setProperties(themesProps);
      myTheme.setUpColorArrs();
      AbstractLookAndFeel.setTheme(myTheme);
    }

  }

  public static void setCurrentTheme(Properties themesProps) {
    setTheme(themesProps);
  }

  public String getName() {
    return "Comfort";
  }

  public String getID() {
    return "Comfort";
  }

  public String getDescription() {
    return "The Comfort Look and Feel";
  }

  @Override
  protected void createDefaultTheme() {
    if (myTheme == null) {
      myTheme = new ComfortDefaultTheme();
    }

    setTheme(myTheme);
  }

  public static class ComfortDefaultTheme extends GraphiteDefaultTheme {
    public String getPropertyFileName() {
      return "ComfortTheme.properties";
    }

    @Override
    public void setUpColor() {
      super.setUpColor();
      inputBackgroundColor = new ColorUIResource(235, 235, 235);
    }
  }
}
