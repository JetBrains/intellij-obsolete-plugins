package com.intellij.jboss.bpmn.jbpm.render;

import com.intellij.diagram.DiagramBuilder;
import com.intellij.jboss.bpmn.jbpm.annotation.AnnotationCoordinator;
import com.intellij.jboss.bpmn.jbpm.model.ChartNode;
import com.intellij.ui.JBColor;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ChartRenderer {
  @NotNull private final List<ChartNodeRenderer> nodeRenderers;
  @NotNull private final Map<Collection<Class<?>>, List<RendererInvocation>> invocationsCache = new HashMap<>();

  public ChartRenderer() {
    nodeRenderers = ChartNodeRenderer.EP_NAME.getExtensionList();
  }

  @SuppressWarnings("unchecked")
  @Nullable
  public <T> JPanel createNodeComponent(ChartNode<T> node, DiagramBuilder builder, Point basePoint, JPanel wrapper) {
    List<RendererInvocation> invocations = getInvocationsList(node.getClassesWithAnnotationsForRendering());
    if (invocations.isEmpty()) {
      return null;
    }
    ChartNodeMainPanel panel = new ChartNodeMainPanel(new BorderLayout());
    //noinspection UseJBColor
    panel.setBackground(JBColor.background());
    panel.setForeground(JBColor.foreground());
    panel.setBorder(JBUI.Borders.empty());
    RenderArgs renderArgs = new RenderArgs(node, builder, basePoint, wrapper, panel);
    for (RendererInvocation invocation : invocations) {
      invocation.invoke(renderArgs);
    }
    return panel;
  }

  private List<RendererInvocation> getInvocationsList(Collection<Class<?>> classes) {
    List<RendererInvocation> invocationList = invocationsCache.get(classes);
    if (invocationList == null) {
      invocationList = calculateInvocationsList(classes);
      invocationsCache.put(classes, invocationList);
    }
    return invocationList;
  }

  private List<RendererInvocation> calculateInvocationsList(Collection<Class<?>> classes) {
    return JBIterable.from(nodeRenderers)
      .transform(renderer -> {
        AnnotationCoordinator coordinator = new AnnotationCoordinator(renderer.getLayoutClass());
        Annotation annotation = coordinator.getAnnotation(classes);
        //noinspection unchecked
        return annotation == null ? null : new RendererInvocation(annotation, renderer);
      })
      .filter(invocation -> invocation != null).toList();
  }


  static class RendererInvocation<T, RenderOptions extends Annotation> {
    private final RenderOptions renderOptions;
    private final ChartNodeRenderer<T, RenderOptions> renderer;

    RendererInvocation(RenderOptions renderOptions, ChartNodeRenderer<T, RenderOptions> renderer) {
      this.renderOptions = renderOptions;
      this.renderer = renderer;
    }

    void invoke(RenderArgs<T> renderArgs) {
      renderer.renderComponent(renderOptions, renderArgs);
    }
  }
}
