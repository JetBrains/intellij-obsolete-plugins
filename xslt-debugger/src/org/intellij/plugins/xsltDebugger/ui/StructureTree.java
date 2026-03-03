// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.plugins.xsltDebugger.ui;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.UiDataProvider;
import com.intellij.pom.Navigatable;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.treeStructure.Tree;
import org.intellij.plugins.xsltDebugger.ui.actions.CopyValueAction;
import org.intellij.plugins.xsltDebugger.ui.actions.NavigateAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class StructureTree extends Tree implements UiDataProvider {
  public StructureTree(GeneratedStructureModel model) {
    super(model);

    setCellRenderer(new GeneratedStructureRenderer());
    setRootVisible(false);
    setShowsRootHandles(true);

    final DefaultActionGroup structureContextActions = DefaultActionGroup.createPopupGroup(() -> "StructureContext");
    structureContextActions.add(NavigateAction.getInstance());
    structureContextActions.add(new CopyValueAction(this));
    PopupHandler.installFollowingSelectionTreePopup(this, structureContextActions, "XSLT.Debugger.GeneratedStructure");
  }

  @Override
  public void uiDataSnapshot(@NotNull DataSink sink) {
    TreePath selection = getSelectionPath();
    Object obj = selection == null ? null : selection.getLastPathComponent();
    sink.set(CommonDataKeys.NAVIGATABLE, obj instanceof Navigatable o ? o : null);
    sink.set(CopyValueAction.SELECTED_NODE, obj instanceof DefaultMutableTreeNode o? o : null);
  }
}
