package com.intellij.jboss.bpmn.jbpm.render.size;

// Sometimes stored size could not be shown correctly, so it change visible size for correct presentation

import com.intellij.jboss.bpmn.jbpm.annotation.AnnotationCoordinator;
import com.intellij.jboss.bpmn.jbpm.layout.ChartLayoutCoordinator;
import com.intellij.jboss.bpmn.jbpm.model.ChartNode;
import com.intellij.jboss.bpmn.jbpm.providers.ProvidersCoordinator;

public abstract class ChartNodeSizeEnhancer {
  private static final AnnotationCoordinator<SizeEnhancer> enhancerAnnotationCoordinator =
    new AnnotationCoordinator<>(SizeEnhancer.class);

  public abstract ChartLayoutCoordinator.Size enhance(ChartLayoutCoordinator.Size size);

  public static <T> ChartNodeSizeEnhancer enhancerForNode(ChartNode<T> node) {
    SizeEnhancer annotation = enhancerAnnotationCoordinator
      .getAnnotation(node.getClassesWithAnnotationsForRendering());
    if (annotation == null) {
      return null;
    }
    return ProvidersCoordinator.getInstance().getProvider(annotation.value());
  }
}
