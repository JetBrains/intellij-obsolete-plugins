package com.intellij.seam.pageflow.graph.renderers;

import com.intellij.openapi.graph.builder.GraphBuilder;
import com.intellij.openapi.graph.builder.renderer.BasicGraphNodeRenderer;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.psi.PsiManager;
import com.intellij.seam.pageflow.graph.PageflowEdge;
import com.intellij.seam.pageflow.graph.PageflowNode;

import javax.swing.*;

public class DefaultPageflowNodeRenderer extends BasicGraphNodeRenderer<PageflowNode, PageflowEdge> {

  public DefaultPageflowNodeRenderer(GraphBuilder<PageflowNode, PageflowEdge> builder) {
    super(builder, PsiManager.getInstance(builder.getProject()).getModificationTracker());
  }

  @Override
  protected JComponent getPresenationComponent(@NlsContexts.Label String text) {
    return super.getPresenationComponent(text);
  }

  @Override
  protected Icon getIcon(final PageflowNode node) {
    return node.getIcon();
  }

  @Override
  protected String getNodeName(PageflowNode node) {
    return node.getName();
  }

  @Override
  protected int getSelectionBorderWidth() {
    return 1;
  }
}
