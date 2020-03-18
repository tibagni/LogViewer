package com.tibagni.logviewer.view;

import com.tibagni.logviewer.util.scaling.UIScaleUtils;

import javax.swing.*;
import javax.swing.plaf.UIResource;
import java.awt.*;
import java.io.Serializable;

public class CustomCheckboxIcon implements Icon, UIResource, Serializable {

    protected int getControlSize() { return UIScaleUtils.dip(15); }
    private CustomCheckBoxPainter painter = new CustomCheckBoxPainter();

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        painter.paintIcon(c, g, x, y);
    }

    @Override
    public int getIconWidth() {
        return getControlSize();
    }

    @Override
    public int getIconHeight() {
        return getControlSize();
    }
}
