/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.designer.inspector.impl;

import com.intellij.designer.inspector.Property;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.TreeUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;

/**
 * @author spleaner
 */
@SuppressWarnings({"AbstractClassNeverImplemented"})
public abstract class InspectorTreeUtil {

  public static int getNodeDepth(JTree tree, TreeNode node) {
    final TreePath path = TreeUtil.getPathFromRoot(node);
    return path.getPathCount() - (tree.isRootVisible() ? 1 : 2);
  }

  public static int getAvailableWidth(final JTree tree, final Property p) {
    return getAvailableWidth(tree, getNodeDepth(tree, p));
  }

  public static int getAvailableWidth(final JTree tree, final int depth) {
    return tree.getSize().width - (tree.getInsets().right + tree.getInsets().left + getRowX(tree, depth));
  }

  @Nullable
  public static <T extends Component> T getParentOfType(Component source, Class<T> clazz) {
    Component parent = source.getParent();
    while (parent != null) {
      if (clazz.isInstance(parent)) {
        return (T) parent;
      }
    }

    return null;
  }

  /**
   * @see javax.swing.plaf.basic.BasicTreeUI#getRowX(int, int)
   */
  public static int getRowX(JTree tree, int depth) {
    final TreeUI ui = tree.getUI();
    if (ui instanceof BasicTreeUI) {
      final BasicTreeUI treeUI = ((BasicTreeUI)ui);
      return (treeUI.getLeftChildIndent() + treeUI.getRightChildIndent()) * depth;
    }

    final int leftIndent = UIUtil.getTreeLeftChildIndent();
    final int rightIndent = UIUtil.getTreeRightChildIndent();

    return (leftIndent + rightIndent) * depth;
  }


}
