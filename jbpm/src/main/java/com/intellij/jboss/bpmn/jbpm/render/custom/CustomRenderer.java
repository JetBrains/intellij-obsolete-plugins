package com.intellij.jboss.bpmn.jbpm.render.custom;

import com.intellij.jboss.bpmn.jbpm.render.RenderArgs;

public interface CustomRenderer<T> {
  void render(RenderArgs<T> renderArgs);
}
