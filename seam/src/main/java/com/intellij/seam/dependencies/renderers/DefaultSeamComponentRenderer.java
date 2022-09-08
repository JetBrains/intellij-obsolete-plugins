package com.intellij.seam.dependencies.renderers;

import com.intellij.openapi.graph.builder.GraphBuilder;
import com.intellij.openapi.graph.builder.renderer.BasicGraphNodeRenderer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiManager;
import com.intellij.seam.dependencies.beans.SeamComponentNodeInfo;
import com.intellij.seam.dependencies.beans.SeamDependencyInfo;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;

public class DefaultSeamComponentRenderer extends BasicGraphNodeRenderer<SeamComponentNodeInfo, SeamDependencyInfo> {

  public DefaultSeamComponentRenderer(GraphBuilder<SeamComponentNodeInfo, SeamDependencyInfo> builder) {
    super(builder, PsiManager.getInstance(builder.getProject()).getModificationTracker());
  }

  @Override
  protected Icon getIcon(final SeamComponentNodeInfo node) {
    return node.getIcon();
  }

  @Override
  @NonNls
  protected String getNodeName(final SeamComponentNodeInfo node) {
    final String name = node.getName();

    return StringUtil.isEmptyOrSpaces(name) ? "Noname": name;
  }

  @Override
  protected int getSelectionBorderWidth() {
    return 1;
  }
}
