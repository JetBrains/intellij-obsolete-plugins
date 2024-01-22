package com.intellij.jboss.bpmn.jbpm.render.label;

import com.intellij.jboss.bpmn.jbpm.render.ChartNodeRenderer;
import com.intellij.jboss.bpmn.jbpm.render.RenderArgs;
import com.intellij.openapi.graph.base.Node;
import com.intellij.openapi.graph.view.NodeLabel;

public class ChartNodeLabelPositionRenderer<T> implements ChartNodeRenderer<T, RenderLabelPosition> {
  @Override
  public void renderComponent(RenderLabelPosition position, RenderArgs<T> renderArgs) {
    Node node = renderArgs.builder.getNode(renderArgs.chartNode);
    NodeLabel label = renderArgs.builder.getGraph().getRealizer(node).getLabel();
    label.setModel(position.modelSpecifier());
    label.setPosition(position.positionSpecifier());
  }

  @Override
  public Class<RenderLabelPosition> getLayoutClass() {
    return RenderLabelPosition.class;
  }
}
