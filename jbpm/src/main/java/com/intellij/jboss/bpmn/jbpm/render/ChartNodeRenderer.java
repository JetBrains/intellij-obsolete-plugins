package com.intellij.jboss.bpmn.jbpm.render;

import com.intellij.openapi.extensions.ExtensionPointName;

import java.lang.annotation.Annotation;

public interface ChartNodeRenderer<T, RenderOptions extends Annotation> {
  ExtensionPointName<ChartNodeRenderer> EP_NAME = ExtensionPointName.create("com.intellij.jbpm.chartNodeRenderer");

  void renderComponent(RenderOptions options, RenderArgs<T> renderArgs);

  Class<RenderOptions> getLayoutClass();
}
