package com.tibagni.logviewer.lookandfeel;

import com.tibagni.logviewer.logger.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class LookNFeelProvider {
  private static LookNFeelProvider instance = new LookNFeelProvider();
  private final List<LookNFeel> availableLookNFeels;

  private LookNFeelProvider() {
    availableLookNFeels = new ArrayList<>();
    init();
  }

  public static LookNFeelProvider getInstance() {
    return instance;
  }

  private void init() {
    availableLookNFeels.add(new LookNFeel("Acryl", "com.jtattoo.plaf.acryl.AcrylLookAndFeel"));
    availableLookNFeels.add(new LookNFeel("Aero", "com.jtattoo.plaf.aero.AeroLookAndFeel"));
    availableLookNFeels.add(new LookNFeel("HiFi (Dark)", "com.jtattoo.plaf.hifi.HiFiLookAndFeel"));
    availableLookNFeels.add(new LookNFeel("Noire (Dark)", "com.jtattoo.plaf.noire.NoireLookAndFeel"));
  }

  public List<LookNFeel> getAvailableLookNFeels() {
    List<LookNFeel> retVal = new ArrayList<>();
    retVal.addAll(availableLookNFeels);
    return retVal;
  }

  public LookNFeel getByName(String name) {
    for (LookNFeel lnf : availableLookNFeels) {
      if (lnf.getName().equals(name)) {
        return lnf;
      }
    }

    return null;
  }

  public LookNFeel getByClass(String cls) {
    for (LookNFeel lnf : availableLookNFeels) {
      if (lnf.getCls().equals(cls)) {
        return lnf;
      }
    }

    return null;
  }

  public void applyTheme(LookNFeel lookNFeel) {
    Properties props = new Properties();
    props.put("logoString", "Log Viewer");

    try {
      Class<?> lookNFeelClass = Class.forName(lookNFeel.getCls());
      Method method = lookNFeelClass.getMethod("setCurrentTheme",
          Properties.class);
      method.invoke(null, props);
    } catch (ClassNotFoundException | NoSuchMethodException |
        IllegalAccessException | InvocationTargetException e) {
      Logger.error("Failed to parse Look And Feel Class", e);
    }
  }
}
