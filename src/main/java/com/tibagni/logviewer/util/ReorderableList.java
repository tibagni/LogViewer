package com.tibagni.logviewer.util;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;

public class ReorderableList extends JList implements DragGestureListener, DragSourceListener {
  private DragSource dragSource = new DragSource();
  private OnReorderedListener reorderedListener;

  public ReorderableList() {
    super();
    setDragEnabled(true);
    setDropMode(DropMode.INSERT);
    setTransferHandler(new MyTransferHandler());
    dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);
  }

  public void setReorderedListener(OnReorderedListener reorderedListener) {
    this.reorderedListener = reorderedListener;
  }

  @Override
  public void dragGestureRecognized(DragGestureEvent dge) {
    StringSelection transferable = new StringSelection(Integer.toString(getSelectedIndex()));
    dragSource.startDrag(dge, DragSource.DefaultCopyDrop, transferable, this);
  }

  @Override
  public void dragEnter(DragSourceDragEvent dsde) {
  }

  @Override
  public void dragOver(DragSourceDragEvent dsde) {
  }

  @Override
  public void dropActionChanged(DragSourceDragEvent dsde) {
  }

  @Override
  public void dragExit(DragSourceEvent dse) {
  }

  @Override
  public void dragDropEnd(DragSourceDropEvent dsde) {
  }

  private class MyTransferHandler extends TransferHandler {

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
      if (!support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        return false;
      }

      if (reorderedListener == null) {
        return false;
      }

      JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
      return dl.getIndex() != -1;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
      if (!canImport(support)) {
        return false;
      }

      Transferable transferable = support.getTransferable();
      String indexString;
      try {
        indexString = (String) transferable.getTransferData(DataFlavor.stringFlavor);
      } catch (Exception e) {
        return false;
      }

      int index = Integer.parseInt(indexString);
      JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
      int dropTargetIndex = dl.getIndex();

      reorderedListener.onReordered(index, dropTargetIndex);
      return true;
    }
  }

  public interface OnReorderedListener {
    void onReordered(int orig, int dest);
  }
}
