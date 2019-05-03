/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.tree;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts.NamedDomModel;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.model.impl.DomModelFactory;
import com.intellij.util.xml.tree.DomFileElementNode;
import com.intellij.util.xml.tree.DomModelTreeView;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;
import java.util.Map;

/**
 * @author Dmitry Avdeev
 */
public class StrutsTreeBase<T extends DomElement, M extends NamedDomModel<T>>  extends DomModelTreeView {

  private final List<Class<? extends DomElement>> myDependencies;

  protected StrutsTreeBase(final Project project,
                           final DomModelFactory<T, M, PsiElement> factory,
                           final Map<Class, Boolean> hiders,
                           final List<Class> consolidated,
                           final List<Class> folders,
                           final List<Class<? extends DomElement>> dependencies) {
    super(null, DomManager.getDomManager(project), new MultiDomTreeStructure<>(project, factory, hiders, consolidated, folders));
    myDependencies = dependencies;
  }

  @Override
  protected boolean isRightFile(final VirtualFile file) {
    final PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(file);

    if (psiFile == null) {
      return false;
    }

    if (!(psiFile instanceof XmlFile)) {
      return false;
    }

    final XmlFile xmlFile = (XmlFile) psiFile;
    if (hasFile((DefaultMutableTreeNode)getBuilder().getTreeModel().getRoot(), xmlFile)) {
      return true;
    }

    final DomManager domManager = DomManager.getDomManager(getProject());
    for (Class<? extends DomElement> dependency : myDependencies) {
      if (domManager.getFileElement(xmlFile, dependency) != null) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasFile(DefaultMutableTreeNode treeNode, XmlFile file) {
    final Object node = treeNode.getUserObject();
    if (node instanceof DomFileElementNode) {
      return ((DomFileElementNode)node).getDomElement().getFile() == file;
    } else {
      treeNode.children();
      for (int i = 0; i < treeNode.getChildCount(); i++) {
        if (hasFile((DefaultMutableTreeNode)treeNode.getChildAt(i), file)) {
          return true;
        }
      }
      return false;
    }
  }

  @Override
  protected boolean isRootVisible() {
    return false;
  }

  public void init() {
    if (getTree().getRowCount() < 2) {
      getTree().setRootVisible(true);
    }
    if (((DefaultMutableTreeNode)(getTree().getModel().getRoot())).getUserObject() == null) {
      getBuilder().initRootNode();
    }
    else {
      getBuilder().updateFromRoot();
    }
    getTree().expandRow(0);
    getTree().setRootVisible(false);
  }
}
