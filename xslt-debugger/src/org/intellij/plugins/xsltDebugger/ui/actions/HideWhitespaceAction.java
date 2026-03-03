// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.intellij.plugins.xsltDebugger.ui.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.treeView.TreeState;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.ui.treeStructure.Tree;
import org.intellij.plugins.xsltDebugger.XsltDebuggerBundle;
import org.intellij.plugins.xsltDebugger.ui.GeneratedStructureModel;
import org.jetbrains.annotations.NotNull;

public class HideWhitespaceAction extends ToggleAction {
  private final Tree myStructureTree;
  private final GeneratedStructureModel myEventModel;

  public HideWhitespaceAction(Tree structureTree, GeneratedStructureModel eventModel) {
    super(XsltDebuggerBundle.message("action.hide.whitespace.nodes.text"));
    myStructureTree = structureTree;
    myEventModel = eventModel;

    getTemplatePresentation().setIcon(AllIcons.ObjectBrowser.FlattenPackages);
  }

  @Override
  public boolean isSelected(@NotNull AnActionEvent e) {
    return myEventModel.isFilterWhitespace();
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean state) {
    final TreeState treeState = TreeState.createOn(myStructureTree);
    myEventModel.setFilterWhitespace(state);
    treeState.applyTo(myStructureTree);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }
}
