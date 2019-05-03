/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.designer.inspector.impl;

import com.intellij.designer.inspector.Property;
import com.intellij.designer.inspector.PropertyInspector;
import com.intellij.designer.inspector.PropertyRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

/**
 * @author spleaner
 */
public class NameRendererAdapter implements TreeCellRenderer {

  private final PropertyInspector myInspector;
  private final TreeCellRenderer myDefaultRenderer;

  public NameRendererAdapter(@NotNull final PropertyInspector inspector, @NotNull final TreeCellRenderer defaultRenderer) {
    myInspector = inspector;
    myDefaultRenderer = defaultRenderer;
  }

  @Override
  public Component getTreeCellRendererComponent(JTree tree,
                                                Object value,
                                                boolean selected,
                                                boolean expanded,
                                                boolean leaf,
                                                int row,
                                                boolean hasFocus) {
    assert value instanceof Property;
    final Property p = ((Property)value);

    final PropertyRenderer<Property> renderer = myInspector.getNameRenderer(p);
    if (p.isValid() && renderer != null) {
      final JComponent c = renderer.getRendererComponent(p, new RenderingContextImpl(myInspector, false, selected, hasFocus, expanded));
      if (c != null) return c;
    }

    return myDefaultRenderer.getTreeCellRendererComponent(tree, p.getName(), selected, expanded, leaf, row, hasFocus);
  }
}
