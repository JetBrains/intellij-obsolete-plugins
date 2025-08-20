package com.intellij.jboss.bpmn.jbpm.render.custom;

import com.intellij.jboss.bpmn.jbpm.render.ChartNodeRenderer;
import com.intellij.jboss.bpmn.jbpm.render.RenderArgs;
import com.intellij.openapi.diagnostic.Logger;

public class ChartNodeCustomRenderer<T> implements ChartNodeRenderer<T, RenderCustom> {
  private static final Logger LOG = Logger.getInstance(ChartNodeCustomRenderer.class);

  @Override
  public void renderComponent(RenderCustom custom, RenderArgs<T> renderArgs) {
    for (Class<? extends CustomRenderer> clazz : custom.value()) {
      try {
        CustomRenderer renderer = clazz.newInstance();
        //noinspection unchecked
        renderer.render(renderArgs);
      }
      catch (InstantiationException | IllegalAccessException e) {
        LOG.warn(e);
      }
    }
  }

  @Override
  public Class<RenderCustom> getLayoutClass() {
    return RenderCustom.class;
  }
}
