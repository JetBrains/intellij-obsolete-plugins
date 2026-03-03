// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.plugins.xsltDebugger.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.Alarm;
import com.intellij.util.ui.tree.TreeModelAdapter;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class SmartStructureTracker extends TreeModelAdapter {
  private final JTree myEventTree;
  private final Alarm myAlarm;

  public SmartStructureTracker(JTree eventTree, @NotNull Disposable disposable) {
    myEventTree = eventTree;
    myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, disposable);
  }

  @Override
  public void treeNodesInserted(TreeModelEvent e) {
    final TreePath path = e.getTreePath();
    final Object child = e.getChildren()[0];
    if (path != null && child != null) {
      myAlarm.cancelAllRequests();
      final Runnable runnable = () -> {
        myEventTree.expandPath(path);
        TreeUtil.showRowCentered(myEventTree, myEventTree.getRowForPath(TreeUtil.getPathFromRoot((TreeNode)child)), false);
      };
      myAlarm.addRequest(runnable, 300);
    }
  }

  @Override
  public void treeNodesRemoved(TreeModelEvent e) {
    final TreePath p = e.getTreePath();
    if (p != null) {
      if (p.getPathCount() > 1) {
        final Runnable runnable = () -> {
          DefaultMutableTreeNode last = (DefaultMutableTreeNode)p.getLastPathComponent();
          if (last.getChildCount() > 0) {
            DefaultMutableTreeNode next = (DefaultMutableTreeNode)last.getFirstChild();
            while (next != null) {
              myEventTree.collapsePath(TreeUtil.getPathFromRoot(next));
              next = next.getNextSibling();
            }
          }
        };
        ApplicationManager.getApplication().invokeLater(runnable);
      }
    }
  }
}
