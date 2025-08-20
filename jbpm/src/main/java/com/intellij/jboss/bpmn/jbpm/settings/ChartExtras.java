package com.intellij.jboss.bpmn.jbpm.settings;

import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramNode;
import com.intellij.diagram.DiagramVfsResolver;
import com.intellij.diagram.extras.DiagramExtras;
import com.intellij.jboss.bpmn.jbpm.annotation.AnnotationNotNullCoordinator;
import com.intellij.jboss.bpmn.jbpm.layout.ChartLayoutCoordinator;
import com.intellij.jboss.bpmn.jbpm.model.ChartNode;
import com.intellij.jboss.bpmn.jbpm.render.ChartRenderer;
import com.intellij.jboss.bpmn.jbpm.render.size.ChartNodeSizeEnhancer;
import com.intellij.jboss.bpmn.jbpm.render.size.RenderDefaultSize;
import com.intellij.jboss.bpmn.jbpm.ui.ChartBuilder;
import com.intellij.openapi.graph.view.NodeRealizer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class ChartExtras<T> extends DiagramExtras<T> {
  @NotNull private final AnnotationNotNullCoordinator<RenderDefaultSize> renderSizeCoordinator;
  @NotNull private final ChartRenderer chartRenderer;

  public ChartExtras() {
    renderSizeCoordinator = new AnnotationNotNullCoordinator<>(RenderDefaultSize.class, getNodeDefaultSize());
    chartRenderer = new ChartRenderer();
  }

  @NotNull
  public AnnotationNotNullCoordinator<RenderDefaultSize> getRenderSizeCoordinator() {
    return renderSizeCoordinator;
  }

  public RenderDefaultSize getNodeDefaultSize() {
    return RenderDefaultSize.class.getAnnotation(RenderDefaultSize.class);
  }

  @Override
  public @NotNull JComponent createNodeComponent(
    @NotNull DiagramNode<T> node,
    @NotNull DiagramBuilder builder,
    @NotNull NodeRealizer nodeRealizer,
    @NotNull JPanel wrapper
  ) {
    assert node instanceof ChartNode;
    ChartNode<T> chartNode = (ChartNode<T>)node;
    assert builder instanceof ChartBuilder;
    @SuppressWarnings("unchecked") ChartBuilder<T> chartBuilder = (ChartBuilder<T>)builder;
    ChartLayoutCoordinator chartLayoutCoordinator = chartBuilder.getChartLayoutCoordinator();
    DiagramVfsResolver<T> resolver = chartBuilder.getProvider().getVfsResolver();
    JComponent result = chartRenderer.createNodeComponent(chartNode, builder, new Point(0, 0), wrapper);
    if (result == null) {
      result = super.createNodeComponent(node, builder, nodeRealizer, wrapper);
    }

    ChartLayoutCoordinator.NodeLayout nodeLayout = null;
    if (chartLayoutCoordinator != null) {
      nodeLayout = chartLayoutCoordinator.getNodeLayout(
        resolver.getQualifiedName(node.getIdentifyingElement()),
        ChartNodeSizeEnhancer.enhancerForNode(chartNode));
    }

    if (nodeLayout != null) {
      result.setPreferredSize(new Dimension((int)nodeLayout.getWidth(), (int)nodeLayout.getHeight()));
    }
    else {
      @NotNull RenderDefaultSize size =
        renderSizeCoordinator.getAnnotation(chartNode.getClassesWithAnnotationsForRendering());
      result.setPreferredSize(new Dimension((int)size.width(), (int)size.height()));
    }
    return result;
  }
}
