package com.intellij.jboss.bpmn.jbpm.render.label;

import com.intellij.jboss.bpmn.jbpm.providers.ProvidersCoordinator;
import com.intellij.jboss.bpmn.jbpm.providers.TextProvider;
import com.intellij.jboss.bpmn.jbpm.render.ChartNodeRenderer;
import com.intellij.jboss.bpmn.jbpm.render.RenderArgs;
import com.intellij.openapi.graph.base.Node;
import com.intellij.openapi.graph.view.NodeLabel;
import com.intellij.openapi.util.text.StringUtil;

public class ChartNodeLabelRenderer<T> implements ChartNodeRenderer<T, RenderLabelText> {
  @SuppressWarnings("unchecked")
  @Override
  public void renderComponent(RenderLabelText text, RenderArgs<T> renderArgs) {
    Node node = renderArgs.builder.getNode(renderArgs.chartNode);
    NodeLabel label = renderArgs.builder.getGraph().getRealizer(node).getLabel();
    TextProvider textProvider = ProvidersCoordinator.getInstance().getProvider(text.text());
    String providerText = textProvider.getText(renderArgs.chartNode);
    label.setText(StringUtil.isNotEmpty(providerText) ? providerText : "");
  }

  @Override
  public Class<RenderLabelText> getLayoutClass() {
    return RenderLabelText.class;
  }
}
