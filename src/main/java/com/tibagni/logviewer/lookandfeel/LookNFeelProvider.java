package com.tibagni.logviewer.lookandfeel;

import com.tibagni.logviewer.logger.Logger;

import javax.swing.*;
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
    // First add the system Look And Feels
    addStockLookAndFeels();

    // Then add some other Look nd Feels from JTatoo
    addCustomLookAndFeels();
  }

  private void addStockLookAndFeels() {
    for (UIManager.LookAndFeelInfo lnf : UIManager.getInstalledLookAndFeels()) {
      availableLookNFeels.add(new LookNFeel(lnf.getName(),
          lnf.getName(), lnf.getClassName(), false));
    }
  }

  private void addCustomLookAndFeels() {
    availableLookNFeels.add(new LookNFeel("Acryl","Acryl",
      "com.jtattoo.plaf.acryl.AcrylLookAndFeel", true));
    availableLookNFeels.add(new LookNFeel("Aero","Aero",
        "com.jtattoo.plaf.aero.AeroLookAndFeel", true));
    availableLookNFeels.add(new LookNFeel("HiFi",
        "HiFi (Dark)", "com.jtattoo.plaf.hifi.HiFiLookAndFeel", true));
    availableLookNFeels.add(new LookNFeel("Noire", "Noire (Dark)",
        "com.jtattoo.plaf.noire.NoireLookAndFeel", true));
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

  public LookNFeel getBySystemName(String name) {
    for (LookNFeel lnf : availableLookNFeels) {
      if (lnf.getSystemName().equals(name)) {
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
    props.put("logoString", "");

    if (!lookNFeel.canSetTheme()) return;

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
