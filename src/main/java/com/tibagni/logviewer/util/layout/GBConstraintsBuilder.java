package com.tibagni.logviewer.util.layout;

import java.awt.*;

public class GBConstraintsBuilder {
  private int gridx = GridBagConstraints.RELATIVE;
  private int gridy = GridBagConstraints.RELATIVE;
  private int gridwidth = 1;
  private int gridheight = 1;

  private double weightx = 0;
  private double weighty = 0;
  private int anchor = GridBagConstraints.CENTER;
  private int fill = GridBagConstraints.NONE;

  private Insets insets = new Insets(0, 0, 0, 0);
  private int ipadx = 0;
  private int ipady = 0;

  public GBConstraintsBuilder withGridx(int gridx) {
    this.gridx = gridx;
    return this;
  }

  public GBConstraintsBuilder withGridy(int gridy) {
    this.gridy = gridy;
    return this;
  }

  public GBConstraintsBuilder withGridWidth(int gridwidth) {
    this.gridwidth = gridwidth;
    return this;
  }

  public GBConstraintsBuilder withGridheight(int gridheight) {
    this.gridheight = gridheight;
    return this;
  }

  public GBConstraintsBuilder withWeightx(double weightx) {
    this.weightx = weightx;
    return this;
  }

  public GBConstraintsBuilder withWeighty(double weighty) {
    this.weighty = weighty;
    return this;
  }

  public GBConstraintsBuilder withAnchor(int anchor) {
    this.anchor = anchor;
    return this;
  }

  public GBConstraintsBuilder withFill(int fill) {
    this.fill = fill;
    return this;
  }

  public GBConstraintsBuilder withInsets(Insets insets) {
    this.insets = insets;
    return this;
  }

  public GBConstraintsBuilder withIpadx(int ipadx) {
    this.ipadx = ipadx;
    return this;
  }

  public GBConstraintsBuilder withIpady(int ipady) {
    this.ipady = ipady;
    return this;
  }

  public GridBagConstraints build() {
    return new GridBagConstraints(gridx, gridy, gridwidth, gridheight,
        weightx, weighty, anchor, fill, insets, ipadx, ipady);
  }
}
