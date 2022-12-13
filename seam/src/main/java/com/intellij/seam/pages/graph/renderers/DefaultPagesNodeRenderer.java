package com.intellij.seam.pages.graph.renderers;

import com.intellij.openapi.graph.builder.GraphBuilder;
import com.intellij.openapi.graph.builder.renderer.BasicGraphNodeRenderer;
import com.intellij.psi.PsiManager;
import com.intellij.seam.pages.graph.beans.BasicPagesEdge;
import com.intellij.seam.pages.graph.beans.BasicPagesNode;

import javax.swing.*;

public class DefaultPagesNodeRenderer extends BasicGraphNodeRenderer<BasicPagesNode, BasicPagesEdge> {
  public DefaultPagesNodeRenderer(final GraphBuilder<BasicPagesNode, BasicPagesEdge> graphBuilder) {
    super(graphBuilder, PsiManager.getInstance(graphBuilder.getProject()).getModificationTracker());
  }

  @Override
  protected Icon getIcon(final BasicPagesNode node) {
    return node.getIcon();
  }

  @Override
  protected String getNodeName(BasicPagesNode node) {
    return node.getName();
  }

  @Override
  protected int getSelectionBorderWidth() {
    return 1;
  }
}
