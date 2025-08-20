package com.intellij.jboss.bpmn.jbpm.render.label;

import com.intellij.jboss.bpmn.jbpm.providers.ColorProvider;
import com.intellij.jboss.bpmn.jbpm.providers.ProvidersCoordinator;
import com.intellij.jboss.bpmn.jbpm.render.ChartNodeRenderer;
import com.intellij.jboss.bpmn.jbpm.render.RenderArgs;
import com.intellij.openapi.graph.base.Node;
import com.intellij.openapi.graph.view.NodeLabel;

public class ChartNodeLabelColorRenderer<T> implements ChartNodeRenderer<T, RenderLabelColor> {
  @SuppressWarnings("unchecked")
  @Override
  public void renderComponent(RenderLabelColor color, RenderArgs<T> renderArgs) {
    Node node = renderArgs.builder.getNode(renderArgs.chartNode);
    NodeLabel label = renderArgs.builder.getGraph().getRealizer(node).getLabel();
    ColorProvider colorProvider = ProvidersCoordinator.getInstance().getProvider(color.color());
    label.setTextColor(colorProvider.getColor(renderArgs.chartNode));
  }

  @Override
  public Class<RenderLabelColor> getLayoutClass() {
    return RenderLabelColor.class;
  }
}
