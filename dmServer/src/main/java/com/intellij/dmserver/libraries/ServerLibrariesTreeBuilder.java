package com.intellij.dmserver.libraries;

import com.intellij.ide.util.treeView.AbstractTreeBuilder;
import com.intellij.ide.util.treeView.IndexComparator;
import com.intellij.ide.util.treeView.TreeBuilderUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.ui.tree.TreeUtil;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;

public class ServerLibrariesTreeBuilder extends AbstractTreeBuilder {

  public ServerLibrariesTreeBuilder(ServerLibrariesContext context, JTree tree, DefaultTreeModel treeModel) {
    super(tree, treeModel, new ServerLibrariesTreeStructure(context), IndexComparator.INSTANCE);
    initRootNode();
  }

  public void expandAll() {
    ArrayList pathsToExpand = new ArrayList();
    ArrayList selectionPaths = new ArrayList();
    TreeBuilderUtil.storePaths(this, getRootNode(), pathsToExpand, selectionPaths, true);
    int row = 0;
    while (row < Math.max(getTree().getRowCount(), 100)) {
      getTree().expandRow(row);
      row++;
    }
    getTree().clearSelection();
    TreeBuilderUtil.restorePaths(this, pathsToExpand, selectionPaths, true);
  }

  public void collapseAll() {
    ArrayList pathsToExpand = new ArrayList();
    ArrayList selectionPaths = new ArrayList();
    TreeBuilderUtil.storePaths(this, getRootNode(), pathsToExpand, selectionPaths, true);
    TreeUtil.collapseAll(getTree(), 1);
    getTree().clearSelection();
    pathsToExpand.clear();
    TreeBuilderUtil.restorePaths(this, pathsToExpand, selectionPaths, true);
  }

  public void forceRefresh() {
    ArrayList pathsToExpand = new ArrayList();
    ArrayList selectionPaths = new ArrayList();
    TreeBuilderUtil.storePaths(this, getRootNode(), pathsToExpand, selectionPaths, true);
    ApplicationManager.getApplication().runReadAction(
      () -> {
        queueUpdate();
      }
    );
    getTree().clearSelection();
    TreeBuilderUtil.restorePaths(this, pathsToExpand, selectionPaths, true);
  }


}
