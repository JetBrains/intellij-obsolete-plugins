/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.designer.inspector;

import com.intellij.ui.treeStructure.treetable.TreeTableModel;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;

/**
 * @author spleaner
 */
public class PropertyInspectorModel extends DefaultTreeModel implements TreeTableModel {

  public PropertyInspectorModel(Property root) {
    super(root);
  }

  @Override
  public void setTree(JTree tree) {
  }

  @Override
  public int getColumnCount() {
    throw new UnsupportedOperationException("Method getColumnCount is not implemented in " + getClass().getName());
  }

  @Override
  public String getColumnName(int column) {
    throw new UnsupportedOperationException("Method getColumnName is not implemented in " + getClass().getName());
  }

  @Override
  public Class getColumnClass(int column) {
    throw new UnsupportedOperationException("Method getColumnClass is not implemented in " + getClass().getName());
  }

  @Override
  public Object getValueAt(Object node, int column) {
    throw new UnsupportedOperationException("Method getValueAt is not implemented in " + getClass().getName());
  }

  @Override
  public boolean isCellEditable(Object node, int column) {
    throw new UnsupportedOperationException("Method isCellEditable is not implemented in " + getClass().getName());
  }

  @Override
  public void setValueAt(Object aValue, Object node, int column) {
    throw new UnsupportedOperationException("Method setValueAt is not implemented in " + getClass().getName());
  }
}
