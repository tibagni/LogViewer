package com.tibagni.logviewer.util.layout;

import java.awt.*;

public class FontBuilder {
  private String name;
  private int style;
  private int size;

  public FontBuilder(Component component) {
    Font currentFont = component.getFont();
    if (currentFont != null) {
      name = currentFont.getName();
      style = currentFont.getStyle();
      size = currentFont.getSize();
    }
  }

  public FontBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public FontBuilder withStyle(int style) {
    this.style = style;
    return this;
  }

  public FontBuilder withSize(int size) {
    this.size = size;
    return this;
  }

  public Font build() {
    return new Font(name, style, size);
  }
}
