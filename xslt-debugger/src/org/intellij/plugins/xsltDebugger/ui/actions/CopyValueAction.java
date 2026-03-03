// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.intellij.plugins.xsltDebugger.ui.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.ide.CopyPasteManager;
import org.intellij.plugins.xsltDebugger.rt.engine.OutputEventQueue;
import org.intellij.plugins.xsltDebugger.ui.GeneratedStructureModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.datatransfer.StringSelection;

public class CopyValueAction extends AnAction {
  public static final DataKey<DefaultMutableTreeNode> SELECTED_NODE = DataKey.create("SELECTED_NODE");

  public CopyValueAction(JComponent component) {
    ActionUtil.copyFrom(this, "$Copy");
    registerCustomShortcutSet(getShortcutSet(), component);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(isEnabled(e));
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final DefaultMutableTreeNode node = e.getData(SELECTED_NODE);
    if (node instanceof GeneratedStructureModel.StructureNode structureNode) {
      final OutputEventQueue.NodeEvent event = structureNode.getUserObject();
      setClipboardData(event.getValue());
    }
  }

  private static void setClipboardData(String value) {
    CopyPasteManager.getInstance().setContents(new StringSelection(value));
  }

  protected static boolean isEnabled(AnActionEvent e) {
    final DefaultMutableTreeNode node = e.getData(SELECTED_NODE);
    if (node instanceof GeneratedStructureModel.StructureNode structureNode) {
      final OutputEventQueue.NodeEvent event = structureNode.getUserObject();
      return event != null && event.getValue() != null;
    }
    return false;
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
