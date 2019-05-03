/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.tree;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiFile;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.tree.DomFileElementNode;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;

/**
 * @author Dmitry Avdeev
 */
public class DomBrowser implements Disposable {

  private final JScrollPane myScrollPane;
  private final StrutsTreeBase myTreeView;

  public DomBrowser(StrutsTreeBase tree) {
    myTreeView = tree;
    Disposer.register(this, myTreeView);
    myScrollPane = ScrollPaneFactory.createScrollPane(myTreeView);
  }

  public SimpleTree getTree() {
    return myTreeView.getTree();
  }

  @NotNull
  public JComponent getComponent() {
    return myScrollPane;
  }

  public void setSelectedDomElement(DomElement element) {
    myTreeView.setSelectedDomElement(element);
  }

  public boolean hasFile(PsiFile file) {

    final TreeModel treeModel = getTree().getModel();
    final Object root = treeModel.getRoot();
    if (root == null) {
      return false;
    }
    for (int i = 0; i < treeModel.getChildCount(root); i++) {
      Object el = ((DefaultMutableTreeNode)treeModel.getChild(root, i)).getUserObject();
      if (el instanceof DomFileElementNode) {
        if (((DomFileElementNode)el).getDomElement().getFile() == file) {
          return true;
        }
      }
    }
    return false;
  }

  public void openDefault() {
    myTreeView.init();
  }

  public void update() {
    myTreeView.getBuilder().updateFromRoot();
  }

  @Override
  public void dispose() {
  }
}
