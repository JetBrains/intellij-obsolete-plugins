/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.designer.inspector.impl;

import com.intellij.designer.inspector.Property;
import com.intellij.designer.inspector.PropertyInspector;
import com.intellij.designer.inspector.PropertyRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * @author spleaner
 */
public class ValueRendererAdapter implements TableCellRenderer {
  private final PropertyInspector myInspector;
  private final TableCellRenderer myDefaultRenderer;


  public ValueRendererAdapter(@NotNull final PropertyInspector inspector, @NotNull final TableCellRenderer defaultRenderer) {
    myInspector = inspector;
    myDefaultRenderer = defaultRenderer;
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    assert value instanceof Property;

    final Property p = ((Property)value);

    final PropertyRenderer<Property> renderer = myInspector.getValueRenderer(p);
    if (p.isValid() && renderer != null) {
      final JComponent c = renderer.getRendererComponent(p, new RenderingContextImpl(myInspector, false, isSelected, hasFocus, false));
      if (c != null) return c;
    }

    return myDefaultRenderer.getTableCellRendererComponent(table, p.getValue(), isSelected, hasFocus, row, column);
  }
}
