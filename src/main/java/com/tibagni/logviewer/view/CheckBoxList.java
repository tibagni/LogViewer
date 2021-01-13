package com.tibagni.logviewer.view;

import com.tibagni.logviewer.util.CommonUtils;
import com.tibagni.logviewer.util.scaling.UIScaleUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

// JList does not support interactive components. So, in order to simulate
// a clickable check box, we need to implement click listener, change the item's state
// (checked/unchecked) and redraw the item
public class CheckBoxList<E> extends JList<E> {
  private ItemsCheckListener listener;
  private final JCheckBox dummyCheckBox;

  public CheckBoxList() {
    super();
    addMouseListener(new MyMouseListener());
    addKeyListener(new MyKeyListener());
    dummyCheckBox = new JCheckBox();
  }

  public void setItemsCheckListener(ItemsCheckListener listener) {
    this.listener = listener;
  }

  private boolean isOnCheckBox(Point point) {
    // Check the width of dummy check box to determine if the click matches a checkbox size
    Dimension d = dummyCheckBox.getPreferredSize();
    return point.x <= d.getWidth();
  }

  private void notifyItemCheckChanged(int index) {
    java.util.List<E> elements = CommonUtils.listOf(getModel().getElementAt(index));
    if (listener != null) {
      listener.onItemsCheckChanged(elements);
      repaint(getCellBounds(index, index));
    }
  }

  private void notifyItemsCheckChanged(int[] indices) {
    if (indices.length == 0) {
      return;
    }

    java.util.List<E> elements = new ArrayList<>();
    for (int index : indices) {
      elements.add(getModel().getElementAt(index));
    }

    if (listener != null) {
      listener.onItemsCheckChanged(elements);
      repaint(getCellBounds(indices[0], indices[indices.length - 1]));
    }

  }

  private class MyMouseListener extends MouseAdapter {

    @Override
    public void mouseClicked(MouseEvent e) {
      Point clickPoint = e.getPoint();
      if (isOnCheckBox(clickPoint)) {
        int index = locationToIndex(clickPoint);
        notifyItemCheckChanged(index);
      }
    }
  }

  private class MyKeyListener extends KeyAdapter {
    @Override
    public void keyPressed(KeyEvent e) {
      if (e.getKeyCode() == KeyEvent.VK_SPACE) {
        notifyItemsCheckChanged(getSelectedIndices());
      }
    }
  }

  public interface ItemsCheckListener<E> {
    void onItemsCheckChanged(java.util.List<E> elements);
  }
}
