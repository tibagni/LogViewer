package com.tibagni.logviewer.view;

import com.tibagni.logviewer.util.scaling.UIScaleUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;

public class CustomCheckBoxPainter implements Painter<JComponent> {

    protected int getControlSize() { return UIScaleUtils.dip(15); }

    @Override
    public void paint(Graphics2D g, JComponent c, int x, int y) {
        paintIcon(c, g, x, y);
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        ButtonModel model = (c instanceof JCheckBoxMenuItem) ?
                ((JCheckBoxMenuItem) c).getModel() : ((JCheckBox) c).getModel();
        int controlSize = getControlSize();

        if (model.isEnabled()) {
            g.setColor(UIManager.getColor("TextPane.background")); // Use same background as text components
            g.fillRect( x, y, controlSize - UIScaleUtils.dip(1), controlSize - UIScaleUtils.dip(1));
            g.setColor(UIManager.getColor("CheckBox.foreground"));
        } else {
            g.setColor(UIManager.getColor("CheckBox.disabledText"));
            g.drawRect( x, y, controlSize - UIScaleUtils.dip(1), controlSize - UIScaleUtils.dip(1));
        }

        drawBorder(g, x, y, controlSize, controlSize);
        if (model.isSelected()) {
            drawCheck(g, x, y);
        }
    }

    protected void drawBorder(Graphics g, int x, int y, int w, int h) {
        g.translate( x, y);
        g.drawRect( 0, 0, w - UIScaleUtils.dip(1), h - UIScaleUtils.dip(1));
        g.translate(-x, -y);
    }

    protected void drawCheck(Graphics g, int x, int y) {
        int controlSize = getControlSize();
        if (g instanceof Graphics2D) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(new BasicStroke(UIScaleUtils.dipf(1.5f)));

            int xStart = x + UIScaleUtils.dip(6);
            int xEnd = x + (controlSize - UIScaleUtils.dip(5));

            int yStart = y + UIScaleUtils.dip(4);
            int yEnd = y + (controlSize - UIScaleUtils.dip(5));
            int yMiddle = (yStart + yEnd) / 2;

            g2.draw(new Line2D.Float(xStart - UIScaleUtils.dip(2), yMiddle, xStart, yEnd));
            g2.draw(new Line2D.Float(xStart, yEnd, xEnd, yStart));
        } else {
            // If it is not Graphics2d, we cannot set the thickness of the line, so draw the icon a little differently
            g.fillRect(x + UIScaleUtils.dip(3), y + UIScaleUtils.dip(5),
                    UIScaleUtils.dip(2), controlSize - UIScaleUtils.dip(8));

            g.drawLine(x + (controlSize - UIScaleUtils.dip(4)), y + UIScaleUtils.dip(3),
                    x + UIScaleUtils.dip(5), y + (controlSize - UIScaleUtils.dip(6)));
            g.drawLine(x + (controlSize - UIScaleUtils.dip(4)), y + UIScaleUtils.dip(4),
                    x + UIScaleUtils.dip(5), y + (controlSize - UIScaleUtils.dip(5)));
        }
    }
}
