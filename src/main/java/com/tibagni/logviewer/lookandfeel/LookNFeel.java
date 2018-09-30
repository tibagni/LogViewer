package com.tibagni.logviewer.lookandfeel;

public class LookNFeel {
  private final String systemName;
  private final String name;
  private final String cls;
  private final boolean canSetTheme;

  LookNFeel(String systemName, String name, String cls, boolean canSetTheme) {
    this.name = name;
    this.cls = cls;
    this.systemName = systemName;
    this.canSetTheme = canSetTheme;
  }

  String getSystemName() {
    return systemName;
  }

  public String getName() {
    return name;
  }

  public String getCls() {
    return cls;
  }

  public boolean canSetTheme() {
    return canSetTheme;
  }

  @Override
  public String toString() {
    return name;
  }
}
