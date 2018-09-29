package com.tibagni.logviewer.lookandfeel;

public class LookNFeel {
  private final String name;
  private final String cls;

  public LookNFeel(String name, String cls) {
    this.name = name;
    this.cls = cls;
  }

  public String getName() {
    return name;
  }

  public String getCls() {
    return cls;
  }

  @Override
  public String toString() {
    return name;
  }
}
