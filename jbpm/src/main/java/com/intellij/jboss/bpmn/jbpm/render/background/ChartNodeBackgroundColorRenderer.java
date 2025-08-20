package com.intellij.jboss.bpmn.jbpm.render.background;

import com.intellij.jboss.bpmn.jbpm.providers.ColorProvider;
import com.intellij.jboss.bpmn.jbpm.providers.ProvidersCoordinator;
import com.intellij.jboss.bpmn.jbpm.render.ChartNodeRenderer;
import com.intellij.jboss.bpmn.jbpm.render.RenderArgs;

public class ChartNodeBackgroundColorRenderer<T> implements ChartNodeRenderer<T, RenderBackgroundColor> {
  @SuppressWarnings("unchecked")
  @Override
  public void renderComponent(RenderBackgroundColor backgroundColor, RenderArgs<T> renderArgs) {
    ColorProvider colorProvider = ProvidersCoordinator.getInstance().getProvider(backgroundColor.color());
    renderArgs.wrapper.setBackground(colorProvider.getColor(renderArgs.chartNode));
  }

  @Override
  public Class<RenderBackgroundColor> getLayoutClass() {
    return RenderBackgroundColor.class;
  }
}
