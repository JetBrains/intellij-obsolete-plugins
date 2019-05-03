/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */

package com.intellij.designer.inspector.impl;

import com.intellij.designer.inspector.Property;
import com.intellij.designer.inspector.PropertyEditor;
import com.intellij.designer.inspector.PropertyInspector;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.EventObject;

/**
 * @author spleaner
 */
public class ValueEditorAdapter extends AbstractCellEditor implements TableCellEditor {
  private final PropertyInspector myInspector;
  private Property myCurrentProperty;
  private PropertyEditor<Property> myCurrentEditor;


  public ValueEditorAdapter(@NotNull final PropertyInspector inspector) {
    myInspector = inspector;
  }

  @Override
  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
    assert value instanceof Property;

    myCurrentProperty = ((Property)value);

    myCurrentEditor = myInspector.getValueEditor(myCurrentProperty);
    if (myCurrentProperty.isValid() && myCurrentEditor != null) {
      return myCurrentEditor.getEditorComponent(myCurrentProperty, new RenderingContextImpl(myInspector, true,  isSelected, false, false));
    }

    return null;
  }

  @Override
  public Object getCellEditorValue() {
    return myCurrentProperty;
  }

  @Override
  public boolean isCellEditable(EventObject anEvent) {
    return true;
  }

  @Override
  public boolean shouldSelectCell(EventObject anEvent) {
    return true;
  }

  @Override
  public boolean stopCellEditing() {
    boolean b = true;
    if (myCurrentEditor != null) {
      b = myCurrentEditor.stopEditing(false);
      resetEditor();
    }

    return super.stopCellEditing() && b;
  }

  @Override
  public void cancelCellEditing() {
    if (myCurrentEditor != null) {
      myCurrentEditor.stopEditing(true);
      resetEditor();
    }

    super.cancelCellEditing();
  }

  private void resetEditor() {
    myCurrentEditor = null;
  }
}
